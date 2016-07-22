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

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.html.style.HtmlValues;
import org.lobobrowser.html.style.RenderState;

public class DelayedPair {
  public final RenderableContainer containingBlock;
  private final RenderableContainer immediateContainingBlock;
  public final @NonNull BoundableRenderable child;
  private final String left;
  private final String top;
  private final String bottom;
  private final String right;
  private final String width;
  private final String height;
  private final RenderState rs;
  private final int initX;
  private final int initY;
  final boolean isFixed;
  final boolean isRelative;

  private boolean isAdded = false;

  public DelayedPair(final RenderableContainer immediateContainingBlock, final RenderableContainer containingBlock,
      final @NonNull BoundableRenderable child, final String left, final String right, final String top, final String bottom,
      final String width, final String height,
      final RenderState rs,
      final int initX, final int initY, final int position) {
    this.immediateContainingBlock = immediateContainingBlock;
    this.containingBlock = containingBlock;
    this.child = child;
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.width = width;
    this.height = height;
    this.rs = rs;
    this.initX = initX;
    this.initY = initY;
    this.isFixed = position == RenderState.POSITION_FIXED;
    this.isRelative = position == RenderState.POSITION_RELATIVE;
  }

  private static Integer helperGetPixelSize(final String spec, final RenderState rs, final int errorValue, final int avail) {
    if (spec != null) {
      return "auto".equals(spec) ? null : HtmlValues.getPixelSize(spec, rs, errorValue, avail);
    } else {
      return null;
    }
  }

  private Integer getLeft() {
    return helperGetPixelSize(left, rs, 0, containingBlock.getInnerWidth());
  }

  private Integer getWidth() {
    return helperGetPixelSize(width, rs, 0, containingBlock.getInnerWidth());
  }

  private Integer getHeight() {
    return helperGetPixelSize(height, rs, 0, containingBlock.getInnerHeight());
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

  public @NonNull BoundableRenderable positionPairChild() {
    final RenderableContainer parent = this.containingBlock;
    if (isRelative) {
      final RElement rChild = (RElement) this.child;
      rChild.setupRelativePosition(this.immediateContainingBlock);
      TranslatedRenderable tr = new TranslatedRenderable(rChild);
      // tr.setX(rChild.getX() + tp.x);
      // tr.setY(rChild.getY() + tp.y);
      rChild.setDelegator(tr);
      return tr;
    }

    final BoundableRenderable child = this.child;
    /*
    System.out.println("DP: " + this);
    System.out.println("  child block           : " + child);
    System.out.println("  containing block: " + this.containingBlock);
    System.out.println("  imm cntng  block: " + this.immediateContainingBlock);
    */

    // final java.awt.Point tp = parent.translateDescendentPoint((BoundableRenderable)(immediateContainingBlock), initX, initY);
    // final java.awt.Point tp = immediateContainingBlock.getOriginRelativeTo(((RBlock)parent).bodyLayout);
    final java.awt.Point tp = immediateContainingBlock.getOriginRelativeToAbs((RCollection) parent);
    tp.translate(initX, initY);

    if (this.immediateContainingBlock != parent) {
        final Insets immediateInsets = this.immediateContainingBlock.getInsetsMarginBorder(false, false);
        tp.translate(immediateInsets.left, immediateInsets.top);
    }

    Integer x = this.getLeft();
    Integer y = this.getTop();

    final Integer width = getWidth();
    final Integer height = getHeight();
    final Integer right = this.getRight();
    final Integer bottom = this.getBottom();
    final int childVerticalScrollBarHeight = child.getVerticalScrollBarHeight();
    if (right != null) {
      if (x != null) {
        // width = parent.getInnerWidth() - (x + right);
        child.setInnerWidth(parent.getInnerWidth() - (x + right) - childVerticalScrollBarHeight);
      } else {
        if (width != null) {
          child.setInnerWidth(width - childVerticalScrollBarHeight);
        }
        final int childWidth = child.getWidth();
        x = parent.getInnerWidth() - (childWidth + right - childVerticalScrollBarHeight);
      }
    } else {
      if (width != null) {
        child.setInnerWidth(width - childVerticalScrollBarHeight);
      }
    }

    final int childHorizontalScrollBarHeight = child.getHorizontalScrollBarHeight();
    if (bottom != null) {
      if (y != null) {
        // height = parent.getInnerHeight() - (y + bottom);
        child.setInnerHeight(parent.getInnerHeight() - (y + bottom) - childHorizontalScrollBarHeight);
      } else {
        if (height != null) {
          child.setInnerHeight(height - childHorizontalScrollBarHeight);
        }
        // final int childHeight = height == null? child.getHeight() : height;
        final int childHeight = child.getHeight();
        y = parent.getInnerHeight() - (childHeight + bottom - childHorizontalScrollBarHeight);
      }
    } else {
      if (height != null) {
        child.setInnerHeight(height - childHorizontalScrollBarHeight);
      }
    }

    child.setX((x == null ? tp.x : x));
    child.setY((y == null ? tp.y : y));

    return child;
  }

  @Override
  public String toString() {
    return "DP " + child + " containing block: " + containingBlock;
  }

  public synchronized boolean isAdded() {
    return isAdded;
  }

  public synchronized void markAdded() {
    isAdded = true;
  }

}
