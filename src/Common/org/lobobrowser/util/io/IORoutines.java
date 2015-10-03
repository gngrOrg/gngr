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
 * Created on Mar 19, 2005
 */
package org.lobobrowser.util.io;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * @author J. H. S.
 */
public class IORoutines {
  public static final byte[] LINE_BREAK_BYTES = { (byte) 13, (byte) 10 };

  public static String loadAsText(final InputStream in, final String encoding) throws IOException {
    return loadAsText(in, encoding, 4096);
  }

  public static String loadAsText(final InputStream in, final String encoding, final int bufferSize) throws IOException {
    final InputStreamReader reader = new InputStreamReader(in, encoding);
    char[] buffer = new char[bufferSize];
    int offset = 0;
    for (;;) {
      int remain = buffer.length - offset;
      if (remain <= 0) {
        final char[] newBuffer = new char[buffer.length * 2];
        System.arraycopy(buffer, 0, newBuffer, 0, offset);
        buffer = newBuffer;
        remain = buffer.length - offset;
      }
      final int numRead = reader.read(buffer, offset, remain);
      if (numRead == -1) {
        break;
      }
      offset += numRead;
    }
    return new String(buffer, 0, offset);
  }

  public static byte[] load(final File file) throws IOException {
    final long fileLength = file.length();
    if (fileLength > Integer.MAX_VALUE) {
      throw new IOException("File '" + file.getName() + "' too big");
    }
    try (
      final InputStream in = new FileInputStream(file)) {
      return loadExact(in, (int) fileLength);
    }
  }

  public static byte[] load(final InputStream in) throws IOException {
    return load(in, 4096);
  }

  public static byte[] load(final InputStream in, int initialBufferSize) throws IOException {
    if (initialBufferSize == 0) {
      initialBufferSize = 1;
    }
    byte[] buffer = new byte[initialBufferSize];
    int offset = 0;
    for (;;) {
      int remain = buffer.length - offset;
      if (remain <= 0) {
        final int newSize = buffer.length * 2;
        final byte[] newBuffer = new byte[newSize];
        System.arraycopy(buffer, 0, newBuffer, 0, offset);
        buffer = newBuffer;
        remain = buffer.length - offset;
      }
      final int numRead = in.read(buffer, offset, remain);
      if (numRead == -1) {
        break;
      }
      offset += numRead;
    }
    if (offset < buffer.length) {
      final byte[] newBuffer = new byte[offset];
      System.arraycopy(buffer, 0, newBuffer, 0, offset);
      buffer = newBuffer;
    }
    return buffer;
  }

  public static byte[] loadExact(final InputStream in, final int length) throws IOException {
    final byte[] buffer = new byte[length];
    int offset = 0;
    for (;;) {
      final int remain = length - offset;
      if (remain <= 0) {
        break;
      }
      final int numRead = in.read(buffer, offset, remain);
      if (numRead == -1) {
        throw new IOException("Reached EOF, read " + offset + " expecting " + length);
      }
      offset += numRead;
    }
    return buffer;
  }

  public static boolean equalContent(final File file, final byte[] content) throws IOException {
    final long length = file.length();
    if (length > Integer.MAX_VALUE) {
      throw new IOException("File '" + file + "' too big");
    }

    try (
      final InputStream in = new FileInputStream(file);) {
      final byte[] fileContent = loadExact(in, (int) length);
      return java.util.Arrays.equals(content, fileContent);
    }
  }

  public static void save(final File file, final byte[] content) throws IOException {
    try (
      final FileOutputStream out = new FileOutputStream(file);) {
      out.write(content);
    }
  }

  /**
   * Reads line without buffering.
   */
  public static String readLine(final InputStream in) throws IOException {
    int b;
    StringBuffer sb = null;
    OUTER: while ((b = in.read()) != -1) {
      if (sb == null) {
        sb = new StringBuffer();
      }
      switch (b) {
      case (byte) '\n':
        break OUTER;
      case (byte) '\r':
        break;
      default:
        sb.append((char) b);
        break;
      }
    }
    return sb == null ? null : sb.toString();
  }

  public static void touch(final File file) {
    file.setLastModified(System.currentTimeMillis());
  }

  public static void saveStrings(final File file, final Collection<String> list) throws IOException {
    try (
      final FileOutputStream fout = new FileOutputStream(file);
      final BufferedOutputStream bout = new BufferedOutputStream(fout);
      final PrintWriter writer = new PrintWriter(bout)) {
      list.forEach(text -> writer.println(text));
      writer.flush();
    }
  }

  public static java.util.List<String> loadStrings(final File file) throws IOException {
    try (
      final InputStream in = new FileInputStream(file);
      final BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
      return reader.lines().collect(Collectors.toList());
    }
  }

  public static InputStream getDecodedStream(final URLConnection connection) throws IOException {
    final InputStream cis = connection.getInputStream();
    if ("gzip".equals(connection.getContentEncoding())) {
      return new GZIPInputStream(cis);
    } else if ("deflate".equals(connection.getContentEncoding())) {
      return new InflaterInputStream(cis);
    } else {
      return cis;
    }
  }

  public static InputStream getDecodedErrorStream(final HttpURLConnection connection) throws IOException {
    final InputStream cis = connection.getErrorStream();
    if (cis != null) {
      if ("gzip".equals(connection.getContentEncoding())) {
        return new GZIPInputStream(cis);
      } else if ("deflate".equals(connection.getContentEncoding())) {
        return new InflaterInputStream(cis);
      } else {
        return cis;
      }
    } else {
      return null;
    }
  }

}
