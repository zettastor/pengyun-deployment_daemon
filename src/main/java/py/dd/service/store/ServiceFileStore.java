
package py.dd.service.store;

import py.common.PyService;

/**
 * xx.
 */
public interface ServiceFileStore {

  /**
   * xx.
   */
  public boolean load();

  public boolean load(PyService service);

  /**
   * xx.
   */
  public boolean flush(PyService service);

  public boolean remove(PyService service);
}
