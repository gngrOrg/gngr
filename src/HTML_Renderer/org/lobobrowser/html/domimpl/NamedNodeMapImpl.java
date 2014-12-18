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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.lobobrowser.js.AbstractScriptableDelegate;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class NamedNodeMapImpl extends AbstractScriptableDelegate implements NamedNodeMap {
  // Note: class must be public for reflection to work.
  private final Map<String, Node> attributes = new HashMap<>();
  private final ArrayList<Node> attributeList = new ArrayList<>();

  public NamedNodeMapImpl(final Element owner, final Map<String, String> attribs) {
    attribs.forEach((name, value) -> {
      // TODO: "specified" attributes
      final Attr attr = new AttrImpl(name, value, true, owner, "ID".equals(name));
      this.attributes.put(name, attr);
      this.attributeList.add(attr);
    });
  }

  public int getLength() {
    return this.attributeList.size();
  }

  public Node getNamedItem(final String name) {
    return this.attributes.get(name);
  }

  /**
   * @param name
   */
  public Node namedItem(final String name) {
    // Method needed for Javascript indexing.
    return this.getNamedItem(name);
  }

  public Node getNamedItemNS(final String namespaceURI, final String localName) throws DOMException {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "No namespace support");
  }

  public Node item(final int index) {
    try {
      return this.attributeList.get(index);
    } catch (final IndexOutOfBoundsException iob) {
      return null;
    }
  }

  public Node removeNamedItem(final String name) throws DOMException {
    return this.attributes.remove(name);
  }

  public Node removeNamedItemNS(final String namespaceURI, final String localName) throws DOMException {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "No namespace support");
  }

  public Node setNamedItem(final Node arg) throws DOMException {
    final Object prevValue = this.attributes.put(arg.getNodeName(), arg);
    if (prevValue != null) {
      this.attributeList.remove(prevValue);
    }
    this.attributeList.add(arg);
    return arg;
  }

  public Node setNamedItemNS(final Node arg) throws DOMException {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "No namespace support");
  }
}
