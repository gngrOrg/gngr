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
package org.lobobrowser.html.domimpl;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.html.style.BaseFontRenderState;
import org.lobobrowser.html.style.HtmlValues;
import org.lobobrowser.html.style.RenderState;
import org.w3c.dom.html.HTMLBaseFontElement;

public class HTMLBaseFontElementImpl extends HTMLAbstractUIElement implements HTMLBaseFontElement {
  public HTMLBaseFontElementImpl(final String name) {
    super(name);
  }

  public String getColor() {
    return this.getAttribute("color");
  }

  public String getFace() {
    return this.getAttribute("face");
  }

  public void setColor(final String color) {
    this.setAttribute("color", color);
  }

  public void setFace(final String face) {
    this.setAttribute("face", face);
  }

  public String getSize() {
    return this.getAttribute("size");
  }

  public void setSize(final String size) {
    this.setAttribute("size", size);
  }

  @Override
  protected @NonNull RenderState createRenderState(RenderState prevRenderState) {
    final String size = this.getAttribute("size");
    if (size != null) {
      final int fontNumber = HtmlValues.getFontNumberOldStyle(size, prevRenderState);
      // TODO: Check why the following call is not used.
      // final float fontSize = HtmlValues.getFontSize(fontNumber);
      prevRenderState = new BaseFontRenderState(prevRenderState, fontNumber);
    }
    return super.createRenderState(prevRenderState);
  }

}
