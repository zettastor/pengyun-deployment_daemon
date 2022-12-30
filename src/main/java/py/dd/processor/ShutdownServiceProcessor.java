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

import java.lang.reflect.Method;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.client.thrift.GenericThriftClientFactory;
import py.common.PyService;
import py.common.struct.EndPoint;
import py.dd.common.ServicePort;
import py.dd.exception.UnableToShutdownServiceException;
import py.exception.GenericThriftClientFactoryException;
import py.thrift.datanode.service.DataNodeService;
import py.thrift.deploymentdaemon.ActivateRequest;
import py.thrift.deploymentdaemon.ChangeConfigurationRequest;
import py.thrift.deploymentdaemon.DeactivateRequest;
import py.thrift.deploymentdaemon.DestroyRequest;
import py.thrift.deploymentdaemon.PutTarRequest;
import py.thrift.deploymentdaemon.RestartRequest;
import py.thrift.deploymentdaemon.StartRequest;
import py.thrift.deploymentdaemon.WipeoutRequest;
import py.thrift.distributedinstancehub.service.DistributedInstanceHub;
import py.thrift.drivercontainer.service.DriverContainer;
import py.thrift.infocenter.service.InformationCenter;
import py.thrift.monitorserver.service.MonitorServer;
import py.thrift.share.ServiceHavingBeenShutdownThrift;
import py.thrift.systemdaemon.service.SystemDaemon;
import py.thrift.systemmonitor.service.MonitorCenter;


/**
 * send shutdown command to service to shutdown a service.
 *
 * <p>It baesed on service name via reflection to get
 * service class and then call shutdown interface.
 */
public class ShutdownServiceProcessor extends DeploymentDaemonProcessor {

  private static final Logger logger = LoggerFactory.getLogger(ShutdownServiceProcessor.class);
  private GenericThriftClientFactory genericThriftClientFactory = null;

  @Override
  public void process(DeactivateRequest request) throws Exception {
    this.service = PyService.findValueByServiceName(request.getServiceName());
    this.servicePort = getServicePort(request.getServiceName(), request.getServicePort());
    // process deactivating service
    process();
  }

  @Override
  public void process(ActivateRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process(StartRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Deprecated
  @Override
  public void process(RestartRequest request) throws Exception {
    this.service = PyService.findValueByServiceName(request.getServiceName());
    this.servicePort = getServicePort(request.getServiceName(), request.getServicePort());
    // process deactivating service
    process();

    if (nextProcessor != null) {
      nextProcessor.process(request);
    }
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
    throw new NotImplementedException();
  }

  @Override
  public void process(PutTarRequest request) throws Exception {
    throw new NotImplementedException();
  }

  @Override
  public void process() throws Exception {

    logger.debug("Stop process manager first before shutdown service {}", service.getServiceName());

    int pmPid;
    try {
      pmPid = systemMonitor.getPmPid(service);
      systemMonitor.killProcess(pmPid);
    } catch (Exception e) {
      logger.error("caught an exception when try get PMPid at:{}", service.getServiceName(), e);
    }

    //@SuppressWarnings("rawtypes")
    genericThriftClientFactory = getGenericThriftClientFactory();

    Method method = null;
    Object client = null;
    EndPoint endPoint = null;
    //If caught TTransportException,will set the flag as true.
    // It use to judge whether to capture the TTransportException
    // and GenericThriftClientFactoryException at the same time ,
    // because the two exception means server has been shutdown
    // successfully ,but response timeout.
    boolean transexceptionflag = false;
    logger.debug("Invoke shutdown interface to stop service {}", service.getServiceName());

    /*
    Network Impact command Send and response,
    so any exception except ServiceHavingBeenShutdownThrift,
    which means shutdown command worked,
    will retry shutdown service in max_tries times.
     */
    int maxRetries = ddConfig.getShutdownRetryTimes();
    for (int loop = 0; loop < maxRetries; loop++) {
      try {
        // change TCompactProtocol to DeploymentDaemonClientFactory.modify by sxl
        // here build a specify service client (like datanode) to notice (datanode) shutdown itself
        String hostname = appContext.getMainEndPoint().getHostName();
        Validate.notNull(hostname);
        endPoint = new EndPoint(hostname, servicePort);
        logger.warn("try to stop at:{}", endPoint);
        client = genericThriftClientFactory.generateSyncClient(endPoint, 3000);
        method = client.getClass().getDeclaredMethod("shutdown");
        method.invoke(client);
        return;
      } catch (Exception e) {
        logger.warn("Caught an exception", e);
        Throwable t = e.getCause();
        if (e instanceof GenericThriftClientFactoryException) {
          if (transexceptionflag) {
            logger.warn(
                "Caugh TTransportException and GenericThriftClientFactoryException at same time"
                    + ",maybe server has been shutdown ");
            return;
          }
          logger.warn("Connect to {} failed", endPoint);
          Thread.sleep(2000 * (loop + 1));
          continue;
        } else if ((t != null) && (t instanceof TTransportException)) {
          logger.warn("Response from {} timeout", endPoint);
          Thread.sleep(2000 * (loop + 1));
          transexceptionflag = true;
          continue;
        } else if ((t != null) && (t instanceof ServiceHavingBeenShutdownThrift)) {
          logger.warn("Service {} at {} having been shutdown", service.getServiceName(), endPoint);
          return;
        } else {
          throw e;
        }
      }
    }
    logger.error("Caught an exception for endpoint={}", endPoint);
    throw new UnableToShutdownServiceException();
  }

  /**
   * xx.
   */
  public GenericThriftClientFactory getGenericThriftClientFactory() {
    switch (service) {
      case DATANODE:
        return GenericThriftClientFactory.create(DataNodeService.Iface.class);
      case DIH:
        return GenericThriftClientFactory.create(DistributedInstanceHub.Iface.class);
      case DRIVERCONTAINER:
        return GenericThriftClientFactory.create(DriverContainer.Iface.class);
      case INFOCENTER:
        return GenericThriftClientFactory.create(InformationCenter.Iface.class);
      default:
        logger.error("Service {} is not thrift service", service.getServiceName());
        throw new RuntimeException("Illegal operation on service " + service.getServiceName());
    }
  }

  /**
   * Get service port.
   *
   * <p>Field 'servicePort' is added to deployment daemon requests after integration tests running
   * after a long while, which means integration tests use default port recording in 'ServicePort'
   * without filling 'servicePort' in request. As a result, deployment daemon adapts to integration
   * tests and use default port when the 'servicePort' field is not filled.
   *
   * @param servicePortInRequest service port in deployment daemon requests. if the value is 0, then
   *                             the filed is not filled in requests; otherwise, it is filled.
   * @return service port in request if not 0, or default service recording in {@link ServicePort}.
   */
  int getServicePort(String serviceName, int servicePortInRequest) {
    if (servicePortInRequest != 0) {
      // service port in request is filled.
      return servicePortInRequest;
    } else {
      ServicePort servicePort = ServicePort.valueOf(serviceName);
      if (servicePort == null) {
        String errMsg = "No corresponding default service port for service " + serviceName;
        logger.error("{}", errMsg);
        throw new IllegalArgumentException(errMsg);
      }

      logger.warn(
          "Service port is not filled in deployment "
              + "daemon request, use the default service port {}.",
          servicePort.getValue());
      return servicePort.getValue();
    }
  }
}