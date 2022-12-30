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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.PyService;
import py.dd.exception.UnableToLinkException;
import py.dd.utils.DdConstants;
import py.dd.utils.DdUtils;
import py.driver.DriverType;
import py.drivercontainer.driver.version.Version;
import py.drivercontainer.driver.version.VersionException;
import py.drivercontainer.driver.version.VersionManager;
import py.drivercontainer.driver.version.file.VersionImpl;
import py.drivercontainer.driver.version.file.VersionManagerImpl;
import py.thrift.deploymentdaemon.ActivateRequest;
import py.thrift.deploymentdaemon.ChangeConfigurationRequest;
import py.thrift.deploymentdaemon.DeactivateRequest;
import py.thrift.deploymentdaemon.DestroyRequest;
import py.thrift.deploymentdaemon.DriverUpgradeExceptionThrift;
import py.thrift.deploymentdaemon.PrepareWorkspaceRequest;
import py.thrift.deploymentdaemon.PutTarRequest;
import py.thrift.deploymentdaemon.RestartRequest;
import py.thrift.deploymentdaemon.StartRequest;
import py.thrift.deploymentdaemon.WipeoutRequest;

/**
 * link service src from service instance path to service running path.
 */
public class LinkProcessor extends DeploymentDaemonProcessor {

  private static final Logger logger = LoggerFactory.getLogger(LinkProcessor.class);

  private Path linkFrom;

  private Path linkTo;

  @Override
  public void process(PutTarRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process(ActivateRequest request) throws Exception {
    logger.warn("active request:{} process", request);

    service = PyService.findValueByServiceName(request.getServiceName());

    if (service == PyService.COORDINATOR) {
      ddConfig.setCoordinatorTimestamp(request.getCoorTimestamp());
    }

    linkFrom = ddConfig.buildServiceInstallationPath(service, request.getServiceVersion());
    linkTo = ddConfig.buildServiceRunningPath(service);
    process();

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

  @Override
  public void process(DestroyRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process(WipeoutRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process(ChangeConfigurationRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process() throws Exception {
    logger.warn("Link all sub-entries under:{} to target directory:{}", linkFrom, linkTo);

    // delete older symbol linked item in running path
    File file = linkTo.toFile();
    if (file.exists()) {
      if (!deleteServiceRunningPath(file)) {
        logger.error("fail to delete old service running path");
        throw new UnableToLinkException("fail to delete old service running path");
      }
    } else {
      Files.createDirectories(linkTo);
    }

    File[] subFiles = linkFrom.toFile().listFiles();
    if (subFiles == null || subFiles.length == 0) {
      logger.warn("Link source:{} is empty!", linkFrom);
      throw new UnableToLinkException("failed link due to instance path is empty");
    }

    /*
     * Link each source file to target directory.
     */
    for (int i = 0; i < subFiles.length; i++) {
      File subFile = subFiles[i];
      if (subFile.getName().equals(DdConstants.LOG_DIR_NAME)
          || subFile.getName().equals(DdConstants.VAR_DIR_NAME)) {
        continue;
      }

      String targetPath = new File(linkTo.toFile(), subFile.getName()).getPath();
      String srcPath = subFile.getAbsolutePath();

      try {
        logger.warn("create symbolic link from src file:{} to target file:{}", srcPath, targetPath);
        Files.createSymbolicLink(Paths.get(targetPath), Paths.get(srcPath));
      } catch (IOException e) {
        logger.error("Caught an exception when create symbolic link from {} to {}", srcPath,
            targetPath, e);
        throw new UnableToLinkException("fail to create symbolic link");
      }
    }

    DdUtils.syncFs();
  }

  @Override
  public void process(PrepareWorkspaceRequest request) throws Exception {
    logger.warn("prepare workspace request:{} process", request);

    service = PyService.findValueByServiceName(request.getServiceName());

    if (service == PyService.COORDINATOR) {
      ddConfig.setCoordinatorTimestamp(request.getCoorTimestamp());
    }
    linkFrom = ddConfig.buildServiceInstallationPath(service, request.getServiceVersion());
    linkTo = ddConfig.buildServiceRunningPath(service);
    process();

    if (nextProcessor != null) {
      nextProcessor.process(request);
    }

    // set the latest version after prepareWorkspace, because running path must
    // be ok before driver upgrade
    // update version info in the latest file for driver upgrade
    if (service == PyService.COORDINATOR) {
      setDriverLatestVersion(request.getServiceVersion(), DriverType.NBD,
          request.getCoorTimestamp());
    }
  }

  /*
   * when coordinator deploy after unTar to _packages
   * check migrate/the latest files which contains ver/timestamp
   * if latest not exits create it
   * latest file contains latest coordinator timestamp
   * migrate file contains coordinator status
   * Use Version library in driver-core
   *
   */
  void setDriverLatestVersion(String serviceVersion, DriverType drivertype, String timestamp)
      throws Exception {
    logger.warn("updateDriverLatestVersion serviceVersion:{}, timestamp:{}", serviceVersion,
        timestamp);

    String serviceRunningPath = ddConfig.buildServiceRunningPath(PyService.DRIVERCONTAINER)
        .toString();
    VersionManager versionManager = new VersionManagerImpl(serviceRunningPath + "/var");
    try {
      versionManager.lockVersion(drivertype);
      boolean isMigrating = versionManager.isOnMigration(drivertype);

      if (isMigrating) {
        logger.error("Driver upgrade migrating, try again later");
        throw new DriverUpgradeExceptionThrift("status upgrading");
      } else {
        //write version to latest
        Version version = VersionImpl.get(serviceVersion + '-' + timestamp);
        versionManager.setLatestVersion(drivertype, version);
        logger.debug("setLatestVersion, driver type:{}, version:{}", drivertype, version);
      }
    } catch (VersionException e) {
      logger.error("Driver upgrade VersionException", e);
      throw new DriverUpgradeExceptionThrift("Driver upgrade VersionException");
    } catch (Exception e) {
      logger.error("Driver upgrade exception", e);
      throw new DriverUpgradeExceptionThrift("Driver upgrade exception");
    } finally {
      versionManager.unlockVersion(drivertype);
    }
  }

  /**
   * when delete sub files, it needs to distinguish symbolic link and normal file.
   *
   */
  private boolean deleteServiceRunningPath(File fileToDelete) {
    logger.warn("Clearing all old symbolic sub-entries under:{}", fileToDelete);

    File[] subFilesToDelete = fileToDelete.listFiles();

    if (subFilesToDelete != null) {
      for (int i = 0; i < subFilesToDelete.length; i++) {
        Path subFileAbsolutePath = Paths.get(subFilesToDelete[i].getAbsolutePath());

        // only delete symbolic linked files in running path
        if (Files.isSymbolicLink(subFileAbsolutePath)) {
          try {
            logger.warn("delete file:{}", subFileAbsolutePath);
            Files.delete(subFileAbsolutePath);
          } catch (IOException e) {
            logger.error("Caught an exception when delete symbolic linked file {}",
                subFileAbsolutePath,
                e);
            return false;
          }
        }
      }
    }

    return true;
  }
}
