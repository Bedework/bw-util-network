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
package org.bedework.util.http;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicStatusLine;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Provide a way to get named values.
 *
 * @author douglm
 */
public class HttpUtil implements Serializable {
  public interface HeadersFetcher {
    Headers get();
  }

  private HttpUtil() {
  }

  public static CloseableHttpClient getClient(final boolean disableRedirects) {
    final HttpClientBuilder bldr =
            HttpClientBuilder.create();

    if (disableRedirects) {
      bldr.disableRedirectHandling();
    }

    return bldr.build();
  }

  public static CloseableHttpResponse doDelete(final CloseableHttpClient cl,
                                               final URI uri,
                                               final HeadersFetcher hdrs,
                                               final String acceptContentType)
          throws IOException {
    final Headers headers = ensureHeaders(hdrs);

    if (acceptContentType != null) {
      headers.add("Accept", acceptContentType);
    }

    final HttpDelete method = new HttpDelete(uri);

    method.setHeaders(headers.asArray());

    return cl.execute(method);
  }

  public static CloseableHttpResponse doGet(final CloseableHttpClient cl,
                                            final URI uri,
                                            final HeadersFetcher hdrs,
                                            final String acceptContentType)
          throws IOException {
    final Headers headers = ensureHeaders(hdrs);

    if (acceptContentType != null) {
      headers.add("Accept", acceptContentType);
    }

    final HttpGet httpGet = new HttpGet(uri);

    httpGet.setHeaders(headers.asArray());

    return cl.execute(httpGet);
  }

  public static CloseableHttpResponse doHead(final CloseableHttpClient cl,
                                             final URI uri,
                                             final HeadersFetcher hdrs,
                                             final String acceptContentType)
          throws IOException {
    final Headers headers = ensureHeaders(hdrs);

    if (acceptContentType != null) {
      headers.add("Accept", acceptContentType);
    }

    final HttpHead httphead = new HttpHead(uri);

    httphead.setHeaders(headers.asArray());

    return cl.execute(httphead);
  }

  public static CloseableHttpResponse doPost(final CloseableHttpClient cl,
                                             final URI uri,
                                             final HeadersFetcher hdrs,
                                             final String contentType,
                                             final String content)
          throws IOException {
    final StringEntity entity;
    if (content != null) {
      entity = new StringEntity(content);
    } else {
      entity = null;
    }

    return doPost(cl, uri, hdrs, entity);
  }

  public static CloseableHttpResponse doPost(final CloseableHttpClient cl,
                                             final URI uri,
                                             final HeadersFetcher hdrs,
                                             final HttpEntity entity)
          throws IOException {
    final Headers headers = ensureHeaders(hdrs);

    final HttpPost httpPost = new HttpPost(uri);

    httpPost.setHeaders(headers.asArray());

    if (entity != null) {
      httpPost.setEntity(entity);
    }

    return cl.execute(httpPost);
  }

  public static CloseableHttpResponse doPut(final CloseableHttpClient cl,
                                            final URI uri,
                                            final HeadersFetcher hdrs,
                                            final String contentType,
                                            final String content)
          throws IOException {
    final Headers headers = ensureHeaders(hdrs);

    if (contentType != null) {
      headers.add("Content-type", contentType);
    }

    final HttpPut httpPut = new HttpPut(uri);

    httpPut.setHeaders(headers.asArray());

    if (content != null) {
      final StringEntity entity = new StringEntity(content);
      httpPut.setEntity(entity);
    }

    return cl.execute(httpPut);
  }

  public static CloseableHttpResponse doReport(final CloseableHttpClient cl,
                                               final URI uri,
                                               final HeadersFetcher hdrs,
                                               final String depth,
                                               final String contentType,
                                               final String content)
          throws IOException {
    final Headers headers = ensureHeaders(hdrs);

    if (depth != null) {
      headers.add("depth", depth);
    }

    if (contentType != null) {
      headers.add("Content-type", contentType);
    }

    final HttpReport httpReport = new HttpReport(uri);

    httpReport.setHeaders(headers.asArray());

    if (content != null) {
      final StringEntity entity = new StringEntity(content);
      httpReport.setEntity(entity);
    }

    return cl.execute(httpReport);
  }

  public static CloseableHttpResponse doPropfind(final CloseableHttpClient cl,
                                                 final URI uri,
                                                 final HeadersFetcher hdrs,
                                                 final String depth,
                                                 final String contentType,
                                                 final String content)
          throws IOException {
    final Headers headers = ensureHeaders(hdrs);

    if (depth != null) {
      headers.add("depth", depth);
    }

    if (contentType != null) {
      headers.add("Content-type", contentType);
    }

    final HttpPropfind httpPropfind = new HttpPropfind(uri);

    httpPropfind.setHeaders(headers.asArray());

    if (content != null) {
      final StringEntity entity = new StringEntity(content);
      httpPropfind.setEntity(entity);
    }

    return cl.execute(httpPropfind);
  }

