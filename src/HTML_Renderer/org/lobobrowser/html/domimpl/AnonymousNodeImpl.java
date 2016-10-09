package org.lobobrowser.html.domimpl;

import java.util.ArrayList;

import org.lobobrowser.js.HideFromJS;
import org.lobobrowser.util.NotImplementedYetException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

public class AnonymousNodeImpl extends NodeImpl {
  public AnonymousNodeImpl(Node parentNode) {
    setParentImpl(parentNode);
  }

  @Override
  protected Node createSimilarNode() {
    throw new NotImplementedYetException();
  }

  @Override
  public String getLocalName() {
    return "";
  }

  @Override
  public String getNodeName() {
    return "";
  }

  @Override
  public String getNodeValue() throws DOMException {
    return null;
  }

  @Override
  public void setNodeValue(String nodeValue) throws DOMException {
    // nop
  }

  @Override
  public short getNodeType() {
    return Node.ELEMENT_NODE;
  }

  /** Append child without informing the child of the new parent */
  @HideFromJS
  public void appendChildSilently(NodeImpl c) {
    synchronized (this.treeLock) {
      ArrayList<Node> nl = this.nodeList;
      if (nl == null) {
        nl = new ArrayList<>(3);
        this.nodeList = nl;
      }
      nl.add(c);
    }
  }

  @Override
  public String toString() {
    return "Anonymous child of " + getParentNode();
  }
}
