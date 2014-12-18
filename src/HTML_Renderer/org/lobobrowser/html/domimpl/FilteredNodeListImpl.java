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
 * Created on Oct 9, 2005
 */
package org.lobobrowser.html.domimpl;

import java.util.Collection;
import java.util.Iterator;

import org.lobobrowser.js.AbstractScriptableDelegate;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// TODO: Use lambdas?
class FilteredNodeListImpl extends AbstractScriptableDelegate implements NodeList {
  private final Collection<Node> sourceNodeList;
  private final NodeFilter filter;
  private final Object lock;

  /**
   * @param filter
   * @param list
   */
  public FilteredNodeListImpl(final NodeFilter filter, final Collection<Node> list, final Object lock) {
    super();
    this.filter = filter;
    sourceNodeList = list;
    this.lock = lock;
  }

  public Node item(final int index) {
    synchronized (this.lock) {
      int count = 0;
      final Iterator<Node> i = this.sourceNodeList.iterator();
      while (i.hasNext()) {
        final Node node = i.next();
        if (this.filter.accept(node)) {
          if (count == index) {
            return node;
          }
          count++;
        }
      }
      return null;
    }
  }

  public int getLength() {
    synchronized (this.lock) {
      int count = 0;
      final Iterator<Node> i = this.sourceNodeList.iterator();
      while (i.hasNext()) {
        final Node node = i.next();
        if (this.filter.accept(node)) {
          count++;
        }
      }
      return count;
    }
  }
}
