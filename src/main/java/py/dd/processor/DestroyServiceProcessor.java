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

package py.dd.processor;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.PyService;
import py.dd.utils.DdUtils;
import py.driver.DriverMetadata;
import py.driver.DriverType;
import py.drivercontainer.utils.DriverContainerUtils;
import py.processmanager.Pmdb;
import py.thrift.deploymentdaemon.ActivateRequest;
import py.thrift.deploymentdaemon.ChangeConfigurationRequest;
import py.thrift.deploymentdaemon.DeactivateRequest;
import py.thrift.deploymentdaemon.DestroyRequest;
import py.thrift.deploymentdaemon.DriverIsAliveExceptionThrift;
import py.thrift.deploymentdaemon.PutTarRequest;
import py.thrift.deploymentdaemon.RestartRequest;
import py.thrift.deploymentdaemon.StartRequest;
import py.thrift.deploymentdaemon.WipeoutRequest;

/**
 * A class as processor to destroy specified service.
 */
public class DestroyServiceProcessor extends DeploymentDaemonProcessor {

  public static final String FILE_SEPARATOR = File.separator;
  private static final Logger logger = LoggerFactory.getLogger(DestroyServiceProcessor.class);

  @Override
  public void process(DestroyRequest request) throws Exception {
    service = PyService.findValueByServiceName(request.getServiceName());
    //coordinator and fsserver just provide pacakge ,has no process ,so should not stop process
    if (service != PyService.COORDINATOR) {
      process();
    }
    if (nextProcessor != null) {
      nextProcessor.process(request);
    }
  }

  @Override
  public void process(DeactivateRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process(StartRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process(RestartRequest request) throws Exception {
    throw new NotImplementedException();
  }

  /**
   * If process of driver is alive ,can not wipeout Coordinator or Fsserver.
   */
  @Override
  public void process(WipeoutRequest request) throws Exception {
    logger.debug("WipeoutRequest {}", request);
    service = PyService.findValueByServiceName(request.getServiceName());
    if (service == PyService.COORDINATOR) {

      if (isCoordinatorProcessAlive()) {
        logger.error("Driver is alive exception");
        throw new DriverIsAliveExceptionThrift();
      }
    }
    if (service != PyService.COORDINATOR) {
      process();
    }
    nextProcessor.process(request);
  }

  @Override
  public void process(ChangeConfigurationRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process(ActivateRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process(PutTarRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process() throws Exception {
    List<File> activationScripts = DdUtils
        .listActivationScripts(ddConfig.buildServicesScriptsPath(service));
    if (!activationScripts.isEmpty()) {
      for (File activationScript : activationScripts) {
        logger.debug(
            "Check if exists process of activation script {} size {}. If it does,"
                + " kill the process.",
            activationScript.getName(), activationScript.length());
        int asPid = systemMonitor.getActivationScriptPid(service, activationScript.getName());
        systemMonitor.killProcess(asPid);
      }
    } else {
      logger.warn("activation scripts {} is empty", activationScripts);
    }

    logger.debug("Check if exists process of pm for service {}. If it does, kill it.",
        service.getServiceName());
    int pmPid;
    try {
      pmPid = systemMonitor.getPmPid(service);
      systemMonitor.killProcess(pmPid);
    } catch (Exception e) {
      logger.error("caught an exception when try get PMPid at:{}", service.getServiceName(), e);
    }

    logger.debug("Check if exists process of script launcher for service {}. If it does, kill it.",
        service.getServiceName());
    int scriptPid = systemMonitor.getServiceLauncherPid(service);
    systemMonitor.killProcess(scriptPid);

    logger.debug("Check if exists process of service {}. If it does, kill it.",
        service.getServiceName());

    List<Integer> pidsList = new ArrayList<>();
    pidsList = systemMonitor.getProcessIds(service);
    for (Integer pid : pidsList) {
      systemMonitor.killProcess(pid);
    }
  }

  /**
   * Get all driver to save in a list from SPid_coordinator.
   */
  public List<DriverMetadata> getAllDriver() {
    List<DriverMetadata> driverList = new ArrayList<DriverMetadata>();
    String serviceRunningPath = ddConfig.buildServiceRunningPath(PyService.DRIVERCONTAINER)
        .toString();
    serviceRunningPath += (FILE_SEPARATOR + "var");
    File runningFile = new File(serviceRunningPath);
    if (!runningFile.exists()) {
      logger.warn("No such directory {}", runningFile);
      return driverList;
    }
    File[] driversPackages = runningFile.listFiles();
    for (File driversPackage : driversPackages) {
      if (driversPackage.isDirectory()) {
        String driversBackupPath = Paths
            .get(serviceRunningPath, driversPackage.getName(), Pmdb.COORDINATOR_PIDS_DIR_NAME)
            .toString();
        File driverBackupDir = new File(driversBackupPath);
        if (!driverBackupDir.exists()) {
          logger.warn("No such directory {}", driversBackupPath);
          return driverList;
        }
        File[] volumeIds = driverBackupDir.listFiles();
        for (File volumeId : volumeIds) {
          File[] driverTypeFiles = volumeId.listFiles();
          for (File driverTypeFile : driverTypeFiles) {
            File[] driverBackupFiles = driverTypeFile.listFiles();
            if (driverBackupFiles == null || driverBackupFiles.length == 0) {
              logger.info("nothing in {}", volumeId);
              continue;
            }
            for (File driverBackUp : driverBackupFiles) {
              DriverMetadata driver = DriverMetadata.buildFromFile(driverBackUp.toPath());
              if (driver == null) {
                logger.error("Failed to build driver metadata from {}",
                    driverBackUp.getAbsolutePath());
                continue;
              } else {
                driverList.add(driver);
                logger.warn("driverList:{}", driverList);
              }
            }
          }
        }
      }
    }
    return driverList;
  }

  /**
   * xx.
   */
  public boolean isCoordinatorProcessAlive() {
    boolean flag = false;
    List<DriverMetadata> driverList = getAllDriver();
    for (DriverMetadata driver : driverList) {
      if (driver.getDriverType() == DriverType.NBD || driver.getDriverType() == DriverType.ISCSI) {
        if (DriverContainerUtils.processExist(driver.getProcessId())) {
          flag = true;
        }
      }
    }

    return flag;
  }

  /**
   * xx.
   */
  public boolean isFsserverProcessAlive() {
    boolean flag = false;
    List<DriverMetadata> driverList = getAllDriver();
    for (DriverMetadata driver : driverList) {
      if (driver.getDriverType() == DriverType.FSD) {
        if (DriverContainerUtils.processExist(driver.getProcessId())) {
          flag = true;
        }
      }
    }

    return flag;
  }

}
