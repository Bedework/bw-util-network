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
package org.bedework.util.dav;

import org.bedework.util.http.HttpUtil;
import org.bedework.util.http.PooledHttpClient;
import org.bedework.util.http.PooledHttpClient.ResponseHolder;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.misc.Util;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.XmlEmit.NameSpace;
import org.bedework.util.xml.XmlUtil;
import org.bedework.util.xml.tagdefs.WebdavTags;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicHeader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jakarta.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/** Helper for DAV interactions
*
* @author Mike Douglass  douglm - rpi.edu
*/
public class DavUtil implements Logged, Serializable {
  public static final int SC_MULTI_STATUS = 207; // not defined for some reason

  /** */
  public static final Header depth0 = new BasicHeader("depth", "0");
  /** */
  public static final Header depth1 = new BasicHeader("depth", "1");
  /** */
  public static final Header depthinf = new BasicHeader("depth", "infinity");

  /** Added to each request */
  protected List<Header> extraHeaders;

  private final Set<String> nameSpaces = new TreeSet<>();

  /**
   */
  public DavUtil() {
    addNs(WebdavTags.namespace);
  }

  /**
   */
  public DavUtil(final List<Header> extraHeaders) {
    this();

    if (!Util.isEmpty(extraHeaders)) {
      this.extraHeaders = new ArrayList<>(extraHeaders);
    }
  }

  public void addNs(final String val) {
    nameSpaces.add(val);
  }

  /** Represents the child of a collection
   *
   * @author Mike Douglass
   */
  public static class DavChild implements Comparable<DavChild>  {
    /** The href */
    public String uri;

    /** Always requested */
    public String displayName;

    /** Always requested */
    public boolean isCollection;

    /** Same order as supplied properties */
    public List<DavProp> propVals = new ArrayList<>();

    /* Extracted from returned resource types */
    public List<QName> resourceTypes = new ArrayList<>();

    /** */
    public int status;

    /**
     * @param nm a QName
     * @return DavProp or null
     */
    public DavProp findProp(final QName nm) {
      if (Util.isEmpty(propVals)) {
        return null;
      }

      for (final DavProp dp: propVals) {
        if (nm.equals(dp.name)) {
          return dp;
        }
      }

      return null;
    }
    @Override
    public int compareTo(final DavChild that) {
      if (isCollection != that.isCollection) {
        if (!isCollection) {
          return -1;
        }

        return 1;
      }

      if (displayName == null) {
        return -1;
      }

      if (that.displayName == null) {
        return 1;
      }

      return displayName.compareTo(that.displayName);
    }
  }

  /** Represents a property
   *
   * @author Mike Douglass
   */
  public static class DavProp {
    /** */
    public QName name;
    /** */
    public Element element;
    /** */
    public int status;
  }

  /** partially parsed propstat response element
   *
   * @author douglm
   */
  public static class PropstatElement {
    /** */
    public List<Element> props;

    /** */
    public int status;

    public Element error;

    /** May be null */
    public String responseDescription;
  }

  /** partially parsed multi-status response element. If we have an
   * href and status there will be no PropstatElements and the status
   * is set. Otherwise the status is in the propstat.
   *
   * @author douglm
   */
  public static class MultiStatusResponseElement {
    /** */
    public String href;

    public int status;

    /** */
    public List<PropstatElement> propstats = new ArrayList<>();

    public Element error;

    /** May be null */
    public String responseDescription;
  }

  /** partially parsed multi-status response
   *
   * @author douglm
   */
  public static class MultiStatusResponse {
    /** */
    public List<MultiStatusResponseElement> responses =
      new ArrayList<>();

    /** May be null */
    public String responseDescription;

    /** May be null */
    public String syncToken;
  }

  public MultiStatusResponse getMultiStatusResponse(final String val) {
    return getMultiStatusResponse(new ByteArrayInputStream(val.getBytes(
            StandardCharsets.UTF_8)));
  }

  public MultiStatusResponse getExtMkcolResponse(final String val) {
    return getExtMkcolResponse(new ByteArrayInputStream(val.getBytes(
            StandardCharsets.UTF_8)));
  }

