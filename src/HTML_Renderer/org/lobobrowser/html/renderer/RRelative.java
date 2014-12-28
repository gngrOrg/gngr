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

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.Iterator;

import org.lobobrowser.html.domimpl.ModelNode;
import org.lobobrowser.util.CollectionUtilities;

public class RRelative extends BaseRCollection {
  private final RElement child;
  private final int xoffset;
  private final int yoffset;

  public RRelative(final RenderableContainer container, final ModelNode modelNode, final RElement child, final int xoffset,
      final int yoffset) {
    super(container, modelNode);
    child.setOriginalParent(this);
    child.setParent(this);
    child.setOrigin(xoffset, yoffset);
    this.child = child;
    this.xoffset = xoffset;
    this.yoffset = yoffset;

    // without this, the UI controls don't display nor interact.
    if (child instanceof RUIControl) {
      this.container.addComponent(((RUIControl) child).widget.getComponent());
    }
  }

  public void assignDimension() {
    final RElement child = this.child;
    this.width = child.getWidth();
    this.height = child.getHeight();
  }

  public FloatingInfo getExportableFloatingInfo() {
    final RElement child = this.child;
    if (child instanceof RBlock) {
      final FloatingInfo floatingInfo = ((RBlock) child).getExportableFloatingInfo();
      if (floatingInfo == null) {
        return null;
      } else {
        return new FloatingInfo(xoffset + floatingInfo.shiftX, yoffset + floatingInfo.shiftY, floatingInfo.floats);
      }
    } else {
      return null;
    }
  }

  // public void adjust() {
  // RElement child = this.child;
  // if(child instanceof RBlock) {
  // ((RBlock) child).adjust();
  // }
  // }
  //
  // public FloatingBounds getExportableFloatingBounds() {
  // RElement child = this.child;
  // if(!(child instanceof RBlock)) {
  // return null;
  // }
  // FloatingBounds blockBounds = ((RBlock)
  // child).getExportableFloatingBounds();
  // if(blockBounds == null) {
  // return null;
  // }
  // return new ShiftedFloatingBounds(blockBounds, this.xoffset, -this.xoffset,
  // this.yoffset);
  // }
  //
  public RElement getElement() {
    return this.child;
  }

  public int getXOffset() {
    return xoffset;
  }

  public int getYOffset() {
    return yoffset;
  }

  public Iterator<? extends Renderable> getRenderables() {
    return CollectionUtilities.singletonIterator((Renderable) this.child);
  }

  @Override
  protected void invalidateLayoutLocal() {
    // nop
  }

  public RenderableSpot getLowestRenderableSpot(final int x, final int y) {
    return this.child.getLowestRenderableSpot(x - this.xoffset, y - this.yoffset);
  }

  public boolean isContainedByNode() {
    return true;
  }

  public boolean onDoubleClick(final MouseEvent event, final int x, final int y) {
    return this.child.onDoubleClick(event, x - this.xoffset, y - this.yoffset);
  }

  public boolean onMouseClick(final MouseEvent event, final int x, final int y) {
    return this.child.onMouseClick(event, x - this.xoffset, y - this.yoffset);
  }

  public boolean onMouseDisarmed(final MouseEvent event) {
    return this.child.onMouseDisarmed(event);
  }

  public boolean onMousePressed(final MouseEvent event, final int x, final int y) {
    return this.child.onMousePressed(event, x - this.xoffset, y - this.yoffset);
  }

  public boolean onMouseReleased(final MouseEvent event, final int x, final int y) {
    return this.child.onMouseReleased(event, x - this.xoffset, y - this.yoffset);
  }

  public void paint(final Graphics g) {
    this.child.paintTranslated(g);
  }


  public void layout(int availWidth, int availHeight, boolean sizeOnly) {
    child.layout(availWidth, availHeight, false, sizeOnly);
    assignDimension();
  }
}
