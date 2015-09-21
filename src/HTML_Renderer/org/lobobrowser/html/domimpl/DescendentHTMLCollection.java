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
 * Created on Dec 3, 2005
 */
package org.lobobrowser.html.domimpl;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lobobrowser.js.AbstractScriptableDelegate;
import org.lobobrowser.js.JavaScript;
import org.lobobrowser.util.Nodes;
import org.lobobrowser.util.Objects;
import org.w3c.dom.Node;
import org.w3c.dom.html.HTMLCollection;

public class DescendentHTMLCollection extends AbstractScriptableDelegate implements HTMLCollection {
  private final NodeImpl rootNode;
  private final NodeFilter nodeFilter;
  private final Object treeLock;
  private final boolean nestIntoMatchingNodes;

  public DescendentHTMLCollection(final NodeImpl node, final NodeFilter filter, final Object treeLock) {
    this(node, filter, treeLock, true);
  }

  /**
   * @param node
   * @param filter
   */
  public DescendentHTMLCollection(final NodeImpl node, final NodeFilter filter, final Object treeLock, final boolean nestMatchingNodes) {
    rootNode = node;
    nodeFilter = filter;
    this.treeLock = treeLock;
    this.nestIntoMatchingNodes = nestMatchingNodes;
    final HTMLDocumentImpl document = (HTMLDocumentImpl) node.getOwnerDocument();
    document.addDocumentNotificationListener(new LocalNotificationListener(document, this));
  }

  private Map<String, ElementImpl> itemsByName = null;
  private List<NodeImpl> itemsByIndex = null;

  private void ensurePopulatedImpl() {
    if (this.itemsByName == null) {
      final ArrayList<NodeImpl> descendents = this.rootNode.getDescendents(this.nodeFilter, this.nestIntoMatchingNodes);
      this.itemsByIndex = descendents == null ? Collections.emptyList() : descendents;
      final int size = descendents == null ? 0 : descendents.size();
      final Map<String, ElementImpl> itemsByName = new HashMap<>((size * 3) / 2);
      this.itemsByName = itemsByName;
      for (int i = 0; i < size; i++) {
        final NodeImpl descNode = descendents.get(i);
        if (descNode instanceof ElementImpl) {
          final ElementImpl element = (ElementImpl) descNode;
          final String id = element.getId();
          if ((id != null) && (id.length() != 0)) {
            itemsByName.put(id, element);
          }
          final String name = element.getAttribute("name");
          if ((name != null) && (name.length() != 0) && !name.equals(id)) {
            itemsByName.put(name, element);
          }
        }
      }
    }
  }

  private void invalidate() {
    synchronized (this.treeLock) {
      this.itemsByName = null;
      this.itemsByIndex = null;
    }
  }

  private boolean isValid() {
    synchronized (this.treeLock) {
      return (this.itemsByName != null) && (this.itemsByIndex != null);
    }
  }

  public int getLength() {
    synchronized (this.treeLock) {
      this.ensurePopulatedImpl();
      return this.itemsByIndex.size();
    }
  }

  public Node item(final int index) {
    synchronized (this.treeLock) {
      this.ensurePopulatedImpl();
      try {
        return this.itemsByIndex.get(index);
      } catch (final java.lang.IndexOutOfBoundsException iob) {
        return null;
      }
    }
  }

  // TODO: This is a quick hack. Need to support WEB-IDL Semantics. GH #67
  public Node item(final Object obj) {
    if (obj instanceof Integer) {
      final Integer index = (Integer) obj;
      return item((int) index);
    }
    return item(0);
  }

  // TODO: This needs to be handled in a general fashion. GH #123
  public boolean hasOwnProperty(final Object obj) {
    if (Objects.isAssignableOrBox(obj, Integer.TYPE)) {
      final Integer i = (Integer) JavaScript.getInstance().getJavaObject(obj, Integer.TYPE);
      return i < getLength();
    } else if (Objects.isAssignableOrBox(obj, String.class)) {
      // This seems to be related to GH #67
      final String s = (String) JavaScript.getInstance().getJavaObject(obj, String.class);
      try {
        return Integer.parseInt(s) < getLength();
      } catch (NumberFormatException nfe){
        return false;
      }
    } else {
      return false;
    }
  }

  public Node namedItem(final String name) {
    synchronized (this.treeLock) {
      this.ensurePopulatedImpl();
      return this.itemsByName.get(name);
    }
  }

  public int indexOf(final Node node) {
    synchronized (this.treeLock) {
      this.ensurePopulatedImpl();
      return this.itemsByIndex.indexOf(node);
    }
  }

  // private final class NodeCounter implements NodeVisitor {
  // private int count = 0;
  //
  // public final void visit(Node node) {
  // if(nodeFilter.accept(node)) {
  // this.count++;
  // throw new SkipVisitorException();
  // }
  // }
  //
  // public int getCount() {
  // return this.count;
  // }
  // }
  //
  // private final class NodeScanner implements NodeVisitor {
  // private int count = 0;
  // private Node foundNode = null;
  // private final int targetIndex;
  //
  // public NodeScanner(int idx) {
  // this.targetIndex = idx;
  // }
  //
  // public final void visit(Node node) {
  // if(nodeFilter.accept(node)) {
  // if(this.count == this.targetIndex) {
  // this.foundNode = node;
  // throw new StopVisitorException();
  // }
  // this.count++;
  // throw new SkipVisitorException();
  // }
  // }
  //
  // public Node getNode() {
  // return this.foundNode;
  // }
  // }
  //
  // private final class NodeScanner2 implements NodeVisitor {
  // private int count = 0;
  // private int foundIndex = -1;
  // private final Node targetNode;
  //
  // public NodeScanner2(Node node) {
  // this.targetNode = node;
  // }
  //
  // public final void visit(Node node) {
  // if(nodeFilter.accept(node)) {
  // if(node == this.targetNode) {
  // this.foundIndex = this.count;
  // throw new StopVisitorException();
  // }
  // this.count++;
  // throw new SkipVisitorException();
  // }
  // }
  //
  // public int getIndex() {
  // return this.foundIndex;
  // }
  // }

  private static class LocalNotificationListener extends DocumentNotificationAdapter {
    // Needs to be a static class with a weak reference to
    // the collection object.
    private final HTMLDocumentImpl document;
    private final WeakReference<DescendentHTMLCollection> collectionRef;

    public LocalNotificationListener(final HTMLDocumentImpl document, final DescendentHTMLCollection collection) {
      super();
      this.document = document;
      this.collectionRef = new WeakReference<>(collection);
    }

    @Override
    public void structureInvalidated(final NodeImpl node) {
      final DescendentHTMLCollection collection = this.collectionRef.get();
      if (collection == null) {
        // Gone!
        this.document.removeDocumentNotificationListener(this);
        return;
      }
      if (collection.isValid()) {
        if (Nodes.isSameOrAncestorOf(collection.rootNode, node)) {
          collection.invalidate();
        }
      }
    }

    @Override
    public void nodeLoaded(final NodeImpl node) {
      this.structureInvalidated(node);
    }
  }

}
