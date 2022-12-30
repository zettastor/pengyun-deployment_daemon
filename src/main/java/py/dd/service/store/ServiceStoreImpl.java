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

package py.dd.service.store;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.PyService;
import py.dd.DeploymentDaemonConfiguration;
import py.dd.common.ServiceMetadata;

/**
 * xx.
 */
public class ServiceStoreImpl implements ServiceStore, ServiceFileStore {

  private static final Logger logger = LoggerFactory.getLogger(ServiceStoreImpl.class);

  private DeploymentDaemonConfiguration ddConfig;

  private Map<PyService, ServiceMetadata> serviceTable = new ConcurrentHashMap<>();

  @Override
  public boolean load() {
    for (PyService service : PyService.values()) {
      if (service != PyService.DEPLOYMENTDAMON) {
        if (ddConfig.buildServiceRunningPath(service).toFile().exists()) {
          load(service);
        }
      }
    }

    return true;
  }

  @Override
  public boolean load(PyService service) {
    Path statusBakPath = ddConfig.buildServiceStatusFileBakPath(service);
    Path statusPath = ddConfig.buildServiceStatusFilePath(service);
    boolean flag = false;
    ServiceMetadata serviceMetadata = ServiceMetadata
        .buildFromFile(ddConfig.buildServiceStatusFilePath(service));
    if (serviceMetadata == null) {
      if (statusBakPath.toFile().exists()) {
        renameStatusFile(statusBakPath, statusPath);
        if ((serviceMetadata = ServiceMetadata
            .buildFromFile(ddConfig.buildServiceStatusFilePath(service))) != null) {
          flag = true;
        }
      }

    } else {
      flag = true;
    }
    if (flag) {
      serviceTable.put(service, serviceMetadata);
    }
    return flag;
  }

  @Override

  public boolean flush(PyService service) {
    ServiceMetadata serviceMetadata = serviceTable.get(service);
    Path statusBakPath = ddConfig.buildServiceStatusFileBakPath(service);
    Path statusPath = ddConfig.buildServiceStatusFilePath(service);
    if (serviceMetadata == null) {
      logger.warn("No such service named {}", service.getServiceName());
      return true;
    }

    Path fileStoreParent = ddConfig.buildServiceStatusFilePath(service).getParent();
    if (!fileStoreParent.toFile().exists()) {
      logger.warn("file store parent {} doesn't exist, make it", fileStoreParent);
      fileStoreParent.toFile().mkdirs();
    }

    if (!ddConfig.buildServiceStatusFilePath(service).toFile().exists()) {
      if (!serviceMetadata.saveToFie(ddConfig.buildServiceStatusFilePath(service))) {
        logger.error("Something wrong when flush service metedata {}", serviceMetadata);
        return false;
      }
    } else {
      //backup StatusFile before write new status to StatusFile
      if (copyStatusFile(statusPath.toFile(), statusBakPath.toFile())) {
        if (!serviceMetadata.saveToFie(ddConfig.buildServiceStatusFilePath(service))) {
          logger.error("Something wrong when flush service metedata {}", serviceMetadata);
          return false;

        } else {
          if (statusBakPath.toFile().exists()) {
            statusBakPath.toFile().delete();
          }
        }
      }
    }

    return true;
  }

  /**
   * xx.
   */
  public void renameStatusFile(Path oldname, Path newname) {
    File oldfile = new File(oldname.toString());
    File newfile = new File(newname.toString());
    if (!oldfile.exists()) {
      return;
    }
    oldfile.renameTo(newfile);
  }

  /**
   * xx.
   */
  public boolean copyStatusFile(File srcFile, File dstFile) {
    try {
      FileOutputStream outputStream = new FileOutputStream(dstFile, true);
      FileInputStream inputStream = new FileInputStream(srcFile);
      DataInputStream dataInput = new DataInputStream(inputStream);
      DataOutputStream dataOutput = new DataOutputStream(outputStream);

      byte[] readBytes = new byte[1024];
      int length = dataInput.read(readBytes);
      while (length != -1) {
        dataOutput.write(readBytes, 0, length);
        length = dataInput.read(readBytes);
      }
    } catch (Exception e) {
      logger.warn("Catch an exception when copy status file:{}", e);
      return false;
    }
    return true;

  }


  @Override
  public ServiceMetadata get(PyService service) {
    if (serviceTable == null) {
      logger.warn("serviceTable is null");
      return null;
    }

    if (serviceTable.get(service) == null) {
      load(service);
    }

    return serviceTable.get(service);
  }

  @Override
  public boolean save(ServiceMetadata service) {
    PyService pyService = PyService.findValueByServiceName(service.getServiceName());
    serviceTable.put(pyService, service);
    if (!flush(pyService)) {
      logger.warn("Flush service to file failed");
      return false;
    }

    return true;
  }

  @Override
  public List<ServiceMetadata> list() {
    List<ServiceMetadata> serviceList = new ArrayList<ServiceMetadata>();
    if (serviceTable.values() == null) {
      return serviceList;
    }

    for (ServiceMetadata service : serviceTable.values()) {
      serviceList.add(service);
    }

    return serviceList;
  }

  @Override
  public boolean remove(PyService service) {
    if (ddConfig.buildServiceStatusFilePath(service).toFile().delete()) {
      serviceTable.remove(service);
      return true;
    } else {
      logger.error("Unable to remove service {} from store", service.getServiceName());
      return false;
    }
  }

  public DeploymentDaemonConfiguration getDdConfig() {
    return ddConfig;
  }

  public void setDdConfig(DeploymentDaemonConfiguration ddConfig) {
    this.ddConfig = ddConfig;
  }

  public Map<PyService, ServiceMetadata> getServiceTable() {
    return serviceTable;
  }

  public void setServiceTable(Map<PyService, ServiceMetadata> serviceTable) {
    this.serviceTable = serviceTable;
  }

  @Override
  public void clearMemory() {
    serviceTable.clear();
  }
}
