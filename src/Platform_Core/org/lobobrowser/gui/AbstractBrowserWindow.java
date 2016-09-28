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

import javax.swing.JFrame;

/**
 * Browser windows should extend this class.
 */
public abstract class AbstractBrowserWindow extends JFrame implements BrowserWindow {
  private static final long serialVersionUID = 3803551200903041444L;

  /**
   * Gets the root {@link FramePanel} of the window.
   */
  public abstract FramePanel getTopFramePanel();

  /**
   * Gets a {@link WindowCallback} instance that receives navigation
   * notifications. This method may return <code>null</code>.
   */
  public abstract WindowCallback getWindowCallback();

  private boolean boundsAssigned;

  /**
   * Returns true if window bounds were assigned via properties.
   */
  public boolean isBoundsAssigned() {
    return boundsAssigned;
  }

  /**
   * Sets whether window bounds were assigned via properties.
   *
   * @param boundsAssigned
   *          True if bounds were assigned.
   */
  public void setBoundsAssigned(final boolean boundsAssigned) {
    this.boundsAssigned = boundsAssigned;
  }
}