  /**
   * @param in input stream
   * @return Collection<DavChild>
   */
  public MultiStatusResponse getMultiStatusResponse(final InputStream in) {
    final MultiStatusResponse res = new MultiStatusResponse();

    final Document doc = parseContent(in);

    final Element root = doc.getDocumentElement();

    /*    <!ELEMENT multistatus (response+, responsedescription?) > */

    expect(root, WebdavTags.multistatus);

    return getMsrEmcr(root, res);
  }

  /**
   * @param in input stream
   * @return Collection<DavChild>
   */
  public MultiStatusResponse getExtMkcolResponse(final InputStream in) {
    final MultiStatusResponse res = new MultiStatusResponse();

    final Document doc = parseContent(in);

    final Element root = doc.getDocumentElement();

    /*    <!ELEMENT multistatus (response+, responsedescription?) > */

    expect(root, WebdavTags.mkcolResponse);

    return getMsrEmcr(root, res);
  }

  private MultiStatusResponse getMsrEmcr(final Element root,
                                         final MultiStatusResponse res) {
    final Collection<Element> responses = getChildren(root);

    int count = 0; // validity
    for (final Element resp: responses) {
      count++;

      if (XmlUtil.nodeMatches(resp, WebdavTags.responseDescription)) {
        res.responseDescription = getElementContent(resp);
        continue;
      }

      if (XmlUtil.nodeMatches(resp, WebdavTags.syncToken)) {
        res.syncToken = getElementContent(resp);
        continue;
      }

      if (!XmlUtil.nodeMatches(resp, WebdavTags.response)) {
        throw new RuntimeException("Bad multistatus Expected " +
            "(response+, responsedescription?, sync-token) found " + resp);
      }

      /*    <!ELEMENT response (href, ((href*, status)|(propstat+)),
                          responsedescription?) >
       */
      final MultiStatusResponseElement msre = new MultiStatusResponseElement();
      res.responses.add(msre);

      final Iterator<Element> elit = getChildren(resp).iterator();

      Element nd = elit.next();

      if (!XmlUtil.nodeMatches(nd, WebdavTags.href)) {
        throw new RuntimeException("Bad response. Expected href found " + nd);
      }

      msre.href = getElementContent(nd);

      boolean hadStatus = false;

      while (elit.hasNext()) {
        nd = elit.next();

        if (XmlUtil.nodeMatches(nd, WebdavTags.status)) {
          hadStatus = true;
          if (!Util.isEmpty(msre.propstats)) {
            throw new RuntimeException(
                    "Bad response. Expected propstat found " + nd);
          }

          msre.status = httpStatus(nd);
          continue;
        }

        if (XmlUtil.nodeMatches(nd, WebdavTags.error)) {
          if (msre.error != null) {
            throw new RuntimeException("Bad response. Multiple error elements");
          }

          msre.error = nd;
          continue;
        }

        if (XmlUtil.nodeMatches(nd, WebdavTags.responseDescription)) {
          if (msre.responseDescription != null) {
            throw new RuntimeException("Bad response. Multiple responseDescription elements");
          }

          msre.responseDescription = getElementContent(nd);
          continue;
        }

        if (!XmlUtil.nodeMatches(nd, WebdavTags.propstat)) {
          throw new RuntimeException("Bad response. Expected propstat found " + nd);
        }

        if (hadStatus) {
          throw new RuntimeException("Bad response. Cannot have status and propstat");
        }

        /*    <!ELEMENT propstat (prop, status, responsedescription?) > */

        final PropstatElement pse = new PropstatElement();
        msre.propstats.add(pse);

        final Iterator<Element> propstatit = getChildren(nd).iterator();
        final Node propnd = propstatit.next();

        if (!XmlUtil.nodeMatches(propnd, WebdavTags.prop)) {
          throw new RuntimeException("Bad response. Expected prop found " + propnd);
        }

        if (!propstatit.hasNext()) {
          throw new RuntimeException("Bad response. Expected propstat/status");
        }

        pse.status = httpStatus(propstatit.next());

        if (propstatit.hasNext()) {
          final Node rdesc = propstatit.next();

          if (!XmlUtil.nodeMatches(rdesc, WebdavTags.responseDescription)) {
            throw new RuntimeException("Bad response, expected null or " +
                "responsedescription. Found: " + rdesc);
          }

          pse.responseDescription = getElementContent(resp);
        }

        /* process each property with this status */

        pse.props = getChildren(propnd);
      }
    }

    return res;
  }

