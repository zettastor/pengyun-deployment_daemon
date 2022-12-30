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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import py.common.PyService;
import py.dd.DeploymentDaemonConfigBean;
import py.dd.DeploymentDaemonConfiguration;
import py.dd.utils.DdUtils;
import py.processmanager.Pmdb;
import py.processmanager.ProcessManager;
import py.processmanager.ProcessManagerMutex;
import py.processmanager.exception.UnableToStartServiceException;
import py.processmanager.utils.PmUtils;

/**
 * xx.
 */
public class IndependentProcessManager extends ProcessManager {

  public static final String PROCESS_MANAGER_WATCH_SERVICE_NAME =
      "-DProcessManagerWatchServiceName=";
  private static final Logger logger = LoggerFactory.getLogger(IndependentProcessManager.class);
  private PyService pyService;

  /**
   * xx.
   */
  public static PyService matchAndGetPyServiceFromOutput(String line) {
    Validate.notNull(line);
    String[] stringArrayBySpace = line.split(" ");
    Validate.isTrue(stringArrayBySpace.length == 2);
    PyService pyService = matchAndGetPyServiceFromTargetString(stringArrayBySpace[1]);
    return pyService;
  }

  /**
   * xx.
   */
  public static int matchAndGetPmPidFromOutput(String line, PyService pyService) {
    Validate.notNull(line);
    Validate.notNull(pyService);
    PyService matchPyService = matchAndGetPyServiceFromOutput(line);
    if (matchPyService != null && matchPyService.equals(pyService)) {
      String[] stringArrayBySpace = line.split(" ");
      Validate.isTrue(stringArrayBySpace.length == 2);
      int pmPid = Integer.valueOf(stringArrayBySpace[0]);
      return pmPid;
    }
    return 0;
  }

  /**
   * xx.
   */
  public static PyService matchAndGetPyServiceFromTargetString(String targetString) {
    Validate.notNull(targetString);
    Path path = Paths.get(targetString);
    PyService pyService = PyService.findValueByPath(path);
    return pyService;
  }

  /**
   * main method for process manager, service command is required to start process manager.
   */
  public static void main(String[] args) throws IOException {

    if (args == null || args.length == 0) {
      logger.error("one argument containing service command is required, but cannot get it");
      System.exit(-1);
    }

    IndependentProcessManager processManager = new IndependentProcessManager();

    String launcherPath = args[0];
    processManager.setLauncherPath(launcherPath); // set script path
    logger.info(launcherPath);
    for (int i = 2; i < args.length; i++) {
      String arg = args[i];

      if (ProcessManager.KW_DISABLE.equals(arg.toLowerCase())) {
        processManager.setDisabled(true);
        continue;
      }

      processManager.addParams(arg);
    }

    try {
      String mutexTargetDir = args[1];
      if (ProcessManagerMutex.checkIfAlreadyRunning(mutexTargetDir)) {
        logger.warn("going to start PM");
        processManager.startService();
      } else {
        logger.warn("exit due to the same process is processing ");
        System.exit(1);
      }
    } catch (Throwable e) {
      logger.error("Caught an exception", e);
      System.exit(1);
    }

  }

  /**
   * this method proguard service running in specified path, always start it as long as the service
   * is dead.
   */
  @Override
  public void startService() throws UnableToStartServiceException {
    if (getLauncherPath() == null) {
      logger.error("unable to start service due to launcherPath is not specified");
      throw new UnableToStartServiceException();
    }

    File launcher = new File(getLauncherPath());
    if (!launcher.exists()) {
      logger.error("unable to start service due to launcher path doesn't exist");
      throw new UnableToStartServiceException();
    }

    String serviceRunningPath = launcher.getParentFile().getParentFile().getAbsolutePath();
    logger.warn("going to start service:{}", serviceRunningPath);

    PyService getPyService = PyService.findValueByPath(Paths.get(serviceRunningPath));
    if (getPyService != null) {
      pyService = getPyService;
    }
    Validate.notNull(pyService);
    logger.info("finger out service:{} by params", pyService);

    /*
     * backup my process id into a file to let system know me
     */
    boolean alreadyBackupPid = false;
    try {
      Pmdb pmdb = Pmdb.build(Paths.get(serviceRunningPath));
      String backupStr = String.valueOf(PmUtils.getCurrentProcessPid());
      alreadyBackupPid = pmdb.save(Pmdb.PM_PID_NAME, backupStr);
    } catch (Exception e) {
      logger.error("Caught an exception", e);
    }
    if (!alreadyBackupPid) {
      logger.error("cannot backup my process manager pid into a file");
      throw new UnableToStartServiceException();
    }

    // append parameters to launch command
    StringBuilder commandBuilder = new StringBuilder();
    commandBuilder.append(getLauncherPath());
    if (getLauncherParams().size() > 0) {
      for (String launcherParam : getLauncherParams()) {
        commandBuilder.append(" " + launcherParam);
      }
    }

    String command = commandBuilder.toString();
    logger.info("process manager build command:{}", command);

    ServiceMonitor serviceMonitor = null;
    DeploymentDaemonConfiguration ddConfig = null;
    try {
      logger.info("process manager get monitor and config before");
      ApplicationContext context = new AnnotationConfigApplicationContext(
          DeploymentDaemonConfigBean.class);
      ddConfig = context.getBean(DeploymentDaemonConfiguration.class);
      serviceMonitor = context.getBean(ServiceMonitor.class);
      logger.info("process manager get monitor and config after");
    } catch (Throwable t) {
      logger.error("caught an exception", t);
    }

    Validate.notNull(serviceMonitor, "system monitor can not be null");
    Validate.notNull(ddConfig, "ddconfig can not be null");

    while (true) {
      try {
        logger.info("process manager check service is runnable");
        if (!DdUtils.isRunnable(pyService)) {
          logger
              .error("process manager going to watch Service:{}, but it's not runnable", pyService);
          Thread.sleep(5000);
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
        int pid = serviceMonitor.getProcessId(pyService, pidFromFile);

        boolean isServiceAlive = (pid != 0) && (pid == pidFromFile);
        logger.warn("get pidFromFile:{}, pid from system and file:{}, judge service alive:{}",
            pidFromFile, pid, isServiceAlive);
        if (!isServiceAlive) {
          logger.warn("execute cmd:{} to start service", command);
          Process process = Runtime.getRuntime().exec(command, null, new File(serviceRunningPath));
          process.waitFor();
        } else {
          logger.info("sleep for next check, service:{}", pyService);
          Thread.sleep(5000);
        }
        if (isDisabled()) {
          logger.warn("Disabled flag detected, process manager exit");
          break;
        }
      } catch (Throwable e) {
        logger.error("Caught an exception when process manager start service", e);
      }
    }
  }
}
