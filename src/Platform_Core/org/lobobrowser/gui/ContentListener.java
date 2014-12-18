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

import java.util.EventListener;

/**
 * A listener of content events.
 *
 * @see FramePanel#addContentListener(ContentListener)
 * @see FramePanel#getComponentContent()
 */
public interface ContentListener extends EventListener {
  static final ContentListener[] EMPTY_ARRAY = new ContentListener[0];

  /**
   * Called as soon as the content has been set in a {@link FramePanel}. Note
   * that content can be set before the originating document has been fully
   * loaded, for example when incremental rendering is performed.
   *
   * @param event
   *          The content event.
   */
  public void contentSet(ContentEvent event);
}
