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

import org.w3c.dom.Document;
import org.w3c.dom.UserDataHandler;

public class HTMLTitleElementImpl extends HTMLElementImpl {
  public HTMLTitleElementImpl(final String name) {
    super(name, true);
  }

  @Override
  public Object setUserData(final String key, final Object data, final UserDataHandler handler) {
    if (org.lobobrowser.html.parser.HtmlParser.MODIFYING_KEY.equals(key) && (data == Boolean.FALSE)) {
      final Document document = this.document;
      if (document instanceof HTMLDocumentImpl) {
        final String textContent = this.getTextContent();
        final String title = textContent == null ? null : textContent.trim();
        ((HTMLDocumentImpl) document).setTitle(title);
      }
    }
    return super.setUserData(key, data, handler);
  }

}
