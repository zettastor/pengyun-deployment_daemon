
package py.dd.exception;

/**
 * xx.
 */
public class FailedToActivateException extends Exception {

  public FailedToActivateException() {
    super();
  }

  public FailedToActivateException(String err) {
    super(err);
  }

  public FailedToActivateException(Throwable thr) {
    super(thr);
  }

  public FailedToActivateException(String err, Throwable thr) {
    super(err, thr);
  }
}
