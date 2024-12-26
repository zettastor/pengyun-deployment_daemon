
package py.dd.exception;

/**
 * xx.
 */
public class FailedToShutdownException extends Exception {

  public FailedToShutdownException() {
    super();
  }

  public FailedToShutdownException(String err) {
    super(err);
  }

  public FailedToShutdownException(Throwable thr) {
    super(thr);
  }

  public FailedToShutdownException(String err, Throwable thr) {
    super(err, thr);
  }

}
