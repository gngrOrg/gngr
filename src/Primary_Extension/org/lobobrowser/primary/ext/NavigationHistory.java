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
 * Created on Jun 6, 2005
 */
package org.lobobrowser.primary.ext;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.lobobrowser.store.StorageManager;

/**
 * History of navigation locations. Not thread safe.
 */
public class NavigationHistory extends BaseHistory<Object> implements java.io.Serializable {
  private static final long serialVersionUID = 2257845000600200100L;
  private static final Logger logger = Logger.getLogger(NavigationHistory.class.getName());
  private static final NavigationHistory instance;

  static {
    NavigationHistory ins = null;
    try {
      ins = (NavigationHistory) StorageManager.getInstance().retrieveSettings(NavigationHistory.class.getSimpleName(),
          NavigationHistory.class.getClassLoader());
    } catch (final Exception err) {
      logger.log(Level.WARNING, "Unable to retrieve settings.", err);
    }
    if (ins == null) {
      ins = new NavigationHistory();
    }
    instance = ins;
  }

  /**
   * @param sequenceCapacity
   * @param commonEntriesCapacity
   */
  private NavigationHistory() {
  }

  public static NavigationHistory getInstance() {
    return instance;
  }

  public void save() {
    synchronized (this) {
      try {
        StorageManager.getInstance().saveSettings(this.getClass().getSimpleName(), this);
      } catch (final java.io.IOException ioe) {
        logger.log(Level.WARNING, "Unable to save settings: " + this.getClass().getSimpleName(), ioe);
      }
    }
  }
}
