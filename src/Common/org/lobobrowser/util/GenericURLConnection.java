/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The Lobo Project

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

    Contact info: lobochief@users.sourceforge.net
 */
/*
 * Created on Jun 22, 2005
 */
package org.lobobrowser.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

/**
 * @author J. H. S.
 */
public class GenericURLConnection extends URLConnection {
  private final java.io.InputStream inputStream;

  /**
   *
   */
  public GenericURLConnection(final java.net.URL url, final InputStream in) {
    super(url);
    this.inputStream = in;
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
    return this.inputStream;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.net.URLConnection#getHeaderField(int)
   */
  @Override
  public String getHeaderField(final int n) {
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.net.URLConnection#getHeaderField(java.lang.String)
   */
  @Override
  public String getHeaderField(final String name) {
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.net.URLConnection#getHeaderFieldKey(int)
   */
  @Override
  public String getHeaderFieldKey(final int n) {
    return null;
  }

}
