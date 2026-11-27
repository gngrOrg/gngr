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

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import org.lobobrowser.html.BrowserFrame;
import org.lobobrowser.html.domimpl.FrameNode;
import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.domimpl.NodeImpl;
import org.lobobrowser.html.gui.FrameSetPanel;
import org.lobobrowser.html.gui.HtmlBlockPanel;
import org.lobobrowser.html.gui.HtmlPanel;
import org.lobobrowser.html.parser.DocumentBuilderImpl;
import org.lobobrowser.html.parser.InputSourceImpl;
import org.lobobrowser.ua.UserAgentContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.html.HTMLCollection;

/**
 * Grinder command server for the Render Harness.
 * Handles TO, SET_SIZE, SCREENSHOT, CLOSE, and QUIT commands.
 */
public class HarnessServer implements Runnable {
  private static final Logger logger = Logger.getLogger(HarnessServer.class.getName());

  static final long DEFAULT_LAYOUT_BLOCK_TIMEOUT_MS = 10_000L;
  static final long DEFAULT_LAYOUT_COMPLETION_TIMEOUT_MS = 10_000L;
  static final long DEFAULT_READY_TO_PAINT_TIMEOUT_MS = 5_000L;
  private static final long BLANK_DOC_TIMEOUT_MS = 2_000L;
  private static final long BLANK_DOC_POLL_INTERVAL_MS = 5L;
  private static final long LAYOUT_BLOCK_POLL_INTERVAL_MS = 10L;
  private static final long READY_POLL_INTERVAL_MS = 20L;

  private final HtmlPanel panel;
  private final UserAgentContext ucontext;
  private final HarnessHtmlRendererContext rcontext;
  private final ServerSocket socket;
  private final long layoutBlockTimeoutMs;
  private final long layoutCompletionTimeoutMs;
  private final long readyToPaintTimeoutMs;

  private volatile int currentWidth = 900;
  private volatile int currentHeight = 900;
  private volatile boolean done = false;

  public HarnessServer(final HtmlPanel panel, final UserAgentContext ucontext, final HarnessHtmlRendererContext rcontext) throws IOException {
    this(panel, ucontext, rcontext,
        DEFAULT_LAYOUT_BLOCK_TIMEOUT_MS,
        DEFAULT_LAYOUT_COMPLETION_TIMEOUT_MS,
        DEFAULT_READY_TO_PAINT_TIMEOUT_MS);
  }

