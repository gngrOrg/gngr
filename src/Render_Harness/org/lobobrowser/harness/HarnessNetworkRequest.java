/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2026 gngr authors

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.
*/

package org.lobobrowser.harness;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.ua.ImageResponse;
import org.lobobrowser.ua.NetworkRequest;
import org.lobobrowser.ua.NetworkRequestEvent;
import org.lobobrowser.ua.NetworkRequestListener;
import org.lobobrowser.ua.UserAgentContext.Request;
import org.w3c.dom.Document;

/**
 * Lightweight NetworkRequest implementation for the Render Harness.
 * Uses standard java.net.URLConnection with no cache, cookies, or security filtering.
 */
public class HarnessNetworkRequest implements NetworkRequest {
  private static final Logger logger = Logger.getLogger(HarnessNetworkRequest.class.getName());

  private volatile int readyState = STATE_UNINITIALIZED;
  private volatile int status = 0;
  private volatile String statusText = "";
  private volatile byte[] responseBytes = null;
  private volatile ImageResponse imageResponse = null;
  private volatile URL requestURL = null;
  private volatile String method = "GET";
  private volatile boolean async = false;

  private final Map<String, String> requestHeaders = new HashMap<>();
  private volatile Map<String, List<String>> responseHeaders = Collections.emptyMap();
  private final List<NetworkRequestListener> listeners = new CopyOnWriteArrayList<>();

  public HarnessNetworkRequest() {
  }

  @Override
  public int getReadyState() {
    return readyState;
  }

  @Override
  public String getResponseText() {
    final byte[] bytes = this.responseBytes;
    if (bytes == null) {
      return null;
    }
    String charset = "UTF-8";
    final String contentType = getResponseHeader("Content-Type");
    if (contentType != null) {
      for (String param : contentType.split(";")) {
        param = param.trim();
        if (param.toLowerCase().startsWith("charset=")) {
          charset = param.substring(8).trim().replace("\"", "");
          break;
        }
      }
    }
    try {
      return new String(bytes, charset);
    } catch (Exception e) {
      return new String(bytes, StandardCharsets.UTF_8);
    }
  }

  @Override
  public Document getResponseXML() {
    return null;
  }

  @Override
  public @NonNull ImageResponse getResponseImage() {
    final ImageResponse ir = this.imageResponse;
    if (ir != null) {
      return ir;
    }
    final byte[] bytes = this.responseBytes;
    if (bytes == null || bytes.length == 0) {
      final ImageResponse empty = new ImageResponse(ImageResponse.State.error, null);
      this.imageResponse = empty;
      return empty;
    }
    try {
      final BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
      final ImageResponse result = img != null
          ? new ImageResponse(ImageResponse.State.loaded, img)
          : new ImageResponse(ImageResponse.State.error, null);
      this.imageResponse = result;
      return result;
    } catch (Exception e) {
      final ImageResponse err = new ImageResponse(ImageResponse.State.error, null);
      this.imageResponse = err;
      return err;
    }
  }

  @Override
  public byte[] getResponseBytes() {
    return responseBytes;
  }

  @Override
  public int getStatus() {
    return status;
  }

  @Override
  public String getStatusText() {
    return statusText;
  }

  @Override
  public void abort() {
    changeReadyState(STATE_ABORTED);
  }

