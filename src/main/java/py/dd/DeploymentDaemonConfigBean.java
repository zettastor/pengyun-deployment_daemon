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

package py.dd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import py.dd.service.ServiceMonitor;
import py.system.monitor.LinuxMonitor;

/**
 * xx.
 */
@Configuration
@Import({DeploymentDaemonConfiguration.class})
public class DeploymentDaemonConfigBean {

  @Autowired
  private DeploymentDaemonConfiguration ddConfig;

  @Bean
  public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }

  /**
   * xx.
   */
  @Bean
  public ServiceMonitor javaSysMonitor() {
    ServiceMonitor systemMonitor = new ServiceMonitor(new LinuxMonitor());
    systemMonitor.setDdConfig(ddConfig);
    return systemMonitor;
  }

}
