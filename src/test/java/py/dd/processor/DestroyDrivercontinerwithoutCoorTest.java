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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import py.common.PyService;
import py.dd.DeploymentDaemonConfiguration;
import py.dd.processor.chain.ProcessorChainFactory;
import py.dd.service.ServiceMonitor;
import py.driver.DriverMetadata;
import py.drivercontainer.utils.DriverContainerUtils;
import py.processmanager.utils.PmUtils;
import py.test.TestBase;
import py.thrift.deploymentdaemon.DestroyRequest;

/**
 * xx.
 */
public class DestroyDrivercontinerwithoutCoorTest extends TestBase {

  private static final Logger logger = LoggerFactory
      .getLogger(DestroyDrivercontinerwithoutCoorTest.class);
  long volumeId;
  int snapshotId;
  int processId;
  private ProcessorChainFactory processorChainFactory;
  private DestroyRequest destroyRequest;
  private DeploymentDaemonConfiguration ddConfig;
  @Mock
  private ServiceMonitor systemMonitor;
  private DriverMetadata driver;

  /**
   * xx.
   */
  public void init() throws Exception {
    super.init();
    super.setLogLevel(Level.ALL);
    volumeId = 1L;
    snapshotId = 1;
    processId = PmUtils.getCurrentProcessPid();

    processorChainFactory = new ProcessorChainFactory();
    ddConfig = new DeploymentDaemonConfiguration();
    ddConfig.setServicesRunningPath("/tmp/packages");
    processorChainFactory.setDdConfig(ddConfig);
    processorChainFactory.setSystemMonitor(systemMonitor);
    destroyRequest = new DestroyRequest();
    destroyRequest.setServiceName(PyService.DRIVERCONTAINER.getServiceName());

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

    driver = DriverMetadata.buildFromFile(file.toPath());
    when(systemMonitor.getPmPid(any(PyService.class))).thenReturn(Integer.MAX_VALUE);
    when(systemMonitor.getServiceLauncherPid(any(PyService.class))).thenReturn(Integer.MAX_VALUE);
    when(systemMonitor.getProcessId(any(PyService.class))).thenReturn(Integer.MAX_VALUE);
  }

  @Test
  public void destroyrivercontainerTest() throws Exception {
    logger.debug("driver:{}", driver);
    processorChainFactory.createServiceDestroyChain().process(destroyRequest);
    Assert.assertTrue(DriverContainerUtils.processExist(driver.getProcessId()));
  }

  @After
  public void clean() throws Exception {
    FileUtils.deleteQuietly(ddConfig.buildServiceRunningPath(PyService.DRIVERCONTAINER).toFile());

  }

}
