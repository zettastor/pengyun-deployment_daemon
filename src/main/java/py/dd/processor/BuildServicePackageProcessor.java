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

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
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
 * xx.
 */
public class BuildServicePackageProcessor extends DeploymentDaemonProcessor {

  private static final Logger logger = LoggerFactory.getLogger(BuildServicePackageProcessor.class);

  private Path packagePath;

  private byte[] packageBytes;

  private boolean append;

  @Override
  public void process(ActivateRequest request) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void process(DeactivateRequest request) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void process(StartRequest request) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void process(RestartRequest request) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void process(DestroyRequest request) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void process(WipeoutRequest request) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void process(ChangeConfigurationRequest request) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void process(PutTarRequest request) throws Exception {
    logger.debug("process {}", request);
    service = PyService.findValueByServiceName(request.getServiceName());
    if (service == PyService.COORDINATOR) {
      ddConfig.setCoordinatorTimestamp(request.getCoorTimestamp());
    } 
    packagePath = ddConfig.buildServicePackagePath(service, request.getServiceVersion());
    append = request.isAppend();
    packageBytes = request.getTarFile();

    process();
  }

  @Override
  public void process() throws Exception {
    logger.debug("process packagePath={} append={}", packagePath, append);
    DataOutputStream outputStream = new DataOutputStream(
        new FileOutputStream(packagePath.toString(), append));
    outputStream.write(packageBytes);
    outputStream.flush();
    outputStream.close();
  }
}
