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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.clientlet.CancelClientletException;
import org.lobobrowser.clientlet.ClientletException;
import org.lobobrowser.clientlet.ClientletRequest;
import org.lobobrowser.clientlet.ClientletResponse;
import org.lobobrowser.clientlet.Header;
import org.lobobrowser.main.ExtensionManager;
import org.lobobrowser.main.PlatformInit;
import org.lobobrowser.settings.BooleanSettings;
import org.lobobrowser.settings.CacheSettings;
import org.lobobrowser.settings.ConnectionSettings;
import org.lobobrowser.store.CacheManager;
import org.lobobrowser.ua.Parameter;
import org.lobobrowser.ua.ParameterInfo;
import org.lobobrowser.ua.ProgressType;
import org.lobobrowser.ua.RequestType;
import org.lobobrowser.ua.UserAgent;
import org.lobobrowser.ua.UserAgentContext;
import org.lobobrowser.ua.UserAgentContext.Request;
import org.lobobrowser.ua.UserAgentContext.RequestKind;
import org.lobobrowser.util.BoxedObject;
import org.lobobrowser.util.ID;
import org.lobobrowser.util.NameValuePair;
import org.lobobrowser.util.SimpleThreadPool;
import org.lobobrowser.util.SimpleThreadPoolTask;
import org.lobobrowser.util.Strings;
import org.lobobrowser.util.Urls;
import org.lobobrowser.util.io.Files;
import org.lobobrowser.util.io.IORoutines;

public final class RequestEngine {
  private static final int MAX_REDIRECT_COUNT = 30;
  private static final Logger logger = Logger.getLogger(RequestEngine.class.getName());
  private static final boolean loggerInfo = logger.isLoggable(Level.INFO);

  private final SimpleThreadPool threadPool;
  private final Collection<RequestInfo> processingRequests = new HashSet<>();
  private final CookieStore cookieStore = CookieStore.getInstance();
  private final CacheSettings cacheSettings;
  private final BooleanSettings booleanSettings;
  private final ConnectionSettings connectionSettings;

  private RequestEngine() {
    // Use few threads to avoid excessive parallelism. Note that
    // downloads are not handled by this thread pool.
    this.threadPool = new SimpleThreadPool("RequestEngineThreadPool", 3, 5, 60 * 1000);

    // Security: Private fields that require privileged access to get
    // initialized.
    this.cacheSettings = CacheSettings.getInstance();
    this.connectionSettings = ConnectionSettings.getInstance();
    this.booleanSettings = BooleanSettings.getInstance();
  }

  private static final RequestEngine instance = new RequestEngine();

  public static RequestEngine getInstance() {
    return instance;
  }

  public String getCookie(final java.net.URL url) {
    final Collection<Cookie> cookies = this.cookieStore.getCookies(url.getProtocol(), url.getHost(), url.getPath());
    final StringBuffer cookieText = new StringBuffer();
    cookies.forEach(cookie -> {
      cookieText.append(cookie.getName());
      cookieText.append('=');
      cookieText.append(cookie.getValue());
      cookieText.append(';');
    });
    // Note: Return blank if there are no cookies, not null.
    return cookieText.toString();
  }

