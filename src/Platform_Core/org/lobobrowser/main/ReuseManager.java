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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocketFactory;

import org.lobobrowser.main.TrustManager.SSLInfo;
import org.lobobrowser.store.StorageManager;

/**
 * Class in charge of allowing mutiple browser launches to share a JVM.
 */
public class ReuseManager {
  private ReuseManager() {
    super();
  }

  private static final ReuseManager instance = new ReuseManager();

  public static ReuseManager getInstance() throws Exception {
    return instance;
  }

  private static final String PORT_FILE = "port.dat";

  public void shutdown() {
    final java.io.File appHome = StorageManager.getInstance().getAppHome();
    final java.io.File portFile = new File(appHome, PORT_FILE);
    portFile.delete();
  }

  /**
   * May launch in this VM or a second one.
   */
  public void launch(final String[] args, final SSLInfo sslInfo) throws Exception {
    boolean launched = false;
    // long time1 = System.currentTimeMillis();
    try {
      // Bind host for reuse server is 127.0.0.1, and it can
      // only be accessed locally.
      final InetAddress bindHost = InetAddress.getByAddress(new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 1 });
      final java.io.File appHome = StorageManager.getInstance().getAppHome();
      final java.io.File portFile = new File(appHome, PORT_FILE);
      OUTER: for (int tries = 0; tries < 5; tries++) {
        // Look for running VM
        int port = -1;
        try (
            final InputStream in = new FileInputStream(portFile);
            final DataInputStream din = new DataInputStream(in);) {
          port = din.readInt();
        } catch (final java.io.EOFException eofe) {
          eofe.printStackTrace(System.err);
          portFile.delete();
        } catch (final FileNotFoundException fnfe) {
          // Likely not running
        }
        if (port != -1) {
          try (
              final Socket s = new Socket(bindHost, port);) {
            s.setTcpNoDelay(true);
            try (
                final OutputStream out = s.getOutputStream();
                final OutputStreamWriter writer = new OutputStreamWriter(out);) {
              boolean hadPath = false;
              for (final String arg : args) {
                final String url = arg;
                if (!url.startsWith("-")) {
                  hadPath = true;
                  writer.write("LAUNCH " + arg);
                  writer.write("\r\n");
                }
              }
              // TODO: Hmm, should check
              // for a response. Some other
              // program could in theory
              // be listening on that port?
              if (!hadPath) {
                writer.write("LAUNCH_BLANK");
                writer.write("\r\n");
              }
              writer.flush();
              launched = true;
            }
          } catch (final ConnectException ce) {
            // VM must have died. We don't have logging at this point.
            PlatformInit.getInstance().initLogging(false);
            Logger.getLogger(ReuseManager.class.getName()).log(Level.WARNING,
                "Another instance of the application must have been running but was not shut down properly.\nDeleting residues of the last instance and creating a new instance.");
            portFile.delete();
          }
        }
        if (launched) {
          break OUTER;
        }
        final ReuseServer server = new ReuseServer();
        port = server.start(bindHost);
        if (!portFile.createNewFile()) {
          // Another app beat us to it.
          server.stop();
          continue OUTER;
        }

        try (
            final OutputStream out = new FileOutputStream(portFile);
            final DataOutputStream dout = new DataOutputStream(out);) {
          dout.writeInt(port);
          dout.flush();
        }
        break OUTER;
      }
    } finally {
      // long time2 = System.currentTimeMillis();
      // System.out.println("launch(): Took " + (time2 - time1) + " ms.");
    }
    if (!launched) {
      final PlatformInit entry = PlatformInit.getInstance();
      boolean debugOn = false;
      for (final String url : args) {
        if (url.equals("-debug")) {
          debugOn = true;
        }
      }
      entry.initLogging(debugOn);
      entry.init(true, !debugOn, sslInfo);
      entry.start(args);
    }
  }
}
