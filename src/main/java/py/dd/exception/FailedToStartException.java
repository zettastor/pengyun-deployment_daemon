
package py.dd.exception;

/**
 * xx.
 */
public class FailedToStartException extends Exception {

  public FailedToStartException() {
    super();
  }

  public FailedToStartException(String err) {
    super(err);
  }

  public FailedToStartException(Throwable thr) {
    super(thr);
  }

  public FailedToStartException(String err, Throwable thr) {
    super(err, thr);
  }
}
