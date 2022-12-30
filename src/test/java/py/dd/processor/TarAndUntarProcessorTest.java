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

import static py.drivercontainer.driver.DriverWorkspaceProvider.FILE_SEPARATOR;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import py.common.PyService;
import py.dd.DeploymentDaemonConfiguration;
import py.dd.service.store.ServiceStore;
import py.dd.test.utils.DdTestUtils;
import py.test.TestBase;

/**
 * A class includes some tests for {@link UntarProcessor}.
 */
public class TarAndUntarProcessorTest extends TestBase {

  private static final Logger logger = LoggerFactory.getLogger(TarAndUntarProcessorTest.class);

  private TarProcessor tarProcessor = new TarProcessor();

  private UntarProcessor untarProcessor = new UntarProcessor();

  private DeploymentDaemonConfiguration ddConfig;

  /**
   * xx.
   */
  public static String setFileContent(File file, String s) {
    String line = "";
    String content = "";
    try {
      FileOutputStream outStream = null;
      outStream = new FileOutputStream(file);
      outStream.write(s.getBytes());
    } catch (Exception e) {
      content = "ERROR ";
    }
    return content;
  }

  /**
   * xx.
   */
  public static String getFileContent(File file) {
    String line = "";
    String content = "";
    try {
      BufferedReader bf = new BufferedReader(new InputStreamReader(
          new FileInputStream(file)));
      while ((line = bf.readLine()) != null) {
        content += line;
      }
    } catch (FileNotFoundException e) {
      content = "ERROR ";
    } catch (IOException e) {
      content = "ERROR ";
    }
    return content;
  }

  /**
   * xx.
   */
  public static FileLock getFileLock(File file) throws IOException {
    RandomAccessFile fi = new RandomAccessFile(file, "rw");
    FileChannel fc = fi.getChannel();
    return fc.tryLock();
  }

  /**
   * xx.
   */
  @Before
  public void init() throws Exception {
    super.init();

    ddConfig = DdTestUtils.buildTestConfiguration();

    tarProcessor.setDdConfig(ddConfig);
    untarProcessor.setDdConfig(ddConfig);
  }

  /**
   * Tar a directory to a package, and then untar the package. If the directory after untaring is
   * the same as directory before taring, then pass the test.
   */
  @Test
  public void testTarAndUntarProcessor() throws Exception {
    ServiceStore serviceStore = Mockito.mock(ServiceStore.class);
    Mockito.when(serviceStore.get(Mockito.any(PyService.class))).thenReturn(null);

    Path tarFrom = Paths.get("/tmp/pengyun-instancehub-2.3.0/");
    FileUtils.deleteQuietly(tarFrom.toFile());
    tarFrom.toFile().mkdirs();

    Path tarFilesOrDirectories = Paths.get(tarFrom.toString(), "pengyun-instancehub-2.3.0");
    FileUtils.deleteQuietly(tarFilesOrDirectories.toFile());
    tarFilesOrDirectories.toFile().mkdirs();

    Path tarTo = Paths.get("/tmp");

    String packageName = "pengyun-instancehub-2.3.0-internal.tar.gz";

    tarProcessor.setTarFrom(tarFrom);
    tarProcessor.setTarTo(tarTo);
    tarProcessor.setPackageName(packageName);
    tarProcessor.process();

    Assert.assertTrue(Paths.get("/tmp", packageName).toFile().exists());

    FileUtils.deleteQuietly(tarFrom.toFile());
    Assert.assertFalse(tarFrom.toFile().exists());

    untarProcessor.setServiceStore(serviceStore);
    untarProcessor.setTargetPackagePath(Paths.get("/tmp", packageName));
    untarProcessor.setUntarTo(tarFrom);
    untarProcessor.setService(PyService.DIH);
    untarProcessor.process();
    Assert.assertTrue(tarFilesOrDirectories.toFile().exists());
  }

  @Test
  public void testDeleteFile() throws Exception {

    Path path = Paths.get("/tmp/var");
    String coordinate = "aaa.*";

    File test = new File("/tmp/var/aaa111");
    File testP = test.getParentFile();
    if (!testP.exists()) {
      testP.mkdirs();
    }
    if (!test.exists()) {
      test.createNewFile();
    }

    Pattern pattern = Pattern.compile(coordinate);
    List<Path> paths = Files.walk(path).filter(p -> {
      File file = p.toFile();
      Matcher matcher = pattern.matcher(file.getName());
      logger.warn("testDeleteFile f={} m={}", file.getName(), matcher.matches());
      return matcher.matches();
    }).collect(Collectors.toList());

    for (Path item : paths) {
      logger.warn("testDeleteFile {}", item.toFile().getPath());
      FileUtils.deleteQuietly(item.toFile());
    }

    boolean res = false;
    if (!test.exists()) {
      res = true;
    }
    assert (res);
  }