  public void setCookie(final URL url, final String cookieSpec) {
    try {
      this.cookieStore.saveCookie(url.toURI(), cookieSpec);
    } catch (final URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public void cancelAllRequests() {
    this.threadPool.cancelAll();
  }

  public void cancelRequest(final RequestHandler rhToDelete) {
    this.threadPool.cancel(new RequestHandlerTask(rhToDelete));
    this.cancelRequestIfRunning(rhToDelete);
  }

  public void cancelRequestIfRunning(final RequestHandler rhToDelete) {
    rhToDelete.cancel();
    final List<RequestInfo> handlersToCancel = new ArrayList<>();
    synchronized (this.processingRequests) {
      final Iterator<RequestInfo> ri = this.processingRequests.iterator();
      while (ri.hasNext()) {
        final RequestInfo rinfo = ri.next();
        if (rinfo.getRequestHandler() == rhToDelete) {
          handlersToCancel.add(rinfo);
        }
      }
    }
    final Iterator<RequestInfo> ri2 = handlersToCancel.iterator();
    while (ri2.hasNext()) {
      final RequestInfo rinfo = ri2.next();
      rinfo.abort();
    }
  }

  public void scheduleRequest(final RequestHandler handler) {
    // Note: Important to create task with current access context if there's
    // a security manager.
    final SecurityManager sm = System.getSecurityManager();
    final AccessControlContext context = sm == null ? null : AccessController.getContext();
    this.threadPool.schedule(new RequestHandlerTask(handler, context));
  }

  private static final String NORMAL_FORM_ENCODING = "application/x-www-form-urlencoded";

  private void postData(final URLConnection connection, final ParameterInfo pinfo, final String altPostData) throws IOException {
    final BooleanSettings boolSettings = this.booleanSettings;
    final String encoding = pinfo != null ? pinfo.getEncoding() : NORMAL_FORM_ENCODING;
    if ((encoding == null) || NORMAL_FORM_ENCODING.equalsIgnoreCase(encoding)) {
      final ByteArrayOutputStream bufOut = new ByteArrayOutputStream();
      if (pinfo != null) {
        final Parameter[] parameters = pinfo.getParameters();
        boolean firstParam = true;
        for (final Parameter parameter : parameters) {
          final String name = parameter.getName();
          final String encName = URLEncoder.encode(name, "UTF-8");
          if (parameter.isText()) {
            if (firstParam) {
              firstParam = false;
            } else {
              bufOut.write((byte) '&');
            }
            final String valueStr = parameter.getTextValue();
            final String encValue = URLEncoder.encode(valueStr, "UTF-8");
            bufOut.write(encName.getBytes("UTF-8"));
            bufOut.write((byte) '=');
            bufOut.write(encValue.getBytes("UTF-8"));
          } else {
            logger.warning("postData(): Ignoring non-textual parameter " + name + " for POST with encoding " + encoding + ".");
          }
        }
      } else {
        // No pinfo provided - check alternative POST data
        if (altPostData != null) {
          bufOut.write(altPostData.getBytes("UTF-8"));
        }
      }
      // Do not add a line break to post content. Some servers
      // can be picky about that (namely, java.net).
      final byte[] postContent = bufOut.toByteArray();
      logInfo("postData(): Will post: " + new String(postContent));
      if (connection instanceof HttpURLConnection) {
        if (boolSettings.isHttpUseChunkedEncodingPOST()) {
          ((HttpURLConnection) connection).setChunkedStreamingMode(8192);
        } else {
          ((HttpURLConnection) connection).setFixedLengthStreamingMode(postContent.length);
        }
      }
      connection.setRequestProperty("Content-Type", NORMAL_FORM_ENCODING);
      // connection.setRequestProperty("Content-Length",
      // String.valueOf(postContent.length));
      final OutputStream postOut = connection.getOutputStream();
      postOut.write(postContent);
      postOut.flush();
    } else if ("multipart/form-data".equalsIgnoreCase(encoding)) {
      final long id = ID.generateLong();
      final String boundary = "----------------" + id;
      final boolean chunked = boolSettings.isHttpUseChunkedEncodingPOST();
      OutputStream mfstream;
      if (chunked) {
        mfstream = connection.getOutputStream();
      } else {
        mfstream = new ByteArrayOutputStream();
      }
      final MultipartFormDataWriter writer = new MultipartFormDataWriter(mfstream, boundary);
      try {
        if (pinfo != null) {
          final Parameter[] parameters = pinfo.getParameters();
          for (final Parameter parameter : parameters) {
            final String name = parameter.getName();
            if (parameter.isText()) {
              writer.writeText(name, parameter.getTextValue(), "UTF-8");
            } else if (parameter.isFile()) {
              final File file = parameter.getFileValue();
              try (
                final FileInputStream in = new FileInputStream(parameter.getFileValue())) {
                final BufferedInputStream bin = new BufferedInputStream(in, 8192);
                writer.writeFileData(name, file.getName(), Files.getContentType(file), bin);
              }
            } else {
              logger.warning("postData(): Skipping parameter " + name + " of unknown type for POST with encoding " + encoding + ".");
            }
          }
        }
      } finally {
        writer.send();
      }
      connection.addRequestProperty("Content-Type", encoding + "; boundary=" + boundary);
      if (chunked) {
        if (connection instanceof HttpURLConnection) {
          ((HttpURLConnection) connection).setChunkedStreamingMode(8192);
        }
      } else {
        final byte[] content = ((ByteArrayOutputStream) mfstream).toByteArray();
        if (connection instanceof HttpURLConnection) {
          ((HttpURLConnection) connection).setFixedLengthStreamingMode(content.length);
        }
        final OutputStream out = connection.getOutputStream();
        out.write(content);
      }
    } else {
      throw new IllegalArgumentException("Unknown encoding: " + encoding);
    }
  }

  private static String completeGetUrl(final String baseURL, final ParameterInfo pinfo, final String ref) throws Exception {
    String newNoRefURL;
    final Parameter[] parameters = pinfo.getParameters();
    if ((parameters != null) && (parameters.length > 0)) {
      final StringBuffer sb = new StringBuffer(baseURL);
      final int qmIdx = baseURL.indexOf('?');
      char separator = qmIdx == -1 ? '?' : '&';
      for (final Parameter parameter : parameters) {
        if (parameter.isText()) {
          sb.append(separator);
          sb.append(parameter.getName());
          sb.append('=');
          final String paramText = parameter.getTextValue();
          sb.append(URLEncoder.encode(paramText, "UTF-8"));
          separator = '&';
        } else {
          logger.warning("completeGetUrl(): Skipping non-textual parameter " + parameter.getName() + " in GET request.");
        }
      }
      newNoRefURL = sb.toString();
    } else {
      newNoRefURL = baseURL;
    }
    if ((ref != null) && (ref.length() != 0)) {
      return newNoRefURL + "#" + ref;
    } else {
      return newNoRefURL;
    }
  }

  private static void addRequestProperties(final URLConnection connection, final ClientletRequest request, final CacheInfo cacheInfo,
      final String requestMethod,
      final URL lastRequestURL, final RequestHandler rhandler) throws ProtocolException {
    final UserAgent userAgent = request.getUserAgent();
    connection.addRequestProperty("User-Agent", userAgent.toString());
    connection.addRequestProperty("Accept-Encoding", "gzip, deflate");

    // The following two headers are for GH #174
    connection.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    connection.addRequestProperty("Accept-Language", "en-US,en;q=0.5");

    // TODO: Harshad: Commenting out X-Java-Version. Check if required.
    // connection.addRequestProperty("X-Java-Version", userAgent.getJavaVersion());

    // TODO: Commenting out X-Session-ID. Needs to be privately generated
    // or available with the right permissions only. Extensions should not
    // have access to the private field. This is not doable if extensions
    // should have a permission to open up access to any member.
    //
    // connection.addRequestProperty("X-Session-ID",
    // userAgent.getSessionID(connection.getURL()));
    final String referrer = request.getReferrer();
    if (referrer != null) {
      if (rhandler.getContext().isRequestPermitted(new Request(request.getRequestURL(), RequestKind.Referrer))) {
        connection.addRequestProperty("Referer", referrer);
      }
    }
    if (cacheInfo != null) {
      final String date = cacheInfo.getDateAsText();
      if (date != null) {
        connection.addRequestProperty("If-Modified-Since", date);
      }
    }
    if (connection instanceof HttpURLConnection) {
      final HttpURLConnection hconnection = (HttpURLConnection) connection;
      hconnection.setRequestMethod(requestMethod);
    }
    final Header[] headers = request.getExtraHeaders();
    if (headers != null) {
      for (final Header header : headers) {
        final String headerName = header.getName();
        if (headerName.startsWith("X-")) {
          connection.addRequestProperty(headerName, header.getValue());
        } else {
          logger.warning("run(): Ignoring request header: " + headerName);
        }
      }
    }
  }

  private static CacheInfo getCacheInfo(final RequestHandler rhandler, final URL url, final boolean isGet) throws Exception {
    final RequestType requestType = rhandler.getRequestType();

    if (isGet && isOKToRetrieveFromCache(requestType)) {
      return AccessController.doPrivileged(new PrivilegedAction<CacheInfo>() {
        // Reason: Caller in context may not have privilege to access
        // the local file system, yet it's necessary to be able to load
        // a cache file.
        public CacheInfo run() {
          byte[] persistentContent = null;
          final CacheManager cm = CacheManager.getInstance();
          final MemoryCacheEntry entry = (MemoryCacheEntry) cm.getTransient(url);
          if (entry == null) {
            if (!"file".equalsIgnoreCase(url.getProtocol()) || !Strings.isBlank(url.getHost())) {
              try {
                persistentContent = CacheManager.getPersistent(url, false);
              } catch (final java.io.IOException ioe) {
                logger.log(Level.WARNING, "getCacheInfo(): Unable to load cache file.", ioe);
              }
            }
          }
          if ((persistentContent == null) && (entry == null)) {
            return null;
          }
          final CacheInfo cinfo = new CacheInfo(entry, persistentContent, url);
          return cinfo;
        }
      });
    } else {
      return null;
    }
  }

  private static void cache(final RequestHandler rhandler, final java.net.URL url, final URLConnection connection, final byte[] content,
      final java.io.Serializable altPersistentObject, final Object altObject, final int approxAltObjectSize) {
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
      // Reason: Caller might not have permission to access the
      // file system. Yet, caching should be allowed.
      public Object run() {
        try {
          final long currentTime = System.currentTimeMillis();
          logInfo("cache(): url=" + url + ",content.length=" + content.length + ",currentTime=" + currentTime);
          final Long expiration = Urls.getExpiration(connection, currentTime);
          if ((expiration != null) && (expiration > 0)) {
            storeCacheEntry(url, connection, content, altPersistentObject, altObject, approxAltObjectSize, currentTime, expiration);
          }
        } catch (final Exception err) {
          logger.log(Level.WARNING, "cache()", err);
        }
        return null;
      }

    });
  }

