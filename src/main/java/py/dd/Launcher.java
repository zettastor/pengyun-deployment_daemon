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
