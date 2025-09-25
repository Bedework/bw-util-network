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

import org.bedework.util.jmx.ConfBase;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.servlet.MethodBase.MethodInfo;
import org.bedework.util.servlet.config.AppInfo;
import org.bedework.util.servlet.io.CharArrayWrappedResponse;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.tagdefs.WebdavTags;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Enumeration;
import java.util.HashMap;

import javax.xml.namespace.QName;

/** WebDAV Servlet.
 * This abstract servlet handles the request/response nonsense and calls
 * abstract routines to interact with an underlying data source.
 *
 * @author Mike Douglass   douglm@rpi.edu
 * @version 1.0
 */
public abstract class ServletBase extends HttpServlet
        implements Logged, HttpSessionListener, ServletContextListener {
  protected boolean dumpContent;
  protected boolean keepSession = true;

  /** Table of methods - set at init
   */
  protected HashMap<String, MethodInfo> methods = new HashMap<>();

  protected AppInfo appInfo;

  private SessionSerializer sessionSerializer;

  protected void addMethod(final String methodName,
                           final MethodInfo info) {
    methods.put(methodName, info);
  }

  /** Add methods for this namespace
   *
   */
  protected abstract void addMethods();

  /** Get appInfo for this namespace
   *
   */
  protected AppInfo getAppInfo() {
    return appInfo;
  }

  protected abstract void initMethodBase(MethodBase mb,
                                         ConfBase<?> conf,
                                         ServletContext context,
                                         boolean dumpContent,
                                         String methodName) throws ServletException;

  @Override
  public void init(final ServletConfig config) throws ServletException {
    super.init(config);

    dumpContent = "true".equals(config.getInitParameter("dumpContent"));

    final var ks = config.getInitParameter("keepSession");
    if (ks != null) {
      keepSession = "true".equals(ks);
    }

    final var ser = config.getInitParameter("serializeSession");
    if ("true".equals(ser)) {
      sessionSerializer = new SessionSerializer();
    }

    final var uai = config.getInitParameter("useAppInfo");
    if ("true".equals(uai)) {
      loadAppInfo();
    }

    addMethods();
  }

  private void loadAppInfo() {
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    try (final InputStream input =
                 classLoader.getResourceAsStream("appinfo.json")) {
      if (input == null) {
        error("appinfo.json not found");
        return;
      }
      appInfo = new ObjectMapper().readValue(input, AppInfo.class);
      appInfo.mapObjects();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** This method differs from the superclass method in the
   * following ways:<ul>
   * <li> The super method only allows for the base set of
   * methods. It does not allow for DAV methods like <b>PROPFIND</b>
   * for example.
   * </li>
   * <li> This class requires method handlers to be registered,
   * allowing for a more dynamic and configurable approach.
   * </li>
   * <li> This class allows the use of a session serializer
   * which will serialize multiple incoming requests from
   * the same source. This can help avoid issues caused by
   * double clicks on links. Probably unnecessary on lightweight
   * services but might help with services interacting with
   * an ORM, for example.
   * </li>
   * <li>Extra debugging is provided - including dumping
   * the content</li>
   * <li>Supports the "X-HTTP-Method-Override" header field.
   * This allows services to tunnel unsupported methods
   * through firewalls. For example, some firewalls reject
   * the <b>DAV PROPFIND</b> method. Instead, use something
   * like <b>POST</b> and set this header to the desired
   * method.</li>
   * <li>Uses the configurable <b>keepSession</b> property
   * to optionally invalidate the session on exit. Use for
   * stateless services, for example, <b>REST</b></li>
   * </ul>
   * @param req the incoming http request
   * @param resp the outgoing http response
   * @throws IOException on any generated exception
   */
  @Override
  protected void service(final HttpServletRequest req,
                         HttpServletResponse resp)
          throws IOException {
    try {
      setLoggerClass(this.getClass());

      if (debug()) {
        debug("entry: " + req.getMethod());
        dumpRequest(req);
      }

      if (sessionSerializer != null) {
        sessionSerializer.tryWait(req, true);
      }

      if (req.getCharacterEncoding() == null) {
        req.setCharacterEncoding("UTF-8");
        if (debug()) {
          debug("No charset specified in request; forced to UTF-8");
        }
      }

      if (debug() && dumpContent) {
        resp = new CharArrayWrappedResponse(resp);
      }

      String methodName = req.getHeader("X-HTTP-Method-Override");

      if (methodName == null) {
        methodName = req.getMethod();
      }

      final MethodBase method = getMethod(methodName);

      if (method == null) {
        info("No method for '" + methodName + "'");

        // ========================================================
        //     Set the correct response
        // ========================================================
      } else {
        if (method.beforeMethod(req, resp)) {
          method.doMethod(req, resp);
        }
      }
    } catch (final Throwable t) {
      handleException(t, resp);
    } finally {
      if (sessionSerializer != null) {
        try {
          sessionSerializer.tryWait(req, false);
        } catch (final Throwable ignored) {
        }
      }

      if (debug() && dumpContent &&
              (resp instanceof final CharArrayWrappedResponse wresp)) {
        /* instanceof check because we might get a subsequent exception before
         * we wrap the response
         */

        if (wresp.getUsedOutputStream()) {
          debug("------------------------ response written to output stream -------------------");
        } else {
          final String str = wresp.toString();

          debug("------------------------ Dump of response -------------------");
          debug(str);
          debug("---------------------- End dump of response -----------------");

          final byte[] bs = str.getBytes();
          resp = (HttpServletResponse)wresp.getResponse();
          debug("contentLength=" + bs.length);
          resp.setContentLength(bs.length);
          resp.getOutputStream().write(bs);
        }
      }

      if (!keepSession) {
        /* WebDAV for example is stateless - toss away the session */
        try {
          final HttpSession sess = req.getSession(false);
          if (sess != null) {
            sess.invalidate();
          }
        } catch (final Throwable ignored) {
        }
      }
    }
  }

  private void handleException(final Throwable t,
                               final HttpServletResponse resp) {
    try {
      error(t);
      sendError(t, resp);
    } catch (final Throwable ignored) {
      // Pretty much screwed if we get here
    }
  }

  private void sendError(final Throwable t,
                         final HttpServletResponse resp) {
    try {
      if (debug()) {
        debug("setStatus(" + HttpServletResponse.SC_INTERNAL_SERVER_ERROR + ")");
      }
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                     t.getMessage());
    } catch (final Throwable ignored) {
      // Pretty much screwed if we get here
    }
  }

  private boolean emitError(final QName errorTag,
                            final String extra,
                            final Writer wtr) {
    try {
      final XmlEmit xml = new XmlEmit();
//      syncher.addNamespace(xml);

      xml.startEmit(wtr);
      xml.openTag(WebdavTags.error);

      //    syncher.emitError(errorTag, extra, xml);

      xml.closeTag(WebdavTags.error);
      xml.flush();

      return true;
    } catch (final Throwable t1) {
      // Pretty much screwed if we get here
      return false;
    }
  }

  /**
   * @param name of method
   * @return method
   * @throws ServletException on error initialising or locating
   */
  public MethodBase getMethod(final String name) throws ServletException {
    final var mn = name.toUpperCase(); // Is this correct
    final MethodInfo mi = methods.get(mn);

//    if ((mi == null) || (getAnonymous() && mi.getRequiresAuth())) {
    //    return null;
    //}

    try {
      final MethodBase mb = mi.getMethodClass()
                              .getDeclaredConstructor()
                              .newInstance();

      initMethodBase(mb, getConfigurator(),
                     getServletContext(), dumpContent, mn);

      return mb;
    } catch (final Throwable t) {
      if (debug()) {
        error(t);
      }
      throw new ServletException(t);
    }
  }

  @Override
  public void sessionCreated(final HttpSessionEvent se) {
  }

  @Override
  public void sessionDestroyed(final HttpSessionEvent se) {
    final HttpSession session = se.getSession();
    final String sessid = session.getId();
    if (sessid == null) {
      return;
    }

    if (sessionSerializer != null) {
      sessionSerializer.removeSession(sessid);
    }
  }

  /** Debug
   *
   * @param req http request
   */
  public void dumpRequest(final HttpServletRequest req) {
    try {
      final Enumeration<String> hnames = req.getHeaderNames();

      String title = "Request headers";

      debug(title);

      while (hnames.hasMoreElements()) {
        final String key = hnames.nextElement();
        final String val = req.getHeader(key);
        debug("  " + key + " = \"" + val + "\"");
      }

      title = "Request parameters";

      debug(title + " - global info and uris");
      debug("getRemoteAddr = " + req.getRemoteAddr());
      debug("getRequestURI = " + req.getRequestURI());
      debug("getRemoteUser = " + req.getRemoteUser());
      debug("getRequestedSessionId = " + req.getRequestedSessionId());
      debug("HttpUtils.getRequestURL(req) = " + req.getRequestURL());
      debug("contextPath=" + req.getContextPath());
      debug("query=" + req.getQueryString());
      debug("contentlen=" + req.getContentLength());
      debug("request=" + req);
      debug("parameters:");

      debug(title);

      final Enumeration<String> pnames = req.getParameterNames();

      while (pnames.hasMoreElements()) {
        final String key = pnames.nextElement();
        final String val = req.getParameter(key);
        debug("  " + key + " = \"" + val + "\"");
      }
    } catch (final Throwable ignored) {
    }
  }

  /* ---------------------------------------------------------------
   *                         JMX support
   */

  protected abstract ConfBase<?> getConfigurator();

  @Override
  public void contextInitialized(final ServletContextEvent sce) {
    getConfigurator().start();
  }

  @Override
  public void contextDestroyed(final ServletContextEvent sce) {
    getConfigurator().stop();
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