  private static void storeCacheEntry(final java.net.URL url, final URLConnection connection, final byte[] content,
      final java.io.Serializable altPersistentObject, final Object altObject, final int approxAltObjectSize, final long currentTime,
      final Long expiration) throws UnsupportedEncodingException, IOException {
    int actualApproxObjectSize = 0;
    if (altObject != null) {
      if (approxAltObjectSize < content.length) {
        actualApproxObjectSize = content.length;
      } else {
        actualApproxObjectSize = approxAltObjectSize;
      }
    }
    final List<NameValuePair> headers = Urls.getHeaders(connection);
    final MemoryCacheEntry memEntry = new MemoryCacheEntry(content, headers, expiration, altObject, actualApproxObjectSize);
    final int approxMemEntrySize = content.length + (altObject == null ? 0 : approxAltObjectSize);
    final CacheManager cm = CacheManager.getInstance();
    cm.putTransient(url, memEntry, approxMemEntrySize);
    try (
      final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      boolean hadDate = false;
      boolean hadContentLength = false;
      for (int counter = 0; true; counter++) {
        final String headerKey = connection.getHeaderFieldKey(counter);
        if (headerKey != null) {
          if (!hadDate && "date".equalsIgnoreCase(headerKey)) {
            hadDate = true;
          }
          if (!hadContentLength && "content-length".equalsIgnoreCase(headerKey)) {
            hadContentLength = true;
          }
        }
        final String headerValue = connection.getHeaderField(counter);
        if (headerValue == null) {
          break;
        }
        if (CacheInfo.HEADER_REQUEST_TIME.equalsIgnoreCase(headerKey)) {
          continue;
        }

        // Fix #142: When stored in cache, decoding of input stream has been already done.  Hence, don't store the content-encoding header
        // TODO: Evaluate the trade-offs of storing the original response with compression. Will save disk-space but increase read-back time.
        if ("content-encoding".equalsIgnoreCase(headerKey)) {
          continue;
        }

        final String headerPrefix = (headerKey == null) || (headerKey.length() == 0) ? "" : headerKey + ": ";
        final byte[] headerBytes = (headerPrefix + headerValue + "\r\n").getBytes("ISO-8859-1");
        out.write(headerBytes);
      }
      if (!hadDate) {
        final String currentDate = Urls.PATTERN_RFC1123.format(new java.util.Date());
        final byte[] headerBytes = ("Date: " + currentDate + "\r\n").getBytes("ISO-8859-1");
        out.write(headerBytes);
      }
      if (!hadContentLength) {
        final byte[] headerBytes = ("Content-Length: " + content.length + "\r\n").getBytes("ISO-8859-1");
        out.write(headerBytes);
      }
      final byte[] rtHeaderBytes = (CacheInfo.HEADER_REQUEST_TIME + ": " + currentTime + "\r\n").getBytes("ISO-8859-1");
      out.write(rtHeaderBytes);
      out.write(IORoutines.LINE_BREAK_BYTES);
      out.write(content);
      try {
        CacheManager.putPersistent(url, out.toByteArray(), false);
      } catch (final IOException err) {
        logger.log(Level.WARNING, "cache(): Unable to cache response content.", err);
      }
    }
    if (altPersistentObject != null) {
      try {
        final ByteArrayOutputStream fileOut = new ByteArrayOutputStream();
        // No need to buffer - Java API already does.
        final ObjectOutputStream objOut = new ObjectOutputStream(fileOut);
        objOut.writeObject(altPersistentObject);
        objOut.flush();
        final byte[] byteArray = fileOut.toByteArray();
        if (byteArray.length == 0) {
          logger
              .log(Level.WARNING, "cache(): Serialized content has zero bytes for persistent object " + altPersistentObject + ".");
        }
        CacheManager.putPersistent(url, byteArray, true);
      } catch (final Exception err) {
        logger.log(Level.WARNING, "cache(): Unable to write persistent cached object.", err);
      }
    }
  }

