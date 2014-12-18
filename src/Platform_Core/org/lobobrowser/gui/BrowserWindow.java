/*
    GNU GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The Lobo Project

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public
    License as published by the Free Software Foundation; either
    version 2 of the License, or (at your option) any later version.

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

/**
 * Whenever {@link FramePanel} is used, browser windows should implement this
 * interface or extend {@link AbstractBrowserWindow}. Note that
 * {@link BrowserPanel} implements this interface already, so it is not
 * necessary to have windows implement it when <code>BrowserPanel</code> is
 * used.
 */
public interface BrowserWindow {
  /**
   * Gets the root {@link FramePanel} of the window.
   */
  public FramePanel getTopFramePanel();

  /**
   * Gets a {@link WindowCallback} instance that receives navigation
   * notifications. This method may return <code>null</code>.
   */
  public WindowCallback getWindowCallback();
}
