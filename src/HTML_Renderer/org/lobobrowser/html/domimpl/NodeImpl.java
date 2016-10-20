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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.js.Event;
import org.lobobrowser.html.style.RenderState;
import org.lobobrowser.html.style.StyleSheetRenderState;
import org.lobobrowser.js.AbstractScriptableDelegate;
import org.lobobrowser.js.HideFromJS;
import org.lobobrowser.ua.UserAgentContext;
import org.lobobrowser.util.Strings;
import org.lobobrowser.util.Urls;
import org.mozilla.javascript.Function;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.UserDataHandler;
import org.w3c.dom.html.HTMLCollection;
import org.w3c.dom.html.HTMLDocument;

import cz.vutbr.web.css.CSSException;
import cz.vutbr.web.css.CSSFactory;
import cz.vutbr.web.css.CombinedSelector;
import cz.vutbr.web.css.RuleSet;
import cz.vutbr.web.css.Selector;
import cz.vutbr.web.css.StyleSheet;

// TODO: Implement org.w3c.dom.events.EventTarget ?
public abstract class NodeImpl extends AbstractScriptableDelegate implements Node, ModelNode {
  private static final NodeImpl[] EMPTY_ARRAY = new NodeImpl[0];
  private static final @NonNull RenderState BLANK_RENDER_STATE = new StyleSheetRenderState(null);
  protected static final Logger logger = Logger.getLogger(NodeImpl.class.getName());
  protected UINode uiNode;
  protected ArrayList<Node> nodeList;
  protected volatile Document document;

  /**
   * A tree lock is less deadlock-prone than a node-level lock. This is assigned
   * in setOwnerDocument.
   */
  protected volatile Object treeLock = this;

  public NodeImpl() {
    super();
  }

  @HideFromJS
  public void setUINode(final UINode uiNode) {
    // Called in GUI thread always.
    this.uiNode = uiNode;
  }

  @HideFromJS
  public UINode getUINode() {
    // Called in GUI thread always.
    return this.uiNode;
  }

  /**
   * Tries to get a UINode associated with the current node. Failing that, it
   * tries ancestors recursively. This method will return the closest
   * <i>block-level</i> renderer node, if any.
   */
  @HideFromJS
  public UINode findUINode() {
    // Called in GUI thread always.
    final UINode uiNode = this.uiNode;
    if (uiNode != null) {
      return uiNode;
    }
    final NodeImpl parentNode = (NodeImpl) this.getParentNode();
    return parentNode == null ? null : parentNode.findUINode();
  }

  public Node appendChild(final Node newChild) throws DOMException {
    if (newChild != null) {
      synchronized (this.treeLock) {
        if (isInclusiveAncestorOf(newChild)) {
          final Node prevParent = newChild.getParentNode();
          if (prevParent instanceof NodeImpl) {
            ((NodeImpl) prevParent).removeChild(newChild);
          }
        } else if ((newChild instanceof NodeImpl) && ((NodeImpl) newChild).isInclusiveAncestorOf(this)) {
          throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Trying to append an ancestor element.");
        }

        ArrayList<Node> nl = this.nodeList;
        if (nl == null) {
          nl = new ArrayList<>(3);
          this.nodeList = nl;
        }
        nl.add(newChild);
        if (newChild instanceof NodeImpl) {
          ((NodeImpl) newChild).handleAddedToParent(this);
        }
      }

      this.postChildListChanged();

      return newChild;
    } else {
      throw new DOMException(DOMException.INVALID_ACCESS_ERR, "Trying to append a null child!");
    }
  }

  // TODO not used by anyone
  protected void removeAllChildren() {
    this.removeAllChildrenImpl();
  }

  protected void removeAllChildrenImpl() {
    synchronized (this.treeLock) {
      final ArrayList<Node> nl = this.nodeList;
      if (nl != null) {
        for (final Node node : nl) {
          if (node instanceof NodeImpl) {
            ((NodeImpl) node).handleDeletedFromParent();
          }
        }
        this.nodeList = null;
      }
    }

    this.postChildListChanged();

  }

  protected NodeList getNodeList(final NodeFilter filter) {
    final Collection<Node> collection = new ArrayList<>();
    synchronized (this.treeLock) {
      this.appendChildrenToCollectionImpl(filter, collection);
    }
    return new NodeListImpl(collection);
  }

  /*
   * TODO: If this is not a w3c DOM method, we can return an Iterator instead of
   * creating a new array But, it changes the semantics slightly (when
   * modifications are needed during iteration). For those cases, we can retain
   * this method.
   */
  public NodeImpl[] getChildrenArray() {
    final ArrayList<Node> nl = this.nodeList;
    synchronized (this.treeLock) {
      return nl == null ? null : nl.toArray(NodeImpl.EMPTY_ARRAY);
    }
  }

  int getChildCount() {
    synchronized (this.treeLock) {
      final ArrayList<Node> nl = this.nodeList;
      return nl == null ? 0 : nl.size();
    }
  }

  // TODO: This is needed to be implemented only by Element, Document and DocumentFragment as per https://developer.mozilla.org/en-US/docs/Web/API/ParentNode
  public HTMLCollection getChildren() {
    return new DescendentHTMLCollection(this, new NodeFilter.ElementFilter(), this.treeLock);
  }

  /**
   * Creates an <code>ArrayList</code> of descendent nodes that the given filter
   * condition.
   */
  public ArrayList<NodeImpl> getDescendents(final NodeFilter filter, final boolean nestIntoMatchingNodes) {
    final ArrayList<NodeImpl> al = new ArrayList<>();
    synchronized (this.treeLock) {
      this.extractDescendentsArrayImpl(filter, al, nestIntoMatchingNodes);
    }
    return al;
  }

