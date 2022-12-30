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

package py.dd.test.utils;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import py.common.PyService;
import py.dd.service.IndependentProcessManager;
import py.dd.utils.DdUtils;
import py.test.TestBase;

/**
 * xx.
 */
public class GetPmPidUtilsTest extends TestBase {

  private String outPutString = "43080 /var/testing/packages/pengyun-drivercontainer/bin";


  @Test
  public void test1() {
    PyService pyService = PyService.DRIVERCONTAINER;
    PyService getPyService = IndependentProcessManager.matchAndGetPyServiceFromOutput(outPutString);
    Assert.assertEquals(pyService, getPyService);
  }

  @Test
  public void test2() {
    int pmPid = 43080;
    int getPmPid = IndependentProcessManager
        .matchAndGetPmPidFromOutput(outPutString, PyService.DRIVERCONTAINER);
    Assert.assertEquals(pmPid, getPmPid);
  }

  @Ignore
  @Test
  public void test3() {
    try {
      DdUtils.getPmPidFromCommandByServiceName(PyService.DIH,
          "/home/zhongyuan/config/getProcessPM.sh");
    } catch (IOException e) {
      logger.error("", e);
    }
  }

  @Test
  public void testEnumEqual() {
    PyService pyService = PyService.DATANODE;
    Assert.assertTrue(pyService.equals(PyService.DATANODE));
    Assert.assertTrue(pyService.getServiceName() == PyService.DATANODE.getServiceName());
  }


}
