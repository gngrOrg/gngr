package org.lobobrowser.html.renderer;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Iterator;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.html.domimpl.ModelNode;

abstract class BaseRCollection extends BaseBoundableRenderable implements RCollection {
  public BaseRCollection(final RenderableContainer container, final ModelNode modelNode) {
    super(container, modelNode);
  }

  public void focus() {
    this.container.focus();
    // TODO: Plus local focus
  }

  public void blur() {
    final RCollection parent = this.parent;
    if (parent != null) {
      parent.focus();
    } else {
      // TODO: Remove local focus
    }
  }

  /**
   * Updates bounds of all descendent's GUI components, based on root bounds.
   */
  public void updateWidgetBounds(final int guiX, final int guiY) {
    final Iterator<? extends Renderable> i = this.getRenderables(false);
    if (i != null) {
      while (i.hasNext()) {
        final Renderable rn = i.next();
        final Renderable r = (rn instanceof PositionedRenderable) ? ((PositionedRenderable) rn).renderable : rn;
        if (r instanceof RCollection) {
          // RUIControl is a RCollection too.
          final RCollection rc = (RCollection) r;
          final Point or = rc.getOriginRelativeTo(this);
          // rc.updateWidgetBounds(guiX + rc.getX(), guiY + rc.getY());
          rc.updateWidgetBounds(guiX + or.x, guiY + or.y);
        }

        /*
        int ox = guiX;
        int oy = guiY;
        Renderable r = rn;
        if (rn instanceof PositionedRenderable) {
          final PositionedRenderable pr = (PositionedRenderable) rn;
          final Point pro = pr.getOffset();
          ox += pro.x;
          oy += pro.y;
          r = pr.renderable;
        }
        if (r instanceof RCollection) {
          // RUIControl is a RCollection too.
          final RCollection rc = (RCollection) r;
          final Point or = rc.getOriginRelativeTo(this);
          // rc.updateWidgetBounds(guiX + rc.getX(), guiY + rc.getY());
          rc.updateWidgetBounds(ox + or.x, oy + or.y);
        }
        */
      }
    }
  }

  private static boolean checkStartSelection(final Rectangle bounds, final Point selectionPoint) {
    if (bounds.y > selectionPoint.y) {
      return true;
    } else if ((selectionPoint.y >= bounds.y) && (selectionPoint.y < (bounds.y + bounds.height)) && (bounds.x > selectionPoint.x)) {
      return true;
    } else {
      return false;
    }
  }

  private static boolean checkEndSelection(final Rectangle bounds, final Point selectionPoint) {
    if (bounds.y > selectionPoint.y) {
      return true;
    } else if ((selectionPoint.y >= bounds.y) && (selectionPoint.y < (bounds.y + bounds.height)) && (selectionPoint.x < bounds.x)) {
      return true;
    } else {
      return false;
    }
  }

  public boolean paintSelection(final Graphics g, boolean inSelection, final RenderableSpot startPoint, final RenderableSpot endPoint) {
    // TODO: Does this work with renderables that are absolutely positioned?
    Point checkPoint1 = null;
    Point checkPoint2 = null;
    if (!inSelection) {
      final boolean isStart = startPoint.renderable == this;
      final boolean isEnd = endPoint.renderable == this;
      if (isStart && isEnd) {
        checkPoint1 = startPoint.getPoint();
        checkPoint2 = endPoint.getPoint();
      } else if (isStart) {
        checkPoint1 = startPoint.getPoint();
      } else if (isEnd) {
        checkPoint1 = endPoint.getPoint();
      }
    } else {
      if (startPoint.renderable == this) {
        checkPoint1 = startPoint.getPoint();
      } else if (endPoint.renderable == this) {
        checkPoint1 = endPoint.getPoint();
      }
    }
    final Iterator<? extends Renderable> i = this.getRenderables(true);
    if (i != null) {
      while (i.hasNext()) {
        final Object robj = i.next();
        if (robj instanceof BoundableRenderable) {
          final BoundableRenderable renderable = (BoundableRenderable) robj;
          final Rectangle bounds = renderable.getVisualBounds();
          if (!inSelection) {
            if ((checkPoint1 != null) && checkStartSelection(bounds, checkPoint1)) {
              if (checkPoint2 != null) {
                checkPoint1 = checkPoint2;
                checkPoint2 = null;
              } else {
                checkPoint1 = null;
              }
              inSelection = true;
            } else if ((checkPoint2 != null) && checkStartSelection(bounds, checkPoint2)) {
              checkPoint1 = null;
              checkPoint2 = null;
              inSelection = true;
            }
          } else if (inSelection && (checkPoint1 != null) && checkEndSelection(bounds, checkPoint1)) {
            return false;
          }
          final int offsetX = bounds.x;
          final int offsetY = bounds.y;
          g.translate(offsetX, offsetY);
          try {
            final boolean newInSelection = renderable.paintSelection(g, inSelection, startPoint, endPoint);
            if (inSelection && !newInSelection) {
              return false;
            }
            inSelection = newInSelection;
          } finally {
            g.translate(-offsetX, -offsetY);
          }
        }
      }
    }
    if (inSelection && (checkPoint1 != null)) {
      return false;
    } else if (!inSelection && ((checkPoint1 != null) || (checkPoint2 != null)) && !((checkPoint1 != null) && (checkPoint2 != null))) {
      // Has to have started not being in selection,
      // but we must start selecting without having
      // selected anything in the block then.
      return true;
    }
    return inSelection;
  }

