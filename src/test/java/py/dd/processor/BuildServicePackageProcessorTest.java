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

import static org.junit.Assert.fail;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import py.common.PyService;
import py.dd.DeploymentDaemonConfiguration;
import py.dd.test.utils.DdTestUtils;
import py.test.TestBase;
import py.thrift.deploymentdaemon.PutTarRequest;

/**
 * A class includes some tests for {@link BuildServicePackageProcessorTest}.
 */
public class BuildServicePackageProcessorTest extends TestBase {

  private static final Logger logger = LoggerFactory
      .getLogger(BuildServicePackageProcessorTest.class);

  private PrepareEnvProcessor prepareEnvProcessor = new PrepareEnvProcessor();

  private BuildServicePackageProcessor buildServicePackageProcessor =
      new BuildServicePackageProcessor();

  private ClearServiceProcessor clearServiceProcessor = new ClearServiceProcessor();

  private DeploymentDaemonConfiguration ddConfig;

  private PyService testService = PyService.DIH;

  private String testVersion = "2.3.0";

  /**
   * xx.
   */
  @Before
  public void init() throws Exception {
    super.init();

    ddConfig = DdTestUtils.buildTestConfiguration();

    prepareEnvProcessor.setDdConfig(ddConfig);
    buildServicePackageProcessor.setDdConfig(ddConfig);
    clearServiceProcessor.setDdConfig(ddConfig);
  }

  /**
   * Build package with multiple bytes. Check if the bytes of package is the same as written
   * before.
   *
   *
   */
  @Test
  public void buildServicePackage() throws Exception {
    clearServiceProcessor.process();

    prepareEnvProcessor.process();

    // create some bytes written to package
    final byte[] byteBuf = new byte[1024];
    for (int i = 0; i < byteBuf.length; i++) {
      byteBuf[i] = (byte) (i % Byte.MAX_VALUE);
    }

    // first time to build package
    PutTarRequest putTarRequest = new PutTarRequest();
    putTarRequest.setAppend(false);
    putTarRequest.setServiceName(testService.getServiceName());
    putTarRequest.setServiceVersion(testVersion);
    putTarRequest.setTarFile(byteBuf);

    buildServicePackageProcessor.process(putTarRequest);

    // second time to build package
    putTarRequest.setAppend(true);
    buildServicePackageProcessor.process(putTarRequest);

    Assert.assertTrue(ddConfig.buildServicePackagePath(testService, testVersion).toFile().exists());

    // read bytes from the package and check them
    DataInputStream input = null;
    try {
      input = new DataInputStream(
          new FileInputStream(ddConfig.buildServicePackagePath(testService, testVersion)
              .toFile()));
    } catch (FileNotFoundException e1) {
      logger
          .error("No such package {}", ddConfig.buildServicePackagePath(testService, testVersion));
      fail();
    }

    byte[] readBuffer = new byte[2048];
    input.read(readBuffer, 0, readBuffer.length);
    // check the first bytes
    for (int i = 0; i < readBuffer.length / 2; i++) {
      Assert.assertEquals(readBuffer[i], (byte) (i % Byte.MAX_VALUE));
    }
    // check the second bytes
    for (int i = 0; i < readBuffer.length / 2; i++) {
      Assert.assertEquals(readBuffer[i + readBuffer.length / 2], (byte) (i % Byte.MAX_VALUE));
    }

  }
}
