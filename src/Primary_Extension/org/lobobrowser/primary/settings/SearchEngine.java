/*
    GNU GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The Lobo Project

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public
    License as published by the Free Software Foundation; either
    verion 2 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Contact info: lobochief@users.sourceforge.net
 */
package org.lobobrowser.primary.settings;

import java.io.Serializable;
import java.net.URL;

import org.eclipse.jdt.annotation.NonNull;

public class SearchEngine implements Serializable {
  private static final long serialVersionUID = 225745010000001000L;
  private final String name;
  private final String description;
  private final String baseUrl;
  private final String queryParameter;

  public SearchEngine(final String name, final String description, final String baseUrl, final String queryParameter) {
    super();
    this.name = name;
    this.description = description;
    this.baseUrl = baseUrl;
    this.queryParameter = queryParameter;
  }

  public @NonNull URL getURL(final String query) throws java.net.MalformedURLException {
    final String baseUrl = this.baseUrl;
    final int qmIdx = baseUrl.indexOf('?');
    final char join = qmIdx == -1 ? '?' : '&';
    try {
      return new java.net.URL(baseUrl + join + this.queryParameter + "=" + java.net.URLEncoder.encode(query, "UTF-8"));
    } catch (final java.io.UnsupportedEncodingException uee) {
      throw new IllegalStateException("not expected", uee);
    }
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getDescription() {
    return description;
  }

  public String getName() {
    return name;
  }

  public String getQueryParameter() {
    return queryParameter;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
