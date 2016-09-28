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

import java.awt.Graphics;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JProgressBar;

public class OpenProgressBar extends JProgressBar {
  private static final long serialVersionUID = -4070820733226115064L;
  private static final Logger logger = Logger.getLogger(OpenProgressBar.class.getName());

  @Override
  public void paintComponent(final Graphics g) {
    try {
      super.paintComponent(g);
    } catch (final Exception err) {
      logger.log(Level.SEVERE, "paintComponent(): Swing bug?", err);
    }
  }
}
