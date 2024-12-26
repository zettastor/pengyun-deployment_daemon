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
