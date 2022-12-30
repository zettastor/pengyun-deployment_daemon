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
