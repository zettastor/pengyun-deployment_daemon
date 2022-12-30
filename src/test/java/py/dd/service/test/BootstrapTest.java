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

package py.dd.service.test;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import py.common.PyService;
import py.dd.Bootstrap;
import py.dd.DeploymentDaemonConfiguration;
import py.dd.common.ServiceMetadata;
import py.dd.common.ServiceStatus;
import py.dd.processor.ClearServiceProcessor;
import py.dd.processor.DeploymentDaemonProcessor;
import py.dd.processor.PrepareEnvProcessor;
import py.dd.processor.chain.ProcessorChainFactory;
import py.dd.service.ServiceMonitor;
import py.dd.service.store.ServiceStoreImpl;
import py.dd.test.utils.DdTestUtils;
import py.processmanager.Pmdb;
import py.test.TestBase;

/**
 * A class includes some tests for {@link Bootstrap}.
 */
public class BootstrapTest extends TestBase {

  private static final Logger logger = LoggerFactory.getLogger(BootstrapTest.class);

  private DeploymentDaemonConfiguration ddConfig;

  private ServiceStoreImpl serviceStore = new ServiceStoreImpl();

  @Mock
  private ServiceMonitor sysMonWrapper;

  @Mock
  private ProcessorChainFactory processorChainFactory;

  private PyService testService = PyService.DIH;

  private ClearServiceProcessor clearServiceProcessor = new ClearServiceProcessor();

  @Mock
  private DeploymentDaemonProcessor serviceActivationChain;

  @Mock
  private DeploymentDaemonProcessor serviceDeactivationChain;

  @Mock
  private DeploymentDaemonProcessor destroyServiceChain;

  @Mock
  private ExecutorService ddThreadPool;

  private Bootstrap bootstrap = new Bootstrap();

  private PrepareEnvProcessor prepareEnvProcessor = new PrepareEnvProcessor();

  /**
   * xx.
   */
  @Before
  public void init() throws Exception {
    super.init();

    when(processorChainFactory.createServiceDestroyChain()).thenReturn(destroyServiceChain);

    ddConfig = DdTestUtils.buildTestConfiguration();

    serviceStore.setDdConfig(ddConfig);

    clearServiceProcessor.setDdConfig(ddConfig);
    prepareEnvProcessor.setDdConfig(ddConfig);

    bootstrap.setDdConfig(ddConfig);
    bootstrap.setProcessorChainFactory(processorChainFactory);
    bootstrap.setServiceStore(serviceStore);
    bootstrap.setSysMonWrapper(sysMonWrapper);
    bootstrap.setDdThreadPool(ddThreadPool);
  }

  /**
   * In the case, status of service is activating, and the process of it is alive which means pid in
   * os is the same as pid in file under service running path. As a result, nothing to do for it.
   */
  @Test
  public void activating2Alive() throws Exception {
    // clear test environment
    clearServiceProcessor.process();
    prepareEnvProcessor.process();

    ServiceMetadata serviceMetadata = new ServiceMetadata();
    serviceMetadata.setServiceName(testService.getServiceName());
    serviceMetadata.setServiceStatus(ServiceStatus.ACTIVATING);
    serviceMetadata.setVersion("2.3.0");

    // build service running path
    Path serviceRunningPath = ddConfig.buildServiceRunningPath(testService);
    serviceRunningPath.toFile().mkdirs();

    serviceStore.save(serviceMetadata);

    // fake active process of service using the same process id
    int processId = Integer.MAX_VALUE;
    Pmdb pmdb = Pmdb.build(Paths.get(serviceRunningPath.toString()));
    pmdb.save(Pmdb.SERVICE_PID_NAME, String.valueOf(processId));
    when(sysMonWrapper.getProcessId(argThat(new IsTestService()))).thenReturn(processId);
    when(sysMonWrapper.getProcessId(argThat(new IsNotTestService()))).thenReturn(0);

    when(sysMonWrapper.getPmPid(argThat(new IsNotTestService()))).thenReturn(0);
    when(sysMonWrapper.getPmPid(argThat(new IsNotTestService()))).thenReturn(0);

    bootstrap.start();

    serviceMetadata = serviceStore.get(testService);
    Assert.assertTrue(serviceMetadata.getServiceStatus() == ServiceStatus.ACTIVE);

    // nothing to do and no exception throws out
  }

