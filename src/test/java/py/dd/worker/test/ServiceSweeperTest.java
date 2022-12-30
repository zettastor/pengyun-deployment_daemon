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

package py.dd.worker.test;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import py.common.PyService;
import py.dd.DeploymentDaemonConfiguration;
import py.dd.common.ServiceMetadata;
import py.dd.common.ServiceStatus;
import py.dd.processor.ClearServiceProcessor;
import py.dd.processor.PrepareEnvProcessor;
import py.dd.service.ServiceMonitor;
import py.dd.service.store.ServiceStoreImpl;
import py.dd.test.utils.DdTestUtils;
import py.dd.worker.ServiceSweeper;
import py.processmanager.Pmdb;
import py.test.TestBase;

/**
 * A class includes some tests for {@link ServiceSweeper}.
 */
public class ServiceSweeperTest extends TestBase {

  private static final Logger logger = LoggerFactory.getLogger(ServiceSweeperTest.class);

  private ServiceStoreImpl serviceStore = new ServiceStoreImpl();

  private DeploymentDaemonConfiguration ddConfig;

  private PyService testService = PyService.DIH;

  @Mock
  private ServiceMonitor systemMonitor;

  private ClearServiceProcessor clearServiceProcessor = new ClearServiceProcessor();

  private PrepareEnvProcessor prepareEnvProcessor = new PrepareEnvProcessor();

  private ServiceSweeper serviceSweeper = new ServiceSweeper();

  /**
   * xx.
   */
  public void init() throws Exception {
    super.init();

    ddConfig = DdTestUtils.buildTestConfiguration();

    serviceStore.setDdConfig(ddConfig);

    clearServiceProcessor.setDdConfig(ddConfig);
    prepareEnvProcessor.setDdConfig(ddConfig);

    serviceSweeper.setDdConfig(ddConfig);
    serviceSweeper.setServiceStore(serviceStore);
    serviceSweeper.setSystemMonitor(systemMonitor);
  }

  /**
   * In the case, the service status is activating and current process state is alive. As a result,
   * the service status turns into active.
   */
  @Test
  public void activating2ative() throws Exception {
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
    when(systemMonitor.getProcessId(argThat(new IsTestService()), anyInt())).thenReturn(processId);
    when(systemMonitor.getProcessId(argThat(new IsNotTestService()), anyInt())).thenReturn(0);

    when(systemMonitor.getPmPid(argThat(new IsNotTestService()), anyInt())).thenReturn(0);
    when(systemMonitor.getPmPid(argThat(new IsNotTestService()), anyInt())).thenReturn(0);

    serviceSweeper.doWork();

    serviceMetadata = serviceStore.get(testService);
    Assert.assertTrue(serviceMetadata.getServiceStatus() == ServiceStatus.ACTIVE);
  }

  /**
   * In the case, the service status is active but service process is not alive. However, pm is
   * alive which means service will be start up by it. As a result, the service status turns into
   * activating.
   */
  @Test
  public void active2Activating() throws Exception {
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
    when(systemMonitor.getProcessId(argThat(new IsTestService()), anyInt())).thenReturn(0);
    when(systemMonitor.getProcessId(argThat(new IsNotTestService()), anyInt())).thenReturn(0);

    int pmPid = Integer.MAX_VALUE;
    pmdb.save(Pmdb.PM_PID_NAME, Integer.toString(pmPid));
    when(systemMonitor.getPmPid(argThat(new IsTestService()), anyInt())).thenReturn(pmPid);
    when(systemMonitor.getPmPid(argThat(new IsNotTestService()), anyInt())).thenReturn(0);

    serviceSweeper.doWork();

    serviceMetadata = serviceStore.get(testService);
    Assert.assertTrue(serviceMetadata.getServiceStatus() == ServiceStatus.ACTIVATING);
  }

  /**
   * In the case, service has status deactivating and its process and pm is already dead. As a
   * result, turn its status to deactive.
   */
  @Test
  public void deactivating2Deactive() throws Exception {
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
    when(systemMonitor.getProcessId(argThat(new IsTestService()))).thenReturn(0);
    when(systemMonitor.getProcessId(argThat(new IsNotTestService()))).thenReturn(0);

    int pmPid = Integer.MAX_VALUE;
    pmdb.save(Pmdb.PM_PID_NAME, Integer.toString(pmPid));
    when(systemMonitor.getPmPid(argThat(new IsTestService()))).thenReturn(0);
    when(systemMonitor.getPmPid(argThat(new IsNotTestService()))).thenReturn(0);

    serviceSweeper.doWork();

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
