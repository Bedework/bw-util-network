/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package org.bedework.util.servlet;

import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.misc.Util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Base class for servlet methods.
 */
public abstract class MethodBase implements Logged {
  protected ServletContext context;
  protected boolean dumpContent;
  protected ReqUtil rutil;

  private static final Map<String,
          Class<? extends MethodHelper>> helpers = new HashMap<>();

  public static void registerHelper(
          final String name,
          final Class<? extends MethodHelper> helperClass) {
    helpers.put(name, helperClass);
  }

  /** Called at each request
   *
   */
  public void init(final ServletContext context,
                            final boolean dumpContent) {
    this.context = context;
    this.dumpContent = dumpContent;
  }

  private final SimpleDateFormat httpDateFormatter =
          new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss ");

  /**
   * 
   * @return mapper used to handle json content
   */
  public abstract ObjectMapper getMapper();

  public ReqUtil getReqUtil() {
    return rutil;
  }

  public ServletContext getContext() {
    return context;
  }

  /** May be overridden but call super(...).
   *
   * @param req the request
   * @param resp and response
   * @return true to continue - false - don't call doMethod
   */
  public boolean beforeMethod(final HttpServletRequest req,
                              final HttpServletResponse resp) {
    rutil = newReqUtil(req, resp);

    return true;
  }

  public void forward(final String path) {
    final RequestDispatcher dispatcher = getContext()
            .getRequestDispatcher(path);
    try {
      dispatcher.forward(rutil.getRequest(),
                         rutil.getResponse());
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }

  /** Override to obtain a subclass.
   *
   * @param req the request
   * @param resp and response
   * @return new object
   */
  public ReqUtil newReqUtil(final HttpServletRequest req,
                            final HttpServletResponse resp) {
    return new ReqUtil(req, resp);
  }

  public MethodHelper getMethodHelper(final String name) {
    final var mclass = helpers.get(name);
    if (mclass == null) {
      return null;
    }

    final var helper = (MethodHelper)loadInstance(mclass);
    if (helper == null) {
      warn("No helper for name " + name);
      return null;
    }

    return helper;
  }

  protected Object loadInstance(final Class<?> cl) {
    return Util.getObject(cl.getName(), cl);
  }

  /**
   * @param req the request
   * @param resp and response
   * @throws ServletException on fatal error
   */
  public abstract void doMethod(HttpServletRequest req,
                                HttpServletResponse resp)
          throws ServletException;

  /** Allow servlet to create method.
   */
  public static class MethodInfo {
    private final Class<? extends MethodBase> methodClass;

    private final boolean requiresAuth;

    /**
     * @param methodClass the class
     * @param requiresAuth true for auth needed
     */
    public MethodInfo(final Class<? extends MethodBase> methodClass,
                      final boolean requiresAuth) {
      this.methodClass = methodClass;
      this.requiresAuth = requiresAuth;
    }

    /**
     * @return Class for this method
     */
    public Class<? extends MethodBase> getMethodClass() {
      return methodClass;
    }

    /** Called when servicing a request to determine if this method requires
     * authentication. Allows the servlet to reject attempts to change state
     * while unauthenticated.
     *
     * @return boolean true if authentication required.
     */
    public boolean getRequiresAuth() {
      return requiresAuth;
    }
  }

  protected String hrefFromPath(final List<String> path,
                                final int start) {
    // Need exactly 3 elements from start
    final int sz = path.size();

    if (start == sz) {
      return null;
    }

    final StringBuilder sb = new StringBuilder();

    for (int i = start; i < sz; i++) {
      sb.append("/");
      sb.append(path.get(i));
    }

    sb.append("/");

    return sb.toString();
  }

  protected void write(final String s,
                       final HttpServletResponse resp) throws ServletException {
    if (s == null) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    try {
      final PrintWriter pw = resp.getWriter();

      pw.write(s);
      pw.flush();
    } catch (final Throwable t) {
      throw new ServletException(t);
    }
    resp.setStatus(HttpServletResponse.SC_OK);
  }

  protected void writeJson(final HttpServletResponse resp,
                           final Object o) throws ServletException {
    if (o == null) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    try {
      getMapper().writeValue(resp.getWriter(), o);
    } catch (final Throwable t) {
      throw new ServletException(t);
    }
    resp.setStatus(HttpServletResponse.SC_OK);
  }

  /** Get the decoded and fixed resource URI. This calls getServletPath() to
   * obtain the path information. The description of that method is a little
   * obscure in it's meaning. In a request of this form:<br/><br/>
   * "GET /ucaldav/user/douglm/calendar/1302064354993-g.ics HTTP/1.1[\r][\n]"<br/><br/>
   * getServletPath() will return <br/><br/>
   * /user/douglm/calendar/1302064354993-g.ics<br/><br/>
   * that is the context has been removed. In addition this method will URL
   * decode the path. getRequestUrl() does neither.
   *
   * @param req      Servlet request object
   * @return List    Path elements of fixed up uri
   * @throws ServletException on bad uri
   */
  public List<String> getResourceUri(final HttpServletRequest req)
          throws ServletException {
    String uri = req.getServletPath();

    if ((uri == null) || (uri.isEmpty())) {
      /* No path specified - set it to root. */
      uri = "/";
    }

    try {
      return Util.fixPath(uri);
    } catch (final RuntimeException re) {
      throw new ServletException(re);
    }
  }

  /*
  protected void addStatus(final int status,
                           final String message) throws ServletException {
    try {
      if (message == null) {
//        message = WebdavStatusCode.getMessage(status);
      }

      property(WebdavTags.status, "HTTP/1.1 " + status + " " + message);
    } catch (ServletException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }
  */

  protected void addHeaders(final HttpServletResponse resp) {
    // This probably needs changes
/*
    StringBuilder methods = new StringBuilder();
    for (String name: getSyncher().getMethodNames()) {
      if (methods.length() > 0) {
        methods.append(", ");
      }

      methods.append(name);
    }

    resp.addHeader("Allow", methods.toString());
    */
    resp.addHeader("Allow", "POST, GET");
  }

  /** Parse the request body, and return the object.
   *
   * @param is         Input stream for content
   * @param cl         The class we expect
   * @param resp       for status
   * @return Object    Parsed body or null for no body
   * @exception ServletException Some error occurred.
   */
  protected Object readJson(final InputStream is,
                            final Class<?> cl,
                            final HttpServletResponse resp) throws ServletException {
    if (is == null) {
      return null;
    }

    try {
      return getMapper().readValue(is, cl);
    } catch (final Throwable t) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      if (debug()) {
        error(t);
      }
      throw new ServletException(t);
    }
  }

