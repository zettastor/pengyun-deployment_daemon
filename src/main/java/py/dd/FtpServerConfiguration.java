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
