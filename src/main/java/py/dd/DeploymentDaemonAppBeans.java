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

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import py.app.NetworkConfiguration;
import py.app.context.AppContextImpl;
import py.common.PyService;
import py.common.struct.EndPoint;
import py.common.struct.EndPointParser;
import py.dd.exception.UnableToBootstrap;
import py.dd.processor.chain.ProcessorChainFactory;
import py.dd.service.DeploymentDaemonImpl;
import py.dd.service.ServiceMonitor;
import py.dd.service.store.ServiceStore;
import py.dd.service.store.ServiceStoreImpl;
import py.dd.worker.LogCollector;
import py.dd.worker.ServiceSweeperFactory;
import py.instance.PortType;
import py.periodic.UnableToStartException;
import py.periodic.impl.ExecutionOptionsReader;
import py.periodic.impl.PeriodicWorkExecutorImpl;
import py.storage.StorageConfiguration;
import py.system.monitor.LinuxMonitor;

/**
 * Beans of service deployment daemon.
 */
@Configuration
@Import({DeploymentDaemonConfiguration.class, FtpServerConfiguration.class,
    NetworkConfiguration.class, StorageConfiguration.class})
public class DeploymentDaemonAppBeans {

  @Autowired
  private DeploymentDaemonConfiguration ddConfig;

  @Autowired
  private NetworkConfiguration networkConfiguration;

  @Autowired
  private FtpServerConfiguration ftpConfig;

  @Autowired
  private StorageConfiguration storageConfig;

