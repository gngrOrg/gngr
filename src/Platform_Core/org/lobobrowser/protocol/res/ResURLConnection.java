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
 * Created on Mar 14, 2005
 */
package org.lobobrowser.protocol.res;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author J. H. S.
 */
public class ResURLConnection extends URLConnection {
  public ResURLConnection(final URL url) {
    super(url);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.net.URLConnection#connect()
   */
  @Override
  public void connect() throws IOException {
  }

  /*
   * (non-Javadoc)
   *
   * @see java.net.URLConnection#getInputStream()
   */
  @Override
  public InputStream getInputStream() throws IOException {
    final String host = this.url.getHost();
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    if (classLoader == null) {
      classLoader = this.getClass().getClassLoader();
    }
    String file = this.url.getPath();
    InputStream in = classLoader.getResourceAsStream(file);
    if (in == null) {
      if (file.startsWith("/")) {
        file = file.substring(1);
        in = classLoader.getResourceAsStream(file);
        if (in == null) {
          throw new IOException("Resource " + file + " not found in " + host + ".");
        }
      }
    }
    return in;
  }
}
