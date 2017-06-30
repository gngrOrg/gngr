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
 * Created on Feb 4, 2006
 */
package org.lobobrowser.context;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.clientlet.ClientletAccess;
import org.lobobrowser.clientlet.ClientletContext;
import org.lobobrowser.clientlet.ClientletException;
import org.lobobrowser.clientlet.ClientletResponse;
import org.lobobrowser.request.RequestEngine;
import org.lobobrowser.request.RequestHandler;
import org.lobobrowser.request.SimpleRequestHandler;
import org.lobobrowser.ua.ImageResponse;
import org.lobobrowser.ua.ImageResponse.State;
import org.lobobrowser.ua.NavigatorProgressEvent;
import org.lobobrowser.ua.NetworkRequest;
import org.lobobrowser.ua.NetworkRequestEvent;
import org.lobobrowser.ua.NetworkRequestListener;
import org.lobobrowser.ua.ProgressType;
import org.lobobrowser.ua.RequestType;
import org.lobobrowser.ua.UserAgentContext;
import org.lobobrowser.ua.UserAgentContext.Request;
import org.lobobrowser.util.EventDispatch;
import org.lobobrowser.util.GenericEventListener;
import org.lobobrowser.util.Threads;
import org.lobobrowser.util.Urls;
import org.w3c.dom.Document;

public class NetworkRequestImpl implements NetworkRequest {
  // TODO: Class not thread safe?
  private static final Logger logger = Logger.getLogger(NetworkRequestImpl.class.getName());
  private final EventDispatch READY_STATE_CHANGE = new EventDispatch();
  private volatile int readyState = NetworkRequest.STATE_UNINITIALIZED;
  private volatile LocalResponse localResponse;
  final private UserAgentContext uaContext;

  public NetworkRequestImpl(final UserAgentContext uaContext) {
    this.uaContext = uaContext;
  }

  public Optional<URL> getURL() {
    return Optional.of(requestURL);
  }

  public int getReadyState() {
    return this.readyState;
  }

  public String getResponseText() {
    final LocalResponse lr = this.localResponse;
    return lr == null ? null : lr.getResponseText();
  }

  public Document getResponseXML() {
    final LocalResponse lr = this.localResponse;
    return lr == null ? null : lr.getResponseXML();
  }

  public @NonNull ImageResponse getResponseImage() {
    final LocalResponse lr = this.localResponse;
    if (lr == null) {
      return new ImageResponse();
    } else {
      return lr.getResponseImage();
    }
  }

  // public java.util.jar.JarFile getResponseJarFile() throws java.io.IOException {
  // LocalResponse lr = this.localResponse;
  // return lr == null ? null : lr.getResponseJarFile();
  // }

  public byte[] getResponseBytes() {
    final LocalResponse lr = this.localResponse;
    return lr == null ? null : lr.getResponseBytes();
  }

  public int getStatus() {
    try {
      final LocalResponse lr = this.localResponse;
      return lr == null ? NetworkRequest.STATE_UNINITIALIZED : lr.getStatus();
    } catch (final java.io.IOException ioe) {
      return 0;
    }
  }

  public String getStatusText() {
    try {
      final LocalResponse lr = this.localResponse;
      return lr == null ? null : lr.getStatusText();
    } catch (final java.io.IOException ioe) {
      return null;
    }
  }

  private volatile RequestHandler currentRequestHandler;

  public void abort() {
    this.readyState = NetworkRequest.STATE_ABORTED;
    this.READY_STATE_CHANGE.fireEvent(new NetworkRequestEvent(this, this.readyState));

    final RequestHandler rhToDelete = this.currentRequestHandler;
    if (rhToDelete != null) {
      RequestEngine.getInstance().cancelRequest(rhToDelete);
    }
  }

  public String getAllResponseHeaders(final List<String> excludedHeadersLowerCase) {
    final LocalResponse lr = this.localResponse;
    return lr == null ? null : lr.getAllResponseHeaders(excludedHeadersLowerCase);
  }

