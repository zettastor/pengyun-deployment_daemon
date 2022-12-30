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