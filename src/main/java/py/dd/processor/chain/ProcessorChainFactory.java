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

package py.dd.processor.chain;

import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.app.context.AppContext;
import py.dd.DeploymentDaemonConfiguration;
import py.dd.processor.ActivateScriptProcessor;
import py.dd.processor.BuildServicePackageProcessor;
import py.dd.processor.ChangeConfigurationProcessor;
import py.dd.processor.ClearIscsiProcessor;
import py.dd.processor.ClearServiceProcessor;
import py.dd.processor.DeploymentDaemonProcessor;
import py.dd.processor.DestroyDriversProcesser;
import py.dd.processor.DestroyServiceProcessor;
import py.dd.processor.LinkProcessor;
import py.dd.processor.PrepareEnvProcessor;
import py.dd.processor.ShutdownServiceProcessor;
import py.dd.processor.StartupServiceProcessor;
import py.dd.processor.TarProcessor;
import py.dd.processor.UntarProcessor;
import py.dd.service.ServiceMonitor;
import py.dd.service.store.ServiceStore;
import py.storage.StorageConfiguration;
import py.thrift.deploymentdaemon.ActivateRequest;
import py.thrift.deploymentdaemon.ChangeConfigurationRequest;
import py.thrift.deploymentdaemon.DeactivateRequest;
import py.thrift.deploymentdaemon.DestroyRequest;
import py.thrift.deploymentdaemon.PutTarRequest;
import py.thrift.deploymentdaemon.RestartRequest;
import py.thrift.deploymentdaemon.StartRequest;
import py.thrift.deploymentdaemon.WipeoutRequest;

/**
 * In service deployment daemon, many processors are included. For each request, multiple processors
 * are required to order as a chain to handle the request. This class is a factory to build a chain
 * for each request so that the request is processed properly.
 */
public class ProcessorChainFactory {

  private static final Logger logger = LoggerFactory.getLogger(ProcessorChainFactory.class);

  private ServiceStore serviceStore;

  private DeploymentDaemonConfiguration ddConfig;

  private StorageConfiguration storageConfiguration;

  private AppContext appContext;

  private ServiceMonitor systemMonitor;

  /**
   * Create a chain handler for request of {@link ActivateRequest}.
   *
   * <p>1. untar service package to installation path <br/> 
   * 2. link service files or directories from
   * installation path to running path <br/> 
   * 3. execute multiple activation scripts if them exist<br/> 
   * 4. execute service launcher script <br/>.
   *
   */
  public DeploymentDaemonProcessor createServiceActivationChain() {
    return createProcessorChain(new UntarProcessor(), new LinkProcessor(),
        new ActivateScriptProcessor(),
        new StartupServiceProcessor());
  }

  /**
   * Create a chain handler for request of {@link StartRequest}
   *
   * <p>1. execute multiple activation scripts if them exist <br/> 
   * 2. execute service launcher script <br/>.
   *
   */
  public DeploymentDaemonProcessor createServiceStartingChain() {
    return createProcessorChain(new ActivateScriptProcessor(), new StartupServiceProcessor());
  }

  /**
   * Create a chain handler for request of {@link RestartRequest}
   *
   * <p>1. shutdown service with synchronization <br/>
   * 2. execute multiple activation scripts if them exist <br/> 
   * 3. execute service launcher script <br/>.
   *
   */
  public DeploymentDaemonProcessor createServiceRestartingChain() {
    return createProcessorChain(new ShutdownServiceProcessor(), new ActivateScriptProcessor(),
        new StartupServiceProcessor());
  }

  /**
   * Create a chain handler for request of {@link DeactivateRequest}
   *
   * <p>1. shutdown service with synchronization <br/>.
   *
   */
  public DeploymentDaemonProcessor createServiceDeactivationChain() {
    return createProcessorChain(new ShutdownServiceProcessor());
  }

  /**
   * Create a chain handler for request of {@link DestroyRequest}
   *
   * <p>1. destroy running services in request <br/> 2. destroy running drivers if request contains
   * service 'DriverContainer' <br/>.
   */
  public DeploymentDaemonProcessor createServiceDestroyChain() {
    // return createProcessorChain(new DestroyServiceProcessor(), new DestroyDriversProcesser());
    return createProcessorChain(new DestroyServiceProcessor());
  }

  /**
   * Create a chain handler for request of {@link ChangeConfigurationRequest}
   *
   * <p>1. untar service package to temporary directory if the request not preserving package <br/> 
   * 2.apply new properties configuration to config file under each service <br/> 3. create a new
   * archive as service package to packages directory <br/>
   *
   */
  public DeploymentDaemonProcessor createServiceConfigurationChain() {
    return createProcessorChain(new UntarProcessor(), new ChangeConfigurationProcessor(),
        new TarProcessor());
  }

  /**
   * Create a chain handler for request of {@link WipeoutRequest}
   *
   * <p>1. destroy running services in request <br/>
   * 2. destroy running drivers if request contains
   * service 'DriverContainer' <br/>
   * 3. clear all service relative files or directories <br/> 
   * 4.clear all iscsi configuration file contents if service in request is 'DriverContainer' <br/>.
   *
   */
  public DeploymentDaemonProcessor createServiceWipeoutChain() {
    return createProcessorChain(new DestroyServiceProcessor(), new DestroyDriversProcesser(),
        new ClearServiceProcessor(), new ClearIscsiProcessor());
  }

  public DeploymentDaemonProcessor createWorkspaceCleanChain() {
    return createProcessorChain(new ClearServiceProcessor());
  }

  /**
   * Create a chain handler for request of {@link PutTarRequest}
   *
   * <p>1. prepare services relative environment such packages direcotry, installation directory and
   * running directory. <br/> 
   * 2. write pakcage bytes to file as package.
   *
   */
  public DeploymentDaemonProcessor createBuildPackageChain() {
    return createProcessorChain(new PrepareEnvProcessor(), new BuildServicePackageProcessor());
  }

  public DeploymentDaemonProcessor createWorkspacePreparationChain() {
    return createProcessorChain(new UntarProcessor(), new LinkProcessor());
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

  public StorageConfiguration getStorageConfiguration() {
    return storageConfiguration;
  }

  public void setStorageConfiguration(StorageConfiguration storageConfiguration) {
    this.storageConfiguration = storageConfiguration;
  }

  public ServiceMonitor getSystemMonitor() {
    return systemMonitor;
  }

  public void setSystemMonitor(ServiceMonitor systemMonitor) {
    this.systemMonitor = systemMonitor;
  }

  public AppContext getAppContext() {
    return appContext;
  }

  public void setAppContext(AppContext appContext) {
    this.appContext = appContext;
  }

  DeploymentDaemonProcessor createProcessorChain(DeploymentDaemonProcessor... processors) {
    List<DeploymentDaemonProcessor> processorLst = Arrays.asList(processors);
    initProcessers(processorLst);
    return processorLst.get(0);
  }

  /**
   * Link all processor together.
   *
   *
   */
  void initProcessers(List<DeploymentDaemonProcessor> processorLst) {
    DeploymentDaemonProcessor lastProcessor = null;

    for (DeploymentDaemonProcessor processor : processorLst) {
      if (lastProcessor != null) {
        lastProcessor.setNextProcessor(processor);
      }

      processor.setDdConfig(ddConfig);
      processor.setStorageConfiguration(storageConfiguration);
      processor.setSystemMonitor(systemMonitor);
      processor.setServiceStore(serviceStore);
      processor.setAppContext(appContext);

      lastProcessor = processor;
    }
  }
}
