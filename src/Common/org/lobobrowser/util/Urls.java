/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The Lobo Project

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Contact info: lobochief@users.sourceforge.net
 */
/*
 * Created on Jun 12, 2005
 */
package org.lobobrowser.util;

import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

public class Urls {
  private static final Logger logger = Logger.getLogger(Urls.class.getName());
  public static final DateFormat PATTERN_RFC1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

  static {
    final DateFormat df = PATTERN_RFC1123;
    df.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  private Urls() {
    super();
  }

  /** Whether the URL refers to a resource in the local file system. */
  public static boolean isLocal(final java.net.URL url) {
    if (isLocalFile(url)) {
      return true;
    }
    final String protocol = url.getProtocol();
    if ("jar".equalsIgnoreCase(protocol)) {
      final String path = url.getPath();
      final int emIdx = path.lastIndexOf('!');
      final String subUrlString = emIdx == -1 ? path : path.substring(0, emIdx);
      try {
        final URL subUrl = new URL(subUrlString);
        return isLocal(subUrl);
      } catch (final java.net.MalformedURLException mfu) {
        return false;
      }
    } else {
      return false;
    }
  }

  /** Whether the URL is a file in the local file system. */
  public static boolean isLocalFile(final java.net.URL url) {
    final String scheme = url.getProtocol();
    return "file".equalsIgnoreCase(scheme) && !hasHost(url);
  }

  public static boolean hasHost(final java.net.URL url) {
    final String host = url.getHost();
    return (host != null) && !"".equals(host);
  }

  /**
   * Creates an absolute URL in a manner equivalent to major browsers.
   */
  public static @NonNull URL createURL(final URL baseUrl, final String relativeUrl) throws java.net.MalformedURLException {
    return new URL(baseUrl, relativeUrl);
  }

  /**
   * Returns the time when the document should be considered expired. The time
   * will be zero if the document always needs to be revalidated. It will be
   * <code>null</code> if no expiration time is specified.
   */
  public static Long getExpiration(final URLConnection connection, final long baseTime) {
    final String cacheControl = connection.getHeaderField("Cache-Control");
    if (cacheControl != null) {
      final StringTokenizer tok = new StringTokenizer(cacheControl, ",");
      while (tok.hasMoreTokens()) {
        final String token = tok.nextToken().trim().toLowerCase();
        if ("must-revalidate".equals(token)) {
          return new Long(0);
        } else if (token.startsWith("max-age")) {
          final int eqIdx = token.indexOf('=');
          if (eqIdx != -1) {
            final String value = token.substring(eqIdx + 1).trim();

            try {
              final long seconds = Long.parseLong(value);
              return new Long(baseTime + (seconds * 1000L));
            } catch (final NumberFormatException nfe) {
              logger.warning("getExpiration(): Bad Cache-Control max-age value: " + value);
              // ignore
            }
          }
        }
      }
    }
    final String expires = connection.getHeaderField("Expires");
    if (expires != null) {
      try {
        synchronized (PATTERN_RFC1123) {
          final Date expDate = PATTERN_RFC1123.parse(expires);
          return new Long(expDate.getTime());
        }
      } catch (final java.text.ParseException pe) {
        int seconds;
        try {
          seconds = Integer.parseInt(expires);
          return new Long(baseTime + (seconds * 1000));
        } catch (final NumberFormatException nfe) {
          logger.warning("getExpiration(): Bad Expires header value: " + expires);
        }
      }
    }

    // For issue #99
    // When there is no cache setting; assume a 60 second cache expiry time, for now.
    // return baseTime + (60 * 1000);
    // ^^ Update: Assume expiry time only if ETag header is present.
    //            We have not implemented the ETag header yet, but the presence of it is a good indicator that the response could be cached.
    final String etag = connection.getHeaderField("Etag");
    return etag == null ? 0 : baseTime + (60 * 1000);
  }

  public static List<NameValuePair> getHeaders(final URLConnection connection) {
    // Random access index recommended.
    final List<NameValuePair> headers = new ArrayList<>();
    for (int n = 0;; n++) {
      final String value = connection.getHeaderField(n);
      if (value == null) {
        break;
      }
      // Key may be null for n == 0.
      final String key = connection.getHeaderFieldKey(n);
      if (key != null) {
        headers.add(new NameValuePair(key, value));
      }
    }
    return headers;
  }

  public static String getCharset(final URLConnection connection) {
    final String contentType = connection.getContentType();
    if (contentType == null) {
      return getDefaultCharset(connection);
    }
    final StringTokenizer tok = new StringTokenizer(contentType, ";");
    if (tok.hasMoreTokens()) {
      tok.nextToken();
      while (tok.hasMoreTokens()) {
        final String assignment = tok.nextToken().trim();
        final int eqIdx = assignment.indexOf('=');
        if (eqIdx != -1) {
          final String varName = assignment.substring(0, eqIdx).trim();
          if ("charset".equalsIgnoreCase(varName)) {
            final String varValue = assignment.substring(eqIdx + 1);
            return Strings.unquote(varValue.trim());
          }
        }
      }
    }
    return getDefaultCharset(connection);
  }

  private static String getDefaultCharset(final URLConnection connection) {
    final URL url = connection.getURL();
    if (Urls.isLocalFile(url)) {
      final String charset = System.getProperty("file.encoding");
      return charset == null ? "ISO-8859-1" : charset;
    } else {
      return "ISO-8859-1";
    }
  }

  public static String getNoRefForm(final URL url) {
    final String host = url.getHost();
    final int port = url.getPort();
    final String portText = port == -1 ? "" : ":" + port;
    final String userInfo = url.getUserInfo();
    final String userInfoText = (userInfo == null) || (userInfo.length() == 0) ? "" : userInfo + "@";
    final String hostPort = (host == null) || (host.length() == 0) ? "" : "//" + userInfoText + host + portText;
    return url.getProtocol() + ":" + hostPort + url.getFile();
  }

  /**
   * Comparison that does not consider Ref.
   *
   * @param url1
   * @param url2
   */
  public static boolean sameNoRefURL(final URL url1, final URL url2) {
    return java.util.Objects.equals(url1.getHost(), url2.getHost()) && java.util.Objects.equals(url1.getProtocol(), url2.getProtocol())
        && (url1.getPort() == url2.getPort()) && java.util.Objects.equals(url1.getFile(), url2.getFile())
        && java.util.Objects.equals(url1.getUserInfo(), url2.getUserInfo());
  }

  /**
   * Returns the port of a URL always. When the port is not explicitly set, it
   * returns the default port
   */
  public static int getPort(final URL url) {
    final int setPort = url.getPort();
    return setPort == -1 ? url.getDefaultPort() : setPort;
  }

  /**
   * Converts the given URL into a valid URL by encoding illegal characters.
   * Right now it is implemented like in IE7: only spaces are replaced with
   * "%20". (Firefox 3 also encodes other non-ASCII and some ASCII characters).
   *
   * @param the
   *          URL to convert
   * @return the encoded URL
   */
  public static String encodeIllegalCharacters(final String url) {
    return url.replace(" ", "%20");
  }

  /**
   * Converts the given URL into a valid URL by removing control characters
   * (ASCII code < 32).
   *
   * @param the
   *          URL to convert
   * @return the encoded URL
   */
  public static String removeControlCharacters(final String url) {
    final StringBuilder sb = new StringBuilder(url.length());
    for (int i = 0; i < url.length(); i++) {
      final char c = url.charAt(i);
      if (c >= 32) {
        sb.append(c);
      }
    }
    return sb.toString();
  }

}
