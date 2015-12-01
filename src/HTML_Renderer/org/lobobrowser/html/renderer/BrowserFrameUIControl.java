package org.lobobrowser.html.renderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;

import org.lobobrowser.html.BrowserFrame;
import org.lobobrowser.html.domimpl.ModelNode;
import org.lobobrowser.html.style.HtmlInsets;
import org.lobobrowser.html.style.HtmlValues;
import org.lobobrowser.html.style.RenderState;
import org.w3c.dom.html.HTMLElement;

class BrowserFrameUIControl implements UIControl {
  private final Component component;
  private final HTMLElement element;
  private final BrowserFrame browserFrame;
  private RUIControl ruiControl;

  public BrowserFrameUIControl(final HTMLElement element, final BrowserFrame browserFrame) {
    this.component = browserFrame.getComponent();
    this.browserFrame = browserFrame;
    this.element = element;
  }

  public Color getBackgroundColor() {
    return this.component.getBackground();
  }

  public Component getComponent() {
    return this.component;
  }

  private int availWidth;
  private int availHeight;

  public void reset(final int availWidth, final int availHeight) {
    this.availWidth = availWidth;
    this.availHeight = availHeight;
    final RUIControl ruiControl = this.ruiControl;
    if (ruiControl != null) {
      final ModelNode node = ruiControl.getModelNode();
      final HTMLElement element = (HTMLElement) node;
      final RenderState renderState = node.getRenderState();
      HtmlInsets insets = null;
      String marginwidth = element.getAttribute("marginwidth");
      String marginheight = element.getAttribute("marginheight");
      if ((marginwidth != null) && (marginwidth.length() != 0)) {
        insets = new HtmlInsets();
        marginwidth = marginwidth.trim();
        if (marginwidth.endsWith("%")) {
          int value;
          try {
            value = Integer.parseInt(marginwidth.substring(0, marginwidth.length() - 1));
          } catch (final NumberFormatException nfe) {
            value = 0;
          }
          insets.left = value;
          insets.right = value;
          insets.leftType = HtmlInsets.TYPE_PERCENT;
          insets.rightType = HtmlInsets.TYPE_PERCENT;
        } else {
          int value;
          try {
            value = Integer.parseInt(marginwidth);
          } catch (final NumberFormatException nfe) {
            value = 0;
          }
          insets.left = value;
          insets.right = value;
          insets.leftType = HtmlInsets.TYPE_PIXELS;
          insets.rightType = HtmlInsets.TYPE_PIXELS;
        }
      }
      if ((marginheight != null) && (marginheight.length() != 0)) {
        if (insets == null) {
          insets = new HtmlInsets();
        }
        marginheight = marginheight.trim();
        if (marginheight.endsWith("%")) {
          int value;
          try {
            value = Integer.parseInt(marginheight.substring(0, marginheight.length() - 1));
          } catch (final NumberFormatException nfe) {
            value = 0;
          }
          insets.top = value;
          insets.bottom = value;
          insets.topType = HtmlInsets.TYPE_PERCENT;
          insets.bottomType = HtmlInsets.TYPE_PERCENT;
        } else {
          int value;
          try {
            value = Integer.parseInt(marginheight);
          } catch (final NumberFormatException nfe) {
            value = 0;
          }
          insets.top = value;
          insets.bottom = value;
          insets.topType = HtmlInsets.TYPE_PIXELS;
          insets.bottomType = HtmlInsets.TYPE_PIXELS;
        }
      }
      final Insets awtMarginInsets = insets == null ? null : insets.getSimpleAWTInsets(availWidth, availHeight);
      final int overflowX = renderState.getOverflowX();
      final int overflowY = renderState.getOverflowY();
      if (awtMarginInsets != null) {
        this.browserFrame.setDefaultMarginInsets(awtMarginInsets);
      }
      if (overflowX != RenderState.OVERFLOW_NONE) {
        this.browserFrame.setDefaultOverflowX(overflowX);
      }
      if (overflowY != RenderState.OVERFLOW_NONE) {
        this.browserFrame.setDefaultOverflowY(overflowY);
      }
    }
  }

  public Dimension getPreferredSize() {
    final int width = HtmlValues.getOldSyntaxPixelSize(element.getAttribute("width"), this.availWidth, 100);
    final int height = HtmlValues.getOldSyntaxPixelSize(element.getAttribute("height"), this.availHeight, 100);
    return new Dimension(width, height);
  }

  public void invalidate() {
    this.component.invalidate();
  }

  public void paint(final Graphics g) {
    // We actually have to paint it.
    this.component.paint(g);
  }

  public boolean paintSelection(final Graphics g, final boolean inSelection, final RenderableSpot startPoint, final RenderableSpot endPoint) {
    // Selection does not cross in here?
    return false;
  }

  public void setBounds(final int x, final int y, final int width, final int height) {
    this.component.setBounds(x, y, width, height);
  }

  public void setRUIControl(final RUIControl ruicontrol) {
    this.ruiControl = ruicontrol;
  }
}
