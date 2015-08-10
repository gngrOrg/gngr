/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The XAMJ Project

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

import java.awt.Insets;

import org.lobobrowser.html.style.HtmlValues;
import org.lobobrowser.html.style.RenderState;

public class DelayedPair {
  public final RenderableContainer containingBlock;
  private final RenderableContainer immediateContainingBlock;
  public final BoundableRenderable child;
  private final String left;
  private final String top;
  private final String bottom;
  private final String right;
  private final RenderState rs;
  private final int initX;
  private final int initY;
  final boolean isFixed;

  public DelayedPair(final RenderableContainer immediateContainingBlock, final RenderableContainer containingBlock,
      final BoundableRenderable child, final String left, final String right, final String top, final String bottom, final RenderState rs,
      final int initX, final int initY, final boolean isFixed) {
    this.immediateContainingBlock = immediateContainingBlock;
    this.containingBlock = containingBlock;
    this.child = child;
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.rs = rs;
    this.initX = initX;
    this.initY = initY;
    this.isFixed = isFixed;
  }

  private static Integer helperGetPixelSize(final String spec, final RenderState rs, final int errorValue, final int avail) {
    if (spec != null) {
      return HtmlValues.getPixelSize(spec, rs, errorValue, avail);
    } else {
      return null;
    }
  }

  private Integer getLeft() {
    return helperGetPixelSize(left, rs, 0, containingBlock.getInnerWidth());
  }

  private Integer getRight() {
    return helperGetPixelSize(right, rs, 0, containingBlock.getInnerWidth());
  }

  private Integer getTop() {
    return helperGetPixelSize(top, rs, 0, containingBlock.getInnerHeight());
  }

  private Integer getBottom() {
    return helperGetPixelSize(bottom, rs, 0, containingBlock.getInnerHeight());
  }

  public void positionPairChild() {
    final RenderableContainer parent = this.containingBlock;
    final BoundableRenderable child = this.child;
    Integer x = this.getLeft();
    Integer y = this.getTop();
    Integer width = null;
    Integer height = null;
    final Integer right = this.getRight();
    final Integer bottom = this.getBottom();
    if (right != null) {
      if (x != null) {
        width = parent.getInnerWidth() - (x + right);
      } else {
        x = parent.getInnerWidth() - (child.getWidth() + right);
      }
    }
    if (bottom != null) {
      if (y != null) {
        height = parent.getInnerHeight() - (y + bottom);
      } else {
        y = parent.getInnerHeight() - child.getHeight() - bottom;
      }
    }
    child.setX(x == null ? initX : x);

    if (y != null) {
      child.setY(y);
    } else {
      if (this.immediateContainingBlock != parent) {
        final Insets immediateInsets = this.immediateContainingBlock.getInsets(false, false);
        child.setY((this.initY + (this.immediateContainingBlock.getY() + immediateInsets.top)) - parent.getY());
      } else {
        child.setY(this.initY);
      }
    }
    if (width != null) {
      child.setWidth(width);
    }
    if (height != null) {
      child.setHeight(height);
    }
  }

  @Override
  public String toString() {
    return "DP " + child + " containing block: " + containingBlock;
  }
}
