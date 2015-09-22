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
 * Created on Oct 29, 2005
 */
package org.lobobrowser.html.domimpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.lobobrowser.util.Strings;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.TypeInfo;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventException;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

public class ElementImpl extends NodeImpl implements Element, EventTarget {
  private final String name;

  public ElementImpl(final String name) {
    super();
    this.name = name;
  }

  protected Map<String, String> attributes;

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.NodeImpl#getattributes()
   */
  @Override
  public NamedNodeMap getAttributes() {
    synchronized (this) {
      Map<String, String> attrs = this.attributes;

      // TODO: Check if NamedNodeMapImpl can be changed to dynamically query the attributes field
      //       instead of keeping a reference to it. This will allow the NamedNodeMap to be live as well
      //       as avoid allocating of a HashMap here when attributes are empty.
      if (attrs == null) {
        attrs = new HashMap<>();
        this.attributes = attrs;
      }
      return new NamedNodeMapImpl(this, this.attributes);
    }
  }

  @Override
  public boolean hasAttributes() {
    synchronized (this) {
      final Map<String, String> attrs = this.attributes;
      return attrs == null ? false : !attrs.isEmpty();
    }
  }

  @Override
  public boolean equalAttributes(final Node arg) {
    if (arg instanceof ElementImpl) {
      synchronized (this) {
        Map<String, String> attrs1 = this.attributes;
        if (attrs1 == null) {
          attrs1 = Collections.emptyMap();
        }
        Map<String, String> attrs2 = ((ElementImpl) arg).attributes;
        if (attrs2 == null) {
          attrs2 = Collections.emptyMap();
        }
        return java.util.Objects.equals(attrs1, attrs2);
      }
    } else {
      return false;
    }
  }

  public String getId() {
    // TODO: Check if a cache is useful for this attribute. Original gngr code had a cache here.
    final String id = this.getAttribute("id");
    return id == null ? "" : id;
  }

  public void setId(final String id) {
    this.setAttribute("id", id);
  }

  // private String title;

  public String getTitle() {
    return this.getAttribute("title");
  }

  public void setTitle(final String title) {
    this.setAttribute("title", title);
  }

  public String getLang() {
    return this.getAttribute("lang");
  }

  public void setLang(final String lang) {
    this.setAttribute("lang", lang);
  }

  public String getDir() {
    return this.getAttribute("dir");
  }

  public void setDir(final String dir) {
    this.setAttribute("dir", dir);
  }

  public final String getAttribute(final String name) {
    final String normalName = normalizeAttributeName(name);
    synchronized (this) {
      final Map<String, String> attributes = this.attributes;
      return attributes == null ? null : attributes.get(normalName);
    }
  }

  private Attr getAttr(final String normalName, final String value) {
    // TODO: "specified" attributes
    return new AttrImpl(normalName, value, true, this, "id".equals(normalName));
  }

  public Attr getAttributeNode(final String name) {
    final String normalName = normalizeAttributeName(name);
    synchronized (this) {
      final Map<String, String> attributes = this.attributes;
      final String value = attributes == null ? null : attributes.get(normalName);
      return value == null ? null : this.getAttr(normalName, value);
    }
  }

  public Attr getAttributeNodeNS(final String namespaceURI, final String localName) throws DOMException {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Namespaces not supported");
  }

  public String getAttributeNS(final String namespaceURI, final String localName) throws DOMException {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Namespaces not supported");
  }

  protected static boolean isTagName(final Node node, final String name) {
    return node.getNodeName().equalsIgnoreCase(name);
  }

  public NodeList getElementsByTagName(final String name) {
    final boolean matchesAll = "*".equals(name);
    final List<Node> descendents = new LinkedList<>();
    synchronized (this.treeLock) {
      final ArrayList<Node> nl = this.nodeList;
      if (nl != null) {
        final Iterator<Node> i = nl.iterator();
        while (i.hasNext()) {
          final Node child = i.next();
          if (child instanceof Element) {
            final Element childElement = (Element) child;
            if (matchesAll || isTagName(childElement, name)) {
              descendents.add(child);
            }
            final NodeList sublist = childElement.getElementsByTagName(name);
            final int length = sublist.getLength();
            for (int idx = 0; idx < length; idx++) {
              descendents.add(sublist.item(idx));
            }
          }
        }
      }
    }
    return new NodeListImpl(descendents);
  }

  public NodeList getElementsByTagNameNS(final String namespaceURI, final String localName) throws DOMException {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Namespaces not supported");
  }

  public TypeInfo getSchemaTypeInfo() {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Namespaces not supported");
  }

  public String getTagName() {
    // In HTML, tag names are supposed to be returned in upper-case, but in XHTML they are returned in original case
    // as per https://developer.mozilla.org/en-US/docs/Web/API/Element.tagName
    return this.getNodeName().toUpperCase();
  }

  public boolean hasAttribute(final String name) {
    final String normalName = normalizeAttributeName(name);
    synchronized (this) {
      final Map<String, String> attributes = this.attributes;
      return attributes == null ? false : attributes.containsKey(normalName);
    }
  }

