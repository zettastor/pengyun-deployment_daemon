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

package py.dd.processor.test;

import static org.mockito.Mockito.when;

import org.apache.thrift.transport.TTransportException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import py.app.context.AppContext;
import py.app.context.AppContextImpl;
import py.client.thrift.GenericThriftClientFactory;
import py.common.PyService;
import py.common.struct.EndPoint;
import py.dd.DeploymentDaemonConfiguration;
import py.dd.exception.UnableToShutdownServiceException;
import py.dd.processor.ShutdownServiceProcessor;
import py.dd.service.ServiceMonitor;
import py.exception.GenericThriftClientFactoryException;
import py.instance.PortType;
import py.test.TestBase;
import py.thrift.share.ServiceHavingBeenShutdownThrift;

public class ShutdownServiceProcessorTest extends TestBase {

  //@Mock
  protected DeploymentDaemonConfiguration ddConfig;
  private ShutdownServiceProcessor shutdownServiceProcessor;
  private AppContext appContext;
  @Mock
  private ServiceMonitor systemMonitor;
  @Mock
  private GenericThriftClientFactory genericThriftClientFactory;
  private PyService testService = PyService.DIH;

  @Override
  public void init() throws Exception {
    super.init();

    shutdownServiceProcessor = new ShutdownServiceProcessor();
    ddConfig = new DeploymentDaemonConfiguration();
  }

  /**
   * test for TTransportException and GenericThriftClientFactoryException,if throw these exceptions,
   * retry send shutdown command for 1 times,in the end throw  UnableToShutdownServiceException.
   */
  @Test
  public void shutdownServiceProcessorFailedTest() throws Exception {
    shutdownServiceProcessor = new ShutdownServiceProcessor() {
      @Override
      public GenericThriftClientFactory getGenericThriftClientFactory() {
        return genericThriftClientFactory;
      }
    };

    ddConfig.setShutdownRetryTimes(1);
    appContext = new AppContextImpl("appTest");
    when(systemMonitor.getPmPid(testService)).thenReturn(Integer.MAX_VALUE);
    EndPoint endPoint = new EndPoint("255.255.255.255", 1234);
    appContext.putEndPoint(PortType.CONTROL, endPoint);
    shutdownServiceProcessor.setDdConfig(ddConfig);
    shutdownServiceProcessor.setAppContext(appContext);
    shutdownServiceProcessor.setService(testService);
    shutdownServiceProcessor.setSystemMonitor(systemMonitor);

    Mockito.doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        throw new Exception(new TTransportException());
      }
    }).when(genericThriftClientFactory)
        .generateSyncClient(Mockito.any(EndPoint.class), Mockito.anyLong());

    try {
      shutdownServiceProcessor.process();
      Assert.fail();
    } catch (UnableToShutdownServiceException utsse) {
      logger.warn("Caught an exception for endpoint={}", endPoint);
    }
    Mockito.doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        throw new GenericThriftClientFactoryException();
      }
    }).when(genericThriftClientFactory)
        .generateSyncClient(Mockito.any(EndPoint.class), Mockito.anyLong());
    try {
      shutdownServiceProcessor.process();
      Assert.fail();
    } catch (UnableToShutdownServiceException utsse) {
      logger.warn("Caught an exception for endpoint={}", endPoint);
    }

  }

  /**
   * test for shutdown command has worked,if throw ServiceHavingBeenShutdownThrift, don't need to
   * send shutdown command,just return.
   */
  @Test
  public void shutdownServiceProcessorSuccessTest() throws Exception {
    appContext = new AppContextImpl("appTest");
    when(systemMonitor.getPmPid(testService)).thenReturn(Integer.MAX_VALUE);
    EndPoint endPoint = new EndPoint("255.255.255.255", 0);
    appContext.putEndPoint(PortType.CONTROL, endPoint);
    ddConfig.setShutdownRetryTimes(1);

    Mockito.doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        throw new Exception(new ServiceHavingBeenShutdownThrift());
      }
    }).when(genericThriftClientFactory)
        .generateSyncClient(Mockito.any(EndPoint.class), Mockito.anyLong());

    shutdownServiceProcessor = new ShutdownServiceProcessor() {
      @Override
      public GenericThriftClientFactory getGenericThriftClientFactory() {
        return genericThriftClientFactory;
      }
    };
    shutdownServiceProcessor.setDdConfig(ddConfig);
    shutdownServiceProcessor.setAppContext(appContext);
    shutdownServiceProcessor.setService(testService);
    shutdownServiceProcessor.setSystemMonitor(systemMonitor);
    shutdownServiceProcessor.process();
  }
}
