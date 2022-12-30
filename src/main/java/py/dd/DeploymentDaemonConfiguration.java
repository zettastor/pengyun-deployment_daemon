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

import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import py.common.PyService;
import py.dd.utils.DdConstants;
import py.processmanager.Pmdb;

/**
 * Configuration for service deployment daemon.
 */
@PropertySource({"classpath:config/deployment_daemon.properties"})
@Configuration
public class DeploymentDaemonConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(DeploymentDaemonConfiguration.class);
  @Value("${dd.app.name}")
  private String ddAppName;

  @Value("${dd.app.port}")
  private int ddAppPort;

  @Value("${shutdown.retry.times:3}")
  private int shutdownRetryTimes = 3;

  @Value("${service.sweeper.interval.ms:3000}")
  private long serviceSweeperIntervalMs = 3000;

  @Value("${dd.thread.pool.size:5}")
  private int ddThreadPoolSize = 5;

  @Value("${deployment.daemon.root.path:/var/deployment_daemon}")
  private String deploymentDaemonRootPath;

  @Value("${services.root.path:/var/testing}")
  private String servicesRootPath;

  @Value("${services.packages.path:/var/testing/tars}")
  private String servicesPackagesPath;

  @Value("${services.installation.path:/var/testing/_packages}")
  private String servicesInstallationPath;

  @Value("${services.running.path:/var/testing/packages}")
  private String servicesRunningPath;

  @Value("${services.config.dir.name:config}")
  private String servicesConfigDirName = "config";

  @Value("${services.scripts.dir.name:bin}")
  private String servicesScriptsDirName = "bin";

  @Value("${services.lib.dir.name:lib}")
  private String servicesLibDirName = "lib";

  @Value("${tomcat.root.dir.name:tomcat}")
  private String tomcatRootDirName = "tomcat";

  @Value("${iscsi.ctl.script:/etc/init.d/iscsi-target}")
  private String iscsiCtlScript = "/etc/init.d/iscsi-target";

  @Value("${name.of.script.to.wipeout.disk:WipeoutDisk.sh}")
  private String nameOfScriptToWipeoutDisk = "WipeoutDisk.sh";

  @Value("${iet.initiators.allow.file.path:/etc/iet/initiators.allow}")
  private String allowFilePath = "/etc/iet/initiators.allow";

  @Value("${iet.conf.file.path:/etc/iet/ietd.conf}")
  private String ietConfigFilePath = "/etc/iet/ietd.conf";

  @Value("${iet.unbind.nbd}")
  private String ietUnbindCmd = "/opt/pyd/pyd-client -f %s";

  //The string use to build path /var/testing/RecordPosition/System
  // for service SystemDaemon,when wipeout all will clear it
  @Value("${record.performance.system:System}")
  private String recordPerformanceSystem = "System";

  private String tmpDir = System.getProperty("java.io.tmpdir");

  //---------------network size----------------------
  //datanode max thrift network frame size
  @Value("${max.network.frame.size:17000000}")
  private int maxNetworkFrameSize = 17 * 1000 * 1000;

  @Value("${stop.lioservice.command}")
  private String stopLioserviceCommand = "service target stop";

  @Value("${clearConfig.command}")
  private String clearConfigCommand = "/usr/bin/targetcli clearconfig confirm=true";

  @Value("${saveConfig.command}")
  private String saveConfigCommand = "/usr/bin/targetcli saveconfig";

  @Value("${jps.command.path:/opt/jdk1.8.0_65/bin/jps}")
  private String jpsCommandPath = "/opt/jdk1.8.0_65/bin/jps";


  /*
   * timestamp used for coordinator and fsserver only which is related with driver upgrade
   * each operation should set timestamp first;
   * also used for drivercontainer deploy to update coordinator info;
   * must be set in request
   *
   */
  private String timestamp = null;

  private String fsserverTimestamp = null;

  private String coordinatorTimestamp = null;

  private String serverVersion = null;

  public String getServerVersion() {
    return serverVersion;
  }

  public void setServerVersion(String serverVersion) {
    this.serverVersion = serverVersion;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public String getFsserverTimestamp() {
    return fsserverTimestamp;
  }

  public void setFsserverTimestamp(String fsserverTimestamp) {
    this.fsserverTimestamp = fsserverTimestamp;
  }

  public String getCoordinatorTimestamp() {
    return coordinatorTimestamp;
  }

  public void setCoordinatorTimestamp(String coordinatorTimestamp) {
    this.coordinatorTimestamp = coordinatorTimestamp;
  }


  public String getRecordPerformanceSystem() {
    return recordPerformanceSystem;
  }

  public void setRecordPerformanceSystem(String recordPerformanceSystem) {
    this.recordPerformanceSystem = recordPerformanceSystem;
  }

  public int getMaxNetworkFrameSize() {
    return maxNetworkFrameSize;
  }

  public void setMaxNetworkFrameSize(int maxNetworkFrameSize) {
    this.maxNetworkFrameSize = maxNetworkFrameSize;
  }

  /**
   * timestamp used for driverUPgrade Coordinator for NBD FSServer for FSD.
   */
  public Path buildServiceRunningPath(PyService service) {
    if (service == PyService.COORDINATOR) {
      logger.debug("buildServiceRunningPath {} {} {}", servicesRunningPath,
          service.getServiceProjectKeyName(), coordinatorTimestamp);
      return buildServiceRunningPath(service, coordinatorTimestamp);
    }  else {
      return Paths.get(servicesRunningPath, service.getServiceProjectKeyName());
    }
  }

  public Path buildServiceRunningPath(PyService service, String timestamp) {
    return Paths.get(servicesRunningPath,
        String.format("%s-%s", service.getServiceProjectKeyName(), timestamp));
  }

  /**
   * timestamp used for driverUPgrade Coordinator for NBD FSServer for FSD.
   */
  public Path buildServicePackagePath(PyService service, String version) {
    if (service == PyService.COORDINATOR) {
      logger.debug("buildServicePackagePath {} {} {}", servicesPackagesPath,
          service.getServiceProjectKeyName(), coordinatorTimestamp);
      return buildServicePackagePath(service, version, coordinatorTimestamp);
    }  else {
      return Paths.get(servicesPackagesPath,
          String.format("%s.tar.gz", service.getServiceProjectKeyName()));
    }
  }

  public Path buildServicePackagePath(PyService service, String version, String timestamp) {
    return Paths.get(servicesPackagesPath,
        String.format("%s-%s.tar.gz", service.getServiceProjectKeyName(), timestamp));
  }

  /**
   * timestamp used for driverUPgrade Coordinator for NBD FSServer for FSD.
   */
  public Path buildServiceInstallationPath(PyService service, String version) {
    version = version.split("-")[0];
    if (service == PyService.COORDINATOR) {
      logger.debug("buildServiceInstallationPath {} {} {}", service, version, coordinatorTimestamp);
      return buildServiceInstallationPath(service, version, coordinatorTimestamp);
    } else {
      return Paths
          .get(servicesInstallationPath, String.format("%s", service.getServiceProjectKeyName()));
    }
  }

  /**
   * xx.
   */
  public Path buildServiceInstallationPath(PyService service, String version, String timestamp) {
    version = version.split("-")[0];
    return Paths.get(servicesInstallationPath,
        String.format("%s-%s", service.getServiceProjectKeyName(), timestamp));
  }

  /**
   * xx.
   */
  public Path buildServicesConfigurationPath(PyService service, String version) {
    return Paths
        .get(buildServiceInstallationPath(service, version).toString(), servicesConfigDirName);
  }

  /**
   * xx.
   */
  public Path buildServicesScriptsPath(PyService service) {
    return Paths.get(buildServiceRunningPath(service).toString(), servicesScriptsDirName);
  }

  /**
   * xx.
   */
  public Path buildServicesLibPath(PyService service) {
    return Paths.get(buildServiceRunningPath(service).toString(), servicesLibDirName);
  }

  /**
   * xx.
   */
  public Path buildTomcatScriptsPath() {
    return Paths.get(buildServiceRunningPath(PyService.CONSOLE).toString(), tomcatRootDirName,
        servicesScriptsDirName);
  }

  /**
   * xx.
   */
  public Path buildConfigFilePath(PyService service, String version, String fileName) {
    return Paths.get(buildServicesConfigurationPath(service, version).toString(), fileName);
  }

  /**
   * xx.
   */
  public Path buildTmpInstallationPath(PyService service, String version) {
    version = version.split("-")[0];
    return Paths.get(tmpDir, String.format("%s", service.getServiceProjectKeyName()));
  }


  /**
   * xx.
   */
  public Path buildTmpConfigFilePath(PyService service, String version, String fileName) {
    version = version.split("-")[0];
    if (service == PyService.COORDINATOR) {
      return Paths.get(buildTmpInstallationPath(service, version).toString(),
          String.format("%s-%s", service.getServiceProjectKeyName(), coordinatorTimestamp),
          servicesConfigDirName, fileName);
    } else {
      return Paths.get(buildTmpInstallationPath(service, version).toString(),
          String.format("%s", service.getServiceProjectKeyName()), servicesConfigDirName, fileName);
    }
  }


  public Path buildServiceStatusFilePath(PyService service) {
    return Paths.get(buildServiceRunningPath(service).toString(), Pmdb.SERVICE_STATUS_NAME);
  }

  public Path buildServiceStatusFileBakPath(PyService service) {
    return Paths.get(buildServiceRunningPath(service).toString(), Pmdb.SERVICE_STATUS_NAME_BAK);
  }

  public Path buildBackupPath() {
    return Paths.get(servicesInstallationPath, DdConstants.BACKUP_DIR_FOR_UPGRADE);
  }


  public String buildIscsiStartCmd() {
    return String.format("%s start", iscsiCtlScript);
  }

  public String buildIscsiStopCmd() {
    return String.format("%s stop", iscsiCtlScript);
  }

  public String getDeploymentDaemonRootPath() {
    return deploymentDaemonRootPath;
  }

  public void setDeploymentDaemonRootPath(String deploymentDaemonRootPath) {
    this.deploymentDaemonRootPath = deploymentDaemonRootPath;
  }

  public String getServicesRootPath() {
    return servicesRootPath;
  }

  public void setServicesRootPath(String servicesRootPath) {
    this.servicesRootPath = servicesRootPath;
  }

  public String getServicesPackagesPath() {
    return servicesPackagesPath;
  }

  public void setServicesPackagesPath(String servicesPackagesPath) {
    this.servicesPackagesPath = servicesPackagesPath;
  }

  public String getServicesInstallationPath() {
    return servicesInstallationPath;
  }

  public void setServicesInstallationPath(String servicesInstallationPath) {
    this.servicesInstallationPath = servicesInstallationPath;
  }

  public String getServicesRunningPath() {
    return servicesRunningPath;
  }

  public void setServicesRunningPath(String servicesRunningPath) {
    this.servicesRunningPath = servicesRunningPath;
  }

  public String getServicesConfigDirName() {
    return servicesConfigDirName;
  }

  public void setServicesConfigDirName(String servicesConfigDirName) {
    this.servicesConfigDirName = servicesConfigDirName;
  }

  public String getServicesScriptsDirName() {
    return servicesScriptsDirName;
  }

  public void setServicesScriptsDirName(String servicesScriptsDirName) {
    this.servicesScriptsDirName = servicesScriptsDirName;
  }

  public String getTomcatRootDirName() {
    return tomcatRootDirName;
  }

  public void setTomcatRootDirName(String tomcatRootDirName) {
    this.tomcatRootDirName = tomcatRootDirName;
  }

  public String getTmpDir() {
    return tmpDir;
  }

  public void setTmpDir(String tmpDir) {
    this.tmpDir = tmpDir;
  }

  public String getIscsiCtlScript() {
    return iscsiCtlScript;
  }

  public void setIscsiCtlScript(String iscsiCtlScript) {
    this.iscsiCtlScript = iscsiCtlScript;
  }

  public String getDdAppName() {
    return ddAppName;
  }

  public void setDdAppName(String ddAppName) {
    this.ddAppName = ddAppName;
  }

  public int getDdAppPort() {
    return ddAppPort;
  }

  public void setDdAppPort(int ddAppPort) {
    this.ddAppPort = ddAppPort;
  }

  public int getShutdownRetryTimes() {
    return shutdownRetryTimes;
  }

  public void setShutdownRetryTimes(int shutdownRetryTimes) {
    this.shutdownRetryTimes = shutdownRetryTimes;
  }

  public long getServiceSweeperIntervalMs() {
    return serviceSweeperIntervalMs;
  }

  public void setServiceSweeperIntervalMs(long serviceSweeperIntervalMs) {
    this.serviceSweeperIntervalMs = serviceSweeperIntervalMs;
  }

  public int getDdThreadPoolSize() {
    return ddThreadPoolSize;
  }

  public void setDdThreadPoolSize(int ddThreadPoolSize) {
    this.ddThreadPoolSize = ddThreadPoolSize;
  }

  public String getAllowFilePath() {
    return allowFilePath;
  }

  public void setAllowFilePath(String allowFilePath) {
    this.allowFilePath = allowFilePath;
  }

  public String getIetConfigFilePath() {
    return ietConfigFilePath;
  }

  public void setIetConfigFilePath(String ietConfigFilePath) {
    this.ietConfigFilePath = ietConfigFilePath;
  }

  public String getNameOfScriptToWipeoutDisk() {
    return nameOfScriptToWipeoutDisk;
  }

  public void setNameOfScriptToWipeoutDisk(String nameOfScriptToWipeoutDisk) {
    this.nameOfScriptToWipeoutDisk = nameOfScriptToWipeoutDisk;
  }


  public String getSaveConfigCommand() {
    return saveConfigCommand;
  }

  public void setSaveConfigCommand(String saveConfigCommand) {
    this.saveConfigCommand = saveConfigCommand;
  }

  public String getClearConfigCommand() {
    return clearConfigCommand;
  }

  public void setClearConfigCommand(String clearConfigCommand) {
    this.clearConfigCommand = clearConfigCommand;
  }

  public String getStopLioserviceCommand() {
    return stopLioserviceCommand;
  }

  public void setStopLioserviceCommand(String stopLioserviceCommand) {
    this.stopLioserviceCommand = stopLioserviceCommand;
  }

  public String getIetUnbindCmd() {
    return ietUnbindCmd;
  }

  public void setIetUnbindCmd(String ietUnbindCmd) {
    this.ietUnbindCmd = ietUnbindCmd;
  }

  public String getJpsCommandPath() {
    return jpsCommandPath;
  }
}
