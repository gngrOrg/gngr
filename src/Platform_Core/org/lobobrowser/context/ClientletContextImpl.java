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
package org.lobobrowser.context;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.clientlet.ClientletContext;
import org.lobobrowser.clientlet.ClientletRequest;
import org.lobobrowser.clientlet.ClientletResponse;
import org.lobobrowser.clientlet.ComponentContent;
import org.lobobrowser.clientlet.ContentBuffer;
import org.lobobrowser.io.ManagedStore;
import org.lobobrowser.request.DomainValidation;
import org.lobobrowser.request.UserAgentImpl;
import org.lobobrowser.store.StorageManager;
import org.lobobrowser.ua.NavigatorFrame;
import org.lobobrowser.ua.NavigatorProgressEvent;
import org.lobobrowser.ua.ProgressType;
import org.lobobrowser.ua.UserAgent;

public class ClientletContextImpl implements ClientletContext {
  private final NavigatorFrame frame;
  private final ClientletRequest request;
  private final ClientletResponse response;

  public ClientletContextImpl(final NavigatorFrame frame, final ClientletRequest request, final ClientletResponse response) {
    this.frame = frame;
    this.request = request;
    this.response = response;
  }

  public ContentBuffer createContentBuffer(final String contentType, final byte[] content) {
    return new VolatileContentImpl(contentType, content);
  }

  public ContentBuffer createContentBuffer(final String contentType, final String content, final String encoding)
      throws UnsupportedEncodingException {
    final byte[] bytes = content.getBytes(encoding);
    return new VolatileContentImpl(contentType, bytes);
  }

  public NavigatorFrame getNavigatorFrame() {
    return this.frame;
  }

  private Map<String, Object> items = null;

  public Object getItem(final String name) {
    synchronized (this) {
      final Map<String, Object> items = this.items;
      if (items == null) {
        return null;
      }
      return items.get(name);
    }
  }

  public ManagedStore getManagedStore() throws IOException {
    final ClientletResponse response = this.response;
    if (response == null) {
      throw new SecurityException("There is no client response");
    }
    final String hostName = response.getResponseURL().getHost();
    return StorageManager.getInstance().getRestrictedStore(hostName, true);
  }

  public ManagedStore getManagedStore(final String hostName) throws IOException {
    return StorageManager.getInstance().getRestrictedStore(hostName, true);
  }

  public ClientletRequest getRequest() {
    return this.request;
  }

  public ClientletResponse getResponse() {
    return this.response;
  }

  public UserAgent getUserAgent() {
    return UserAgentImpl.getInstance();
  }

  public void setItem(final String name, final Object value) {
    synchronized (this) {
      Map<String, Object> items = this.items;
      if (items == null) {
        items = new HashMap<>(1);
        this.items = items;
      }
      items.put(name, value);
    }
  }

  private volatile ComponentContent resultingContent;

  public ComponentContent getResultingContent() {
    return this.resultingContent;
  }

  public void navigate(final String url) throws java.net.MalformedURLException {
    final java.net.URL responseURL = this.response.getResponseURL();
    final java.net.URL newURL = DomainValidation.guessURL(responseURL, url);
    this.frame.navigate(newURL);
  }

  /*
  public final void setResultingContent(final Component content, final URL url) {
    // Must call other overload, which may be overridden.

    this.setResultingContent(new SimpleComponentContent(content, url.toExternalForm(), null));
  }
  */

  public void setResultingContent(final ComponentContent content) {
    this.resultingContent = content;
  }

  private volatile Properties windowProperties;

  public void overrideWindowProperties(final Properties properties) {
    this.windowProperties = properties;
  }

  public Properties getOverriddingWindowProperties() {
    return this.windowProperties;
  }

  public boolean isResultingContentSet() {
    return this.resultingContent != null;
  }

  public void setProgressEvent(final ProgressType progressType, final int value, final int max) {
    this.setProgressEvent(progressType, value, max, this.getResponse().getResponseURL());
  }

  public NavigatorProgressEvent getProgressEvent() {
    return this.frame.getProgressEvent();
  }

  public void setProgressEvent(final ProgressType progressType, final int value, final int max, final @NonNull URL url) {
    final ClientletResponse response = this.getResponse();
    final NavigatorFrame frame = this.getNavigatorFrame();
    final String method = response.getLastRequestMethod();
    frame.setProgressEvent(new NavigatorProgressEvent(this, frame, progressType, url, method, value, max));
  }

  public void setProgressEvent(final NavigatorProgressEvent event) {
    this.getNavigatorFrame().setProgressEvent(event);
  }

  public void alert(final String message) {
    this.getNavigatorFrame().alert(message);
  }

  public NavigatorFrame createNavigatorFrame() {
    return this.getNavigatorFrame().createFrame();
  }
}
