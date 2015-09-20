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
package org.lobobrowser.html.renderer;

import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.domimpl.HTMLElementImpl;
import org.lobobrowser.html.domimpl.NodeImpl;
import org.lobobrowser.html.style.HtmlValues;
import org.lobobrowser.html.style.JStyleProperties;
import org.lobobrowser.html.style.ListStyle;
import org.lobobrowser.ua.UserAgentContext;

class BaseRListElement extends RBlock {
  protected static final String DEFAULT_COUNTER_NAME = "$cobra.counter";
  protected ListStyle listStyle = null;

  public BaseRListElement(final NodeImpl modelNode, final int listNesting, final UserAgentContext pcontext,
      final HtmlRendererContext rcontext,
      final FrameContext frameContext, final RenderableContainer parentContainer) {
    super(modelNode, listNesting, pcontext, rcontext, frameContext, parentContainer);
  }

  @Override
  protected void applyStyle(final int availWidth, final int availHeight, final boolean updateLayout) {
    this.listStyle = null;
    super.applyStyle(availWidth, availHeight, updateLayout);
    final Object rootNode = this.modelNode;
    if (!(rootNode instanceof HTMLElementImpl)) {
      return;
    }
    final HTMLElementImpl rootElement = (HTMLElementImpl) rootNode;
    final JStyleProperties props = rootElement.getCurrentStyle();
    ListStyle listStyle = null;
    final String listStyleText = props.getListStyle();
    if (listStyleText != null) {
      listStyle = HtmlValues.getListStyle(listStyleText);
    }
    final String listStyleTypeText = props.getListStyleType();
    if (listStyleTypeText != null) {
      final int listType = HtmlValues.getListStyleType(listStyleTypeText);
      if (listType != ListStyle.TYPE_UNSET) {
        if (listStyle == null) {
          listStyle = new ListStyle();
        }
        listStyle.type = listType;
      }
    }
    if ((listStyle == null) || (listStyle.type == ListStyle.TYPE_UNSET)) {
      final String typeAttributeText = rootElement.getAttribute("type");
      if (typeAttributeText != null) {
        final int newStyleType = HtmlValues.getListStyleTypeDeprecated(typeAttributeText);
        if (newStyleType != ListStyle.TYPE_UNSET) {
          if (listStyle == null) {
            listStyle = new ListStyle();
            this.listStyle = listStyle;
          }
          listStyle.type = newStyleType;
        }
      }
    }
    this.listStyle = listStyle;
  }

  @Override
  public String toString() {
    return "BaseRListElement[node=" + this.modelNode + "]";
  }
}
