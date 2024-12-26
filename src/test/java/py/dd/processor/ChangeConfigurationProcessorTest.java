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

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import py.common.PyService;
import py.common.RequestIdBuilder;
import py.dd.DeploymentDaemonConfiguration;
import py.dd.test.utils.DdTestUtils;
import py.test.TestBase;
import py.thrift.deploymentdaemon.ChangeConfigurationRequest;
import py.thrift.deploymentdaemon.ChangeConfigurationResponse;

/**
 * A class includes some test for {@link ChangeConfigurationResponse}.
 */
public class ChangeConfigurationProcessorTest extends TestBase {

  private static final Logger logger = LoggerFactory
      .getLogger(ChangeConfigurationProcessorTest.class);

  private PrepareEnvProcessor prepareEnvProcessor = new PrepareEnvProcessor();

  private ChangeConfigurationProcessor changeConfigurationProcessor =
      new ChangeConfigurationProcessor();

  private PyService testService = PyService.DIH;

  private String testVersion = "2.3.0";

  private String propKey = "center.dih";

  private String testConfigFile = "instancehub.config";

  private String propOldValue = "localhost:10000";

  private String propNewValue = "10.0.1.16:10001";

  private DeploymentDaemonConfiguration ddConfig;

  private ClearServiceProcessor clearServiceProcessor = new ClearServiceProcessor();

  /**
   * xx.
   */
  @Before
  public void init() throws Exception {
    super.init();

    ddConfig = DdTestUtils.buildTestConfiguration();

    prepareEnvProcessor.setDdConfig(ddConfig);

    changeConfigurationProcessor.setDdConfig(ddConfig);

    clearServiceProcessor.setDdConfig(ddConfig);
  }

  @Test
  public void testChangingConfiguration() throws Exception {
    clearServiceProcessor.process();

    prepareEnvProcessor.process();

    Path configFilePath = ddConfig.buildConfigFilePath(testService, testVersion, testConfigFile);
    if (!configFilePath.getParent().toFile().exists()) {
      configFilePath.getParent().toFile().mkdirs();
    }
    BufferedWriter writer = new BufferedWriter(
        new FileWriter(ddConfig.buildConfigFilePath(testService,
            testVersion, testConfigFile).toFile()));
    writer.write(String.format("%s:%s", propKey, propOldValue));
    writer.flush();
    writer.close();

    Map<String, String> changes = new HashMap<String, String>();
    changes.put(propKey, propNewValue);
    ChangeConfigurationRequest request = new ChangeConfigurationRequest();
    request.setRequestId(RequestIdBuilder.get());
    request.setServiceName(testService.getServiceName());
    request.setConfigFile(testConfigFile);
    request.setChangingConfigurations(changes);
    request.setServiceVersion(testVersion);
    request.setPreserve(true);

    changeConfigurationProcessor.process(request);

    BufferedInputStream in = new BufferedInputStream(
        new FileInputStream(ddConfig.buildConfigFilePath(testService,
            testVersion, testConfigFile).toFile()));
    Properties prop = new Properties();
    prop.load(in);

    Assert.assertEquals(prop.get(propKey), propNewValue);
  }
}
