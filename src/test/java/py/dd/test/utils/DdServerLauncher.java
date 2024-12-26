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
