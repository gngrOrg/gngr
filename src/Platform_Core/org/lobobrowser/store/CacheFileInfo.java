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

import java.io.File;

/**
 * @author J. H. S.
 */
public class CacheFileInfo implements Comparable<CacheFileInfo> {
  private final long size;
  private final String path;
  private final File file;
  private final long initialLastModified;

  /**
   *
   */
  public CacheFileInfo(final File file) {
    super();
    this.path = file.getAbsolutePath();
    this.size = file.length();
    this.file = file;
    this.initialLastModified = file.lastModified();
  }

  public File getFile() {
    return this.file;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(final CacheFileInfo arg0) {
    final CacheFileInfo cfi1 = this;
    final CacheFileInfo cfi2 = arg0;
    final int fileCompare = cfi1.file.compareTo(cfi2.file);
    if (fileCompare == 0) {
      return 0;
    }
    final long lm1 = cfi1.initialLastModified;
    final long lm2 = cfi2.initialLastModified;
    if (lm1 < lm2) {
      return -1;
    } else if (lm1 > lm2) {
      return +1;
    } else {
      return fileCompare;
    }
  }

  public void delete() {
    new File(this.path).delete();
  }

  public long getLastModified() {
    return this.file.lastModified();
  }

  public long getInitialLength() {
    return this.size;
  }

  @Override
  public String toString() {
    return "CacheFileInfo[path=" + this.path + ",lastModified=" + new java.util.Date(this.initialLastModified) + "]";
  }
}
