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

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box.Filler;

/**
 * A component used in conjunction with <code>BoxLayot</code>.
 *
 */
public class FillerComponent extends Filler {
  private static final long serialVersionUID = -6482494343603168295L;

  public FillerComponent(final Component wrappedComponent, final boolean forMax) {
    super(new Dimension(0, 0), forMax ? new Dimension(0, 0) : new Dimension(Short.MAX_VALUE, Short.MAX_VALUE), new Dimension(
        Short.MAX_VALUE, Short.MAX_VALUE));
    this.setLayout(org.lobobrowser.util.gui.WrapperLayout.getInstance());
    this.add(wrappedComponent);
  }

  public FillerComponent(final Component wrappedComponent, final Dimension minSize, final Dimension prefSize, final Dimension maxSize) {
    super(minSize, prefSize, maxSize);
    this.setLayout(org.lobobrowser.util.gui.WrapperLayout.getInstance());
    this.add(wrappedComponent);
  }

}