  private static Headers ensureHeaders(final HeadersFetcher hdrs) {
    if (hdrs == null) {
      return new Headers();
    }

    final Headers headers = hdrs.get();

    if (headers == null) {
      return new Headers();
    }

    // Return a copy so we can change it.
    final Headers nheaders = new Headers();
    nheaders.addAll(headers);

    return nheaders;
  }

  /**
   * @param name of header
   * @return value of header or null if no header
   */
  public static String getFirstHeaderValue(final HttpResponse resp,
                                           final String name) {
    final Header h = resp.getFirstHeader(name);

    if (h == null) {
      return null;
    }

    return h.getValue();
  }

  /** Specify the next method by name.
   *
   * @param name of the method
   * @param uri target
   * @return method object or null for unknown
   */
  public static HttpRequestBase findMethod(final String name,
                                           final URI uri) {
    final String nm = name.toUpperCase();

    if ("PUT".equals(nm)) {
      return new HttpPut(uri);
    }

    if ("GET".equals(nm)) {
      return new HttpGet(uri);
    }

    if ("DELETE".equals(nm)) {
      return new HttpDelete(uri);
    }

    if ("POST".equals(nm)) {
      return new HttpPost(uri);
    }

    if ("PROPFIND".equals(nm)) {
      return new HttpPropfind(uri);
    }

    if ("PROPPATCH".equals(nm)) {
      return new HttpPropPatch(uri);
    }

    if ("MKCALENDAR".equals(nm)) {
      return new HttpMkcalendar(uri);
    }

    if ("MKCOL".equals(nm)) {
      return new HttpMkcol(uri);
    }

    if ("OPTIONS".equals(nm)) {
      return new HttpOptions(uri);
    }

    if ("REPORT".equals(nm)) {
      return new HttpReport(uri);
    }

    if ("HEAD".equals(nm)) {
      return new HttpHead(uri);
    }

    return null;
  }

  /** Send content
   *
   * @param content the content as bytes
   * @param contentType its type
   * @throws HttpException if not entity enclosing request
   */
  public static void setContent(final HttpRequestBase req,
                                final byte[] content,
                                final String contentType) throws HttpException {
    if (content == null) {
      return;
    }

    if (!(req instanceof HttpEntityEnclosingRequestBase)) {
      throw new HttpException("Invalid operation for method " +
                                      req.getMethod());
    }

    final HttpEntityEnclosingRequestBase eem = (HttpEntityEnclosingRequestBase)req;

    final ByteArrayEntity entity = new ByteArrayEntity(content);
    entity.setContentType(contentType);
    eem.setEntity(entity);
  }

  public static int getStatus(final HttpResponse resp) {
    return resp.getStatusLine().getStatusCode();
  }

  /**
   * @return Status line
   */
  public static StatusLine getHttpStatus(final String statusLine) throws HttpException {
    final String[] splits = statusLine.split("\\s+");

    if ((splits.length < 2) ||
         (!splits[0].startsWith("HTTP/"))) {
      throw new HttpException("Bad status line: " + statusLine);
    }

    final String[] version = splits[0].substring(5).split(".");

    if (version.length != 2) {
      throw new HttpException("Bad status line: " + statusLine);
    }

    final HttpVersion hv = new HttpVersion(Integer.valueOf(version[0]),
                                           Integer.valueOf(version[1]));

    final int status = Integer.valueOf(splits[1]);

    final String reason;

    if (splits.length < 3) {
      reason = null;
    } else {
      reason = splits[2];
    }

    return new BasicStatusLine(hv, status, reason);
  }

  /**
   * @param status the code
   * @param reason some text
   * @return Correctly formatted string
   */
  public static String makeHttpStatus(final int status,
                                      final String reason) {
    return "HTTP/1.1 " + status + reason;
  }

  /**
   * @return Correctly formatted string
   */
  public static String makeOKHttpStatus() {
    return makeHttpStatus(HttpServletResponse.SC_OK, "OK");
  }

  /** Returns the String url from the request.
   *
   *  @param   request     incoming request
   *  @return  String      url from the request
   */
  public static String getUrl(final HttpServletRequest request) {
    try {
      final StringBuffer sb = request.getRequestURL();
      if (sb != null) {
        return sb.toString();
      }

      // Presumably portlet - see what happens with uri
      return request.getRequestURI();
    } catch (Throwable t) {
      return "BogusURL.this.is.probably.a.portal";
    }
  }
}
