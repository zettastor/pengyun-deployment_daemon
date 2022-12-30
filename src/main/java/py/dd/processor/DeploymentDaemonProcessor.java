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
