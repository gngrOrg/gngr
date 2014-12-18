/*
    GNU GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The Lobo Project

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public
    License as published by the Free Software Foundation; either
    version 2 of the License, or (at your option) any later version.

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.lobobrowser.util.NameValuePair;

public class MemoryURLConnection extends URLConnection {
  private final MemoryCacheEntry memoryEntry;

  public MemoryURLConnection(final URL url, final MemoryCacheEntry memoryEntry) {
    super(url);
    this.memoryEntry = memoryEntry;
  }

  private InputStream inputStream;
  private final Map<String, List<String>> headersMap = new HashMap<>();

  @Override
  public void connect() throws IOException {
    if (!this.connected) {
      this.readHeaders();
      final InputStream in = new ByteArrayInputStream(this.memoryEntry.content);
      this.inputStream = in;
      this.connected = true;
    }
  }

  private void readHeaders() throws IOException {
    final Map<String, List<String>> headersMap = this.headersMap;
    final List<NameValuePair> origList = this.memoryEntry.headers;
    final Iterator<NameValuePair> i = origList.iterator();
    while (i.hasNext()) {
      final NameValuePair pair = i.next();
      String name = pair.name;
      if (name != null) {
        name = name.toLowerCase();
      }
      final String value = pair.value;
      List<String> hvalues = headersMap.get(name);
      if (hvalues == null) {
        hvalues = new ArrayList<>(1);
        headersMap.put(name, hvalues);
      }
      hvalues.add(value);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.net.URLConnection#getHeaderField(int)
   */
  @Override
  public String getHeaderField(final int n) {
    try {
      this.connect();
      final NameValuePair pair = this.memoryEntry.headers.get(n);
      return pair.value;
    } catch (final IndexOutOfBoundsException iob) {
      return null;
    } catch (final IOException ioe) {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.net.URLConnection#getHeaderField(java.lang.String)
   */
  @Override
  public String getHeaderField(final String name) {
    try {
      this.connect();
    } catch (final IOException ioe) {
      return null;
    }
    final List<String> hvalues = this.headersMap.get(name.toLowerCase());
    if ((hvalues == null) || (hvalues.size() == 0)) {
      return null;
    }
    return hvalues.get(0);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.net.URLConnection#getHeaderFieldKey(int)
   */
  @Override
  public String getHeaderFieldKey(final int n) {
    try {
      this.connect();
      final NameValuePair pair = this.memoryEntry.headers.get(n);
      return pair.name;
    } catch (final IndexOutOfBoundsException iob) {
      return null;
    } catch (final IOException ioe) {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.net.URLConnection#getHeaderFields()
   */
  @Override
  public Map<String, List<String>> getHeaderFields() {
    try {
      this.connect();
    } catch (final IOException ioe) {
      return null;
    }
    return this.headersMap;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    this.connect();
    return this.inputStream;
  }
}