  /**
   * Extracts all descendents that match the filter, except those descendents of
   * nodes that match the filter.
   *
   * @param filter
   * @param al
   */
  private void extractDescendentsArrayImpl(final NodeFilter filter, final ArrayList<NodeImpl> al, final boolean nestIntoMatchingNodes) {
    final ArrayList<Node> nl = this.nodeList;
    if (nl != null) {
      final Iterator<Node> i = nl.iterator();
      while (i.hasNext()) {
        final NodeImpl n = (NodeImpl) i.next();
        if (filter.accept(n)) {
          al.add(n);
          if (nestIntoMatchingNodes) {
            n.extractDescendentsArrayImpl(filter, al, nestIntoMatchingNodes);
          }
        } else if (n.getNodeType() == Node.ELEMENT_NODE) {
          n.extractDescendentsArrayImpl(filter, al, nestIntoMatchingNodes);
        }
      }
    }
  }

  private void appendChildrenToCollectionImpl(final NodeFilter filter, final Collection<Node> collection) {
    final ArrayList<Node> nl = this.nodeList;
    if (nl != null) {
      final Iterator<Node> i = nl.iterator();
      while (i.hasNext()) {
        final NodeImpl node = (NodeImpl) i.next();
        if (filter.accept(node)) {
          collection.add(node);
        }
        node.appendChildrenToCollectionImpl(filter, collection);
      }
    }
  }

  /**
   * Should create a node with some cloned properties, like the node name, but
   * not attributes or children.
   */
  protected abstract Node createSimilarNode();

  public Node cloneNode(final boolean deep) {
    // TODO: Synchronize with treeLock?
    try {
      final Node newNode = this.createSimilarNode();
      final NodeList children = this.getChildNodes();
      final int length = children.getLength();
      for (int i = 0; i < length; i++) {
        final Node child = children.item(i);
        final Node newChild = deep ? child.cloneNode(deep) : child;
        newNode.appendChild(newChild);
      }
      if (newNode instanceof Element) {
        final Element elem = (Element) newNode;
        final NamedNodeMap nnmap = this.getAttributes();
        if (nnmap != null) {
          final int nnlength = nnmap.getLength();
          for (int i = 0; i < nnlength; i++) {
            final Attr attr = (Attr) nnmap.item(i);
            elem.setAttributeNode((Attr) attr.cloneNode(true));
          }
        }
      }

      synchronized (this) {
        if ((userDataHandlers != null) && (userData != null)) {
          userDataHandlers.forEach((k, handler) -> handler.handle(UserDataHandler.NODE_CLONED, k, userData.get(k), this, newNode));
        }
      }

      return newNode;
    } catch (final Exception err) {
      throw new IllegalStateException(err.getMessage());
    }
  }

  private int getNodeIndex() {
    final NodeImpl parent = (NodeImpl) this.getParentNode();
    return parent == null ? -1 : parent.getChildIndex(this);
  }

  int getChildIndex(final Node child) {
    synchronized (this.treeLock) {
      final ArrayList<Node> nl = this.nodeList;
      return nl == null ? -1 : nl.indexOf(child);
    }
  }

  Node getChildAtIndex(final int index) {
    synchronized (this.treeLock) {
      final ArrayList<Node> nl = this.nodeList;
      try {
        return nl == null ? null : nl.get(index);
      } catch (final IndexOutOfBoundsException iob) {
        this.warn("getChildAtIndex(): Bad index=" + index + " for node=" + this + ".");
        return null;
      }
    }
  }

  private boolean isAncestorOf(final Node other) {
    final NodeImpl parent = (NodeImpl) other.getParentNode();
    if (parent == this) {
      return true;
    } else if (parent == null) {
      return false;
    } else {
      return this.isAncestorOf(parent);
    }
  }

  private boolean isInclusiveAncestorOf(final Node other) {
    if (other == this) {
      return true;
    } else if (other == null) {
      return false;
    } else {
      return this.isAncestorOf(other);
    }
  }

  public short compareDocumentPosition(final Node other) throws DOMException {
    final Node parent = this.getParentNode();
    if (!(other instanceof NodeImpl)) {
      throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unknwon node implementation");
    }
    if ((parent != null) && (parent == other.getParentNode())) {
      final int thisIndex = this.getNodeIndex();
      final int otherIndex = ((NodeImpl) other).getNodeIndex();
      if ((thisIndex == -1) || (otherIndex == -1)) {
        return Node.DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC;
      }
      if (thisIndex < otherIndex) {
        return Node.DOCUMENT_POSITION_FOLLOWING;
      } else {
        return Node.DOCUMENT_POSITION_PRECEDING;
      }
    } else if (this.isAncestorOf(other)) {
      return Node.DOCUMENT_POSITION_CONTAINED_BY;
    } else if (((NodeImpl) other).isAncestorOf(this)) {
      return Node.DOCUMENT_POSITION_CONTAINS;
    } else {
      return Node.DOCUMENT_POSITION_DISCONNECTED;
    }
  }

  public NamedNodeMap getAttributes() {
    return null;
  }

  public Document getOwnerDocument() {
    return this.document;
  }

  void setOwnerDocument(final Document value) {
    this.document = value;
    this.treeLock = value == null ? this : (Object) value;
  }

  void setOwnerDocument(final Document value, final boolean deep) {
    this.document = value;
    this.treeLock = value == null ? this : (Object) value;
    if (deep) {
      synchronized (this.treeLock) {
        final ArrayList<Node> nl = this.nodeList;
        if (nl != null) {
          final Iterator<Node> i = nl.iterator();
          while (i.hasNext()) {
            final NodeImpl child = (NodeImpl) i.next();
            child.setOwnerDocument(value, deep);
          }
        }
      }
    }
  }

  void visitImpl(final NodeVisitor visitor) {
    try {
      visitor.visit(this);
    } catch (final SkipVisitorException sve) {
      return;
    } catch (final StopVisitorException sve) {
      throw sve;
    }
    final ArrayList<Node> nl = this.nodeList;
    if (nl != null) {
      final Iterator<Node> i = nl.iterator();
      while (i.hasNext()) {
        final NodeImpl child = (NodeImpl) i.next();
        try {
          // Call with child's synchronization
          child.visit(visitor);
        } catch (final StopVisitorException sve) {
          throw sve;
        }
      }
    }
  }

  void visit(final NodeVisitor visitor) {
    synchronized (this.treeLock) {
      this.visitImpl(visitor);
    }
  }

