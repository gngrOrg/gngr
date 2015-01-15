/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2015 Uproot Labs India Pvt Ltd

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

 */

package org.lobobrowser.html.renderer;

import java.awt.FontMetrics;

import org.lobobrowser.html.domimpl.ModelNode;

public class RTab extends RWord {

  public RTab(final ModelNode me, final RenderableContainer container, final FontMetrics fontMetrics, final int descent, final int ascentPlusLeading,
      final int height, final int numSpaces) {
    super(me, "\t", container, fontMetrics, descent, ascentPlusLeading, height, 0);
    this.width = fontMetrics.charWidth(' ') * numSpaces;
  }

  public boolean extractSelectionText(final StringBuffer buffer, final boolean inSelection, final RenderableSpot startPoint, final RenderableSpot endPoint) {
    int startX = -1;
    int endX = -1;
    if (this == startPoint.renderable) {
      startX = startPoint.x;
    }
    if (this == endPoint.renderable) {
      endX = endPoint.x;
    }
    if (!inSelection && (startX == -1) && (endX == -1)) {
      return false;
    }
    if ((startX != -1) && (endX != -1)) {
      if (endX < startX) {
        final int temp = startX;
        startX = endX;
        endX = temp;
      }
    } else if ((startX != -1) && (endX == -1) && inSelection) {
      endX = startX;
      startX = -1;
    } else if ((startX == -1) && (endX != -1) && !inSelection) {
      startX = endX;
      endX = -1;
    }
    int index1 = -1;
    int index2 = -1;
    if (startX != -1) {
      index1 = 0;
    }
    if (endX != -1) {
      index2 = 0;
    }
    if ((index1 != -1) || (index2 != -1)) {
      if (index2 == -1) {
        buffer.append('\t');
      }
    } else {
      if (inSelection) {
        buffer.append('\t');
        return true;
      }
    }
    if ((index1 != -1) && (index2 != -1)) {
      return false;
    } else {
      return !inSelection;
    }
  }
}
