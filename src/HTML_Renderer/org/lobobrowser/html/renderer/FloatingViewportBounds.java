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

class FloatingViewportBounds implements FloatingBounds {
  private final FloatingBounds prevBounds;
  private final boolean leftFloat;
  private final int y;
  private final int offsetFromBorder;
  private final int height;

  /**
   *
   * @param prevBounds
   * @param leftFloat
   * @param y
   * @param offsetFromBorder
   *          Width of floating box, including padding insets.
   * @param height
   */
  public FloatingViewportBounds(final FloatingBounds prevBounds, final boolean leftFloat, final int y, final int offsetFromBorder,
      final int height) {
    this.prevBounds = prevBounds;
    this.leftFloat = leftFloat;
    this.y = y;
    this.offsetFromBorder = offsetFromBorder;
    this.height = height;
  }

  public int getLeft(final int y) {
    int left = 0;
    if (this.leftFloat && (y >= this.y) && (y < (this.y + height))) {
      left = this.offsetFromBorder;
    }
    final FloatingBounds prev = this.prevBounds;
    if (prev != null) {
      final int newLeft = prev.getLeft(y);
      if (newLeft > left) {
        left = newLeft;
      }
    }
    return left;
  }

  /**
   * The offset from the right edge, not counting padding.
   */
  public int getRight(final int y) {
    int right = 0;
    if (!this.leftFloat && (y >= this.y) && (y < (this.y + this.height))) {
      right = this.offsetFromBorder;
    }
    final FloatingBounds prev = this.prevBounds;
    if (prev != null) {
      final int newRight = prev.getRight(y);
      if (newRight > right) {
        right = newRight;
      }
    }
    return right;
  }

  public int getClearY(final int y) {
    int cleary = Math.max(y, this.y + this.height);
    final FloatingBounds prev = this.prevBounds;
    if (prev != null) {
      final int pcy = prev.getClearY(y);
      if (pcy > cleary) {
        cleary = pcy;
      }
    }
    return cleary;
  }

  public int getFirstClearY(final int y) {
    int clearY = y;
    final FloatingBounds prev = this.prevBounds;
    if (prev != null) {
      final int prevClearY = prev.getFirstClearY(y);
      if (prevClearY != y) {
        clearY = prevClearY;
      }
    }
    if ((clearY == y) && (y >= this.y) && (y < (this.y + this.height))) {
      clearY = this.y + this.height;
    }
    return clearY;
  }

  public int getLeftClearY(final int y) {
    int cleary;
    if (this.leftFloat) {
      cleary = Math.max(y, this.y + this.height);
    } else {
      cleary = y;
    }
    final FloatingBounds prev = this.prevBounds;
    if (prev != null) {
      final int pcy = prev.getLeftClearY(y);
      if (pcy > cleary) {
        cleary = pcy;
      }
    }
    return cleary;
  }

  public int getRightClearY(final int y) {
    int cleary;
    if (!this.leftFloat) {
      cleary = Math.max(y, this.y + this.height);
    } else {
      cleary = y;
    }
    final FloatingBounds prev = this.prevBounds;
    if (prev != null) {
      final int pcy = prev.getLeftClearY(y);
      if (pcy > cleary) {
        cleary = pcy;
      }
    }
    return cleary;
  }

  public int getMaxY() {
    int maxY = this.y + this.height;
    final FloatingBounds prev = this.prevBounds;
    if (prev != null) {
      final int prevMaxY = prev.getMaxY();
      if (prevMaxY > maxY) {
        maxY = prevMaxY;
      }
    }
    return maxY;
  }

  @Override
  public boolean equals(final Object other) {
    // Important for layout caching.
    if (other == this) {
      return true;
    }
    if (!(other instanceof FloatingViewportBounds)) {
      return false;
    }
    final FloatingViewportBounds olm = (FloatingViewportBounds) other;
    return (olm.leftFloat == this.leftFloat) && (olm.y == this.y) && (olm.height == this.height)
        && (olm.offsetFromBorder == this.offsetFromBorder)
        && java.util.Objects.equals(olm.prevBounds, this.prevBounds);
  }

  @Override
  public int hashCode() {
    return (this.leftFloat ? 1 : 0) ^ this.y ^ this.height ^ this.offsetFromBorder;
  }
}
