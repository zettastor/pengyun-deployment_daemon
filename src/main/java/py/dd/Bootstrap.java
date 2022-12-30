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

package py.dd;

import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.PyService;
import py.dd.common.ServiceMetadata;
import py.dd.common.ServiceStatus;
import py.dd.processor.DeploymentDaemonProcessor;
import py.dd.processor.chain.ProcessorChainFactory;
import py.dd.service.ServiceMonitor;
import py.dd.service.store.ServiceFileStore;
import py.dd.service.store.ServiceStore;
import py.dd.utils.DdUtils;
import py.processmanager.Pmdb;
import py.thrift.deploymentdaemon.DeactivateRequest;
import py.thrift.deploymentdaemon.DestroyRequest;
import py.thrift.deploymentdaemon.StartRequest;

/**
 * After service deployed to remote machines and ran well, an unexpected power off can stop all
 * services. In the case, we should make sure all services will automatically start up as long as
 * the machine rebooted up. The instance of this class do the job.
 *
 * <p>Each service has its status stored in file and
 * current process status to decide what should to do when deployment daemon service restart.
 */
public class Bootstrap {

  private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

  private DeploymentDaemonConfiguration ddConfig;

  private ServiceMonitor sysMonWrapper;

  private ServiceStore serviceStore;

  private ProcessorChainFactory processorChainFactory;

  private ExecutorService ddThreadPool;

  /**
   * xx.
   */
  public void start() {
    logger.info("Now to bootstrap services on localhost");

    for (PyService service : PyService.values()) {
      if ((!DdUtils.isRunnable(service)) || (service == PyService.DEPLOYMENTDAMON)) {
        continue;
      }

      if (ddConfig.buildServiceRunningPath(service).toFile().exists()) {
        // the running path of service exists, load its metadata to memory
        ((ServiceFileStore) serviceStore).load(service);
        logger.info("serviceStore load :{}", serviceStore.list());
        bootstrap(service);
      } else {
        // check if pengyun service is alive in 
        // environment but no running path to record its state, destroy
        // these orphan processed
        DestroyRequest request = new DestroyRequest();
        request.setServiceName(service.getServiceName());

        int pid = sysMonWrapper.getProcessId(service);
        int pmPid = 0;
        try {
          pmPid = sysMonWrapper.getPmPid(service);
        } catch (Exception e) {
          logger.error("caught an exception when try get PMPid at:{}", service.getServiceName(), e);
        }

        if (pid == 0 && pmPid == 0) {
          continue;
        }

        logger
            .info("Destroy orphan process of service {} in environment", service.getServiceName());
        try {
          processorChainFactory.createServiceDestroyChain().process(request);
        } catch (Exception e) {
          logger.error("Caught an exception", e);
        }
      }
    }
  }