  public boolean extractSelectionText(final StringBuffer buffer, boolean inSelection, final RenderableSpot startPoint,
      final RenderableSpot endPoint) {
    Point checkPoint1 = null;
    Point checkPoint2 = null;
    if (!inSelection) {
      final boolean isStart = startPoint.renderable == this;
      final boolean isEnd = endPoint.renderable == this;
      if (isStart && isEnd) {
        checkPoint1 = startPoint.getPoint();
        checkPoint2 = endPoint.getPoint();
      } else if (isStart) {
        checkPoint1 = startPoint.getPoint();
      } else if (isEnd) {
        checkPoint1 = endPoint.getPoint();
      }
    } else {
      if (startPoint.renderable == this) {
        checkPoint1 = startPoint.getPoint();
      } else if (endPoint.renderable == this) {
        checkPoint1 = endPoint.getPoint();
      }
    }
    final Iterator<? extends Renderable> i = this.getRenderables(true);
    if (i != null) {
      while (i.hasNext()) {
        final Renderable rn = i.next();
        final Renderable robj = (rn instanceof PositionedRenderable) ? ((PositionedRenderable) rn).renderable : rn;
        if (robj instanceof BoundableRenderable) {
          final BoundableRenderable renderable = (BoundableRenderable) robj;
          if (!inSelection) {
            final Rectangle bounds = renderable.getVisualBounds();
            if ((checkPoint1 != null) && checkStartSelection(bounds, checkPoint1)) {
              if (checkPoint2 != null) {
                checkPoint1 = checkPoint2;
                checkPoint2 = null;
              } else {
                checkPoint1 = null;
              }
              inSelection = true;
            } else if ((checkPoint2 != null) && checkStartSelection(bounds, checkPoint2)) {
              checkPoint1 = null;
              checkPoint2 = null;
              inSelection = true;
            }
          } else if (inSelection && (checkPoint1 != null) && checkEndSelection(renderable.getBounds(), checkPoint1)) {
            return false;
          }
          final boolean newInSelection = renderable.extractSelectionText(buffer, inSelection, startPoint, endPoint);
          if (inSelection && !newInSelection) {
            return false;
          }
          inSelection = newInSelection;
        }
      }
    }
    if (inSelection && (checkPoint1 != null)) {
      return false;
    } else if (!inSelection && ((checkPoint1 != null) || (checkPoint2 != null)) && !((checkPoint1 != null) && (checkPoint2 != null))) {
      // Has to have started not being in selection,
      // but we must start selecting without having
      // selected anything in the block then.
      return true;
    }
    return inSelection;
  }

  public void invalidateLayoutDeep() {
    // TODO: May be pretty inefficient in RLine's
    // if it's true that non-layable components
    // are not in RLine's anymore.
    this.invalidateLayoutLocal();
    final Iterator<? extends Renderable> renderables = this.getRenderables(true);
    if (renderables != null) {
      while (renderables.hasNext()) {
        final Renderable rn = renderables.next();
        final Renderable r = (rn instanceof PositionedRenderable) ? ((PositionedRenderable) rn).renderable : rn;
        if (r instanceof RCollection) {
          ((RCollection) r).invalidateLayoutDeep();
        }
      }
    }
  }

  private BoundableRenderable renderableWithMouse = null;

