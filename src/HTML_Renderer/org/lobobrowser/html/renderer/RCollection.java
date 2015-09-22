/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The Lobo Project

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Contact info: lobochief@users.sourceforge.net
 */

package org.lobobrowser.html.renderer;

import java.awt.Rectangle;
import java.util.Iterator;

import org.eclipse.jdt.annotation.NonNull;

/**
 * A {@link Renderable} with children.
 */
public interface RCollection extends BoundableRenderable {
  /**
   * Gets the collection of {@link Renderable} children.
   * @param topFirst If true, then the renderable that is visually on top comes first in the iterator.
   *                 Currently, topFirst=true is worse for performance, as it requires reversing.
   */
  public Iterator<@NonNull ? extends Renderable> getRenderables(final boolean topFirst);

  /**
   * Gets the collection of {@link Renderable} children in unspecified order.
   * Call this variant when the order of the result doesn't matter.
   * The order defaults to the order that is natural to the implementation.
   */
  default public Iterator<@NonNull ? extends Renderable> getRenderables() {
    return getRenderables(false);
  }

  public void updateWidgetBounds(int guiX, int guiY);

  /**
   * Invalidates layout in all descendents.
   */
  public void invalidateLayoutDeep();

  public void focus();

  public void blur();

  public BoundableRenderable getRenderable(final int x, final int y);

  public Rectangle getClipBounds();
  public Rectangle getClipBoundsWithoutInsets();
}
