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

package py.dd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * xx.
 */
@Configuration
@PropertySource({"classpath:config/ftp.properties"})
public class FtpServerConfiguration {

  @Value("${ftp.server.host}")
  private String hostName;

  @Value("${ftp.server.port}")
  private int port;

  @Value("${ftp.server.user.name}")
  private String userName;

  @Value("${ftp.server.password}")
  private String password;

  @Value("${ftp.server.path.root}")
  private String rootPath;

  @Value("${log.collect.delay}")
  private int delay;

  @Value("${log.collect.period}")
  private int period;

  @Value("${ftp.switch}")
  private boolean ftpSwitch;

  public boolean getFtpSwitch() {
    return ftpSwitch;
  }

  public void setFtpSwitch(boolean ftpSwitch) {
    this.ftpSwitch = ftpSwitch;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getRootPath() {
    return rootPath;
  }

  public void setRootPath(String rootPath) {
    this.rootPath = rootPath;
  }

  public int getDelay() {
    return delay;
  }

  public void setDelay(int delay) {
    this.delay = delay;
  }

  public int getPeriod() {
    return period;
  }

  public void setPeriod(int period) {
    this.period = period;
  }
}
