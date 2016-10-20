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
 * Created on Nov 19, 2005
 */
package org.lobobrowser.html.renderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.domimpl.HTMLElementImpl;
import org.lobobrowser.html.domimpl.ModelNode;
import org.lobobrowser.html.renderer.TableMatrix.RTableRowGroup;
import org.lobobrowser.html.style.RenderState;
import org.lobobrowser.html.style.RenderThreadState;
import org.lobobrowser.ua.UserAgentContext;
import org.lobobrowser.util.CollectionUtilities;

class RTable extends BaseBlockyRenderable {
  private static final int MAX_CACHE_SIZE = 10;
  private final Map<LayoutKey, LayoutValue> cachedLayout = new HashMap<>(5);
  private final TableMatrix tableMatrix;
  private SortedSet<@NonNull PositionedRenderable> positionedRenderables;
  private int otherOrdinal;
  private LayoutKey lastLayoutKey = null;
  private LayoutValue lastLayoutValue = null;

  public RTable(final HTMLElementImpl modelNode, final UserAgentContext pcontext, final HtmlRendererContext rcontext,
      final FrameContext frameContext,
      final RenderableContainer container) {
    super(container, modelNode, pcontext);
    this.tableMatrix = new TableMatrix(modelNode, pcontext, rcontext, frameContext, this, this);
  }

  @Override
  public void paintShifted(final Graphics g) {
    final RenderState rs = this.modelNode.getRenderState();
    if ((rs != null) && (rs.getVisibility() != RenderState.VISIBILITY_VISIBLE)) {
      // Just don't paint it.
      return;
    }

    this.prePaint(g);
    final Dimension size = this.getSize();
    // TODO: No scrollbars
    final TableMatrix tm = this.tableMatrix;
    tm.paint(g, size);
    final Collection<PositionedRenderable> prs = this.positionedRenderables;
    if (prs != null) {
      final Iterator<PositionedRenderable> i = prs.iterator();
      while (i.hasNext()) {
        final PositionedRenderable pr = i.next();
        pr.paint(g);
        /*
        final BoundableRenderable r = pr.renderable;
        r.paintTranslated(g);
        */
      }
    }
  }

  @Override
  public void doLayout(final int availWidth, final int availHeight, final boolean sizeOnly) {
    final Map<LayoutKey, LayoutValue> cachedLayout = this.cachedLayout;
    final RenderState rs = this.modelNode.getRenderState();
    final int whitespace = rs == null ? RenderState.WS_NORMAL : rs.getWhiteSpace();
    final Font font = rs == null ? null : rs.getFont();
    // Having whiteSpace == NOWRAP and having a NOWRAP override
    // are not exactly the same thing.
    final boolean overrideNoWrap = RenderThreadState.getState().overrideNoWrap;
    final LayoutKey layoutKey = new LayoutKey(availWidth, availHeight, whitespace, font, overrideNoWrap);
    LayoutValue layoutValue;
    if (sizeOnly) {
      layoutValue = cachedLayout.get(layoutKey);
    } else {
      if (java.util.Objects.equals(layoutKey, this.lastLayoutKey)) {
        layoutValue = this.lastLayoutValue;
      } else {
        layoutValue = null;
      }
    }
    if (layoutValue == null) {
      final Collection<PositionedRenderable> prs = this.positionedRenderables;
      if (prs != null) {
        prs.clear();
      }
      this.otherOrdinal = 0;
      this.clearGUIComponents();
      this.clearDelayedPairs();
      this.applyStyle(availWidth, availHeight);
      final TableMatrix tm = this.tableMatrix;
      final Insets insets = this.getInsets(false, false);
      tm.reset(insets, availWidth, availHeight);
      // TODO: No scrollbars
      tm.build(availWidth, availHeight, sizeOnly);
      tm.doLayout(insets);

      // Import applicable delayed pairs.
      // Only needs to be done if layout was forced. Otherwise, they should've been imported already.
      final Collection<DelayedPair> pairs = this.delayedPairs;
      if (pairs != null) {
        final Iterator<DelayedPair> i = pairs.iterator();
        while (i.hasNext()) {
          final DelayedPair pair = i.next();
          if (pair.containingBlock == this) {
            this.importDelayedPair(pair);
          }
        }
      }
      layoutValue = new LayoutValue(tm.getTableWidth(), tm.getTableHeight());
      if (sizeOnly) {
        if (cachedLayout.size() > MAX_CACHE_SIZE) {
          // Unlikely, but we should ensure it's bounded.
          cachedLayout.clear();
        }
        cachedLayout.put(layoutKey, layoutValue);
        this.lastLayoutKey = null;
        this.lastLayoutValue = null;
      } else {
        this.lastLayoutKey = layoutKey;
        this.lastLayoutValue = layoutValue;
      }
    }
    this.width = layoutValue.width;
    this.height = layoutValue.height;
    this.sendGUIComponentsToParent();
    this.sendDelayedPairsToParent();
  }

