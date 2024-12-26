
package py.dd.exception;

/**
 * xx.
 */
public class NotReadyException extends Exception {

  public NotReadyException() {
    super();
  }

  public NotReadyException(String err) {
    super(err);
  }

  public NotReadyException(Throwable thr) {
    super(thr);
  }

  public NotReadyException(String err, Throwable thr) {
    super(err, thr);
  }
}
