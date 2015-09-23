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
 * Created on Oct 8, 2005
 */
package org.lobobrowser.html.domimpl;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.html.style.BodyRenderState;
import org.lobobrowser.html.style.RenderState;
import org.mozilla.javascript.Function;
import org.w3c.dom.Document;
import org.w3c.dom.html.HTMLBodyElement;
import org.w3c.dom.html.HTMLDocument;

public class HTMLBodyElementImpl extends HTMLAbstractUIElement implements HTMLBodyElement {
  public HTMLBodyElementImpl(final String name) {
    super(name);
  }

  @Override
  void setOwnerDocument(final Document value, final boolean deep) {
    super.setOwnerDocument(value, deep);
    if (value instanceof HTMLDocument) {
      ((HTMLDocument) value).setBody(this);
    }
  }

  @Override
  void setOwnerDocument(final Document value) {
    super.setOwnerDocument(value);
    if (value instanceof HTMLDocument) {
      ((HTMLDocument) value).setBody(this);
    }
  }

  public String getALink() {
    return this.getAttribute("alink");
  }

  public void setALink(final String aLink) {
    this.setAttribute("alink", aLink);
  }

  public String getBackground() {
    return this.getAttribute("background");
  }

  public void setBackground(final String background) {
    this.setAttribute("background", background);
  }

  public String getBgColor() {
    return this.getAttribute("bgcolor");
  }

  public void setBgColor(final String bgColor) {
    this.setAttribute("bgcolor", bgColor);
  }

  public String getLink() {
    return this.getAttribute("link");
  }

  public void setLink(final String link) {
    this.setAttribute("link", link);
  }

  public String getText() {
    return this.getAttribute("text");
  }

  public void setText(final String text) {
    this.setAttribute("text", text);
  }

  public String getVLink() {
    return this.getAttribute("vlink");
  }

  public void setVLink(final String vLink) {
    this.setAttribute("vlink", vLink);
  }

  @Override
  protected @NonNull RenderState createRenderState(final RenderState prevRenderState) {
    return new BodyRenderState(prevRenderState, this);
  }

  public Function getOnload() {
    final Object document = this.document;
    if (document instanceof HTMLDocumentImpl) {
      return ((HTMLDocumentImpl) document).getOnloadHandler();
    } else {
      return null;
    }
  }

  public void setOnload(final Function onload) {
    final Object document = this.document;
    if (document instanceof HTMLDocumentImpl) {
      // Note that body.onload overrides
      // Window.onload.
      ((HTMLDocumentImpl) document).setOnloadHandler(onload);
    }
  }

  /*
  @Override
  protected void assignAttributeField(final String normalName, final String value) {
    if ("onload".equals(normalName)) {
      final Function onload = this.getEventFunction(null, normalName);
      if (onload != null) {
        this.setOnload(onload);
      }
    } else {
      super.assignAttributeField(normalName, value);
    }
  }*/

  @Override
  protected void handleAttributeChanged(String name, String oldValue, String newValue) {
    super.handleAttributeChanged(name, oldValue, newValue);
    if ("onload".equals(name)) {
      final Function onload = this.getEventFunction(null, name);
      if (onload != null) {
        this.setOnload(onload);
      }
    }
  }
}