  @Override
  public void invalidateLayoutLocal() {
    super.invalidateLayoutLocal();
    this.cachedLayout.clear();
    this.lastLayoutKey = null;
    this.lastLayoutValue = null;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.renderer.BoundableRenderable#getRenderablePoint(int,
   * int)
   */
  public RenderableSpot getLowestRenderableSpot(final int x, final int y) {
    final Collection<PositionedRenderable> prs = this.positionedRenderables;
    if (prs != null) {
      final Iterator<PositionedRenderable> i = prs.iterator();
      while (i.hasNext()) {
        final PositionedRenderable pr = i.next();
        final BoundableRenderable r = pr.renderable;
        final int childX = x - r.getVisualX();
        final int childY = y - r.getVisualY();
        final RenderableSpot rs = r.getLowestRenderableSpot(childX, childY);
        if (rs != null) {
          return rs;
        }
      }
    }
    final RenderableSpot rs = this.tableMatrix.getLowestRenderableSpot(x, y);
    if (rs != null) {
      return rs;
    }
    return new RenderableSpot(this, x, y);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.html.renderer.BoundableRenderable#onMouseClick(java.awt.event
   * .MouseEvent, int, int)
   */
  public boolean onMouseClick(final MouseEvent event, final int x, final int y) {
    final Collection<PositionedRenderable> prs = this.positionedRenderables;
    if (prs != null) {
      final Iterator<PositionedRenderable> i = prs.iterator();
      while (i.hasNext()) {
        final PositionedRenderable pr = i.next();
        final BoundableRenderable r = pr.renderable;
        final Rectangle bounds = r.getVisualBounds();
        if (bounds.contains(x, y)) {
          final int childX = x - r.getVisualX();
          final int childY = y - r.getVisualY();
          if (!r.onMouseClick(event, childX, childY)) {
            return false;
          }
        }
      }
    }
    return this.tableMatrix.onMouseClick(event, x, y);
  }

