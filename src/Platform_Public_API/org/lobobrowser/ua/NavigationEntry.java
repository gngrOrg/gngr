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

import java.net.URL;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.clientlet.ClientletResponse;

/**
 * Represents one item in the navigation history.
 */
public class NavigationEntry {
  // Note: Do not retain request context here.

  private final @NonNull URL url;
  private final String method;
  private final String title;
  private final String description;
  private final NavigatorFrame frame;

  public NavigationEntry(final NavigatorFrame frame, final @NonNull URL url, final String method, final String title, final String description) {
    super();
    this.frame = frame;
    this.url = url;
    this.method = method;
    this.title = title;
    this.description = description;
  }

  /**
   * Gets the uppercase request method that resulted in this navigation entry.
   */
  public String getMethod() {
    return method;
  }

  public String getTitle() {
    return title;
  }

  public @NonNull URL getUrl() {
    return url;
  }

  public NavigatorFrame getNavigatorFrame() {
    return frame;
  }

  public static NavigationEntry fromResponse(final NavigatorFrame frame, final ClientletResponse response, final String title,
      final String description) {
    return new NavigationEntry(frame, response.getResponseURL(), response.getLastRequestMethod(), title, description);
  }

  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return "NavigationEntry[url=" + this.url + ",method=" + this.method + ",title=" + title + "]";
  }
}
