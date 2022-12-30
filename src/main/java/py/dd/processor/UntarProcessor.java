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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.PyService;
import py.common.Utils;
import py.dd.common.ServiceMetadata;
import py.dd.utils.DdUtils;
import py.thrift.deploymentdaemon.ActivateRequest;
import py.thrift.deploymentdaemon.ChangeConfigurationRequest;
import py.thrift.deploymentdaemon.DeactivateRequest;
import py.thrift.deploymentdaemon.DestroyRequest;
import py.thrift.deploymentdaemon.PrepareWorkspaceRequest;
import py.thrift.deploymentdaemon.PutTarRequest;
import py.thrift.deploymentdaemon.RestartRequest;
import py.thrift.deploymentdaemon.StartRequest;
import py.thrift.deploymentdaemon.WipeoutRequest;

/**
 * processor that untar the *tar.gz file to the service install path
 *
 */
public class UntarProcessor extends DeploymentDaemonProcessor {

  private static final Logger logger = LoggerFactory.getLogger(UntarProcessor.class);

  private Path targetPackagePath;

  private Path untarTo;

  // make public for unit tests

  /**
   * xx.
   */
  public static String generateDeleteVersionFile(String absoluteFile, String timestamp) {
    Validate.notNull(absoluteFile);
    logger.warn("going to delete version from file name:{}", absoluteFile);
    int lastSlashIndex = absoluteFile.lastIndexOf("/");
    final String absoluteDir = absoluteFile.substring(0, lastSlashIndex + 1);
    String fileNameWithVersion = absoluteFile.substring(lastSlashIndex + 1, absoluteFile.length());

    String splitString = "-";
    String[] splitArray = fileNameWithVersion.split("-");
    Validate.notNull(splitArray);
    Validate.notEmpty(splitArray);
    StringBuilder stringBuilder = new StringBuilder();
    for (String tmpString : splitArray) {
      if (tmpString.contains(".")) {
        logger.warn("find version:{}", tmpString);
        break;
      } else {
        stringBuilder.append(tmpString);
        stringBuilder.append(splitString);
      }
    }
    stringBuilder.delete(stringBuilder.lastIndexOf(splitString), stringBuilder.length());
    if (stringBuilder.toString().contains(PyService.COORDINATOR.getServiceProjectKeyName())) {
      stringBuilder.append(splitString);
      stringBuilder.append(timestamp);
    }
    String deleteVersionFile = absoluteDir + stringBuilder.toString();
    Validate.notNull(deleteVersionFile);
    return deleteVersionFile;
  }

  @Override
  public void process(ActivateRequest request) throws Exception {
    process(request.getServiceName(), request.getServiceVersion());
    nextProcessor.process(request);
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
    logger.warn("process {}", request);
    if (!request.isPreserve()) {
      service = PyService.findValueByServiceName(request.getServiceName());
      targetPackagePath = ddConfig.buildServicePackagePath(service, request.getServiceVersion());
      untarTo = Paths
          .get(ddConfig.buildTmpInstallationPath(service, request.getServiceVersion()).toString());
      process();
    }
    nextProcessor.process(request);
  }

  /**
   * xx.
   */
  public void process(PrepareWorkspaceRequest request) throws Exception {

    logger.warn("process {}", request);

    PyService service = PyService.findValueByName(request.getServiceName());

    if (service == PyService.COORDINATOR) {
      ddConfig.setCoordinatorTimestamp(request.getCoorTimestamp());
      process(request.getServiceName(), request.getServiceVersion());
    } else {
      process(request.getServiceName(), request.getServiceVersion());
    }
    nextProcessor.process(request);
  }

  @Override
  public void process(PutTarRequest request) throws Exception {
  }

  /*
   * PrepareWorkspaceRequest process
   * driver upgrade need timestamp
   */
  void process(String serviceName, String serviceVersion) throws Exception {
    service = PyService.findValueByServiceName(serviceName);
    targetPackagePath = ddConfig.buildServicePackagePath(service, serviceVersion);
    untarTo = Paths.get(ddConfig.getServicesInstallationPath());
    logger.warn("Untar process for service:{}, untarToPath:{}", serviceName, untarTo);

    process();

  }

