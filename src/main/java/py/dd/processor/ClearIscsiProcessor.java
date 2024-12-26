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

import java.io.IOException;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.OsCmdExecutor;
import py.common.PyService;
import py.dd.utils.DdUtils;
import py.iet.file.mapper.ConfigurationFileMapper;
import py.iet.file.mapper.InitiatorsAllowFileMapper;
import py.thrift.deploymentdaemon.ActivateRequest;
import py.thrift.deploymentdaemon.ChangeConfigurationRequest;
import py.thrift.deploymentdaemon.DeactivateRequest;
import py.thrift.deploymentdaemon.DestroyRequest;
import py.thrift.deploymentdaemon.PutTarRequest;
import py.thrift.deploymentdaemon.RestartRequest;
import py.thrift.deploymentdaemon.StartRequest;
import py.thrift.deploymentdaemon.WipeoutRequest;

/**
 * A class as a processor in processor chain. This processor clear all iscsi enterprise target
 * service relative configuration files.
 */
public class ClearIscsiProcessor extends DeploymentDaemonProcessor {

  private static final Logger logger = LoggerFactory.getLogger(ClearIscsiProcessor.class);

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

    if (service == PyService.DRIVERCONTAINER) {
      process();
    }
  }

  @Override
  public void process(ChangeConfigurationRequest request) throws Exception {
    throw new NotImplementedException();
  }

  /**
   * xx.
   */
  public void process() throws Exception {
    // TODO should wipeout iscsi and pyd env also
    // clear initiator allow table first
    InitiatorsAllowFileMapper initiatorsAllowFileMapper = new InitiatorsAllowFileMapper();
    initiatorsAllowFileMapper.setFilePath(ddConfig.getAllowFilePath());
    initiatorsAllowFileMapper.load();
    initiatorsAllowFileMapper.getInitiatorAllowTable().clear();
    initiatorsAllowFileMapper.flush();

    ConfigurationFileMapper configurationFileMapper = new ConfigurationFileMapper();
    configurationFileMapper.setFilePath(ddConfig.getIetConfigFilePath());
    configurationFileMapper.load();
    configurationFileMapper.getTargetList().clear();
    configurationFileMapper.flush();
    //clear lio saveconfig.json file content
    //clearCommand is "/usr/bin/targetcli clearconfig confirm=true"

    String clearCommand = ddConfig.getClearConfigCommand();
    OsCmdExecutor.OsCmdOutputLogger clearConsumer = new OsCmdExecutor.OsCmdOutputLogger(logger,
        clearCommand);
    //saveCommand is "/usr/bin/targetcli saveconfig"
    String saveCommand = ddConfig.getSaveConfigCommand();
    OsCmdExecutor.OsCmdOutputLogger saveConsumer = new OsCmdExecutor.OsCmdOutputLogger(logger,
        saveCommand);

    try {
      int clearExistCode = OsCmdExecutor
          .exec(clearCommand, DdUtils.osCMDThreadPool, clearConsumer, clearConsumer);
      int saveExistCode = OsCmdExecutor
          .exec(saveCommand, DdUtils.osCMDThreadPool, saveConsumer, saveConsumer);
      if (clearExistCode != 0) {
        logger.error("Catch an exception when exec clear command :{} to clear saveconfig.json",
            clearCommand);
      }
      if (saveExistCode != 0) {
        logger.error("Catch an exception when exec save command :{} ", saveCommand);
      }
    } catch (IOException e) {
      logger.warn("clear command :{} or save command :{} not found ", clearCommand, saveCommand);
    }

  }

  @Override
  public void process(PutTarRequest request) throws Exception {
    throw new NotImplementedException();
  }

}