  /** Do a synch report on the targeted collection.
   *
   *
   * @param cl http client
   * @param path of collection
   * @param syncToken from last report or null
   * @param props   null for a default set
   * @return Collection of DavChild or null for not found
   */
  public Collection<DavChild> syncReport(final PooledHttpClient cl,
                                         final String path,
                                         final String syncToken,
                                         final Collection<QName> props) {
    try {
      final StringWriter sw = new StringWriter();
      final XmlEmit xml = getXml();

      addNs(xml, WebdavTags.namespace);

      xml.startEmit(sw);

    /*
      <?xml version="1.0" encoding="utf-8" ?>
      <D:sync-collection xmlns:D="DAV:"
                         xmlns:C="urn:ietf:params:xml:ns:caldav">
        <D:sync-token/>
        <D:prop>
          <D:getetag/>
        </D:prop>
      </D:sync-collection>
     */
      xml.openTag(WebdavTags.syncCollection);
      if (syncToken == null) {
        xml.emptyTag(WebdavTags.syncToken);
      } else {
        xml.property(WebdavTags.syncToken, syncToken);
      }
      xml.property(WebdavTags.synclevel, "1");
      xml.openTag(WebdavTags.prop);
      xml.emptyTag(WebdavTags.getetag);

      if (props != null) {
        for (final QName pr : props) {
          if (pr.equals(WebdavTags.getetag)) {
            continue;
          }

          addNs(xml, pr.getNamespaceURI());
          xml.emptyTag(pr);
        }
      }

      xml.closeTag(WebdavTags.prop);
      xml.closeTag(WebdavTags.syncCollection);

      final ResponseHolder<Collection<DavChild>> resp =
              cl.report(path, "0", sw.toString(),
                        this::processSyncResponse);

      if (resp.failed) {
        return null;
      }

      return resp.response;
    } catch (final HttpException e) {
      throw new RuntimeException(e);
    }
  }

  final ResponseHolder<Collection<DavChild>> processSyncResponse(final String path,
                                                                 final CloseableHttpResponse resp) {
    try {
      final int status = HttpUtil.getStatus(resp);

      if (status != SC_MULTI_STATUS) {
        return new ResponseHolder<>(status,
                                    "Failed response from server");
      }

      if (resp.getEntity() == null) {
        return new ResponseHolder<>(status,
                                    "No content in response from server");
      }

      final InputStream is = resp.getEntity().getContent();

      final Document doc = parseContent(is);

      final Element root = doc.getDocumentElement();

      /*    <!ELEMENT multistatus (response+, responsedescription?) > */

      expect(root, WebdavTags.multistatus);

      return new ResponseHolder<>(processResponses(getChildren(root), null));
    } catch (final Throwable t) {
      return new ResponseHolder<>(t);
    }
  }

  /** Return the DavChild element for the targeted node.
   *
   * @param cl http client
   * @param path of resource
   * @param props   null for a default set
   * @return DavChild or null for not found
   */
  public DavChild getProps(final PooledHttpClient cl,
                           final String path,
                           final Collection<QName> props) {
    return makeDavChild(propfind(cl,
                                 normalizePath(path),
                                 props,
                                 "0"));
  }

  /**
   * @param cl the client
   * @param parentPath the path - "" for empty path
   * @param props   null for a default set
   * @return Collection<DavChild> - empty for no children - null for path not found.
   */
  public Collection<DavChild> getChildrenUrls(final PooledHttpClient cl,
                                              final String parentPath,
                                              final Collection<QName> props) {
    final String path = normalizePath(parentPath);

    final URI uri;
    try {
      uri = new URI(path);
    } catch(final URISyntaxException e) {
      throw new RuntimeException(e);
    }

    return processResponses(propfind(cl, path, props, "1"), uri);
  }

