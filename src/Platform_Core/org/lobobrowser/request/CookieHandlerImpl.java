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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.CookieHandler;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CookieHandlerImpl extends CookieHandler {
  private static final Logger logger = Logger.getLogger(CookieHandlerImpl.class.getName());
  private final CookieStore cookieStore = CookieStore.getInstance();

  private static void printHeaders(final Map<String, List<String>> headers) {
    final StringWriter swriter = new StringWriter();
    final PrintWriter writer = new PrintWriter(swriter);
    writer.println();
    for (final Map.Entry<String, List<String>> entry : headers.entrySet()) {
      for (final String value : entry.getValue()) {
        writer.println("Header: " + entry.getKey() + "=" + value);
      }
    }
    writer.println();
    writer.flush();
    logger.info(swriter.toString());
  }

  @Override
  public Map<String, List<String>> get(final URI uri, final Map<String, List<String>> requestHeaders) throws IOException {
    final Map<String, List<String>> resultHeaders = new java.util.HashMap<>(2);
    final java.util.Collection<Cookie> cookies = this.cookieStore.getCookies(uri.getScheme(), uri.getHost(), uri.getPath());
    if (cookies != null) {
      StringBuffer cookieHeaderValue = null;
      for (final Cookie cookie : cookies) {
        // We should not decode values. Servers expect to receive what they set the values to.
        final String cookieName = cookie.getName();
        final String cookieValue = cookie.getValue();
        final String assignment = cookieName + "=" + cookieValue;
        // if(logger.isLoggable(Level.INFO)) {
        // logger.info("get(): found cookie: [" + assignment + "]; uri=" + uri);
        // }
        if (cookieHeaderValue == null) {
          cookieHeaderValue = new StringBuffer();
        } else {
          cookieHeaderValue.append("; ");
        }
        cookieHeaderValue.append(assignment);
      }
      if (cookieHeaderValue != null) {
        resultHeaders.put("Cookie", java.util.Collections.singletonList(cookieHeaderValue.toString()));
      }
    }
    if (logger.isLoggable(Level.FINE)) {
      logger.info("get(): ---- Cookie headers for uri=[" + uri + "].");
      printHeaders(resultHeaders);
    }
    return resultHeaders;
  }

  @Override
  public void put(final URI uri, final Map<String, List<String>> responseHeaders) throws IOException {
    if (logger.isLoggable(Level.FINE)) {
      logger.info("put(): ---- Response headers for uri=[" + uri + "].");
      printHeaders(responseHeaders);
    }
    storeCookies(uri, responseHeaders);
  }

  public void storeCookies(final URI uri, final Map<String, List<String>> responseHeaders) {
    for (final Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
      final String key = entry.getKey();
      if (key != null) {
        for (final String value : entry.getValue()) {
          if (value != null) {
            if ("Set-Cookie".equalsIgnoreCase(key)) {
              cookieStore.saveCookie(uri, value);
              // } else if ("Set-Cookie2".equalsIgnoreCase(key)) {
              // cookieStore.saveCookie(uri, value);
            }
          }
        }
      }
    }
  }
}
