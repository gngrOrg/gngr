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
package org.lobobrowser.request;

import java.net.URL;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.clientlet.ClientletRequest;
import org.lobobrowser.clientlet.Header;
import org.lobobrowser.ua.ParameterInfo;
import org.lobobrowser.ua.RequestType;
import org.lobobrowser.ua.UserAgent;

public class ClientletRequestImpl implements ClientletRequest {
  private final String method;
  private final @NonNull URL url;
  private final ParameterInfo paramInfo;
  private final Header[] extraHeaders;
  private final boolean forNewWindow;
  private final String referrer;
  private final String altPostData;
  private final RequestType requestType;

  public ClientletRequestImpl(final boolean forNewWindow, final @NonNull URL url, final String method, final ParameterInfo paramInfo,
      final Header[] extraHeaders,
      final String referrer, final String altPostData, final RequestType requestType) {
    this.method = method;
    this.url = url;
    this.paramInfo = paramInfo;
    this.extraHeaders = extraHeaders;
    this.forNewWindow = forNewWindow;
    this.referrer = referrer;
    this.altPostData = altPostData;
    this.requestType = requestType;
  }

  public ClientletRequestImpl(final boolean forNewWindow, final @NonNull URL url, final String method, final ParameterInfo paramInfo,
      final RequestType requestType) {
    this(forNewWindow, url, method, paramInfo, null, null, null, requestType);
  }

  public ClientletRequestImpl(final @NonNull URL url, final RequestType requestType) {
    this(false, url, "GET", null, null, null, null, requestType);
  }

  public ClientletRequestImpl(final @NonNull URL url, final String method, final String altPostData, final RequestType requestType) {
    this(false, url, method, null, null, null, altPostData, requestType);
  }

  public Header[] getExtraHeaders() {
    return this.extraHeaders;
  }

  public String getMethod() {
    return this.method;
  }

  public ParameterInfo getParameterInfo() {
    return this.paramInfo;
  }

  public @NonNull URL getRequestURL() {
    return this.url;
  }

  public UserAgent getUserAgent() {
    return UserAgentImpl.getInstance();
  }

  public boolean isGetRequest() {
    return "GET".equalsIgnoreCase(method);
  }

  public boolean isNewWindowRequest() {
    return this.forNewWindow;
  }

  public boolean isPostRequest() {
    return "POST".equalsIgnoreCase(this.method);
  }

  public String getReferrer() {
    return this.referrer;
  }

  public String getAltPostData() {
    return this.altPostData;
  }

  public RequestType getRequestType() {
    return this.requestType;
  }
}