  /**
   * In the case, status of service is not alive, but its process manager is alive who will start
   * the service all the time. As a result, nothing will do in bootstrap.
   */
  @Test
  public void activating2PmAlive() throws Exception {
    // clear test environment
    clearServiceProcessor.process();
    prepareEnvProcessor.process();

    ServiceMetadata serviceMetadata = new ServiceMetadata();
    serviceMetadata.setServiceName(testService.getServiceName());
    serviceMetadata.setServiceStatus(ServiceStatus.ACTIVATING);
    serviceMetadata.setVersion("2.3.0");

    // build service running path
    Path serviceRunningPath = ddConfig.buildServiceRunningPath(testService);
    serviceRunningPath.toFile().mkdirs();

    serviceStore.save(serviceMetadata);

    // fake active process of service using the same process id
    int processId = Integer.MAX_VALUE;
    Pmdb pmdb = Pmdb.build(Paths.get(serviceRunningPath.toString()));
    pmdb.save(Pmdb.SERVICE_PID_NAME, String.valueOf(processId));
    when(sysMonWrapper.getProcessId(argThat(new IsTestService()))).thenReturn(0);
    when(sysMonWrapper.getProcessId(argThat(new IsNotTestService()))).thenReturn(0);

    int pmPid = Integer.MAX_VALUE;
    pmdb.save(Pmdb.PM_PID_NAME, Integer.toString(pmPid));
    when(sysMonWrapper.getPmPid(argThat(new IsTestService()))).thenReturn(pmPid);
    when(sysMonWrapper.getPmPid(argThat(new IsNotTestService()))).thenReturn(0);

    bootstrap.start();

    // nothing to do and no exception throws out
  }

  /**
   * In the case, both service and pm are not alive, but service's status is activating which needs
   * dd to start up the service when bootstrap.
   */
  @Test
  public void activating2BothNotAlive() throws Exception {
    // clear test environment
    clearServiceProcessor.process();
    prepareEnvProcessor.process();

    ServiceMetadata serviceMetadata = new ServiceMetadata();
    serviceMetadata.setServiceName(testService.getServiceName());
    serviceMetadata.setServiceStatus(ServiceStatus.ACTIVATING);
    serviceMetadata.setVersion("2.3.0");

    // build service running path
    Path serviceRunningPath = ddConfig.buildServiceRunningPath(testService);
    serviceRunningPath.toFile().mkdirs();

    serviceStore.save(serviceMetadata);

    // fake active process of service using the same process id
    int processId = Integer.MAX_VALUE;
    Pmdb pmdb = Pmdb.build(Paths.get(serviceRunningPath.toString()));
    pmdb.save(Pmdb.SERVICE_PID_NAME, String.valueOf(processId));
    when(sysMonWrapper.getProcessId(argThat(new IsTestService()))).thenReturn(0);
    when(sysMonWrapper.getProcessId(argThat(new IsNotTestService()))).thenReturn(0);

    int pmPid = Integer.MAX_VALUE;
    pmdb.save(Pmdb.PM_PID_NAME, Integer.toString(pmPid));
    when(sysMonWrapper.getPmPid(argThat(new IsTestService()))).thenReturn(0);
    when(sysMonWrapper.getPmPid(argThat(new IsNotTestService()))).thenReturn(0);

    when(processorChainFactory.createServiceStartingChain()).thenReturn(serviceActivationChain);

    bootstrap.start();

    Mockito.verify(processorChainFactory, Mockito.times(1)).createServiceStartingChain();
  }

  /**
   * In the case, status of service is active and the process of it is alive. As a result, nothing
   * to do.
   */
  @Test
  public void active2Alive() throws Exception {
    // clear test environment
    clearServiceProcessor.process();
    prepareEnvProcessor.process();

    ServiceMetadata serviceMetadata = new ServiceMetadata();
    serviceMetadata.setServiceName(testService.getServiceName());
    serviceMetadata.setServiceStatus(ServiceStatus.ACTIVE);
    serviceMetadata.setVersion("2.3.0");

    // build service running path
    Path serviceRunningPath = ddConfig.buildServiceRunningPath(testService);
    serviceRunningPath.toFile().mkdirs();

    serviceStore.save(serviceMetadata);

    // fake active process of service using the same process id
    int processId = Integer.MAX_VALUE;
    Pmdb pmdb = Pmdb.build(Paths.get(serviceRunningPath.toString()));
    pmdb.save(Pmdb.SERVICE_PID_NAME, String.valueOf(processId));
    when(sysMonWrapper.getProcessId(argThat(new IsTestService()))).thenReturn(processId);
    when(sysMonWrapper.getProcessId(argThat(new IsNotTestService()))).thenReturn(0);

    when(sysMonWrapper.getPmPid(argThat(new IsNotTestService()))).thenReturn(0);
    when(sysMonWrapper.getPmPid(argThat(new IsNotTestService()))).thenReturn(0);

    bootstrap.start();

    // nothing to do and no exception throws out
  }

