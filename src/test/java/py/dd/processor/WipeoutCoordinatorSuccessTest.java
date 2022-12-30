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
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import py.common.PyService;
import py.common.RequestIdBuilder;
import py.dd.DeploymentDaemonConfiguration;
import py.dd.common.ServiceMetadata;
import py.dd.processor.chain.ProcessorChainFactory;
import py.dd.service.ServiceMonitor;
import py.dd.service.store.ServiceStore;
import py.drivercontainer.utils.DriverContainerUtils;
import py.storage.StorageConfiguration;
import py.test.TestBase;
import py.thrift.deploymentdaemon.WipeoutRequest;

/**
 * xx.
 */
public class WipeoutCoordinatorSuccessTest extends TestBase {

  private static final Logger logger = LoggerFactory.getLogger(WipeoutCoordinatorFailedTest.class);
  long volumeId;
  int snapshotId;
  int processId;
  private ProcessorChainFactory processorChainFactory;
  private DeploymentDaemonConfiguration ddConfig;
  private WipeoutRequest wipeoutRequest;
  @Mock
  private ServiceMonitor systemMonitor;
  @Mock
  private ServiceStore serviceStore;
  @Mock
  private StorageConfiguration storageConfiguration;
  private ServiceMetadata service;
  private List<File> files;

  /**
   * xx.
   */
  @Before
  public void init() throws Exception {
    super.init();
    volumeId = 1L;
    snapshotId = 1;
    processId = Integer.MAX_VALUE;
    ddConfig = new DeploymentDaemonConfiguration();
    ddConfig.setServicesRunningPath("/tmp/packages");
    ddConfig.setServicesInstallationPath("/tmp/_packages");
    ddConfig.setServicesPackagesPath("/tmp/tars");
    String directory = "/tmp/packages/pengyun-drivercontainer/var/SPid_coordinator/1/NBD/";
    String filename = "1";
    File file1 = new File(directory, filename);
    File file2 = new File("/tmp/_packages", "pengyun-coordinator-2.3.0-201708080909");
    files = new ArrayList<File>();
    files.add(file1);
    files.add(file2);
    File file3 = new File("/tmp/tars", "pengyun-coordinator-2.3.0-201708080909.tar.gz");
    files.add(file3);
    File file4 = new File("/tmp/packages", "pengyun-coordinator-201708080909");
    files.add(file4);
    for (File file : files) {
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
    }
    FileWriter fileWriter;
    String str =
        "{" + "\"volumeId\":" + volumeId + ",\"snapshotId\":" + snapshotId + ",\"driverType\":"
            + "\"NBD\""
            + ",\"processId\":" + processId + "}";
    try {
      fileWriter = new FileWriter(
          "/tmp/packages/pengyun-drivercontainer/var/SPid_coordinator/1/NBD/1");
      BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
      bufferedWriter.write(str);
      bufferedWriter.flush();
      bufferedWriter.close();
    } catch (IOException e) {
      logger.error("No file or directory");
      throw new IllegalStateException(e);
    }
    wipeoutRequest = new WipeoutRequest(RequestIdBuilder.get());
    processorChainFactory = new ProcessorChainFactory();
    service = new ServiceMetadata();
    service.setPid(0);
    service.setPmpid(0);
    service.setVersion("2.3.0");
    service.setServiceName(PyService.COORDINATOR.getServiceName());
    processorChainFactory.setSystemMonitor(systemMonitor);
    processorChainFactory.setServiceStore(serviceStore);
    processorChainFactory.setDdConfig(ddConfig);
    processorChainFactory.setStorageConfiguration(storageConfiguration);
    wipeoutRequest.setServiceName(PyService.COORDINATOR.getServiceName());
    // driverUpgrade add timestamp for coordinator
    wipeoutRequest.setCoorTimestamp("201708080909");
    when(systemMonitor.getPmPid(any(PyService.class))).thenReturn(Integer.MAX_VALUE);
    when(systemMonitor.getServiceLauncherPid(any(PyService.class))).thenReturn(Integer.MAX_VALUE);
    when(systemMonitor.getProcessId(any(PyService.class))).thenReturn(Integer.MAX_VALUE);
    when(serviceStore.get(any(PyService.class))).thenReturn(service);
    when(storageConfiguration.getOutputRootpath()).thenReturn("/tmp");
  }

  @Test
  public void wipeoutCoordinatorSuccessTest() throws Exception {
    Assert.assertFalse(DriverContainerUtils.processExist(processId));
    processorChainFactory.createServiceWipeoutChain().process(wipeoutRequest);
    FileUtils.deleteQuietly(ddConfig.buildServiceRunningPath(PyService.DRIVERCONTAINER).toFile());
    for (File file : files) {
      Assert.assertFalse(file.exists());
    }

  }

}
