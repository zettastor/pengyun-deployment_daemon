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
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.OsCmdExecutor.OsCmdStreamConsumer;
import py.common.PyService;
import py.dd.service.IndependentProcessManager;

/**
 * xx.
 */
public class GetManyPmPidCmdStreamConsumer implements OsCmdStreamConsumer {

  private static final Logger logger = LoggerFactory.getLogger(DdUtils.class);

  private Map<PyService, Integer> pmPidMap;

  // the past PMPidMap must init already
  public GetManyPmPidCmdStreamConsumer(Map<PyService, Integer> pmPidMap) {
    Validate.notNull(pmPidMap);
    this.pmPidMap = pmPidMap;
  }

  public Map<PyService, Integer> getPmPidMap() {
    return pmPidMap;
  }

  @Override
  public void consume(InputStream stream) throws IOException {
    String line = null;
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

    while ((line = reader.readLine()) != null) {
      if (pmPidMap.isEmpty()) {
        logger.error("given py services is empty, current line:{}", line);
        continue;
      }
      for (PyService pyService : pmPidMap.keySet()) {
        int pid = IndependentProcessManager.matchAndGetPmPidFromOutput(line, pyService);
        logger.info("can not get any py service pm pid from line:{}", line);
        if (pid > 0) {
          logger.warn("get PMPId:{} from line:{}", pid, line);
          int pmPidInMap = this.pmPidMap.get(pyService);
          if (pmPidInMap > 0) {
            logger.error("find two PMPid:{},{} on one service", pmPidInMap, pid,
                pyService.getServiceName());
          } else {
            this.pmPidMap.put(pyService, pid);
          }
        }
      } // for py services loop
    }
    reader.close();
  }
}