  /**
   * @param cl the client
   * @param path to resource
   * @param props   null for a default set
   * @param depth to set depth of operation
   * @return List<Element> from multi-status response
   */
  public List<Element> propfind(final PooledHttpClient cl,
                                final String path,
                                final Collection<QName> props,
                                final String depth) {
    try {
      final StringWriter sw = new StringWriter();
      final XmlEmit xml = getXml();

      addNs(xml, WebdavTags.namespace);

      xml.startEmit(sw);

      xml.openTag(WebdavTags.propfind);
      xml.openTag(WebdavTags.prop);
      xml.emptyTag(WebdavTags.displayname);
      xml.emptyTag(WebdavTags.resourcetype);

      if (props != null) {
        for (final QName pr : props) {
          if (pr.equals(WebdavTags.displayname)) {
            continue;
          }

          if (pr.equals(WebdavTags.resourcetype)) {
            continue;
          }

          addNs(xml, pr.getNamespaceURI());
          xml.emptyTag(pr);
        }
      }

      xml.closeTag(WebdavTags.prop);
      xml.closeTag(WebdavTags.propfind);

      final ResponseHolder<List<Element>> resp =
              cl.propfind(path,
                          depth,
                          sw.toString(),
                          this::processPropfindResponse);
      if (resp.failed) {
        if (debug()) {
          debug("Failed: status = " + resp.status + " msg=" + resp.message);
        }

        return null;
      }

      return resp.response;
    } catch (final HttpException e) {
      throw new RuntimeException(e);
    }
  }

  final ResponseHolder<List<Element>> processPropfindResponse(final String path,
                                                              final CloseableHttpResponse resp) {
    try {
      final int status = HttpUtil.getStatus(resp);

      if (status != SC_MULTI_STATUS) {
        return new ResponseHolder<>(status,
                                    "Failed response from server");
      }

      if (resp.getEntity() == null) {
        return new ResponseHolder<>(status,
                                    "No content in response from server");
      }

      final InputStream is = resp.getEntity().getContent();

      final Document doc = parseContent(is);

      final Element root = doc.getDocumentElement();

      /*    <!ELEMENT multistatus (response+, responsedescription?) > */

      expect(root, WebdavTags.multistatus);

      return new ResponseHolder<>(getChildren(root));
    } catch (final Throwable t) {
      return new ResponseHolder<>(t);
    }
  }

  /* ====================================================================
   *                   XmlUtil wrappers
   * ==================================================================== */

  protected XmlEmit getXml() {
    final XmlEmit xml = new XmlEmit();

    for (final String ns: nameSpaces) {
      addNs(xml, ns);
    }

    return xml;
  }


  /** Add a namespace
   *
   * @param xml emit object
   * @param val the namespace
   * @throws RuntimeException on error
   */
  protected void addNs(final XmlEmit xml,
                       final String val) {
    if (xml.getNameSpace(val) == null) {
      xml.addNs(new NameSpace(val, null), false);
    }
  }

  /** Parse the content, and return the DOM representation.
   *
   * @param in         content as stream
   * @return Document  Parsed body or null for no body
   * @exception RuntimeException Some error occurred.
   */
  protected Document parseContent(final InputStream in) {
    try {
      final DocumentBuilderFactory factory = DocumentBuilderFactory
              .newInstance();
      factory.setNamespaceAware(true);

      final DocumentBuilder builder = factory.newDocumentBuilder();

      return builder
              .parse(new InputSource(new InputStreamReader(in)));
    } catch (final Throwable t) {
      if (t instanceof RuntimeException) {
        throw (RuntimeException)t;
      }
      throw new RuntimeException(t);
    }
  }

  /** Parse a DAV error response
   *
   * @param in response
   * @return a single error element or null
   */
  public Element parseError(final InputStream in) {
    try {
      final Document doc = parseContent(in);

      final Element root = doc.getDocumentElement();

      expect(root, WebdavTags.error);

      final List<Element> els = getChildren(root);

      if (els.size() != 1) {
        return null;
      }

      return els.get(0);
    } catch (final Throwable ignored) {
      return null;
    }
  }

  /**
   * @param nd the node
   * @return List<Element>
   */
  public static List<Element> getChildren(final Node nd) {
    return XmlUtil.getElements(nd);
  }

  /**
   * @param nd the node
   * @return Element[]
   */
  public static Element[] getChildrenArray(final Node nd) {
    return XmlUtil.getElementsArray(nd);
  }

  /**
   * @param nd the node
   * @return Element
   */
  public static Element getOnlyChild(final Node nd) {
    return XmlUtil.getOnlyElement(nd);
  }

