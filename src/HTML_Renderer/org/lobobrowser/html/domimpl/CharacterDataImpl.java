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
 * Created on Sep 3, 2005
 */
package org.lobobrowser.html.domimpl;

import org.w3c.dom.CharacterData;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

public abstract class CharacterDataImpl extends NodeImpl implements CharacterData {
  protected volatile String text;

  public CharacterDataImpl() {
    super();
  }

  public CharacterDataImpl(final String text) {
    this.text = text;
  }

  public String getClassName() {
    return "HTMLCharacterData";
  }

  @Override
  public String getTextContent() throws DOMException {
    return this.text;
  }

  @Override
  public void setTextContent(final String textContent) throws DOMException {
    this.text = textContent;
    if (!this.notificationsSuspended) {
      this.informInvalid();
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.NodeImpl#cloneNode(boolean)
   */
  @Override
  public Node cloneNode(final boolean deep) {
    final CharacterDataImpl newNode = (CharacterDataImpl) super.cloneNode(deep);
    newNode.setData(this.getData());
    return newNode;
  }

  public void appendData(final String arg) throws DOMException {
    this.text += arg;
    if (!this.notificationsSuspended) {
      this.informInvalid();
    }
  }

  public void deleteData(final int offset, final int count) throws DOMException {
    final StringBuffer buffer = new StringBuffer(this.text);
    final StringBuffer result = buffer.delete(offset, offset + count);
    this.text = result.toString();
    if (!this.notificationsSuspended) {
      this.informInvalid();
    }
  }

  public String getData() throws DOMException {
    return this.text;
  }

  public int getLength() {
    return this.text.length();
  }

  public void insertData(final int offset, final String arg) throws DOMException {
    final StringBuffer buffer = new StringBuffer(this.text);
    final StringBuffer result = buffer.insert(offset, arg);
    this.text = result.toString();
    if (!this.notificationsSuspended) {
      this.informInvalid();
    }
  }

  public void replaceData(final int offset, final int count, final String arg) throws DOMException {
    final StringBuffer buffer = new StringBuffer(this.text);
    final StringBuffer result = buffer.replace(offset, offset + count, arg);
    this.text = result.toString();
    if (!this.notificationsSuspended) {
      this.informInvalid();
    }
  }

  public void setData(final String data) throws DOMException {
    this.text = data;
    if (!this.notificationsSuspended) {
      this.informInvalid();
    }
  }

  public String substringData(final int offset, final int count) throws DOMException {
    return this.text.substring(offset, offset + count);
  }

  @Override
  public String toString() {
    String someText = this.text;
    if ((someText != null) && (someText.length() > 32)) {
      someText = someText.substring(0, 29) + "...";
    }
    final int length = someText == null ? 0 : someText.length();
    return this.getNodeName() + "[length=" + length + ",text=" + someText + "]";
  }

}