  private static boolean mayBeCached(final HttpURLConnection connection) {
    final String cacheControl = connection.getHeaderField("Cache-Control");
    if (cacheControl != null) {
      final StringTokenizer tok = new StringTokenizer(cacheControl, ",");
      while (tok.hasMoreTokens()) {
        final String token = tok.nextToken().trim();
        if ("no-cache".equalsIgnoreCase(token)) {
          return false;
        }
      }
    }
    return true;
  }

  private static void printRequestHeaders(final URLConnection connection) {
    final Map<String, List<String>> headers = connection.getRequestProperties();
    final StringBuffer buffer = new StringBuffer();
    for (final Map.Entry<String, List<String>> entry : headers.entrySet()) {
      buffer.append(entry.getKey() + ": " + entry.getValue());
      buffer.append(System.getProperty("line.separator"));
    }
    logger.info("printRequestHeaders(): Request headers for URI=[" + connection.getURL() + "]\r\n" + buffer.toString());
  }

  public void inlineRequest(final RequestHandler rhandler) {
    // Security checked by low-level APIs in this case.
    this.processHandler(rhandler, 0, false);
  }

  public byte[] loadBytes(final String urlOrPath, final UserAgentContext uaContext) throws Exception {
    return this.loadBytes(DomainValidation.guessURL(urlOrPath), uaContext);
  }

  private byte[] loadBytes(final @NonNull URL url, final UserAgentContext uaContext) throws Exception {
    final BoxedObject boxed = new BoxedObject();
    this.inlineRequest(new SimpleRequestHandler(url, RequestType.ELEMENT, uaContext) {
      @Override
      public boolean handleException(final ClientletResponse response, final Throwable exception, final RequestType requestType)
          throws ClientletException {
        if (exception instanceof ClientletException) {
          throw (ClientletException) exception;
        } else {
          throw new ClientletException(exception);
        }
      }

      public void processResponse(final ClientletResponse response, Consumer<Boolean> consumer) throws ClientletException, IOException {
        final byte[] bytes = org.lobobrowser.util.io.IORoutines.load(response.getInputStream(), 4096);
        boxed.setObject(bytes);
        consumer.accept(true);
      }
    });
    return (byte[]) boxed.getObject();
  }

  /*
  public AsyncResult<byte[]> loadBytesAsync(final String urlOrPath, final UserAgentContext uaContext) throws java.net.MalformedURLException {
    return this.loadBytesAsync(Urls.guessURL(urlOrPath), uaContext);
  }

  public AsyncResult<byte[]> loadBytesAsync(final URL url, final UserAgentContext uaContext) {
    final AsyncResultImpl<byte[]> asyncResult = new AsyncResultImpl<>();
    this.scheduleRequest(new SimpleRequestHandler(url, RequestType.ELEMENT, uaContext) {
      @Override
      public boolean handleException(final ClientletResponse response, final Throwable exception, final RequestType requestType) throws ClientletException {
        asyncResult.setException(exception);
        return true;
      }

      public void processResponse(final ClientletResponse response) throws ClientletException, IOException {
        final byte[] bytes = org.lobobrowser.util.io.IORoutines.load(response.getInputStream(), 4096);
        asyncResult.setResult(bytes);
      }
    });
    return asyncResult;
  } */