  /**
   * From old status stored in file and current process state to decide what to do for each
   * service.
   */
  private void bootstrap(final PyService service) {
    int pid = sysMonWrapper.getProcessId(service);
    int pmPid = 0;
    try {
      pmPid = sysMonWrapper.getPmPid(service);
    } catch (Exception e) {
      logger.error("caught an exception when try get PMPid at:{}", service.getServiceName(), e);
    }
    int pidFromFile = DdUtils.getProcessPid(ddConfig.buildServiceRunningPath(service).toString(),
        Pmdb.SERVICE_PID_NAME);
    int pmPidFromFile = DdUtils.getProcessPid(ddConfig.buildServiceRunningPath(service).toString(),
        Pmdb.PM_PID_NAME);

    ServiceMetadata serviceMetadata = serviceStore.get(service);
    if (serviceMetadata == null) {
      // no service metadata means the process is orphan
      logger.info(
          "Destroy orphan process of service {} in "
              + "environment due to no metadata saved in running path",
          service.getServiceName());
      DestroyRequest request = new DestroyRequest();
      request.setServiceName(service.getServiceName());

      try {
        processorChainFactory.createServiceDestroyChain().process(request);
      } catch (Exception e) {
        logger.error("Caught an exception", e);
      }
      return;
    }

    // exist service process id in linux process directory 
    // ''/proc/'' and the id is same as process id record in
    // file under running path
    boolean isServiceAlive = (pid != 0 && pidFromFile == pid);

    // exist pm process id in linux process directroy 
    // ''/proc/'' and the id is same as process id record in
    // file under running path
    boolean isPmAlive = (pmPid != 0 && pmPidFromFile == pmPid);

    // both service and pm processes doesn't exist ''/proc/''.
    boolean isServiceDead = (pid == 0 && pmPid == 0);

    switch (serviceMetadata.getServiceStatus()) {
      case ACTIVATING:
        if (isServiceAlive) {
          logger.info("Serivice {} is alive, turn status from {} to {}", service.getServiceName(),
              ServiceStatus.ACTIVATING, ServiceStatus.ACTIVE);

          serviceMetadata.setServiceStatus(ServiceStatus.ACTIVE);
          serviceStore.save(serviceMetadata);
          return;
        } else if (isPmAlive) {
          logger.info("PM for service {} is alive, the service is not", service.getServiceName());
          return;
        }
        break;
      case ACTIVE:
        if (isServiceAlive) {
          logger.info("Service {} is alive", service.getServiceName());
          return;
        } else if (isPmAlive) {
          logger.info("PM for service {} is alive", service.getServiceName());
          serviceMetadata.setServiceStatus(ServiceStatus.ACTIVATING);
          serviceStore.save(serviceMetadata);
          return;
        }
        logger.debug("Service {} needs to start up in bootstrap", service.getServiceName());
        DestroyRequest destroyRequest = new DestroyRequest();
        destroyRequest.setServiceName(service.getServiceName());

        try {
          processorChainFactory.createServiceDestroyChain().process(destroyRequest);
        } catch (Exception e) {
          logger.error("Caught an exception", e);
        }

        if (service.equals(PyService.DATANODE)) {
          logger
              .warn("can not startup datanode:{} here, because 010/020/030 shell can not be felt, "
                  + "then sweeper worker will startup datanode again", service.getServiceName());
          return;
        }

        final StartRequest request = new StartRequest();
        request.setServiceName(service.getServiceName());

        serviceMetadata.setServiceStatus(ServiceStatus.ACTIVATING);
        serviceStore.save(serviceMetadata);

        final DeploymentDaemonProcessor chainToStartService = processorChainFactory
            .createServiceStartingChain();
        ddThreadPool.execute(new Runnable() {
          @Override
          public void run() {
            try {
              chainToStartService.process(request);
            } catch (Exception e) {
              logger
                  .error("Caught an exception when start service {}", service.getServiceName(), e);
            }
          }
        });

        break;
      case DEACTIVATING:
        if (isServiceDead) {
          logger.info("Service {} is dead", service.getServiceName());
          serviceMetadata.setServiceStatus(ServiceStatus.DEACTIVE);
          serviceStore.save(serviceMetadata);
          return;
        }
        final DeactivateRequest deactivateRequest = new DeactivateRequest();
        deactivateRequest.setServiceName(service.getServiceName());

        final DeploymentDaemonProcessor chainToDeactivateService = processorChainFactory
            .createServiceDeactivationChain();
        ddThreadPool.execute(new Runnable() {
          @Override
          public void run() {
            try {
              chainToDeactivateService.process(deactivateRequest);
            } catch (Exception e) {
              logger
                  .error("Caught an exception when deactivate service {}", service.getServiceName(),
                      e);
            }
          }
        });

        break;
      case DEACTIVE:
        logger.info("Service {} is {}, do nothing.", serviceMetadata.getServiceName(),
            serviceMetadata.getServiceStatus());
        break;
      default:
        break;
    }
  }

  public DeploymentDaemonConfiguration getDdConfig() {
    return ddConfig;
  }

  public void setDdConfig(DeploymentDaemonConfiguration ddConfig) {
    this.ddConfig = ddConfig;
  }

  public ServiceMonitor getSysMonWrapper() {
    return sysMonWrapper;
  }

  public void setSysMonWrapper(ServiceMonitor sysMonWrapper) {
    this.sysMonWrapper = sysMonWrapper;
  }

  public ServiceStore getServiceStore() {
    return serviceStore;
  }

  public void setServiceStore(ServiceStore serviceStore) {
    this.serviceStore = serviceStore;
  }

  public ProcessorChainFactory getProcessorChainFactory() {
    return processorChainFactory;
  }

  public void setProcessorChainFactory(ProcessorChainFactory processorChainFactory) {
    this.processorChainFactory = processorChainFactory;
  }

  public ExecutorService getDdThreadPool() {
    return ddThreadPool;
  }

  public void setDdThreadPool(ExecutorService ddThreadPool) {
    this.ddThreadPool = ddThreadPool;
  }
}
