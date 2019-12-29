/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.util.http;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import java.net.URI;

/**
 * User: mike Date: 11/8/17 Time: 13:54
 */
public class HttpPropPatch extends HttpEntityEnclosingRequestBase {
  public static final String METHOD_NAME = "PROPPATCH";

  public HttpPropPatch() {
  }

  public HttpPropPatch(URI uri) {
    this.setURI(uri);
  }

  public HttpPropPatch(String uri) {
    this.setURI(URI.create(uri));
  }

  public String getMethod() {
    return METHOD_NAME;
  }
}

