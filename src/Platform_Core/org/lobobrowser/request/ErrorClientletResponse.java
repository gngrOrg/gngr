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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.clientlet.ClientletResponse;
import org.lobobrowser.ua.RequestType;

/**
 * Synthetic {@link ClientletResponse} used when a request fails before a
 * server response is obtained (for example, DNS resolution or connection errors).
 */
public final class ErrorClientletResponse implements ClientletResponse {
  private final @NonNull URL responseURL;
  private final String lastRequestMethod;
  private final RequestType requestType;
  private final Throwable exception;

  public ErrorClientletResponse(final @NonNull URL responseURL, final String lastRequestMethod,
      final RequestType requestType, final Throwable exception) {
    this.responseURL = responseURL;
    this.lastRequestMethod = lastRequestMethod;
    this.requestType = requestType;
    this.exception = exception;
  }

  @Override
  public @NonNull URL getResponseURL() {
    return this.responseURL;
  }

  @Override
  public String getLastRequestMethod() {
    return this.lastRequestMethod;
  }

  @Override
  public String getHeader(final String name) {
    return null;
  }

  @Override
  public String[] getHeaders(final String name) {
    return null;
  }

  @Override
  public Iterator<String> getHeaderNames() {
    return Collections.emptyIterator();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    throw new IOException("No response stream available for failed request: " + this.responseURL, this.exception);
  }

  @Override
  public String getContentType() {
    return null;
  }

  @Override
  public String getMimeType() {
    return null;
  }

  @Override
  public boolean matches(final String mimeType, final String[] fileExtension) {
    return false;
  }

  @Override
  public int getContentLength() {
    return -1;
  }

  @Override
  public boolean isFromCache() {
    return false;
  }

  @Override
  public String getCharset() {
    return null;
  }

  @Override
  public boolean isCharsetProvided() {
    return false;
  }

  @Override
  public int getResponseCode() throws IOException {
    throw new IOException("No response code available for failed request: " + this.responseURL, this.exception);
  }

  @Override
  public String getResponseMessage() throws IOException {
    return "";
  }

  @Override
  public boolean isCacheable() {
    return false;
  }

  @Override
  public boolean isNewNavigationAction() {
    return ClientletResponseImpl.isNewNavigationAction(this.requestType);
  }

  @Override
  public <T extends Serializable> T getPersistentCachedObject(final ClassLoader classLoader, final Class<T> expectedClass) {
    return null;
  }

  @Override
  public void setNewPersistentCachedObject(final Serializable object) {
  }

  @Override
  public Object getTransientCachedObject() {
    return null;
  }

  @Override
  public void setNewTransientCachedObject(final Object object, final int approxSize) {
  }

  @Override
  public Date getDate() {
    return null;
  }

  @Override
  public RequestType getRequestType() {
    return this.requestType;
  }

  @Override
  public String toString() {
    return "ErrorClientletResponse[url=" + this.responseURL + ",method=" + this.lastRequestMethod
        + ",requestType=" + this.requestType + "]";
  }
}