  public String getResponseHeader(final String headerName) {
    final LocalResponse lr = this.localResponse;
    return lr == null ? null : lr.getResponseHeader(headerName);
  }

  public void open(final String method, final String url) throws IOException {
    this.open(method, url, true, null);
  }

  public void open(final String method, final @NonNull URL url) {
    this.open(method, url, true, null, null, null);
  }

  public void open(final String method, final @NonNull URL url, final boolean asyncFlag) {
    this.open(method, url, asyncFlag, null, null, null);
  }

  public void open(final String method, final String url, final boolean asyncFlag, final String integrity) throws IOException {
    final URL urlObj = Urls.createURL(null, url);
    this.open(method, urlObj, asyncFlag, null, null, integrity);
  }

  public void open(final String method, final @NonNull URL url, final boolean asyncFlag, final String userName) {
    this.open(method, url, asyncFlag, userName, null, null);
  }

  private boolean isAsynchronous = false;
  private String requestMethod;
  private URL requestURL;
  private String integrity;

  // private String requestUserName;
  // private String requestPassword;

  public void open(final String method, final @NonNull URL url, final boolean asyncFlag, final String userName, final String password, final String integrity) {
    this.isAsynchronous = asyncFlag;
    this.requestMethod = method;
    this.requestURL = url;
    this.integrity = integrity;
    // this.requestUserName = userName;
    // this.requestPassword = password;
    this.changeReadyState(NetworkRequest.STATE_LOADING);
  }

  public void send(final String content, final Request requestType) throws IOException {
    final URL requestURLLocal = this.requestURL;
    if (requestURLLocal != null && uaContext.isRequestPermitted(requestType)) {
      try {
        final Map<String, String> requestedHeadersCopy = new HashMap<>(requestedHeaders);
        final RequestHandler rhandler = new LocalRequestHandler(requestURLLocal, this.requestMethod, content, uaContext,
            requestedHeadersCopy);
        this.currentRequestHandler = rhandler;
        try {
          // TODO: Username and password support
          if (this.isAsynchronous) {
            RequestEngine.getInstance().scheduleRequest(rhandler);
          } else {
            RequestEngine.getInstance().inlineRequest(rhandler);
          }
        } finally {
          this.currentRequestHandler = null;
        }
      } catch (final Exception err) {
        logger.log(Level.SEVERE, "open()", err);
      }
    } else {
      abort();
    }
  }

  public void addNetworkRequestListener(final NetworkRequestListener listener) {
    this.READY_STATE_CHANGE.addListener(new GenericEventListener() {
      public void processEvent(final EventObject event) {
        listener.readyStateChanged((NetworkRequestEvent) event);
      }
    });
  }

  public boolean isAsnyc() {
    return isAsynchronous;
  }

  private void changeReadyState(final int newState) {
    this.readyState = newState;
    this.READY_STATE_CHANGE.fireEvent(new NetworkRequestEvent(this, newState));
  }

