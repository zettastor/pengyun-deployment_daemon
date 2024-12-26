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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import py.common.PyService;
import py.common.RequestIdBuilder;
import py.dd.DeploymentDaemonConfiguration;
import py.driver.DriverMetadata;
import py.driver.DriverType;
import py.drivercontainer.utils.DriverContainerUtils;
import py.processmanager.utils.PmUtils;
import py.test.TestBase;
import py.thrift.deploymentdaemon.DriverIsAliveExceptionThrift;
import py.thrift.deploymentdaemon.WipeoutRequest;

/**
 * xx.
 */
public class WipeoutCoordinatorFailedTest extends TestBase {

  private static final Logger logger = LoggerFactory.getLogger(WipeoutCoordinatorFailedTest.class);
  long volumeId;
  int snapshotId;
  int processId;
  private DestroyServiceProcessor destroyServiceProcessor;
  private DeploymentDaemonConfiguration ddConfig;
  private WipeoutRequest wipeoutRequest;
  private DriverType driverType;
  @Mock
  private DeploymentDaemonProcessor processor;

  /**
   * xx.
   */
  @Before
  public void init() throws Exception {
    super.init();
    //super.setLogLevel(Level.ALL);
    volumeId = 1L;
    driverType = DriverType.NBD;
    snapshotId = 1;
    processId = PmUtils.getCurrentProcessPid();
    ddConfig = new DeploymentDaemonConfiguration();
    ddConfig.setServicesRunningPath("/tmp");
    String directory = "/tmp/pengyun-drivercontainer/var/SPid_coordinator/1/NBD/";
    String filename = "1";
    File file = new File(directory, filename);
    if (file.exists()) {
      file.delete();
    } else {
      file.getParentFile().mkdirs();
      try {
        file.createNewFile();
      } catch (IOException e) {
        logger.error("Can not find the path");
        throw new IllegalStateException(e);

      }
    }
    FileWriter fileWriter;
    String str =
        "{" + "\"volumeId\":" + volumeId + ",\"snapshotId\":" + snapshotId + ",\"driverType\":"
            + "\"NBD\""
            + ",\"processId\":" + processId + "}";
    try {
      fileWriter = new FileWriter("/tmp/pengyun-drivercontainer/var/SPid_coordinator/1/NBD/1");
      BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
      bufferedWriter.write(str);
      bufferedWriter.flush();
      bufferedWriter.close();
    } catch (IOException e) {
      logger.error("No file or directory");
      throw new IllegalStateException(e);
    }
    DriverMetadata driver = DriverMetadata.buildFromFile(file.toPath());
    wipeoutRequest = new WipeoutRequest(RequestIdBuilder.get());
    destroyServiceProcessor = new DestroyServiceProcessor();
    destroyServiceProcessor.setDdConfig(ddConfig);
    destroyServiceProcessor.setNextProcessor(processor);
    wipeoutRequest.setServiceName(PyService.COORDINATOR.getServiceName());

  }

  @Test
  public void wipeoutCoordinatorTest() throws Exception {
    Assert.assertTrue(DriverContainerUtils.processExist(processId));
    try {
      destroyServiceProcessor.process(wipeoutRequest);
    } catch (DriverIsAliveExceptionThrift e) {
      Assert.assertNotNull(e);

    }
  }

  @After
  public void clean() throws Exception {
    FileUtils.deleteQuietly(ddConfig.buildServiceRunningPath(PyService.DRIVERCONTAINER).toFile());

  }

}
