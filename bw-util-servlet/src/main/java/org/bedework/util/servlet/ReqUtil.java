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

import org.bedework.util.misc.Util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/** Class to handle the incoming request.
 *
 * @author Mike Douglass
 */
public class ReqUtil implements Serializable {
  protected HttpServletRequest request;
  protected HttpServletResponse response;

  protected boolean errFlag;

  /**
   * @param request
   * @param response
   */
  public ReqUtil(final HttpServletRequest request,
                 final HttpServletResponse response) {
    this.request = request;
    this.response = response;
  }

  /**
   * @return HttpServletRequest
   */
  public HttpServletRequest getRequest() {
    return request;
  }

  /**
   * @return HttpServletResponse
   */
  public HttpServletResponse getResponse() {
    return response;
  }

  /**
   * @param val
   */
  public void setErrFlag(final boolean val) {
    errFlag = val;
  }

  /**
   * @return boolean
   */
  public boolean getErrFlag() {
    return errFlag;
  }

  /** Get a request parameter stripped of white space. Return null for zero
   * length.
   *
   * @param name    name of parameter
   * @return  String   value
   */
  public String getReqPar(final String name) {
    return Util.checkNull(request.getParameter(name));
  }

  /** Get a request parameter stripped of white space. Return default for null
   * or zero length.
   *
   * @param name    name of parameter
   * @param def default value
   * @return  String   value
   */
  public String getReqPar(final String name, final String def) {
    final String s = Util.checkNull(request.getParameter(name));

    if (s != null) {
      return s;
    }

    return def;
  }

  /** See if a request parameter is present
   *
   * @param name    name of parameter
   * @return  boolean true for present
   */
  public boolean present(final String name) {
    return request.getParameter(name) != null;
  }

  /** See if a request parameter is absent or has an empty
   * value. Returns true for not present or no value or
   * zero length value
   *
   * @param name    name of parameter
   * @return  boolean true for empty
   */
  public boolean empty(final String name) {
    final var par = request.getParameter(name);
    return (par == null) || par.isEmpty();
  }

  /** See if a request parameter is present and not null or empty
   *
   * @param name    name of parameter
   * @return  boolean true for present and not null
   */
  public boolean notNull(final String name) {
    return getReqPar(name) != null;
  }

  /** Get a multi-valued request parameter stripped of white space.
   * Return null for zero length.
   *
   * @param name    name of parameter
   * @return  List<String> or null
   */
  public List<String> getReqPars(final String name) {
    final String[] s = request.getParameterValues(name);
    ArrayList<String> res = null;

    if ((s == null) || (s.length == 0)) {
      return null;
    }

    for (String par: s) {
      par = Util.checkNull(par);
      if (par != null) {
        if (res == null) {
          res = new ArrayList<>();
        }

        res.add(par);
      }
    }

    return res;
  }

  /** Get an Integer request parameter or null.
   *
   * @param name    name of parameter
   * @return  Integer   value or null
   */
  public Integer getIntReqPar(final String name) {
    final String reqpar = getReqPar(name);

    if (reqpar == null) {
      return null;
    }

    return Integer.valueOf(reqpar);
  }

  /** Get an integer valued request parameter.
   *
   * @param name    name of parameter
   * @param defaultVal default to return if missing or invalid
   * @return  int   value
   */
  public int getIntReqPar(final String name,
                          final int defaultVal) {
    final String reqpar = getReqPar(name);

    if (reqpar == null) {
      return defaultVal;
    }

    try {
      return Integer.parseInt(reqpar);
    } catch (final Throwable t) {
      return defaultVal; // XXX exception?
    }
  }

  /** Get a Long request parameter or null.
   *
   * @param name    name of parameter
   * @return  Long   value or null
   */
  public Long getLongReqPar(final String name) {
    final String reqpar = getReqPar(name);

    if (reqpar == null) {
      return null;
    }

    return Long.valueOf(reqpar);
  }

  /** Get an long valued request parameter.
   *
   * @param name    name of parameter
   * @param defaultVal default to return if missing or invalid
   * @return  long  value
   */
  public long getLongReqPar(final String name,
                            final long defaultVal) {
    final String reqpar = getReqPar(name);

    if (reqpar == null) {
      return defaultVal;
    }

    try {
      return Long.parseLong(reqpar);
    } catch (final Throwable t) {
      return defaultVal; // XXX exception?
    }
  }

  /** Get a boolean valued request parameter.
   *
   * @param name    name of parameter
   * @return  Boolean   value or null for absent parameter
   */
  public Boolean getBooleanReqPar(final String name) {
    String reqpar = getReqPar(name);

    if (reqpar == null) {
      return null;
    }

    try {
      if (reqpar.equalsIgnoreCase("yes")) {
        reqpar = "true";
      }

      if (reqpar.equalsIgnoreCase("on")) {
        reqpar = "true";
      }

      return Boolean.valueOf(reqpar);
    } catch (final Throwable ignored) {
      return null; // XXX exception?
    }
  }

  /** Get a boolean valued request parameter giving a default value.
   *
   * @param name    name of parameter
   * @param defVal default value for absent parameter
   * @return  boolean   value
   */
  public boolean getBooleanReqPar(final String name,
                                  final boolean defVal) {
    boolean val = defVal;
    final Boolean valB = getBooleanReqPar(name);
    if (valB != null) {
      val = valB;
    }

    return val;
  }

  /** Set the value of a named session attribute.
   *
   * @param attrName    Name of the attribute
   * @param val         Object
   */
  public void setSessionAttr(final String attrName,
                             final Object val) {
    request.getSession().setAttribute(attrName, val);
  }

  /** Remove a named session attribute.
   *
   * @param attrName    Name of the attribute
   */
  public void removeSessionAttr(final String attrName) {
    final HttpSession sess = request.getSession(false);

    if (sess == null) {
      return;
    }

    sess.removeAttribute(attrName);
  }

  /** Return the value of a named session attribute.
   *
   * @param attrName    Name of the attribute
   * @return Object     Attribute value or null
   */
  public Object getSessionAttr(final String attrName) {
    final HttpSession sess = request.getSession(false);

    if (sess == null) {
      return null;
    }

    return sess.getAttribute(attrName);
  }

  /** Set the value of a named request attribute.
   *
   * @param attrName    Name of the attribute
   * @param val         Object
   */
  public void setRequestAttr(final String attrName,
                             final Object val) {
    request.setAttribute(attrName, val);
  }

  /** Return the value of a named request attribute.
   *
   * @param attrName    Name of the attribute
   * @return Object     Attribute value or null
   */
  public Object getRequestAttr(final String attrName) {
    return request.getAttribute(attrName);
  }

  /**
   * @return String remote address
   */
  public String getRemoteAddr() {
    return request.getRemoteAddr();
  }

  /**
   * @return String remote host
   */
  public String getRemoteHost() {
    return request.getRemoteHost();
  }

  /**
   * @return int remote port
   */
  public int getRemotePort() {
    return request.getRemotePort();
  }

  /** If there is no Accept-Language header returns null, otherwise returns a
   * collection of Locales ordered with preferred first.
   *
   * @return Collection of locales or null
   */
  public Collection<Locale> getLocales() {
    return HttpServletUtils.getLocales(request);
  }
}