  /**
   * In the case, status of service is not alive, but its process manager is alive who will start
   * the service all the time. Meanwhile, the service status is active. As a result, change the
   * status of service to activating.
   */
  @Test
  public void active2PmAlive() throws Exception {
    // clear test environment
    clearServiceProcessor.process();
    prepareEnvProcessor.process();

    ServiceMetadata serviceMetadata = new ServiceMetadata();
    serviceMetadata.setServiceName(testService.getServiceName());
    serviceMetadata.setServiceStatus(ServiceStatus.ACTIVE);
    serviceMetadata.setVersion("2.3.0");

    // build service running path
    Path serviceRunningPath = ddConfig.buildServiceRunningPath(testService);
    serviceRunningPath.toFile().mkdirs();

    serviceStore.save(serviceMetadata);

    // fake active process of service using the same process id
    int processId = Integer.MAX_VALUE;
    Pmdb pmdb = Pmdb.build(Paths.get(serviceRunningPath.toString()));
    pmdb.save(Pmdb.SERVICE_PID_NAME, String.valueOf(processId));
    when(sysMonWrapper.getProcessId(argThat(new IsTestService()))).thenReturn(0);
    when(sysMonWrapper.getProcessId(argThat(new IsNotTestService()))).thenReturn(0);

    int pmPid = Integer.MAX_VALUE;
    pmdb.save(Pmdb.PM_PID_NAME, Integer.toString(pmPid));
    when(sysMonWrapper.getPmPid(argThat(new IsTestService()))).thenReturn(pmPid);
    when(sysMonWrapper.getPmPid(argThat(new IsNotTestService()))).thenReturn(0);

    bootstrap.start();

    serviceMetadata = serviceStore.get(testService);
    Assert.assertTrue(serviceMetadata.getServiceStatus() == ServiceStatus.ACTIVATING);
  }

  /**
   * In the case, both service and pm are not alive, but service's status is active which needs dd
   * to start up the service when bootstrap.
   */
  @Test
  public void active2BothNotAlive() throws Exception {
    // clear test environment
    clearServiceProcessor.process();

    prepareEnvProcessor.process();
    ServiceMetadata serviceMetadata = new ServiceMetadata();
    serviceMetadata.setServiceName(testService.getServiceName());
    serviceMetadata.setServiceStatus(ServiceStatus.ACTIVE);
    serviceMetadata.setVersion("2.3.0");

    // build service running path
    Path serviceRunningPath = ddConfig.buildServiceRunningPath(testService);
    serviceRunningPath.toFile().mkdirs();

    serviceStore.save(serviceMetadata);

    // fake active process of service using the same process id
    int processId = Integer.MAX_VALUE;
    Pmdb pmdb = Pmdb.build(Paths.get(serviceRunningPath.toString()));
    pmdb.save(Pmdb.SERVICE_PID_NAME, String.valueOf(processId));
    when(sysMonWrapper.getProcessId(argThat(new IsTestService()))).thenReturn(0);
    when(sysMonWrapper.getProcessId(argThat(new IsNotTestService()))).thenReturn(0);

    int pmPid = Integer.MAX_VALUE;
    pmdb.save(Pmdb.PM_PID_NAME, Integer.toString(pmPid));
    when(sysMonWrapper.getPmPid(argThat(new IsTestService()))).thenReturn(0);
    when(sysMonWrapper.getPmPid(argThat(new IsNotTestService()))).thenReturn(0);

    when(processorChainFactory.createServiceStartingChain()).thenReturn(serviceActivationChain);

    bootstrap.start();

    serviceMetadata = serviceStore.get(testService);
    Assert.assertTrue(serviceMetadata.getServiceStatus() == ServiceStatus.ACTIVATING);

    Mockito.verify(processorChainFactory, Mockito.times(1)).createServiceStartingChain();
  }

  /**
   * In the case, status of service is deactivating and process of service is alive. As a result, dd
   * will try to deactivate it again.
   */
  @Test
  public void deactivating2Alive() throws Exception {
    // clear test environment
    clearServiceProcessor.process();
    prepareEnvProcessor.process();

    ServiceMetadata serviceMetadata = new ServiceMetadata();
    serviceMetadata.setServiceName(testService.getServiceName());
    serviceMetadata.setServiceStatus(ServiceStatus.DEACTIVATING);
    serviceMetadata.setVersion("2.3.0");

    // build service running path
    Path serviceRunningPath = ddConfig.buildServiceRunningPath(testService);
    serviceRunningPath.toFile().mkdirs();

    serviceStore.save(serviceMetadata);

    // fake active process of service using the same process id
    int processId = Integer.MAX_VALUE;
    Pmdb pmdb = Pmdb.build(Paths.get(serviceRunningPath.toString()));
    pmdb.save(Pmdb.SERVICE_PID_NAME, String.valueOf(processId));
    when(sysMonWrapper.getProcessId(argThat(new IsTestService()))).thenReturn(processId);
    when(sysMonWrapper.getProcessId(argThat(new IsNotTestService()))).thenReturn(0);

    int pmPid = Integer.MAX_VALUE;
    pmdb.save(Pmdb.PM_PID_NAME, Integer.toString(pmPid));
    when(sysMonWrapper.getPmPid(argThat(new IsTestService()))).thenReturn(0);
    when(sysMonWrapper.getPmPid(argThat(new IsNotTestService()))).thenReturn(0);

    when(processorChainFactory.createServiceDeactivationChain())
        .thenReturn(serviceDeactivationChain);

    bootstrap.start();

    Mockito.verify(processorChainFactory, Mockito.times(1)).createServiceDeactivationChain();
  }

