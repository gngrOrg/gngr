/*
Copyright 1994-2006 The Lobo Project. All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer. Redistributions in binary form must
reproduce the above copyright notice, this list of conditions and the following
disclaimer in the documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE LOBO PROJECT ``AS IS'' AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
EVENT SHALL THE FREEBSD PROJECT OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.lobobrowser.ua;

import org.lobobrowser.clientlet.ClientletResponse;

/**
 * An event containing response information.
 */
public class NavigatorResponseEvent extends NavigatorEvent {
  private static final long serialVersionUID = 8569384791191603705L;
  private final ClientletResponse response;
  private final RequestType requestType;

  public NavigatorResponseEvent(final Object source, final NavigatorEventType eventType, final NavigatorFrame clientletFrame,
      final ClientletResponse response,
      final RequestType requestType) {
    super(source, eventType, clientletFrame);
    this.response = response;
    this.requestType = requestType;
  }

  public ClientletResponse getResponse() {
    return response;
  }

  public java.net.URL getUrl() {
    return this.response == null ? null : this.response.getResponseURL();
  }

  public String getMethod() {
    return this.response == null ? null : this.response.getLastRequestMethod();
  }

  public RequestType getRequestType() {
    return requestType;
  }

  @Override
  public String toString() {
    return "NavigatorWindowEvent[type=" + this.getEventType() + ",url=" + this.getUrl() + "]";
  }
}
