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

package py.dd.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.OsCmdExecutor.OsCmdStreamConsumer;
import py.common.PyService;
import py.dd.service.IndependentProcessManager;

/**
 * xx.
 */
public class GetPmPidCmdStreamConsumer implements OsCmdStreamConsumer {

  private static final Logger logger = LoggerFactory.getLogger(DdUtils.class);

  private int pmPid;
  private PyService pyService;

  public GetPmPidCmdStreamConsumer(PyService pyService) {
    this.pyService = pyService;
    this.pmPid = 0;
  }

  public int getPmPid() {
    return pmPid;
  }

  @Override
  public void consume(InputStream stream) throws IOException {
    String line = null;
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

    while ((line = reader.readLine()) != null) {
      int tmpPmPid = IndependentProcessManager.matchAndGetPmPidFromOutput(line, pyService);
      logger.warn("get PMPId:{} from line:{}", tmpPmPid, line);
      if (tmpPmPid > 0) {
        pmPid = tmpPmPid;
        break;
      }
    }
    reader.close();
  }
}
