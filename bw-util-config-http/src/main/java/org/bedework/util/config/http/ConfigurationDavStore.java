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
package org.bedework.util.config.http;

import org.bedework.util.config.ConfigBase;
import org.bedework.util.config.ConfigException;
import org.bedework.util.config.ConfigurationStore;
import org.bedework.util.dav.DavUtil;
import org.bedework.util.dav.DavUtil.DavChild;
import org.bedework.util.http.HttpUtil;
import org.bedework.util.http.PooledHttpClient;
import org.bedework.util.http.PooledHttpClient.ResponseHolder;
import org.bedework.util.misc.Util;

import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

import static org.apache.http.HttpStatus.SC_OK;

/** A configuration DAV store interacts with a DAV server to access
 * configurations. The remote end must support enough DAV to allow GET/PUT of
 * configurations and PROPFIND to discover child entities.
 *
 * @author Mike Douglass douglm
 */
public class ConfigurationDavStore implements ConfigurationStore {
  private String url;
  private final PooledHttpClient client;
  private final DavUtil du = new DavUtil();

  private final String path;

  /**
   * @param url of configs
   * @throws ConfigException on fatal error
   */
  public ConfigurationDavStore(final String url) throws ConfigException {
    try {
      this.url = url;

      if (!url.endsWith("/")) {
        this.url += "/";
      }

      URI u = new URI(url);

      client = new PooledHttpClient(u);

      path = u.getPath();
    } catch (final Throwable t) {
      throw new ConfigException(t);
    }
  }

  @Override
  public boolean readOnly() {
    return false;
  }

  @Override
  public String getLocation() {
    return url;
  }

  @Override
  public void saveConfiguration(final ConfigBase config) throws ConfigException {
    try {
      final StringWriter sw = new StringWriter();

      config.toXml(sw);

      client.put(path + config.getName() + ".xml",
                 sw.toString(),
                 "application/xml");
    } catch (final ConfigException ce) {
      throw ce;
    } catch (final Throwable t) {
      throw new ConfigException(t);
    }
  }

  @Override
  public ConfigBase getConfig(final String name) throws ConfigException {
    return getConfig(name, null);
  }

  @Override
  public ConfigBase getConfig(final String name,
                              final Class cl) throws ConfigException {
    try {
      final ResponseHolder resp = client.get(path + "/" + name + ".xml",
                                             "text/xml",
                                             this::processGetResponse);

      if (resp.failed) {
        return null;
      }

      final String xml = (String)resp.response;
      final ByteArrayInputStream bais =
              new ByteArrayInputStream(xml.getBytes());

      return new ConfigBase().fromXml(bais, cl);
    } catch (final ConfigException ce) {
      throw ce;
    } catch (final Throwable t) {
      throw new ConfigException(t);
    }
  }

  final ResponseHolder processGetResponse(final String path,
                                          final CloseableHttpResponse resp) {
    try {
      final int status = HttpUtil.getStatus(resp);

      if (status != SC_OK) {
        return new ResponseHolder(status,
                                  "Failed response from server");
      }

      if (resp.getEntity() == null) {
        return new ResponseHolder(status,
                                  "No content in response from server");
      }

      final InputStream is = resp.getEntity().getContent();

      return new ResponseHolder(Util.streamToString(is));
    } catch (final Throwable t) {
      return new ResponseHolder(t);
    }
  }

  @Override
  public List<String> getConfigs() throws ConfigException {
    try {
      final Collection<DavChild> dcs = du.getChildrenUrls(client, path, null);
      final List<String> names = new ArrayList<>();

      final URI parentUri = new URI(url);
      for (final DavChild dc: dcs) {
        if (dc.isCollection) {
          continue;
        }

        final String child = parentUri.relativize(new URI(dc.uri)).getPath();

        if (!child.endsWith(".xml")){
          continue;
        }

        names.add(child.substring(0, child.indexOf(".xml")));
      }

      return names;
    } catch (final Throwable t) {
      throw new ConfigException(t);
    }
  }

  @Override
  public ConfigurationStore getStore(final String name) throws ConfigException {
    try {
      String newPath = path + name;
      if (!newPath.endsWith("/")) {
        newPath += "/";
      }

      final DavChild dc = du.getProps(client, newPath, null);

      if (dc == null) {
        throw new ConfigException("mkcol not implemented");
      }

      final URI parentUri = new URI(url);
      return new ConfigurationDavStore(parentUri.relativize(new URI(dc.uri)).toASCIIString());
    } catch (final Throwable t) {
      throw new ConfigException(t);
    }
  }

  @Override
  public ResourceBundle getResource(final String name,
                                    final String locale)
          throws ConfigException {
    return null;
  }
}
