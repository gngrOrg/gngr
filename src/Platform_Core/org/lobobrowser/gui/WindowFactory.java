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
package org.lobobrowser.gui;

import java.util.Properties;

import org.lobobrowser.ua.NavigatorWindow;

/**
 * Factory used to create browser windows. It is used when the JavaScript method
 * Window.open() is invoked, for example.
 *
 * @see NavigatorWindowImpl#setWindowFactory(WindowFactory)
 */
public interface WindowFactory {
  /**
   * Creates a new navigator window, even if the windowId provided exists. The
   * implementation of this method is expected to add the top frame of the
   * window context to the new navigator window.
   *
   * @param windowId
   *          The window ID. It may be <code>null</code>.
   * @param windowProperties
   *          Window properties, following Window.open() conventions. In
   *          addition, properties <code>title</code> and <code>icon</code>
   *          should be supported.
   * @see DefaultBrowserWindow
   */
  public AbstractBrowserWindow createWindow(String windowId, Properties windowProperties, NavigatorWindow windowContext);

  /**
   * Gets an existing window given a windowId.
   */
  public AbstractBrowserWindow getExistingWindow(String windowId);

  public void overrideProperties(AbstractBrowserWindow window, Properties properties);
}