  public boolean hasAttributeNS(final String namespaceURI, final String localName) throws DOMException {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Namespaces not supported");
  }

  public void removeAttribute(final String name) throws DOMException {
    changeAttribute(name, null);
  }

  public Attr removeAttributeNode(final Attr oldAttr) throws DOMException {
    final String attrName = oldAttr.getName();
    final String oldValue = changeAttribute(attrName, null);

    final String normalName = normalizeAttributeName(attrName);
    return oldValue == null ? null : this.getAttr(normalName, oldValue);
  }

  public void removeAttributeNS(final String namespaceURI, final String localName) throws DOMException {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Namespaces not supported");
  }

  /*
  protected void assignAttributeField(final String normalName, final String value) {
    // Note: overriders assume that processing here is only done after
    // checking attribute names, i.e. they may not call the super
    // implementation if an attribute is already taken care of.

    // TODO: Need to move this to a separate function, similar to updateIdMap()
    // TODO: Need to update the name map, whenever attachment changes
    if (isAttachedToDocument()) {
      final HTMLDocumentImpl document = (HTMLDocumentImpl) this.document;
      if ("name".equals(normalName)) {
        final String oldName = this.getAttribute("name");
        if (oldName != null) {
          document.removeNamedItem(oldName);
        }
        document.setNamedItem(value, this);
      }
    }
  }*/

  protected final static String normalizeAttributeName(final String name) {
    return name.toLowerCase();
  }

  public void setAttribute(final String name, final String value) throws DOMException {
    // Convert null to "null" : String.
    // This is how Firefox behaves and is also consistent with DOM 3
    final String valueNonNull = value == null ? "null" : value;
    changeAttribute(name, valueNonNull);
  }

  public Attr setAttributeNode(final Attr newAttr) throws DOMException {
    changeAttribute(newAttr.getName(), newAttr.getValue());

    return newAttr;
  }

  public Attr setAttributeNodeNS(final Attr newAttr) throws DOMException {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Namespaces not supported");
  }

  public void setAttributeNS(final String namespaceURI, final String qualifiedName, final String value) throws DOMException {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Namespaces not supported");
  }

