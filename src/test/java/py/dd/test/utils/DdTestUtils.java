

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
