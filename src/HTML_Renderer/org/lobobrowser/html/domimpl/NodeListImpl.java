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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NodeListImpl extends AbstractScriptableDelegate implements NodeList {
  // Note: class must be public for reflection to work.
  private final ArrayList<Node> nodeList = new ArrayList<>();

  public NodeListImpl(final Collection<Node> collection) {
    super();
    nodeList.addAll(collection);
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
}
