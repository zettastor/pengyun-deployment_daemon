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
