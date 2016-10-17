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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.html.domimpl.ModelNode;
import org.lobobrowser.html.domimpl.NodeImpl;
import org.lobobrowser.html.domimpl.UINode;
import org.lobobrowser.html.style.RenderState;
import org.lobobrowser.ua.UserAgentContext;

import cz.vutbr.web.css.CSSProperty.VerticalAlign;

/**
 * @author J. H. S.
 */
class RUIControl extends BaseElementRenderable {
  private static final int MAX_CACHE_SIZE = 10;
  public final UIControl widget;
  private final FrameContext frameContext;

  public RUIControl(final ModelNode me, final UIControl widget, final RenderableContainer container, final FrameContext frameContext,
      final UserAgentContext ucontext) {
    super(container, me, ucontext);
    this.widget = widget;
    this.frameContext = frameContext;
    widget.setRUIControl(this);
  }

  @Override
  public void focus() {
    super.focus();
    final java.awt.Component c = this.widget.getComponent();
    c.requestFocus();
  }

  @Override
  public final void invalidateLayoutLocal() {
    // Invalidate widget (some redundancy)
    super.invalidateLayoutLocal();
    this.widget.invalidate();
    // Invalidate cached values
    this.cachedLayout.clear();
    this.lastLayoutKey = null;
    this.lastLayoutValue = null;
  }

  public VerticalAlign getVAlign() {
    return this.widget.getVAlign();
  }

  public boolean hasBackground() {
    return (this.backgroundColor != null) || (this.backgroundImage != null) || (this.lastBackgroundImageUri != null);
  }

  @Override
  public final void paintShifted(final Graphics g) {
    final RenderState rs = this.modelNode.getRenderState();
    if ((rs != null) && (rs.getVisibility() != RenderState.VISIBILITY_VISIBLE)) {
      // Just don't paint it.
      return;
    }
    // Prepaint borders, background images, etc.
    this.prePaint(g);
    // We need to paint the GUI component.
    // For various reasons, we need to do that
    // instead of letting AWT do it.
    final Insets insets = this.getBorderInsets();
    g.translate(insets.left, insets.top);
    try {
      this.widget.paint(g);
    } finally {
      g.translate(-insets.left, -insets.top);
    }
  }

  public boolean onMouseClick(final java.awt.event.MouseEvent event, final int x, final int y) {
    final ModelNode me = this.modelNode;
    if (me != null) {
      return HtmlController.getInstance().onMouseClick(me, event, x, y);
    } else {
      return true;
    }
  }

  public boolean onDoubleClick(final java.awt.event.MouseEvent event, final int x, final int y) {
    final ModelNode me = this.modelNode;
    if (me != null) {
      return HtmlController.getInstance().onDoubleClick(me, event, x, y);
    } else {
      return true;
    }
  }

  public boolean onMousePressed(final java.awt.event.MouseEvent event, final int x, final int y) {
    final ModelNode me = this.modelNode;
    if (me != null) {
      return HtmlController.getInstance().onMouseDown(me, event, x, y);
    } else {
      return true;
    }
  }

  public boolean onMouseReleased(final java.awt.event.MouseEvent event, final int x, final int y) {
    final ModelNode me = this.modelNode;
    if (me != null) {
      return HtmlController.getInstance().onMouseUp(me, event, x, y);
    } else {
      return true;
    }
  }

