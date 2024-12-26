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

package py.dd.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import py.test.TestBase;

/**
 * There is no FTP server right now, so ignore all the ftp unit tests.
 */
public class FtpUtilsTester extends TestBase {

  private static final Logger logger = LoggerFactory.getLogger(FtpUtilsTester.class);
  FtpHandler ftpHandler = null;

  /**
   * xx.
   */
  public void init() {
    try {
      // ftpHandler = new FtpHandler("10.0.1.131", 21, "root", "312");
      // ftpHandler.connect();
    } catch (Exception e) {
      logger.error("Caught an exception", e);
      Assert.fail();
    }
  }

  @Ignore
  @Test
  public void testUploadFiles() {
    try {
      FileInputStream in = new FileInputStream(new File("/home/sxl/DIH集成测试.docx"));
      ftpHandler.uploadFile("/ftp-files", "Graphite安装使用指南.doc", in);

      FileInputStream in1 = new FileInputStream(new File("/home/sxl/DIH集成测试.docx"));
      // ftpHandler.closeServer();
      // this.init();
      ftpHandler.uploadFile("/ftp-files", "Graphite.doc", in1);
    } catch (Exception e) {
      logger.error("Caught an exception", e);
      Assert.fail();
    }
  }

  @Ignore
  @Test
  public void testUploadFolder() {
    try {
      ftpHandler.uploadFolder("/home/sxl/david", "/ftp-files/testDirectory/");
    } catch (Exception e) {
      logger.error("Caught an exception", e);
      Assert.fail();
    }
  }

  @Ignore
  @Test
  public void testDowdloadFiles() {
    try {
      ftpHandler.downloadFile("/ftp-files", "test", "/tmp");
    } catch (Exception e) {
      logger.error("Caught an exception", e);
      Assert.fail();
    }
  }

  @Ignore
  @Test
  public void testCreateDirectory() {
    try {
      ftpHandler.createDirectory("/ftp-files/testDirectory/");
    } catch (Exception e) {
      logger.error("Caught an exception", e);
      Assert.fail();
    }
  }

  @Ignore
  @Test
  public void testGetLocalIpAddress() {
    InetAddress ia = null;
    try {
      ia = InetAddress.getLocalHost();

      String localname = ia.getHostName();
      String localip = ia.getHostAddress();
      System.out.println("本机名称是：" + localname);
      System.out.println("本机的ip是 ：" + localip);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * xx.
   */
  public void cleanUp() {
    try {
      ftpHandler.disconnect();
    } catch (IOException e) {
      logger.error("Caught an exception", e);
    }
  }
}
