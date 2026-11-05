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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.gui.HtmlPanel;
import org.lobobrowser.html.parser.DocumentBuilderImpl;
import org.lobobrowser.html.parser.InputSourceImpl;
import org.lobobrowser.ua.UserAgentContext;
import org.w3c.dom.Document;

/**
 * Grinder command server for the Render Harness.
 * Handles TO, SET_SIZE, SCREENSHOT, CLOSE, and QUIT commands.
 */
public class HarnessServer implements Runnable {
  private static final Logger logger = Logger.getLogger(HarnessServer.class.getName());

  private final HtmlPanel panel;
  private final UserAgentContext ucontext;
  private final HarnessHtmlRendererContext rcontext;
  private final ServerSocket socket;

  private volatile int currentWidth = 900;
  private volatile int currentHeight = 900;
  private volatile boolean done = false;

  public HarnessServer(final HtmlPanel panel, final UserAgentContext ucontext, final HarnessHtmlRendererContext rcontext) throws IOException {
    this.panel = panel;
    this.ucontext = ucontext;
    this.rcontext = rcontext;
    this.socket = new ServerSocket(0);

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
      final DocumentBuilderImpl builder = new DocumentBuilderImpl(ucontext, rcontext);
      final InputSourceImpl is = new InputSourceImpl(new StringReader("<!DOCTYPE html><html><body></body></html>"), "http://localhost/blank");
      final HTMLDocumentImpl doc = (HTMLDocumentImpl) builder.parse(is);
      doc.finishModifications();
      SwingUtilities.invokeAndWait(() -> {
        panel.setSize(currentWidth, currentHeight);
        panel.setBounds(0, 0, currentWidth, currentHeight);
        panel.setDocument(doc, rcontext);
        panel.validate();
        panel.doLayout();
        for (java.awt.Component c : panel.getComponents()) {
          c.validate();
          c.doLayout();
        }
      });
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
        panel.validate();
        panel.doLayout();
        for (java.awt.Component c : panel.getComponents()) {
          c.validate();
          c.doLayout();
        }
      });
    } catch (InvocationTargetException | InterruptedException e) {
      logger.log(Level.WARNING, "Error resizing viewport", e);
    }

    markDoneAndWaitForAck(s, br, 0);
  }

  private void handleTo(final Socket s, final BufferedReader br, final String path) throws IOException {
    URL url;
    try {
      if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("file:") || path.startsWith("jar:")) {
        url = new URL(path);
      } else {
        final File file = new File(path);
        url = file.toURI().toURL();
      }
    } catch (Exception e) {
      logger.warning("Invalid URL or path: " + path);
      markDoneAndWaitForAck(s, br, 1);
      return;
    }

    try {
      final URLConnection conn = url.openConnection();
      conn.setConnectTimeout(10000);
      conn.setReadTimeout(15000);
      conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; gngr/1.0; RenderHarness)");

      final String contentType = conn.getContentType();
      String charset = extractCharset(contentType);
      if (charset == null) {
        charset = "UTF-8";
      }

      final HTMLDocumentImpl doc;
      try (InputStream in = conn.getInputStream()) {
        final InputSourceImpl is = new InputSourceImpl(in, url.toExternalForm(), charset);
        final DocumentBuilderImpl builder = new DocumentBuilderImpl(ucontext, rcontext);
        doc = (HTMLDocumentImpl) builder.createDocument(is, contentType != null ? contentType : "text/html");
        doc.load(false);
      }

      doc.finishModifications();

      SwingUtilities.invokeAndWait(() -> {
        panel.setSize(currentWidth, currentHeight);
        panel.setBounds(0, 0, currentWidth, currentHeight);
        panel.setDocument(doc, rcontext);
        panel.validate();
        panel.doLayout();
        for (java.awt.Component c : panel.getComponents()) {
          c.validate();
          c.doLayout();
        }
      });

      rcontext.setCurrentURL(url.toExternalForm());

      // Wait for layout completion
      final Future<Boolean> layoutFuture = panel.layoutCompletion();
      if (layoutFuture != null) {
        try {
          layoutFuture.get(10, TimeUnit.SECONDS);
        } catch (Exception te) {
          logger.log(Level.WARNING, "Layout completion timeout for " + url, te);
        }
      }

      // Wait until ready to paint
      final long start = System.currentTimeMillis();
      while (!isReadyToPaint() && (System.currentTimeMillis() - start < 5000)) {
        try {
          Thread.sleep(20);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          break;
        }
      }

      markDoneAndWaitForAck(s, br, 0);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Failed to navigate to: " + path, e);
      markDoneAndWaitForAck(s, br, 1);
    }
  }

  private void handleScreenShot(final Socket s, final BufferedReader br) throws IOException {
    final long start = System.currentTimeMillis();
    while (!isReadyToPaint() && (System.currentTimeMillis() - start < 5000)) {
      try {
        Thread.sleep(20);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    panel.disableRenderHints();

    final int width = Math.max(1, currentWidth);
    final int height = Math.max(1, currentHeight);
    final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      SwingUtilities.invokeAndWait(() -> {
        panel.paint(g);
      });
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error painting component", e);
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

  private class ReadyChecker implements Runnable {
    boolean isReady = false;

    @Override
    public void run() {
      isReady = panel.isReadyToPaint();
    }
  }

  private boolean isReadyToPaint() {
    try {
      final ReadyChecker checker = new ReadyChecker();
      SwingUtilities.invokeAndWait(checker);
      return checker.isReady;
    } catch (Exception e) {
      return true;
    }
  }

  private static void markDoneAndWaitForAck(final Socket s, final BufferedReader br, final int status) throws IOException {
    final DataOutputStream dos = new DataOutputStream(s.getOutputStream());
    dos.writeInt(status);
    dos.flush();
    br.readLine();
  }

  private static String extractCharset(final String contentType) {
    if (contentType == null) {
      return null;
    }
    for (String param : contentType.split(";")) {
      param = param.trim();
      if (param.toLowerCase().startsWith("charset=")) {
        return param.substring(8).trim().replace("\"", "");
      }
    }
    return null;
  }
}
