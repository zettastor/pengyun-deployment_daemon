

package py.dd.ftp;

/**
 * xx.
 */
public class NoServiceException extends Exception {

  private static final long serialVersionUID = 1L;

  public NoServiceException() {
    super();
  }

  public NoServiceException(String message) {
    super(message);
  }

  public NoServiceException(String message, Throwable cause) {
    super(message, cause);
  }

  public NoServiceException(Throwable cause) {
    super(cause);
  }
}