  /**
   * Whether possibly cached request should always be revalidated, i.e. any
   * expiration information is ignored.
   */
  private static boolean shouldRevalidateAlways(final URL connectionUrl, final RequestType requestType) {
    return requestType == RequestType.ADDRESS_BAR;
  }

  /**
   * Whether the request type should always be obtained from cache if it is
   * there.
   */
  private static boolean doesNotExpire(final RequestType requestType) {
    return requestType == RequestType.HISTORY;
  }

  private static ExtensionManager getSafeExtensionManager() {
    return AccessController.doPrivileged(new PrivilegedAction<ExtensionManager>() {
      public ExtensionManager run() {
        return ExtensionManager.getInstance();
      }
    });
  }

  private URLConnection getURLConnection(final URL connectionUrl, final ClientletRequest request, final String protocol,
      final String method,
      final RequestHandler rhandler, final CacheInfo cacheInfo) throws IOException {
    URLConnection connection;
    if (cacheInfo != null) {
      final RequestType requestType = rhandler.getRequestType();
      if (doesNotExpire(requestType)) {
        if (loggerInfo) {
          if (cacheInfo.hasTransientEntry()) {
            logger.info("getURLConnection(): FROM-RAM: " + connectionUrl + ".");
          } else {
            logger.info("getURLConnection(): FROM-FILE: " + connectionUrl + ".");
          }
        }
        return cacheInfo.getURLConnection();
      } else if (!shouldRevalidateAlways(connectionUrl, requestType)) {
        Long expires = cacheInfo.getExpires();
        if (expires == null) {
          final int defaultOffset = this.cacheSettings.getDefaultCacheExpirationOffset();
          expires = cacheInfo.getExpiresGivenOffset(defaultOffset);
          if (loggerInfo) {
            final java.util.Date expiresDate = expires == null ? null : new Date(expires);
            logger.info("getURLConnection(): Used default offset for " + connectionUrl + ": expires=" + expiresDate);
          }
        }
        if (expires != null) {
          if (expires.longValue() > System.currentTimeMillis()) {
            if (loggerInfo) {
              final long secondsToExpiration = (expires.longValue() - System.currentTimeMillis()) / 1000;
              if (cacheInfo.hasTransientEntry()) {
                logger.info("getURLConnection(): FROM-RAM: " + connectionUrl + ". Expires in " + secondsToExpiration + " seconds.");
              } else {
                logger.info("getURLConnection(): FROM-FILE: " + connectionUrl + ". Expires in " + secondsToExpiration + " seconds.");
              }
            }
            return cacheInfo.getURLConnection();
          } else {
            if (loggerInfo) {
              logger.info("getURLConnection(): EXPIRED: " + connectionUrl + ". Expired on " + new Date(expires) + ".");
            }
          }
        }
        // If the document has expired, the cache may still
        // be used, but only after validation.
      }
    }
    final boolean isPost = "POST".equalsIgnoreCase(method);
    final String host = connectionUrl.getHost();
    final boolean isResURL = "res".equalsIgnoreCase(protocol);
    if (isResURL || (host == null) || (host.length() == 0)) {
      connection = connectionUrl.openConnection();
    } else {
      final Proxy proxy = this.connectionSettings.getProxy(host);
      if (proxy == Proxy.NO_PROXY) {
        // Workaround for JRE 1.5.0.
        connection = connectionUrl.openConnection();
      } else {
        connection = connectionUrl.openConnection(proxy);
      }
    }
    if (connection instanceof HttpsURLConnection) {
      ((HttpsURLConnection) connection).setHostnameVerifier(rhandler.getHostnameVerifier());
    }
    if (isPost) {
      connection.setDoOutput(true);
    }
    connection.setUseCaches(false);
    if (connection instanceof HttpURLConnection) {
      final HttpURLConnection hconnection = (HttpURLConnection) connection;
      hconnection.setConnectTimeout(60000);
      hconnection.setReadTimeout(90000);
    }
    addRequestProperties(connection, request, cacheInfo, method, connectionUrl, rhandler);

    addRequestedHeadersToRequest(connection, rhandler);

    // Moved add cookies here since connection is initiated in this method for POST requests.
    // And we can't add headers after the connection is made.
    addCookiesToRequest(connection, rhandler);
    // dumpRequestInfo(connection);

    // Allow extensions to modify the connection object.
    // Doing it after addRequestProperties() to allow such
    // functionality as altering the Accept header.
    connection = getSafeExtensionManager().dispatchPreConnection(connection);
    // Print request headers
    if (logger.isLoggable(Level.FINE)) {
      printRequestHeaders(connection);
    }
    // POST data if we need to.
    if (isPost) {
      final ParameterInfo pinfo = rhandler instanceof RedirectRequestHandler ? null : request.getParameterInfo();
      final String altPostData = rhandler instanceof RedirectRequestHandler ? null : request.getAltPostData();
      if ((pinfo == null) && (altPostData == null)) {
        logger.info("POST has no parameter information");
      } else {
        this.postData(connection, pinfo, altPostData);
      }
    }
    return connection;
  }

