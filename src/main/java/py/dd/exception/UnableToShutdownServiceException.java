
package py.dd.exception;

/**
 * xx.
 */
public class UnableToShutdownServiceException extends Exception {

  public UnableToShutdownServiceException() {
    super();
  }

  public UnableToShutdownServiceException(String err) {
    super(err);
  }

  public UnableToShutdownServiceException(Throwable thr) {
    super(thr);
  }

  public UnableToShutdownServiceException(String err, Throwable thr) {
    super(err, thr);
  }

}
