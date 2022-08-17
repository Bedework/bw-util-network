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
package org.bedework.util.servlet.filters;

import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.misc.Util;
import org.bedework.util.servlet.HttpServletUtils;
import org.bedework.util.servlet.io.ByteArrayWrappedResponse;
import org.bedework.util.servlet.io.PooledBufferedOutputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import static java.lang.String.format;

/** Class to implement a basic XSLT filter. The final configuration of this
 *  object can be carried out by overriding init.
 *  <p>Loosely based on some public example of filter code.</p>
 */
public class XSLTFilter extends AbstractFilter implements Logged {
  /** A transformer is identified by a path like key of locale + browser +
   * skin name (with path delimiters).
   *
   * <p>Thsi may not be the actual path as components may be defaulted.
   * pathMap maps the 'ideal' path on to the actual path to the skin.
   */
  private static final HashMap<String, String> pathMap =
          new HashMap<>();

  private static final HashMap<String, Transformer> transformers =
          new HashMap<>();

  /** This can be set in the web.xml configuration to run with a single
   * transformer
   */
  private String configUrl;

  private boolean ignoreContentType;

  private final TransformerFactory tf;

  /** globals
   *
   */
  public static class XsltGlobals extends AbstractFilter.FilterGlobals {
    /** Url for the next transform.
     */
    public String url;

    /** Reason we had a problem
     */
    public String reason = null;
  }

  public XSLTFilter() {
    if ("yes".equals(
            System.getProperty("org.bedework.use.saxon"))) {
      tf = (TransformerFactory)Util.getObject(
              "net.sf.saxon.TransformerFactoryImpl",
              TransformerFactory.class);
    } else {
      tf = TransformerFactory.newInstance();
    }
  }

  /**
   * @param req HTTP Servlet Request
   * @return our globals
   */
  public XsltGlobals getXsltGlobals(final HttpServletRequest req) {
    return (XsltGlobals)getGlobals(req);
  }

  @Override
  public AbstractFilter.FilterGlobals newFilterGlobals() {
    return new XsltGlobals();
  }

  /** Set the url to be used for the next transform.
   *
   * @param req HTTP Servlet Request
   * @param ideal ideal or virtual path
   */
  public void setUrl(final HttpServletRequest req, final String ideal) {
    getXsltGlobals(req).url = ideal;
  }

  /** Get the url to be used for the next transform.
   *
   * @param req HTTP Servlet Request
   * @return url
   */
  public String getUrl(final HttpServletRequest req) {
    return getXsltGlobals(req).url;
  }

  /** Set ideal to actual mapping.
   *
   * @param ideal ideal or virtual path
   * @param actual an actual path
   */
  public void setPath(final String ideal, final String actual) {
    synchronized (transformers) {
      pathMap.put(ideal, actual);
    }
  }

  /** Get the url to be used for the next transform after mapping with pathMap.
   *
   * @param ideal ideal or virtual path
   * @return url
   */
  public String lookupPath(final String ideal) {
    return pathMap.get(ideal);
  }

  /** Flush all the transformers - for ALL clients
   */
  public static void flushXslt() {
    synchronized (transformers) {
      transformers.clear();
      pathMap.clear();
    }
  }

