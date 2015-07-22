package org.lobobrowser.primary.clientlets;

import org.lobobrowser.html.domimpl.NodeImpl;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

public final class SimpleDocument extends NodeImpl implements Document {
  private final String mimeType;

  public SimpleDocument(final String mimeType) {
    this.mimeType = mimeType;
  }

  @Override
  public Scriptable getScriptable() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getContentType() {
    return mimeType;
  }

  @Override
  public DocumentType getDoctype() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DOMImplementation getImplementation() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Element getDocumentElement() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Element createElement(String tagName) throws DOMException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DocumentFragment createDocumentFragment() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Text createTextNode(String data) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Comment createComment(String data) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CDATASection createCDATASection(String data) throws DOMException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ProcessingInstruction createProcessingInstruction(String target, String data) throws DOMException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Attr createAttribute(String name) throws DOMException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public EntityReference createEntityReference(String name) throws DOMException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Node importNode(Node importedNode, boolean deep) throws DOMException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Element createElementNS(String namespaceURI, String qualifiedName) throws DOMException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Attr createAttributeNS(String namespaceURI, String qualifiedName) throws DOMException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Element getElementById(String elementId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getInputEncoding() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getXmlEncoding() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean getXmlStandalone() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void setXmlStandalone(boolean xmlStandalone) throws DOMException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public String getXmlVersion() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setXmlVersion(String xmlVersion) throws DOMException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean getStrictErrorChecking() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void setStrictErrorChecking(boolean strictErrorChecking) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public String getDocumentURI() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setDocumentURI(String documentURI) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Node adoptNode(Node source) throws DOMException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DOMConfiguration getDomConfig() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void normalizeDocument() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Node renameNode(Node n, String namespaceURI, String qualifiedName) throws DOMException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected Node createSimilarNode() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getLocalName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getNodeName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getNodeValue() throws DOMException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setNodeValue(String nodeValue) throws DOMException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public short getNodeType() {
    // TODO Auto-generated method stub
    return 0;
  }
  
}