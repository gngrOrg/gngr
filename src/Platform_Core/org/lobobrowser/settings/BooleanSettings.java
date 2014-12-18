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

/**
 * Miscellaneous settings in the form of boolean values. This is a singleton
 * class with an instance obtained by calling {@link #getInstance()}.
 */
public class BooleanSettings implements Serializable {
  private static final Logger logger = Logger.getLogger(BooleanSettings.class.getName());
  private static final BooleanSettings instance;
  private static final long serialVersionUID = 22574500900000604L;

  /**
   * Whether chunked encoding is used in POSTs. Note that some servers (e.g.
   * Wikimedia) apparently expect a content length.
   */
  private boolean httpUseChunkedEncodingPOST;

  static {
    BooleanSettings ins = null;
    try {
      ins = (BooleanSettings) StorageManager.getInstance().retrieveSettings(BooleanSettings.class.getSimpleName(),
          BooleanSettings.class.getClassLoader());
    } catch (final Exception err) {
      logger.log(Level.WARNING, "Unable to retrieve settings.", err);
    }
    if (ins == null) {
      ins = new BooleanSettings();
    }
    instance = ins;
  }

  private BooleanSettings() {
    this.resetDefaults();
  }

  private void resetDefaults() {
    this.httpUseChunkedEncodingPOST = false;
  }

  /**
   * Gets the class singleton.
   */
  public static BooleanSettings getInstance() {
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(GenericLocalPermission.EXT_GENERIC);
    }
    return instance;
  }

  public void save() {
    try {
      StorageManager.getInstance().saveSettings(this.getClass().getSimpleName(), this);
    } catch (final java.io.IOException ioe) {
      logger.log(Level.WARNING, "Unable to save settings: " + this.getClass().getSimpleName(), ioe);
    }
  }

  public boolean isHttpUseChunkedEncodingPOST() {
    return httpUseChunkedEncodingPOST;
  }

  public void setHttpUseChunkedEncodingPOST(final boolean httpUseChunkedEncodingPOST) {
    this.httpUseChunkedEncodingPOST = httpUseChunkedEncodingPOST;
  }
}
