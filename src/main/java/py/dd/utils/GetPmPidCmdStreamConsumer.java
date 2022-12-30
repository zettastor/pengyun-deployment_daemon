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