  /**
   * @param el element
   * @return String
   * @throws RuntimeException on error
   */
  public static String getElementContent(final Element el) {
    return XmlUtil.getElementContent(el);
  }

  /**
   * @param el xml element
   * @return boolean
   * @throws RuntimeException on error
   */
  public static boolean isEmpty(final Element el) {
    return XmlUtil.isEmpty(el);
  }

  /**
   * @param el xml element
   * @param tag QName
   * @throws RuntimeException on error
   */
  public static void expect(final Element el,
                            final QName tag) {
    if (!XmlUtil.nodeMatches(el, tag)) {
      throw new RuntimeException("Expected " + tag);
    }
  }

  /**
   * @param el - must be status element
   * @return int status
   * @throws RuntimeException on bad status
   */
  public static int httpStatus(final Element el) {
    if (!XmlUtil.nodeMatches(el, WebdavTags.status)) {
      throw new RuntimeException("Bad response. Expected status found " + el);
    }

    final String s = getElementContent(el);

    if (s == null) {
      throw new RuntimeException("Bad http status. Found null");
    }

    try {
      final int start = s.indexOf(" ");
      final int end = s.indexOf(" ", start + 1);

      if (end < 0) {
        return Integer.parseInt(s.substring(start + 1));
      }

      return Integer.parseInt(s.substring(start + 1, end));
    } catch (final Throwable t) {
      throw new RuntimeException("Bad http status. Found " + s);
    }
  }

  /*
  private static class Response implements DavResp {
//    private boolean debug;

    DavIo client;

    Response(final DavIo client, final boolean debug) throws DavioException {
      this.client = client;
//      this.debug = debug;
    }

    @Override
    public int getRespCode() throws DavioException {
      return client.getStatusCode();
    }

    @Override
    public String getContentType() throws DavioException {
      return client.getResponseContentType();
    }

    @Override
    public long getContentLength() throws DavioException {
      return client.getResponseContentLength();
    }

    @Override
    public String getCharset() throws DavioException {
      return client.getResponseCharSet();
    }

    @Override
    public InputStream getContentStream() throws DavioException {
      return client.getResponseBodyAsStream();
    }

    @Override
    public String getResponseBodyAsString() throws DavioException {
      return client.getResponseBodyAsString();
    }

    @Override
    public Header getResponseHeader(final String name) throws DavioException {
      return client.getResponseHeader(name);
    }

    @Override
    public String getResponseHeaderValue(final String name) throws DavioException {
      return client.getResponseHeaderValue(name);
    }

    @Override
    public void close() {
      try {
        client.release();
      } catch (Throwable t) {
        //error(t)
      }
    }
  }
*/

  /**
   * @param responses   null for a default set
   * @param parentURI   null or uri of collection to excluse response
   * @return Collection<DavChild> - empty for no children - null for path not found.
   */
  public Collection<DavChild> processResponses(final Collection<Element> responses,
                                               final URI parentURI) {
    if (responses == null) {
      return null;
    }

    final Collection<DavChild> result = new ArrayList<>();

    int count = 0; // validity
    for (final Element resp: responses) {
      count++;

      if (XmlUtil.nodeMatches(resp, WebdavTags.responseDescription)) {
        // Has to be last
        if (responses.size() > count) {
          throw new RuntimeException("Bad multstatus Expected " +
                                      "(response+, responsedescription?)");
        }

        continue;
      }

      if (XmlUtil.nodeMatches(resp, WebdavTags.syncToken)) {
        // We did a sync report. Add the token to the list

        final DavChild dc = new DavChild();

        final DavProp dp = new DavProp();
        dp.name = WebdavTags.syncToken;
        dp.element = resp;
        dp.status = HttpServletResponse.SC_OK;

        dc.propVals.add(dp);

        dc.status = HttpServletResponse.SC_OK;
        result.add(dc);

        continue;
      }

      if (!XmlUtil.nodeMatches(resp, WebdavTags.response)) {
        throw new RuntimeException("Bad multstatus Expected " +
                                    "(response+, responsedescription?) found " + resp);
      }

      final DavChild dc = makeDavResponse(resp);

      /* We get the collection back as well - check for it and skip it. */
      final URI childURI;
      try {
        childURI = new URI(dc.uri);
      } catch (final URISyntaxException use) {
        throw new RuntimeException(use);
      }

      if ((parentURI != null) &&
              parentURI.getPath().equals(childURI.getPath())) {
        continue;
      }

      result.add(dc);
    }

    return result;
  }

