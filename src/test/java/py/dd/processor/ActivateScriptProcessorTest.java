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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import py.common.PyService;
import py.dd.DeploymentDaemonConfiguration;
import py.dd.test.utils.DdTestUtils;
import py.test.TestBase;

/**
 * A class includes some test for {@link ActivateScriptProcessor}.
 */
public class ActivateScriptProcessorTest extends TestBase {

  private static final Logger logger = LoggerFactory.getLogger(ActivateScriptProcessorTest.class);

  private DeploymentDaemonConfiguration ddConfig;

  private ActivateScriptProcessor activateScriptProcessor = new ActivateScriptProcessor();

  private PyService testService = PyService.DIH;

  private ClearServiceProcessor clearServiceProcessor = new ClearServiceProcessor();

  /**
   * xx.
   */
  @Before
  public void init() throws Exception {
    super.init();

    ddConfig = DdTestUtils.buildTestConfiguration();

    activateScriptProcessor.setDdConfig(ddConfig);
    activateScriptProcessor.setService(testService);

    clearServiceProcessor.setDdConfig(ddConfig);
  }

  /**
   * Create two activation scripts and check if the two scripts are executed.
   *
   *
   */
  @Test
  public void testActivateScriptProcessor() throws Exception {
    clearServiceProcessor.process();

    Path scriptsPath = ddConfig.buildServicesScriptsPath(testService);
    scriptsPath.toFile().mkdirs();

    // create 2 activation scripts
    Path scriptPath010 = Paths.get(scriptsPath.toString(), "010script");
    Path scriptPathResult010 = Paths.get(scriptsPath.getParent().toString(), "010script_Result");

    BufferedWriter bw = new BufferedWriter(new FileWriter(scriptPath010.toFile()));
    bw.write("#!/usr/bin/perl");
    bw.newLine();
    bw.write(String.format("system(\"touch %s\")", scriptPathResult010.getFileName()));
    bw.close();
    Path scriptPath020 = Paths.get(scriptsPath.toString(), "020script");

    bw = new BufferedWriter(new FileWriter(scriptPath020.toFile()));
    bw.write("#!/usr/bin/perl");
    bw.newLine();
    Path scriptPathResult020 = Paths.get(scriptsPath.getParent().toString(), "020script_Result");

    bw.write(String.format("system(\"touch %s\")", scriptPathResult020.getFileName()));
    bw.close();

    // run the processor
    activateScriptProcessor.process();

    // check if the scripts are executed
    Assert.assertTrue(scriptPathResult010.toFile().exists());
    Assert.assertTrue(scriptPathResult020.toFile().exists());
  }

}