  private void setResponse(final ClientletResponse response, Consumer<Boolean> consumer) {

    final Runnable runnable = () -> {
      if (response.isFromCache()) {
        final Object cachedResponse = response.getTransientCachedObject();
        if (cachedResponse instanceof CacheableResponse) {
          // It can be of a different type.
          final CacheableResponse cr = (CacheableResponse) cachedResponse;
          this.changeReadyState(NetworkRequest.STATE_LOADING);
          this.localResponse = cr.newLocalResponse(response);
          this.changeReadyState(NetworkRequest.STATE_LOADED);
          this.changeReadyState(NetworkRequest.STATE_INTERACTIVE);
          final boolean valid = integrityCheck(cr.getResponseBytes());
          if (valid==false) {
            this.localResponse = null;
          }
          consumer.accept(valid);
          this.changeReadyState(NetworkRequest.STATE_COMPLETE);
          return;
        }
      }
      try {
        this.changeReadyState(NetworkRequest.STATE_LOADING);
        final LocalResponse newResponse = new LocalResponse(response);
        this.localResponse = newResponse;
        this.changeReadyState(NetworkRequest.STATE_LOADED);
        final int cl = response.getContentLength();
        final InputStream in = response.getInputStream();
        final int bufferSize = cl == -1 ? 8192 : Math.min(cl, 8192);
        final byte[] buffer = new byte[bufferSize];
        int numRead;
        int readSoFar = 0;
        boolean firstTime = true;
        final ClientletContext threadContext = ClientletAccess.getCurrentClientletContext();
        NavigatorProgressEvent prevProgress = null;
        if (threadContext != null) {
          prevProgress = threadContext.getProgressEvent();
        }
        try {
          long lastProgress = 0;
          while ((numRead = in.read(buffer)) != -1) {
            if (numRead == 0) {
              if (logger.isLoggable(Level.INFO)) {
                logger.info("setResponse(): Read zero bytes from " + response.getResponseURL());
              }
              break;
            }
            readSoFar += numRead;
            if (threadContext != null) {
              final long currentTime = System.currentTimeMillis();
              if ((currentTime - lastProgress) > 500) {
                lastProgress = currentTime;
                threadContext.setProgressEvent(ProgressType.CONTENT_LOADING, readSoFar, cl, response.getResponseURL());
              }
            }
            newResponse.writeBytes(buffer, 0, numRead);


            if (firstTime) {
              firstTime = false;
              this.changeReadyState(NetworkRequest.STATE_INTERACTIVE);
            }
          }
        } finally {
          if (threadContext != null) {
            threadContext.setProgressEvent(prevProgress);
          }
        }

        // here goes integrity
        final boolean valid = integrityCheck(newResponse.getBuffer().toByteArray());

        if (valid == false) {
          this.localResponse = null;
        }
        consumer.accept(valid);

        //TODO: CORS support

        newResponse.setComplete(valid);

        // The following should return non-null if the response is complete.
        final CacheableResponse cacheable = newResponse.getCacheableResponse();
        if (cacheable != null) {
          response.setNewTransientCachedObject(cacheable, cacheable.getEstimatedSize());
        }
        this.changeReadyState(NetworkRequest.STATE_COMPLETE);
      } catch (final IOException ioe) {
        logger.log(Level.WARNING, "setResponse()", ioe);
        this.localResponse = null;
        this.changeReadyState(NetworkRequest.STATE_COMPLETE);
      }
    };
    if (isAsynchronous) {
      // TODO: Use the JS queue to schedule this
      runnable.run();
    } else {
      runnable.run();
    }
  }

  private boolean integrityCheck(byte[] response) {
    return AlgorithmDigest.validate(response, integrity);
  }

  private class LocalRequestHandler extends SimpleRequestHandler {
    private final String method;
    private final Map<String, String> requestedHeadersCopy;

    public LocalRequestHandler(final @NonNull URL url, final String method, final String altPostData, final UserAgentContext uaContext,
        final Map<String, String> requestedHeaders) {
      super(url, method, altPostData, RequestType.ELEMENT, uaContext);
      this.method = method;
      this.requestedHeadersCopy = requestedHeaders;
    }

    @Override
    public String getLatestRequestMethod() {
      return this.method;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.sourceforge.xamj.http.BaseRequestHandler#handleException(java.net
     * .URL, java.lang.Exception)
     */
    @Override
    public boolean handleException(final ClientletResponse response, final Throwable exception, final RequestType requestType)
        throws ClientletException {
      logger.log(Level.WARNING, "handleException(): url=" + this.getLatestRequestURL() + ",response=[" + response + "]", exception);
      NetworkRequestImpl.this.abort();
      return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.sourceforge.xamj.http.BaseRequestHandler#processResponse(org.xamjwg
     * .clientlet.ClientletResponse)
     */
    public void processResponse(final ClientletResponse response, Consumer<Boolean> consumer) throws ClientletException, IOException {
      NetworkRequestImpl.this.setResponse(response, consumer);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sourceforge.xamj.http.RequestHandler#handleProgress(int,
     * java.net.URL, int, int)
     */
    // public void handleProgress(final org.lobobrowser.ua.ProgressType progressType, final URL url, final int value, final int max) {
    // }

