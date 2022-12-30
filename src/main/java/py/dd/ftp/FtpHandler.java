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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.struct.EndPoint;

/**
 * this class is used to do FTP download & upload & so on
 *
 * <p>CAUTION: close the ftp connection if after you.
 */
public class FtpHandler {

  private static final Logger logger = LoggerFactory.getLogger(FtpHandler.class);
  private static final String DIRECTORY_SEPERATOR = "/";
  private static String encoding = System.getProperty("file.encoding");
  private final String host;
  private final int port;
  private final String user;
  private final String password;
  private FTPClient ftpClient;

  /**
   * Constructor.
   *
   * @param host host name of FTP server
   * @param port port of the FTP service
   */
  public FtpHandler(String host, int port, String user, String password) {
    this.host = host;
    this.port = port;
    this.user = user;
    this.password = password;
    this.ftpClient = new FTPClient();
  }

  /**
   * Constructor.
   *
   * @param endpoint end-point of the FTP server
   */
  public FtpHandler(EndPoint endpoint, String user, String password) {
    this.host = endpoint.getHostName();
    this.port = endpoint.getPort();
    this.user = user;
    this.password = password;
    this.ftpClient = new FTPClient();
  }

  /**
   * xx.
   */
  public void connect() throws IOException {
    ftpClient = new FTPClient();
    ftpClient.setControlEncoding(encoding);
    ftpClient.connect(host, port);
    ftpClient.login(user, password);
    ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);

    checkConnection();
  }

  /**
   * xx.
   */
  public void disconnect() throws IOException {
    try {
      if (ftpClient != null) {
        ftpClient.disconnect();
      }
    } catch (IOException e) {
      logger.error("Failed to disconnect the connection from ftp server", e);
      throw e;
    }
  }

  /**
   * Description: upload files to FTP server.
   *
   * @param path     path in remote FTP
   * @param filename the name of the file that will be place in the FTP server
   * @param input    local file input stream
   */
  public void uploadFile(String path, String filename, InputStream input) throws IOException {
    try {
      this.connect();
      checkConnection();

      // do upload
      boolean change = ftpClient.changeWorkingDirectory(path);
      // logger.debug("change is {}", change);
      ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
      if (change) {
        ftpClient.storeFile(new String(filename.getBytes(encoding), "iso-8859-1"), input);
      }
      input.close();
      ftpClient.logout();
    } catch (IOException e) {
      logger.error("The FTP client has been disconnected from the server", e);
      throw e;
    } finally {
      this.disconnect();
    }
  }

  /**
   * xx.
   */
  public void uploadFolder(String localPath, String ftpPath) throws IOException {
    try {
      this.connect();

      // walk through all files in the local path, and push them to the FTP server.
      Files.walk(Paths.get(localPath)).forEach(filePath -> {
        if (Files.isRegularFile(filePath)) {
          // upload to FTP server
          try {
            File fileToBeUpload = filePath.toFile();
            logger.debug("Going to upload file [{}] to [{}]", fileToBeUpload, ftpPath);
            FileInputStream in = new FileInputStream(fileToBeUpload);
            uploadFile(ftpPath, fileToBeUpload.getName(), in);
          } catch (Exception e) {
            logger.error("Failed to upload file {}", filePath);
            // do not throw exception out here, but just continue.
          }
        }
      });
    } catch (IOException e) { // this exception is catching for Files.walk.
      logger.error("Caught an exception", e);
      throw e;
    } finally {
      this.disconnect();
    }
  }

  /**
   * Description: download files from FTP servers.
   *
   * @param remotePath path in remote FTP
   * @param fileName   the name of the file which is going to be download
   * @param localPath  the path in the local machine where to place the file.
   */
  public void downloadFile(String remotePath, String fileName, String localPath)
      throws IOException {
    try {
      this.connect();
      // move to the path in the FTP server
      ftpClient.changeWorkingDirectory(new String(remotePath.getBytes(encoding), "iso-8859-1"));
      FTPFile[] fs = ftpClient.listFiles();
      for (FTPFile ff : fs) {
        if (ff.getName().equals(fileName)) {
          File localFile = new File(localPath + "/" + ff.getName());
          OutputStream is = new FileOutputStream(localFile);
          ftpClient.retrieveFile(ff.getName(), is);
          is.close();
        }
      }

      ftpClient.logout();
    } catch (IOException e) {
      logger.error("Caught an exception", e);
      throw e;
    } finally {
      this.disconnect();
    }
  }

  /**
   * xx.
   */
  public void createDirectory(String path) throws IOException {
    try {
      this.connect();
      List<String> tokens = new ArrayList<String>();
      StringTokenizer st = new StringTokenizer(path, DIRECTORY_SEPERATOR);
      while (st.hasMoreElements()) {
        tokens.add(st.nextToken());
      }

      if (tokens.size() == 0) {
        return;
      }

      // find out if the directory is already existed
      String directoryToBeCreated = tokens.get(tokens.size() - 1);
      String higherLevelDirectory = path.substring(0, path.indexOf(directoryToBeCreated));
      List<String> alreadyCreatedDirectories = getFileList(higherLevelDirectory);
      logger.debug("higherLevelDirectory is {}, directoryToBeCreated is {}", higherLevelDirectory,
          directoryToBeCreated);
      if (alreadyCreatedDirectories.contains(directoryToBeCreated)) {
        logger.warn("{} is already existed", path);
        return;
      }

      boolean result = ftpClient.makeDirectory(path);
      if (!result) {
        throw new IOException();
      }
    } catch (IOException e) {
      logger.error("Caught an exception when list all ftp filenames from ftp server", e);
      throw new IOException();
    } finally {
      this.disconnect();
    }
  }

  private void checkConnection() throws IOException {
    // make sure that we connect FTP server successfully.
    int reply = ftpClient.getReplyCode();
    if (!FTPReply.isPositiveCompletion(reply)) {
      disconnect();
    }
    logger.debug("connection is OK");
  }

  private List<String> getFileList(String path) throws IOException {
    List<String> fileList = new ArrayList<String>();
    try {
      FTPFile[] ftpFiles = ftpClient.listFiles(path);
      for (int i = 0; i < ftpFiles.length; ++i) {
        fileList.add(ftpFiles[i].getName());
      }
    } catch (IOException e) {
      logger.error("Caught an exception when list all ftp filenames from ftp server", e);
      throw new IOException();
    }
    return fileList;
  }

}
