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
package org.lobobrowser.primary.ext;

import java.net.URL;

public class HistoryEntry<T> {
  private final java.net.URL url;
  private final long timetstamp;
  private final T itemInfo;

  public HistoryEntry(final URL url, final long timetstamp, final T itemInfo) {
    super();
    this.url = url;
    this.timetstamp = timetstamp;
    this.itemInfo = itemInfo;
  }

  public T getItemInfo() {
    return itemInfo;
  }

  public long getTimetstamp() {
    return timetstamp;
  }

  public java.net.URL getUrl() {
    return url;
  }
}
