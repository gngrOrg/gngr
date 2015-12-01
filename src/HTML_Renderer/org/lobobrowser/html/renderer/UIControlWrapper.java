package org.lobobrowser.html.renderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

import org.lobobrowser.html.HtmlObject;

class UIControlWrapper implements UIControl {
  private final Component component;
  private final HtmlObject htmlObject;

  public UIControlWrapper(final HtmlObject ho) {
    this.htmlObject = ho;
    Component c;
    if (ho == null) {
      c = new BrokenComponent();
    } else {
      c = ho.getComponent();
    }
    this.component = c;
  }

  public void reset(final int availWidth, final int availHeight) {
    this.htmlObject.reset(availWidth, availHeight);
  }

  public Component getComponent() {
    return this.component;
  }

  public Color getBackgroundColor() {
    return this.component.getBackground();
  }

  public Dimension getPreferredSize() {
    return this.component.getPreferredSize();
  }

  public void invalidate() {
    // Calls its AWT parent's invalidate, but I guess that's OK.
    this.component.invalidate();
  }

  public boolean paintSelection(final Graphics g, final boolean inSelection, final RenderableSpot startPoint, final RenderableSpot endPoint) {
    // Does not paint selection
    return inSelection;
  }

  public void setBounds(final int x, final int y, final int width, final int height) {
    this.component.setBounds(x, y, width, height);
  }

  public void setRUIControl(final RUIControl ruicontrol) {
    // Not doing anything with this.
  }

  public void paint(final Graphics g) {
    this.component.paint(g);
  }
}