  private static boolean isOKToRetrieveFromCache(final RequestType requestType) {
    return (requestType != RequestType.SOFT_RELOAD) && (requestType !=
        RequestType.HARD_RELOAD) && (requestType != RequestType.DOWNLOAD);

  }

  private static void logInfo(final String msg) {
    if (loggerInfo) {
      logger.info(msg);
    }
  }

  private static void logInfo(final String msg, final Throwable cce) {
    if (loggerInfo) {
      logger.log(Level.INFO, msg, cce);
    }
  }

  @SuppressWarnings("unused")
  private static void dumpRequestInfo(final URLConnection connection) {
    if (PlatformInit.getInstance().debugOn) {
      System.out.println("URL: " + connection.getURL());
      System.out.println("  Request Headers: ");
      connection.getRequestProperties().forEach((key, value) -> System.out.println("    " + key + " : " + value));
    }
  }

// It is referenced in this class so the method stays in case it's needed in the future
//  private static void dumpResponseInfo(final URLConnection connection) {
//    if (PlatformInit.getInstance().debugOn) {
//      System.out.println("URL: " + connection.getURL());
//      System.out.println("  Response Headers: ");
//      connection.getHeaderFields().forEach((key, value) -> System.out.println("    " + key + " : " + value));
//    }
//  }

