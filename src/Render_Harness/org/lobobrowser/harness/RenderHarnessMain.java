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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.gui.HtmlBlockPanel;
import org.lobobrowser.html.gui.HtmlPanel;
import org.lobobrowser.ua.UserAgentContext;

/**
 * Standalone entry point for the headless Render Harness.
 * Supports the Grinder discovery and command protocol without browser chrome,
 * request manager, security manager, or JavaScript.
 */
public final class RenderHarnessMain {
  private static final Logger logger = Logger.getLogger(RenderHarnessMain.class.getName());

  private static volatile File activePortFile = null;
  private static volatile ServerSocket activeReuseServer = null;
  private static volatile boolean protocolHandlersRegistered = false;

  /**
   * Registers the {@code data:} URL stream handler for standalone Render Harness execution.
   */
  public static synchronized void registerProtocolHandlers() {
    if (protocolHandlersRegistered) {
      return;
    }
    try {
      URL.setURLStreamHandlerFactory(protocol ->
          "data".equalsIgnoreCase(protocol) ? new org.lobobrowser.protocol.data.Handler() : null);
    } catch (final Error e) {
      logger.log(Level.FINE, "URLStreamHandlerFactory already registered", e);
    }
    protocolHandlersRegistered = true;
  }

  public static void main(final String[] args) {
    registerProtocolHandlers();
    String grinderKey = System.getProperty("gngr.grinder.key");
    String profilePath = System.getProperty("gngr.profile");
    int width = 900;
    int height = 900;

    for (final String arg : args) {
      if (arg.startsWith("-grinder-key=")) {
        grinderKey = arg.substring("-grinder-key=".length()).trim();
      } else if (arg.startsWith("-profile=")) {
        profilePath = arg.substring("-profile=".length()).trim();
      } else if (arg.startsWith("-width=")) {
        width = Integer.parseInt(arg.substring("-width=".length()).trim());
      } else if (arg.startsWith("-height=")) {
        height = Integer.parseInt(arg.substring("-height=".length()).trim());
      }
    }

    if (grinderKey == null || grinderKey.isEmpty()) {
      final byte[] randomBytes = new byte[32];
      new SecureRandom().nextBytes(randomBytes);
      final StringBuilder sb = new StringBuilder();
      for (byte b : randomBytes) {
        sb.append(String.format("%02x", b));
      }
      grinderKey = sb.toString();
      System.err.println("Notice: No grinder key specified. Using generated key: " + grinderKey);
    }

    final File profileDir;
    if (profilePath != null && !profilePath.isEmpty()) {
      profileDir = new File(profilePath);
    } else {
      profileDir = new File(System.getProperty("user.home"), ".gngr/default");
    }

    if (!profileDir.exists()) {
      profileDir.mkdirs();
    }

    final File portFile = new File(profileDir, "port.dat");
    activePortFile = portFile;

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      deletePortFile();
    }, "HarnessShutdownHook"));

    final ServerSocket reuseServer;
    final int reusePort;
    try {
      reuseServer = new ServerSocket();
      reuseServer.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0));
      reusePort = reuseServer.getLocalPort();
      activeReuseServer = reuseServer;

      try (FileOutputStream fos = new FileOutputStream(portFile);
           DataOutputStream dos = new DataOutputStream(fos)) {
        dos.writeInt(reusePort);
        dos.flush();
      }

      logger.info("Render Harness reuse server listening on 127.0.0.1:" + reusePort);
      logger.info("Port file written to: " + portFile.getAbsolutePath());
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to start reuse server", e);
      System.exit(1);
      return;
    }

    // Initialize UI components on EDT
    final int initW = width;
    final int initH = height;
    final HtmlPanel[] panelHolder = new HtmlPanel[1];
    try {
      SwingUtilities.invokeAndWait(() -> {
        final HtmlPanel panel = createOffscreenHtmlPanel();
        panel.setSize(initW, initH);
        panel.setBounds(0, 0, initW, initH);
        panelHolder[0] = panel;
      });
    } catch (InvocationTargetException | InterruptedException e) {
      logger.log(Level.SEVERE, "Failed to initialize HtmlPanel", e);
      System.exit(1);
      return;
    }

    final HtmlPanel panel = panelHolder[0];
    final HarnessUserAgentContext ucontext = new HarnessUserAgentContext();
    final HarnessHtmlRendererContext rcontext = new HarnessHtmlRendererContext(panel, ucontext);

    // Reuse server loop
    final String finalGrinderKey = grinderKey;
    while (!reuseServer.isClosed()) {
      try {
        final Socket clientSocket = reuseServer.accept();
        clientSocket.setSoTimeout(15000);
        clientSocket.setTcpNoDelay(true);

        try (final InputStream in = clientSocket.getInputStream()) {
          final Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
          final BufferedReader br = new BufferedReader(reader);
          final String line = br.readLine();

          if (line != null && line.startsWith("GRINDER")) {
            logger.info("Received Grinder connection request from " + clientSocket.getRemoteSocketAddress());
            final DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
            final int blankIdx = line.indexOf(' ');

            if (blankIdx != -1) {
              final String suppliedPasskey = line.substring(blankIdx + 1).trim();
              if (verifyAuth(reusePort, suppliedPasskey, finalGrinderKey)) {
                logger.info("Grinder authentication succeeded.");
                final HarnessServer harnessServer = new HarnessServer(panel, ucontext, rcontext);
                final int gsPort = harnessServer.getPort();
                dos.writeInt(gsPort);
                dos.flush();
                logger.info("Started Harness Grinder control server on 127.0.0.1:" + gsPort);
              } else {
                logger.warning("Grinder authentication failed: invalid passkey.");
                dos.writeInt(-1);
                dos.flush();
              }
            } else {
              logger.warning("Grinder request rejected: no passkey supplied.");
              dos.writeInt(-1);
              dos.flush();
            }

            // Wait for client ACK
            br.readLine();
          }
        } catch (Exception e) {
          logger.log(Level.WARNING, "Error handling reuse client connection", e);
        }
      } catch (Exception e) {
        if (!reuseServer.isClosed()) {
          logger.log(Level.WARNING, "Reuse server accept error", e);
        }
      }
    }
  }

  private static boolean verifyAuth(final int port, final String suppliedKey, final String grinderKey) {
    if (grinderKey == null) {
      return false;
    }
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hash = digest.digest((grinderKey + port).getBytes(StandardCharsets.UTF_8));
      final String expected = Base64.getEncoder().encodeToString(hash);
      return expected.equals(suppliedKey);
    } catch (NoSuchAlgorithmException e) {
      logger.log(Level.SEVERE, "SHA-256 not available", e);
      return false;
    }
  }

  public static HtmlPanel createOffscreenHtmlPanel() {
    return new HtmlPanel() {
      private static final long serialVersionUID = 1L;

      @Override
      protected HtmlBlockPanel createHtmlBlockPanel(final UserAgentContext ucontext, final HtmlRendererContext rcontext) {
        return new HtmlBlockPanel(java.awt.Color.WHITE, true, ucontext, rcontext, this) {
          private static final long serialVersionUID = 1L;

          @Override
          protected void validateAll() {
            super.validateAll();
            if (!this.isDisplayable()) {
              doLayoutOffscreen();
            }
          }

          @Override
          protected void revalidatePanel() {
            super.revalidatePanel();
            if (!this.isDisplayable()) {
              doLayoutOffscreen();
            }
          }

          private void doLayoutOffscreen() {
            final java.awt.Container parent = this.getParent();
            if (parent != null) {
              parent.doLayout();
            }
            this.doLayout();
          }
        };
      }
    };
  }

  public static void shutdown() {
    deletePortFile();
    try {
      if (activeReuseServer != null && !activeReuseServer.isClosed()) {
        activeReuseServer.close();
      }
    } catch (IOException ignored) {
    }
    System.exit(0);
  }

  private static void deletePortFile() {
    final File portFile = activePortFile;
    if (portFile != null && portFile.exists()) {
      try {
        portFile.delete();
      } catch (Exception ignored) {
      }
    }
  }
}
