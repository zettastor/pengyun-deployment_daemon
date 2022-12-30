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
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import py.processmanager.ProcessManagerMutex;

/**
 * xx.
 */
public class Launcher {

  private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

  /**
   * xx.
   */
  public static void main(String[] args) throws Exception {

    // avoid more than one same process processing
    if (ProcessManagerMutex.checkIfAlreadyRunning(System.getProperty("user.dir"))) {
      try {
        ApplicationContext context = new AnnotationConfigApplicationContext(
            DeploymentDaemonAppBeans.class);
        DeploymentDaemonAppEngine engine = context.getBean(DeploymentDaemonAppEngine.class);

        logger.debug("Going to start deployment service..");
        engine.start();

      } catch (Exception e) {
        logger.error("caught an exception", e);
        System.exit(1);
      }
    } else {
      logger.error("exit due to the same process is processing ");
      System.exit(1);
    }
  }
}
