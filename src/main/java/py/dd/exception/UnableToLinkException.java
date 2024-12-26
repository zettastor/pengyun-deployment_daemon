
package py.dd.exception;

/**
 * xx.
 */
public class UnableToLinkException extends Exception {

  public UnableToLinkException() {
    super();
  }

  public UnableToLinkException(String err) {
    super(err);
  }

  public UnableToLinkException(Throwable thr) {
    super(thr);
  }

  public UnableToLinkException(String err, Throwable thr) {
    super(err, thr);
  }

}