    @Override
    public Optional<Map<String, String>> getRequestedHeaders() {
      return Optional.of(requestedHeadersCopy);
    }

  }

  private static class CacheableResponse {
    private WeakReference<Image> imageRef;
    private java.io.ByteArrayOutputStream buffer;
    private Document document;
    private String textContent;
    private boolean complete;

    public int getEstimatedSize() {
      final ByteArrayOutputStream out = this.buffer;
      final int factor = 3;
      // Note that when this is called, no one has
      // necessarily called getResponseText().
      return ((out == null ? 0 : out.size()) * factor) + 512;
    }

    public LocalResponse newLocalResponse(final ClientletResponse response) {
      return new LocalResponse(response, this);
    }

    public @NonNull ImageResponse getResponseImage() {
      // A hard reference to the image is not a good idea here.
      // Images will retain their observers, and it's also
      // hard to estimate their actual size.
      final WeakReference<Image> imageRef = this.imageRef;
      Image img = imageRef == null ? null : imageRef.get();
      if (this.complete) {
        if (img == null) {
          final byte[] bytes = this.getResponseBytes();
            img = Toolkit.getDefaultToolkit().createImage(bytes);
            Toolkit.getDefaultToolkit().prepareImage(img, -1, -1, null);
            int checkedFlags = Toolkit.getDefaultToolkit().checkImage(img, -1, -1, null);
            while (!isImgDone(checkedFlags)) {
              checkedFlags = Toolkit.getDefaultToolkit().checkImage(img, -1, -1, null);
              Threads.sleep(33);
            }
            if ((checkedFlags & ImageObserver.ERROR) != 0) {
              return new ImageResponse(State.error, null);
            } else {
              this.imageRef = new WeakReference<>(img);
              return new ImageResponse(State.loaded, img);
            }
        } else {
          return new ImageResponse(State.loaded, img);
        }
      } else {
        return new ImageResponse();
      }
    }

    private static boolean isImgDone(final int checkedFlags) {
      return ((checkedFlags & ImageObserver.ERROR) != 0) || ((checkedFlags & (ImageObserver.WIDTH | ImageObserver.HEIGHT)) != 0);
    }

    public String getResponseText(final String charset) {
      String responseText = this.textContent;
      if (responseText != null) {
        return responseText;
      }
      final byte[] bytes = this.getResponseBytes();
      if (bytes == null) {
        return null;
      }
      try {
        responseText = new String(bytes, charset);
      } catch (final UnsupportedEncodingException uee) {
        logger.log(Level.WARNING, "getResponseText()", uee);
        try {
          responseText = new String(bytes, "ISO-8859-1");
        } catch (final UnsupportedEncodingException uee2) {
          // ignore
        }
      }
      this.textContent = responseText;
      return responseText;
    }

    /**
     * @return Returns the responseBytes.
     */
    public byte[] getResponseBytes() {
      final ByteArrayOutputStream out = this.buffer;
      return out == null ? null : out.toByteArray();
    }

    public Document getResponseXML() {
      Document doc = this.document;
      // TODO: GH #138
      // Although the following works, it has two issues
      //   1. It returns an internal class (com.sun.*) after parsing, and security policy has not given permission yet
      //   2. Even if permission is given, need to check if it will work
      /*
      if ((doc == null) && this.complete) {
        final byte[] bytes = this.getResponseBytes();
        if (bytes != null) {
          final InputStream in = new ByteArrayInputStream(bytes);
          try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
          } catch (final Exception err) {
            logger.log(Level.SEVERE, "getResponseXML()", err);
          }
          this.document = doc;
        }
      }*/
      return doc;
    }
  }

  private static class LocalResponse {
    private final ClientletResponse cresponse;
    private final CacheableResponse cacheable;

    // Caching fields:
    private Map<String, String> headers;

