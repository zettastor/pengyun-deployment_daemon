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

package py.dd.service;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.apache.commons.io.FileUtils;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.AbstractConfigurationServer;
import py.app.thrift.ThriftProcessorFactory;
import py.common.PyService;
import py.dd.Bootstrap;
import py.dd.DeploymentDaemonConfiguration;
import py.dd.RequestResponseHelper;
import py.dd.common.ServiceMetadata;
import py.dd.common.ServiceStatus;
import py.dd.processor.chain.ProcessorChainFactory;
import py.dd.service.store.ServiceStore;
import py.dd.utils.DdConstants;
import py.dd.utils.DdUtils;
import py.driver.DriverType;
import py.drivercontainer.driver.version.Version;
import py.drivercontainer.driver.version.VersionException;
import py.drivercontainer.driver.version.VersionManager;
import py.drivercontainer.driver.version.file.VersionImpl;
import py.drivercontainer.driver.version.file.VersionManagerImpl;
import py.periodic.PeriodicWorkExecutor;
import py.periodic.UnableToStartException;
import py.thrift.deploymentdaemon.ActivateRequest;
import py.thrift.deploymentdaemon.ActivateResponse;
import py.thrift.deploymentdaemon.BackupKeyRequest;
import py.thrift.deploymentdaemon.BackupKeyResponse;
import py.thrift.deploymentdaemon.ChangeConfigurationRequest;
import py.thrift.deploymentdaemon.ChangeConfigurationResponse;
import py.thrift.deploymentdaemon.ConfigurationNotFoundExceptionThrift;
import py.thrift.deploymentdaemon.DeactivateRequest;
import py.thrift.deploymentdaemon.DeactivateResponse;
import py.thrift.deploymentdaemon.DeploymentDaemon;
import py.thrift.deploymentdaemon.DestroyRequest;
import py.thrift.deploymentdaemon.DestroyResponse;
import py.thrift.deploymentdaemon.DriverIsAliveExceptionThrift;
import py.thrift.deploymentdaemon.DriverUpgradeExceptionThrift;
import py.thrift.deploymentdaemon.FailedToActivateServiceExceptionThrift;
import py.thrift.deploymentdaemon.FailedToChangeConfigurationExceptionThrift;
import py.thrift.deploymentdaemon.FailedToDeactivateServiceExceptionThrift;
import py.thrift.deploymentdaemon.FailedToPrepareWorkspaceThrift;
import py.thrift.deploymentdaemon.FailedToStartServiceExceptionThrift;
import py.thrift.deploymentdaemon.FailedToWipeoutExceptionThrift;
import py.thrift.deploymentdaemon.GetStatusRequest;
import py.thrift.deploymentdaemon.GetStatusResponse;
import py.thrift.deploymentdaemon.GetUpgradeStatusRequest;
import py.thrift.deploymentdaemon.GetUpgradeStatusResponse;
import py.thrift.deploymentdaemon.PrepareWorkspaceRequest;
import py.thrift.deploymentdaemon.PrepareWorkspaceResponse;
import py.thrift.deploymentdaemon.PutTarRequest;
import py.thrift.deploymentdaemon.PutTarResponse;
import py.thrift.deploymentdaemon.ServiceIsBusyExceptionThrift;
import py.thrift.deploymentdaemon.ServiceNotFoundExceptionThrift;
import py.thrift.deploymentdaemon.ServiceNotRunnableExceptionThrift;
import py.thrift.deploymentdaemon.ServiceStatusIsErrorExceptionThrift;
import py.thrift.deploymentdaemon.StartRequest;
import py.thrift.deploymentdaemon.StartResponse;
import py.thrift.deploymentdaemon.UpdateLatestVersionRequest;
import py.thrift.deploymentdaemon.UpdateLatestVersionResponse;
import py.thrift.deploymentdaemon.UseBackupKeyRequest;
import py.thrift.deploymentdaemon.UseBackupKeyResponse;
import py.thrift.deploymentdaemon.WipeoutRequest;
import py.thrift.deploymentdaemon.WipeoutResponse;
import py.thrift.share.ServiceMetadataThrift;
import py.thrift.share.UpgradeInfoThrift;

/**
 * A class to implement dd interface.
 */
