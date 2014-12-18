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
 * This class allows trapping the creation of {@link FramePanel} instances. This
 * is useful, for example, if you need to add a listener to every
 * <code>FramePanel</code> that is created (e.g. for IFRAMEs).
 */
public class FramePanelFactorySource {
  private static final FramePanelFactorySource instance = new FramePanelFactorySource();
  private volatile FramePanelFactory activeFactory = new DefaultFramePanelFactory();

  public static FramePanelFactorySource getInstance() {
    return instance;
  }

  public FramePanelFactory getActiveFactory() {
    return activeFactory;
  }

  /**
   * Sets a new {@link FramePanelFactory} that is used to create
   * <code>FramePanel</code>s as requested by browser content (e.g. IFRAMEs in
   * HTML).
   *
   * @param activeFactory
   */
  public void setActiveFactory(final FramePanelFactory activeFactory) {
    if (activeFactory == null) {
      throw new IllegalArgumentException("activeFactory==null");
    }
    this.activeFactory = activeFactory;
  }
}
