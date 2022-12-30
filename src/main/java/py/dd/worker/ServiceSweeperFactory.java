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

package py.dd.worker;

import java.util.concurrent.ExecutorService;
import py.dd.DeploymentDaemonConfiguration;
import py.dd.processor.chain.ProcessorChainFactory;
import py.dd.service.ServiceMonitor;
import py.dd.service.store.ServiceStore;
import py.periodic.Worker;
import py.periodic.WorkerFactory;

/**
 * xx.
 */
public class ServiceSweeperFactory implements WorkerFactory {

  private ServiceSweeper sweeper;

  private ServiceMonitor systemMonitor;

  private ServiceStore serviceStore;

  private DeploymentDaemonConfiguration ddConfig;

  private ProcessorChainFactory processorChainFactory;

  private ExecutorService ddThreadPool;

  @Override
  public Worker createWorker() {
    if (sweeper == null) {
      sweeper = new ServiceSweeper();
      sweeper.setDdConfig(ddConfig);
      sweeper.setServiceStore(serviceStore);
      sweeper.setSystemMonitor(systemMonitor);
      sweeper.setProcessorChainFactory(processorChainFactory);
      sweeper.setDdThreadPool(ddThreadPool);
    }

    return sweeper;
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

  public DeploymentDaemonConfiguration getDdConfig() {
    return ddConfig;
  }

  public void setDdConfig(DeploymentDaemonConfiguration ddConfig) {
    this.ddConfig = ddConfig;
  }

  public void setProcessorChainFactory(ProcessorChainFactory processorChainFactory) {
    this.processorChainFactory = processorChainFactory;
  }

  public void setDdThreadPool(ExecutorService ddThreadPool) {
    this.ddThreadPool = ddThreadPool;
  }
}
