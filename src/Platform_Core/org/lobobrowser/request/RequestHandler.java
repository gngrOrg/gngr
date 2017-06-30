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
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.net.ssl.HostnameVerifier;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.clientlet.ClientletException;
import org.lobobrowser.clientlet.ClientletRequest;
import org.lobobrowser.clientlet.ClientletResponse;
import org.lobobrowser.ua.ProgressType;
import org.lobobrowser.ua.RequestType;
import org.lobobrowser.ua.UserAgentContext;

public interface RequestHandler {
  public ClientletRequest getRequest();

  /**
   * Gets the actual request URL, which may be different from the URL of the
   * original request.
   */
  public @NonNull URL getLatestRequestURL();

  /**
   * Gets the actual request method, which may be different from the method of
   * the original request.
   */
  public String getLatestRequestMethod();

  public UserAgentContext getContext();

  /**
   * Gets a hostname verifier used when an HTTPS host does not match the
   * cerificate information.
   */
  public HostnameVerifier getHostnameVerifier();

  public void processResponse(ClientletResponse response, Consumer<Boolean> consumer) throws ClientletException, IOException;

  public boolean handleException(ClientletResponse response, Throwable exception, RequestType requestType) throws ClientletException;

  public void handleProgress(ProgressType progressType, @NonNull URL url, String method, int value, int max);

  public void cancel();

  public boolean isCancelled();

  default public RequestType getRequestType() {
    return getRequest().getRequestType();
  }

  default public Optional<Map<String, String>> getRequestedHeaders() {
    return Optional.empty();
  }
}
