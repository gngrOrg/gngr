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
/*
 * Created on Apr 17, 2005
 */
package org.lobobrowser.html.renderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lobobrowser.html.domimpl.ModelNode;

/**
 * @author J. H. S.
 */
abstract class BaseBoundableRenderable extends BaseRenderable implements BoundableRenderable {
  protected static final Logger logger = Logger.getLogger(BaseBoundableRenderable.class.getName());
  protected static final Color SELECTION_COLOR = Color.BLUE;
  protected static final Color SELECTION_XOR = Color.LIGHT_GRAY;

  // protected final Rectangle bounds = new Rectangle();
  protected final RenderableContainer container;
  protected final ModelNode modelNode;

  public int x, y, width, height;

  /**
   * Starts as true because ancestors could be invalidated.
   */
  protected boolean layoutUpTreeCanBeInvalidated = true;

  public void markLayoutValid() {
    this.layoutUpTreeCanBeInvalidated = true;
  }

  public BaseBoundableRenderable(final RenderableContainer container, final ModelNode modelNode) {
    this.container = container;
    this.modelNode = modelNode;
  }

  public java.awt.Point getGUIPoint(final int clientX, final int clientY) {
    final Renderable parent = this.getParent();
    if (parent instanceof BoundableRenderable) {
      return ((BoundableRenderable) parent).getGUIPoint(clientX + this.x, clientY + this.y);
    } else if (parent == null) {
      return this.container.getGUIPoint(clientX + this.x, clientY + this.y);
    } else {
      throw new IllegalStateException("parent=" + parent);
    }
  }

  public Point getRenderablePoint(final int guiX, final int guiY) {
    final Renderable parent = this.getParent();
    if (parent instanceof BoundableRenderable) {
      return ((BoundableRenderable) parent).getRenderablePoint(guiX - this.x, guiY - this.y);
    } else if (parent == null) {
      return new Point(guiX - this.x, guiY - this.y);
    } else {
      throw new IllegalStateException("parent=" + parent);
    }
  }

  public int getHeight() {
    return height;
  }

  public int getWidth() {
    return width;
  }

