
package py.dd.exception;

/**
 * xx.
 */
public class UnableToBootstrap extends Exception {

  public UnableToBootstrap() {
    super();
  }

  public UnableToBootstrap(String err) {
    super(err);
  }

  public UnableToBootstrap(Throwable thr) {
    super(thr);
  }

  public UnableToBootstrap(String err, Throwable thr) {
    super(err, thr);
  }

}
