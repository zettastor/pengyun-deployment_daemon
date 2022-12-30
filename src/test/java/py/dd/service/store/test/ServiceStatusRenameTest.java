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

import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.PyService;
import py.dd.DeploymentDaemonConfiguration;
import py.dd.common.ServiceMetadata;
import py.dd.common.ServiceStatus;
import py.dd.processor.PrepareEnvProcessor;
import py.dd.service.store.ServiceStoreImpl;
import py.dd.test.utils.DdTestUtils;
import py.test.TestBase;

/**
 * xx.
 */
public class ServiceStatusRenameTest extends TestBase {

  private static final Logger logger = LoggerFactory.getLogger(ServiceStoreTest.class);
  ServiceMetadata serviceMetadata = new ServiceMetadata();
  private ServiceStoreImpl serviceStoreImpl = new ServiceStoreImpl();
  private DeploymentDaemonConfiguration ddConfig;
  private PyService testService = PyService.DRIVERCONTAINER;
  @Mock
  private Map<PyService, ServiceMetadata> serviceTable;

  /**
   * xx.
   */
  @Before
  public void init() throws Exception {
    super.init();

    serviceMetadata.setServiceName(testService.getServiceName());
    serviceMetadata.setServiceStatus(ServiceStatus.ACTIVE);
    serviceMetadata.setVersion("2.3.0");

    serviceStoreImpl.setServiceTable(serviceTable);
    ddConfig = DdTestUtils.buildTestConfiguration();
    PrepareEnvProcessor processor = new PrepareEnvProcessor();
    processor.setDdConfig(ddConfig);
    processor.process();
    ddConfig.buildServiceRunningPath(testService).toFile().mkdirs();

    serviceStoreImpl.setDdConfig(ddConfig);
    when(serviceTable.get(Mockito.any(PyService.class))).thenReturn(serviceMetadata);

  }

  @Test
  public void renameStatusFileTest() {

    Path fileStoreParent = ddConfig.buildServiceStatusFilePath(testService).getParent();
    File statusFile = new File(fileStoreParent.toString(), "Status");
    statusFile.getParentFile().mkdirs();
    try {
      statusFile.createNewFile();
    } catch (IOException e) {
      logger.warn("catch an exception :{}", e);
    }
    Path statusBakPath = ddConfig.buildServiceStatusFileBakPath(testService);
    Path statusPath = ddConfig.buildServiceStatusFilePath(testService);
    serviceMetadata.saveToFie(statusBakPath);
    serviceStoreImpl.load(testService);
    ServiceMetadata serviceMetadata = ServiceMetadata.buildFromFile(statusPath);
    Assert.assertNotNull(serviceMetadata);
    Assert.assertTrue(!statusBakPath.toFile().exists());

  }

  @Test
  public void testRename() {
    Path oldname = Paths.get("/tmp/test/old");
    Path newname = Paths.get("/tmp/test/new");
    creatFileAndPath(oldname);
    creatFileAndPath(newname);
    String str1 = "11111";
    String str2 = "22222";
    writeToFile(str1, newname);
    writeToFile(str2, oldname);
    serviceStoreImpl.renameStatusFile(oldname, newname);
    String str = readFileByLines(newname);
    Assert.assertTrue(str.equals(str2));
    Assert.assertTrue(!oldname.toFile().exists());


  }

  @Test
  public void testCopy() {
    Path srcname = Paths.get("/tmp/test/src");
    Path dstname = Paths.get("/tmp/test/dst");
    creatFileAndPath(srcname);
    creatFileAndPath(dstname);
    String srcString = "1111111";
    writeToFile(srcString, srcname);
    serviceStoreImpl.copyStatusFile(srcname.toFile(), dstname.toFile());
    String dstString = readFileByLines(dstname);
    Assert.assertTrue(dstString.equals(srcString));
  }

  /**
   * xx.
   */
  public void creatFileAndPath(Path filePath) {
    filePath.toFile().getParentFile().mkdirs();
    if (!filePath.toFile().exists()) {
      try {
        filePath.toFile().createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * xx.
   */
  public void writeToFile(String string, Path filePath) {
    try {
      FileWriter fileWriter = new FileWriter(filePath.toString());
      BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
      bufferedWriter.write(string);
      bufferedWriter.newLine();
      bufferedWriter.flush();
      bufferedWriter.close();
    } catch (IOException e) {
      logger.error("No file or directory");
      throw new IllegalStateException(e);
    }

  }

  /**
   * xx.
   */
  public String readFileByLines(Path filePath) {
    String output = "";
    try {
      BufferedReader input = new BufferedReader(new FileReader(filePath.toFile()));
      StringBuffer buffer = new StringBuffer();
      String text;
      while ((text = input.readLine()) != null) {
        buffer.append(text);
      }
      output = buffer.toString();
    } catch (IOException ioException) {
      System.err.println("File Error!");
    }
    return output;
  }


  @After
  public void clean() {
    FileUtils.deleteQuietly(Paths.get("/tmp/test").toFile());

  }

}
