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

package py.dd.service.store.test;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import py.common.PyService;
import py.dd.DeploymentDaemonConfiguration;
import py.dd.common.ServiceMetadata;
import py.dd.common.ServiceStatus;
import py.dd.processor.ClearServiceProcessor;
import py.dd.processor.PrepareEnvProcessor;
import py.dd.service.store.ServiceStore;
import py.dd.service.store.ServiceStoreImpl;
import py.dd.test.utils.DdTestUtils;
import py.test.TestBase;

/**
 * A class includes some tests for {@link ServiceStore}.
 */
public class ServiceStoreTest extends TestBase {

  private static final Logger logger = LoggerFactory.getLogger(ServiceStoreTest.class);

  private ServiceStoreImpl serviceStoreImpl = new ServiceStoreImpl();

  private DeploymentDaemonConfiguration ddConfig;

  private PyService testService = PyService.DIH;

  private ServiceMetadata serviceMetadata = new ServiceMetadata();

  /**
   * xx.
   */
  @Before
  public void init() throws Exception {
    super.init();

    ddConfig = DdTestUtils.buildTestConfiguration();

    PrepareEnvProcessor processor = new PrepareEnvProcessor();
    processor.setDdConfig(ddConfig);
    processor.process();
    ddConfig.buildServiceRunningPath(testService).toFile().mkdirs();

    serviceStoreImpl.setDdConfig(ddConfig);

    serviceMetadata.setServiceName(testService.getServiceName());
    serviceMetadata.setServiceStatus(ServiceStatus.ACTIVE);
    serviceMetadata.setVersion("2.3.0");
  }

  @Test
  public void save2flush2load2get() throws Exception {
    // save to store
    serviceStoreImpl.save(serviceMetadata);

    // clear memory store
    serviceStoreImpl.clearMemory();

    // load from file
    serviceStoreImpl.load();

    // check
    ServiceMetadata checkServiceMetadata = serviceStoreImpl.get(testService);
    Assert.assertTrue(checkServiceMetadata.equals(serviceMetadata));

    ClearServiceProcessor clearServiceProcessor = new ClearServiceProcessor();
    clearServiceProcessor.setDdConfig(ddConfig);
    clearServiceProcessor.process();
  }
}
