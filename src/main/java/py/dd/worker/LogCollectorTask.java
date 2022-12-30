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

package py.dd.worker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.struct.EndPoint;
import py.dd.common.ServiceMetadata;
import py.dd.ftp.FtpHandler;
import py.dd.ftp.NoServiceException;
import py.dd.service.store.ServiceStore;

/**
 * This {@code TimerTask} is used to push logs from all services
 *
 * <p>this is just a helper class of {@code AttributePuller}.
 */
public class LogCollectorTask extends TimerTask {

  private static final Logger logger = LoggerFactory.getLogger(LogCollectorTask.class);
  private static final char DIRECTORY_SEPERATOR = '/';

  private final EndPoint ftpServer;
  private final String userName;
  private final String password;
  private final String basePath;

  private ServiceStore serviceStore;
  private Map<String, String> logNamesForeachService;

  /**
   * The client coder needn't care the creation of the puller-task. {@code AttributePullerTask} will
   * be created {@code AttributePuller}. so I turn the constructor from "public" to "protected"
   */
  protected LogCollectorTask(EndPoint ftpServer, String user, String password, String basePath,
      ServiceStore serviceStore, Map<String, String> logNamesForeachService) throws IOException {
    this.ftpServer = ftpServer;
    this.userName = user;
    this.password = password;
    this.basePath = basePath;
    this.serviceStore = serviceStore;
    this.logNamesForeachService = logNamesForeachService;
  }

  @Override
  public void run() {
    logger.debug("scheduling push service logs to FTP server : {}", ftpServer);

    FtpHandler ftpHandler = new FtpHandler(ftpServer, userName, password);
    try {
      // connect to ftp server
      ftpHandler.connect();
      logger.debug("Connect to FTP server [{}] successful", ftpServer);

      // get local host name
      String localHostName = getLocalHostName();
      logger.debug("Host name is {}", localHostName);

      // get all services in local host.
      List<ServiceMetadata> existingServiceList = serviceStore.list();
      if (existingServiceList == null || existingServiceList.isEmpty()) {
        logger.error("No services in local host to push to the FTP server {}", this.ftpServer);
        throw new NoServiceException();
      }

      String tmpBasePath = this.basePath;
      if (tmpBasePath.charAt(tmpBasePath.length() - 1) != DIRECTORY_SEPERATOR) {
        tmpBasePath += DIRECTORY_SEPERATOR;
      }
      logger.debug("tmpBasePath is {}", tmpBasePath);

      for (ServiceMetadata service : existingServiceList) {
        try {
          String directoryToBeCreated = tmpBasePath + service.getServiceName();

          // create a service level directory named local host name in FTP server
          logger.debug("Going to create service level directory : {}", directoryToBeCreated);
          ftpHandler.createDirectory(directoryToBeCreated);

          // create a service level instance level directory
          directoryToBeCreated += DIRECTORY_SEPERATOR + localHostName;
          logger.debug("Going to create instance level directory : {}", directoryToBeCreated);
          ftpHandler.createDirectory(directoryToBeCreated);

          // push all service logs to FTP server
          logger.debug("Going to push all service's logs to the FTP server");

          // get local log file folder
          String localLogFileFolder = logNamesForeachService.get(service.getServiceName());
          ftpHandler.uploadFolder(localLogFileFolder, directoryToBeCreated);
        } catch (Exception e) {
          logger.warn("Caught an exception.", e);
          // do not throw exception out but just continue;
        }
      }
    } catch (IOException e) {
      logger.error("Caught an exception for Log Collector Task", e);
    } catch (NoServiceException e) {
      logger.error("Caught an exception", e);
    }
  }

  private String getLocalHostName() throws UnknownHostException {
    InetAddress ia;
    try {
      ia = InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      logger.error("Caught an exception", e);
      throw e;
    }

    String localname = ia.getHostName();
    String localip = ia.getHostAddress();
    return String.format("%s[%s]", localname, localip);
  }

}
