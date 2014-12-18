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

public class BookmarkInfo implements java.io.Serializable {
  private static final long serialVersionUID = 2257845000007000400L;

  private java.net.URL url;
  private String title;
  private String description;
  private String[] tags;

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String[] getTags() {
    return tags;
  }

  public String getTagsText() {
    final String[] tags = this.tags;
    if (tags == null) {
      return "";
    }
    final StringBuffer buffer = new StringBuffer();
    boolean firstTime = true;
    for (final String tag : tags) {
      if (firstTime) {
        firstTime = false;
      } else {
        buffer.append(' ');
      }
      buffer.append(tag);
    }
    return buffer.toString();
  }

  public void setTags(final String[] tags) {
    this.tags = tags;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(final String title) {
    this.title = title;
  }

  public java.net.URL getUrl() {
    return url;
  }

  public void setUrl(final java.net.URL url) {
    this.url = url;
  }
}