  /** This method will only do something if there is no current XML transformer.
   *  A previous call to setXslt will discard any previous transformer.
   *  <p>Subclasses could call setXslt then call this method to check that the
   *  stylesheet is valid. A TransformerException provides inforamtion about
   *  where any error occuured.
   *
   * @param ideal          'ideal' path of the stylesheet
   * @return  Transformer  Existing or new XML transformer
   * @throws TransformerException
   * @throws ServletException
   * @throws FileNotFoundException
   */
  public Transformer getXmlTransformer(final String ideal)
      throws TransformerException, ServletException, FileNotFoundException {
    final String url = lookupPath(ideal);
    if (debug()) {
      debug("getXmlTransformer: ideal = " + ideal +
                        " actual = " + url);
    }
    Transformer trans = transformers.get(url);

    if (trans != null) {
      return trans;
    }

    try {
      trans = tf.newTransformer(new StreamSource(url));
    } catch (final TransformerConfigurationException tce) {
      /* Work our way down the chain to see if we have an embedded file
       * not found. If so, throw that to let the caller try another path.
       */
      Throwable cause = tce.getCause();
      while (cause instanceof TransformerException) {
        cause = cause.getCause();
      }

      if (!(cause instanceof FileNotFoundException)) {
        throw tce;
      }

      throw (FileNotFoundException)cause;
    } catch (final Exception e) {
      error("Could not initialize transform for " + url);
      error(e);
      throw new ServletException("Could not initialize transform for " + url, e);
    } finally {
  /*    if (is != null) {
        try {
          is.close();
        } catch (Exception fe) {}
      } */
    }

    synchronized (transformers) {
      final Transformer trans2 = transformers.get(url);

      if (trans2 != null) {
        // somebody beat us to it.
        return trans2;
      }

      transformers.put(url, trans);
    }

    if (debug()) {
      debug(format("Obtained new transformer with class %s" +
                           " from factory class %s",
                   trans.getClass(), tf.getClass()));
    }

    return trans;
  }

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
    super.init(filterConfig);

    configUrl = filterConfig.getInitParameter("xslt");

    if ((configUrl != null) && debug()) {
      debug("Filter " + filterConfig.getFilterName() +
                                        " using xslt " + configUrl);
    }

    final String temp = filterConfig.getInitParameter("ignoreContentType");