  public void setWidth(final int width) {
    this.width = width;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public boolean contains(final int x, final int y) {
    return (x >= this.x) && (y >= this.y) && (x < (this.x + this.width)) && (y < (this.y + this.height));
  }

  public Rectangle getBounds() {
    return new Rectangle(this.x, this.y, this.width, this.height);
  }

  public Dimension getSize() {
    return new Dimension(this.width, this.height);
  }

  public ModelNode getModelNode() {
    return this.modelNode;
  }

  // /* (non-Javadoc)
  // * @see net.sourceforge.xamj.domimpl.markup.Renderable#getBounds()
  // */
  // public Rectangle getBounds() {
  // return this.bounds;
  // }
  //
  public void setBounds(final int x, final int y, final int width, final int height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  public void setX(final int x) {
    this.x = x;
  }

  public void setY(final int y) {
    this.y = y;
  }

  public void setHeight(final int height) {
    this.height = height;
  }

  public void setOrigin(final int x, final int y) {
    this.x = x;
    this.y = y;
  }

  protected abstract void invalidateLayoutLocal();

  /**
   * Invalidates this Renderable and its parent (i.e. all ancestors).
   */
  public final void invalidateLayoutUpTree() {
    if (this.layoutUpTreeCanBeInvalidated) {
      this.layoutUpTreeCanBeInvalidated = false;
      this.invalidateLayoutLocal();
      // Try original parent first.
      RCollection parent = this.originalParent;
      if (parent == null) {
        parent = this.parent;
        if (parent == null) {
          // Has to be top block
          final RenderableContainer rc = this.container;
          if (rc != null) {
            rc.invalidateLayoutUpTree();
          }
        } else {
          parent.invalidateLayoutUpTree();
        }
      } else {
        parent.invalidateLayoutUpTree();
      }
    } else {
    }
  }

  protected boolean isValid() {
    return this.layoutUpTreeCanBeInvalidated;
  }

  private final void relayoutImpl(final boolean invalidateLocal, final boolean onlyIfValid) {
    if (onlyIfValid && !this.layoutUpTreeCanBeInvalidated) {
      return;
    }
    if (invalidateLocal) {
      this.invalidateLayoutUpTree();
    }
    final Renderable parent = this.parent;
    if (parent instanceof BaseBoundableRenderable) {
      ((BaseBoundableRenderable) parent).relayoutImpl(false, false);
    } else if (parent == null) {
      // Has to be top RBlock.
      this.container.relayout();
    } else {
      if (logger.isLoggable(Level.INFO)) {
        logger.warning("relayout(): Don't know how to relayout " + this + ", parent being " + parent);
      }
    }
  }

  /**
   * Invalidates the current Renderable (which invalidates its ancestors) and
   * then requests the top level GUI container to do the layout and repaint.
   * It's safe to call this method outside the GUI thread.
   */
  public void relayout() {
    if (EventQueue.isDispatchThread()) {
      this.relayoutImpl(true, false);
    } else {
      EventQueue.invokeLater(() -> relayoutImpl(true, false));
    }
  }

  public void relayoutIfValid() {
    if (EventQueue.isDispatchThread()) {
      this.relayoutImpl(true, true);
    } else {
      EventQueue.invokeLater(() -> relayoutImpl(true, true));
    }
  }

  /**
   * Parent for graphics coordinates.
   */
  protected RCollection parent;

  public void setParent(final RCollection parent) {
    this.parent = parent;
  }

  public RCollection getParent() {
    return this.parent;
  }

  /**
   * Parent for invalidation.
   */
  protected RCollection originalParent;

  public void setOriginalParent(final RCollection origParent) {
    this.originalParent = origParent;
  }

  /**
   * This is the parent based on the original element hierarchy.
   */
  public RCollection getOriginalParent() {
    return this.originalParent;
  }

  public RCollection getOriginalOrCurrentParent() {
    final RCollection origParent = this.originalParent;
    if (origParent == null) {
      return this.parent;
    }
    return origParent;
  }

  public void repaint(final int x, final int y, final int width, final int height) {
    final Renderable parent = this.parent;
    if (parent instanceof BoundableRenderable) {
      ((BoundableRenderable) parent).repaint(x + this.x, y + this.y, width, height);
    } else if (parent == null) {
      // Has to be top RBlock.
      this.container.repaint(x, y, width, height);
    } else {
      if (logger.isLoggable(Level.INFO)) {
        logger.warning("repaint(): Don't know how to repaint " + this + ", parent being " + parent);
      }
    }
  }

  public void repaint() {
    this.repaint(0, 0, this.width, this.height);
  }

  public Color getBlockBackgroundColor() {
    return this.container.getPaintedBackgroundColor();
  }

  public final void paintTranslated(final Graphics g) {
    final int x = this.x;
    final int y = this.y;
    g.translate(x, y);
    try {
      this.paint(g);
    } finally {
      g.translate(-x, -y);
    }
  }

  protected final java.awt.Point translateDescendentPoint(BoundableRenderable descendent, int x, int y) {
    while (descendent != this) {
      if (descendent == null) {
        throw new IllegalStateException("Not descendent");
      }
      x += descendent.getX();
      y += descendent.getY();
      // Coordinates are always relative to actual parent?
      descendent = descendent.getParent();
    }
    return new Point(x, y);
  }

  public void onMouseOut(final MouseEvent event, final int x, final int y, final ModelNode limit) {
    if (this.isContainedByNode()) {
      HtmlController.getInstance().onMouseOut(this.modelNode, event, x, y, limit);
    }
  }

  public void onMouseMoved(final MouseEvent event, final int x, final int y, final boolean triggerEvent, final ModelNode limit) {
    if (triggerEvent) {
      if (this.isContainedByNode()) {
        HtmlController.getInstance().onMouseOver(this, this.modelNode, event, x, y, limit);
      }
    }
  }

  public Point getOrigin() {
    return new Point(this.x, this.y);
  }

  public Point getOriginRelativeTo(final RCollection ancestor) {
    int x = this.x;
    int y = this.y;
    RCollection parent = this.parent;
    for (;;) {
      if (parent == null) {
        throw new java.lang.IllegalArgumentException("Not an ancestor: " + ancestor);
      }
      if (parent == ancestor) {
        return new Point(x, y);
      }
      x += parent.getX();
      y += parent.getY();
      parent = parent.getParent();
    }
  }
}
