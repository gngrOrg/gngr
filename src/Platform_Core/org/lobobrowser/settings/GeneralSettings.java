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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lobobrowser.security.GenericLocalPermission;
import org.lobobrowser.store.StorageManager;

/**
 * General browser settings. This is a singleton class with an instance obtained
 * by calling {@link #getInstance()}.
 */
public class GeneralSettings implements java.io.Serializable {
  private static final Logger logger = Logger.getLogger(GeneralSettings.class.getName());
  private static final String DEFAULT_STARTUP = "https://gngr.info";
  private static final long serialVersionUID = 22574500070000402L;
  private static final GeneralSettings instance;

  private volatile Collection<String> startupURLs;
  private volatile java.awt.Rectangle initialWindowBounds;

  static {
    GeneralSettings ins = null;
    try {
      ins = (GeneralSettings) StorageManager.getInstance().retrieveSettings(GeneralSettings.class.getSimpleName(),
          GeneralSettings.class.getClassLoader());
    } catch (final Exception err) {
      logger.log(Level.WARNING, "getInstance(): Unable to retrieve settings.", err);
    }
    if (ins == null) {
      ins = new GeneralSettings();
    }
    instance = ins;
  }

  /**
   * Gets the class singleton.
   */
  public static GeneralSettings getInstance() {
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(GenericLocalPermission.EXT_GENERIC);
    }
    return instance;
  }

  private GeneralSettings() {
    // Only called if not persisted
    this.restoreDefaults();
  }

  public void save() {
    try {
      this.saveChecked();
    } catch (final java.io.IOException ioe) {
      logger.log(Level.WARNING, "save(): Unable to save settings", ioe);
    }
  }

  public void restoreDefaults() {
    this.startupURLs = Collections.singletonList(DEFAULT_STARTUP);
  }

  public void saveChecked() throws java.io.IOException {
    StorageManager.getInstance().saveSettings(this.getClass().getSimpleName(), this);
  }

  /**
   * Gets URLs that the browser should open when it starts up.
   *
   * @see #setStartupURLs(String[])
   */
  public String[] getStartupURLs() {
    // Cannot return empty or null
    final Collection<String> urls = this.startupURLs;
    if ((urls == null) || (urls.size() == 0)) {
      return new String[] { DEFAULT_STARTUP };
    }
    return urls.toArray(new String[0]);
  }

  public void setStartupURLs(final String[] urls) {
    this.startupURLs = Arrays.asList(urls);
  }

  public java.awt.Rectangle getInitialWindowBounds() {
    final java.awt.Rectangle bounds = initialWindowBounds;
    if (bounds == null) {
      return java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    }
    if (bounds.width < 100) {
      bounds.width = 100;
    }
    if (bounds.height < 100) {
      bounds.height = 100;
    }
    return bounds;
  }

  public void setInitialWindowBounds(final java.awt.Rectangle initialWindowBounds) {
    this.initialWindowBounds = initialWindowBounds;
  }
}
