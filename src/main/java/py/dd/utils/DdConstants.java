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

package py.dd.utils;

/**
 * constants.
 */
public class DdConstants {

  // deploy path for deployment_daemon
  public static String DEPLOYMENT_DAEMON_DEPLOY_PATH = "/var/deployment_daemon";

  // deploy path for pengyun services
  public static String PENGYUN_SERVICES_DEPLOY_PATH = "/var/testing";

  // service default install path
  public static String DEFAULT_INSTALL_PATH = "/var/testing/_packages";

  // service default running path
  public static String DEFAULT_RUNNING_PATH = "/var/testing/packages";

  // default path to store service gz file
  public static String DEFAULT_SRC_PATH = "/var/testing/tars";

  // temporary path for to untar target file
  public static String UNTAR_DEST_DIR = "/tmp/";

  public static String CONFIG_DIR_NAME = "config";

  public static String LOG_DIR_NAME = "logs";

  public static String VAR_DIR_NAME = "var";

  public static String BACKUP_DIR_FOR_UPGRADE = "backup_for_upgrade";

  public static String getDefaultInstallPath() {
    return DEFAULT_INSTALL_PATH;
  }

  public static void setDefaultInstallPath(String defaultInstallPath) {
    DEFAULT_INSTALL_PATH = defaultInstallPath;
  }

  public static String getDefaultRunningPath() {
    return DEFAULT_RUNNING_PATH;
  }

  public static void setdefaultRunningPath(String defaultRunningPath) {
    DEFAULT_RUNNING_PATH = defaultRunningPath;
  }

  public static String getDefaultSrcPath() {
    return DEFAULT_SRC_PATH;
  }

  public static void setDefaultSrcPath(String defaultSrcPath) {
    DEFAULT_SRC_PATH = defaultSrcPath;
  }

  public static String getUntarDestDir() {
    return UNTAR_DEST_DIR;
  }

  public static void setUntarDestDir(String untarDestDir) {
    UNTAR_DEST_DIR = untarDestDir;
  }
}
