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
package org.lobobrowser.request;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MultipartFormDataWriter {
  private static final byte[] LINE_BREAK_BYTES;
  private final OutputStream out;
  private final String boundary;

  static {
    LINE_BREAK_BYTES = "\r\n".getBytes();
  }

  public MultipartFormDataWriter(final OutputStream out, final String boundary) {
    super();
    this.out = out;
    this.boundary = boundary;
  }

  /**
   *
   * @param name
   * @param contentType
   * @param in
   *          Data stream. The caller is responsible for closing it.
   */
  public final void writeFileData(final String name, final String fileName, final String contentType, final InputStream in)
      throws java.io.IOException {
    final String headers = "--" + this.boundary + "\r\n" + "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName
        + "\"\r\n" + "Content-Type: " + contentType + "\r\n" + "\r\n";
    final OutputStream out = this.out;
    out.write(headers.getBytes("ISO-8859-1"));
    final byte[] buffer = new byte[4096];
    int numRead;
    while ((numRead = in.read(buffer)) != -1) {
      out.write(buffer, 0, numRead);
    }
    out.write(LINE_BREAK_BYTES);
  }

  public final void writeText(final String name, final String value, final String charset) throws IOException {
    final String headers = "--" + this.boundary + "\r\n" + "Content-Disposition: form-data; name=\"" + name + "\"\r\n"
        + "Content-Type: text/plain; charset=\"" + charset + "\"\r\n" + "\r\n";
    final OutputStream out = this.out;
    out.write(headers.getBytes("ISO-8859-1"));
    out.write(value.getBytes(charset));
    out.write(LINE_BREAK_BYTES);
  }

  public final void send() throws java.io.IOException {
    final String finalDelimiter = "--" + this.boundary + "--\r\n";
    final OutputStream out = this.out;
    out.write(finalDelimiter.getBytes("ISO-8859-1"));
    out.flush();
  }
}
