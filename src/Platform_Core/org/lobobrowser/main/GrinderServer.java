/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2015 Uproot Labs India Pvt Ltd

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

 */

package org.lobobrowser.main;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.lobobrowser.clientlet.ComponentContent;
import org.lobobrowser.ua.NavigatorFrame;
import org.lobobrowser.ua.NavigatorProgressEvent;
import org.lobobrowser.ua.ProgressType;

class GrinderServer implements Runnable {
  private final NavigatorFrame frame;
  private final ServerSocket socket;

  public GrinderServer(final NavigatorFrame frame) throws IOException {
    this.frame = frame;
    socket = new ServerSocket(0);
    final Thread t = new Thread(this, "GrinderServer");
    t.setDaemon(true);
    t.start();

  }

  @Override
  public void run() {
    boolean done = false;
    while (!done) {
      try {
        final ServerSocket ss = socket;
        try (
          final Socket s = ss.accept()) {
          s.setSoTimeout(10000);
          s.setTcpNoDelay(true);
          try (
            final InputStream in = s.getInputStream()) {
            final Reader reader = new InputStreamReader(in);
            final BufferedReader br = new BufferedReader(reader);
            String commandLine = br.readLine();
            if (commandLine != null) {
              final int blankIdx = commandLine.indexOf(' ');
              final String command = blankIdx == -1 ? commandLine : commandLine.substring(0, blankIdx).trim();
              // System.out.println("Command: " + command);
              if ("TO".equals(command)) {
                if (blankIdx != -1) {
                  final String path = commandLine.substring(blankIdx + 1).trim();
                  handleTo(s, br, path);
                }
              } else if ("SET_SIZE".equals(command)) {
                if (blankIdx != -1) {
                  final String params = commandLine.substring(blankIdx + 1).trim();
                  handleResize(s, br, params);
                }
              } else if ("SCREENSHOT".equals(command)) {
                handleScreenShot(s, br);
              } else if ("CLOSE".equals(command)) {
                frame.closeWindow();
                done = true;
              } else if ("QUIT".equals(command)) {
                frame.closeWindow();
                PlatformInit.shutdown();
                done = true;
              }
            }
          }
        }
      } catch (final Exception t) {
        t.printStackTrace(System.err);
      }
    }

  }

  private void handleResize(final Socket s, final BufferedReader br, final String params) throws IOException {
    final String[] parts = params.split("[,\\s]+");
    final int w = Integer.parseInt(parts[0]);
    final int h = Integer.parseInt(parts[1]);
    try {
      SwingUtilities.invokeAndWait(() -> {
        frame.resizeWindowTo(w, h);
      });
    } catch (InvocationTargetException | InterruptedException e) {
      e.printStackTrace();
    }

    markDoneAndWaitForAck(s, br);
  }

  // TODO: Add way to mark error
  private static void markDoneAndWaitForAck(final Socket s, final BufferedReader br) throws IOException {
    final DataOutputStream dos = new DataOutputStream(s.getOutputStream());
    dos.writeInt(0);
    dos.flush();

    // Wait for ACK
    br.readLine();
  }

  private class ReadyChecker implements Runnable {
    boolean isReady = false;
    @Override
    public void run() {
      isReady = frame.getComponentContent().isReadyToPaint();
    }
  }

  private boolean isReadyToPaint() {
    try {
      ReadyChecker checker = new ReadyChecker();
      SwingUtilities.invokeAndWait(checker);
      return checker.isReady;
    } catch (InvocationTargetException | InterruptedException e) {
      e.printStackTrace();
    }
    return true;
  }

  private void handleScreenShot(final Socket s, final BufferedReader br) throws IOException {
    try {
      while (!isReadyToPaint()) {
        Thread.sleep(50);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    final ComponentContent componentContent = frame.getComponentContent();
    componentContent.disableRenderHints();

    final Component component = componentContent.getComponent();
    final BufferedImage img = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics g = img.getGraphics();
    try {
      SwingUtilities.invokeAndWait(() -> {
        component.paint(g);
      });
    } catch (InvocationTargetException | InterruptedException e) {
      e.printStackTrace();
    }
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ImageIO.write(img, "PNG", bos);
    final OutputStream os = s.getOutputStream();
    {
      final DataOutputStream dos = new DataOutputStream(os);
      dos.writeInt(bos.size());
      dos.flush();
    }
    bos.writeTo(os);
    os.flush();

    // Wait for ACK
    br.readLine();
  }

  private void handleTo(final Socket s, final BufferedReader br, final String path)
      throws MalformedURLException, InterruptedException, ExecutionException, IOException {
    // System.out.println("  path: " + path);
    frame.setProgressEvent(null);
    frame.navigate(path);
    waitForNavigationCompletion();

    // TODO: reload and wait for navigation after enabling all requests?
    frame.allowAllFirstPartyRequests();

    markDoneAndWaitForAck(s, br);
  }

  private void waitForNavigationCompletion() throws InterruptedException, ExecutionException {
    {
      NavigatorProgressEvent progressEvent = frame.getProgressEvent();
      while (progressEvent == null || progressEvent.getProgressType() != ProgressType.DONE) {
        Thread.sleep(10);
        progressEvent = frame.getProgressEvent();
      }

      while (frame.getComponentContent() == null) {
        Thread.sleep(10);
      }

      final Component component = frame.getComponentContent().getComponent();
      if (component instanceof DefferedLayoutSupport) {
        final DefferedLayoutSupport defSupport = (DefferedLayoutSupport) component;
        defSupport.layoutCompletion().get();
      }
    }
  }

  public int getPort() {
    return socket.getLocalPort();
  }
}