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

package py.dd.processor;

import org.apache.commons.lang.NotImplementedException;
import py.app.context.AppContext;
import py.common.PyService;
import py.dd.DeploymentDaemonConfiguration;
import py.dd.service.ServiceMonitor;
import py.dd.service.store.ServiceStore;
import py.storage.StorageConfiguration;
import py.thrift.deploymentdaemon.ActivateRequest;
import py.thrift.deploymentdaemon.ChangeConfigurationRequest;
import py.thrift.deploymentdaemon.DeactivateRequest;
import py.thrift.deploymentdaemon.DestroyRequest;
import py.thrift.deploymentdaemon.PrepareWorkspaceRequest;
import py.thrift.deploymentdaemon.PutTarRequest;
import py.thrift.deploymentdaemon.RestartRequest;
import py.thrift.deploymentdaemon.StartRequest;
import py.thrift.deploymentdaemon.WipeoutRequest;

/**
 * processor of deployment daemon that process a kind of job.
 */
public abstract class DeploymentDaemonProcessor {

  protected PyService service;

  protected int servicePort;

  protected DeploymentDaemonConfiguration ddConfig;

  protected StorageConfiguration storageConfiguration;

  protected AppContext appContext;

  protected ServiceMonitor systemMonitor;

  protected ServiceStore serviceStore;

  protected DeploymentDaemonProcessor nextProcessor;

  public DeploymentDaemonProcessor getNextProcessor() {
    return this.nextProcessor;
  }

  public void setNextProcessor(DeploymentDaemonProcessor processor) {
    this.nextProcessor = processor;
  }

  public void process() throws Exception {
  }

  /**
   * process the job
   *
   * <p>if the process need some parameters, you can via process()
   * to call another private function or via constructor.
   */
  public abstract void process(ActivateRequest request) throws Exception;

  public abstract void process(DeactivateRequest request) throws Exception;

  public abstract void process(StartRequest request) throws Exception;

  /**
   * To restart a service through deployment daemon, client deactivates the service first, and then
   * start it again. And relative request is {@link DeactivateRequest} and {@link ActivateRequeste}.
   * And {@link RestartRequest} is not used.
   */
  public abstract void process(RestartRequest request) throws Exception;

  public abstract void process(DestroyRequest request) throws Exception;

  public abstract void process(WipeoutRequest request) throws Exception;

  public abstract void process(ChangeConfigurationRequest request) throws Exception;

  public abstract void process(PutTarRequest request) throws Exception;

  /**
   * Process request to prepare workspace for some service.
   *
   * @throws Exception if something wrong when processing request to prepare workspace for some
   *                   service.
   */
  public void process(PrepareWorkspaceRequest request) throws Exception {
    throw new NotImplementedException();
  }

  public PyService getService() {
    return service;
  }

  public void setService(PyService service) {
    this.service = service;
  }

  public int getServicePort() {
    return servicePort;
  }

  public void setServicePort(int servicePort) {
    this.servicePort = servicePort;
  }

  public DeploymentDaemonConfiguration getDdConfig() {
    return ddConfig;
  }

  public void setDdConfig(DeploymentDaemonConfiguration ddConfig) {
    this.ddConfig = ddConfig;
  }

  public ServiceMonitor getSystemMonitor() {
    return systemMonitor;
  }

  public void setSystemMonitor(ServiceMonitor systemMonitor) {
    this.systemMonitor = systemMonitor;
  }

  public ServiceStore getServiceStore() {
    return serviceStore;
  }

  public void setServiceStore(ServiceStore serviceStore) {
    this.serviceStore = serviceStore;
  }

  public AppContext getAppContext() {
    return appContext;
  }

  public void setAppContext(AppContext appContext) {
    this.appContext = appContext;
  }

  public StorageConfiguration getStorageConfiguration() {
    return storageConfiguration;
  }

  public void setStorageConfiguration(StorageConfiguration storageConfiguration) {
    this.storageConfiguration = storageConfiguration;
  }
}
