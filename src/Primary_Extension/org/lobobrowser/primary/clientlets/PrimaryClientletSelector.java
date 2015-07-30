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

    Contact info: info@xamjwg.org
 */
/*
 * Created on Sep 18, 2005
 */
package org.lobobrowser.primary.clientlets;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.lobobrowser.clientlet.Clientlet;
import org.lobobrowser.clientlet.ClientletRequest;
import org.lobobrowser.clientlet.ClientletResponse;
import org.lobobrowser.clientlet.ClientletSelector;
import org.lobobrowser.primary.clientlets.download.DownloadClientlet;
import org.lobobrowser.primary.clientlets.html.HtmlClientlet;
import org.lobobrowser.primary.clientlets.img.ImageClientlet;

public class PrimaryClientletSelector implements ClientletSelector {
  private static final Logger logger = Logger.getLogger(PrimaryClientletSelector.class.getName());

  public PrimaryClientletSelector() {
    super();
  }

  public Clientlet select(final ClientletRequest request, final ClientletResponse response) {
    // Don't try to catch too much here.
    // Clientlets here are not overriddable.

    final String mimeType = response.getMimeType();
    if (logger.isLoggable(Level.INFO)) {
      logger.info("select(): mimeType=" + mimeType);
    }
    final String mimeTypeTL = mimeType == null ? null : mimeType.toLowerCase();
    if ("text/html".equals(mimeTypeTL) || "image/svg+xml".equals(mimeTypeTL) || "application/xhtml+xml".equals(mimeTypeTL) || "application/xml".equals(mimeTypeTL)) {
      // TODO: XHTML needs its own clientlet.
      return new HtmlClientlet();
    } else if ("image/jpeg".equals(mimeTypeTL) || "image/jpg".equals(mimeTypeTL) || "image/gif".equals(mimeTypeTL)
        || "image/png".equals(mimeTypeTL) || "image/bmp".equals(mimeTypeTL) || "image/x-ms-bmp".equals(mimeTypeTL)) {
      return new ImageClientlet();
    } else if ((mimeType == null) || "application/octet-stream".equals(mimeTypeTL) || "content/unknown".equals(mimeTypeTL)) {

      final String path = response.getResponseURL().getPath();
      final int lastDotIdx = path.lastIndexOf('.');
      final String extension = lastDotIdx == -1 ? "" : path.substring(lastDotIdx + 1);
      final String extensionTL = extension.toLowerCase();
      if ("html".equals(extensionTL) || "htm".equals(extensionTL) || (extensionTL.length() == 0)) {
        return new HtmlClientlet();
      } else if ("gif".equals(extensionTL) || "jpg".equals(extensionTL) || "png".equals(extensionTL)) {
        return new ImageClientlet();
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  public Clientlet lastResortSelect(final ClientletRequest request, final ClientletResponse response) {
    final String mimeType = response.getMimeType();
    final String mimeTypeTL = mimeType == null ? null : mimeType.toLowerCase();
    if ((mimeTypeTL != null) && mimeTypeTL.startsWith("text/")) {
      return new TextClientlet();
    } else if ("application/xhtml+xml".equals(mimeTypeTL)) {
      // TODO: XHTML needs its own clientlet.
      return new HtmlClientlet();
    } else if ("application/json".equals(mimeTypeTL)) {
      // TODO: JSON needs its own clientlet.
      return new TextClientlet();
    } else {
      final String path = response.getResponseURL().getPath();
      final int lastDotIdx = path.lastIndexOf('.');
      final String extension = lastDotIdx == -1 ? "" : path.substring(lastDotIdx + 1);
      final String extensionTL = extension.toLowerCase();
      if ("xhtml".equals(extensionTL)) {
        return new HtmlClientlet();
      } else if ("txt".equals(extensionTL) || "xml".equals(extensionTL) || "svg".equals(extensionTL) || "rss".equals(extensionTL)
          || "xaml".equals(extensionTL)) {
        return new TextClientlet();
      } else if (mimeType == null) {
        // If mime-type is completely missing,
        // disregard extension and assume HTML.
        // Works for
        // DLink router authentication page.
        return new HtmlClientlet();
      } else {
        return new DownloadClientlet();
      }
    }
  }
}
