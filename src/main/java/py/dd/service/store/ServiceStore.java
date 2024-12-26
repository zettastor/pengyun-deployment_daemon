
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