  /*
  public Node insertBefore(final Node newChild, final Node refChild) throws DOMException {
    synchronized (this.treeLock) {
      final ArrayList<Node> nl = getNonEmptyNodeList();
      // int idx = nl == null ? -1 : nl.indexOf(refChild);
      int idx = nl.indexOf(refChild);
      if (idx == -1) {
        // The exception was misleading. -1 could have resulted from an empty node list too. (but that is no more the case)
        // throw new DOMException(DOMException.NOT_FOUND_ERR, "refChild not found");

        // From what I understand from https://developer.mozilla.org/en-US/docs/Web/API/Node.insertBefore
        // an invalid refChild will add the new child at the end of the list

        idx = nl.size();
      }
      nl.add(idx, newChild);
      if (newChild instanceof NodeImpl) {
        ((NodeImpl) newChild).handleAddedToParent(this);
      }
    }

    this.postChildListChanged();

    return newChild;
  }*/

  // Ongoing issue : 152
  // This is a changed and better version of the above. It gives the same number of pass / failures on http://web-platform.test:8000/dom/nodes/Node-insertBefore.html
  // Pass 2: FAIL: 24
  public Node insertBefore(final Node newChild, final Node refChild) throws DOMException {
    if (newChild == null) {
      throw new DOMException(DOMException.TYPE_MISMATCH_ERR, "child is null");
    }
    synchronized (this.treeLock) {
      if (newChild instanceof NodeImpl) {
        final NodeImpl newChildImpl = (NodeImpl) newChild;
        if (newChildImpl.isInclusiveAncestorOf(this)) {
          throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "new child is an ancestor");
        }
      }

      // From what I understand from https://developer.mozilla.org/en-US/docs/Web/API/Node.insertBefore
      // a null or undefined refChild will cause the new child to be appended at the end of the list
      // otherwise, this function will throw an exception if refChild is not found in the child list
      final ArrayList<Node> nl = refChild == null ? getNonEmptyNodeList() : this.nodeList;
      final int idx = refChild == null ? nl.size() : (nl == null ? -1 : nl.indexOf(refChild));
      if (idx == -1) {
        throw new DOMException(DOMException.NOT_FOUND_ERR, "refChild not found");
      }
      nl.add(idx, newChild);
      if (newChild instanceof NodeImpl) {
        ((NodeImpl) newChild).handleAddedToParent(this);
      }
    }

    this.postChildListChanged();

