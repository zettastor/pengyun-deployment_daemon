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

package py.dd.processor;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.lang.NotImplementedException;
import org.rauschig.jarchivelib.ArchiveFormat;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.rauschig.jarchivelib.CompressionType;
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
 * A class as processor of processor chain to create archive.
 */
public class TarProcessor extends DeploymentDaemonProcessor {

  private static final Logger logger = LoggerFactory.getLogger(TarProcessor.class);

  private Path tarFrom;

  private Path tarTo;

  private String packageName;

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
    logger.warn("process {}", request);
    service = PyService.findValueByServiceName(request.getServiceName());
    packageName = ddConfig.buildServicePackagePath(service, request.getServiceVersion()).toFile()
        .getName();
    tarFrom = ddConfig.buildTmpInstallationPath(service, request.getServiceVersion());
    tarTo = Paths.get(ddConfig.getServicesPackagesPath());

    process();

  }

  @Override
  public void process() throws Exception {
    logger.warn("Create archive file named {} from {} to {}", packageName, tarFrom, tarTo);
    Archiver archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP);
    archiver.create(packageName, tarTo.toFile(), tarFrom.toFile());
  }

  @Override
  public void process(PutTarRequest request) throws Exception {
  }

  public Path getTarFrom() {
    return tarFrom;
  }

  public void setTarFrom(Path tarFrom) {
    this.tarFrom = tarFrom;
  }

  public Path getTarTo() {
    return tarTo;
  }

  public void setTarTo(Path tarTo) {
    this.tarTo = tarTo;
  }

  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }
}
