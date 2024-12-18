package org.bedework.util.servlet;

import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** These can be registered with a method and located by some
 * appropriate criteria, e.g. the first element in a path
 *
 */
public abstract class MethodHelper {
  private MethodBase mb;

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

    public void forward(final String path) {
    mb.forward(path);
  }
}
