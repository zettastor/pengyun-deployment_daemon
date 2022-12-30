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

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.PyService;
import py.dd.common.ServicePort;
import py.test.TestBase;

/**
 * A class contains some tests for {@link ShutdownServiceProcessor}.
 */
public class ShutdownServiceProccessorTest extends TestBase {

  private static final Logger logger = LoggerFactory.getLogger(ShutdownServiceProccessorTest.class);

  @Override
  public void init() throws Exception {
    super.init();
  }

  /**
   * In this test plan, service port in request is not filled and default service port will be
   * returned.
   */
  @Test
  public void testGetDefaultServicePort() throws Exception {
    ShutdownServiceProcessor shutdownServiceProcessor = new ShutdownServiceProcessor();
    int servicePort = shutdownServiceProcessor.getServicePort(PyService.DIH.getServiceName(), 0);
    Assert.assertEquals(ServicePort.DIH.getValue(), servicePort);
  }

  /**
   * In this test plan, service port in request is filled and it will be returned.
   */
  @Test
  public void testGetServicePort() throws Exception {
    final int servicePortInRequest = 10001;

    ShutdownServiceProcessor shutdownServiceProcessor = new ShutdownServiceProcessor();
    int servicePort = shutdownServiceProcessor
        .getServicePort(PyService.DIH.getServiceName(), servicePortInRequest);
    Assert.assertEquals(servicePortInRequest, servicePort);
  }

  /**
   * In this test plan, it gets service port with an unrecognized service.
   */
  @Test
  public void testGetServicePortWithUnrecognizedService() throws Exception {
    ShutdownServiceProcessor shutdownServiceProcessor = new ShutdownServiceProcessor();
    try {
      shutdownServiceProcessor.getServicePort("", 0);
      Assert.fail();
    } catch (IllegalArgumentException e) {
      logger.error("", e);
    }
  }
}
