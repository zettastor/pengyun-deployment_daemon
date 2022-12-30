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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.PyService;
import py.thrift.deploymentdaemon.ActivateRequest;
import py.thrift.deploymentdaemon.ChangeConfigurationRequest;
import py.thrift.deploymentdaemon.DeactivateRequest;
import py.thrift.deploymentdaemon.DestroyRequest;
import py.thrift.deploymentdaemon.PutTarRequest;
import py.thrift.deploymentdaemon.RestartRequest;
import py.thrift.deploymentdaemon.StartRequest;
import py.thrift.deploymentdaemon.WipeoutRequest;

/**
 * A class as a processor in processor chain which apply new configurations to properties file.
 */
public class ChangeConfigurationProcessor extends DeploymentDaemonProcessor {

  private static final Logger logger = LoggerFactory.getLogger(ChangeConfigurationProcessor.class);

  /*
   * Each service has its private configuration file. This value give the path to it.
   */
  private Path propFilePath;

  /*
   * Configuration file is format as properties which is made up of key and value.
   */
  private Map<String, String> properties;

  @Override
  public void process(ActivateRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process(DeactivateRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process(StartRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process(RestartRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process(DestroyRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process(WipeoutRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process(ChangeConfigurationRequest request) throws Exception {
    service = PyService.findValueByServiceName(request.getServiceName());

    if (request.isPreserve()) {
      // The word 'preserve' means keep service 
      // package having no changes. As a result, change properties file in
      // installation directory.
      propFilePath = ddConfig
          .buildConfigFilePath(service, request.getServiceVersion(), request.getConfigFile());
    } else {
      // Not preserving service package, change properties
      // to service packages. And we use a temporary directory
      // to put services config files.
      propFilePath = ddConfig.buildTmpConfigFilePath(service, request.getServiceVersion(),
          request.getConfigFile());
    }

    logger.debug("build propFilePath:{}", propFilePath);
    properties = request.getChangingConfigurations();

    process();

    if (!request.isPreserve()) {
      nextProcessor.process(request);
    }
    logger.debug("end process change configuration, request:{}", request);
  }

  /**
   * xx.
   */
  public void process() throws Exception {
    Properties prop = new Properties();
    try {
      InputStream in = new BufferedInputStream(new FileInputStream(propFilePath.toFile()));
      prop.load(in);
      in.close();

      FileOutputStream out = new FileOutputStream(propFilePath.toFile());
      for (Entry<String, String> entry : properties.entrySet()) {
        if (prop.getProperty(entry.getKey()) == null) {
          prop.put(entry.getKey(), entry.getValue());
        }
        prop.setProperty(entry.getKey(), entry.getValue());
      }
      prop.store(out, null);
      out.close();
    } catch (FileNotFoundException e) {
      logger.error("caught an exception", e);
      throw new Exception(e);
    } catch (IOException e) {
      logger.error("caught an exception", e);
      throw new Exception(e);
    }
  }

  @Override
  public void process(PutTarRequest request) throws Exception {
    throw new NotImplementedException();
  }
}
