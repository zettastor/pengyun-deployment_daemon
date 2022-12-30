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

import py.dd.DeploymentDaemonConfiguration;

/**
 * xx.
 */
public class DdTestUtils {

  /**
   * xx.
   */
  public static DeploymentDaemonConfiguration buildTestConfiguration() {
    DeploymentDaemonConfiguration ddConfig = new DeploymentDaemonConfiguration();
    ddConfig.setServicesRootPath("/tmp/test");
    ddConfig.setServicesPackagesPath("/tmp/test/tars");
    ddConfig.setServicesInstallationPath("/tmp/test/_packages");
    ddConfig.setServicesRunningPath("/tmp/test/packages");

    return ddConfig;
  }
}