  @Bean
  public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }

  /**
   * xx.
   */
  @Bean
  public AppContextImpl appContext() {
    AppContextImpl appContext = new AppContextImpl(ddConfig.getDdAppName());
    EndPoint endPoint = EndPointParser.parseInSubnet(ddConfig.getDdAppPort(),
        networkConfiguration.getControlFlowSubnet());
    appContext.putEndPoint(PortType.CONTROL, endPoint);
    return appContext;
  }

  /**
   * xx.
   */
  @Bean
  public DeploymentDaemonImpl deploymentDaemonImpl() throws UnableToStartException {
    DeploymentDaemonImpl deploymentDaemonImpl = new DeploymentDaemonImpl();
    deploymentDaemonImpl.setDdConfig(ddConfig);
    deploymentDaemonImpl.setBootstrap(bootstrap());
    deploymentDaemonImpl.setProcessorChainFactory(processorChainFactory());
    deploymentDaemonImpl.setServiceStore(serviceStore());
    deploymentDaemonImpl.setSweepExecutor(sweepExecutor());
    deploymentDaemonImpl.setDdThreadPool(ddThreadPool());
    deploymentDaemonImpl.setSystemMonitor(javaSysMonWrapper());
    deploymentDaemonImpl.init();
    return deploymentDaemonImpl;
  }

  /**
   * xx.
   */
  @Bean
  public DeploymentDaemonAppEngine deploymentDaemonAppEngine()
      throws UnableToBootstrap, UnableToStartException {
    DeploymentDaemonAppEngine deploymentDaemonAppEngine = new DeploymentDaemonAppEngine(
        deploymentDaemonImpl());
    deploymentDaemonAppEngine.setContext(appContext());
    deploymentDaemonAppEngine.setMaxNetworkFrameSize(ddConfig.getMaxNetworkFrameSize());
    deploymentDaemonAppEngine.setFtpSwitch(ftpConfig.getFtpSwitch());
    try {
      deploymentDaemonAppEngine.setLogCollector(logCollector());
    } catch (Exception e) {
      throw new UnableToStartException();
    }

    return deploymentDaemonAppEngine;
  }

  /**
   * xx.
   */
  @Bean
  public Bootstrap bootstrap() {
    Bootstrap bootstrap = new Bootstrap();
    bootstrap.setDdConfig(ddConfig);
    bootstrap.setSysMonWrapper(javaSysMonWrapper());
    bootstrap.setServiceStore(serviceStore());
    bootstrap.setProcessorChainFactory(processorChainFactory());
    bootstrap.setDdThreadPool(ddThreadPool());
    return bootstrap;
  }

  /**
   * xx.
   */
  @Bean
  public ServiceMonitor javaSysMonWrapper() {
    ServiceMonitor systemMonitor = new ServiceMonitor(new LinuxMonitor());
    systemMonitor.setDdConfig(ddConfig);
    return systemMonitor;
  }

  /**
   * xx.
   */
  @Bean
  public ServiceStore serviceStore() {
    ServiceStoreImpl serviceStore = new ServiceStoreImpl();
    serviceStore.setDdConfig(ddConfig);
    return serviceStore;
  }

  /**
   * xx.
   */
  @Bean
  public ProcessorChainFactory processorChainFactory() {
    ProcessorChainFactory processorChainFactory = new ProcessorChainFactory();
    processorChainFactory.setServiceStore(serviceStore());
    processorChainFactory.setDdConfig(ddConfig);
    processorChainFactory.setStorageConfiguration(storageConfig);
    processorChainFactory.setAppContext(appContext());
    processorChainFactory.setSystemMonitor(javaSysMonWrapper());

    return processorChainFactory;
  }

  /**
   * xx.
   */
  @Bean
  public ExecutionOptionsReader sweepExecutionOptionsReader() {
    ExecutionOptionsReader sweepExecutionOptionReader = new ExecutionOptionsReader(1, 1,
        (int) ddConfig.getServiceSweeperIntervalMs(), null);
    return sweepExecutionOptionReader;
  }

  /**
   * xx.
   */
  @Bean
  public PeriodicWorkExecutorImpl sweepExecutor() {
    PeriodicWorkExecutorImpl sweepExecutor = new PeriodicWorkExecutorImpl(
        sweepExecutionOptionsReader(),
        serviceSweeperFactory(), "sweeper-woker");
    return sweepExecutor;
  }

  /**
   * xx.
   */
  @Bean
  public ServiceSweeperFactory serviceSweeperFactory() {
    ServiceSweeperFactory serviceSweeperFactory = new ServiceSweeperFactory();
    serviceSweeperFactory.setDdConfig(ddConfig);
    serviceSweeperFactory.setServiceStore(serviceStore());
    serviceSweeperFactory.setSystemMonitor(javaSysMonWrapper());
    serviceSweeperFactory.setProcessorChainFactory(processorChainFactory());
    serviceSweeperFactory.setDdThreadPool(ddThreadPool());
    return serviceSweeperFactory;
  }

  @Bean
  public ExecutorService ddThreadPool() {
    ExecutorService executor = Executors.newFixedThreadPool(5);
    return executor;
  }

  /**
   * xx.
   */
  @Bean
  public LogCollector logCollector() throws Exception {
    PyService[] services = new PyService[]{PyService.CONSOLE, PyService.COORDINATOR,
        PyService.DATANODE, PyService.DIH, PyService.DRIVERCONTAINER, PyService.INFOCENTER};
    Map<String, String> logNamesForeachService = new HashMap<String, String>();
    for (PyService service : services) {
      logNamesForeachService.put(service.getServiceName(),
          Paths.get(ddConfig.getServicesRunningPath(), service.getServiceProjectKeyName(), "logs")
              .toAbsolutePath().toString());
    }

    EndPoint server = new EndPoint(ftpConfig.getHostName(), ftpConfig.getPort());
    LogCollector logCollector = LogCollector
        .builder(server, ftpConfig.getUserName(), ftpConfig.getPassword())
        .basePath(ftpConfig.getRootPath()) // FTP root path
        .delay(0).period(ftpConfig.getPeriod()) // timer configuration
        .serviceStore(serviceStore()) // services in local host
        .logNamesForeachService(logNamesForeachService) // all service's log path
        .build();

    return logCollector;
  }
}