  @Override
  public String getAllResponseHeaders(final List<String> excludedHeadersLowerCase) {
    final StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
      final String key = entry.getKey();
      if (key != null) {
        if (excludedHeadersLowerCase == null || !excludedHeadersLowerCase.contains(key.toLowerCase())) {
          for (String val : entry.getValue()) {
            sb.append(key).append(": ").append(val).append("\r\n");
          }
        }
      }
    }
    return sb.toString();
  }

  @Override
  public String getResponseHeader(final String headerName) {
    if (headerName == null) {
      return null;
    }
    for (Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
      if (headerName.equalsIgnoreCase(entry.getKey())) {
        final List<String> vals = entry.getValue();
        return (vals != null && !vals.isEmpty()) ? vals.get(0) : null;
      }
    }
    return null;
  }

  @Override
  public void open(final String method, final String url) throws IOException {
    open(method, new URL(url), true);
  }

  @Override
  public void open(final String method, final @NonNull URL url) throws IOException {
    open(method, url, true);
  }

  @Override
  public void open(final String method, final @NonNull URL url, final boolean asyncFlag) throws IOException {
    this.method = method;
    this.requestURL = url;
    this.async = asyncFlag;
    changeReadyState(STATE_LOADING);
  }

  @Override
  public void open(final String method, final String url, final boolean asyncFlag, final String integrity) throws IOException {
    open(method, new URL(url), asyncFlag);
  }

  @Override
  public void open(final String method, final @NonNull URL url, final boolean asyncFlag, final String userName) throws IOException {
    open(method, url, asyncFlag);
  }

  @Override
  public void open(final String method, final @NonNull URL url, final boolean asyncFlag, final String userName, final String password, final String integrity) throws IOException {
    open(method, url, asyncFlag);
  }

  @Override
  public void send(final String content, final Request requestType) throws IOException {
    if (async) {
      final Thread t = new Thread(this::executeRequest, "HarnessNetReq-" + requestURL);
      t.setDaemon(true);
      t.start();
    } else {
      executeRequest();
    }
  }

  private void executeRequest() {
    try {
      final URL url = this.requestURL;
      if (url == null) {
        throw new IOException("Request URL not set");
      }
      final URLConnection conn = url.openConnection();
      conn.setConnectTimeout(10000);
      conn.setReadTimeout(15000);
      for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
        conn.setRequestProperty(header.getKey(), header.getValue());
      }
      if (conn instanceof HttpURLConnection) {
        final HttpURLConnection httpConn = (HttpURLConnection) conn;
        httpConn.setRequestMethod(method);
        httpConn.setInstanceFollowRedirects(true);
        httpConn.connect();
        this.status = httpConn.getResponseCode();
        this.statusText = httpConn.getResponseMessage();
        this.responseHeaders = httpConn.getHeaderFields();
      } else {
        conn.connect();
        this.status = 200;
        this.statusText = "OK";
        this.responseHeaders = conn.getHeaderFields();
      }

      changeReadyState(STATE_LOADED);

      final ByteArrayOutputStream bos = new ByteArrayOutputStream();
      try (InputStream in = (status >= 400 && conn instanceof HttpURLConnection)
          ? ((HttpURLConnection) conn).getErrorStream()
          : conn.getInputStream()) {
        if (in != null) {
          final byte[] buf = new byte[8192];
          int read;
          while ((read = in.read(buf)) != -1) {
            bos.write(buf, 0, read);
          }
        }
      }
      this.responseBytes = bos.toByteArray();

      // Pre-decode image if possible
      try {
        final BufferedImage img = ImageIO.read(new ByteArrayInputStream(responseBytes));
        this.imageResponse = img != null
            ? new ImageResponse(ImageResponse.State.loaded, img)
            : new ImageResponse(ImageResponse.State.error, null);
      } catch (Exception ignored) {
        this.imageResponse = new ImageResponse(ImageResponse.State.error, null);
      }

      changeReadyState(STATE_COMPLETE);
    } catch (Exception e) {
      logger.log(Level.FINE, "Failed to load resource: " + requestURL, e);
      this.status = 500;
      this.statusText = e.getMessage() != null ? e.getMessage() : "Error";
      this.imageResponse = new ImageResponse(ImageResponse.State.error, null);
      changeReadyState(STATE_ABORTED);
    }
  }

  private void changeReadyState(final int state) {
    this.readyState = state;
    final NetworkRequestEvent event = new NetworkRequestEvent(this, state);
    for (NetworkRequestListener listener : listeners) {
      try {
        listener.readyStateChanged(event);
      } catch (Exception e) {
        logger.log(Level.WARNING, "Error notifying NetworkRequestListener", e);
      }
    }
  }

  @Override
  public void addNetworkRequestListener(final NetworkRequestListener listener) {
    listeners.add(listener);
  }

  @Override
  public Optional<URL> getURL() {
    return Optional.ofNullable(requestURL);
  }

  @Override
  public boolean isAsnyc() {
    return async;
  }

  @Override
  public void addRequestedHeader(final String header, final String value) {
    requestHeaders.put(header, value);
  }
}
