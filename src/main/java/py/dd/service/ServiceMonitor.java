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

package py.dd.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.PyService;
import py.dd.DeploymentDaemonConfiguration;
import py.dd.utils.DdUtils;
import py.system.monitor.LinuxMonitor;
import py.system.monitor.ProcessInfo;

/**
 * After service start up, its process id saved into file. DD check if the pid in file is correctly
 * equal to pid saved in linux directory ''/proc'' to make sure the state of service. This class
 * wraps some common used function to manage service process.
 */
public class ServiceMonitor {

  private static final Logger logger = LoggerFactory.getLogger(ServiceMonitor.class);

  private LinuxMonitor systemMonitor;

  private DeploymentDaemonConfiguration ddConfig;

  private AtomicBoolean startingDatanode;

  public ServiceMonitor(LinuxMonitor systemMonitor) {
    this.systemMonitor = systemMonitor;
    this.startingDatanode = new AtomicBoolean(false);
  }

  public boolean markDatanodeStarting() {
    return this.startingDatanode.compareAndSet(false, true);
  }

  public boolean isDatanodeStarting() {
    return this.startingDatanode.get();
  }

  public void doneDatanodeStarting() {
    this.startingDatanode.set(false);
  }

  /**
   * Service running path is missing for some reason, but the relative service process is running.
   * The service process is orphan. We unable to manage it anymore. In the case, to kill the orphan
   * process is necessary to provide more opportunity to start other services.
   */
  public void clearOrphanServiceProcess(List<PyService> adoptedServiceList) {
    if (adoptedServiceList == null) {
      adoptedServiceList = new ArrayList<PyService>();
    }

    for (PyService service : PyService.values()) {

      if (adoptedServiceList.contains(service) || service == PyService.DEPLOYMENTDAMON) {
        continue;
      }

      int pmPid = 0;
      try {
        pmPid = getPmPid(service);
      } catch (Exception e) {
        logger.error("caught an exception when try get PMPid at:{}", service.getServiceName(), e);
      }
      int pid = getProcessId(service);

      if (pmPid != 0) {
        logger.warn("Kill process manager for orphan service process, name: {}, pid: {}",
            service.getServiceName(), pmPid);
        systemMonitor.killProcess(pmPid);
      }

      if (pid != 0) {
        logger
            .warn("Kill orphan service process, name: {}, pid: {}", service.getServiceName(), pid);
        systemMonitor.killProcess(pid);
      }
    }
  }

  /**
   * After running script to launch service, a process is assigned to the script. This method check
   * the process id from the service command if the same with given pid
   *
   * @return 0:no such process for the given service
   */
  public int getProcessId(PyService service, int pid) {
    ProcessInfo processInfo = systemMonitor.processInfo(pid);
    if (processInfo != null) {
      String processCommand = processInfo.getCommand();
      if (processCommand.contains(service.getServiceMainClassName())
          && processCommand.contains(service.getServiceProjectKeyName())) {
        return processInfo.getPid();
      }
    }
    return 0;
  }

  /**
   * After running script to launch service, a process is assigned to the script. This method get
   * the process id from the service command.
   *
   * @return 0: no such process for the given service
   */
  public int getProcessId(PyService service) {
    for (ProcessInfo processInfo : systemMonitor.processTable()) {
      String processCommand = processInfo.getCommand();
      if (processCommand.contains(service.getServiceMainClassName())
          && processCommand.contains(service.getServiceProjectKeyName())) {
        return processInfo.getPid();
      }
    }

    return 0;
  }

  /**
   * xx.
   */
  public List<Integer> getProcessIds(PyService service) {
    List<Integer> list = new ArrayList<>();
    for (ProcessInfo processInfo : systemMonitor.processTable()) {
      String processCommand = processInfo.getCommand();
      if (processCommand.contains(service.getServiceMainClassName())
          && processCommand.contains(service.getServiceProjectKeyName())) {
        list.add(processInfo.getPid());
      }
    }
    return list;
  }

  /**
   * For every service that is alive, a process manager is alive to guard the service process. In
   * fact, the pm process is parent of the service process due to each service is started up by the
   * pm. This method to check process manager process id with given pid
   *
   * @return 0: no such process for pm
   */
  public int getPmPid(PyService service, int pid) throws Exception {
    int pmPid = 0;
    try {
      pmPid = DdUtils.getPmPidFromCommandByServiceName(service, ddConfig.getJpsCommandPath());
    } catch (Exception e) {
      logger.error("caught an exception when get pmPid by:{}", service.getServiceName(), e);
      throw e;
    }
    return pmPid;
  }

  /**
   * For every service that is alive, a process manager is alive to guard the service process. In
   * fact, the pm process is parent of the service process due to each service is started up by the
   * pm. This method get process manager process id.
   *
   * @return 0: no such process for pm
   */
  public int getPmPid(PyService service) throws Exception {
    return getPmPid(service, 0);
  }

  /**
   * Before launching some service, multiple activation scripts is necessary to run to initialize
   * service running environment. Of course a process exists for the scripts. This method is used to
   * get the script process id if the process exist.
   *
   * @return 0: no such process for activation script
   */
  public int getActivationScriptPid(PyService service, String activationScriptName) {
    for (ProcessInfo processInfo : systemMonitor.processTable()) {
      String processCommand = processInfo.getCommand();
      if (processCommand.contains(activationScriptName)
          && processCommand.contains(ddConfig.buildServiceRunningPath(service).toString())) {
        return processInfo.getPid();
      }
    }

    return 0;
  }

  /**
   * For each service, there is a script to launch it. Always the script process is parent of the
   * service process, but sometimes is not. This method get pid of the script.
   *
   * @return 0: no such process for activation script
   */
  public int getServiceLauncherPid(PyService service) {
    for (ProcessInfo processInfo : systemMonitor.processTable()) {
      String processCommand = processInfo.getCommand();
      if (processCommand.contains(service.getServiceLauchingScriptName())
          && processCommand.contains(ddConfig.buildServiceRunningPath(service).toString())) {
        return processInfo.getPid();
      }
    }

    return 0;
  }

  /**
   * Kill process with a given pid.
   */
  public void killProcess(int pid) {
    if (pid == 0) {
      return;
    }

    logger.info("Kill process {}", pid);
    systemMonitor.killProcess(pid);
  }

  public LinuxMonitor getSystemMonitor() {
    return systemMonitor;
  }

  public void setSystemMonitor(LinuxMonitor systemMonitor) {
    this.systemMonitor = systemMonitor;
  }

  public DeploymentDaemonConfiguration getDdConfig() {
    return ddConfig;
  }

  public void setDdConfig(DeploymentDaemonConfiguration ddConfig) {
    this.ddConfig = ddConfig;
  }
}
