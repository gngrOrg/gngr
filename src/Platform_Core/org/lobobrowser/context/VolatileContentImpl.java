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
 * Created on May 21, 2005
 */
package org.lobobrowser.context;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.lobobrowser.clientlet.ContentBuffer;
import org.lobobrowser.util.ID;
import org.lobobrowser.util.WeakValueHashMap;

/**
 * @author J. H. S.
 */
public class VolatileContentImpl implements ContentBuffer {
  private static final Map<Long, ContentBuffer> volatileMap = new WeakValueHashMap<>();
  private final long id;
  private final String contentType;
  private final byte[] bytes;

  /**
   *
   */
  public VolatileContentImpl(final String contentType, final byte[] bytes) {
    super();
    this.id = ID.generateLong();
    this.contentType = contentType;
    this.bytes = bytes;
    synchronized (volatileMap) {
      volatileMap.put(new Long(this.id), this);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.clientlet.VolatileContent#getURL()
   */
  public URL getURL() {
    try {
      return new URL("vc:" + this.id);
    } catch (final MalformedURLException mfu) {
      throw new IllegalStateException(mfu);
    }
  }

  public static VolatileContentImpl getVolatileContent(final long id) {
    synchronized (volatileMap) {
      return (VolatileContentImpl) volatileMap.get(new Long(id));
    }
  }

  /**
   * @return Returns the bytes.
   */
  public byte[] getBytes() {
    return bytes;
  }

  /**
   * @return Returns the contentType.
   */
  public String getContentType() {
    return contentType;
  }
}
