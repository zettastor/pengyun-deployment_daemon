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
