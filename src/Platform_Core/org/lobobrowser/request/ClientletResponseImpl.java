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
 * Created on Mar 13, 2005
 */
package org.lobobrowser.request;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.clientlet.ClientletResponse;
import org.lobobrowser.ua.ProgressType;
import org.lobobrowser.ua.RequestType;
import org.lobobrowser.util.GenericEventListener;
import org.lobobrowser.util.InputProgressEvent;
import org.lobobrowser.util.MonitoredInputStream;
import org.lobobrowser.util.Strings;
import org.lobobrowser.util.Urls;
import org.lobobrowser.util.io.BufferExceededException;
import org.lobobrowser.util.io.IORoutines;
import org.lobobrowser.util.io.RecordedInputStream;

/**
 * @author J. H. S.
 */
public class ClientletResponseImpl implements ClientletResponse {
  private static final Logger logger = Logger.getLogger(ClientletResponseImpl.class.getName());
  private static final int MAX_CACHE_BUFFER_SIZE = 10 * 1024 * 1024;

  private final URLConnection connection;
  private final RequestHandler requestHandler;
  private final boolean isCacheable;
  private final CacheInfo cacheInfo;
  private final boolean fromCache;
  private final RequestType requestType;
  // Security note: This URL must be final.
  private final @NonNull URL responseURL;

  private InputStream inputStream;

  public ClientletResponseImpl(final RequestHandler rhandler, final URLConnection connection, final @NonNull URL responseURL,
      final boolean fromCache, final CacheInfo cacheInfo,
      final boolean isCacheable, final RequestType requestType) {
    this.connection = connection;
    this.responseURL = responseURL;
    this.requestHandler = rhandler;
    this.isCacheable = isCacheable;
    this.cacheInfo = cacheInfo;
    this.fromCache = fromCache;
    this.requestType = requestType;
  }

  public ClientletResponseImpl(final RequestHandler rhandler, final URL url, final boolean fromCache, final CacheInfo cacheInfo,
      final boolean isCacheable,
      final String requestMethod, final RequestType requestType) throws IOException {
    this.connection = url.openConnection();
    this.responseURL = url;
    this.requestHandler = rhandler;
    this.isCacheable = isCacheable;
    this.cacheInfo = cacheInfo;
    this.fromCache = fromCache;
    this.requestType = requestType;
  }

  public boolean isNewNavigationAction() {
    final RequestType rt = this.requestType;
    return (rt != RequestType.HISTORY) && (rt != RequestType.SOFT_RELOAD) && (rt != RequestType.HARD_RELOAD);
  }

  public boolean matches(final String mimeType, final String[] fileExtensions) {
    final String responseMimeType = this.getMimeType();
    if ((responseMimeType == null) || "application/octet-stream".equalsIgnoreCase(responseMimeType)
        || "content/unknown".equalsIgnoreCase(responseMimeType)) {
      final String path = this.responseURL.getPath();
      if (path == null) {
        return false;
      }
      final String pathTL = path.toLowerCase();
      for (final String fileExtension : fileExtensions) {
        String fileExtensionTL = fileExtension.toLowerCase();
        if (!fileExtensionTL.startsWith(".")) {
          fileExtensionTL = "." + fileExtensionTL;
        }
        if (pathTL.endsWith(fileExtensionTL)) {
          return true;
        }
      }
      return false;
    } else {
      return responseMimeType.equalsIgnoreCase(mimeType);
    }
  }

  public String getLastRequestMethod() {
    return this.requestHandler.getLatestRequestMethod();
  }