  @Override
  public void onMouseMoved(final MouseEvent event, final int x, final int y, final boolean triggerEvent, final ModelNode limit) {
    super.onMouseMoved(event, x, y, triggerEvent, limit);
    final BoundableRenderable oldRenderable = this.renderableWithMouse;
    final Renderable r = this.getRenderable(x, y);
    final BoundableRenderable newRenderable = r instanceof BoundableRenderable ? (BoundableRenderable) r : null;
    ModelNode newLimit;
    if (this.isContainedByNode()) {
      newLimit = this.modelNode;
    } else {
      newLimit = limit;
    }
    final boolean changed = oldRenderable != newRenderable;
    if (changed) {
      if (oldRenderable != null) {
        oldRenderable.onMouseOut(event, x - oldRenderable.getVisualX(), y - oldRenderable.getVisualY(), newLimit);
      }
      this.renderableWithMouse = newRenderable;
    }
    // Must recurse always
    if (newRenderable != null) {
      // newRenderable.onMouseMoved(event, x - newRenderable.getVisualX(), y - newRenderable.getVisualY(), changed, newLimit);
      final Point or = newRenderable.getOriginRelativeTo(this);
      newRenderable.onMouseMoved(event, x - or.x, y - or.y, changed, newLimit);
    }
  }

  @Override
  public boolean onMousePressed(MouseEvent event, int x, int y) {
    final Renderable r = this.getRenderable(x, y);
    final RCollection newRenderable = r instanceof RCollection ? (RCollection) r : null;
    if (newRenderable != null) {
      final Point or = newRenderable.getOriginRelativeTo(this);
      if(!newRenderable.onMousePressed(event, x - or.x, y - or.y)) {
        return false;
      }
    }
    if (!HtmlController.getInstance().onMouseDown(this.modelNode, event, x, y)) {
      return false;
    }
    return true;
  }

  @Override
  public boolean onMouseClick(MouseEvent event, int x, int y) {
    final Renderable r = this.getRenderable(x, y);
    final RCollection newRenderable = r instanceof RCollection ? (RCollection) r : null;
    if (newRenderable != null) {
      final Point or = newRenderable.getOriginRelativeTo(this);
      if(!newRenderable.onMouseClick(event, x - or.x, y - or.y)) {
        return false;
      }
    }
    if (!HtmlController.getInstance().onMouseClick(this.modelNode, event, x, y)) {
      return false;
    }
    return true;
  }

  @Override
  public void onMouseOut(final MouseEvent event, final int x, final int y, final ModelNode limit) {
    super.onMouseOut(event, x, y, limit);
    final BoundableRenderable oldRenderable = this.renderableWithMouse;
    if (oldRenderable != null) {
      this.renderableWithMouse = null;
      ModelNode newLimit;
      if (this.isContainedByNode()) {
        newLimit = this.modelNode;
      } else {
        newLimit = limit;
      }
      final Point or = oldRenderable.getOriginRelativeTo(this);
      oldRenderable.onMouseOut(event, x - or.x, y - or.y, newLimit);
    }
  }

  public BoundableRenderable getRenderable(final int x, final int y) {
    final Iterator<? extends Renderable> i = this.getRenderables(true);
    if (i != null) {
      while (i.hasNext()) {
        final Renderable rn = i.next();
        final Renderable r = rn;
        if (r instanceof BoundableRenderable) {
          final BoundableRenderable br = (BoundableRenderable) r;
          /*
          if (br instanceof RBlockViewport) {
            return br;
          }*/
          if ((!br.isDelegated()) && br.contains(x, y)) {
            return br;
          }
        } else if (r instanceof PositionedRenderable) {
          PositionedRenderable pr = (PositionedRenderable) r;
          if (pr.contains(x, y)) {
            return pr.renderable;
          }
        }
      }
    }
    return null;
  }

  public boolean onMiddleClick(final MouseEvent event, final int x, final int y) {
    final BoundableRenderable br = this.getRenderable(x, y);
    if (br == null) {
      return HtmlController.getInstance().onMiddleClick(this.modelNode, event, x, y);
    } else {
      final Point or = br.getOriginRelativeTo(this);
      return br.onMiddleClick(event, x - or.x, y - or.y);
    }
  }

  public boolean onRightClick(final MouseEvent event, final int x, final int y) {
    final BoundableRenderable br = this.getRenderable(x, y);
    if (br == null) {
      return HtmlController.getInstance().onContextMenu(this.modelNode, event, x, y);
    } else {
      final Point or = br.getOriginRelativeTo(this);
      return br.onRightClick(event, x - or.x, y - or.y);
    }
  }

  @Override
  public Rectangle getClipBoundsWithoutInsets() {
    // TODO
    return getClipBounds();
  }

  @Override
  public boolean isReadyToPaint() {
    final Iterator<@NonNull ? extends Renderable> renderables = getRenderables();
    if (renderables == null) {
      return true;
    }
    while (renderables.hasNext()) {
      final Renderable next = renderables.next();
      if (!next.isReadyToPaint()) {
        return false;
      }
    }
    return true;
  }
}
