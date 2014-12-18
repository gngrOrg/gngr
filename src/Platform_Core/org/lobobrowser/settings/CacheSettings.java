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
import org.lobobrowser.store.CacheManager;
import org.lobobrowser.store.StorageManager;

/**
 * Browser cache settings. This is a singleton class with an instance obtained
 * by calling {@link #getInstance()}.
 */
public class CacheSettings implements Serializable {
  private static final Logger logger = Logger.getLogger(CacheSettings.class.getName());
  private static final CacheSettings instance;
  private static final long serialVersionUID = 22574500900000604L;

  static {
    CacheSettings ins = null;
    try {
      ins = (CacheSettings) StorageManager.getInstance().retrieveSettings(CacheSettings.class.getSimpleName(),
          CacheSettings.class.getClassLoader());
    } catch (final Exception err) {
      logger.log(Level.WARNING, "getInstance(): Unable to retrieve settings.", err);
    }
    if (ins == null) {
      ins = new CacheSettings();
    }
    instance = ins;
  }

  /**
   * Gets the class singleton.
   */
  public static CacheSettings getInstance() {
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
      logger.log(Level.WARNING, "Unable to save settings: " + this.getClass().getSimpleName() + ".", ioe);
    }
  }

  private CacheSettings() {
    resetDefaults();
  }

  public void resetDefaults() {
    this.setMaxRAMCacheSize(5 * 1024 * 1024);
    this.setDefaultCacheExpirationOffset(60);
  }

  public int getMaxRAMCacheSize() {
    return CacheManager.getInstance().getMaxTransientCacheSize();
  }

  /**
   * Sets the approximate maximum RAM cache size.
   *
   * @param maxRAMCacheSize
   *          The maximum cache size in bytes.
   */
  public void setMaxRAMCacheSize(final int maxRAMCacheSize) {
    CacheManager.getInstance().setMaxTransientCacheSize(maxRAMCacheSize);
  }

  private int defaultCacheExpirationOffset;

  public int getDefaultCacheExpirationOffset() {
    return defaultCacheExpirationOffset;
  }

  /**
   * Sets the default offset in seconds added to the response cache timestamp to
   * get the expiration time of a document. This is used with cacheable
   * documents when max-age and the Expires header are missing.
   */
  public void setDefaultCacheExpirationOffset(final int defaultCacheExpirationOffset) {
    this.defaultCacheExpirationOffset = defaultCacheExpirationOffset;
  }
}