  /** Parse the request body, and return the object.
   *
   * @param is         Input stream for content
   * @param tr         For the class we expect
   * @param resp       for status
   * @return Object    Parsed body or null for no body
   * @exception ServletException Some error occurred.
   */
  protected Object readJson(final InputStream is,
                            final TypeReference<?> tr,
                            final HttpServletResponse resp) throws ServletException {
    if (is == null) {
      return null;
    }

    try {
      return getMapper().readValue(is, tr);
    } catch (final Throwable t) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      if (debug()) {
        error(t);
      }
      throw new ServletException(t);
    }
  }

  public void sendJsonError(final HttpServletResponse resp,
                            final String msg) {
    try {
      resp.setStatus(HttpServletResponse.SC_OK);
      resp.setContentType("application/json; charset=UTF-8");

      final String json = "{\"status\": \"failed\", \"msg\": \"" + msg + "\"}";

      resp.setContentType("application/json; charset=UTF-8");

      final OutputStream os = resp.getOutputStream();

      final byte[] bytes = json.getBytes();

      resp.setContentLength(bytes.length);
      os.write(bytes);
      os.close();
    } catch (final Throwable ignored) {
      // Pretty much screwed if we get here
      if (debug()) {
        debug("Unable to send error: " + msg);
      }
    }
  }

  protected void sendOkJsonData(final HttpServletResponse resp) {
    final String json = "{\"status\": \"ok\"}";

    sendOkJsonData(resp, json);
  }

  protected void sendOkJsonData(final HttpServletResponse resp,
                                final String data) {
    try {
      resp.setStatus(HttpServletResponse.SC_OK);
      resp.setContentType("application/json; charset=UTF-8");

      final OutputStream os = resp.getOutputStream();

      final byte[] bytes = data.getBytes();

      resp.setContentLength(bytes.length);
      os.write(bytes);
      os.close();
    } catch (final Throwable ignored) {
      // Pretty much screwed if we get here
    }
  }

  public void outputJson(final HttpServletResponse resp,
                         final String etag,
                         final String[] header,
                         final Object val)
          throws ServletException {
    resp.setStatus(HttpServletResponse.SC_OK);

    if (etag != null) {
      resp.setHeader("etag", etag);
    }

    if (header != null) {
      resp.setHeader(header[0], header[1]);
    }

    resp.setContentType("application/json; charset=UTF-8");

    writeJson(resp, val);
    try {
      resp.getWriter().close();
    } catch (final IOException ioe) {
      throw new ServletException(ioe);
    }
  }

  protected String formatHTTPDate(final Timestamp val) {
    if (val == null) {
      return null;
    }

    synchronized (httpDateFormatter) {
      return httpDateFormatter.format(val) + "GMT";
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

