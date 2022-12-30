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

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import py.common.PyService;
import py.dd.DeploymentDaemonConfiguration;
import py.dd.test.utils.DdTestUtils;
import py.test.TestBase;
import py.thrift.deploymentdaemon.ActivateRequest;

/**
 * A class includes some test for {@link LinkProcessorTest}.
 */
public class LinkProcessorTest extends TestBase {

  private static final Logger logger = LoggerFactory.getLogger(LinkProcessorTest.class);

  private LinkProcessor linkProcessor = new LinkProcessor();

  private PrepareEnvProcessor prepareEnvProcessor = new PrepareEnvProcessor();

  private DeploymentDaemonConfiguration ddConfig;

  private PyService testService = PyService.DIH;

  private String testVersion = "2.3.0";

  private ClearServiceProcessor clearServiceProcessor = new ClearServiceProcessor();

  @Mock
  private DeploymentDaemonProcessor processor;

  /**
   * xx.
   */
  @Before
  public void init() throws Exception {
    super.init();

    ddConfig = DdTestUtils.buildTestConfiguration();

    prepareEnvProcessor.setDdConfig(ddConfig);
    linkProcessor.setDdConfig(ddConfig);
    clearServiceProcessor.setDdConfig(ddConfig);
    linkProcessor.setNextProcessor(processor);
  }

  /**
   * After link service processor done linking, configuration directory will exist in service
   * running path.
   *
   *
   */
  @Test
  public void testLinkProcessor() throws Exception {
    clearServiceProcessor.process();

    ActivateRequest request = new ActivateRequest();
    request.setServiceName(testService.getServiceName());
    request.setServiceVersion(testVersion);

    prepareEnvProcessor.process();

    Path configurationPath = ddConfig.buildServicesConfigurationPath(testService, testVersion);
    configurationPath.toFile().mkdirs();
    Assert.assertTrue(configurationPath.toFile().exists());

    Path configurationPathInRunningPath = Paths
        .get(ddConfig.buildServiceRunningPath(testService).toString(),
            ddConfig.getServicesConfigDirName());
    Assert.assertFalse(configurationPathInRunningPath.toFile().exists());
    linkProcessor.process(request);
    Assert.assertTrue(configurationPathInRunningPath.toFile().exists());
  }
}