  @Override
  public void process() throws Exception {
    logger
        .warn("Extracting archive file named {} to {} for {}", targetPackagePath, untarTo, service);

    File untarToDir;
    ServiceMetadata serviceInfo;

    untarToDir = untarTo.toFile();
    if (!untarToDir.exists() && !untarToDir.mkdirs()) {
      String errMsg = "No such directory: " + untarToDir.getAbsolutePath();
      logger.error("{}", errMsg);
      throw new IllegalStateException(errMsg);
    } else if (untarToDir.exists() && !untarToDir.isDirectory()) {
      String errMsg = "Not a directory: " + untarToDir.getAbsolutePath();
      logger.error("{}", errMsg);
      throw new IllegalStateException(errMsg);
    }

    serviceInfo = serviceStore.get(service);
    File[] subEntries = untarToDir.listFiles();
    logger.warn("going to delete exist directories from:{} for service:{}", untarToDir,
        service.getServiceName());
    if (serviceInfo == null) {
      logger.warn(
          "There is no such service of {} in store,"
              + " maybe this is in process of preparation for service.",
          service.getServiceName());

      for (File subEntry : subEntries) {
        if (subEntry.getName().contains(service.getServiceProjectKeyName())) {
          logger.warn("delete directory:{} for service:{}", subEntry.getAbsoluteFile(),
              service.getServiceName());
          Utils.deleteDirectory(subEntry.getAbsoluteFile());
        }
      }
    } else {
      String requestVersion = serviceInfo.getVersion();
      String internalVersion = "-internal";
      String releaseVersion = "-release";
      if (requestVersion.contains(internalVersion)) {
        requestVersion = requestVersion.substring(0, requestVersion.lastIndexOf(internalVersion));
      }
      if (requestVersion.contains(releaseVersion)) {
        requestVersion = requestVersion.substring(0, requestVersion.lastIndexOf(releaseVersion));
      }
      logger.warn("get request version:{}", requestVersion);
      // String tarPrefix = String.format("%s-%s",
      // service.getServiceProjectKeyName(), requestVersion);

      for (File subEntry : subEntries) {
        logger.warn("subEntry name:{}, service project name:{}", subEntry.getName(),
            service.getServiceProjectKeyName());
        if (subEntry.getName().contains(service.getServiceProjectKeyName())) {
          logger.warn("delete directory:{} for service:{}", subEntry.getAbsoluteFile(),
              service.getServiceName());
          Utils.deleteDirectory(subEntry.getAbsoluteFile());
        }
      }
    }

    subEntries = untarToDir.listFiles();
    // check if delete clean
    for (File subEntry : subEntries) {
      logger.warn("subEntry name:{}, service project name:{}", subEntry.getName(),
          service.getServiceProjectKeyName());
      if (subEntry.getName().contains(service.getServiceProjectKeyName())) {
        logger.error("still exist directory:{} for service:{}", subEntry.getAbsoluteFile(),
            service.getServiceName());
        throw new Exception();
      }
    }

    String cmd = String.format("tar -zxf %s -C %s", targetPackagePath, untarTo);
    logger.warn("going to execute untar command:{} for service:{}", cmd, service.getServiceName());
    Utils.executeCommand(cmd, null, null);

    DdUtils.syncFs();

    subEntries = untarToDir.listFiles();
    if (serviceInfo == null) {
      for (File subEntry : subEntries) {
        logger.warn("no service info, exist dir:{}, service name:{}", subEntry.getName(),
            service.getServiceProjectKeyName());
        if (subEntry.getName().contains(service.getServiceProjectKeyName())) {
          logger.warn("Sub-entries under {} are: {}", subEntry.getAbsoluteFile(),
              Arrays.toString(subEntry.list()));
          String dirNameWithoutVersion;
          if (service == PyService.COORDINATOR) {
            dirNameWithoutVersion = generateDeleteVersionFile(subEntry.getAbsolutePath(),
                ddConfig.getCoordinatorTimestamp());
          } else {
            dirNameWithoutVersion = generateDeleteVersionFile(subEntry.getAbsolutePath(), null);
          }
          deleteVersionAfterUntar(subEntry.getAbsolutePath(), dirNameWithoutVersion);
          break;
        }
      }
    } else {
      String requestVersion = serviceInfo.getVersion();
      String internalVersion = "-internal";
      String releaseVersion = "-release";
      String opensourceVersion = "-opensource";
      if (requestVersion.contains(internalVersion)) {
        requestVersion = requestVersion.substring(0, requestVersion.lastIndexOf(internalVersion));
      }
      if (requestVersion.contains(releaseVersion)) {
        requestVersion = requestVersion.substring(0, requestVersion.lastIndexOf(releaseVersion));
      }
      if (requestVersion.contains(opensourceVersion)) {
        requestVersion = requestVersion.substring(0, requestVersion.lastIndexOf(opensourceVersion));
      }
      logger.warn("get request version:{}", requestVersion);
      String tarPrefix = String.format("%s", service.getServiceProjectKeyName());

      for (File subEntry : subEntries) {
        logger
            .warn("has service info, exist dir:{}, service name:{}", subEntry.getName(), tarPrefix);
        if (subEntry.getName().contains(tarPrefix)) {
          logger.warn("Sub-entries under {} are: {}", subEntry.getAbsoluteFile(),
              Arrays.toString(subEntry.list()));
          String dirNameWithoutVersion = generateDeleteVersionFile(subEntry.getAbsolutePath(),
              ddConfig.getCoordinatorTimestamp());
          deleteVersionAfterUntar(subEntry.getAbsolutePath(), dirNameWithoutVersion);
          break;
        }
      }
    }
  }

  private void deleteVersionAfterUntar(String absoluteFile, String deleteVersionFile)
      throws Exception {
    String cmd = String.format("mv %s %s", absoluteFile, deleteVersionFile);
    logger.warn("going to execute mv cmd:{}", cmd);
    Utils.executeCommand(cmd, null, null);
  }


  public Path getTargetPackagePath() {
    return targetPackagePath;
  }

  public void setTargetPackagePath(Path targetPackagePath) {
    this.targetPackagePath = targetPackagePath;
  }

  public Path getUntarTo() {
    return untarTo;
  }

  public void setUntarTo(Path untarTo) {
    this.untarTo = untarTo;
  }
}
