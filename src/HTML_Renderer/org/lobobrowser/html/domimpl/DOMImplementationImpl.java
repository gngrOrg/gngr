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
 * Created on Oct 15, 2005
 */
package org.lobobrowser.html.domimpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.lobobrowser.html.parser.HtmlParser;
import org.lobobrowser.ua.UserAgentContext;
import org.unbescape.xml.XmlEscape;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.xml.sax.SAXException;

public class DOMImplementationImpl implements DOMImplementation {
  private final UserAgentContext context;

  public DOMImplementationImpl(final UserAgentContext context) {
    this.context = context;
  }

  public boolean hasFeature(final String feature, final String version) {
    return "HTML".equals(feature) && ("2.0".compareTo(version) <= 0);
  }

  public DocumentType createDocumentType(final String qualifiedName, final String publicId, final String systemId) throws DOMException {
    return new DocumentTypeImpl(qualifiedName, publicId, systemId);
  }

  // TODO: Use default parameter values instead of replicating function. GH #126
  public Document createDocument(final String namespaceURI, final String qualifiedName) throws DOMException {
    return createDocument(namespaceURI, qualifiedName, null);
  }

  public Document createDocument(final String namespaceURI, final String qualifiedName, final DocumentType doctype) throws DOMException {
    return new HTMLDocumentImpl(this.context);
  }

  public Object getFeature(final String feature, final String version) {
    if ("HTML".equals(feature) && ("2.0".compareTo(version) <= 0)) {
      return this;
    } else {
      return null;
    }
  }

  public Document createHTMLDocument(final String title) throws DOMException {
    // TODO: Should a new context / null context be used?
    final HTMLDocumentImpl doc = new HTMLDocumentImpl(this.context);
    final HtmlParser parser = new HtmlParser(context, doc);
    final String escapedTitle = XmlEscape.escapeXml11(title);
    final String initString = "<html><head><title>" + escapedTitle + "</title><body></body></html>";
    try {
      parser.parse(new ByteArrayInputStream(initString.getBytes()));
    } catch (IOException | SAXException e) {
      throw new RuntimeException("Couldn't create HTML Document", e);
    }
    return doc;
  }

}
