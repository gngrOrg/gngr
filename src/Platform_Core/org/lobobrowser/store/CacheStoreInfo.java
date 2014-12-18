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
 * Created on Jun 12, 2005
 */
package org.lobobrowser.store;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author J. H. S.
 */
public class CacheStoreInfo {
  private long length;
  private final Collection<CacheFileInfo> fileInfos = new ArrayList<>();

  /**
   * @param length
   * @param fileInfos
   */
  public CacheStoreInfo() {
    super();
  }

  /**
   * @return Returns the fileInfos.
   */
  public CacheFileInfo[] getFileInfos() {
    return this.fileInfos.toArray(new CacheFileInfo[0]);
  }

  /**
   * @return Returns the length.
   */
  public long getLength() {
    return this.length;
  }

  public void addCacheFile(final java.io.File file) {
    final CacheFileInfo cfi = new CacheFileInfo(file);
    this.length += cfi.getInitialLength();
    this.fileInfos.add(cfi);
  }
}
