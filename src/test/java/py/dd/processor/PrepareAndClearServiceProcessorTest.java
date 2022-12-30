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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import py.common.PyService;
import py.dd.DeploymentDaemonConfiguration;
import py.dd.common.ServiceMetadata;
import py.dd.service.store.ServiceStore;
import py.dd.test.utils.DdTestUtils;
import py.storage.StorageConfiguration;
import py.test.TestBase;
import py.thrift.deploymentdaemon.WipeoutRequest;

/**
 * A class includes some tests for {@link ClearServiceProcessor}.
 */
public class PrepareAndClearServiceProcessorTest extends TestBase {

  private static final Logger logger = LoggerFactory
      .getLogger(PrepareAndClearServiceProcessorTest.class);
  @Mock
  public ServiceStore serviceStore;
  private ClearServiceProcessor clearServiceProcessor = new ClearServiceProcessor();
  private PrepareEnvProcessor prepareEnvProcessor = new PrepareEnvProcessor();
  private DeploymentDaemonConfiguration ddConfig;
  private PyService testService = PyService.DIH;
  private String testVersion = "2.3.0";
  @Mock
  private DeploymentDaemonProcessor nextProcessor;
  @Mock
  private StorageConfiguration storageConfiguration;

  /**
   * xx.
   */
  @Before
  public void init() throws Exception {
    super.init();

    ddConfig = DdTestUtils.buildTestConfiguration();

    prepareEnvProcessor.setServiceStore(serviceStore);
    clearServiceProcessor.setServiceStore(serviceStore);
    prepareEnvProcessor.setDdConfig(ddConfig);
    clearServiceProcessor.setDdConfig(ddConfig);
    clearServiceProcessor.setNextProcessor(nextProcessor);
    clearServiceProcessor.setStorageConfiguration(storageConfiguration);
    when(storageConfiguration.getOutputRootpath()).thenReturn("/tmp");
  }

  /**
   * Prepare services relative paths and clear the root path.
   *
   *
   */
  @Test
  public void testPreAndClearAllServiceProcessor() throws Exception {
    clearServiceProcessor.process();

    prepareEnvProcessor.process();

    Assert.assertTrue(new File(ddConfig.getServicesRootPath()).exists());
    Assert.assertTrue(new File(ddConfig.getServicesPackagesPath()).exists());
    Assert.assertTrue(new File(ddConfig.getServicesInstallationPath()).exists());
    Assert.assertTrue(new File(ddConfig.getServicesRunningPath()).exists());

    WipeoutRequest request = new WipeoutRequest();
    clearServiceProcessor.process(request);

    Assert.assertFalse(new File(ddConfig.getServicesRootPath()).exists());
  }

  /**
   * Prepare service installation path and service running path for some service and then clear the
   * two directory.
   *
   *
   */
  @Test
  public void testPreAndClearSomeServiceProcessor() throws Exception {
    clearServiceProcessor.process();

    ServiceMetadata serviceInfo = new ServiceMetadata();
    serviceInfo.setServiceName(testService.getServiceName());
    serviceInfo.setVersion(testVersion);

    when(serviceStore.get(any(PyService.class))).thenReturn(serviceInfo);
    prepareEnvProcessor.process();

    Path serviceInstallationPath = ddConfig.buildServiceInstallationPath(testService, testVersion);
    Path serviceRunningPath = ddConfig.buildServiceRunningPath(testService);

    logger.debug("Create service installation path {} and running path {}", serviceInstallationPath,
        serviceRunningPath);
    if (!serviceInstallationPath.toFile().exists()) {
      serviceInstallationPath.toFile().mkdirs();
    }

    if (!serviceRunningPath.toFile().exists()) {
      serviceRunningPath.toFile().mkdirs();
    }
    Assert.assertTrue(serviceInstallationPath.toFile().exists());
    Assert.assertTrue(serviceRunningPath.toFile().exists());

    WipeoutRequest request = new WipeoutRequest();
    request.setServiceName(testService.getServiceName());
    clearServiceProcessor.process(request);

    Assert.assertFalse(serviceInstallationPath.toFile().exists());
    Assert.assertFalse(serviceRunningPath.toFile().exists());
  }
}