  @Test
  public void testCreateAndWriteFile() throws Exception {

    logger.warn("Driver_Upgrade init cur/latest/status files");
    String serviceRunningPath = "/tmp";
    serviceRunningPath += (FILE_SEPARATOR + "var");
    logger.warn("Driver_Upgrade serviceRunningPath={}", serviceRunningPath);
    String pydCur = Paths.get(serviceRunningPath, "pydCur").toString();
    String pydLatest = Paths.get(serviceRunningPath, "pydLatest").toString();
    String status = Paths.get(serviceRunningPath, "status").toString();
    logger.warn("Driver_Upgrade pydCur={} pydLatest={} status={}", pydCur, pydLatest, status);
    String curVer = null;
    String latestVer = null;
    String curStatus = null;
    File file = new File(pydLatest);
    File parentFile = file.getParentFile();
    if (!parentFile.exists()) {
      parentFile.mkdirs();
    }
    if (!file.exists()) {
      file.createNewFile();
    }

    File fstatus = new File(status);
    File fstatusParentFile = fstatus.getParentFile();
    if (!fstatusParentFile.exists()) {
      fstatusParentFile.mkdirs();
    }
    if (!fstatus.exists()) {
      fstatus.createNewFile();
    }

    File fpydCur = new File(pydCur);
    File fpydCurParent = fstatus.getParentFile();
    if (!fpydCurParent.exists()) {
      fpydCurParent.mkdirs();
    }
    if (!fpydCur.exists()) {
      fpydCur.createNewFile();

      FileLock lock = getFileLock(fpydCur);
      setFileContent(fpydCur, "1111");
      lock.release();
    } else {
      FileLock lock = getFileLock(fpydCur);
      curVer = getFileContent(fpydCur);
      lock.release();

      if (curVer != "" && Long.parseLong(curVer) != Long.parseLong("1111")) {
        lock = getFileLock(fstatus);
        curStatus = getFileContent(fstatus);
        lock.release();
      }
    }
  }

  @Test
  public void testCreateAndWriteFile2() throws Exception {
    logger.debug("Driver_Upgrade check cur/latest/migrate files");
    String serviceRunningPath = "/tmp";
    serviceRunningPath += (FILE_SEPARATOR + "var");
    String pydCur = Paths.get(serviceRunningPath, "pydCur").toString();
    String pydLatest = Paths.get(serviceRunningPath, "pydLatest").toString();
    String migrate = Paths.get(serviceRunningPath, "migrate").toString();
    logger.warn("Driver_Upgrade pydCur={} pydLatest={} migrate={}", pydCur, pydLatest, migrate);
    boolean migrating = false;

    // create latest file if not exist
    File fpydLatest = new File(pydLatest);
    File fpydLatestParentFile = fpydLatest.getParentFile();
    if (!fpydLatestParentFile.exists()) {
      fpydLatestParentFile.mkdirs();
    }
    if (!fpydLatest.exists()) {
      try {
        logger.warn("Driver_Upgrade pydLatest={}", pydLatest);
        fpydLatest.createNewFile();
      } catch (IOException e) {
        logger.error("Can not find the path");
        throw new IllegalStateException(e);
      }
    }

    // lock latest first then read migrate & update latest
    FileLock lock;
    try {
      lock = getFileLock(fpydLatest);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    File fmigrate = new File(migrate);
    if (fmigrate.exists()) {
      String tmp = getFileContent(fmigrate);
      if (Integer.parseInt(tmp) == 1) {
        logger.warn("Driver_Upgrade migrating!");
        migrating = true;
      }
    }

    if (migrating) {
      logger.warn("Driver_Upgrade migrating,try again later");
      throw new IllegalStateException("status upgrating");
    } else {
      logger.warn("Driver_Upgrade getTimestamp={}", "20170808");
      setFileContent(fpydLatest, "20170808");
    }
    lock.release();

    try {
      lock = getFileLock(fpydLatest);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    boolean res = false;
    if (fpydLatest.exists()) {
      String tmp = getFileContent(fpydLatest);
      if (Integer.parseInt(tmp) == 20170808) {
        res = true;
        logger.warn("Driver_Upgrade  read success");
      }
    }
    lock.release();
    assert (res);

  }

  @Test
  public void testDeleteVersionFromFileName() {
    String originDatanodeName = "/var/testing/_packages/pengyun-datanode-2.8.0";
    String originCoordinatorName = "/var/testing/_packages"
        + "/pengyun-coordinator-2.8.0";
    String coordinatorTimestamp = "20220117125313";
    String expectDatanodeName = "/var/testing/_packages/pengyun-datanode";
    String expectCoordinatorName = "/var/testing/_packages/pengyun-coordinator-20220117125313";

    String generateDatanodeName = UntarProcessor
        .generateDeleteVersionFile(originDatanodeName, coordinatorTimestamp);
    String generateCoordinatorName = UntarProcessor
        .generateDeleteVersionFile(originCoordinatorName, coordinatorTimestamp);
    Assert.assertEquals(expectDatanodeName, generateDatanodeName);
    Assert.assertEquals(expectCoordinatorName, generateCoordinatorName);
  }

  @Test
  public void testReplace() {
    String version = "2.8.0-internal";
    String internalVersion = "-internal";
    version = version.substring(0, version.lastIndexOf(internalVersion));
    Assert.assertEquals(version, "2.8.0");
  }

}
