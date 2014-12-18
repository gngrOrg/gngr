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
package org.lobobrowser.settings;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lobobrowser.security.GenericLocalPermission;
import org.lobobrowser.store.StorageManager;

class CookieSettings implements Serializable {
  private static final Logger logger = Logger.getLogger(CookieSettings.class.getName());
  private static final CookieSettings instance;
  private static final long serialVersionUID = 22574500005000503L;

  static {
    CookieSettings ins = null;
    try {
      ins = (CookieSettings) StorageManager.getInstance().retrieveSettings(CookieSettings.class.getSimpleName(),
          CookieSettings.class.getClassLoader());
    } catch (final Exception err) {
      logger.log(Level.WARNING, "getInstance(): Unable to retrieve settings.", err);
    }
    if (ins == null) {
      ins = new CookieSettings();
    }
    instance = ins;
  }

  private CookieSettings() {
  }

  /**
   * Gets the class singleton.
   */
  public static CookieSettings getInstance() {
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(GenericLocalPermission.EXT_GENERIC);
    }
    return instance;
  }
}
