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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.app.thrift.ThriftAppEngine;
import py.app.thrift.ThriftProcessorFactory;
import py.dd.utils.DdUtils;
import py.dd.worker.LogCollector;

/**
 * xx.
 */
public class DeploymentDaemonAppEngine extends ThriftAppEngine {

  private static Logger logger = LoggerFactory.getLogger(DeploymentDaemonAppEngine.class);

  private Bootstrap bootstrap;
  private LogCollector logCollector;
  private boolean ftpSwitch;

  public DeploymentDaemonAppEngine(ThriftProcessorFactory processorFactory) {
    super(processorFactory);
  }

  public void setBootstrap(Bootstrap bootstrap) {
    this.bootstrap = bootstrap;
  }

  /**
   * xx.
   */
  public void start() throws Exception {
    super.start();

    logger.info("Init dd utils ...");
    DdUtils.init();

    if (!ftpSwitch) {
      logger.warn("ftp switch:{}, do not start ftp thread", ftpSwitch);
      return;
    }

    try {
      logCollector.start();
    } catch (Exception e) {
      logger.error("Caught an exception when start log collector", e);
      //throw e;
    }
  }


  public LogCollector getLogCollector() {
    return logCollector;
  }

  public void setLogCollector(LogCollector logCollector) {
    this.logCollector = logCollector;
  }

  public boolean isFtpSwitch() {
    return ftpSwitch;
  }

  public void setFtpSwitch(boolean ftpSwitch) {
    this.ftpSwitch = ftpSwitch;
  }

}