  /** Return the DavChild element for the targeted node.
   *
   * @param responseElements   null for a default set
   * @return DavChild or null for not found
   * @throws RuntimeException on error
   */
  private DavChild makeDavChild(final Collection<Element> responseElements) {
    if (responseElements == null) {
      // status 400
      return null;
    }

    DavChild dc = null;

    int count = 0; // validity
    for (final Element resp: responseElements) {
      count++;

      if (XmlUtil.nodeMatches(resp, WebdavTags.responseDescription)) {
        // Has to be last
        if (responseElements.size() > count) {
          throw new RuntimeException("Bad multstatus Expected " +
                                      "(response+, responsedescription?)");
        }

        continue;
      }

      if (!XmlUtil.nodeMatches(resp, WebdavTags.response)) {
        throw new RuntimeException("Bad multstatus Expected " +
                                    "(response+, responsedescription?) found " + resp);
      }

      if (dc != null){
        throw new RuntimeException("Bad multstatus Expected only 1 response");
      }

      dc = makeDavResponse(resp);
    }

    return dc;
  }

  private DavChild makeDavResponse(final Element resp) {
    /*    <!ELEMENT response (href, ((href*, status)|(propstat+)),
          responsedescription?) >
     */
    final Iterator<Element> elit = getChildren(resp).iterator();

    Element nd = elit.next();

    final DavChild dc = new DavChild();

    if (!XmlUtil.nodeMatches(nd, WebdavTags.href)) {
      throw new RuntimeException("Bad response. Expected href found " + nd);
    }

    dc.uri = URLDecoder.decode(getElementContent(nd),
                               StandardCharsets.UTF_8); // href should be escaped

    while (elit.hasNext()) {
      nd = elit.next();

      if (XmlUtil.nodeMatches(nd, WebdavTags.status)) {
        dc.status = httpStatus(nd);
        continue;
      }

      dc.status = HttpServletResponse.SC_OK;

      if (!XmlUtil.nodeMatches(nd, WebdavTags.propstat)) {
        throw new RuntimeException("Bad response. Expected propstat found " + nd);
      }

      /*    <!ELEMENT propstat (prop, status, responsedescription?) > */

      final Iterator<Element> propstatit = getChildren(nd).iterator();
      final Node propnd = propstatit.next();

      if (!XmlUtil.nodeMatches(propnd, WebdavTags.prop)) {
        throw new RuntimeException("Bad response. Expected prop found " + propnd);
      }

      if (!propstatit.hasNext()) {
        throw new RuntimeException("Bad response. Expected propstat/status");
      }

      final int st = httpStatus(propstatit.next());

      if (propstatit.hasNext()) {
        final Node rdesc = propstatit.next();

        if (!XmlUtil.nodeMatches(rdesc, WebdavTags.responseDescription)) {
          throw new RuntimeException("Bad response, expected null or " +
              "responsedescription. Found: " + rdesc);
        }
      }

      /* process each property with this status */

      final Collection<Element> respProps = getChildren(propnd);

      for (final Element pr: respProps) {
        /* XXX This needs fixing to handle content that is xml
         */
        if (XmlUtil.nodeMatches(pr, WebdavTags.resourcetype)) {
          final Collection<Element> rtypeProps = getChildren(pr);

          for (final Element rtpr: rtypeProps) {
            if (XmlUtil.nodeMatches(rtpr, WebdavTags.collection)) {
              dc.isCollection = true;
            }

            dc.resourceTypes.add(XmlUtil.fromNode(rtpr));
          }
        } else {
          final DavProp dp = new DavProp();

          dc.propVals.add(dp);

          dp.name = new QName(pr.getNamespaceURI(), pr.getLocalName());
          dp.status = st;
          dp.element = pr;

          if (XmlUtil.nodeMatches(pr, WebdavTags.displayname)) {
            dc.displayName = getElementContent(pr);
          }
        }
      }
    }

    return dc;
  }

  private String normalizePath(final String path) {
    if (!path.endsWith("/")) {
      return path + "/";
    }

    return path;
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
