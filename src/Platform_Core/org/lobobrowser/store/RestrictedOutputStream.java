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
 * Created on Jun 1, 2005
 */
package org.lobobrowser.store;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author J. H. S.
 */
public class RestrictedOutputStream extends OutputStream {
  private final OutputStream out;
  private final QuotaSource quotaSource;

  /**
   *
   */
  public RestrictedOutputStream(final OutputStream out, final QuotaSource quotaSource) throws IOException {
    this.out = out;
    this.quotaSource = quotaSource;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.io.OutputStream#write(int)
   */
  @Override
  public void write(final int b) throws IOException {
    this.quotaSource.addUsedBytes(1);
    this.out.write(b);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.io.OutputStream#close()
   */
  @Override
  public void close() throws IOException {
    this.out.close();
  }

  /*
   * (non-Javadoc)
   *
   * @see java.io.OutputStream#flush()
   */
  @Override
  public void flush() throws IOException {
    this.out.flush();
  }

  /*
   * (non-Javadoc)
   *
   * @see java.io.OutputStream#write(byte[], int, int)
   */
  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException {
    this.quotaSource.addUsedBytes(len);
    this.out.write(b, off, len);
  }
}
