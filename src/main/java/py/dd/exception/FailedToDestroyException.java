
package py.dd.exception;

/**
 * xx.
 */
public class FailedToDestroyException extends Exception {

  public FailedToDestroyException() {
    super();
  }

  public FailedToDestroyException(String err) {
    super(err);
  }

  public FailedToDestroyException(Throwable thr) {
    super(thr);
  }

  public FailedToDestroyException(String err, Throwable thr) {
    super(err, thr);
  }
}
