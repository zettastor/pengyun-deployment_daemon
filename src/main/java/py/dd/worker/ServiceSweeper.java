/*
 * Copyright (c) 2022. PengYunNetWork
 *
 * This program is free software: you can use, redistribute, and/or modify it
 * under the terms of the GNU Affero General Public License, version 3 or later ("AGPL"),
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 *  You should have received a copy of the GNU Affero General Public License along with
 *  this program. If not, see <http://www.gnu.org/licenses/>.
 */

package py.dd.worker;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.PyService;
import py.dd.DeploymentDaemonConfiguration;
import py.dd.common.ServiceMetadata;
import py.dd.common.ServiceStatus;
import py.dd.processor.DeploymentDaemonProcessor;
import py.dd.processor.chain.ProcessorChainFactory;
import py.dd.service.ServiceMonitor;
import py.dd.service.store.ServiceStore;
import py.dd.utils.DdUtils;
import py.periodic.Worker;
import py.processmanager.Pmdb;
import py.thrift.deploymentdaemon.StartRequest;

/**
 * After start service action done, service always takes lots of time to turn into stable. {@link
 * ServiceSweeper} implements interface {@link Worker} to periodically check state of service and
 * decide which status the service is and backup the status to file store.
 *
 * <p>Actually the instance of this class is just a state machine which transfer service status.
 */
public class ServiceSweeper implements Worker {

  public static final int NEED_START_COUNT_THRESHOLD = 10;
  private static final Logger logger = LoggerFactory.getLogger(ServiceSweeper.class);
  private ServiceStore serviceStore;

  private DeploymentDaemonConfiguration ddConfig;

  private ServiceMonitor systemMonitor;

  private ProcessorChainFactory processorChainFactory;

  private ExecutorService ddThreadPool;

  private Map<PyService, AtomicInteger> needStartPmCountMap;

  public ServiceSweeper() {
    this.needStartPmCountMap = new ConcurrentHashMap<>();
  }

