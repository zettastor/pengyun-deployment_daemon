
package py.dd.exception;

/**
 * xx.
 */
public class ServiceIsBusyException extends Exception {

  public ServiceIsBusyException() {
    super();
  }

  public ServiceIsBusyException(String err) {
    super(err);
  }

  public ServiceIsBusyException(Throwable thr) {
    super(thr);
  }

  public ServiceIsBusyException(String err, Throwable thr) {
    super(err, thr);
  }
}