  public boolean onDoubleClick(final MouseEvent event, final int x, final int y) {
    final Collection<PositionedRenderable> prs = this.positionedRenderables;
    if (prs != null) {
      final Iterator<PositionedRenderable> i = prs.iterator();
      while (i.hasNext()) {
        final PositionedRenderable pr = i.next();
        final BoundableRenderable r = pr.renderable;
        final Rectangle bounds = r.getVisualBounds();
        if (bounds.contains(x, y)) {
          final int childX = x - r.getVisualX();
          final int childY = y - r.getVisualY();
          if (!r.onDoubleClick(event, childX, childY)) {
            return false;
          }
        }
      }
    }
    return this.tableMatrix.onDoubleClick(event, x, y);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.html.renderer.BoundableRenderable#onMouseDisarmed(java.awt.event
   * .MouseEvent)
   */
  public boolean onMouseDisarmed(final MouseEvent event) {
    return this.tableMatrix.onMouseDisarmed(event);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.html.renderer.BoundableRenderable#onMousePressed(java.awt.event
   * .MouseEvent, int, int)
   */
  public boolean onMousePressed(final MouseEvent event, final int x, final int y) {
    final Collection<PositionedRenderable> prs = this.positionedRenderables;
    if (prs != null) {
      final Iterator<PositionedRenderable> i = prs.iterator();
      while (i.hasNext()) {
        final PositionedRenderable pr = i.next();
        final BoundableRenderable r = pr.renderable;
        final Rectangle bounds = r.getVisualBounds();
        if (bounds.contains(x, y)) {
          final int childX = x - r.getVisualX();
          final int childY = y - r.getVisualY();
          if (!r.onMousePressed(event, childX, childY)) {
            return false;
          }
        }
      }
    }
    return this.tableMatrix.onMousePressed(event, x, y);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.html.renderer.BoundableRenderable#onMouseReleased(java.awt.event
   * .MouseEvent, int, int)
   */
  public boolean onMouseReleased(final MouseEvent event, final int x, final int y) {
    final Collection<PositionedRenderable> prs = this.positionedRenderables;
    if (prs != null) {
      final Iterator<PositionedRenderable> i = prs.iterator();
      while (i.hasNext()) {
        final PositionedRenderable pr = i.next();
        final BoundableRenderable r = pr.renderable;
        final Rectangle bounds = r.getVisualBounds();
        if (bounds.contains(x, y)) {
          final int childX = x - r.getVisualX();
          final int childY = y - r.getVisualY();
          if (!r.onMouseReleased(event, childX, childY)) {
            return false;
          }
        }
      }
    }
    return this.tableMatrix.onMouseReleased(event, x, y);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.renderer.RCollection#getRenderables()
   */
  public Iterator<@NonNull ? extends Renderable> getRenderables(final boolean topFirst) {
    final Collection<@NonNull PositionedRenderable> prs = this.positionedRenderables;
    if (prs != null) {
      final List<@NonNull Renderable> c = new java.util.LinkedList<>();
      final Iterator<@NonNull PositionedRenderable> i = prs.iterator();
      while (i.hasNext()) {
        final PositionedRenderable pr = i.next();
        final BoundableRenderable r = pr.renderable;
        c.add(r);
      }
      final Iterator<@NonNull RAbstractCell> i2 = this.tableMatrix.getCells();
      while (i2.hasNext()) {
        c.add(i2.next());
      }

      final Iterator<@NonNull RTableRowGroup> i3 = this.tableMatrix.getRowGroups();
      while (i3.hasNext()) {
        c.add(i3.next());
      }

      if (topFirst) {
        Collections.reverse(c);
      }

      return c.iterator();
    } else {
      final Iterator<@NonNull Renderable>[] rs = new Iterator[] {this.tableMatrix.getCells(), this.tableMatrix.getRowGroups()};
      return CollectionUtilities.iteratorUnion(rs);
    }
  }

  public void repaint(final ModelNode modelNode) {
    // NOP
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.renderer.RenderableContainer#getBackground()
   */
  public Color getPaintedBackgroundColor() {
    return this.container.getPaintedBackgroundColor();
  }

  private final void addPositionedRenderable(final @NonNull BoundableRenderable renderable, final boolean verticalAlignable, final boolean isFloat, final boolean isFixed) {
    // Expected to be called only in GUI thread.
    SortedSet<@NonNull PositionedRenderable> others = this.positionedRenderables;
    if (others == null) {
      others = new TreeSet<>(new ZIndexComparator());
      this.positionedRenderables = others;
    }
    others.add(new PositionedRenderable(renderable, verticalAlignable, this.otherOrdinal++, isFloat, isFixed, false));
    renderable.setParent(this);
    if (renderable instanceof RUIControl) {
      this.container.addComponent(((RUIControl) renderable).widget.getComponent());
    }
  }

  private void importDelayedPair(final DelayedPair pair) {
    BoundableRenderable r = pair.positionPairChild();
    // final BoundableRenderable r = pair.child;
    this.addPositionedRenderable(r, false, false, pair.isFixed);
  }

  @Override
  public String toString() {
    return "RTable[this=" + System.identityHashCode(this) + ",node=" + this.modelNode + "]";
  }

  private static class LayoutKey {
    public final int availWidth;
    public final int availHeight;
    public final int whitespace;
    public final Font font;
    public final boolean overrideNoWrap;

    public LayoutKey(final int availWidth, final int availHeight, final int whitespace, final Font font, final boolean overrideNoWrap) {
      super();
      this.availWidth = availWidth;
      this.availHeight = availHeight;
      this.whitespace = whitespace;
      this.font = font;
      this.overrideNoWrap = overrideNoWrap;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof LayoutKey)) {
        return false;
      }
      final LayoutKey other = (LayoutKey) obj;
      return (other.availWidth == this.availWidth) && (other.availHeight == this.availHeight) && (other.whitespace == this.whitespace)
          && (other.overrideNoWrap == this.overrideNoWrap) && java.util.Objects.equals(other.font, this.font);
    }

    @Override
    public int hashCode() {
      final Font font = this.font;
      return ((this.availWidth * 1000) + this.availHeight) ^ (font == null ? 0 : font.hashCode()) ^ this.whitespace;
    }
  }

  private static class LayoutValue {
    public final int width;
    public final int height;

    public LayoutValue(final int width, final int height) {
      this.width = width;
      this.height = height;
    }
  }

  @Override
  public void layout(int availWidth, int availHeight, boolean b, boolean c, FloatingBoundsSource source, boolean sizeOnly) {
    this.doLayout(availWidth, availHeight, sizeOnly);
  }

}
