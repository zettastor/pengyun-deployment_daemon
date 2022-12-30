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

package py.dd.processor.chain;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.dd.processor.DeploymentDaemonProcessor;
import py.test.TestBase;

/**
 * xx.
 */
public class ProcessorChainFactoryTest extends TestBase {

  private static final Logger logger = LoggerFactory.getLogger(ProcessorChainFactoryTest.class);

  public void init() throws Exception {
    super.init();
  }

  /**
   * In this test plan, it starts many threads to create processor chain. All processors in these
   * chains should be different instances.
   */
  @Test
  public void testBuildProcessorChainConcurrently() throws Exception {
    final int nThreads = 10;
    final ProcessorChainFactory factory = new ProcessorChainFactory();
    final Set<Integer> instanceIds = new HashSet<>();
    final CountDownLatch threadLatch = new CountDownLatch(nThreads);

    for (int i = 0; i < nThreads; i++) {
      new Thread() {
        public void run() {
          DeploymentDaemonProcessor iteratorProcessor = factory.createServiceActivationChain();
          synchronized (instanceIds) {
            instanceIds.add(System.identityHashCode(iteratorProcessor));
            while (iteratorProcessor.getNextProcessor() != null) {
              iteratorProcessor = iteratorProcessor.getNextProcessor();
              instanceIds.add(System.identityHashCode(iteratorProcessor));
            }
          }

          threadLatch.countDown();
        }
      }.start();
    }

    threadLatch.await();

    Assert.assertEquals(4 * nThreads, instanceIds.size());
  }
}
