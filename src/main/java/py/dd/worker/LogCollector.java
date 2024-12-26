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

package py.dd.worker;

import java.util.Map;
import java.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.struct.EndPoint;
import py.dd.service.store.ServiceStore;

/**
 * This class is use to create a schedular service to push all of the service logs to FTP server
 * {@code MBeanServer}.
 */
public class LogCollector {

  private static final Logger logger = LoggerFactory.getLogger(LogCollector.class);
  private Timer timer;
  private long delay;
  private long period;
  private LogCollectorTask timerTask;

  private LogCollector(LogCollectorTask timerTask, Timer timer, long delay, long period) {
    this.timer = timer;
    this.timerTask = timerTask;
    this.delay = delay;
    this.period = period;
  }

  public static Builder builder(EndPoint server, String userName, String password) {
    return new Builder(server, userName, password);
  }

  public void start() throws Exception {
    timer.schedule(timerTask, delay, period);
  }

  /**
   * xx.
   */
  public void stop() throws Exception {
    if (timerTask.cancel() == false) {
      throw new Exception("Failed to stop the task as a timer task");
    }

    timer.cancel();
  }

  /**
   * xx.
   */
  public static class Builder {

    private final EndPoint ftpServer;
    private final String userName;
    private final String password;
    private Timer timer;
    private long delay = 0;
    private long period = 1000;
    private String basePath = "/ftp-files";
    private ServiceStore serviceStore;
    private Map<String, String> logNamesForeachService;

    /**
     * xx.
     */
    public Builder(EndPoint ftpServer, String userName, String password) {
      this.ftpServer = ftpServer;
      this.userName = userName;
      this.password = password;
    }

    public Builder bindTimer(Timer timer) {
      this.timer = timer;
      return this;
    }

    public Builder delay(long delay) {
      this.delay = delay;
      return this;
    }

    public Builder period(long period) {
      this.period = period;
      return this;
    }

    public Builder basePath(String basePath) {
      this.basePath = basePath;
      return this;
    }

    public Builder serviceStore(ServiceStore serviceStore) {
      this.serviceStore = serviceStore;
      return this;
    }

    public Builder logNamesForeachService(Map<String, String> logNamesForeachService) {
      this.logNamesForeachService = logNamesForeachService;
      return this;
    }

    /**
     * <code>
     * public Builder userName(String userName) { this.userName = userName; return this; }
     * public Builder password(String password) { this.password = password; return this; }
     * </code>.
     */
    public LogCollector build() throws Exception {
      if (timer == null) {
        logger.warn("It is NOT good enough to create a timer for each LogCollector");
        timer = new Timer();
      }

      LogCollectorTask timerTask = new LogCollectorTask(ftpServer, userName, password, basePath,
          serviceStore, logNamesForeachService);
      return new LogCollector(timerTask, timer, delay, period);
    }
  }

}
