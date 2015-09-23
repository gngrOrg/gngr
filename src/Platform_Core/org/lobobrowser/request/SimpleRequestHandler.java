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
/*
 * Created on Apr 16, 2005
 */
package org.lobobrowser.request;

import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.clientlet.ClientletException;
import org.lobobrowser.clientlet.ClientletRequest;
import org.lobobrowser.clientlet.ClientletResponse;
import org.lobobrowser.ua.ProgressType;
import org.lobobrowser.ua.RequestType;
import org.lobobrowser.ua.UserAgentContext;

/**
 * Request handler used by request().
 *
 * @author J. H. S.
 */
public abstract class SimpleRequestHandler implements RequestHandler {
  private static final Logger logger = Logger.getLogger(SimpleRequestHandler.class.getName());
  private final ClientletRequest request;
  private final UserAgentContext uaContext;

  @Override
  public UserAgentContext getContext() {
    return uaContext;
  }

  public SimpleRequestHandler(final @NonNull URL url, final RequestType requestType, final UserAgentContext uaContext) {
    this.request = new ClientletRequestImpl(url, requestType);
    this.uaContext = uaContext;
  }

  public SimpleRequestHandler(final @NonNull URL url, final String method, final String altPostData, final RequestType requestType,
      final UserAgentContext uaContext) {
    this.request = new ClientletRequestImpl(url, method, altPostData, requestType);
    this.uaContext = uaContext;
  }

  public boolean isNewNavigationEntry() {
    return false;
  }

  public String getCacheFileSuffix() {
    return null;
  }

  public HostnameVerifier getHostnameVerifier() {
    return new LocalHostnameVerifier();
  }

  public String getLatestRequestMethod() {
    return this.request.getMethod();
  }

  public @NonNull URL getLatestRequestURL() {
    return this.request.getRequestURL();
  }

  public ClientletRequest getRequest() {
    return this.request;
  }

  public void handleProgress(final ProgressType progressType, final @NonNull URL url, final String method, final int value, final int max) {
    // nop
  }

  public boolean handleException(final ClientletResponse response, final Throwable exception, final RequestType requestType)
      throws ClientletException {
    logger.log(Level.WARNING, "handleException(): Error processing response=[" + response + "]", exception);
    return true;
  }

  private volatile boolean cancelled;

  public void cancel() {
    this.cancelled = true;
  }

  public boolean isCancelled() {
    return this.cancelled;
  }

  private class LocalHostnameVerifier implements HostnameVerifier {
    /*
     * (non-Javadoc)
     *
     * @see javax.net.ssl.HostnameVerifier#verify(java.lang.String,
     * javax.net.ssl.SSLSession)
     */
    public boolean verify(final String host, final SSLSession session) {
      if (OkHostnameVerifier.INSTANCE.verify(host, session)) {
        return true;
      } else {
        final VerifiedHostsStore vhs = VerifiedHostsStore.getInstance();
        if (vhs.contains(host)) {
          return true;
        }
        // Does not ask user.
        return false;
      }
    }
  }

  public Optional<Map<String, String>> getRequestedHeaders() {
    return Optional.empty();
  }
}