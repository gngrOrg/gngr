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
import java.util.Collection;

import org.lobobrowser.js.AbstractScriptableDelegate;
import org.lobobrowser.js.JavaScript;
import org.lobobrowser.util.Objects;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// TODO: This needs to be live (dynamic) not a static store of nodes.
public class NodeListImpl extends AbstractScriptableDelegate implements NodeList {
  // Note: class must be public for reflection to work.
  private final ArrayList<Node> nodeList;

  // TODO: Add more constructors that take arrays for example
  public NodeListImpl(final Collection<Node> collection) {
    super();
    nodeList = new ArrayList<>(collection);
  }

  public int getLength() {
    return this.nodeList.size();
  }

  public Node item(final int index) {
    try {
      return this.nodeList.get(index);
    } catch (final IndexOutOfBoundsException iob) {
      return null;
    }
  }

  // TODO: This needs to be handled in a general fashion. GH #123
  public boolean hasOwnProperty(final Object obj) {
    if (Objects.isAssignableOrBox(obj, Integer.TYPE)) {
      final Integer i = (Integer) JavaScript.getInstance().getJavaObject(obj, Integer.TYPE);
      return i < getLength();
    } else {
      return false;
    }
  }

  /* Described here: http://www.w3.org/TR/dom/#dom-htmlcollection-nameditem. This actually needs to be in a separate class that implements HTMLCollection */
  public Node namedItem(final String key) {
    final int length = getLength();
    for (int i = 0; i < length; i++) {
      final Node n = item(0);
      if (n instanceof Element) {
        final Element element = (Element) n;
        if (key.equals(element.getAttribute("id")) || key.equals(element.getAttribute("name"))) {
          return n;
        }
      }

    }
    return null;
  }

  @Override
  public String toString() {
    return nodeList.toString();
  }
}
