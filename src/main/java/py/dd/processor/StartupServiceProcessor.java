/**
* Copyright (C) 2013-2024 Nanjing Pengyun Network Technology Co., Ltd.
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/ 

package py.dd.processor;

import static py.dd.service.IndependentProcessManager.PROCESS_MANAGER_WATCH_SERVICE_NAME;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.PyService;
import py.dd.service.IndependentProcessManager;
import py.dd.worker.ServiceSweeper;
import py.processmanager.JavaProcessBuilder;
import py.processmanager.exception.UnableToStartServiceException;
import py.thrift.deploymentdaemon.ActivateRequest;
import py.thrift.deploymentdaemon.ChangeConfigurationRequest;
import py.thrift.deploymentdaemon.DeactivateRequest;
import py.thrift.deploymentdaemon.DestroyRequest;
import py.thrift.deploymentdaemon.PutTarRequest;
import py.thrift.deploymentdaemon.RestartRequest;
import py.thrift.deploymentdaemon.StartRequest;
import py.thrift.deploymentdaemon.WipeoutRequest;

/**
 * processor that do the job start service via bin/startup.sh in service running path
 *
 */
public class StartupServiceProcessor extends DeploymentDaemonProcessor {

  private static final Logger logger = LoggerFactory.getLogger(StartupServiceProcessor.class);

  private List<String> cmdLineParams;

  @Override
  public void process(ActivateRequest request) throws Exception {
    service = PyService.findValueByServiceName(request.getServiceName());
    cmdLineParams = request.getCmdParams();

    process();
  }

  @Override
  public void process(DeactivateRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process(StartRequest request) throws Exception {
    service = PyService.findValueByServiceName(request.getServiceName());
    cmdLineParams = request.getCmdParams();

    process();

  }

  @Override
  public void process(RestartRequest request) throws Exception {
    service = PyService.findValueByServiceName(request.getServiceName());
    cmdLineParams = request.getCmdParams();

    process();

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
    File binFile;
    try {
      Path serviceLauncherPath = Paths.get(ddConfig.buildServicesScriptsPath(service).toString(),
          service.getServiceLauchingScriptName());
      String cmdToLaunchService = serviceLauncherPath.toString();
      logger.debug("Startup Service process - cmd: {}", cmdToLaunchService);

      binFile = new File(cmdToLaunchService);
      if (!binFile.exists()) {
        logger.error("start command not exists: {}", cmdToLaunchService);
        throw new UnableToStartServiceException(
            "unable to start service due start command not exists");
      } else {
        logger.debug("start bin file {}, size {}, path {},", binFile.getName(), binFile.length(),
            binFile.getAbsolutePath());
      }

      if (service == PyService.CONSOLE) {
        Path tomcatScriptsPath = ddConfig.buildTomcatScriptsPath();
        List<File> scriptList = Arrays.asList(tomcatScriptsPath.toFile().listFiles());
        logger.debug("Scripts under tomcat scripts path {} are {}", tomcatScriptsPath, scriptList);
        for (File script : scriptList) {
          script.setExecutable(true, true);
        }
      }

      // set the start command can execute
      binFile.setExecutable(true, true);

      int pmPid;
      try {
        pmPid = systemMonitor.getPmPid(service);
      } catch (Exception e) {
        logger.error(
            "caught an exception when start service"
                + ":{} need check PM status, but not sure only return",
            service.getServiceName());
        throw new Exception();
      }
      if (pmPid > 0) {
        logger.error("PM:{} on service:{} exists, do not startup AGAIN", pmPid,
            service.getServiceName());
        throw new Exception();
      }
      JavaProcessBuilder javaProcessBuilder = new JavaProcessBuilder();
      javaProcessBuilder.addArgument(cmdToLaunchService)
          .addArgument(ddConfig.buildServicesScriptsPath(service).toString()).setMainClass(
          IndependentProcessManager.class.getName());
      // append launch parameters to process manager
      if (cmdLineParams != null && cmdLineParams.size() > 0) {
        for (String cmdParam : cmdLineParams) {
          javaProcessBuilder.addArgument(cmdParam);
        }
      }
      String watchServiceString = PROCESS_MANAGER_WATCH_SERVICE_NAME + service.getServiceName();
      javaProcessBuilder.setPmIdFlag(watchServiceString);

      try {
        javaProcessBuilder.startProcess();
      } catch (Exception e) {
        logger.error("Caught an exception when start service {}", service.getServiceName(), e);
        throw new UnableToStartServiceException();
      }
    } catch (Exception e) {
      if (service.equals(PyService.DATANODE)) {
        logger.warn("caught an exception, release start datanode right", e);
        systemMonitor.doneDatanodeStarting();
      }
      throw e;
    }
    long sleepTimeMs = 2000;
    if (service.equals(PyService.DATANODE)) {
      for (int i = 0; i < ServiceSweeper.NEED_START_COUNT_THRESHOLD; i++) {
        try {
          Thread.sleep(sleepTimeMs);
          int pmPid = systemMonitor.getPmPid(service);
          if (pmPid > 0) {
            logger.warn("DataNode is startup, done, loop count:{}", i);
            systemMonitor.doneDatanodeStarting();
            break;
          } else {
            logger.warn("DataNode is not startup, loop count:{}", i);
          }
        } catch (Exception e) {
          logger.error("caught an exception when get DataNode startup status, loop count:{}", i, e);
        }
      }
      logger
          .warn("after wait {} ms, still not find datanode PM, should release start datanode right",
              ServiceSweeper.NEED_START_COUNT_THRESHOLD * sleepTimeMs);
      systemMonitor.doneDatanodeStarting();
    }

    if (!binFile.exists()) {
      logger.debug("after run start-scripts, file{} not exists under{}", binFile.getName(),
          binFile.getAbsolutePath());
    } else {
      logger
          .debug("after run start-scripts, start bin file {}, size {}, path {},", binFile.getName(),
              binFile.length(), binFile.getAbsolutePath());
    }


  }

  @Override
  public void process(PutTarRequest request) throws Exception {
    // TODO Auto-generated method stub

  }

}