public class DeploymentDaemonImpl extends AbstractConfigurationServer
    implements DeploymentDaemon.Iface, ThriftProcessorFactory {

  private static final Logger logger = LoggerFactory.getLogger(DeploymentDaemonImpl.class);

  private final DeploymentDaemon.Processor<DeploymentDaemon.Iface> processor;

  private Bootstrap bootstrap;

  private ServiceStore serviceStore;

  private DeploymentDaemonConfiguration ddConfig;

  private ProcessorChainFactory processorChainFactory;

  private PeriodicWorkExecutor sweepExecutor;

  private ExecutorService ddThreadPool;

  private ServiceMonitor systemMonitor;

  private VersionManagerImpl versionManager;

  public DeploymentDaemonImpl() {
    processor = new DeploymentDaemon.Processor<DeploymentDaemon.Iface>(this);
  }

  public Bootstrap getBootstrap() {
    return bootstrap;
  }

  public void setBootstrap(Bootstrap bootstrap) {
    this.bootstrap = bootstrap;
  }

  public ServiceStore getServiceStore() {
    return serviceStore;
  }

  public void setServiceStore(ServiceStore serviceStore) {
    this.serviceStore = serviceStore;
  }

  public DeploymentDaemonConfiguration getDdConfig() {
    return ddConfig;
  }

  public void setDdConfig(DeploymentDaemonConfiguration ddConfig) {
    this.ddConfig = ddConfig;
  }

  public ProcessorChainFactory getProcessorChainFactory() {
    return processorChainFactory;
  }

  public void setProcessorChainFactory(ProcessorChainFactory processorChainFactory) {
    this.processorChainFactory = processorChainFactory;
  }

  public PeriodicWorkExecutor getSweepExecutor() {
    return sweepExecutor;
  }

  public void setSweepExecutor(PeriodicWorkExecutor sweepExecutor) {
    this.sweepExecutor = sweepExecutor;
  }

  public ExecutorService getDdThreadPool() {
    return ddThreadPool;
  }

  public void setDdThreadPool(ExecutorService ddThreadPool) {
    this.ddThreadPool = ddThreadPool;
  }

  public ServiceMonitor getSystemMonitor() {
    return systemMonitor;
  }

  public void setSystemMonitor(ServiceMonitor systemMonitor) {
    this.systemMonitor = systemMonitor;
  }

  /**
   * xx.
   */
  public void init() throws UnableToStartException {
    bootstrap.start();
    try {
      sweepExecutor.start();
    } catch (UnableToStartException e) {
      logger.error("Unable to start deployment daemon due to failed to initialize it");
      throw e;
    }
  }

  @Override
  public TProcessor getProcessor() {
    return processor;
  }

  @Override
  public void ping() throws TException {
  }

  @Override
  public PutTarResponse putTar(PutTarRequest request) throws TException {
    PyService service = PyService.findValueByServiceName(request.getServiceName());
    logger.debug("putTar {} {} coor-{} fsserver-{}", request, service, request.getCoorTimestamp(),
        request.getFsTimestamp());
    // update version info in latest file for driver upgrade
    if (service == PyService.COORDINATOR) {
      ddConfig.setCoordinatorTimestamp(request.getCoorTimestamp());
    }

    if (service == null) {
      logger.error("Unknow service with name {}", service.getServiceName());
      throw new TException();
    }

    String version = request.getServiceVersion();
    if (version == null || version.isEmpty()) {
      request.setServiceVersion(getServiceTarVersion(service.getServiceName()));
    }

    if (request.isAppend() && !ddConfig.buildServicePackagePath(service, version).toFile()
        .exists()) {
      logger.error("Cannot append package bytes to no existing file {}",
          ddConfig.buildServicePackagePath(service, version));
      throw new TException();
    }

    try {
      processorChainFactory.createBuildPackageChain().process(request);
    } catch (Exception e) {
      logger.error("Caught an exception", e);
      throw new TException(e);
    }

    return new PutTarResponse(request.getRequestId());
  }

  /**
   * activate a service of which installation and running directory would be overwrite.
   *
   * <p>to activate a service, fol.
   */
  @Override
  public ActivateResponse activate(final ActivateRequest request)
      throws FailedToActivateServiceExceptionThrift, ServiceIsBusyExceptionThrift, TException {
    logger.warn("active request: {}", request);

    PyService service = PyService.findValueByServiceName(request.getServiceName());
    if (service == null) {
      logger.error("Unknow service with name {}", service.getServiceName());
      throw new TException();
    }

    if (!DdUtils.isRunnable(service)) {
      logger.error("Service {} is not runnable, maybe it is a driver!", request.getServiceName());
      throw new ServiceNotRunnableExceptionThrift();
    }

    String version = request.getServiceVersion();
    if (version == null || version.isEmpty()) {
      request.setServiceVersion(getServiceTarVersion(service.getServiceName()));
      logger
          .debug("Version info is not in request, give it a value {}", request.getServiceVersion());
    }

    synchronized (serviceStore) {
      // check if the service is permitted to apply operation activate
      ServiceMetadata serviceMetadata = serviceStore.get(service);
      if (serviceMetadata != null) {
        switch (serviceMetadata.getServiceStatus()) {
          case ACTIVATING:
            // don't allow to activate service having status activating
          case DEACTIVATING:
            // don't allow to activate service having status deactivating
            logger.error("Operation is not permitted due to service {} is {}",
                service.getServiceName(),
                serviceMetadata.getServiceStatus());

            String detail = String.format("Unable to activate service %s, due to status is %s",
                serviceMetadata.getServiceName(), serviceMetadata.getServiceStatus());
            throw new ServiceIsBusyExceptionThrift().setDetail(detail);
          case ACTIVE:
            logger.warn("Nothing to do due to service {} is already {}", service.getServiceName(),
                serviceMetadata.getServiceStatus());

            return new ActivateResponse(request.getRequestId());
          case ERROR:
            logger.warn("Service {} status is {}，Operation is not permitted",
                service.getServiceName(),
                serviceMetadata.getServiceStatus());
            throw new ServiceStatusIsErrorExceptionThrift();
          case DEACTIVE:
            break;
          default:
            break;
        }
      }

      // start to activate service
      serviceMetadata = new ServiceMetadata();
      serviceMetadata.setServiceName(request.getServiceName());
      serviceMetadata.setVersion(request.getServiceVersion());
      serviceMetadata.setServiceStatus(ServiceStatus.ACTIVATING);
      serviceStore.save(serviceMetadata);
    }

    ddThreadPool.execute(new Runnable() {
      @Override
      public void run() {
        try {
          processorChainFactory.createServiceActivationChain().process(request);
        } catch (Exception e) {
          logger.error("Caught an exception", e);
        }
      }
    });

    return new ActivateResponse(request.getRequestId());
  }

  @Override
  public StartResponse start(final StartRequest request)
      throws FailedToStartServiceExceptionThrift, ServiceIsBusyExceptionThrift, TException {
    logger.warn("{}", request);

    PyService service = PyService.findValueByServiceName(request.getServiceName());
    if (service == null) {
      logger.error("Unknow service with name {}", service.getServiceName());
      throw new TException();
    }

    if (!DdUtils.isRunnable(service)) {
      logger.error("Service {} is not runnable, maybe it is a driver!", request.getServiceName());
      throw new ServiceNotRunnableExceptionThrift();
    }

    synchronized (serviceStore) {

      ServiceMetadata serviceMetadata = serviceStore.get(service);
      if (serviceMetadata == null) {
        logger.error("Unable to start service {} due to it didn't deploy before",
            request.getServiceName());
        throw new FailedToStartServiceExceptionThrift();
      }

      switch (serviceMetadata.getServiceStatus()) {
        case ACTIVATING:
          // operation is not permitted
        case DEACTIVATING:
          // operation is not permitted
          logger
              .error("Operation is not permitted due to service {} is {}", service.getServiceName(),
                  serviceMetadata.getServiceStatus());

          throw new ServiceIsBusyExceptionThrift();
        case ERROR:
          logger
              .warn("Service {} status is {}，Operation is not permitted", service.getServiceName(),
                  serviceMetadata.getServiceStatus());
          throw new ServiceStatusIsErrorExceptionThrift();
        case ACTIVE:
          logger.warn("Nothing to do due to service {} is already {}", service.getServiceName(),
              serviceMetadata.getServiceStatus());

          return new StartResponse(request.getRequestId());
        case DEACTIVE:
          break;
        default:
          break;
      }

      serviceMetadata.setServiceStatus(ServiceStatus.ACTIVATING);
      serviceStore.save(serviceMetadata);
    }

    ddThreadPool.execute(new Runnable() {
      @Override
      public void run() {
        try {
          processorChainFactory.createServiceStartingChain().process(request);
        } catch (Exception e) {
          logger.error("Caught an exception", e);
        }
      }
    });

    return new StartResponse(request.getRequestId());
  }

  @Override
  public DestroyResponse destroy(DestroyRequest request)
      throws ServiceIsBusyExceptionThrift, TException {
    logger.warn("{}", request);

    PyService service = PyService.findValueByServiceName(request.getServiceName());
    if (service == null) {
      logger.error("Unknow service with name {}", service.getServiceName());
      throw new TException();
    }

    synchronized (this) {
      ServiceMetadata serviceMetadata = serviceStore.get(service);
      if (serviceMetadata == null) {
        logger.warn("No such service named {}, nothing to do", request.getServiceName());
        return new DestroyResponse(request.getRequestId());
      }

      serviceMetadata.setServiceStatus(ServiceStatus.DEACTIVE);
      serviceMetadata.setErrorCause(null);
      serviceStore.save(serviceMetadata);

      try {
        processorChainFactory.createServiceDestroyChain().process(request);
      } catch (Exception e) {
        logger.error("Caught an exception", e);
        throw new TException(e);
      }
    }

    return new DestroyResponse(request.getRequestId());
  }

  @Override
  public GetStatusResponse getStatus(GetStatusRequest request)
      throws TException, ServiceNotFoundExceptionThrift {
    logger.debug("{}", request);

    PyService service = PyService.findValueByServiceName(request.getServiceName());
    if (service == null) {
      logger.error("Unknow service with name {}", service.getServiceName());
      throw new TException();
    }

    if (!DdUtils.isRunnable(service)) {
      logger.error("Service {} is not runnable, maybe it is a driver!", request.getServiceName());
      throw new ServiceNotRunnableExceptionThrift();
    }

    ServiceMetadata serviceMetadata = serviceStore.get(service);
    if (serviceMetadata == null) {
      logger.warn("No such service {} in running path {}", service.getServiceName(),
          ddConfig.getServicesRunningPath());

      throw new ServiceNotFoundExceptionThrift();
    }

    final ServiceStatus status = serviceMetadata.getServiceStatus();
    logger.warn("serviceMetadata.getErrorCause():{}", serviceMetadata.getErrorCause());
    logger.debug("Got service {} status {}", serviceMetadata.getServiceName(),
        serviceMetadata.getServiceStatus());

    GetStatusResponse response = new GetStatusResponse();
    ServiceMetadataThrift serviceMetadataThrift = new ServiceMetadataThrift();
    response.setRequestId(request.getRequestId());
    serviceMetadataThrift.setServiceStatus(RequestResponseHelper.convertServiceStatus(status));
    int pmPid = 0;
    try {
      pmPid = systemMonitor.getPmPid(service);
    } catch (Exception e) {
      logger.error("caught an exception when try get PMPid at:{}", service.getServiceName(), e);
    }
    serviceMetadataThrift.setPmpid(pmPid);
    serviceMetadataThrift.setPid(systemMonitor.getProcessId(service));
    serviceMetadataThrift.setErrorCause(serviceMetadata.getErrorCause());
    serviceMetadataThrift.setServiceName(serviceMetadata.getServiceName());
    serviceMetadataThrift.setVersion(serviceMetadata.getVersion());
    response.setServiceMetadataThrift(serviceMetadataThrift);
    return response;
  }

  @Override
  public GetUpgradeStatusResponse getUpgradeStatus(GetUpgradeStatusRequest request)
      throws TException, ServiceNotFoundExceptionThrift {
    logger.debug("{}", request);
    boolean upgrading = false;
    PyService service = PyService.findValueByServiceName(request.getServiceName());
    if (service == null) {
      logger.error("Unknow service with name {}", service.getServiceName());
      throw new TException();
    }

    try {
      if (service == PyService.COORDINATOR) {
        upgrading = checkUpgradeStatus(DriverType.NBD);
      }
    } catch (Exception e) {
      logger.error("checkUpgradeStatus service with name {} fail", service.getServiceName());
      throw new TException();
    }

    GetUpgradeStatusResponse response = new GetUpgradeStatusResponse();
    UpgradeInfoThrift upgradeInfoThrift = new UpgradeInfoThrift();
    response.setRequestId(request.getRequestId());
    upgradeInfoThrift.setUpgrading(upgrading);
    response.setUpgradeInfoThrift(upgradeInfoThrift);
    return response;
  }


  @Override
  public UpdateLatestVersionResponse updateLatestVersion(UpdateLatestVersionRequest request)
      throws TException, ServiceNotFoundExceptionThrift {
    logger.debug("{}", request);

    PyService service = PyService.findValueByServiceName(request.getServiceName());
    String serviceVersion = request.getVersion();

    try {
      if (service == PyService.COORDINATOR) {
        String timestamp = request.getCoorTimestamp();
        updateLatestVersion(service, serviceVersion, timestamp, DriverType.NBD);
      }

    } catch (Exception e) {
      logger.error("checkUpgradeStatus service fail");
      throw new TException();
    }

    UpdateLatestVersionResponse response = new UpdateLatestVersionResponse();
    response.setRequestId(request.getRequestId());
    return response;
  }

  /**
   * xx.
   */
  public void updateLatestVersion(PyService service, String serviceVersion, String timestamp,
      DriverType drivertype) throws Exception {
    try {
      String serviceRunningPath = ddConfig.buildServiceRunningPath(service, timestamp).toString();
      String driverContainerRunningPath = ddConfig
          .buildServiceRunningPath(PyService.DRIVERCONTAINER).toString();
      VersionManager versionManager = new VersionManagerImpl(driverContainerRunningPath + "/var");
      File file = new File(serviceRunningPath);
      logger.debug(
          "updateLatestVersion coordinatorRunningPath={} "
              + "driverContainerRunningPath={} timestamp={} ",
          serviceRunningPath, driverContainerRunningPath, timestamp);

      if (file.exists()) {
        try {
          versionManager.lockVersion(drivertype);
          boolean isMigrating = versionManager.isOnMigration(drivertype);
          logger.warn("updateLatestVersion drivertype={} isMigrating={}", drivertype.name(),
              isMigrating);
          if (isMigrating) {
            logger.error("Driver upgrade migrating, try again later");
            throw new IllegalStateException("status upgrading");
          } else {
            //write version to latest
            Version version = VersionImpl.get(serviceVersion + '-' + timestamp);
            versionManager.setLatestVersion(drivertype, version);
            logger.debug("setLatestVersion, drivertype {} version {}", drivertype, version);
          }

        } catch (VersionException e) {
          logger.error("updateLatestVersion VersionException", e);
          throw new IllegalStateException(e);
        } catch (Exception e) {
          logger.error("updateLatestVersion exception", e);
          throw e;
        } finally {
          versionManager.unlockVersion(drivertype);
        }
      } else {
        logger.warn("No need updateLatestVersion coordinatorRunningPath not Exits");
      }
    } catch (Exception ee) {
      logger.error("Driver updateLatestVersion error", ee);
      throw ee;

    }
  }

  /**
   * check if coordinator/fsServer is upgrading false is upgrading, true is upgraded when upgraded
   * current & latest version must the same and not migrating.
   */
  public boolean checkUpgradeStatus(DriverType drivertype) throws Exception {
    boolean upgrading = true;
    String serviceRunningPath = ddConfig.buildServiceRunningPath(PyService.DRIVERCONTAINER)
        .toString();
    VersionManager versionManager = new VersionManagerImpl(serviceRunningPath + "/var");
    try {
      versionManager.lockVersion(drivertype);
      boolean isMigrating = versionManager.isOnMigration(drivertype);
      Version curent = versionManager.getCurrentVersion(drivertype);
      Version latest = versionManager.getLatestVersion(drivertype);
      logger.debug("checkUpgradeStatus1 drivertype={} isMigrating={} curent={} latest={}",
          drivertype.name(), isMigrating, curent, latest);
      if (!isMigrating && curent.equals(latest)) {
        logger.warn("checkUpgradeStatus2 drivertype={} isMigrating={} curent={} latest={}",
            drivertype.name(), isMigrating, curent, latest);
        upgrading = false;
      }
      return upgrading;
    } catch (Exception e) {
      logger.error("checkUpgradeStatus exception", e);
      throw e;
    } finally {
      versionManager.unlockVersion(drivertype);
    }
  }

  /**
   * Shutdown a service by the given service name.
   *
   * <p>Deactivate a service including shutdown a service and kill process manager.
   *
   * <p>If shutdown type is sync, then wait until the service was killed successfully.
   *
   * <p>And if shutdown type is not sync, then return after send shutdowm command to service.
   *
   * <p>if service status is DEACTIVE which means service has been shutdown then return.
   */
  @Override
  public DeactivateResponse deactivate(final DeactivateRequest request)
      throws FailedToDeactivateServiceExceptionThrift, ServiceIsBusyExceptionThrift, TException {
    logger.warn("{}", request);

    PyService service = PyService.findValueByServiceName(request.getServiceName());

    ServiceMetadata serviceMetadata;
    if (service == null) {
      logger.error("Unknow service with name {}", service.getServiceName());
      throw new TException();
    }

    if (!DdUtils.isRunnable(service)) {
      logger.error("Service {} is not runnable, maybe it is a driver!", request.getServiceName());
      throw new ServiceNotRunnableExceptionThrift();
    }

    synchronized (serviceStore) {

      serviceMetadata = serviceStore.get(service);
      if (serviceMetadata == null) {
        logger.error("Unable to deactivate service {} due to it didn't deployed before");
        throw new FailedToDeactivateServiceExceptionThrift();
      }

      switch (serviceMetadata.getServiceStatus()) {
        case ACTIVATING:
        case DEACTIVATING:
          logger
              .error("Operation is not permitted due to service {} is {}", service.getServiceName(),
                  serviceMetadata.getServiceStatus());
          throw new TException();
        case ERROR:
          logger
              .warn("Service {} status is {}，Operation is not permitted", service.getServiceName(),
                  serviceMetadata.getServiceStatus());
          throw new ServiceStatusIsErrorExceptionThrift();
        case ACTIVE:
          break;
        case DEACTIVE:
          logger.warn("Nothing to do due to service {} is already {}", service.getServiceName(),
              serviceMetadata.getServiceStatus());
          return new DeactivateResponse(request.getRequestId());
        default:
          break;
      }

      serviceMetadata.setServiceStatus(ServiceStatus.DEACTIVATING);
      serviceStore.save(serviceMetadata);
    }

    if (service.equals(PyService.CONSOLE)) {
      logger.warn("service:{} not support deactivate. now turn to destroy operation.", service);
      DestroyRequest destroyRequest = new DestroyRequest();
      destroyRequest.setRequestId(request.getRequestId());
      destroyRequest.setServiceName(service.getServiceName());
      destroy(destroyRequest);
      return new DeactivateResponse(request.getRequestId());
    }

    ddThreadPool.execute(new Runnable() {
      @Override
      public void run() {
        try {
          logger.warn("request servicename {} and port {}", request.getServiceName(),
              request.getServicePort());
          processorChainFactory.createServiceDeactivationChain().process(request);
        } catch (Exception e) {
          logger.error("Caught an exception", e);
          serviceMetadata.setServiceStatus(ServiceStatus.ERROR);
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
          pw.flush();
          sw.flush();
          serviceMetadata.setErrorCause(sw.toString());
          serviceStore.save(serviceMetadata);

        }
      }
    });

    return new DeactivateResponse(request.getRequestId());
  }

  /*
   * Apply configuration changes to some service.
   * Change configuration in compressing target file if no need to
   * preserve running path. Or change configuration in config file under running path.
   *
   * @parameter ChangeConfigurationRequest
   *
   * @return ChangeConfigurationResponse
   */
  @Override
  public ChangeConfigurationResponse changeConfiguration(ChangeConfigurationRequest request)
      throws ServiceNotFoundExceptionThrift, ConfigurationNotFoundExceptionThrift,
      FailedToChangeConfigurationExceptionThrift, TException {
    logger.warn("{}", request);

    PyService service = PyService.findValueByServiceName(request.getServiceName());
    if (service == null) {
      logger.error("Unknow service with name {}", service.getServiceName());
      throw new TException();
    }

    // set timestamp
    if (service == PyService.COORDINATOR) {
      ddConfig.setCoordinatorTimestamp(request.getCoorTimestamp());
    }

    ServiceMetadata serviceMetadata = serviceStore.get(service);
    logger.debug("get service:{}", serviceMetadata);
    // change configuration under running path, but actually no such running service
    if (request.isPreserve() && serviceMetadata == null) {
      logger.error("No such service {} in running path {}", service.getServiceName(),
          ddConfig.getServicesRunningPath());
      throw new ServiceNotFoundExceptionThrift();
    }

    String version = request.getServiceVersion();
    if (request.isPreserve() && version == null) {
      // give current version
      request.setServiceVersion(serviceMetadata.getVersion());
    } else if (request.isPreserve() && !version.equals(serviceMetadata.getVersion())) {
      // version conflict
      logger
          .error("Conflict service version {} in request with version {} in running path", version,
              serviceMetadata.getVersion());
      throw new FailedToChangeConfigurationExceptionThrift().setDetail("Conflict version");
    } else if (version == null || version.isEmpty()) {
      request.setServiceVersion(getServiceTarVersion(service.getServiceName()));
    }

    logger.info("going to create service by request:{} coor-{} fsserver-{}", request,
        request.getCoorTimestamp(), request.getFsTimestamp());
    try {
      processorChainFactory.createServiceConfigurationChain().process(request);
    } catch (Exception e) {
      logger.error("Caught an exception", e);
      throw new TException(e);
    }

    logger.info("response to client withing change config request:{}", request);
    return new ChangeConfigurationResponse(request.getRequestId());
  }

  @Override
  public WipeoutResponse wipeout(WipeoutRequest request)
      throws FailedToWipeoutExceptionThrift, DriverIsAliveExceptionThrift, TException {
    logger.debug("{}", request);
    List<PyService> serviceList = new ArrayList<PyService>();
    //If ignoreConfig is true means wipeout ignore config 
    // ,dd will collect all service name and wipeout it
    if (request.isIgnoreConfig() || request.getServiceName() == null) {
      // wipeout all service running on local machine
      for (PyService service : PyService.values()) {
        if (service != PyService.DEPLOYMENTDAMON) {
          serviceList.add(service);
        }
      }
    } else {
      // wipeout some service running on local machine
      PyService service = PyService.findValueByServiceName(request.getServiceName());
      if (service == null) {
        logger.error("Unknow service with name {}", request.getServiceName());
        throw new TException();
      }
      serviceList.add(service);
    }
    ddConfig.setCoordinatorTimestamp(request.getCoorTimestamp());
    ddConfig.setFsserverTimestamp(request.getFsTimestamp());
    boolean failed = false;
    synchronized (serviceStore) {
      for (PyService service : serviceList) {
        if (service != PyService.COORDINATOR) {
          //coordinator and fsserver status is deactive, they should skip to change status
          ServiceMetadata serviceMetadata = serviceStore.get(service);
          if (serviceMetadata == null) {
            logger.warn("No such service {} in running path {}, nothing to do",
                service.getServiceName(),
                ddConfig.getServicesRunningPath());
            continue;
          }
          serviceMetadata.setServiceStatus(ServiceStatus.DEACTIVE);
          serviceStore.save(serviceMetadata);
        }
        request.setServiceName(service.getServiceName());

        try {
          processorChainFactory.createServiceWipeoutChain().process(request);

        } catch (DriverIsAliveExceptionThrift e) {
          logger.warn("Driver process is alive,can not wipeout {}", service.getServiceName());
          throw new DriverIsAliveExceptionThrift();

        } catch (FailedToWipeoutExceptionThrift e) {
          throw new FailedToWipeoutExceptionThrift().setDetail(e.getDetail());
        } catch (Exception e) {
          logger.error("Caught an exception", e);
          failed = true;
          // throw new TException(e);
        }
      }
    }

    if (failed) {
      throw new FailedToWipeoutExceptionThrift();
    }

    return new WipeoutResponse(request.getRequestId());
  }

  /**
   * when to activate a service, normally you need specify the service version.
   *
   * <p>if the version was not specified, then to scan the tar path to get the latest version as
   * the
   * install version.
   */
  private String getServiceTarVersion(String serviceName) {
    String version = null;
    String versionSuffix = null;
    String currentVersionSuffix = null;
    String[] tarsNameArray = new File(DdConstants.DEFAULT_SRC_PATH).list();
    for (String tarName : tarsNameArray) {
      if (tarName
          .contains(PyService.findValueByServiceName(serviceName).getServiceProjectKeyName())) {
        String[] tarNameArray = tarName.split("-");
        if (tarName.contains("internal")) {
          currentVersionSuffix = "internal";
        } else {
          currentVersionSuffix = "release";
        }
        if (version == null) {
          version = tarNameArray[2];
          versionSuffix = currentVersionSuffix;
        }
        if (versionCompare(version, tarNameArray[2]) < 0) {
          version = tarNameArray[2];
          versionSuffix = currentVersionSuffix;
        }
      }
    }
    if (version != null) {
      version = version + "-" + versionSuffix;
    }
    return version;
  }

  private int versionCompare(String v1, String v2) {
    String[] v1Array = v1.split("\\.");
    String[] v2Array = v2.split("\\.");
    if (Integer.valueOf(v1Array[0]) > Integer.valueOf(v2Array[0])) {
      return 1;
    } else if (Integer.valueOf(v1Array[0]) < Integer.valueOf(v2Array[0])) {
      return -1;
    }
    if (Integer.valueOf(v1Array[1]) > Integer.valueOf(v2Array[1])) {
      return 1;
    } else if (Integer.valueOf(v1Array[1]) < Integer.valueOf(v2Array[1])) {
      return -1;
    }
    if (Integer.valueOf(v1Array[2]) > Integer.valueOf(v2Array[2])) {
      return 1;
    } else if (Integer.valueOf(v1Array[2]) < Integer.valueOf(v2Array[2])) {
      return -1;
    }
    return 0;
  }

  @Override
  public BackupKeyResponse backupKey(BackupKeyRequest request) throws TException {
    logger.debug("Going to list all files from {}", request);
    return new BackupKeyResponse(request.getRequestId());
  }

  /**
   * use the key that had been backed up in the last package.
   */
  @Override
  public UseBackupKeyResponse useBackupKey(UseBackupKeyRequest request) throws TException {
    logger.debug("useBackupKey {}", request);
    return new UseBackupKeyResponse(request.getRequestId());
  }

  @Override
  public PrepareWorkspaceResponse prepareWorkspace(PrepareWorkspaceRequest request)
      throws FailedToPrepareWorkspaceThrift, DriverUpgradeExceptionThrift, TException {
    logger.debug(
        "DeploymentDaemonImplDeploymentDaemonImpl{},service name:{},"
            + "coodTimestamp:{},fsTimestamp:{}",
        request,
        request.getServiceName(), request.getCoorTimestamp(), request.getFsTimestamp());

    PyService service = PyService.findValueByServiceName(request.getServiceName());
    if (service == null) {
      logger.error("Illegal service name {} specified in request!", request.getServiceName());
      throw new TException();
    }

    String version = request.getServiceVersion();
    if (version == null || version.isEmpty()) {
      logger.warn(
          "Version info for service {} is not specified in request!"
              + " Use lastest version in environment.",
          request.getServiceName());
      request.setServiceVersion(getServiceTarVersion(request.getServiceName()));
      logger.warn("Latest version in environment for service {} is {}", request.getServiceName(),
          request.getServiceVersion());
    }

    ServiceMetadata serviceMetadata = new ServiceMetadata();
    serviceMetadata.setServiceName(request.getServiceName());
    serviceMetadata.setVersion(request.getServiceVersion());
    serviceMetadata.setServiceStatus(ServiceStatus.DEACTIVE);
    synchronized (serviceStore) {
      // save service info in environment
      serviceStore.save(serviceMetadata);
    }

    try {
      processorChainFactory.createWorkspacePreparationChain().process(request);
    } catch (DriverUpgradeExceptionThrift e) {
      logger.error("Caught an exception when preparing workspace for service {}",
          request.getServiceName(), e);
      throw new DriverUpgradeExceptionThrift();
    } catch (Exception e) {
      logger.error("Caught an exception when preparing workspace for service {}",
          request.getServiceName(), e);
      throw new TException(e);
    }

    return new PrepareWorkspaceResponse(request.getRequestId());
  }

}