  @Override
  public void doWork() throws Exception {
    List<ServiceMetadata> existingServiceList = serviceStore.list();

    if (existingServiceList == null || existingServiceList.isEmpty()) {
      logger.debug("No service to check for sweeper");
      return;
    }
    try {
      Map<PyService, Integer> pmPidMap = new HashMap<>();
      for (ServiceMetadata service : existingServiceList) {
        PyService pyService = PyService.findValueByServiceName(service.getServiceName());
        pmPidMap.put(pyService, 0);
      }

      try {
        DdUtils.getManyPmPidFromCommandByServiceNameMap(pmPidMap, ddConfig.getJpsCommandPath());
      } catch (Exception e) {
        logger.error("caught an exception when execute command to get PM pid", e);
      }

      for (ServiceMetadata service : existingServiceList) {
        PyService pyService = PyService.findValueByServiceName(service.getServiceName());
        if (!this.needStartPmCountMap.containsKey(pyService)) {
          this.needStartPmCountMap.putIfAbsent(pyService, new AtomicInteger(0));
        }
        if (!DdUtils.isRunnable(pyService)) {
          continue;
        }

        int pidFromFile = (Paths
            .get(ddConfig.buildServiceRunningPath(pyService).toString(), Pmdb.SERVICE_PID_NAME)
            .toFile()
            .exists())
            ? DdUtils.getProcessPid(ddConfig.buildServiceRunningPath(pyService).toString(),
            Pmdb.SERVICE_PID_NAME)
            : 0;
        // int pid = systemMonitor.getProcessId(pyService);
        int pid = systemMonitor.getProcessId(pyService, pidFromFile);

        final int pmPidFromFile =
            (Paths.get(ddConfig.buildServiceRunningPath(pyService).toString(), Pmdb.PM_PID_NAME)
                .toFile().exists())
                ? DdUtils.getProcessPid(ddConfig.buildServiceRunningPath(pyService).toString(),
                Pmdb.PM_PID_NAME)
                : 0;
        // int pmPid = systemMonitor.getPMPid(pyService);
        Validate.isTrue(pmPidMap.containsKey(pyService));
        int pmPid = pmPidMap.get(pyService);
        logger.debug("service {}, running path: {}", pyService.getServiceName(),
            ddConfig.buildServiceRunningPath(pyService).toString());

        logger.debug("Process id from file: {}, process id from os env: {}", pidFromFile, pid);
        logger.debug("PM id from file: {}, pm id from os env: {}", pmPidFromFile, pmPid);

        boolean isServiceAlive = (pid != 0) && (pid == pidFromFile);
        boolean isPmAlive = (pmPid != 0) && (pmPid == pmPidFromFile);
        boolean isDead = (pmPid == 0) && (pid == 0);
        boolean needStartProcessManager = false;

        switch (service.getServiceStatus()) {
          case ACTIVATING:
            if (isServiceAlive) {
              logger.debug("Service is alive, turn status from {} to {}", ServiceStatus.ACTIVATING,
                  ServiceStatus.ACTIVE);
              service.setServiceStatus(ServiceStatus.ACTIVE);
              serviceStore.save(service);
            }
            if (!isPmAlive) {
              needStartProcessManager = true;
            }
            break;
          case ACTIVE:
            if (isPmAlive && !isServiceAlive) {
              logger.debug("Service is not alive, but pm is alive, turn status from {} to {}",
                  ServiceStatus.ACTIVE, ServiceStatus.ACTIVATING);
              service.setServiceStatus(ServiceStatus.ACTIVATING);
              serviceStore.save(service);
            } else if (!isPmAlive && !isServiceAlive) {
              logger.error("Service {} is already dead, but it has incorrect status {}",
                  service.getServiceName(), service.getServiceStatus());
            }
            if (!isPmAlive) {
              needStartProcessManager = true;
            }
            break;
          case DEACTIVATING:
            if (isDead) {
              logger.debug("Service and process manager both are dead, turn status from {} to {}",
                  ServiceStatus.DEACTIVATING, ServiceStatus.DEACTIVE);
              service.setServiceStatus(ServiceStatus.DEACTIVE);
              serviceStore.save(service);
            }
            break;
          case DEACTIVE:
            if (isServiceAlive || isPmAlive) {
              logger.warn(
                  "Service {} or its PM is still running,"
                      + " but it has incorrect status {}, maybe the service is being destroyed",
                  service.getServiceName(), service.getServiceStatus());
            }
            break;
          default:
        }
        if (needStartProcessManager) {
          this.needStartPmCountMap.get(pyService).incrementAndGet();
          logger.warn(
              "service sweeper found process manager on:{} "
                  + "is not running, count it, service status:{}",
              service.getServiceName(), service.getServiceStatus());
        } else {
          this.needStartPmCountMap.get(pyService).set(0);
          logger.warn(
              "service sweeper found process manager on:{} is running, set its' count to zero",
              service.getServiceName());
        }
        int thresholdMultiple = 1;
        if (pyService.equals(PyService.DATANODE)) {
          thresholdMultiple = 10;
        }
        int needStartCount = this.needStartPmCountMap.get(pyService).get();
        if (needStartCount > 0
            && needStartCount % (NEED_START_COUNT_THRESHOLD * thresholdMultiple) == 0) {
          logger.warn(
              "service sweeper found process manager on:{} is not running, start it, multiple:{}",
              service.getServiceName(), thresholdMultiple);
          final StartRequest request = new StartRequest();
          request.setServiceName(service.getServiceName());

          service.setServiceStatus(ServiceStatus.ACTIVATING);
          serviceStore.save(service);

          final DeploymentDaemonProcessor chainToStartService = processorChainFactory
              .createServiceStartingChain();
          ddThreadPool.execute(new Runnable() {
            @Override
            public void run() {
              try {
                chainToStartService.process(request);
              } catch (Exception e) {
                logger.error("Caught an exception when start service {}", service.getServiceName(),
                    e);
              }
            }
          });
        } // end if (needStartCount judgement)
      } // service for loop
    } catch (Exception e) {
      logger.error("sweeper caught an exception", e);
      throw e;
    }
  }

  public ServiceStore getServiceStore() {
    return serviceStore;
  }

  public void setServiceStore(ServiceStore serviceStore) {
    this.serviceStore = serviceStore;
  }

  public DeploymentDaemonConfiguration getDdConfig() {
    return ddConfig;
  }

  public void setDdConfig(DeploymentDaemonConfiguration ddConfig) {
    this.ddConfig = ddConfig;
  }

  public ServiceMonitor getSystemMonitor() {
    return systemMonitor;
  }

  public void setSystemMonitor(ServiceMonitor systemMonitor) {
    this.systemMonitor = systemMonitor;
  }

  public void setProcessorChainFactory(ProcessorChainFactory processorChainFactory) {
    this.processorChainFactory = processorChainFactory;
  }

  public void setDdThreadPool(ExecutorService ddThreadPool) {
    this.ddThreadPool = ddThreadPool;
  }
}