  public boolean onMouseDisarmed(final java.awt.event.MouseEvent event) {
    final ModelNode me = this.modelNode;
    if (me != null) {
      return HtmlController.getInstance().onMouseDisarmed(me, event);
    } else {
      return true;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.html.renderer.BoundableRenderable#invalidateState(org.xamjwg
   * .html.renderer.RenderableContext)
   */
  public void invalidateRenderStyle() {
    // NOP - No RenderStyle below this node.
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.html.domimpl.ContainingBlockContext#repaint(org.xamjwg.html.
   * renderer.RenderableContext)
   */
  public void repaint(final ModelNode modelNode) {
    final Object widget = this.widget;
    if (widget instanceof UINode) {
      ((UINode) widget).repaint(modelNode);
    } else {
      this.repaint();
    }
  }

  @Override
  public void updateWidgetBounds(final int guiX, final int guiY) {
    // Overrides
    super.updateWidgetBounds(guiX, guiY);
    final Insets insets = this.getBorderInsets();
    this.widget.setBounds(guiX + insets.left, guiY + insets.top, this.width - insets.left - insets.right, this.height - insets.top
        - insets.bottom);
  }

  @Override
  public Color getBlockBackgroundColor() {
    return this.widget.getBackgroundColor();
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.html.renderer.BoundableRenderable#paintSelection(java.awt.Graphics
   * , boolean, org.xamjwg.html.renderer.RenderablePoint,
   * org.xamjwg.html.renderer.RenderablePoint)
   */
  @Override
  public boolean paintSelection(final Graphics g, boolean inSelection, final RenderableSpot startPoint, final RenderableSpot endPoint) {
    inSelection = super.paintSelection(g, inSelection, startPoint, endPoint);
    if (inSelection) {
      final Color over = new Color(0, 0, 255, 50);
      final Color oldColor = g.getColor();
      try {
        g.setColor(over);
        g.fillRect(0, 0, this.width, this.height);
      } finally {
        g.setColor(oldColor);
      }
    }
    return inSelection;
  }

  @Override
  public boolean extractSelectionText(final StringBuffer buffer, final boolean inSelection, final RenderableSpot startPoint,
      final RenderableSpot endPoint) {
    // No text here
    return inSelection;
  }

  public RenderableSpot getLowestRenderableSpot(final int x, final int y) {
    // Nothing draggable - return self
    return new RenderableSpot(this, x, y);
  }

  private int declaredWidth = -1;
  private int declaredHeight = -1;
  private LayoutKey lastLayoutKey = null;
  private LayoutValue lastLayoutValue = null;
  private final Map<LayoutKey, LayoutValue> cachedLayout = new HashMap<>(5);

  @Override
  public void doLayout(final int availWidth, final int availHeight, final boolean sizeOnly) {
    final Map<LayoutKey, LayoutValue> cachedLayout = this.cachedLayout;
    final RenderState rs = this.modelNode.getRenderState();
    final int whitespace = rs == null ? RenderState.WS_NORMAL : rs.getWhiteSpace();
    final Font font = rs == null ? null : rs.getFont();
    final LayoutKey layoutKey = new LayoutKey(availWidth, availHeight, whitespace, font);
    LayoutValue layoutValue;
    if (sizeOnly) {
      layoutValue = cachedLayout.get(layoutKey);
    } else {
      if (java.util.Objects.equals(this.lastLayoutKey, layoutKey)) {
        layoutValue = this.lastLayoutValue;
      } else {
        layoutValue = null;
      }
    }
    if (layoutValue == null) {
      this.applyStyle(availWidth, availHeight);

      final UIControl widget = this.widget;
      widget.reset(availWidth, availHeight);

      final RenderState renderState = this.modelNode.getRenderState();
      Insets paddingInsets = this.paddingInsets;
      if (paddingInsets == null) {
        paddingInsets = RBlockViewport.ZERO_INSETS;
      }
      Insets borderInsets = this.borderInsets;
      if (borderInsets == null) {
        borderInsets = RBlockViewport.ZERO_INSETS;
      }
      Insets marginInsets = this.marginInsets;
      if (marginInsets == null) {
        marginInsets = RBlockViewport.ZERO_INSETS;
      }

      final int actualAvailWidth = availWidth - paddingInsets.left - paddingInsets.right - borderInsets.left - borderInsets.right
          - marginInsets.left - marginInsets.right;
      final int actualAvailHeight = availHeight - paddingInsets.top - paddingInsets.bottom - borderInsets.top - borderInsets.bottom
          - marginInsets.top - marginInsets.bottom;
      final Integer dw = this.getDeclaredWidth(renderState, actualAvailWidth);
      final Integer dh = this.getDeclaredHeight(renderState, actualAvailHeight);
      final int declaredWidth = dw == null ? -1 : dw.intValue();
      final int declaredHeight = dh == null ? -1 : dh.intValue();
      this.declaredWidth = declaredWidth;
      this.declaredHeight = declaredHeight;

      this.widthConstrained = declaredWidth != -1;
      this.heightConstrained = declaredHeight != -1;

      final Insets insets = this.getInsets(false, false);
      int finalWidth = declaredWidth == -1 ? -1 : declaredWidth + insets.left + insets.right;
      int finalHeight = declaredHeight == -1 ? -1 : declaredHeight + insets.top + insets.bottom;
      if ((finalWidth == -1) || (finalHeight == -1)) {
        final Dimension size = widget.getPreferredSize();
        if (finalWidth == -1) {
          finalWidth = size.width + insets.left + insets.right;
        }
        if (finalHeight == -1) {
          finalHeight = size.height + insets.top + insets.bottom;
        }
      }

      {
        final Integer maxWidth = getDeclaredMaxWidth(renderState, actualAvailWidth);
        if (maxWidth != null) {
          if (finalWidth > maxWidth) {
            finalWidth = maxWidth;
            widthConstrained = true;
          }
        }
      }
      {
        final Integer minWidth = getDeclaredMinWidth(renderState, actualAvailWidth);
        if (minWidth != null) {
          if (finalWidth < minWidth) {
            finalWidth = minWidth;
            widthConstrained = true;
          }
        }
      }

      {
        final Integer maxHeight = getDeclaredMaxHeight(renderState, actualAvailHeight);
        if (maxHeight != null) {
          if (finalHeight > maxHeight) {
            finalHeight = maxHeight;
            heightConstrained = true;
          }
        }
      }

      {
        final Integer minHeight = getDeclaredMinHeight(renderState, actualAvailHeight);
        if (minHeight != null) {
          if (finalHeight < minHeight) {
            finalHeight = minHeight;
            heightConstrained = true;
          }
        }
      }

      layoutValue = new LayoutValue(finalWidth, finalHeight);
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
  }

  /**
   * May be called by controls when they wish to modifiy their preferred size
   * (e.g. an image after it's loaded). This method must be called in the GUI
   * thread.
   */
  public final void preferredSizeInvalidated() {
    final int dw = RUIControl.this.declaredWidth;
    final int dh = RUIControl.this.declaredHeight;
    if ((dw == -1) || (dh == -1)) {
      this.frameContext.delayedRelayout((NodeImpl) this.modelNode);
    } else {
      RUIControl.this.repaint();
    }
  }

  public Iterator<@NonNull Renderable> getRenderables(final boolean topFirst) {
    // No children for GUI controls
    return null;
  }

  public Color getPaintedBackgroundColor() {
    return this.container.getPaintedBackgroundColor();
  }

  public Color getForegroundColor() {
    final RenderState rs = this.modelNode.getRenderState();
    return rs == null ? null : rs.getColor();
  }

  private static class LayoutKey {
    public final int availWidth;
    public final int availHeight;
    public final int whitespace;
    public final Font font;

    public LayoutKey(final int availWidth, final int availHeight, final int whitespace, final Font font) {
      this.availWidth = availWidth;
      this.availHeight = availHeight;
      this.whitespace = whitespace;
      this.font = font;
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
          && java.util.Objects.equals(other.font, this.font);
    }

    @Override
    public int hashCode() {
      final Font font = this.font;
      return ((this.availWidth * 1000) + this.availHeight) ^ (font == null ? 0 : font.hashCode());
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

  private boolean widthConstrained = false;
  private boolean heightConstrained = false;

  protected boolean isWidthConstrained() {
    return widthConstrained;
  }

  protected boolean isHeightConstrained() {
    return heightConstrained;
  }

  @Override
  public void setInnerWidth(Integer newWidth) {
    super.setInnerWidth(newWidth);
    widthConstrained = true;
  }

  @Override
  public void setInnerHeight(Integer newHeight) {
    super.setInnerHeight(newHeight);
    heightConstrained = true;
  }
}
