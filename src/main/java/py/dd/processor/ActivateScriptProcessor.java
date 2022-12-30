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
import java.util.List;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.PyService;
import py.dd.utils.DdUtils;
import py.thrift.deploymentdaemon.ActivateRequest;
import py.thrift.deploymentdaemon.ChangeConfigurationRequest;
import py.thrift.deploymentdaemon.DeactivateRequest;
import py.thrift.deploymentdaemon.DestroyRequest;
import py.thrift.deploymentdaemon.PutTarRequest;
import py.thrift.deploymentdaemon.RestartRequest;
import py.thrift.deploymentdaemon.StartRequest;
import py.thrift.deploymentdaemon.WipeoutRequest;

/**
 * Before launching service, some environment initialization is required. The job is done by scripts
 * with name start by digit number such as 010, 020 e.g. The processor run these scripts in terminal
 * environment.
 */
public class ActivateScriptProcessor extends DeploymentDaemonProcessor {

  private static final Logger logger = LoggerFactory.getLogger(ActivateScriptProcessor.class);

  private boolean doNextStartDataNodeProcess;

  public ActivateScriptProcessor() {
    this.doNextStartDataNodeProcess = true;
  }

  @Override
  public void process(ActivateRequest request) throws Exception {
    service = PyService.findValueByServiceName(request.getServiceName());
    logger.debug("Activate script process - service : {}", request.getServiceName());
    process();
    logger.debug("Activate script process Finished. Turn to next process");
    if (!doNextStartDataNodeProcess) {
      logger.warn("can not get start datanode right, just exit");
      return;
    }
    nextProcessor.process(request);
  }

  @Override
  public void process(DeactivateRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process(StartRequest request) throws Exception {
    service = PyService.findValueByServiceName(request.getServiceName());
    process();
    if (!doNextStartDataNodeProcess) {
      logger.warn("can not get start datanode right, just exit");
      return;
    }
    nextProcessor.process(request);
  }

  @Override
  public void process(RestartRequest request) throws Exception {
    service = PyService.findValueByServiceName(request.getServiceName());
    process();

    nextProcessor.process(request);
  }

  @Override
  public void process(PutTarRequest request) throws Exception {
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
    if (service.equals(PyService.DATANODE)) {
      if (systemMonitor.isDatanodeStarting()) {
        logger.error(
            "going to active DataNode shell, but find it is starting, "
                + "throw exception to interrupt next process");
        this.doNextStartDataNodeProcess = false;
        return;
      } else {
        boolean getStartDataNodeRight = systemMonitor.markDatanodeStarting();
        if (getStartDataNodeRight) {
          logger.warn("mark DataNode starting now");
        } else {
          logger.warn("can not get start DataNode right now, just return");
          this.doNextStartDataNodeProcess = false;
          return;
        }
      }
    }

    Path scriptsPath = ddConfig.buildServicesScriptsPath(service);
    logger.warn("Activate scripts process - script Path: {} ", scriptsPath.toString());

    File file = new File(scriptsPath.toString());
    String[] filelist = file.list();
    for (int i = 0; i < filelist.length; i++) {
      File readfile = new File(scriptsPath.toString() + "\\" + filelist[i]);
      if (!readfile.isDirectory()) {
        logger.warn("Activate scripts process - file name:{}, file size:{}", readfile.getName(),
            readfile.length());
      } else if (readfile.isDirectory()) {
        logger.warn("Activate scripts process - this is a directory. file {}", readfile.toString());
      }
    }

    List<File> activationScriptList = DdUtils.listActivationScripts(scriptsPath);
    if (activationScriptList.isEmpty()) {
      logger.warn("No activation scripts begin with digits in {}", scriptsPath);
      return;
    }

    logger.warn("Activation scripts to initialize running env: {}", activationScriptList);

    for (File activationScript : activationScriptList) {
      try {
        DdUtils.executeScript(activationScript);
      } catch (IOException e) {
        logger.error("Caught an exception when exec {}", activationScript.getAbsolutePath(), e);
        if (service.equals(PyService.DATANODE)) {
          logger.warn("caught an exception, release start datanode right", e);
          systemMonitor.doneDatanodeStarting();
        }
        throw new Exception(e);
      }
    }
  }
}
