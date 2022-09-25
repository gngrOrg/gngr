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
 * Created on Jun 18, 2005
 */
package org.lobobrowser.main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.ua.NavigatorFrame;

/**
 * The local-bound server that allows the browser JVM to be reused.
 */
public class ReuseServer implements Runnable {
  /**
   *
   */
  public ReuseServer() {
    final Thread t = new Thread(this, "ReuseServer");
    t.setDaemon(true);
    t.start();
  }

  private static final int MIN_PORT = 55000;
  private static final int TOP_PORT = 65000;
  private static final Random RAND = new Random(System.currentTimeMillis());

  private static int getRandomPort() {
    return (Math.abs(RAND.nextInt()) % (TOP_PORT - MIN_PORT)) + MIN_PORT;
  }

  private ServerSocket serverSocket;

  public int start(final InetAddress bindAddr) {
    // Should be called with bindAddr=127.0.0.1 only.
    synchronized (this) {
      if (this.serverSocket != null) {
        throw new IllegalStateException("Already started");
      }
      for (int tries = 0; tries < 100; tries++) {
        final int rport = getRandomPort();
        try {
          final ServerSocket ss = new ServerSocket(rport, 100, bindAddr);
          this.serverSocket = ss;
          this.notify();
          return rport;
        } catch (final IOException ioe) {
          ioe.printStackTrace(System.err);
        }
      }
    }
    throw new IllegalStateException("Unable to bind reuse server after many tries.");
  }

  public void stop() {
    synchronized (this) {
      if (this.serverSocket != null) {
        try {
          this.serverSocket.close();
        } catch (final IOException ioe) {
          // ignore
        }
        this.serverSocket = null;
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Runnable#run()
   */
  public void run() {
    for (;;) {
      try {
        ServerSocket ss;
        synchronized (this) {
          while (this.serverSocket == null) {
            this.wait();
          }
          ss = this.serverSocket;
        }
        try (
          final Socket s = ss.accept()) {
          s.setSoTimeout(10000);
          s.setTcpNoDelay(true);
          try (
            final InputStream in = s.getInputStream()) {
            final Reader reader = new InputStreamReader(in);
            final BufferedReader br = new BufferedReader(reader);
            String line;
            while ((line = br.readLine()) != null) {
              final int blankIdx = line.indexOf(' ');
              final String command = blankIdx == -1 ? line : line.substring(0, blankIdx).trim();
              if ("LAUNCH".equals(command)) {
                if (blankIdx == -1) {
                  PlatformInit.getInstance().launch();
                } else {
                  final String path = line.substring(blankIdx + 1).trim();
                  PlatformInit.getInstance().launch(path);
                }
              } else if ("LAUNCH_BLANK".equals(command)) {
                PlatformInit.getInstance().launch();
              } else if ("GRINDER".equals(command)) {
                final DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                if (blankIdx != -1) {
                  final @NonNull String key = line.substring(blankIdx + 1).trim();
                  if (PlatformInit.getInstance().verifyAuth(ss.getLocalPort(), key)) {
                    final NavigatorFrame frame = PlatformInit.getInstance().launch("about:blank");
                    final GrinderServer gs = new GrinderServer(frame);
                    final int gsPort = gs.getPort();
                    dos.writeInt(gsPort);
                    dos.flush();
                  } else {
                    dos.writeInt(-1);
                    dos.flush();
                  }
                } else {
                  dos.writeInt(-1);
                  dos.flush();
                }
                // Wait for ACK
                br.readLine();
              }
            }
          }
        }
      } catch (final Exception t) {
        t.printStackTrace(System.err);
      }
    }
  }
}
