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
package org.lobobrowser.html.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;

import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.domimpl.DOMImplementationImpl;
import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.io.WritableLineReader;
import org.lobobrowser.ua.UserAgentContext;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * The <code>DocumentBuilderImpl</code> class is an HTML DOM parser that
 * implements the standard W3C <code>DocumentBuilder</code> interface.
 *
 * @author J. H. S.
 */
public class DocumentBuilderImpl extends DocumentBuilder {
  private static final Logger logger = Logger.getLogger(DocumentBuilderImpl.class.getName());
  private EntityResolver resolver;
  private ErrorHandler errorHandler;
  private final UserAgentContext bcontext;
  private final HtmlRendererContext rcontext;

  /**
   * Constructs a <code>DocumentBuilderImpl</code>. This constructor should be
   * used when only the parsing functionality (without rendering) is required.
   *
   * @param context
   *          An instance of {@link org.lobobrowser.html.UserAgentContext},
   *          which may be an instance of
   *          {@link org.lobobrowser.html.test.SimpleUserAgentContext}.
   */
  public DocumentBuilderImpl(final UserAgentContext context) {
    this.rcontext = null;
    this.bcontext = context;
  }

  /**
   * Constructs a <code>DocumentBuilderImpl</code>. This constructor should be
   * used when rendering is expected.
   *
   * @param ucontext
   *          An instance of {@link org.lobobrowser.html.UserAgentContext},
   *          which may be an instance of
   *          {@link org.lobobrowser.html.test.SimpleUserAgentContext}.
   * @param rcontext
   *          An instance of {@link org.lobobrowser.html.HtmlRendererContext},
   *          which may be an instance of
   *          {@link org.lobobrowser.html.test.SimpleHtmlRendererContext}.
   */
  public DocumentBuilderImpl(final UserAgentContext ucontext, final HtmlRendererContext rcontext) {
    this.rcontext = rcontext;
    this.bcontext = ucontext;
  }

  /**
   * Constructs a <code>DocumentBuilderImpl</code>. This constructor should be
   * used when rendering is expected.
   *
   * @param rcontext
   *          An instance of {@link org.lobobrowser.html.HtmlRendererContext},
   *          which may be an instance of
   *          {@link org.lobobrowser.html.test.SimpleHtmlRendererContext}.
   */
  public DocumentBuilderImpl(final HtmlRendererContext rcontext) {
    this.rcontext = rcontext;
    this.bcontext = rcontext.getUserAgentContext();
  }

  /**
   * Parses an HTML document. Note that this method will read the entire input
   * source before returning a <code>Document</code> instance.
   *
   * @param is
   *          The input source, which may be an instance of
   *          {@link org.lobobrowser.html.parser.InputSourceImpl}.
   * @see #createDocument(InputSource)
   */
  @Override
  public Document parse(final InputSource is) throws org.xml.sax.SAXException, IOException {
    final HTMLDocumentImpl document = (HTMLDocumentImpl) this.createDocument(is, "");
    document.load();
    return document;
  }

  /**
   * Creates a document without parsing the input provided, so the document
   * object can be used for incremental rendering.
   *
   * @param is
   *          The input source, which may be an instance of
   *          {@link org.lobobrowser.html.parser.InputSourceImpl}. The input
   *          source must provide either an input stream or a reader.
   * @see HTMLDocumentImpl#load()
   */
  public Document createDocument(final InputSource is, final String contentType) throws SAXException, IOException {
    final String encoding = is.getEncoding();
    String charset = encoding;
    if (charset == null) {
      charset = "US-ASCII";
    }
    final String uri = is.getSystemId();
    if (uri == null) {
      logger.warning("parse(): InputSource has no SystemId (URI); document item URLs will not be resolvable.");
    }
    WritableLineReader wis;
    final Reader reader = is.getCharacterStream();
    if (reader != null) {
      wis = new WritableLineReader(reader);
    } else {
      final InputStream in = is.getByteStream();
      if (in != null) {
        wis = new WritableLineReader(new InputStreamReader(in, charset));
      } else if (uri != null) {
        throw new IllegalArgumentException("The input source didn't have a character stream, nor an inputstream!");
        /*
        // To comply with the InputSource documentation, we need
        // to do this:
        final java.net.URLConnection connection = new java.net.URL(uri).openConnection();
        in = connection.getInputStream();
        if (encoding == null) {
          charset = org.lobobrowser.util.Urls.getCharset(connection);
        }
        wis = new WritableLineReader(new InputStreamReader(in, charset));
         */
      } else {
        throw new IllegalArgumentException("The InputSource must have either a reader, an input stream or a URI.");
      }
    }
    final HTMLDocumentImpl document = new HTMLDocumentImpl(this.bcontext, this.rcontext, wis, uri, contentType);
    return document;
  }

  @Override
  public boolean isNamespaceAware() {
    return false;
  }

  @Override
  public boolean isValidating() {
    return false;
  }

  @Override
  public void setEntityResolver(final EntityResolver er) {
    this.resolver = er;
  }

  @Override
  public void setErrorHandler(final ErrorHandler eh) {
    this.errorHandler = eh;
  }

  @Override
  public Document newDocument() {
    return new HTMLDocumentImpl(this.bcontext);
  }

  private DOMImplementation domImplementation;

  @Override
  public DOMImplementation getDOMImplementation() {
    synchronized (this) {
      if (this.domImplementation == null) {
        this.domImplementation = new DOMImplementationImpl(this.bcontext);
      }
      return this.domImplementation;
    }
  }

  public ErrorHandler getErrorHandler() {
    return errorHandler;
  }

  public EntityResolver getResolver() {
    return resolver;
  }
}
