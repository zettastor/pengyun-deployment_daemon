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
