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