  public void handleProgress(final ProgressType progressType, final @NonNull URL url, final String method, final int value, final int max) {
    this.requestHandler.handleProgress(progressType, url, method, value, max);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.clientlet.ClientletResponse#isFromCache()
   */
  public boolean isFromCache() {
    return this.fromCache;
  }

  public boolean isCacheable() {
    return this.isCacheable;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.dom.ClientletResponse#getResponseURL()
   */
  public @NonNull URL getResponseURL() {
    // Assumes connection doesn't use internal redirection.
    return this.responseURL;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.dom.ClientletResponse#getHeader(java.lang.String)
   */
  public String getHeader(final String name) {
    return this.connection.getHeaderField(name);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.dom.ClientletResponse#getHeaders(java.lang.String, char)
   */
  public String[] getHeaders(final String name) {
    final Map<String, List<String>> headers = this.connection.getHeaderFields();
    final List<String> valuesList = headers.get(name);
    return valuesList == null ? null : valuesList.toArray(new String[0]);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.dom.ClientletResponse#getHeaderNames()
   */
  public Iterator<String> getHeaderNames() {
    final Map<String, List<String>> headers = this.connection.getHeaderFields();
    return headers.keySet().iterator();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.dom.ClientletResponse#getInputStream()
   */
  public InputStream getInputStream() throws IOException {
    if (this.inputStream == null) {
      final URLConnection connection = this.connection;
      InputStream in;
      if (connection instanceof HttpURLConnection) {
        in = IORoutines.getDecodedErrorStream(((HttpURLConnection) connection));
        if (in == null) {
          in = IORoutines.getDecodedStream(connection);
        }
      } else {
        in = connection.getInputStream();
      }
      final int contentLength = connection.getContentLength();
      final int bufferSize = contentLength <= 0 ? 4096 : Math.min(contentLength, 8192);
      final URL responseURL = this.getResponseURL();
      // if(logger.isLoggable(Level.INFO))logger.info("getInputStream(): Connection stream is "
      // + in);
      InputStream bis;
      if (this.requestHandler != null) {
        final MonitoredInputStream mis = new MonitoredInputStream(in);
        mis.evtProgress.addListener(new GenericEventListener() {
          public void processEvent(final EventObject event) {
            final InputProgressEvent pe = (InputProgressEvent) event;
            requestHandler.handleProgress(org.lobobrowser.ua.ProgressType.CONTENT_LOADING, responseURL, getLastRequestMethod(),
                pe.getProgress(), contentLength);
          }
        });
        // TODO Buffer size too big if contentLength small
        bis = new BufferedInputStream(mis, bufferSize);
      } else {
        bis = new BufferedInputStream(in, bufferSize);
      }
      if (this.isCacheable) {
        this.inputStream = new RecordedInputStream(bis, MAX_CACHE_BUFFER_SIZE);
      } else {
        this.inputStream = bis;
      }
    }
    return this.inputStream;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.dom.ClientletResponse#getContentType()
   */
  public String getContentType() {
    return this.connection.getContentType();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.clientlet.ClientletResponse#getMimeType()
   */
  public String getMimeType() {
    final String contentType = this.getContentType();
    if (contentType == null) {
      return null;
    }
    final int scIdx = contentType.indexOf(';');
    return scIdx == -1 ? contentType.trim() : contentType.substring(0, scIdx).trim();
  }

  public int getContentLength() {
    return this.connection.getContentLength();
  }

  public void ensureReachedEOF() throws IOException {
    // Don't get cached inputStream - could be null here.
    final InputStream in = this.getInputStream();
    if (in instanceof RecordedInputStream) {
      final RecordedInputStream rin = (RecordedInputStream) in;
      if (!rin.hasReachedEOF()) {
        rin.consumeToEOF();
      }
    }
  }

  public byte[] getStoredContent() {
    // Should call ensureReachedEOF() which will also ensure
    // inputStream is not null.
    final InputStream in = this.inputStream;
    if (in instanceof RecordedInputStream) {
      final RecordedInputStream rin = (RecordedInputStream) in;
      if (rin.hasReachedEOF()) {
        try {
          return rin.getBytesRead();
        } catch (final BufferExceededException bee) {
          logger.warning("getStoredContent(): Recorded stream buffer size exceeded.");
          return null;
        }
      }
    }
    return null;
  }

  private String getDefaultCharset() {
    final URL url = this.getResponseURL();
    if (Urls.isLocalFile(url)) {
      final String charset = System.getProperty("file.encoding");
      return charset == null ? "ISO-8859-1" : charset;
    } else {
      return "ISO-8859-1";
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.clientlet.ClientletResponse#getCharset()
   */
  public String getCharset() {
    final String contentType = this.getContentType();
    if (contentType == null) {
      return this.getDefaultCharset();
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
    return this.getDefaultCharset();
  }

  public boolean isCharsetProvided() {
    final String contentType = this.getContentType();
    if (contentType == null) {
      return false;
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
            return true;
          }
        }
      }
    }
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.clientlet.ClientletResponse#getResponseCode()
   */
  public int getResponseCode() throws IOException {
    if (this.connection instanceof HttpURLConnection) {
      return ((HttpURLConnection) this.connection).getResponseCode();
    } else {
      return 200;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.clientlet.ClientletResponse#getResponseMessage()
   */
  public String getResponseMessage() throws IOException {
    if (this.connection instanceof HttpURLConnection) {
      return ((HttpURLConnection) this.connection).getResponseMessage();
    } else {
      return "";
    }
  }

  @Override
  public String toString() {
    return "ClientletResponseImpl[url=" + this.responseURL + ",method=" + this.getLastRequestMethod() + ",mimeType=" + this.getMimeType()
        + ",fromCache=" + this.isFromCache() + ",requestType=" + this.requestType + "]";
  }

  public Object getPersistentCachedObject(final ClassLoader classLoader) {
    final CacheInfo cacheInfo = this.cacheInfo;
    return cacheInfo == null ? null : cacheInfo.getPersistentObject(classLoader);
  }

  public Object getTransientCachedObject() {
    final CacheInfo cacheInfo = this.cacheInfo;
    return cacheInfo == null ? null : cacheInfo.getTransientObject();
  }

  /* Commented because nothing is using it. And it returns null for a primitive return type!
  public int getTransientCachedObjectSize() {
    final CacheInfo cacheInfo = this.cacheInfo;
    return cacheInfo == null ? null : cacheInfo.getTransientObjectSize();
  }*/

  private Serializable newPeristentCachedObject;
  private Object newTransientCachedObject;
  private int newTransientObjectSize;

  public void setNewPersistentCachedObject(final Serializable object) {
    this.newPeristentCachedObject = object;
  }

  public void setNewTransientCachedObject(final Object object, final int approxSize) {
    this.newTransientCachedObject = object;
    this.newTransientObjectSize = approxSize;
  }

  public Serializable getNewPersistentCachedObject() {
    return newPeristentCachedObject;
  }

  public Object getNewTransientCachedObject() {
    return newTransientCachedObject;
  }

  public int getNewTransientObjectSize() {
    return newTransientObjectSize;
  }

  public java.util.Date getDate() {
    final String dateText = this.connection.getHeaderField("Date");
    if (dateText == null) {
      return null;
    }
    try {
      return Urls.PATTERN_RFC1123.parse(dateText);
    } catch (final java.text.ParseException pe) {
      logger.warning("getDate(): Bad date '" + dateText + "' from " + this.getResponseURL() + ".");
      return null;
    }
  }

  public RequestType getRequestType() {
    return this.requestType;
  }
}
