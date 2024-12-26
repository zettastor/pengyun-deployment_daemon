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

package py.dd.test.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.DeploymentDaemonClientFactory;
import py.thrift.deploymentdaemon.DeploymentDaemon;

/**
 * xx.
 */
public class DdClientBuilder {

  private static final Logger logger = LoggerFactory.getLogger(DdClientBuilder.class);

  private DeploymentDaemon.Iface ddClient;

  /**
   * xx.
   */
  public DeploymentDaemon.Iface build() throws Exception {
    DeploymentDaemonClientFactory clientFactory = new DeploymentDaemonClientFactory(1);
    ddClient = clientFactory.build("localhost", 10002).getClient();

    ddClient.ping();

    logger.info("dd client built successfully");
    return ddClient;
  }
}