  public void setIdAttribute(final String name, final boolean isId) throws DOMException {
    final String normalName = normalizeAttributeName(name);
    if (!"id".equals(normalName)) {
      throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "IdAttribute can't be anything other than ID");
    }
  }

  public void setIdAttributeNode(final Attr idAttr, final boolean isId) throws DOMException {
    final String normalName = normalizeAttributeName(idAttr.getName());
    if (!"id".equals(normalName)) {
      throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "IdAttribute can't be anything other than ID");
    }
  }

  public void setIdAttributeNS(final String namespaceURI, final String localName, final boolean isId) throws DOMException {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Namespaces not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.NodeImpl#getLocalName()
   */
  @Override
  public String getLocalName() {
    return this.getNodeName();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.NodeImpl#getNodeName()
   */
  @Override
  public String getNodeName() {
    return this.name;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.NodeImpl#getNodeType()
   */
  @Override
  public short getNodeType() {
    return Node.ELEMENT_NODE;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.NodeImpl#getNodeValue()
   */
  @Override
  public String getNodeValue() throws DOMException {
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.NodeImpl#setNodeValue(java.lang.String)
   */
  @Override
  public void setNodeValue(final String nodeValue) throws DOMException {
    // nop
  }

  /**
   * Gets inner text of the element, possibly including text in comments. This
   * can be used to get Javascript code out of a SCRIPT element.
   *
   * @param includeComment
   */
  protected String getRawInnerText(final boolean includeComment) {
    synchronized (this.treeLock) {
      final ArrayList<Node> nl = this.nodeList;
      if (nl != null) {
        final Iterator<Node> i = nl.iterator();
        StringBuffer sb = null;
        while (i.hasNext()) {
          final Object node = i.next();
          if (node instanceof Text) {
            final Text tn = (Text) node;
            final String txt = tn.getNodeValue();
            if (!"".equals(txt)) {
              if (sb == null) {
                sb = new StringBuffer();
              }
              sb.append(txt);
            }
          } else if (node instanceof ElementImpl) {
            final ElementImpl en = (ElementImpl) node;
            final String txt = en.getRawInnerText(includeComment);
            if (!"".equals(txt)) {
              if (sb == null) {
                sb = new StringBuffer();
              }
              sb.append(txt);
            }
          } else if (includeComment && (node instanceof Comment)) {
            final Comment cn = (Comment) node;
            final String txt = cn.getNodeValue();
            if (!"".equals(txt)) {
              if (sb == null) {
                sb = new StringBuffer();
              }
              sb.append(txt);
            }
          }
        }
        return sb == null ? "" : sb.toString();
      } else {
        return "";
      }
    }
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer();
    sb.append(this.getNodeName());
    sb.append(" [");
    final NamedNodeMap attribs = this.getAttributes();
    final int length = attribs.getLength();
    for (int i = 0; i < length; i++) {
      final Attr attr = (Attr) attribs.item(i);
      sb.append(attr.getNodeName());
      sb.append('=');
      sb.append(attr.getNodeValue());
      if ((i + 1) < length) {
        sb.append(',');
      }
    }
    sb.append("]");
    return sb.toString();
  }

  public void setInnerText(final String newText) {
    // TODO: Is this check for owner document really required?
    final org.w3c.dom.Document document = this.document;
    if (document == null) {
      this.warn("setInnerText(): Element " + this + " does not belong to a document.");
      return;
    }

    removeAllChildrenImpl();

    // Create node and call appendChild outside of synchronized block.
    final Node textNode = document.createTextNode(newText);
    this.appendChild(textNode);
  }

  @Override
  protected Node createSimilarNode() {
    final HTMLDocumentImpl doc = (HTMLDocumentImpl) this.document;
    return doc == null ? null : doc.createElement(this.getTagName());
  }

  @Override
  protected String htmlEncodeChildText(final String text) {
    if (org.lobobrowser.html.parser.HtmlParser.isDecodeEntities(this.name)) {
      return Strings.strictHtmlEncode(text, false);
    } else {
      return text;
    }
  }

  /**
   * To be overridden by Elements that need a notification of attribute changes.
   *
   * This is called only when the element is attached to a document at the time
   * the attribute is changed. If an attribute is changed while not attached to
   * a document, this function is *not* called when the element is attached to a
   * document. We chose this design because it covers our current use cases
   * well.
   *
   * If, in the future, a notification is always desired then the design can be
   * altered easily later.
   *
   * @param name
   *          normalized name
   * @param oldValue
   *          null, if the attribute was absent
   * @param newValue
   *          null, if the attribute is now removed
   */
  protected void handleAttributeChanged(final String name, final String oldValue, final String newValue) {
    // TODO: Need to move this to a separate function, similar to updateIdMap()
    // TODO: Need to update the name map, whenever attachment changes
      final HTMLDocumentImpl document = (HTMLDocumentImpl) this.document;
      if ("name".equals(name)) {
        if (oldValue != null) {
          document.removeNamedItem(oldValue);
        }
        document.setNamedItem(newValue, this);
      }
  }

  /**
   * changes an attribute to the specified value. If the specified value is
   * null, the attribute is removed
   *
   * @return the old attribute value. null if not set previously.
   */
  private String changeAttribute(final String name, final String newValue) {
    final String normalName = normalizeAttributeName(name);

    String oldValue = null;
    synchronized (this) {
      if (newValue == null) {
        if (attributes != null) {
          oldValue = attributes.remove(normalName);
        }
      } else {
        if (attributes == null) {
          attributes = new HashMap<>(2);
        }

        oldValue = attributes.put(normalName, newValue);
      }
    }

    if ("id".equals(normalName)) {
      updateIdMap(oldValue, newValue);
    }

    if (isAttachedToDocument()) {
      handleAttributeChanged(normalName, oldValue, newValue);
    }

    return oldValue;
  }

  protected void updateIdMap(final boolean isAttached) {
    if (hasAttribute("id")) {
      final String id = getId();
      if (isAttached) {
        ((HTMLDocumentImpl) document).setElementById(id, this);
      } else {
        ((HTMLDocumentImpl) document).removeElementById(getId());
      }
    }
  }

  private void updateIdMap(final String oldIdValue, final String newIdValue) {
    if (isAttachedToDocument() && !java.util.Objects.equals(oldIdValue, newIdValue)) {
      if (oldIdValue != null) {
        ((HTMLDocumentImpl) document).removeElementById(oldIdValue);
      }
      if (newIdValue != null) {
        ((HTMLDocumentImpl) document).setElementById(newIdValue, this);
      }
    }
  }

  // TODO: GH #88 Need to implement these for Document and DocumentFragment as part of ParentNode API
  public Element getFirstElementChild() {
    final ArrayList<Node> nl = this.nodeList;
    for (final Node n : nl) {
      if (n instanceof Element) {
        return (Element) n;
      }
    }

    return null;
  }

  public Element getLastElementChild() {
    final ArrayList<Node> nl = this.nodeList;
    final int N = nl.size();
    for (int i = N - 1; i >= 0; i--) {
      final Node n = nl.get(i);
      if (n instanceof Element) {
        return (Element) n;
      }
    }

    return null;
  }

  public int getChildElementCount() {
    final ArrayList<Node> nl = this.nodeList;
    int count = 0;
    for (final Node n : nl) {
      if (n instanceof Element) {
        count++;
      }
    }

    return count;
  }

  @Override
  public void addEventListener(String type, EventListener listener, boolean useCapture) {
    // TODO Auto-generated method stub
    System.out.println("TODO: addEventListener() in ElementImpl");
  }

  @Override
  public void removeEventListener(String type, EventListener listener, boolean useCapture) {
    // TODO Auto-generated method stub
    System.out.println("TODO: removeEventListener() in ElementImpl");
  }

  @Override
  public boolean dispatchEvent(Event evt) throws EventException {
    // TODO Auto-generated method stub
    System.out.println("TODO: dispatchEvent() in ElementImpl");
    return false;
  }
}
