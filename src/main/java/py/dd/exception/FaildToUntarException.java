
package py.dd.exception;

/**
 * xx.
 */
public class FaildToUntarException extends Exception {

  public FaildToUntarException() {
    super();
  }

  public FaildToUntarException(String err) {
    super(err);
  }

  public FaildToUntarException(Throwable thr) {
    super(thr);
  }

  public FaildToUntarException(String err, Throwable thr) {
    super(err, thr);
  }

}