  /**
   * In the case, status of service is deactivating and process of pm is alive. As a result, dd will
   * try to deactivate it again.
   */
  @Test
  public void deactivating2PmAlive() throws Exception {
    // clear test environment
    clearServiceProcessor.process();
    prepareEnvProcessor.process();

    ServiceMetadata serviceMetadata = new ServiceMetadata();
    serviceMetadata.setServiceName(testService.getServiceName());
    serviceMetadata.setServiceStatus(ServiceStatus.DEACTIVATING);
    serviceMetadata.setVersion("2.3.0");

    // build service running path
    Path serviceRunningPath = ddConfig.buildServiceRunningPath(testService);
    serviceRunningPath.toFile().mkdirs();

    serviceStore.save(serviceMetadata);

    // fake active process of service using the same process id
    int processId = Integer.MAX_VALUE;
    Pmdb pmdb = Pmdb.build(Paths.get(serviceRunningPath.toString()));
    pmdb.save(Pmdb.SERVICE_PID_NAME, String.valueOf(processId));
    when(sysMonWrapper.getProcessId(argThat(new IsTestService()))).thenReturn(0);
    when(sysMonWrapper.getProcessId(argThat(new IsNotTestService()))).thenReturn(0);

    int pmPid = Integer.MAX_VALUE;
    pmdb.save(Pmdb.PM_PID_NAME, Integer.toString(pmPid));
    when(sysMonWrapper.getPmPid(argThat(new IsTestService()))).thenReturn(pmPid);
    when(sysMonWrapper.getPmPid(argThat(new IsNotTestService()))).thenReturn(0);

    when(processorChainFactory.createServiceDeactivationChain())
        .thenReturn(serviceDeactivationChain);

    bootstrap.start();

    Mockito.verify(processorChainFactory, Mockito.times(1)).createServiceDeactivationChain();
  }

  /**
   * In the case, status of service is deactivating and meanwhile both service and pm is not alive.
   * As a result, the dd will do change status of the service to deactive.
   */
  @Test
  public void deactivating2BothNotAlive() throws Exception {
    // clear test environment
    clearServiceProcessor.process();
    prepareEnvProcessor.process();

    ServiceMetadata serviceMetadata = new ServiceMetadata();
    serviceMetadata.setServiceName(testService.getServiceName());
    serviceMetadata.setServiceStatus(ServiceStatus.DEACTIVATING);
    serviceMetadata.setVersion("2.3.0");

    // build service running path
    Path serviceRunningPath = ddConfig.buildServiceRunningPath(testService);
    serviceRunningPath.toFile().mkdirs();

    serviceStore.save(serviceMetadata);

    // fake active process of service using the same process id
    int processId = Integer.MAX_VALUE;
    Pmdb pmdb = Pmdb.build(Paths.get(serviceRunningPath.toString()));
    pmdb.save(Pmdb.SERVICE_PID_NAME, String.valueOf(processId));
    when(sysMonWrapper.getProcessId(argThat(new IsTestService()))).thenReturn(0);
    when(sysMonWrapper.getProcessId(argThat(new IsNotTestService()))).thenReturn(0);

    int pmPid = Integer.MAX_VALUE;
    pmdb.save(Pmdb.PM_PID_NAME, Integer.toString(pmPid));
    when(sysMonWrapper.getPmPid(argThat(new IsTestService()))).thenReturn(0);
    when(sysMonWrapper.getPmPid(argThat(new IsNotTestService()))).thenReturn(0);

    when(processorChainFactory.createServiceDeactivationChain())
        .thenReturn(serviceDeactivationChain);

    bootstrap.start();

    serviceMetadata = serviceStore.get(testService);
    Assert.assertTrue(serviceMetadata.getServiceStatus() == ServiceStatus.DEACTIVE);
  }

  class IsTestService extends ArgumentMatcher<PyService> {

    @Override
    public boolean matches(Object argument) {
      if (argument == null) {
        return false;
      }

      PyService service = (PyService) argument;
      return service == testService;
    }
  }

  class IsNotTestService extends ArgumentMatcher<PyService> {

    @Override
    public boolean matches(Object argument) {
      if (argument == null) {
        return true;
      }

      PyService service = (PyService) argument;
      return service != testService;
    }
  }
}
