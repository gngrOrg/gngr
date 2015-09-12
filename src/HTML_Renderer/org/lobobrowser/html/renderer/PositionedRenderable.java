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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Iterator;

import org.lobobrowser.html.domimpl.ModelNode;
import org.lobobrowser.html.style.RenderState;
import org.w3c.dom.html.HTMLDocument;
import org.w3c.dom.html.HTMLHtmlElement;

public class PositionedRenderable implements Renderable {
  public static final PositionedRenderable[] EMPTY_ARRAY = new PositionedRenderable[0];
  public final BoundableRenderable renderable;
  public final boolean verticalAlignable;
  public final int ordinal;
  public final boolean isFloat;
  private final boolean isFixed;

  public PositionedRenderable(final BoundableRenderable renderable, final boolean verticalAlignable, final int ordinal,
      final boolean isFloat, final boolean isFixed) {
    super();
    this.renderable = renderable;
    this.verticalAlignable = verticalAlignable;
    this.ordinal = ordinal;
    this.isFloat = isFloat;
    this.isFixed = isFixed;
  }

  public RCollection getOriginalParent() {
    return this.renderable.getOriginalParent();
  }

  @Override
  public void paint(final Graphics gIn) {
    final RCollection originalParent = this.renderable.getOriginalParent();
    final RCollection rparent = renderable.getParent();

    /*
    System.out.println("pr: " + this);
    System.out.println("  parent     : " + rparent);
    System.out.println("  orig parent: " + originalParent);
    */

      final Point or = originalParent.getOriginRelativeTo(rparent);
    final int pos = this.renderable.getModelNode().getRenderState().getPosition();

    if (isFloat || pos == RenderState.POSITION_ABSOLUTE || pos == RenderState.POSITION_FIXED) {
      final Graphics g2 = gIn.create();
      final Point some = getSome();

      if (some != null) {
        g2.translate(some.x, some.y);
      }

      /*
      if (isFloat) {
        g2.translate(or.x, or.y);
      }*/

      // g2.setColor(Color.GREEN);
      // g2.fillRect(0, 0, renderable.getWidth(), 100);

      this.renderable.paintTranslated(g2);
    } else {
      final Point orNoScroll = originalParent.getOriginRelativeToNoScroll(rparent);

      // System.out.println("  orNoScroll: " + orNoScroll);
      // System.out.println("  or        : " + or);

      final Rectangle bounds = originalParent.getClipBounds();
      // System.out.println("  clip bounds: " + bounds);
      Graphics g2;
      if (bounds != null) {
        final int tx = bounds.x + orNoScroll.x;
        final int ty = bounds.y + orNoScroll.y;
        g2 = gIn.create(tx, ty, bounds.width, bounds.height);
        g2.translate(-tx, -ty);
      } else {
        g2 = gIn.create();
      }

      g2.translate(or.x, or.y);

      // g2.setColor(new java.awt.Color(0.5f, 0.5f, 0f, 0.8f));
      // g2.fillRect(0, 0, bounds.width, bounds.height);

      try {
        this.renderable.paintTranslated(g2);
      } finally {
        g2.dispose();
      }
    }
  }

  @Override
  public ModelNode getModelNode() {
    return this.renderable.getModelNode();
  }

  @Override
  public boolean isFixed() {
    return isFixed;
  }

  @Override
  public String toString() {
    return "PosRndrbl [" + renderable + "]";
  }

  public Rectangle getVisualBounds() {
    final Rectangle bounds = renderable.getVisualBounds();
    final Point offset = getOffset();
    bounds.translate(offset.x, offset.y);
    return bounds;
  }

  public Point getOffset() {
    final Point offset = new Point();
    final int pos = this.renderable.getModelNode().getRenderState().getPosition();

    final RCollection originalParent = this.renderable.getOriginalParent();
    final RCollection rparent = renderable.getParent();
    final Point or = originalParent.getOriginRelativeTo(rparent);
    if (isFloat || pos == RenderState.POSITION_ABSOLUTE || pos == RenderState.POSITION_FIXED) {
      final Point some = getSome();
      if (some!= null) {
        offset.translate(some.x, some.y);
      }
    } else {
      offset.translate(or.x, or.y);
    }
    return offset;
  }

  private Point getSome() {
    final RCollection rparent = renderable.getParent();
    if (!isFixed && rparent.getModelNode() instanceof HTMLDocument) {
      final Iterator<? extends Renderable> rs = rparent.getRenderables();
      if (rs != null) {
        while (rs.hasNext()) {
          final Renderable r = rs.next();
          if (r.getModelNode() instanceof HTMLHtmlElement) {
            final RBlock htmlBlock = ((RBlock) r);
            final Point htmlOffset = htmlBlock.bodyLayout.getOrigin();
            final Insets htmlInsets = htmlBlock.getInsetsMarginBorder(htmlBlock.hasHScrollBar, htmlBlock.hasVScrollBar);

            return new Point((int) htmlOffset.getX() - htmlInsets.left, (int) htmlOffset.getY() - htmlInsets.top);

          }
        }
      }
    }

    return null;
  }

  public boolean contains(int x, int y) {
    return getVisualBounds().contains(x, y);
  }
}