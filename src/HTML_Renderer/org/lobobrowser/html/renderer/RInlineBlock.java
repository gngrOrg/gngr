/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2014 Uproot Labs India Pvt Ltd

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

 */
package org.lobobrowser.html.renderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.Iterator;

import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.domimpl.HTMLElementImpl;
import org.lobobrowser.html.domimpl.ModelNode;
import org.lobobrowser.ua.UserAgentContext;
import org.lobobrowser.util.CollectionUtilities;

public class RInlineBlock extends BaseElementRenderable {
  private final RBlock child;

  public RInlineBlock(final RenderableContainer container, final HTMLElementImpl modelNode, final UserAgentContext uacontext,
      final HtmlRendererContext rendererContext, final FrameContext frameContext) {
    super(container, modelNode, uacontext);
    final RBlock child = new RBlock(modelNode, 0, userAgentContext, rendererContext, frameContext, this);
    child.setOriginalParent(this);
    child.setParent(this);
    this.child = child;
  }

  public void assignDimension() {
    this.width = child.getWidth();
    this.height = child.getHeight();
  }

  public Iterator<? extends Renderable> getRenderables() {
    return CollectionUtilities.singletonIterator((Renderable) this.child);
  }

  public RenderableSpot getLowestRenderableSpot(final int x, final int y) {
    return this.child.getLowestRenderableSpot(x, y);
  }

  public boolean onDoubleClick(final MouseEvent event, final int x, final int y) {
    return this.child.onDoubleClick(event, x, y);
  }

  public boolean onMouseClick(final MouseEvent event, final int x, final int y) {
    return this.child.onMouseClick(event, x, y);
  }

  public boolean onMouseDisarmed(final MouseEvent event) {
    return this.child.onMouseDisarmed(event);
  }

  public boolean onMousePressed(final MouseEvent event, final int x, final int y) {
    return this.child.onMousePressed(event, x, y);
  }

  public boolean onMouseReleased(final MouseEvent event, final int x, final int y) {
    return this.child.onMouseReleased(event, x, y);
  }

  @Override
  public void paint(final Graphics g) {
    this.child.paint(g);
  }

  @Override
  public int getVAlign() {
    // Not used
    return VALIGN_BASELINE;
  }

  @Override
  public void repaint(final ModelNode modelNode) {
    this.child.repaint(modelNode);
  }

  @Override
  public Color getPaintedBackgroundColor() {
    return this.backgroundColor;
  }

  @Override
  protected void doLayout(final int availWidth, final int availHeight, final boolean sizeOnly) {
    this.child.layout(availWidth, availHeight, false, false, null, sizeOnly);
    assignDimension();
  }

  @Override
  public Component addComponent(final Component component) {
    this.container.addComponent(component);
    return super.addComponent(component);
  }

  @Override
  protected void doLayout(int availWidth, int availHeight, boolean expand, boolean sizeOnly) {
    this.child.doLayout(availWidth, availHeight, expand, expand, null, 0, 0, sizeOnly, true);
  }
}
