/*
 * HtmlProcessingInstruction.java
 * Selima Prague FBI Project
 * 5th-March-2008
 */
package org.lobobrowser.html.domimpl;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;

/**
 * HTML DOM object representing processing instruction as per HTML 4.0
 * specification.
 *
 * @author vitek
 */
public class HTMLProcessingInstruction extends NodeImpl implements ProcessingInstruction, Cloneable {
  String target;
  String data;

  public HTMLProcessingInstruction(final String target, final String data) {
    this.target = target;
    this.data = data;
  }

  @Override
  protected Node createSimilarNode() {
    return (Node) clone();
  }

  @Override
  public String getLocalName() {
    return target;
  }

  @Override
  public String getNodeName() {
    return target;
  }

  @Override
  public short getNodeType() {
    return Node.PROCESSING_INSTRUCTION_NODE;
  }

  @Override
  public String getNodeValue() throws DOMException {
    return data;
  }

  @Override
  public void setNodeValue(final String nodeValue) throws DOMException {
    this.data = nodeValue;
  }

  public String getData() {
    return data;
  }

  public String getTarget() {
    return target;
  }

  public void setData(final String data) throws DOMException {
    this.data = data;
  }

  @Override
  public Object clone() {
    try {
      return super.clone();
    } catch (final CloneNotSupportedException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public String toString() {
    return "<?" + target + " " + data + ">";
  }
}
