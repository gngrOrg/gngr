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
 * Created on Mar 12, 2005
 */
package org.lobobrowser.main;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.lobobrowser.main.TrustManager.SSLInfo;

/**
 * Entry point class of the browser application.
 */
public final class EntryPoint {
  /**
   * Launches a browser window. If a browser instance is found to already exist,
   * the new browser window is opened in the running application.
   * <p>
   * Note: To run without an "ext" directory (e.g. from the project source code
   * in Eclipse) you need to set up the "ext.dirs" and/or "ext.files"
   * properties.
   */
  public static void main(final String[] args) {
    // Checking for stack allows us to call AccessController.doPrivileged()
    // which in turn allows us to reduce the permissions on Uno codesource
    final int stackDepth = Thread.currentThread().getStackTrace().length;
    if (stackDepth > 11) {
      System.err.println("Stack depth (" + stackDepth + ") is too deep! Quitting as a safety precaution");
      Thread.dumpStack();
      System.exit(1);
    } else {
      privilegedLaunch(args);
    }
  }

  private static void launch(final String[] args) {
    try {
      final SSLInfo sslInfo = TrustManager.makeSSLSocketFactory(ReuseManager.class.getResourceAsStream("/trustStore.certs"));
      ReuseManager.getInstance().launch(args, sslInfo);
    } catch (final Exception err) {
      final StringWriter swriter = new StringWriter();
      final PrintWriter writer = new PrintWriter(swriter);
      err.printStackTrace(writer);
      writer.flush();
      JOptionPane.showMessageDialog(new JFrame(),
          "An unexpected error occurred during application startup:\r\n" + swriter.toString(),
          "ERROR", JOptionPane.ERROR_MESSAGE);
      System.err.println(swriter.toString());
      System.exit(1);
    }
  }

  private static void privilegedLaunch(final String[] args) {
    AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
      launch(args);
      return null;
    });
  }
}