  HarnessServer(final HtmlPanel panel, final UserAgentContext ucontext, final HarnessHtmlRendererContext rcontext,
      final long layoutBlockTimeoutMs, final long layoutCompletionTimeoutMs, final long readyToPaintTimeoutMs) throws IOException {
    this.panel = panel;
    this.ucontext = ucontext;
    this.rcontext = rcontext;
    this.layoutBlockTimeoutMs = Math.max(0L, layoutBlockTimeoutMs);
    this.layoutCompletionTimeoutMs = Math.max(0L, layoutCompletionTimeoutMs);
    this.readyToPaintTimeoutMs = Math.max(0L, readyToPaintTimeoutMs);
    this.socket = new ServerSocket(0, 50, InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }));

    loadBlankDocument();

    final Thread t = new Thread(this, "HarnessGrinderServer");
    t.setDaemon(true);
    t.start();
  }

  public int getPort() {
    return socket.getLocalPort();
  }

  private void loadBlankDocument() {
    try {
      rcontext.setCurrentURL("http://localhost/blank");
      final DocumentBuilderImpl builder = new DocumentBuilderImpl(ucontext, rcontext);
      final InputSourceImpl is = new InputSourceImpl(new StringReader("<!DOCTYPE html><html><body></body></html>"), "http://localhost/blank");
      final HTMLDocumentImpl doc = (HTMLDocumentImpl) builder.parse(is);
      SwingUtilities.invokeAndWait(() -> {
        panel.setSize(currentWidth, currentHeight);
        panel.setBounds(0, 0, currentWidth, currentHeight);
        panel.setDocument(doc, rcontext);
      });
      doc.finishModifications();
      waitUntil(() -> !doc.layoutBlocked.get(), BLANK_DOC_TIMEOUT_MS, BLANK_DOC_POLL_INTERVAL_MS,
          () -> "Timed out after " + BLANK_DOC_TIMEOUT_MS + "ms waiting for blank document layout unblock");
      SwingUtilities.invokeAndWait(() -> layoutFrameTree(panel));
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      logger.log(Level.WARNING, "Interrupted initializing blank document", ie);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Error initializing blank document", e);
    }
  }

  @Override
  public void run() {
    while (!done) {
      try {
        final ServerSocket ss = socket;
        if (ss.isClosed()) {
          break;
        }
        try (final Socket s = ss.accept()) {
          s.setSoTimeout(120000);
          s.setTcpNoDelay(true);
          try (final InputStream in = s.getInputStream()) {
            final Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
            final BufferedReader br = new BufferedReader(reader);
            final String commandLine = br.readLine();
            if (commandLine != null) {
              final int blankIdx = commandLine.indexOf(' ');
              final String command = blankIdx == -1 ? commandLine : commandLine.substring(0, blankIdx).trim();
              logger.info("Received Harness Grinder command: " + command);

              if ("TO".equals(command)) {
                if (blankIdx != -1) {
                  final String path = commandLine.substring(blankIdx + 1).trim();
                  handleTo(s, br, path);
                } else {
                  markDoneAndWaitForAck(s, br, 1);
                }
              } else if ("SET_SIZE".equals(command)) {
                if (blankIdx != -1) {
                  final String params = commandLine.substring(blankIdx + 1).trim();
                  handleResize(s, br, params);
                } else {
                  markDoneAndWaitForAck(s, br, 1);
                }
              } else if ("SCREENSHOT".equals(command)) {
                handleScreenShot(s, br);
              } else if ("CLOSE".equals(command)) {
                handleClose(s, br);
              } else if ("QUIT".equals(command)) {
                handleQuit(s, br);
              } else {
                logger.warning("Unknown command: " + command);
                markDoneAndWaitForAck(s, br, 1);
              }
            }
          }
        }
      } catch (final Exception t) {
        if (!done && !socket.isClosed()) {
          logger.log(Level.SEVERE, "Error handling Grinder command", t);
        }
      }
    }

    try {
      if (!socket.isClosed()) {
        socket.close();
      }
    } catch (IOException ignored) {
    }
  }

  private void handleResize(final Socket s, final BufferedReader br, final String params) throws IOException {
    final String[] parts = params.split("[,\\s]+");
    if (parts.length < 2) {
      markDoneAndWaitForAck(s, br, 1);
      return;
    }
    final int w = Integer.parseInt(parts[0]);
    final int h = Integer.parseInt(parts[1]);
    this.currentWidth = w;
    this.currentHeight = h;

    try {
      SwingUtilities.invokeAndWait(() -> {
        panel.setSize(w, h);
        panel.setBounds(0, 0, w, h);
        layoutFrameTree(panel);
      });
    } catch (InvocationTargetException | InterruptedException e) {
      logger.log(Level.WARNING, "Error resizing viewport", e);
    }

    markDoneAndWaitForAck(s, br, 0);
  }

  public static URL resolveURL(final String path) throws MalformedURLException {
    if (path == null || path.isEmpty()) {
      throw new MalformedURLException("Empty path");
    }

    // 1. If it already has a protocol scheme, parse directly
    if (path.regionMatches(true, 0, "data:", 0, 5)) {
      RenderHarnessMain.registerProtocolHandlers();
      return new URL(path);
    }
    if (path.contains("://") || path.startsWith("file:") || path.startsWith("jar:")) {
      return new URL(path);
    }

    // 2. Absolute filesystem paths (/... or C:\...)
    if (path.startsWith("/") || (path.length() > 2 && Character.isLetter(path.charAt(0)) && path.charAt(1) == ':' && (path.charAt(2) == '/' || path.charAt(2) == '\\'))) {
      return new File(path).toURI().toURL();
    }

    // 3. Explicit relative filesystem paths (./... or ../...)
    if (path.startsWith("./") || path.startsWith("../")) {
      return new File(path).toURI().toURL();
    }

    // 4. Existing file or directory on local filesystem
    final File localFile = new File(path);
    if (localFile.exists()) {
      return localFile.toURI().toURL();
    }

    // 5. Hostname or host:port (e.g. localhost:8000/..., 127.0.0.1:8000/..., example.com/...)
    return new URL("http://" + path);
  }

  private void handleTo(final Socket s, final BufferedReader br, final String path) throws IOException {
    final URL url;
    try {
      url = resolveURL(path);
    } catch (Exception e) {
      logger.warning("Invalid URL or path: " + path);
      markDoneAndWaitForAck(s, br, 1);
      return;
    }

    try {
      rcontext.setCurrentURL(url.toExternalForm());

      final URLConnection conn = url.openConnection();
      conn.setConnectTimeout(10000);
      conn.setReadTimeout(15000);
      conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; gngr/1.0; RenderHarness)");

      final String rawContentType = conn.getContentType();
      final String charset = resolveCharset(rawContentType);
      final String mimeType = resolveMimeType(rawContentType, url);

      final HTMLDocumentImpl doc;
      try (InputStream in = conn.getInputStream()) {
        final InputSourceImpl is = new InputSourceImpl(in, url.toExternalForm(), charset);
        final DocumentBuilderImpl builder = new DocumentBuilderImpl(ucontext, rcontext);
        doc = (HTMLDocumentImpl) builder.createDocument(is, mimeType);
        doc.load(false);
      }

      SwingUtilities.invokeAndWait(() -> {
        panel.setSize(currentWidth, currentHeight);
        panel.setBounds(0, 0, currentWidth, currentHeight);
        panel.setDocument(doc, rcontext);
      });

      doc.finishModifications();

      // Wait for layout-blocking jobs (e.g. external <link> stylesheets) and in-flight requests to finish
      waitUntil(() -> !doc.layoutBlocked.get() && HarnessNetworkRequest.getActiveRequestCount() == 0,
          layoutBlockTimeoutMs, LAYOUT_BLOCK_POLL_INTERVAL_MS, () ->
              "Timed out after " + layoutBlockTimeoutMs + "ms waiting for layout unblock/network requests for " + url
                  + " (layoutBlocked=" + doc.layoutBlocked.get()
                  + ", activeRequests=" + HarnessNetworkRequest.getActiveRequestCount() + ")");

      // Trigger layout on EDT now that layout is unblocked and external stylesheets are loaded
      @SuppressWarnings("unchecked")
      final Future<Boolean>[] layoutFutureHolder = new Future[1];
      SwingUtilities.invokeAndWait(() -> {
        panel.setSize(currentWidth, currentHeight);
        panel.setBounds(0, 0, currentWidth, currentHeight);
        layoutFrameTree(panel);
        if (panel.getFrameSetPanel() == null) {
          layoutFutureHolder[0] = panel.layoutCompletion();
        }
      });

      // Wait for layout completion
      final Future<Boolean> layoutFuture = layoutFutureHolder[0];
      if (layoutFuture != null) {
        try {
          final Boolean completed = layoutFuture.get(layoutCompletionTimeoutMs, TimeUnit.MILLISECONDS);
          if (Boolean.FALSE.equals(completed)) {
            throw new IOException("Layout failed for " + url);
          }
        } catch (TimeoutException te) {
          throw new TimeoutException(
              "Timed out after " + layoutCompletionTimeoutMs + "ms waiting for layout completion for " + url);
        }
      }

      // Wait until ready to paint and no active network requests remain
      waitUntilReadyToPaint("ready-to-paint after navigation to " + url);

      markDoneAndWaitForAck(s, br, 0);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      logger.log(Level.WARNING, "Interrupted while navigating to: " + path, ie);
      markDoneAndWaitForAck(s, br, 1);
    } catch (TimeoutException te) {
      logger.log(Level.WARNING, "Timed out while navigating to: " + path + " - " + te.getMessage(), te);
      markDoneAndWaitForAck(s, br, 1);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Failed to navigate to: " + path, e);
      markDoneAndWaitForAck(s, br, 1);
    }
  }

  private void handleScreenShot(final Socket s, final BufferedReader br) throws IOException {
    try {
      waitUntilReadyToPaint("screenshot capture");
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      logger.log(Level.WARNING, "Interrupted while waiting for screenshot readiness", ie);
      writeScreenshotError(s);
      return;
    } catch (TimeoutException te) {
      logger.log(Level.WARNING, "Screenshot readiness timeout: " + te.getMessage(), te);
      writeScreenshotError(s);
      return;
    } catch (InvocationTargetException ite) {
      logger.log(Level.SEVERE, "Error checking screenshot readiness", ite);
      writeScreenshotError(s);
      return;
    }

    final int width = Math.max(1, currentWidth);
    final int height = Math.max(1, currentHeight);
    final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      SwingUtilities.invokeAndWait(() -> {
        panel.setSize(width, height);
        panel.setBounds(0, 0, width, height);
        disableRenderHintsTree(panel);
        layoutFrameTree(panel);
        panel.paint(g);
      });
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      logger.log(Level.SEVERE, "Interrupted while painting component for screenshot", ie);
      writeScreenshotError(s);
      return;
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error painting component", e);
      writeScreenshotError(s);
      return;
    } finally {
      g.dispose();
    }

    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ImageIO.write(img, "PNG", bos);

    final OutputStream os = s.getOutputStream();
    final DataOutputStream dos = new DataOutputStream(os);
    dos.writeInt(bos.size());
    dos.flush();
    bos.writeTo(os);
    os.flush();

    // Wait for ACK
    br.readLine();
  }

  private static void writeScreenshotError(final Socket s) throws IOException {
    final DataOutputStream dos = new DataOutputStream(s.getOutputStream());
    dos.writeInt(-1);
    dos.flush();
  }

  private void handleClose(final Socket s, final BufferedReader br) throws IOException {
    loadBlankDocument();
    done = true;
    try {
      socket.close();
    } catch (IOException ignored) {
    }
  }

  private void handleQuit(final Socket s, final BufferedReader br) throws IOException {
    done = true;
    try {
      socket.close();
    } catch (IOException ignored) {
    }
    RenderHarnessMain.shutdown();
  }

  @FunctionalInterface
  private interface ReadyCondition {
    boolean isSatisfied() throws InterruptedException, InvocationTargetException;
  }

  private static void waitUntil(final ReadyCondition condition, final long timeoutMs, final long pollIntervalMs,
      final Supplier<String> timeoutMessageSupplier)
      throws TimeoutException, InterruptedException, InvocationTargetException {
    final long timeoutNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(0L, timeoutMs));
    final long deadlineNanos = System.nanoTime() + timeoutNanos;
    while (!condition.isSatisfied()) {
      final long remainingNanos = deadlineNanos - System.nanoTime();
      if (remainingNanos <= 0L) {
        throw new TimeoutException(timeoutMessageSupplier.get());
      }
      final long remainingMs = TimeUnit.NANOSECONDS.toMillis(remainingNanos);
      final long sleepMs = Math.max(1L, Math.min(pollIntervalMs, remainingMs));
      Thread.sleep(sleepMs);
    }
  }

  private void waitUntilReadyToPaint(final String context)
      throws TimeoutException, InterruptedException, InvocationTargetException {
    final boolean[] lastReady = { false };
    waitUntil(() -> {
      final boolean ready = isReadyToPaint();
      lastReady[0] = ready;
      return ready && HarnessNetworkRequest.getActiveRequestCount() == 0;
    }, readyToPaintTimeoutMs, READY_POLL_INTERVAL_MS, () ->
        "Timed out after " + readyToPaintTimeoutMs + "ms waiting for " + context
            + " (readyToPaint=" + lastReady[0]
            + ", activeRequests=" + HarnessNetworkRequest.getActiveRequestCount() + ")");
  }

  private class ReadyChecker implements Runnable {
    boolean isReady = false;

    @Override
    public void run() {
      isReady = isPanelTreeReadyToPaint(panel, new HashSet<>());
    }
  }

  private boolean isPanelTreeReadyToPaint(final HtmlPanel targetPanel, final Set<HtmlPanel> visited) {
    if (targetPanel == null || !visited.add(targetPanel)) {
      return true;
    }
    final NodeImpl rootNode = targetPanel.getRootNode();
    if (!(rootNode instanceof HTMLDocumentImpl)) {
      return targetPanel.isReadyToPaint();
    }
    final HTMLDocumentImpl doc = (HTMLDocumentImpl) rootNode;
    if (doc.layoutBlocked.get()) {
      return false;
    }
    if (targetPanel.getFrameSetPanel() == null) {
      final Future<Boolean> layoutFuture = targetPanel.layoutCompletion();
      if (layoutFuture != null && !layoutFuture.isDone()) {
        layoutFrameTree(targetPanel);
        if (!layoutFuture.isDone()) {
          return false;
        }
      }
      if (!targetPanel.isReadyToPaint()) {
        return false;
      }
    }
    final HTMLCollection frames = doc.getFrames();
    if (frames != null) {
      final int len = frames.getLength();
      for (int i = 0; i < len; i++) {
        final Node node = frames.item(i);
        if (!(node instanceof FrameNode)) {
          continue;
        }
        final BrowserFrame bf = ((FrameNode) node).getBrowserFrame();
        if (bf == null) {
          continue;
        }
        if (bf instanceof HarnessHtmlRendererContext.HarnessBrowserFrame) {
          final HarnessHtmlRendererContext.HarnessBrowserFrame hbf =
              (HarnessHtmlRendererContext.HarnessBrowserFrame) bf;
          if (hbf.isLoading()) {
            return false;
          }
          boolean expectsLoad = false;
          if (node instanceof Element) {
            final String src = ((Element) node).getAttribute("src");
            if (src != null) {
              try {
                final URL fullUrl = doc.getFullURL(src);
                expectsLoad = fullUrl != null && ucontext.isRequestPermitted(
                    new UserAgentContext.Request(fullUrl, UserAgentContext.RequestKind.Frame));
              } catch (final MalformedURLException ignored) {
              }
            }
          }
          if (expectsLoad && !hbf.isLoadAttempted()) {
            return false;
          }
        }
        final Component comp = bf.getComponent();
        if (comp instanceof HtmlPanel) {
          final HtmlPanel subPanel = (HtmlPanel) comp;
          if (subPanel.getRootNode() != null && !isPanelTreeReadyToPaint(subPanel, visited)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private static boolean isFrameContainer(final Component comp) {
    return (comp instanceof HtmlPanel)
        || (comp instanceof HtmlBlockPanel)
        || (comp instanceof FrameSetPanel)
        || (comp instanceof JSplitPane);
  }

  private static void layoutFrameTree(final Component comp) {
    if (!isFrameContainer(comp)) {
      return;
    }
    comp.validate();
    comp.doLayout();
    if (comp instanceof Container) {
      for (final Component child : ((Container) comp).getComponents()) {
        if (isFrameContainer(child)) {
          layoutFrameTree(child);
        }
      }
    }
  }

  private static void disableRenderHintsTree(final Component comp) {
    if (!isFrameContainer(comp)) {
      return;
    }
    if (comp instanceof HtmlPanel) {
      final HtmlPanel hp = (HtmlPanel) comp;
      if (hp.getFrameSetPanel() == null && hp.getComponentCount() > 0 && hp.getComponent(0) instanceof HtmlBlockPanel) {
        hp.disableRenderHints();
      }
    }
    if (comp instanceof Container) {
      for (final Component child : ((Container) comp).getComponents()) {
        if (isFrameContainer(child)) {
          disableRenderHintsTree(child);
        }
      }
    }
  }

  private boolean isReadyToPaint() throws InterruptedException, InvocationTargetException {
    final ReadyChecker checker = new ReadyChecker();
    SwingUtilities.invokeAndWait(checker);
    return checker.isReady;
  }

  private static void markDoneAndWaitForAck(final Socket s, final BufferedReader br, final int status) throws IOException {
    final DataOutputStream dos = new DataOutputStream(s.getOutputStream());
    dos.writeInt(status);
    dos.flush();
    br.readLine();
  }

  static String resolveMimeType(final String contentType, final URL url) {
    String mimeType = extractMimeType(contentType);
    if (mimeType == null || "content/unknown".equalsIgnoreCase(mimeType) || "application/octet-stream".equalsIgnoreCase(mimeType)) {
      final String lowerPath = url.getPath().toLowerCase(Locale.ROOT);
      if (lowerPath.endsWith(".xht") || lowerPath.endsWith(".xhtml") || lowerPath.endsWith(".xml")) {
        mimeType = "application/xhtml+xml";
      } else {
        mimeType = "text/html";
      }
    }
    return mimeType;
  }

  static String resolveCharset(final String contentType) {
    final String candidate = extractCharset(contentType);
    if (candidate != null && !candidate.isEmpty()) {
      try {
        if (Charset.isSupported(candidate)) {
          return candidate;
        }
        logger.warning("Unsupported charset '" + candidate + "', falling back to UTF-8");
      } catch (final IllegalCharsetNameException e) {
        logger.warning("Illegal charset name '" + candidate + "', falling back to UTF-8");
      }
    }
    return "UTF-8";
  }

  private static String extractMimeType(final String contentType) {
    if (contentType == null) {
      return null;
    }
    final int semi = contentType.indexOf(';');
    final String mime = (semi == -1 ? contentType : contentType.substring(0, semi)).trim();
    return mime.isEmpty() ? null : mime.toLowerCase(Locale.ROOT);
  }

  private static String extractCharset(final String contentType) {
    if (contentType == null) {
      return null;
    }
    for (String param : contentType.split(";")) {
      param = param.trim();
      final int eqIdx = param.indexOf('=');
      if (eqIdx > 0 && "charset".equalsIgnoreCase(param.substring(0, eqIdx).trim())) {
        String val = param.substring(eqIdx + 1).trim();
        if (val.length() >= 2 && ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'")))) {
          val = val.substring(1, val.length() - 1).trim();
        }
        return val.isEmpty() ? null : val;
      }
    }
    return null;
  }
}