    /**
     * @param status
     * @param text
     * @param bytes
     * @param headers
     */
    public LocalResponse(final ClientletResponse response) {
      this.cresponse = response;
      this.cacheable = new CacheableResponse();
    }

    public LocalResponse(final ClientletResponse response, final CacheableResponse cacheable) {
      this.cresponse = response;
      this.cacheable = cacheable;
    }

    public CacheableResponse getCacheableResponse() {
      final CacheableResponse c = this.cacheable;
      if (!c.complete) {
        return null;
      }
      return c;
    }

    public void writeBytes(final byte[] bytes, final int offset, final int length) throws java.io.IOException {
      ByteArrayOutputStream out = this.cacheable.buffer;
      if (out == null) {
        out = new ByteArrayOutputStream();
        this.cacheable.buffer = out;
      }
      out.write(bytes, offset, length);
    }

    private ByteArrayOutputStream getBuffer() {
      return this.cacheable.buffer;
    }

    public void setComplete(final boolean complete) {
      this.cacheable.complete = complete;
    }

    public Map<String, String> getHeaders() {
      Map<String, String> h = this.headers;
      if (h == null) {
        h = this.getHeadersImpl();
        this.headers = h;
      }
      return h;
    }

    private Map<String, String> getHeadersImpl() {
      final Map<String, String> headers = new HashMap<>();
      final ClientletResponse cresponse = this.cresponse;
      final Iterator<String> headerNames = cresponse.getHeaderNames();
      while (headerNames.hasNext()) {
        final String headerName = headerNames.next();
        if (headerName != null) {
          final String[] values = cresponse.getHeaders(headerName);
          if ((values != null) && (values.length > 0)) {
            headers.put(headerName.toLowerCase(), values[0]);
          }
        }
      }
      return headers;
    }

    // public int getLength() {
    // final ByteArrayOutputStream out = this.cacheable.buffer;
    // return out == null ? 0 : out.size();
    // }

    /**
     * @return Returns the status.
     */
    public int getStatus() throws IOException {
      return this.cresponse.getResponseCode();
    }

    /**
     * @return Returns the statusText.
     */
    public String getStatusText() throws IOException {
      return this.cresponse.getResponseMessage();
    }

    public String getResponseHeader(final String headerName) {
      return this.getHeaders().get(headerName.toLowerCase());
    }

    public String getAllResponseHeaders(final List<String> excludedHeadersLowerCase) {
      final ClientletResponse cresponse = this.cresponse;
      final Iterator<String> headerNames = cresponse.getHeaderNames();
      final StringBuffer allHeadersBuf = new StringBuffer();
      while (headerNames.hasNext()) {
        final String headerName = headerNames.next();
        if (headerName != null) {
          if (!excludedHeadersLowerCase.contains(headerName.toLowerCase())) {
            final String[] values = cresponse.getHeaders(headerName);
            for (final String value : values) {
              allHeadersBuf.append(headerName);
              allHeadersBuf.append(": ");
              allHeadersBuf.append(value);
              allHeadersBuf.append("\r\n");
            }
          }
        }
      }
      return allHeadersBuf.toString();
    }

    public String getResponseText() {
      return this.cacheable.getResponseText(this.cresponse.getCharset());
    }

    public Document getResponseXML() {
      return this.cacheable.getResponseXML();
    }

    public @NonNull ImageResponse getResponseImage() {
      return this.cacheable.getResponseImage();
    }

    public byte[] getResponseBytes() {
      // TODO: OPTIMIZATION: When the response comes from the RAM cache,
      // there's no need to build a custom buffer here.
      return this.cacheable.getResponseBytes();
    }
  }

  private final Map<String, String> requestedHeaders = new HashMap<>();

  public void addRequestedHeader(final String key, final String value) {
    if ((key != null) && (value != null)) {
      if (requestedHeaders.containsKey(key)) {
        // Need to merge values as per https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest#setRequestHeader()
        final String oldValue = requestedHeaders.get(key);
        requestedHeaders.put(key, oldValue + "," + key);
      }
      requestedHeaders.put(key, value);
    }
  }

}
