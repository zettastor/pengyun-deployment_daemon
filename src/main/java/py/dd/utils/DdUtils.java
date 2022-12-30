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

package py.dd.utils;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.OsCmdExecutor;
import py.common.OsCmdExecutor.OsCmdOutputLogger;
import py.common.OsCmdExecutor.OsCmdStreamConsumer;
import py.common.PyService;
import py.processmanager.Pmdb;
import py.processmanager.exception.PmdbPathNotExist;

/**
 * A class collects utils for deployment daemon.
 */
public class DdUtils {

  private static final Logger logger = LoggerFactory.getLogger(DdUtils.class);
  /**
   * There are always 2 threads in pool. One is for STDOUT stream consumer, and another is for
   * STDERR stream consumer.
   */
  private static final int OS_CMD_THREAD_POOL_SIZE = 2;
  public static ExecutorService osCMDThreadPool;

  /**
   * xx.
   */
  public static String getLocalHost() {
    InetAddress inetAddress;
    try {
      inetAddress = InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      logger.error("caught an exception", e);
      return null;
    }
    return inetAddress.getHostAddress();
  }

  private static void bindPort(String host, int port) throws Exception {
    Socket s = new Socket();
    s.bind(new InetSocketAddress(host, port));
    s.close();
  }

  /**
   * xx.
   */
  public static boolean isPortAvailable(int port) {
    try {
      bindPort("0.0.0.0", port);
      bindPort(InetAddress.getLocalHost().getHostAddress(), port);
      return true;
    } catch (Throwable t) {
      logger.trace("port {} is most likely bound,the exception is:{}", port, t);
      return false;
    }
  }

  /**
   * get process pid for Pmdb.
   *
   * @param name type of process, service process or pm process
   */
  public static int getProcessPid(String serviceRunningPath, String name) {
    int pid = 0;
    try {
      Pmdb pmdb = Pmdb.build(Paths.get(serviceRunningPath));
      String str = pmdb.get(name);
      if (str != null) {
        pid = Integer.valueOf(str);
      }
    } catch (PmdbPathNotExist e) {
      logger.warn("caught an exception", e);
    }
    return pid;
  }

  /**
   * List all activation scripts whose name started with digit number.
   */
  public static List<File> listActivationScripts(Path targetPath) {
    List<File> activationScriptList = new ArrayList<File>();

    File targetDir = targetPath.toFile();
    if (!targetDir.exists() || !targetDir.isDirectory()) {
      return activationScriptList;
    }

    File[] scripts = targetDir.listFiles();
    if (scripts == null || scripts.length == 0) {
      return activationScriptList;
    }

    for (File script : scripts) {
      script.setExecutable(true, true);
      if (!isActivateScript(script.getName())) {
        continue;
      }
      logger.debug("add file {} to activation script list", script.getName());
      activationScriptList.add(script);
    }

    Collections.sort(activationScriptList, new Comparator<File>() {

      @Override
      public int compare(File f1, File f2) {
        int in1 = Integer.valueOf(f1.getName().substring(0, 3));
        int in2 = Integer.valueOf(f2.getName().substring(0, 3));
        return in1 - in2;
      }
    });

    return activationScriptList;
  }

  /**
   * Check if a script is activation script whose name start with digit numbers.
   */
  public static boolean isActivateScript(String name) {
    if (name.length() > 3) {
      if (Character.isDigit(name.charAt(0)) && Character.isDigit(name.charAt(1))
          && Character.isDigit(name.charAt(2))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Execute activation scripts under terminal.
   */
  public static void executeScript(File script) throws IOException {
    logger.debug("Execute script: {}", script.getAbsoluteFile());

    // make the script runnable
    script.setExecutable(true, true);

    final String oscmd = "bin" + File.separator + script.getName();
    int exitCode;
    OsCmdOutputLogger stdoutLogger = new OsCmdOutputLogger(logger, oscmd);
    OsCmdOutputLogger stderrLogger = new OsCmdOutputLogger(logger, oscmd);

    stdoutLogger.setErrorStream(false);
    stderrLogger.setErrorStream(true);

    try {
      exitCode = OsCmdExecutor
          .exec(oscmd, null, script.getParentFile().getParentFile(), stdoutLogger,
              stderrLogger);
    } catch (InterruptedException e) {
      throw new IOException(e);
    }

    if (exitCode != 0) {
      logger.error("Something wrong when execute script {}", script.getAbsolutePath());
      throw new IOException();
    }
  }

  public static boolean isRunnable(PyService service) {
    return !service.getServiceLauchingScriptName().isEmpty();
  }

  /**
   * xx.
   */
  public static int getPmPidFromCommandByServiceName(PyService pyService, String jpsCommandPath)
      throws IOException {
    Validate.notNull(pyService);
    logger.warn("get pid from command by service:{}", pyService.getServiceName());
    GetPmPidCmdStreamConsumer cmdStreamConsumer = new GetPmPidCmdStreamConsumer(
        pyService);

    execCommandWithTimeoutAndConsume(jpsCommandPath, cmdStreamConsumer);

    int pid = cmdStreamConsumer.getPmPid();
    logger.warn("after exec command, got pid:{}", pid);
    return pid;
  }

  /**
   * xx.
   */
  public static void getManyPmPidFromCommandByServiceNameMap(Map<PyService, Integer> pmPidMap,
      String jpsCommandPath)
      throws IOException {
    Validate.notNull(pmPidMap);
    Validate.notEmpty(pmPidMap);
    logger.info("get many PMPid from command by services:{}", pmPidMap.keySet());
    GetManyPmPidCmdStreamConsumer cmdStreamConsumer = new GetManyPmPidCmdStreamConsumer(
        pmPidMap);

    execCommandWithTimeoutAndConsume(jpsCommandPath, cmdStreamConsumer);

    logger.warn("after exec command, got PMPid:{}", cmdStreamConsumer.getPmPidMap());
    return;
  }

  /**
   * xx.
   */
  public static int execCommandWithTimeoutAndConsume(String command,
      OsCmdStreamConsumer oscmdStreamConsumer) throws IOException {
    Validate.notNull(command);
    String execCommand = String.format("bash %s", command);
    logger.warn("going to exec command:{}", execCommand);
    int exitCode;

    OsCmdOutputLogger stderrLogger = new OsCmdOutputLogger(logger, execCommand);
    try {
      exitCode = OsCmdExecutor.execWithTimeoutMs(execCommand, oscmdStreamConsumer,
          stderrLogger, 5000);
    } catch (InterruptedException | IOException e) {
      logger.error("exec command:{} caught an exception", execCommand, e);
      throw new IOException(e);
    }
    if (exitCode != 0) {
      logger.warn("command:{} exit code:{} not zero", execCommand, exitCode);
    }
    return exitCode;
  }

  /**
   * DD has some operation(s) on file system. And it is necessary to sync file system due to cache
   * of it.
   */
  public static void syncFs() throws IOException, InterruptedException {
    long startTime;
    long endTime;

    startTime = System.currentTimeMillis();

    Process process = Runtime.getRuntime().exec("sync");
    if (process != null) {
      process.waitFor();
    }

    endTime = System.currentTimeMillis();

    logger.debug("It takes {} millis to sync file system!", (endTime - startTime));
  }

  /**
   * xx.
   */
  public static void init() {
    osCMDThreadPool = Executors.newFixedThreadPool(OS_CMD_THREAD_POOL_SIZE, new ThreadFactory() {

      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, "Os CMD Consumer");
      }
    });
  }

  public static void destroy() {
    osCMDThreadPool.shutdown();
  }
}
