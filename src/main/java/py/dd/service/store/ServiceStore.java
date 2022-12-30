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

import java.util.List;
import py.common.PyService;
import py.dd.common.ServiceMetadata;

/**
 * xx.
 */
public interface ServiceStore {

  public ServiceMetadata get(PyService service);

  public boolean save(ServiceMetadata service);

  /**
   * List all drivers existing in driver store.
   */
  public List<ServiceMetadata> list();

  /**
   * Remove driver with specified volume id as key.
   */
  public boolean remove(PyService service);

  public void clearMemory();
}
