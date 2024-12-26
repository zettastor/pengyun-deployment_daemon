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