    ignoreContentType = "true".equals(temp);
  }

  @Override
  public void doFilter(final ServletRequest req,
                       final ServletResponse response,
                       final FilterChain filterChain)
         throws IOException, ServletException {
    final HttpServletRequest hreq = (HttpServletRequest)req;
    final HttpServletResponse resp = (HttpServletResponse)response;
    final long startTime = System.currentTimeMillis();

    final PooledBufferedOutputStream pbos = new PooledBufferedOutputStream();

    final WrappedResponse wrappedResp = new WrappedResponse(resp, hreq);

    filterChain.doFilter(req, wrappedResp);

    final XsltGlobals glob = getXsltGlobals(hreq);

    glob.reason = null;

    if (debug()) {
      debug("XSLTFilter: Accessing filter for " +
                                HttpServletUtils.getReqLine(hreq) + " " +
                                hreq.getMethod() +
                                " response class: " + resp.getClass().getName());
      debug("XSLTFilter: response: " + resp);
    }

    /* We don't get a session till we've been through to the servlet.
     */
    final HttpSession sess = hreq.getSession(false);
    final String sessId;
    if (sess == null) {
      sessId = "NONE";
    } else {
      sessId = sess.getId();
    }

    logTime("PRETRANSFORM", sessId,
            System.currentTimeMillis() - startTime);

    /* Ensure we're all set up to handle content
     */
    doPreFilter(hreq);

    //byte[] bytes = wrappedResp.toByteArray();

    if (wrappedResp.size() == 0) {
      if (debug()) {
        debug("No content");
      }

//      xformNeeded[0] = false;
      wrappedResp.setTransformNeeded(false);
      glob.reason = "No content";
    }

    try {
      if ((!glob.dontFilter) && (wrappedResp.getTransformNeeded())) {
        if (debug()) {
          debug("+*+*+*+*+*+*+*+*+*+*+* about to transform: len=" +
              wrappedResp.size());
        }
        //debug(new String(bytes));

        TransformerException te = null;
        Transformer xmlt = null;

        try {
          xmlt = getXmlTransformer(glob.url);
        } catch (final TransformerException te1) {
          te = te1;
        }

        if (xmlt == null) {
          outputErrorMessage("No xml transformer",
                             "Unable to obtain an XML transformer probably " +
                                 "due to a previous error. Server logs may " +
                                 "help determine the cause.",
                                 pbos);
          glob.contentType = "text/html";
        } else if (te != null) {
          /* We had an exception getting the transformer.
              Output error information instead of the transformed output
           */

          outputInitErrorInfo(te, pbos);
          glob.contentType = "text/html";
        } else {
          /* We seem to be getting invalid bytes occasionally
          for (int i = 0; i < bytes.length; i++) {
            if ((bytes[i] & 0x0ff) > 128) {
              warn("Found byte > 128 at " + i +
                             " bytes = " + (bytes[i] & 0x0ff));
              bytes[i] = (int)('?');
            }
          }
          The above commented out. It breaks Unicode characters
           */
          /* Managed to get a transformer. Do the thing.
            */
          try {
            /* The choice is a pool of transformers per thread or to
               synchronize during the transform.

               Transforms appear to be fast, for a dynamic site they take a
               small proportiuon of the response time. Try synch for now.
             */
            synchronized (xmlt) {
              xmlt.transform(
                  new StreamSource(
                       new InputStreamReader(
                               wrappedResp.getInputStream(),
                               StandardCharsets.UTF_8)),
                  new StreamResult(pbos));
            }
          } catch (final TransformerException e) {
            outputTransformErrorInfo(e, pbos);
            glob.contentType = "text/html";
          }

          if (debug()) {
            final Properties pr = xmlt.getOutputProperties();
            if (pr != null) {
              for (final String key: pr.stringPropertyNames()) {
                debug("--------- xslt-output property " +
                                key + "=" + pr.getProperty(key));
              }
            }
          }
        }

        if (glob.contentType != null) {
          /* Set explicitly by caller.
           */
          resp.setContentType(glob.contentType);
        } else {
          /* The encoding and media type should be available from the
           *  Transformer. Letting the stylesheet dictate the media-type
           *  is the right thing to do as only the stylesheet knows what
           *  it's producing.
           */
          final Properties pr = xmlt.getOutputProperties();
          if (pr != null) {
            final String encoding = pr.getProperty("encoding");
            final String mtype = pr.getProperty("media-type");

            if (mtype != null) {
              if (debug()) {
                debug("Stylesheet set media-type to " + mtype);
              }
              if (encoding != null) {
                resp.setContentType(mtype + ";charset=" + encoding);
              } else {
                resp.setContentType(mtype);
              }
            }
          }
        }

        resp.setContentLength(pbos.size());
        pbos.writeTo(resp.getOutputStream());

        if (debug()) {
          debug("XML -> HTML conversion completed");
        }
      } else {
        if (debug()) {
          if (glob.dontFilter) {
            glob.reason = "dontFilter";
          }

          if (glob.reason == null) {
            glob.reason = "Unknown";
          }

          debug("+*+*+*+*+*+*+*+*+*+*+* transform suppressed" +
                          " reason = " + glob.reason);
        }
        resp.setContentLength(wrappedResp.size());
        wrappedResp.writeTo(resp.getOutputStream());
        if (glob.contentType != null) {
          /* Set explicitly by caller.
           */
          resp.setContentType(glob.contentType);
        }
      }
    } catch (final Throwable t) {
      /* We're seeing tomcat specific exceptions here when the client aborts.
          Try to detect these without making this code tomcat specific.
       */
      if ("org.apache.catalina.connector.ClientAbortException".equals(t.getClass().getName())) {
        warn("ClientAbortException: dropping response");
      } else if ("Connection reset by peer".equals(t.getMessage())) {
        warn("Connection reset by peer: dropping response");
      } else {
        error("Unable to transform document");
        error(t);
      }
    } finally {
      wrappedResp.release();
      wrappedResp.close();

      try {
        pbos.release();
      } catch (final Exception ignored) {}
    }

    logTime("POSTTRANSFORM", sessId,
            System.currentTimeMillis() - startTime);
  }

  private class WrappedResponse extends ByteArrayWrappedResponse {
    /* For xslt transformations */
    protected boolean transformNeeded = false;
    protected HttpServletRequest req;

    /**
     * @param response HTTP Servlet Respone
     * @param req HTTP Servlet Request
     */
    public WrappedResponse(final HttpServletResponse response,
                           final HttpServletRequest req) {
      super(response);
      this.req = req;
    }

    /**
     * @param val true if transform needed
     */
    public void setTransformNeeded(final boolean val) {
      transformNeeded = val;
    }

    /**
     * @return true for need to transform
     */
    public boolean getTransformNeeded() {
      return transformNeeded;
    }

    @Override
    public void setContentType(final String type) {
      final XsltGlobals glob = getXsltGlobals(req);

      if (ignoreContentType) {
        transformNeeded = true;
      } else if ((type.startsWith("text/xml")) ||
                 (type.startsWith("application/xml"))) {
        if (debug()) {
          debug("XSLTFilter: Converting xml to html");
        }
        transformNeeded = true;
      } else {
        super.setContentType(type);
        if (debug()) {
          glob.reason = "Content-type = " + type;
        }
      }
    }
  }

  @Override
  public void destroy() {
    super.destroy();
  }

  private void outputInitErrorInfo(final TransformerException te,
                                   final OutputStream wtr) {
    final PrintWriter pw = new PrintWriter(wtr);

    outputErrorHtmlHead(pw, "XSLT initialization errors");
    pw.println("<body>");

    final SourceLocator sl = te.getLocator();

    if (sl != null) {
      pw.println("<table>");
      outputErrorTr(pw, "Line", "" + sl.getLineNumber());
      outputErrorTr(pw, "Column", "" + sl.getColumnNumber());
      pw.println("</table>");
    }

    outputErrorException(pw, te.getCause());

    pw.println("</body>");
    pw.println("</html>");
  }

  private void outputTransformErrorInfo(final Exception e,
                                        final OutputStream wtr) {
    final PrintWriter pw = new PrintWriter(wtr);

    outputErrorHtmlHead(pw, "XSLT transform error");
    pw.println("<body>");

    outputErrorPara(pw, "There was an error transforming content.");
    outputErrorPara(pw, "This is possibly due to incorrectly formatted " +
                        "content.");
    outputErrorPara(pw, "Following is a trace to help us locate the cause.");

    outputErrorException(pw, e);

    pw.println("</body>");
    pw.println("</html>");
  }

  @SuppressWarnings("SameParameterValue")
  private void outputErrorMessage(final String title,
                                  final String para,
                                  final OutputStream wtr) {
    final PrintWriter pw = new PrintWriter(wtr);

    outputErrorHtmlHead(pw, title);
    pw.println("<body>");

    outputErrorPara(pw, para);
    pw.println("</body>");
    pw.println("</html>");
  }

  private void outputErrorHtmlHead(final PrintWriter pw, final String head) {
    pw.println("<html>");
    pw.println("<head>");
    pw.println("<title>" + head + "</title>");
    pw.println("</head>");
  }

  private void outputErrorTr(final PrintWriter pw, final String s1, final String s2) {
    pw.println("<tr>");
    pw.println("<td>" + s1 + "</td>");
    pw.println("<td>" + s2 + "</td>");
    pw.println("</tr>");
  }

  private void outputErrorPara(final PrintWriter pw, final String s) {
    pw.println("<p>");
    pw.println(s);
    pw.println("</p>");
  }

  private void outputErrorException(final PrintWriter pw, final Throwable e) {
    pw.println("<h2>Cause:</h2>");

    if (e == null) {
      pw.println("<br />********Unknown<br />");
    } else {
      pw.println("<pre>");
      e.printStackTrace(pw);
      pw.println("</pre>");
    }
  }

  private void logTime(final String recId, final String sessId, final long timeVal) {
    info(new StringBuilder(recId)
                 .append(":")
                 .append(sessId)
                 .append(":")
                 .append(timeVal)

                 .toString());
  }

  /* ====================================================================
   *                   Logged methods
   * ==================================================================== */

  private final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}

