package org.bedework.util.servlet;

import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.servlet.config.HelperInfo;

import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** These can be registered with a method and located by some
 * appropriate criteria, e.g. the first element in a path
 *
 */
public abstract class MethodHelper implements Logged {
  private HelperInfo helperInfo;
  private MethodBase mb;

  public void init(final HelperInfo helperInfo) {
    this.helperInfo = helperInfo;
  }

  public abstract void process(List<String> resourceUri,
                               HttpServletRequest req,
                               HttpServletResponse resp)
          throws ServletException;

  public void execute(final List<String> resourceUri,
                              final HttpServletRequest req,
                              final HttpServletResponse resp,
                              final MethodBase mb)
          throws ServletException {
    this.mb = mb;
    process(resourceUri, req, resp);
  }

  protected MethodBase getMethodBase() {
    return mb;
  }

  protected ReqUtil getReqUtil() {
    return mb.getReqUtil();
  }

  /** Set the value of a named session attribute.
   *
   * @param attrName    Name of the attribute
   * @param val         Object
   */
  public void setSessionAttr(final String attrName,
                             final Object val) {
    getReqUtil().setSessionAttr(attrName, val);
  }

  public void outputJson(final HttpServletResponse resp,
                         final String etag,
                         final String[] header,
                         final Object val) {
    try {
      getMethodBase().outputJson(resp, etag, header, val);
    } catch (final ServletException e) {
      throw new RuntimeException(e);
    }
  }

  public void sendJsonError(final HttpServletResponse resp,
                            final String msg) {
    mb.sendJsonError(resp, msg);
  }

  /**
   * Forwards a request to the specified path using the associated RequestDispatcher.
   *
   * @param name the name or identifier used to determine the forward path
   */
  public void forward(final String name) {
    final var fi = helperInfo.getForward(name);
    if (fi == null) {
      throw new RuntimeException("No forward for name " + name);
    }

    if (fi.isRedirect()) {
      mb.redirectTo(fi.getPath());
    } else {
      mb.forwardToPath(fi.getPath());
    }
  }

  /* ==============================================================
   *                   Logged methods
   * ============================================================== */

  private final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
