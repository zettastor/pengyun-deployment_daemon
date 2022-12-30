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

import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.app.thrift.ThriftProcessorFactory;
import py.dd.service.DeploymentDaemonImpl;

/**
 * xx.
 */
public class DdServerLauncher {

  private static final Logger logger = LoggerFactory.getLogger(DdServerLauncher.class);

  private TServer ddServer;

  /**
   * xx.
   */
  public void launch() throws Exception {
    TServerTransport serverTransport = new TServerSocket(10002);
    ThriftProcessorFactory processorFactory = new DeploymentDaemonImpl();
    TThreadPoolServer.Args args = new TThreadPoolServer.Args(serverTransport).processor(
        processorFactory.getProcessor()).protocolFactory(new TCompactProtocol.Factory());

    ddServer = new TThreadPoolServer(args);

    Thread thread = new Thread("App thread") {
      public void run() {
        logger.info("dd server is running on localhost:10002");
        ddServer.serve();
      }
    };

    thread.start();
  }

}