    return newChild;
  }

  // TODO: Use this wherever nodeList needs to be non empty
  private @NonNull ArrayList<Node> getNonEmptyNodeList() {
    ArrayList<Node> nl = this.nodeList;
    if (nl == null) {
      nl = new ArrayList<>();
      this.nodeList = nl;
    }
    return nl;
  }

  protected Node insertAt(final Node newChild, final int idx) throws DOMException {
    synchronized (this.treeLock) {
      final ArrayList<Node> nl = getNonEmptyNodeList();
      nl.add(idx, newChild);
      if (newChild instanceof NodeImpl) {
        ((NodeImpl) newChild).handleAddedToParent(this);
      }
    }

    this.postChildListChanged();

    return newChild;
  }

  public Node replaceChild(final Node newChild, final Node oldChild) throws DOMException {
    synchronized (this.treeLock) {
      if (this.isInclusiveAncestorOf(newChild)) {
        throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "newChild is already a child of the node");
      }
      if ((newChild instanceof NodeImpl) && ((NodeImpl) newChild).isInclusiveAncestorOf(this)) {
        throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Trying to set an ancestor element as a child.");
      }

      final ArrayList<Node> nl = this.nodeList;
      final int idx = nl == null ? -1 : nl.indexOf(oldChild);
      if (idx == -1) {
        throw new DOMException(DOMException.NOT_FOUND_ERR, "oldChild not found");
      }
      nl.set(idx, newChild);

      if (newChild instanceof NodeImpl) {
        ((NodeImpl) newChild).handleAddedToParent(this);
      }

      if (oldChild instanceof NodeImpl) {
        ((NodeImpl) oldChild).handleDeletedFromParent();
      }
    }

    this.postChildListChanged();

    return newChild;
  }

  public Node removeChild(final Node oldChild) throws DOMException {
    synchronized (this.treeLock) {
      final ArrayList<Node> nl = this.nodeList;
      if ((nl == null) || !nl.remove(oldChild)) {
        throw new DOMException(DOMException.NOT_FOUND_ERR, "oldChild not found");
      }
      if (oldChild instanceof NodeImpl) {
        ((NodeImpl) oldChild).handleDeletedFromParent();
      }
    }

    this.postChildListChanged();

    return oldChild;
  }

  @HideFromJS
  public Node removeChildAt(final int index) throws DOMException {
    try {
      synchronized (this.treeLock) {
        final ArrayList<Node> nl = this.nodeList;
        if (nl == null) {
          throw new DOMException(DOMException.INDEX_SIZE_ERR, "Empty list of children");
        }
        final Node n = nl.remove(index);
        if (n == null) {
          throw new DOMException(DOMException.INDEX_SIZE_ERR, "No node with that index");
        }
        if (n instanceof NodeImpl) {
          ((NodeImpl) n).handleDeletedFromParent();
        }
        return n;
      }
    } finally {
      this.postChildListChanged();
    }
  }

  public boolean hasChildNodes() {
    synchronized (this.treeLock) {
      final ArrayList<Node> nl = this.nodeList;
      return (nl != null) && !nl.isEmpty();
    }
  }

  public String getBaseURI() {
    final Document document = this.document;
    return document == null ? null : document.getBaseURI();
  }

  public NodeList getChildNodes() {
    synchronized (this.treeLock) {
      final ArrayList<Node> nl = this.nodeList;
      return new NodeListImpl(nl == null ? Collections.emptyList() : nl);
    }
  }

  public Node getFirstChild() {
    synchronized (this.treeLock) {
      final ArrayList<Node> nl = this.nodeList;
      try {
        return nl == null ? null : nl.get(0);
      } catch (final IndexOutOfBoundsException iob) {
        return null;
      }
    }
  }

  public Node getLastChild() {
    synchronized (this.treeLock) {
      final ArrayList<Node> nl = this.nodeList;
      try {
        return nl == null ? null : nl.get(nl.size() - 1);
      } catch (final IndexOutOfBoundsException iob) {
        return null;
      }
    }
  }

  private Node getPreviousTo(final Node node) {
    synchronized (this.treeLock) {
      final ArrayList<Node> nl = this.nodeList;
      final int idx = nl == null ? -1 : nl.indexOf(node);
      if (idx == -1) {
        throw new DOMException(DOMException.NOT_FOUND_ERR, "node not found");
      }
      try {
        return nl.get(idx - 1);
      } catch (final IndexOutOfBoundsException iob) {
        return null;
      }
    }
  }

  private Node getNextTo(final Node node) {
    synchronized (this.treeLock) {
      final ArrayList<Node> nl = this.nodeList;
      final int idx = nl == null ? -1 : nl.indexOf(node);
      if (idx == -1) {
        throw new DOMException(DOMException.NOT_FOUND_ERR, "node not found");
      }
      try {
        return nl.get(idx + 1);
      } catch (final IndexOutOfBoundsException iob) {
        return null;
      }
    }
  }

  public Node getPreviousSibling() {
    final NodeImpl parent = (NodeImpl) this.getParentNode();
    return parent == null ? null : parent.getPreviousTo(this);
  }

  public Node getNextSibling() {
    final NodeImpl parent = (NodeImpl) this.getParentNode();
    return parent == null ? null : parent.getNextTo(this);
  }

  public Element getPreviousElementSibling() {
    final NodeImpl parent = (NodeImpl) this.getParentNode();
    if (parent != null) {
      Node previous = this;
      do {
        previous = parent.getPreviousTo(previous);
        if ((previous != null) && (previous instanceof Element)) {
          return (Element) previous;
        }
      } while (previous != null);
      return null;
    } else {
      return null;
    }
  }

  public Element getNextElementSibling() {
    final NodeImpl parent = (NodeImpl) this.getParentNode();
    if (parent != null) {
      Node next = this;
      do {
        next = parent.getNextTo(next);
        if ((next != null) && (next instanceof Element)) {
          return (Element) next;
        }
      } while (next != null);
      return null;
    } else {
      return null;
    }
  }

  public Object getFeature(final String feature, final String version) {
    // TODO What should this do?
    return null;
  }

  private Map<String, Object> userData;
  // TODO: Inform handlers on cloning, etc.
  private Map<String, UserDataHandler> userDataHandlers;
  protected volatile boolean notificationsSuspended = false;

  public Object setUserData(final String key, final Object data, final UserDataHandler handler) {
    if (org.lobobrowser.html.parser.HtmlParser.MODIFYING_KEY.equals(key)) {
      final boolean ns = (Boolean.TRUE == data);
      this.notificationsSuspended = ns;
      if (!ns) {
        this.informNodeLoaded();
      }
    }
    // here we spent some effort preventing our maps from growing too much
    synchronized (this) {
      if (handler != null) {
        if (this.userDataHandlers == null) {
          this.userDataHandlers = new HashMap<>();
        } else {
          this.userDataHandlers.remove(key);
        }
        this.userDataHandlers.put(key, handler);
      }

      Map<String, Object> userData = this.userData;
      if (data != null) {
        if (userData == null) {
          userData = new HashMap<>();
          this.userData = userData;
        }
        return userData.put(key, data);
      } else if (userData != null) {
        return userData.remove(key);
      } else {
        return null;
      }
    }
  }

  public Object getUserData(final String key) {
    synchronized (this) {
      final Map<String, Object> ud = this.userData;
      return ud == null ? null : ud.get(key);
    }
  }

  public abstract String getLocalName();

  public boolean hasAttributes() {
    return false;
  }

  public String getNamespaceURI() {
    return null;
  }

  public abstract String getNodeName();

  public abstract String getNodeValue() throws DOMException;

  private volatile String prefix;

  public String getPrefix() {
    return this.prefix;
  }

  public void setPrefix(final String prefix) throws DOMException {
    this.prefix = prefix;
  }

  public abstract void setNodeValue(String nodeValue) throws DOMException;

  public abstract short getNodeType();

  /**
   * Gets the text content of this node and its descendents.
   */
  public String getTextContent() throws DOMException {
    final StringBuffer sb = new StringBuffer();
    synchronized (this.treeLock) {
      final ArrayList<Node> nl = this.nodeList;
      if (nl != null) {
        final Iterator<Node> i = nl.iterator();
        while (i.hasNext()) {
          final Node node = i.next();
          final short type = node.getNodeType();
          switch (type) {
          case Node.CDATA_SECTION_NODE:
          case Node.TEXT_NODE:
          case Node.ELEMENT_NODE:
            final String textContent = node.getTextContent();
            if (textContent != null) {
              sb.append(textContent);
            }
            break;
          default:
            break;
          }
        }
      }
    }
    return sb.toString();
  }

  public void setTextContent(final String textContent) throws DOMException {
    synchronized (this.treeLock) {
      this.removeChildrenImpl(new TextFilter());
      if ((textContent != null) && !"".equals(textContent)) {
        final TextImpl t = new TextImpl(textContent);
        t.setOwnerDocument(this.document);
        t.setParentImpl(this);
        ArrayList<Node> nl = this.nodeList;
        if (nl == null) {
          nl = new ArrayList<>();
          this.nodeList = nl;
        }
        nl.add(t);
      }
    }

    this.postChildListChanged();

  }

  protected void removeChildren(final NodeFilter filter) {
    synchronized (this.treeLock) {
      this.removeChildrenImpl(filter);
    }

    this.postChildListChanged();

  }

  protected void removeChildrenImpl(final NodeFilter filter) {
    final ArrayList<Node> nl = this.nodeList;
    if (nl != null) {
      final int len = nl.size();
      for (int i = len; --i >= 0;) {
        final Node node = nl.get(i);
        if (filter.accept(node)) {
          final Node n = nl.remove(i);
          if (n instanceof NodeImpl) {
            ((NodeImpl) n).handleDeletedFromParent();
          }
        }
      }
    }
  }

  @HideFromJS
  public Node insertAfter(final Node newChild, final Node refChild) {
    synchronized (this.treeLock) {
      final ArrayList<Node> nl = this.nodeList;
      final int idx = nl == null ? -1 : nl.indexOf(refChild);
      if (idx == -1) {
        throw new DOMException(DOMException.NOT_FOUND_ERR, "refChild not found");
      }
      nl.add(idx + 1, newChild);
      if (newChild instanceof NodeImpl) {
        ((NodeImpl) newChild).handleAddedToParent(this);
      }
    }

    this.postChildListChanged();

    return newChild;
  }

  @HideFromJS
  public Text replaceAdjacentTextNodes(final Text node, final String textContent) {
    try {
      synchronized (this.treeLock) {
        final ArrayList<Node> nl = this.nodeList;
        if (nl == null) {
          throw new DOMException(DOMException.NOT_FOUND_ERR, "Node not a child");
        }
        final int idx = nl.indexOf(node);
        if (idx == -1) {
          throw new DOMException(DOMException.NOT_FOUND_ERR, "Node not a child");
        }
        int firstIdx = idx;
        final List<Object> toDelete = new LinkedList<>();
        for (int adjIdx = idx; --adjIdx >= 0;) {
          final Object child = this.nodeList.get(adjIdx);
          if (child instanceof Text) {
            firstIdx = adjIdx;
            toDelete.add(child);
          }
        }
        final int length = this.nodeList.size();
        for (int adjIdx = idx; ++adjIdx < length;) {
          final Object child = this.nodeList.get(adjIdx);
          if (child instanceof Text) {
            toDelete.add(child);
          }
        }
        this.nodeList.removeAll(toDelete);
        final TextImpl textNode = new TextImpl(textContent);
        textNode.setOwnerDocument(this.document);
        textNode.setParentImpl(this);
        this.nodeList.add(firstIdx, textNode);
        return textNode;
      }
    } finally {
      this.postChildListChanged();
    }
  }

  @HideFromJS
  public Text replaceAdjacentTextNodes(final Text node) {
    try {
      synchronized (this.treeLock) {
        final ArrayList<Node> nl = this.nodeList;
        if (nl == null) {
          throw new DOMException(DOMException.NOT_FOUND_ERR, "Node not a child");
        }
        final int idx = nl.indexOf(node);
        if (idx == -1) {
          throw new DOMException(DOMException.NOT_FOUND_ERR, "Node not a child");
        }
        final StringBuffer textBuffer = new StringBuffer();
        int firstIdx = idx;
        final List<Object> toDelete = new LinkedList<>();
        for (int adjIdx = idx; --adjIdx >= 0;) {
          final Object child = this.nodeList.get(adjIdx);
          if (child instanceof Text) {
            firstIdx = adjIdx;
            toDelete.add(child);
            textBuffer.append(((Text) child).getNodeValue());
          }
        }
        final int length = this.nodeList.size();
        for (int adjIdx = idx; ++adjIdx < length;) {
          final Object child = this.nodeList.get(adjIdx);
          if (child instanceof Text) {
            toDelete.add(child);
            textBuffer.append(((Text) child).getNodeValue());
          }
        }
        this.nodeList.removeAll(toDelete);
        final TextImpl textNode = new TextImpl(textBuffer.toString());
        textNode.setOwnerDocument(this.document);
        textNode.setParentImpl(this);
        this.nodeList.add(firstIdx, textNode);
        return textNode;
      }
    } finally {
      this.postChildListChanged();
    }
  }

  protected volatile Node parentNode;

  public Node getParentNode() {
    // Should it be synchronized? Could have side-effects.
    return this.parentNode;
  }

  public boolean isSameNode(final Node other) {
    return this == other;
  }

  public boolean isSupported(final String feature, final String version) {
    return ("HTML".equals(feature) && (version.compareTo("4.01") <= 0));
  }

  public String lookupNamespaceURI(final String prefix) {
    return null;
  }

  public boolean equalAttributes(final Node arg) {
    return false;
  }

  public boolean isEqualNode(final Node arg) {
    return (arg instanceof NodeImpl) && (this.getNodeType() == arg.getNodeType()) && java.util.Objects.equals(this.getNodeName(), arg.getNodeName())
        && java.util.Objects.equals(this.getNodeValue(), arg.getNodeValue()) && java.util.Objects.equals(this.getLocalName(), arg.getLocalName())
        && java.util.Objects.equals(this.nodeList, ((NodeImpl) arg).nodeList) && this.equalAttributes(arg);
  }

  public boolean isDefaultNamespace(final String namespaceURI) {
    return namespaceURI == null;
  }

  public String lookupPrefix(final String namespaceURI) {
    return null;
  }

  public void normalize() {
    synchronized (this.treeLock) {
      final ArrayList<Node> nl = this.nodeList;
      if (nl != null) {
        Iterator<Node> i = nl.iterator();
        final List<Node> textNodes = new LinkedList<>();
        boolean prevText = false;
        while (i.hasNext()) {
          final Node child = i.next();
          if (child.getNodeType() == Node.TEXT_NODE) {
            if (!prevText) {
              prevText = true;
              textNodes.add(child);
            }
          } else {
            prevText = false;
          }
        }
        i = textNodes.iterator();
        while (i.hasNext()) {
          final Text text = (Text) i.next();
          this.replaceAdjacentTextNodes(text);
        }
      }
    }
    this.postChildListChanged();
  }

  @Override
  public String toString() {
    return this.getNodeName();
  }

  public UserAgentContext getUserAgentContext() {
    final Object doc = this.document;
    if (doc instanceof HTMLDocumentImpl) {
      return ((HTMLDocumentImpl) doc).getUserAgentContext();
    } else {
      return null;
    }
  }

  public HtmlRendererContext getHtmlRendererContext() {
    final Object doc = this.document;
    if (doc instanceof HTMLDocumentImpl) {
      return ((HTMLDocumentImpl) doc).getHtmlRendererContext();
    } else {
      return null;
    }
  }

  final void setParentImpl(final Node parent) {
    // Call holding treeLock.
    this.parentNode = parent;
  }

  // ----- ModelNode implementation

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.html.renderer.RenderableContext#getFullURL(java.lang.String)
   */
  public @NonNull URL getFullURL(final String spec) throws MalformedURLException {
    final Object doc = this.document;
    final String cleanSpec = Urls.encodeIllegalCharacters(spec);
    if (doc instanceof HTMLDocumentImpl) {
      return ((HTMLDocumentImpl) doc).getFullURL(cleanSpec);
    } else {
      return new java.net.URL(cleanSpec);
    }
  }

  public URL getDocumentURL() {
    final Object doc = this.document;
    if (doc instanceof HTMLDocumentImpl) {
      return ((HTMLDocumentImpl) doc).getDocumentURL();
    } else {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.html.renderer.RenderableContext#getDocumentItem(java.lang.String
   * )
   */
  public Object getDocumentItem(final String name) {
    final org.w3c.dom.Document document = this.document;
    return document == null ? null : document.getUserData(name);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.html.renderer.RenderableContext#setDocumentItem(java.lang.String
   * , java.lang.Object)
   */
  public void setDocumentItem(final String name, final Object value) {
    final org.w3c.dom.Document document = this.document;
    if (document == null) {
      return;
    }
    document.setUserData(name, value, null);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.html.renderer.RenderableContext#isEqualOrDescendentOf(org.xamjwg
   * .html.renderer.RenderableContext)
   */
  public final boolean isEqualOrDescendentOf(final ModelNode otherContext) {
    if (otherContext == this) {
      return true;
    }
    final Object parent = this.getParentNode();
    if (parent instanceof HTMLElementImpl) {
      return ((HTMLElementImpl) parent).isEqualOrDescendentOf(otherContext);
    } else {
      return false;
    }
  }

  public final ModelNode getParentModelNode() {
    return (ModelNode) this.parentNode;
  }

  public void warn(final String message, final Throwable err) {
    logger.log(Level.WARNING, message, err);
  }

  public void warn(final String message) {
    logger.log(Level.WARNING, message);
  }

  public void informSizeInvalid() {
    final HTMLDocumentImpl doc = (HTMLDocumentImpl) this.document;
    if (doc != null) {
      doc.sizeInvalidated(this);
    }
  }

  public void informLookInvalid() {
    this.forgetRenderState();
    final HTMLDocumentImpl doc = (HTMLDocumentImpl) this.document;
    if (doc != null) {
      doc.lookInvalidated(this);
    }
  }

  public void informPositionInvalid() {
    final HTMLDocumentImpl doc = (HTMLDocumentImpl) this.document;
    if (doc != null) {
      doc.positionInParentInvalidated(this);
    }
  }

  public void informInvalid() {
    // This is called when an attribute or child changes.
    this.forgetRenderState();
    final HTMLDocumentImpl doc = (HTMLDocumentImpl) this.document;
    if (doc != null) {
      doc.invalidated(this);
    }
  }

  public void informStructureInvalid() {
    // This is called when an attribute or child changes.
    this.forgetRenderState();
    final HTMLDocumentImpl doc = (HTMLDocumentImpl) this.document;
    if (doc != null) {
      doc.structureInvalidated(this);
    }
  }

  protected void informNodeLoaded() {
    // This is called when an attribute or child changes.
    this.forgetRenderState();
    final HTMLDocumentImpl doc = (HTMLDocumentImpl) this.document;
    if (doc != null) {
      doc.nodeLoaded(this);
    }
  }

  protected void informExternalScriptLoading() {
    // This is called when an attribute or child changes.
    this.forgetRenderState();
    final HTMLDocumentImpl doc = (HTMLDocumentImpl) this.document;
    if (doc != null) {
      doc.externalScriptLoading(this);
    }
  }

  public void informLayoutInvalid() {
    // This is called by the style properties object.
    this.forgetRenderState();
    final HTMLDocumentImpl doc = (HTMLDocumentImpl) this.document;
    if (doc != null) {
      doc.invalidated(this);
    }
  }

  public void informDocumentInvalid() {
    // This is called when an attribute or child changes.
    final HTMLDocumentImpl doc = (HTMLDocumentImpl) this.document;
    if (doc != null) {
      doc.allInvalidated(true);
    }
  }

  private RenderState renderState = null;

  public @NonNull RenderState getRenderState() {
    // Generally called from the GUI thread, except for
    // offset properties.
    synchronized (this.treeLock) {
      RenderState rs = this.renderState;
      rs = this.renderState;
      if (rs != null) {
        return rs;
      }
      final Object parent = this.parentNode;
      if ((parent != null) || (this instanceof Document)) {
        final RenderState prs = getParentRenderState(parent);
        rs = this.createRenderState(prs);
        this.renderState = rs;
        return rs;
      } else {
        // Scenario is possible due to Javascript.
        return BLANK_RENDER_STATE;
      }
    }
  }

  private final static RenderState getParentRenderState(final Object parent) {
    if (parent instanceof NodeImpl) {
      return ((NodeImpl) parent).getRenderState();
    } else {
      return null;
    }
  }

  // abstract protected RenderState createRenderState(final RenderState prevRenderState);
  protected @NonNull RenderState createRenderState(final RenderState prevRenderState) {
    if (prevRenderState == null) {
      return BLANK_RENDER_STATE;
    } else {
      return prevRenderState;
    }
  }

  protected void forgetRenderState() {
    synchronized (this.treeLock) {
      if (this.renderState != null) {
        this.renderState = null;
        // Note that getRenderState() "validates"
        // ancestor states as well.
        final java.util.ArrayList<Node> nl = this.nodeList;
        if (nl != null) {
          final Iterator<Node> i = nl.iterator();
          while (i.hasNext()) {
            ((NodeImpl) i.next()).forgetRenderState();
          }
        }
      }
    }
  }

  public String getInnerHTML() {
    final StringBuffer buffer = new StringBuffer();
    synchronized (this) {
      this.appendInnerHTMLImpl(buffer);
    }
    return buffer.toString();
  }

  protected void appendInnerHTMLImpl(final StringBuffer buffer) {
    final ArrayList<Node> nl = this.nodeList;
    int size;
    if ((nl != null) && ((size = nl.size()) > 0)) {
      for (int i = 0; i < size; i++) {
        final Node child = nl.get(i);
        if (child instanceof HTMLElementImpl) {
          ((HTMLElementImpl) child).appendOuterHTMLImpl(buffer);
        } else if (child instanceof Comment) {
          buffer.append("<!--" + ((Comment) child).getTextContent() + "-->");
        } else if (child instanceof Text) {
          final String text = ((Text) child).getTextContent();
          final String encText = this.htmlEncodeChildText(text);
          buffer.append(encText);
        } else if (child instanceof ProcessingInstruction) {
          buffer.append(child.toString());
        }
      }
    }
  }

  protected String htmlEncodeChildText(final String text) {
    return Strings.strictHtmlEncode(text, false);
  }

  /**
   * Attempts to convert the subtree starting at this point to a close text
   * representation. BR elements are converted to line breaks, and so forth.
   */
  public String getInnerText() {
    final StringBuffer buffer = new StringBuffer();
    synchronized (this.treeLock) {
      this.appendInnerTextImpl(buffer);
    }
    return buffer.toString();
  }

  protected void appendInnerTextImpl(final StringBuffer buffer) {
    final ArrayList<Node> nl = this.nodeList;
    if (nl == null) {
      return;
    }
    final int size = nl.size();
    if (size == 0) {
      return;
    }
    for (int i = 0; i < size; i++) {
      final Node child = nl.get(i);
      if (child instanceof ElementImpl) {
        ((ElementImpl) child).appendInnerTextImpl(buffer);
      }
      if (child instanceof Comment) {
        // skip
      } else if (child instanceof Text) {
        buffer.append(((Text) child).getTextContent());
      }
    }
  }

  /*
  protected void dispatchEventToHandlers(final Event event, final List<Function> handlers) {
    if (handlers != null) {
      // We clone the collection and check if original collection still contains
      // the handler before dispatching
      // This is to avoid ConcurrentModificationException during dispatch
      // TODO: Event Bubbling
      final ArrayList<Function> handlersCopy = new ArrayList<>(handlers);
      for (final Function h : handlersCopy) {
        if (handlers.contains(h)) {
          Executor.executeFunction(this, h, event);
        }
      }
    }
  }

  private final Map<String, List<Function>> onEventHandlers = new HashMap<>();

  public void addEventListener(final String type, final Function listener) {
    addEventListener(type, listener, false);
  }

  public void addEventListener(final String type, final Function listener, final boolean useCapture) {
    // TODO
    System.out.println("node by name: " + getNodeName() + " adding Event listener of type: " + type);

    List<Function> handlerList = null;
    if (onEventHandlers.containsKey(type)) {
      handlerList = onEventHandlers.get(type);
    } else {
      handlerList = new ArrayList<>();
      onEventHandlers.put(type, handlerList);
    }
    handlerList.add(listener);
  }

  public void removeEventListener(final String type, final Function listener, final boolean useCapture) {
    // TODO
    System.out.println("node remove Event listener: " + type);
    if (onEventHandlers.containsKey(type)) {
      onEventHandlers.get(type).remove(listener);
    }
  }

  public boolean dispatchEvent(final Event evt) {
    System.out.println("Dispatching event: " + evt);
    dispatchEventToHandlers(evt, onEventHandlers.get(evt.getType()));
    return false;
  }*/

  private volatile boolean attachedToDocument = this instanceof HTMLDocument;

  /**
   * @return the attachment with the document. true if the element is attached
   *         to the document, false otherwise. Document nodes are considered
   *         attached by default.
   */
  protected final boolean isAttachedToDocument() {
    return this.attachedToDocument;
  }

  /**
   * This method is intended to be overriden by subclasses that are interested
   * in processing their child-list whenever it is updated.
   */
  protected void handleChildListChanged() {

  }

  /**
   * This method is intended to be overriden by subclasses that are interested
   * in performing some operation when they are attached/detached from the
   * document.
   */
  protected void handleDocumentAttachmentChanged() {

  }

  /**
   * This method will be called on a node whenever it is being appended to a
   * parent node.
   *
   * NOTE: changeDocumentAttachment will call updateIds() which needs to be tree
   * locked, and hence these methods are also being tree locked
   */
  private void handleAddedToParent(final NodeImpl parent) {
    this.setParentImpl(parent);
    changeDocumentAttachment(parent.isAttachedToDocument());
  }

  /**
   * This method will be called on a node whenever it is being deleted from a
   * parent node.
   *
   * NOTE: changeDocumentAttachment will call updateIds() which needs to be tree
   * locked, and hence these methods are also being tree locked
   */
  private void handleDeletedFromParent() {
    this.setParentImpl(null);
    changeDocumentAttachment(false);
  }

  /**
   * This method will change the attachment of a node with the document. It will
   * also change the attachment of all its descendant nodes.
   *
   * @param attached
   *          the attachment with the document. true when attached, false
   *          otherwise.
   */
  private void changeDocumentAttachment(final boolean attached) {
    if (this.attachedToDocument != attached) {
      this.attachedToDocument = attached;
      handleDocumentAttachmentChanged();
      if (this instanceof ElementImpl) {
        final ElementImpl elementImpl = (ElementImpl) this;
        elementImpl.updateIdMap(attached);
      }
    }
    if (nodeList != null) {
      for (final Node node : this.nodeList) {
        if (node instanceof NodeImpl) {
          ((NodeImpl) node).changeDocumentAttachment(attached);
        }
      }
    }
  }

  /**
   * Common tasks to be performed when the NodeList of an element is changed.
   */
  private void postChildListChanged() {
    this.handleChildListChanged();

    if (!this.notificationsSuspended) {
      this.informStructureInvalid();
    }
  }

  /*
  public void addEventListener(final String type, final EventListener listener) {
    addEventListener(type, listener, false);
  }

  public void addEventListener(final String type, final EventListener listener, final boolean useCapture) {
    if (useCapture) {
      throw new UnSupportedOperationException();
    }
  }

  public void removeEventListener(final String type, final EventListener listener, final boolean useCapture) {
    // TODO Auto-generated method stub

  }

  public boolean dispatchEvent(final org.w3c.dom.events.Event evt) throws EventException {
    // TODO Auto-generated method stub
    return false;
  }*/

  public void addEventListener(final String type, final Function listener) {
    addEventListener(type, listener, false);
  }

  public void addEventListener(final String type, final Function listener, final boolean useCapture) {
    // TODO
    System.out.println("node by name: " + getNodeName() + " adding Event listener of type: " + type);
    // System.out.println("  txt content: " + getInnerText());
    ((HTMLDocumentImpl) getOwnerDocument()).getEventTargetManager().addEventListener(this, type, listener);
  }

  public void removeEventListener(final String type, final Function listener, final boolean useCapture) {
    // TODO
    System.out.println("node remove Event listener: " + type);
    ((HTMLDocumentImpl) getOwnerDocument()).getEventTargetManager().removeEventListener(this, type, listener, useCapture);
  }

  public boolean dispatchEvent(final Event evt) {
    System.out.println("Dispatching event: " + evt);
    // dispatchEventToHandlers(evt, onEventHandlers.get(evt.getType()));
    ((HTMLDocumentImpl) getOwnerDocument()).getEventTargetManager().dispatchEvent(this, evt);
    return false;
  }

  /*
  public void addEventListener(final String type, final EventListener listener) {
    addEventListener(type, listener, false);
  }

  public void addEventListener(final String type, final EventListener listener, final boolean useCapture) {
    if (useCapture) {
      throw new UnSupportedOperationException();
    }
  }

  public void removeEventListener(final String type, final EventListener listener, final boolean useCapture) {
    // TODO Auto-generated method stub

  }

  public boolean dispatchEvent(final org.w3c.dom.events.Event evt) throws EventException {
    // TODO Auto-generated method stub
    return false;
  }*/

  public Element querySelector(final String query) {
    // TODO: Optimize: Avoid getting all matches. Only first match is sufficient.
    final NodeList matchingElements = querySelectorAll(query);
    if (matchingElements.getLength() > 0) {
      return (Element) matchingElements.item(0);
    } else {
      return null;
    }
  }

  private static CombinedSelector[] makeSelectors(final String query) throws IOException, CSSException {
    // this is quick way to parse the selectors. TODO: check if jStyleParser supports a better option.
    final String tempBlock = query + " { display: none}";
    final StyleSheet styleSheet = CSSFactory.parseString(tempBlock, null);
    final RuleSet firstRuleBlock = (RuleSet) styleSheet.get(0);
    final CombinedSelector[] selectors = firstRuleBlock.getSelectors();
    return selectors;
  }

  /*
  protected Collection<Node> getMatchingChildren(CombinedSelector selectors) {
    final Collection<Node> matchingElements = new LinkedList<>();
    final NodeImpl[] childrenArray = getChildrenArray();
    if (childrenArray != null) {
      for (final NodeImpl n : childrenArray) {
        if (n instanceof ElementImpl) {
          final ElementImpl element = (ElementImpl) n;
          if (selectors.stream().anyMatch(selector -> selector.matches(element))) {
            System.out.println("Found match: " + element + " of class: " + element.getClass());
            matchingElements.add(element);
          }
          matchingElements.addAll(element.getMatchingChildren(selectors));
        }
      }
    }
    return matchingElements;
  }*/

  protected Collection<Node> getMatchingChildren(final List<Selector> selectors) {
    final Collection<Node> matchingElements = new LinkedList<>();
    final int numSelectors = selectors.size();
    if (numSelectors > 0) {
      final Selector firstSelector = selectors.get(0);
      final NodeImpl[] childrenArray = getChildrenArray();
      if (childrenArray != null) {
        for (final NodeImpl n : childrenArray) {
          if (n instanceof ElementImpl) {
            final ElementImpl element = (ElementImpl) n;
            if (firstSelector.matches(element)) {
              if (numSelectors > 1) {
                 final List<Selector> tailSelectors = selectors.subList(1, numSelectors);
                 matchingElements.addAll(element.getMatchingChildren(tailSelectors));
              } else {
                matchingElements.add(element);
              }
            }
            matchingElements.addAll(element.getMatchingChildren(selectors));
          }
        }
      }
    }
    return matchingElements;
  }

  public NodeList querySelectorAll(final String query) {
    try {
      final CombinedSelector[] selectors = makeSelectors(query);
      final LinkedList<Node> matches = new LinkedList<>();
      for (final CombinedSelector selector : selectors) {
        matches.addAll(getMatchingChildren(selector));
      }
      return new NodeListImpl(matches);
    } catch (final IOException | CSSException e) {
      e.printStackTrace();
      throw new DOMException(DOMException.SYNTAX_ERR, "Couldn't parse selector: " + query);
    }
  }

  public NodeList getElementsByClassName(final String classNames) {
    final String[] classNamesArray = classNames.split("\\s");
    // TODO: escape commas in class-names
    final String query = Arrays.stream(classNamesArray)
        .filter(cn -> cn.length() > 0)
        .map(cn -> "." + cn)
        .collect(Collectors.joining(","));
    return querySelectorAll(query);
  }

  public NodeList getElementsByTagName(final String classNames) {
    final String[] classNamesArray = classNames.split("\\s");
    // TODO: escape commas in class-names
    final String query = Arrays.stream(classNamesArray).collect(Collectors.joining(","));
    return querySelectorAll(query);
  }

  // TODO: This is a plug
  public String getNameSpaceURI() {
    final short nodeType = getNodeType();
    if (nodeType == ELEMENT_NODE || nodeType == ATTRIBUTE_NODE) {
      return "http://www.w3.org/1999/xhtml";
    } else {
      return null;
    }
  }
}
