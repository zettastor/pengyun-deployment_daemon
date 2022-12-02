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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.PyService;
import py.dd.common.ServiceMetadata;
import py.dd.utils.DdUtils;
import py.storage.EdRootpathSingleton;
import py.thrift.deploymentdaemon.ActivateRequest;
import py.thrift.deploymentdaemon.ChangeConfigurationRequest;
import py.thrift.deploymentdaemon.DeactivateRequest;
import py.thrift.deploymentdaemon.DestroyRequest;
import py.thrift.deploymentdaemon.FailedToWipeoutExceptionThrift;
import py.thrift.deploymentdaemon.PutTarRequest;
import py.thrift.deploymentdaemon.RestartRequest;
import py.thrift.deploymentdaemon.StartRequest;
import py.thrift.deploymentdaemon.WipeoutRequest;

/**
 * A class as a processor of processor chain which clear all service relative resources including
 * packages, installation info and running vars.
 */
public class ClearServiceProcessor extends DeploymentDaemonProcessor {

  private static final Logger logger = LoggerFactory.getLogger(ClearServiceProcessor.class);

  @Override
  public void process(ActivateRequest request) throws Exception {
    throw new NotImplementedException();
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
    logger.debug("process {}", request);
    service = PyService.findValueByServiceName(request.getServiceName());
    if (service == PyService.COORDINATOR) {
      ddConfig.setCoordinatorTimestamp(request.getCoorTimestamp());
    }

    process();

    if (nextProcessor != null) {
      nextProcessor.process(request);
    }
  }

  @Override
  public void process(ChangeConfigurationRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process() throws Exception {
    if (service == null) {
      logger.debug("Delete services root directory {}", ddConfig.getServicesRootPath());
      FileUtils.deleteQuietly(new File(ddConfig.getServicesRootPath()));
    } else {
      // wipeout disk if the service is datanode
      if (service == PyService.DATANODE) {
        Path scriptsPath = ddConfig.buildServicesScriptsPath(service);
        File script = new File(scriptsPath.toFile(), ddConfig.getNameOfScriptToWipeoutDisk());
        if (script.exists()) {
          DdUtils.executeScript(script);
        } else {
          logger.info("No such script file:{}", script.getAbsolutePath());
        }
      }

      // if the service is coordinator or fsServer, delete all related directory&files
      if (service == PyService.COORDINATOR) {
        logger.debug("(Coordinator)Delete service running directroy {}",
            ddConfig.getServicesRunningPath());
        deleteAllServicePath(ddConfig.getServicesRunningPath());
        logger.debug("(Coordinator)Delete service installation directory {}",
            ddConfig.getServicesInstallationPath());
        deleteAllServicePath(ddConfig.getServicesInstallationPath());
        logger.debug("(Coordinator)Delete service package {}", ddConfig.getServicesPackagesPath());
        deleteAllServicePath(ddConfig.getServicesPackagesPath());
      } else {
        ServiceMetadata serviceInfo = serviceStore.get(service);
        logger.debug("Delete service running directroy {}",
            ddConfig.buildServiceRunningPath(service));
        FileUtils.deleteDirectory(ddConfig.buildServiceRunningPath(service).toFile());
        logger.debug("Delete service installation directory {}",
            ddConfig.buildServiceInstallationPath(service, serviceInfo.getVersion()));
        FileUtils.deleteDirectory(
            ddConfig.buildServiceInstallationPath(service, serviceInfo.getVersion()).toFile());
        logger.debug("Delete service package {}",
            ddConfig.buildServicePackagePath(service, serviceInfo.getVersion()));
        FileUtils.deleteQuietly(
            ddConfig.buildServicePackagePath(service, serviceInfo.getVersion()).toFile());
      }

      //Service relevent logs such as performance will save 
      // to /var/testing/EventData/serviceName,and when wipeout
      //service,will clear this logs
      String rootPath = storageConfiguration.getOutputRootpath();
      Path dataPath = Paths
          .get(rootPath, EdRootpathSingleton.EVENT_DATA_PATH_PREFIX, service.getServiceName());
      logger.debug("delete serice :{} event data  path:{}", service, dataPath);
      logger.debug("dataPath:{} exist or not :{}", dataPath, dataPath.toFile().exists());
      if (dataPath.toFile().exists() && !FileUtils.deleteQuietly(dataPath.toFile())) {
        String errorMsg = "Delete service event data path " + dataPath + " failed";
        throw new FailedToWipeoutExceptionThrift().setDetail(errorMsg);
      }

      Path recordPath = Paths
          .get(rootPath, EdRootpathSingleton.RECORD_POSITION_PATH_PREFIX, service.getServiceName());
      logger.debug("delete service :{} record  path:{}", service, recordPath);
      if (recordPath.toFile().exists() && !FileUtils.deleteQuietly(recordPath.toFile())) {
        String errorMsg = "Delete service record path " + recordPath + " failed";
        throw new FailedToWipeoutExceptionThrift().setDetail(errorMsg);
      }

      serviceStore.clearMemory();
    }
  }

  @Override
  public void process(PutTarRequest request) throws Exception {
    throw new NotImplementedException();
  }

  // use regular expression to delete service relative directory
  // now only used for coordinator

  /**
   * xx.
   */
  public void deleteAllServicePath(String parent) throws Exception {
    logger.debug("deleteAllServicePath {}", parent);
    Path path = Paths.get(parent);
    String coordinate = service.getServiceProjectKeyName();
    Pattern pattern = Pattern.compile(coordinate + ".*");
    List<Path> paths = Files.walk(path).filter(p -> {
      File file = p.toFile();
      Matcher matcher = pattern.matcher(file.getName());
      return matcher.matches();
    }).collect(Collectors.toList());

    for (Path item : paths) {
      logger.debug("delete path {}", item.toFile().getPath());
      FileUtils.deleteQuietly(item.toFile());
    }
  }
}
