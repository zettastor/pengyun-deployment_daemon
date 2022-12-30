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
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.OsCmdExecutor;
import py.common.PyService;
import py.dd.utils.DdUtils;
import py.driver.DriverMetadata;
import py.processmanager.Pmdb;
import py.thrift.deploymentdaemon.ActivateRequest;
import py.thrift.deploymentdaemon.ChangeConfigurationRequest;
import py.thrift.deploymentdaemon.DeactivateRequest;
import py.thrift.deploymentdaemon.DestroyRequest;
import py.thrift.deploymentdaemon.PutTarRequest;
import py.thrift.deploymentdaemon.RestartRequest;
import py.thrift.deploymentdaemon.StartRequest;
import py.thrift.deploymentdaemon.WipeoutRequest;

/**
 * Kill all launched drivers process which launched by service driver container, process info of
 * drivers stored in a file different to other services, it is not possible to deal with drivers'
 * process as them, this class deal with killing drivers.
 */
public class DestroyDriversProcesser extends DeploymentDaemonProcessor {

  public static final String FILE_SEPARATOR = File.separator;
  private static final Logger logger = LoggerFactory.getLogger(DestroyDriversProcesser.class);
  private String driversBackupPath;

  @Override
  public void process(DestroyRequest request) throws Exception {
    service = PyService.findValueByServiceName(request.getServiceName());
    if (service == PyService.DRIVERCONTAINER) {
      process();
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

  @Override
  public void process(WipeoutRequest request) throws Exception {
    logger.debug("process {}", request);
    service = PyService.findValueByServiceName(request.getServiceName());
    if (service == PyService.DRIVERCONTAINER) {
      process();
    }

    nextProcessor.process(request);
  }

  @Override
  public void process(ActivateRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process(ChangeConfigurationRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process(PutTarRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process() throws Exception {
    String serviceRunningPath = ddConfig.buildServiceRunningPath(PyService.DRIVERCONTAINER)
        .toString();
    serviceRunningPath += (FILE_SEPARATOR + "var");
    //Scan all driverPath in drivercontainer running path ,
    // and stop driver process ,unbind pyd-client for every driver.
    File runningFile = new File(serviceRunningPath);
    if (!runningFile.exists()) {
      logger.info("No such directory {}", runningFile);
      return;
    }
    File[] driversPackages = runningFile.listFiles();
    for (File driversPackage : driversPackages) {
      if (driversPackage.isDirectory()) {
        driversBackupPath = Paths
            .get(serviceRunningPath, driversPackage.getName(), Pmdb.COORDINATOR_PIDS_DIR_NAME)
            .toString();
        logger.debug("Drivers' info stored in directory {}", driversBackupPath);
        File driverBackupDir = new File(driversBackupPath);
        if (!driverBackupDir.exists()) {
          logger.info("No such directory {}", driversBackupPath);
          return;
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
              }
              logger.info("Destroy driver {}", driver);
              systemMonitor.killProcess(driver.getProcessId());
              String format = String
                  .format(ddConfig.getIetUnbindCmd(), driver.getNbdDevice());
              OsCmdExecutor.OsCmdOutputLogger stdoutLogger = new OsCmdExecutor.OsCmdOutputLogger(
                  logger, format);
              OsCmdExecutor.OsCmdOutputLogger stderrLogger = new OsCmdExecutor.OsCmdOutputLogger(
                  logger, format);
              logger.debug("Stop nbdclient {}", format);
              OsCmdExecutor
                  .exec(format, (String[]) null, (File) null, stdoutLogger, stderrLogger);
            }
          }

          Path iscsiCtlScriptPath = Paths.get(ddConfig.getIscsiCtlScript());
          if (iscsiCtlScriptPath.toFile().exists()) {
            logger.debug("Stop iscsi service by script {}", iscsiCtlScriptPath);
            Runtime.getRuntime().exec(ddConfig.buildIscsiStopCmd());
          }
          //stop lio service command is /usr/sbin/service target stop
          String stopLioServiceCommand = ddConfig.getStopLioserviceCommand();
          try {
            OsCmdExecutor.OsCmdOutputLogger consumer = new OsCmdExecutor.OsCmdOutputLogger(logger,
                stopLioServiceCommand);
            int existCode = OsCmdExecutor
                .exec(stopLioServiceCommand, DdUtils.osCMDThreadPool, consumer, consumer);
            if (existCode != 0) {
              logger.error("Catch an exception exec stop command :{} stop lio service",
                  stopLioServiceCommand);
            }
          } catch (IOException e) {
            logger.warn("{},no such file or directory", stopLioServiceCommand);
          }
        }
      }
    }
  }
}
