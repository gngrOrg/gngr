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
 * Created on Nov 13, 2005
 */
package org.lobobrowser.html.io;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;

public class WritableLineReader extends LineNumberReader {
  private final Reader delegate;

  public WritableLineReader(final Reader reader, final int bufferSize) {
    super(reader, bufferSize);
    this.delegate = reader;
  }

  public WritableLineReader(final Reader reader) {
    super(reader);
    this.delegate = reader;
  }

  /*
   * Note: Not implicitly thread safe.
   */
  @Override
  public int read() throws IOException {
    final StringBuffer sb = this.writeBuffer;
    if ((sb != null) && (sb.length() > 0)) {
      final char ch = sb.charAt(0);
      sb.deleteCharAt(0);
      if (sb.length() == 0) {
        this.writeBuffer = null;
      }
      return ch;
    }
    return super.read();
  }

  /*
   * (non-Javadoc) Note: Not implicitly thread safe.
   *
   * @see java.io.Reader#read(byte[], int, int)
   */
  @Override
  public int read(final char[] b, final int off, final int len) throws IOException {
    final StringBuffer sb = this.writeBuffer;
    if ((sb != null) && (sb.length() > 0)) {
      final int srcEnd = Math.min(sb.length(), len);
      sb.getChars(0, srcEnd, b, off);
      sb.delete(0, srcEnd);
      if (sb.length() == 0) {
        this.writeBuffer = null;
      }
      return srcEnd;
    }
    return super.read(b, off, len);
  }

  @Override
  public boolean ready() throws IOException {
    final StringBuffer sb = this.writeBuffer;
    if ((sb != null) && (sb.length() > 0)) {
      return true;
    }
    return super.ready();
  }

  /*
   * (non-Javadoc) Note: Not implicitly thread safe.
   *
   * @see java.io.Reader#close()
   */
  @Override
  public void close() throws IOException {
    this.writeBuffer = null;
    super.close();
  }

  private StringBuffer writeBuffer = null;

  /**
   * Note: Not implicitly thread safe.
   *
   * @param text
   * @throws IOException
   */
  public void write(final String text) throws IOException {
    // Document overrides this to know that new data is coming.
    StringBuffer sb = this.writeBuffer;
    if (sb == null) {
      sb = new StringBuffer();
      this.writeBuffer = sb;
    }
    sb.append(text);
  }
}
