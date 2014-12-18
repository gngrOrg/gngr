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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import org.lobobrowser.util.io.IORoutines;

public class IconFactory {
  private static final Logger logger = Logger.getLogger(IconFactory.class.getName());
  private static final IconFactory instance = new IconFactory();

  private IconFactory() {
  }

  public static IconFactory getInstance() {
    return instance;
  }

  private final Map<String, ImageIcon> iconMap = new HashMap<>();

  public ImageIcon getIcon(final String resourcePath) {
    synchronized (this) {
      ImageIcon icon = this.iconMap.get(resourcePath);
      if (icon == null) {
        // TODO: final URL url = this.getClass().getResource(resourcePath);
        final InputStream stream = this.getClass().getResourceAsStream(resourcePath);
        if (stream == null) {
          logger.log(Level.WARNING, "getIcon(): Resource path " + resourcePath + " gave error.");
        } else {
          try {
            final byte[] bytes = IORoutines.load(stream);
            icon = new ImageIcon(bytes);
          } catch (final IOException e) {
            throw new RuntimeException(e);
          }
        }
      }
      return icon;
    }
  }
}