  private void processHandler(final RequestHandler rhandler, final int recursionLevel, final boolean trackRequestInfo) {
    // Method must be private.
    final URL baseURL = rhandler.getLatestRequestURL();
    RequestInfo rinfo = null;
    ClientletResponseImpl response = null;
    final String method = rhandler.getLatestRequestMethod().toUpperCase();
    try {
      final ClientletRequest request = rhandler.getRequest();
      // TODO: Hack: instanceof below
      final ParameterInfo pinfo = rhandler instanceof RedirectRequestHandler ? null : request.getParameterInfo();
      final boolean isGet = "GET".equals(method);
      final URL url = makeCompleteURL(baseURL, pinfo, isGet);
      final String protocol = url.getProtocol();
      final URL connectionUrl = makeConnectionURL(url, protocol);
      final CacheInfo cacheInfo = getCacheInfo(rhandler, connectionUrl, isGet);
      try {
        URLConnection connection = this.getURLConnection(connectionUrl, request, protocol, method, rhandler, cacheInfo);

        // This causes exceptions sometimes (when the connection is already open)
        // dumpRequestInfo(connection);

        rinfo = new RequestInfo(connection, rhandler);
        // InputStream responseIn = null;
        if (trackRequestInfo) {
          synchronized (this.processingRequests) {
            this.processingRequests.add(rinfo);
          }
        }
        try {
          if (rhandler.isCancelled()) {
            throw new CancelClientletException("cancelled");
          }
          rhandler.handleProgress(ProgressType.CONNECTING, url, method, 0, -1);
          // Handle response
          boolean isContentCached = (cacheInfo != null) && cacheInfo.isCacheConnection(connection);

          boolean isCacheable = false;
          if ((connection instanceof HttpURLConnection) && !isContentCached) {
            final HttpURLConnection hconnection = (HttpURLConnection) connection;
            hconnection.setInstanceFollowRedirects(false);
            final int responseCode = hconnection.getResponseCode();
            logInfo("run(): ResponseCode=" + responseCode + " for url=" + connectionUrl);
            // dumpResponseInfo(connection);
            handleCookies(connectionUrl, hconnection, rhandler);

            if (responseCode == HttpURLConnection.HTTP_OK) {
              logInfo("run(): FROM-HTTP: " + connectionUrl);
              if (mayBeCached(hconnection)) {
                isCacheable = true;
              } else {
                logInfo("run(): NOT CACHEABLE: " + connectionUrl);
                if (cacheInfo != null) {
                  cacheInfo.delete();
                }
              }
              // responseIn = connection.getInputStream();
              // rinfo.setConnection(connection, responseIn);
              rinfo.setConnection(connection);
            } else if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
              if (cacheInfo == null) {
                throw new IllegalStateException("Cache info missing but it is necessary to process response code " + responseCode + ".");
              }
              logInfo("run(): FROM-VALIDATION: " + connectionUrl);
              // Disconnect the HTTP connection.
              hconnection.disconnect();
              isContentCached = true;
              // Even though the response is actually from the cache,
              // we need to cache it again, if only to update the
              // request time (used to calculate default expiration).
              // TODO: Can this special case be optimized?
              isCacheable = true;
              connection = cacheInfo.getURLConnection();
              // responseIn = connection.getInputStream();
              // rinfo.setConnection(connection, responseIn);
              rinfo.setConnection(connection);
            } else if ((responseCode == HttpURLConnection.HTTP_MOVED_PERM) || (responseCode == HttpURLConnection.HTTP_MOVED_TEMP)
                || (responseCode == HttpURLConnection.HTTP_SEE_OTHER)) {
              logInfo("run(): REDIRECTING: ResponseCode=" + responseCode + " for url=" + url);
              final RequestHandler newHandler = new RedirectRequestHandler(rhandler, hconnection);
              Thread.yield();
              if (recursionLevel > MAX_REDIRECT_COUNT) {
                throw new ClientletException("Exceeded redirect recursion limit.");
              }
              this.processHandler(newHandler, recursionLevel + 1, trackRequestInfo);
              return;
            }
          } else {
            // Force it to throw exception if file does not exist
            // responseIn = connection.getInputStream();
            // rinfo.setConnection(connection, responseIn);
            rinfo.setConnection(connection);
          }
          if (rinfo.isAborted()) {
            throw new CancelClientletException("Stopped");
          }

          // Give a chance to extensions to post-process the connection.
          final URLConnection newConnection = getSafeExtensionManager().dispatchPostConnection(connection);
          if (newConnection != connection) {
            // responseIn = newConnection.getInputStream();
            connection = newConnection;
          }

          // Create clientlet response.
          response = new ClientletResponseImpl(rhandler, connection, url, isContentCached, cacheInfo, isCacheable,
              rhandler.getRequestType());
          final ClientletResponseImpl resp = response;
          final URLConnection con = connection;
          final boolean isCache = isCacheable;
          rhandler.processResponse(response, (valid) -> {
            if(valid)
              try {
                updateCache(rhandler, resp, connectionUrl, cacheInfo , con, isCache);
              } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
          });
        } finally {
          if (trackRequestInfo) {
            synchronized (this.processingRequests) {
              this.processingRequests.remove(rinfo);
            }
          }
          /*
          if (responseIn != null) {
            try {
              responseIn.close();
            } catch (final java.io.IOException ioe) {
              // ignore
            }
          }*/

          // TODO: Possible optimization. By not disconnecting, we might be able to get a faster response for next request
          /*
          if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection) connection).disconnect();
          }*/
        }
      } finally {
        if (cacheInfo != null) {
          // This is necessary so that the file stream doesn't stay open potentially.
          cacheInfo.dispose();
        }
      }
    } catch (final CancelClientletException cce) {
      logInfo("run(): Clientlet cancelled: " + baseURL, cce);
    } catch (final Exception exception) {
      if ((rinfo != null) && rinfo.isAborted()) {
        logInfo("run(): Exception ignored because request aborted.", exception);
      } else {
        try {
          if (!rhandler.handleException(response, exception, rhandler.getRequestType())) {
            logger.log(Level.WARNING, "Was unable to handle exception.", exception);
          }
        } catch (final Exception err) {
          System.out.println("Exception while handling exception:" + exception);
          exception.printStackTrace();
          logger.log(Level.WARNING, "Exception handler threw an exception.", err);
        }
      }
    } finally {
      rhandler.handleProgress(ProgressType.DONE, baseURL, method, 0, 0);
    }
  }

  final private CookieHandler cookieHandler = new CookieHandlerImpl();

  private static void addRequestedHeadersToRequest(final URLConnection connection, final RequestHandler rhandler) {
    final Optional<Map<String, String>> requestedHeadersOpt = rhandler.getRequestedHeaders();
    if (requestedHeadersOpt.isPresent()) {
      final Map<String, String> requestedHeaders = requestedHeadersOpt.get();
      requestedHeaders.forEach((key, value) -> connection.addRequestProperty(key, value));
    }
  }

  private void addCookiesToRequest(final URLConnection connection, final RequestHandler rhandler) {
    try {
      final String protocol = connection.getURL().getProtocol();
      if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol)) {
        final URL url = connection.getURL();
        final URI uri = url.toURI();
        // TODO: optimization #1: list is not required if we directly call our CookieHandler implementation
        // TODO: optimization #2: even if we use the CookieHandler interface, we can avoid the joining of List entries, since our impl always returns a single element list
        final Map<String, List<String>> cookieHeaders = cookieHandler.get(uri, null);
        if (!cookieHeaders.isEmpty()) {
          if (rhandler.getContext().isRequestPermitted(new Request(url, RequestKind.Cookie))) {
            addCookieHeaderToRequest(connection, cookieHeaders, "Cookie");
          }
        }
      }
    } catch (IOException | URISyntaxException e) {
      logger.warning("Couldn't add cookies for : " + connection.getURL());
      logger.warning("  .. reason: " + e.getMessage());
      // TODO: These exceptions should be either not captured, or failure should be
      // propagated to caller by return value.
    }
  }

  private static void addCookieHeaderToRequest(final URLConnection connection, final Map<String, List<String>> cookieHeaders,
      final String cookieKey) {
    final List<String> cookieValue = cookieHeaders.get(cookieKey);
    if (cookieValue != null) {
      final String cookieValueStr = cookieValue.stream().collect(Collectors.joining(";"));
      connection.addRequestProperty(cookieKey, cookieValueStr);
    }
  }

  private void handleCookies(final URL url, final HttpURLConnection hconnection, final RequestHandler rhandler) throws URISyntaxException,
      IOException {
    final Map<String, List<String>> headerFields = hconnection.getHeaderFields();
    final boolean cookieSetterExists = headerFields.keySet().stream().anyMatch(key ->
        "Set-Cookie".equalsIgnoreCase(key) // || "Set-Cookie2".equalsIgnoreCase(key)
        );
    if (cookieSetterExists) {
      if (rhandler.getContext().isRequestPermitted(new Request(url, RequestKind.Cookie))) {
        cookieHandler.put(url.toURI(), headerFields);
      }
    }
  }

  private static void updateCache(final RequestHandler rhandler, final ClientletResponseImpl response, final URL connectionUrl,
      final CacheInfo cacheInfo,
      final URLConnection connection, final boolean isCacheable) throws IOException {
    if (isCacheable) {
      // Make sure stream reaches EOF so we don't get null stored content.
      response.ensureReachedEOF();

      final byte[] content = response.getStoredContent();
      if (content != null) {
        final Serializable persObject = response.getNewPersistentCachedObject();
        final Object altObject = response.getNewTransientCachedObject();
        final int altObjectSize = response.getNewTransientObjectSize();
        cache(rhandler, connectionUrl, connection, content, persObject, altObject, altObjectSize);
      } else {
        logger.warning("processHandler(): Cacheable response not available: " + connectionUrl);
      }
    } else if ((cacheInfo != null) && !cacheInfo.hasTransientEntry()) {
      // Content that came from cache cannot be cached again, but a RAM entry was missing.
      final byte[] persContent = cacheInfo.getPersistentContent();
      final Object altObject = response.getNewTransientCachedObject();
      final int altObjectSize = response.getNewTransientObjectSize();
      final MemoryCacheEntry newMemEntry = new MemoryCacheEntry(persContent, cacheInfo.getExpires(), cacheInfo.getRequestTime(), altObject,
          altObjectSize);
      final int actualApproxObjectSize = altObject == null ? 0 : Math.max(altObjectSize, persContent.length);
      // Reason: Privileges needed to access CacheManager.
      AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
        CacheManager.getInstance().putTransient(connectionUrl, newMemEntry, actualApproxObjectSize + persContent.length);
        return null;
      });
    }
  }

  private static URL makeConnectionURL(final URL url, final String protocol) throws MalformedURLException {
    if ((url.getQuery() != null) && "file".equalsIgnoreCase(protocol)) {
      // Remove query (replace file with path) if "file" protocol.
      final String ref = url.getRef();
      final String refText = (ref == null) || (ref.length() == 0) ? "" : "#" + ref;
      return new URL(protocol, url.getHost(), url.getPort(), url.getPath() + refText);
    } else {
      return url;
    }
  }

  private static URL makeCompleteURL(final URL baseURL, final ParameterInfo pinfo, final boolean isGet) throws Exception,
      MalformedURLException {
    if (isGet && (pinfo != null)) {
      final String ref = baseURL.getRef();
      final String noRefForm = Urls.getNoRefForm(baseURL);
      final String newURLText = completeGetUrl(noRefForm, pinfo, ref);
      return new URL(newURLText);
    } else {
      return baseURL;
    }
  }

  private static class RequestInfo {
    private final RequestHandler requestHandler;

    private volatile boolean isAborted = false;
    // private volatile InputStream inputStream;
    private volatile URLConnection connection;

    RequestInfo(final URLConnection connection, final RequestHandler rhandler) {
      this.connection = connection;
      this.requestHandler = rhandler;
    }

    boolean isAborted() {
      return this.isAborted;
    }

    void abort() {
      try {
        this.isAborted = true;
        if (this.connection instanceof HttpURLConnection) {
          ((HttpURLConnection) this.connection).disconnect();
        }
        // final InputStream in = this.inputStream;
        // if (in != null) {
        // in.close();
        // }
      } catch (final Exception err) {
        logger.log(Level.SEVERE, "abort()", err);
      }
    }

    RequestHandler getRequestHandler() {
      return this.requestHandler;
    }

    // void setConnection(final URLConnection connection, final InputStream in) {
    void setConnection(final URLConnection connection) {
      this.connection = connection;
      // this.inputStream = in;
    }
  }

  private class RequestHandlerTask implements SimpleThreadPoolTask {
    private final RequestHandler handler;

    private final AccessControlContext accessContext;

    private RequestHandlerTask(final RequestHandler handler, final AccessControlContext accessContext) {
      this.handler = handler;
      this.accessContext = accessContext;
    }

    private RequestHandlerTask(final RequestHandler handler) {
      this.handler = handler;
      this.accessContext = null;
    }

    public void run() {
      final SecurityManager sm = System.getSecurityManager();
      if ((sm != null) && (this.accessContext != null)) {
        final PrivilegedAction<Object> action = () -> {
          processHandler(handler, 0, true);
          return null;
        };
        // This way we ensure scheduled requests have the same
        // protection as inline requests, particularly in relation
        // to file and host name checks.
        AccessController.doPrivileged(action, this.accessContext);
      } else {
        processHandler(this.handler, 0, true);
      }
    }

    public void cancel() {
      cancelRequestIfRunning(this.handler);
    }

    @Override
    public int hashCode() {
      return this.handler.hashCode();
    }

    @Override
    public boolean equals(final Object other) {
      return (other instanceof RequestHandlerTask) && ((RequestHandlerTask) other).handler.equals(this.handler);
    }

    @Override
    public String toString() {
      return "RequestHandlerTask[host=" + this.handler.getLatestRequestURL().getHost() + "]";
    }
  }
}
