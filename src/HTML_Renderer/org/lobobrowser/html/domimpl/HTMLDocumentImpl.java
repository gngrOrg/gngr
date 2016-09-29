/*    GNU LESSER GENERAL PUBLIC LICENSE
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
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.domimpl.NodeFilter.AnchorFilter;
import org.lobobrowser.html.domimpl.NodeFilter.AppletFilter;
import org.lobobrowser.html.domimpl.NodeFilter.ElementFilter;
import org.lobobrowser.html.domimpl.NodeFilter.ElementNameFilter;
import org.lobobrowser.html.domimpl.NodeFilter.FormFilter;
import org.lobobrowser.html.domimpl.NodeFilter.FrameFilter;
import org.lobobrowser.html.domimpl.NodeFilter.ImageFilter;
import org.lobobrowser.html.domimpl.NodeFilter.LinkFilter;
import org.lobobrowser.html.domimpl.NodeFilter.TagNameFilter;
import org.lobobrowser.html.io.WritableLineReader;
import org.lobobrowser.html.js.Event;
import org.lobobrowser.html.js.EventTargetManager;
import org.lobobrowser.html.js.Location;
import org.lobobrowser.html.js.Window;
import org.lobobrowser.html.js.Window.JSRunnableTask;
import org.lobobrowser.html.parser.HtmlParser;
import org.lobobrowser.html.style.CSSNorm;
import org.lobobrowser.html.style.RenderState;
import org.lobobrowser.html.style.StyleElements;
import org.lobobrowser.html.style.StyleSheetRenderState;
import org.lobobrowser.js.HideFromJS;
import org.lobobrowser.request.DomainValidation;
import org.lobobrowser.ua.ImageResponse;
import org.lobobrowser.ua.NetworkRequest;
import org.lobobrowser.ua.UserAgentContext;
import org.lobobrowser.ua.UserAgentContext.Request;
import org.lobobrowser.ua.UserAgentContext.RequestKind;
import org.lobobrowser.util.SecurityUtil;
import org.lobobrowser.util.Urls;
import org.lobobrowser.util.WeakValueHashMap;
import org.lobobrowser.util.io.EmptyReader;
import org.mozilla.javascript.Function;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.UserDataHandler;
import org.w3c.dom.css.CSSStyleSheet;
import org.w3c.dom.events.EventException;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLCollection;
import org.w3c.dom.html.HTMLDocument;
import org.w3c.dom.html.HTMLElement;
import org.w3c.dom.ranges.Range;
import org.w3c.dom.stylesheets.DocumentStyle;
import org.w3c.dom.stylesheets.LinkStyle;
import org.w3c.dom.stylesheets.StyleSheetList;
import org.w3c.dom.views.AbstractView;
import org.w3c.dom.views.DocumentView;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import co.uproot.css.domimpl.JStyleSheetWrapper;
import co.uproot.css.domimpl.StyleSheetBridge;
import cz.vutbr.web.css.CSSException;
import cz.vutbr.web.css.ElementMatcher;
import cz.vutbr.web.css.MediaSpec;
import cz.vutbr.web.css.StyleSheet;
import cz.vutbr.web.csskit.ElementMatcherSafeCS;
import cz.vutbr.web.csskit.ElementMatcherSafeStd;
import cz.vutbr.web.csskit.antlr4.CSSParserFactory;
import cz.vutbr.web.csskit.antlr4.CSSParserFactory.SourceType;
import cz.vutbr.web.domassign.Analyzer.Holder;
import cz.vutbr.web.domassign.AnalyzerUtil;

/**
 * Implementation of the W3C <code>HTMLDocument</code> interface.
 */
public class HTMLDocumentImpl extends NodeImpl implements HTMLDocument, DocumentView, DocumentStyle, EventTarget {
  private final ElementFactory factory;
  private final HtmlRendererContext rcontext;
  private final UserAgentContext ucontext;
  private final Window window;
  private final Map<String, Element> elementsById = new WeakValueHashMap<>();
  private String documentURI;
  private java.net.URL documentURL;
  protected final StyleSheetManager styleSheetManager = new StyleSheetManager();
  private final String contentType;

  private WritableLineReader reader;

  public HTMLDocumentImpl(final HtmlRendererContext rcontext) {
    this(rcontext.getUserAgentContext(), rcontext, null, null, null);
  }

  public HTMLDocumentImpl(final UserAgentContext ucontext) {
    this(ucontext, null, null, null, null);
  }

  public HTMLDocumentImpl(final UserAgentContext ucontext, final HtmlRendererContext rcontext, final WritableLineReader reader,
      final String documentURI, final String contentType) {
    this.factory = ElementFactory.getInstance();
    this.rcontext = rcontext;
    this.ucontext = ucontext;
    this.reader = reader;
    this.documentURI = documentURI;
    this.contentType = contentType;
    try {
      final java.net.URL docURL = new java.net.URL(documentURI);
      final SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
        // Do not allow creation of HTMLDocumentImpl if there's
        // no permission to connect to the host of the URL.
        // This is so that cookies cannot be written arbitrarily
        // with setCookie() method.
        final String protocol = docURL.getProtocol();
        if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol)) {
        sm.checkPermission(new java.net.SocketPermission(docURL.getHost(), "connect"));
        }
      }
      this.documentURL = docURL;
      this.domain = docURL.getHost();
    } catch (final java.net.MalformedURLException mfu) {
      logger.warning("HTMLDocumentImpl(): Document URI [" + documentURI + "] is malformed.");
    }

    // TODO: This should be inside the try block above. That is, if there is a malformed URL, the below shouldn't be allowed.
    //       It is currently being allowed to quickly bootstrap and run web-platform-tests.
    //       One failure case is: The methods in DOMImplemenationImpl call those constructors which have null document URIs.
    //       Such constructors should be ideally removed.
    this.document = this;
    // Get Window object
    Window window;
    if (rcontext != null) {
      window = Window.getWindow(rcontext);
    } else {
      // Plain parsers may use Javascript too.
      window = new Window(null, ucontext);
    }
    // Window must be retained or it will be garbage collected.
    this.window = window;
    window.setDocument(this);
  }

  private Set<Locale> locales;

  /**
   * Gets an <i>immutable</i> set of locales previously set for this document.
   */
  public Set<Locale> getLocales() {
    return locales;
  }

  /**
   * Sets the locales of the document. This helps determine whether specific
   * fonts can display text in the languages of all the locales.
   *
   * @param locales
   *          An <i>immutable</i> set of <code>java.util.Locale</code>
   *          instances.
   */
  public void setLocales(final Set<Locale> locales) {
    this.locales = locales;
  }

  String getDocumentHost() {
    final URL docUrl = this.documentURL;
    return docUrl == null ? null : docUrl.getHost();
  }

  @Override
  public URL getDocumentURL() {
    // TODO: Security considerations?
    return this.documentURL;
  }

  /**
   * Caller should synchronize on document.
   */
  void setElementById(final String id, final Element element) {
    synchronized (this) {
      // TODO: Need to take care of document order. The following check is crude and only takes
      //       care of document order for elements in static HTML.
      if (!elementsById.containsKey(id)) {
        this.elementsById.put(id, element);
      }
    }
  }

  void removeElementById(final String id) {
    synchronized (this) {
      this.elementsById.remove(id);
    }
  }

  private volatile String baseURI;

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.NodeImpl#getbaseURI()
   */
  @Override
  public String getBaseURI() {
    final String buri = this.baseURI;
    return buri == null ? this.documentURI : buri;
  }

  public void setBaseURI(final String value) {
    if (value != null) {
      try {
        @SuppressWarnings("unused")
        final URL ignore = new URL(value);

        // this is a full url if it parses
        this.baseURI = value;
      } catch (final MalformedURLException mfe) {
        try {
          Urls.createURL(documentURL, value);
        } catch (final MalformedURLException mfe2) {
          throw new IllegalArgumentException(mfe2);
        }
      }
    } else {
      this.baseURI = null;
    }
  }

  private String defaultTarget;

  public String getDefaultTarget() {
    return this.defaultTarget;
  }

  public void setDefaultTarget(final String value) {
    this.defaultTarget = value;
  }

  public AbstractView getDefaultView() {
    return this.window;
  }

  public Window getWindow() {
    return this.window;
  }

  @Override
  public String getTextContent() throws DOMException {
    return null;
  }

  @Override
  public void setTextContent(final String textContent) throws DOMException {
    // NOP, per spec
  }

  private String title;

  public String getTitle() {
    return this.title;
  }

  public void setTitle(final String title) {
    this.title = title;
  }

  private String referrer;

  public String getReferrer() {
    return this.referrer;
  }

  public void setReferrer(final String value) {
    this.referrer = value;
  }

  private String domain;

  public String getDomain() {
    return this.domain;
  }

  public void setDomain(final String domain) {
    final String oldDomain = this.domain;
    if ((oldDomain != null) && DomainValidation.isValidCookieDomain(domain, oldDomain)) {
      this.domain = domain;
    } else {
      throw new SecurityException("Cannot set domain to '" + domain + "' when current domain is '" + oldDomain + "'");
    }
  }

  public HTMLElement getBody() {
    synchronized (this) {
      return this.body;
    }
  }

  private HTMLCollection images;
  private HTMLCollection applets;
  private HTMLCollection links;
  private HTMLCollection forms;
  private HTMLCollection anchors;
  private HTMLCollection frames;

  public HTMLCollection getImages() {
    synchronized (this) {
      if (this.images == null) {
        this.images = new DescendentHTMLCollection(this, new ImageFilter(), this.treeLock);
      }
      return this.images;
    }
  }

  public HTMLCollection getApplets() {
    synchronized (this) {
      if (this.applets == null) {
        // TODO: Should include OBJECTs that are applets?
        this.applets = new DescendentHTMLCollection(this, new AppletFilter(), this.treeLock);
      }
      return this.applets;
    }
  }

  public HTMLCollection getLinks() {
    synchronized (this) {
      if (this.links == null) {
        this.links = new DescendentHTMLCollection(this, new LinkFilter(), this.treeLock);
      }
      return this.links;
    }
  }

  public HTMLCollection getForms() {
    synchronized (this) {
      if (this.forms == null) {
        this.forms = new DescendentHTMLCollection(this, new FormFilter(), this.treeLock);
      }
      return this.forms;
    }
  }

  public HTMLCollection getFrames() {
    synchronized (this) {
      if (this.frames == null) {
        this.frames = new DescendentHTMLCollection(this, new FrameFilter(), this.treeLock);
      }
      return this.frames;
    }
  }

  public HTMLCollection getAnchors() {
    synchronized (this) {
      if (this.anchors == null) {
        this.anchors = new DescendentHTMLCollection(this, new AnchorFilter(), this.treeLock);
      }
      return this.anchors;
    }
  }

  public String getCookie() {
    // Justification: A caller (e.g. Google Analytics script)
    // might want to get cookies from the parent document.
    // If the caller has access to the document, it appears
    // they should be able to get cookies on that document.
    // Note that this Document instance cannot be created
    // with an arbitrary URL.

    // TODO: Security: Review rationale.

    return SecurityUtil.doPrivileged(() -> ucontext.getCookie(documentURL));
  }

  public void setCookie(final String cookie) throws DOMException {
    // Justification: A caller (e.g. Google Analytics script)
    // might want to set cookies on the parent document.
    // If the caller has access to the document, it appears
    // they should be able to set cookies on that document.
    // Note that this Document instance cannot be created
    // with an arbitrary URL.
    SecurityUtil.doPrivileged(() -> {
      ucontext.setCookie(documentURL, cookie);
      return null;
    });
  }

  public void open() {
    synchronized (this.treeLock) {
      if (this.reader != null) {
        if (this.reader instanceof LocalWritableLineReader) {
          try {
            this.reader.close();
          } catch (final IOException ioe) {
            // ignore
          }
          this.reader = null;
        } else {
          // Already open, return.
          // Do not close http/file documents in progress.
          return;
        }
      }
      this.removeAllChildrenImpl();
      this.reader = new LocalWritableLineReader(new EmptyReader());
    }
  }

  /**
   * Loads the document from the reader provided when the current instance of
   * <code>HTMLDocumentImpl</code> was constructed. It then closes the reader.
   *
   * @throws IOException
   * @throws SAXException
   * @throws UnsupportedEncodingException
   */
  public void load() throws IOException, SAXException, UnsupportedEncodingException {
    this.load(true);
  }

  public void load(final boolean closeReader) throws IOException, SAXException, UnsupportedEncodingException {
    WritableLineReader reader;
    synchronized (this.treeLock) {
      this.removeAllChildrenImpl();
      this.setTitle(null);
      this.setBaseURI(null);
      this.setDefaultTarget(null);
      this.styleSheetManager.invalidateStyles();
      reader = this.reader;
    }
    if (reader != null) {
      try {
        final ErrorHandler errorHandler = new LocalErrorHandler();
        final String systemId = this.documentURI;
        final String publicId = systemId;
        final HtmlParser parser = new HtmlParser(this.ucontext, this, errorHandler, publicId, systemId, isXML(), true);
        parser.parse(reader);
      } finally {
        if (closeReader) {
          try {
            reader.close();
          } catch (final Exception err) {
            logger.log(Level.WARNING, "load(): Unable to close stream", err);
          }
          synchronized (this.treeLock) {
            this.reader = null;
          }
        }
      }
    }
  }

  @HideFromJS
  public boolean isXML() {
    return isDocTypeXHTML || "application/xhtml+xml".equals(contentType);
  }

  public void close() {
    synchronized (this.treeLock) {
      if (this.reader instanceof LocalWritableLineReader) {
        try {
          this.reader.close();
        } catch (final java.io.IOException ioe) {
          // ignore
        }
        this.reader = null;
      } else {
        // do nothing - could be parsing document off the web.
      }
      // TODO: cause it to render
    }
  }

  public void write(final String text) {
    synchronized (this.treeLock) {
      if (this.reader != null) {
        try {
          // This can end up in openBufferChanged
          this.reader.write(text);
        } catch (final IOException ioe) {
          // ignore
        }
      }
    }
  }

  public void writeln(final String text) {
    synchronized (this.treeLock) {
      if (this.reader != null) {
        try {
          // This can end up in openBufferChanged
          this.reader.write(text + "\r\n");
        } catch (final IOException ioe) {
          // ignore
        }
      }
    }
  }

  private void openBufferChanged(final String text) {
    // Assumed to execute in a lock
    // Assumed that text is not broken up HTML.
    final ErrorHandler errorHandler = new LocalErrorHandler();
    final String systemId = this.documentURI;
    final String publicId = systemId;
    final HtmlParser parser = new HtmlParser(this.ucontext, this, errorHandler, publicId, systemId, false /* TODO */, true);
    final StringReader strReader = new StringReader(text);
    try {
      // This sets up another Javascript scope Window. Does it matter?
      parser.parse(strReader);
    } catch (final Exception err) {
      this.warn("Unable to parse written HTML text. BaseURI=[" + this.getBaseURI() + "].", err);
    }
  }

  /**
   * Gets the collection of elements whose <code>name</code> attribute is
   * <code>elementName</code>.
   */
  public NodeList getElementsByName(final String elementName) {
    return this.getNodeList(new ElementNameFilter(elementName));
  }

  private DocumentType doctype;

  private boolean isDocTypeXHTML = false;

  public DocumentType getDoctype() {
    return this.doctype;
  }

  private final static String XHTML_STRICT_PUBLIC_ID = "-//W3C//DTD XHTML 1.0 Strict//EN";
  private final static String XHTML_STRICT_SYS_ID = "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd";

  public void setDoctype(final DocumentType doctype) {
    this.doctype = doctype;
    isDocTypeXHTML = (doctype != null) && (doctype.getName().equals("html"))
        && (doctype.getPublicId().equals(XHTML_STRICT_PUBLIC_ID)) && (doctype.getSystemId().equals(XHTML_STRICT_SYS_ID));

  }

  public Element getDocumentElement() {
    synchronized (this.treeLock) {
      final ArrayList<Node> nl = this.nodeList;
      if (nl != null) {
        final Iterator<Node> i = nl.iterator();
        while (i.hasNext()) {
          final Object node = i.next();
          if (node instanceof Element) {
            return (Element) node;
          }
        }
      }
      return null;
    }
  }

  public Element createElement(final String tagName) throws DOMException {
    return this.factory.createElement(this, tagName);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.w3c.dom.Document#createDocumentFragment()
   */
  public DocumentFragment createDocumentFragment() {
    // TODO: According to documentation, when a document
    // fragment is added to a node, its children are added,
    // not itself.
    final DocumentFragmentImpl node = new DocumentFragmentImpl();
    node.setOwnerDocument(this);
    return node;
  }

  public Text createTextNode(final String data) {
    final TextImpl node = new TextImpl(data);
    node.setOwnerDocument(this);
    return node;
  }

  public Comment createComment(final String data) {
    final CommentImpl node = new CommentImpl(data);
    node.setOwnerDocument(this);
    return node;
  }

  public CDATASection createCDATASection(final String data) throws DOMException {
    final CDataSectionImpl node = new CDataSectionImpl(data);
    node.setOwnerDocument(this);
    return node;
  }

  public ProcessingInstruction createProcessingInstruction(final String target, final String data) throws DOMException {
    final HTMLProcessingInstruction node = new HTMLProcessingInstruction(target, data);
    node.setOwnerDocument(this);
    return node;
  }

  public Attr createAttribute(final String name) throws DOMException {
    return new AttrImpl(name);
  }

  public EntityReference createEntityReference(final String name) throws DOMException {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "HTML document");
  }

  /**
   * Gets all elements that match the given tag name.
   *
   * @param tagname
   *          The element tag name or an asterisk character (*) to match all
   *          elements.
   */
  public NodeList getElementsByTagName(final String tagname) {
    if ("*".equals(tagname)) {
      return this.getNodeList(new ElementFilter());
    } else {
      return this.getNodeList(new TagNameFilter(tagname));
    }
  }

  public Node importNode(final Node importedNode, final boolean deep) throws DOMException {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not implemented");
  }

  public Element createElementNS(final String namespaceURI, final String qualifiedName) throws DOMException {
    if (namespaceURI == null || (namespaceURI.trim().length() == 0) || "http://www.w3.org/1999/xhtml".equalsIgnoreCase(namespaceURI)) {
      return createElement(qualifiedName);
    } else if ("http://www.w3.org/2000/svg".equalsIgnoreCase(namespaceURI)) {
      // TODO: This is a plug
      return createElement(qualifiedName);
    }
    System.out.println("unhandled request to create element in NS: " + namespaceURI + " with tag: " + qualifiedName);
    return null;
    // TODO
    // throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not implemented: createElementNS");
  }

  public Attr createAttributeNS(final String namespaceURI, final String qualifiedName) throws DOMException {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not implemented: createAttributeNS");
  }

  public NodeList getElementsByTagNameNS(final String namespaceURI, final String localName) {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not implemented: getElementsByTagNameNS");
  }

  public Element getElementById(final String elementId) {
    if ((elementId != null) && (elementId.length() > 0)) {
      synchronized (this) {
        return this.elementsById.get(elementId);
      }
    } else {
      return null;
    }
  }

  private final Map<String, Element> elementsByName = new HashMap<>(0);

  public Element namedItem(final String name) {
    Element element;
    synchronized (this) {
      element = this.elementsByName.get(name);
    }
    return element;
  }

  void setNamedItem(final String name, final Element element) {
    synchronized (this) {
      this.elementsByName.put(name, element);
    }
  }

  void removeNamedItem(final String name) {
    synchronized (this) {
      this.elementsByName.remove(name);
    }
  }

  private String inputEncoding;

  public String getInputEncoding() {
    return this.inputEncoding;
  }

  private String xmlEncoding;

  public String getXmlEncoding() {
    return this.xmlEncoding;
  }

  private boolean xmlStandalone;

  public boolean getXmlStandalone() {
    return this.xmlStandalone;
  }

  public void setXmlStandalone(final boolean xmlStandalone) throws DOMException {
    this.xmlStandalone = xmlStandalone;
  }

  private String xmlVersion = null;

  public String getXmlVersion() {
    return this.xmlVersion;
  }

  public void setXmlVersion(final String xmlVersion) throws DOMException {
    this.xmlVersion = xmlVersion;
  }

  private boolean strictErrorChecking = true;

  public boolean getStrictErrorChecking() {
    return this.strictErrorChecking;
  }

  public void setStrictErrorChecking(final boolean strictErrorChecking) {
    this.strictErrorChecking = strictErrorChecking;
  }

  public String getDocumentURI() {
    return this.documentURI;
  }

  public void setDocumentURI(final String documentURI) {
    // TODO: Security considerations? Chaging documentURL?
    this.documentURI = documentURI;
  }

  public Node adoptNode(final Node source) throws DOMException {
    if (source instanceof NodeImpl) {
      final NodeImpl node = (NodeImpl) source;
      node.setOwnerDocument(this, true);
      return node;
    } else {
      throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Invalid Node implementation");
    }
  }

  private DOMConfiguration domConfig;

  public DOMConfiguration getDomConfig() {
    synchronized (this) {
      if (this.domConfig == null) {
        this.domConfig = new DOMConfigurationImpl();
      }
      return this.domConfig;
    }
  }

  public void normalizeDocument() {
    // TODO: Normalization options from domConfig
    synchronized (this.treeLock) {
      this.visitImpl(new NodeVisitor() {
        public void visit(final Node node) {
          node.normalize();
        }
      });
    }
  }

  public Node renameNode(final Node n, final String namespaceURI, final String qualifiedName) throws DOMException {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "No renaming");
  }

  private DOMImplementation domImplementation;

  /*
   * (non-Javadoc)
   *
   * @see org.w3c.dom.Document#getImplementation()
   */
  public DOMImplementation getImplementation() {
    synchronized (this) {
      if (this.domImplementation == null) {
        this.domImplementation = new DOMImplementationImpl(this.ucontext);
      }
      return this.domImplementation;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.NodeImpl#getLocalName()
   */
  @Override
  public String getLocalName() {
    // Always null for document
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.NodeImpl#getNodeName()
   */
  @Override
  public String getNodeName() {
    return "#document";
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.NodeImpl#getNodeType()
   */
  @Override
  public short getNodeType() {
    return Node.DOCUMENT_NODE;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.NodeImpl#getNodeValue()
   */
  @Override
  public String getNodeValue() throws DOMException {
    // Always null for document
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.NodeImpl#setNodeValue(java.lang.String)
   */
  @Override
  public void setNodeValue(final String nodeValue) throws DOMException {
    throw new DOMException(DOMException.INVALID_MODIFICATION_ERR, "Cannot set node value of document");
  }

  @Override
  public final HtmlRendererContext getHtmlRendererContext() {
    return this.rcontext;
  }

  @Override
  public UserAgentContext getUserAgentContext() {
    return this.ucontext;
  }

  @Override
  public final @NonNull URL getFullURL(final String uri) throws MalformedURLException {
    try {
      final String baseURI = this.getBaseURI();
      final URL documentURL = baseURI == null ? null : new URL(baseURI);
      return Urls.createURL(documentURL, uri);
    } catch (final MalformedURLException mfu) {
      return new URL(uri);
    }
  }

  public final Location getLocation() {
    return this.window.getLocation();
  }

  public void setLocation(final String location) {
    this.getLocation().setHref(location);
  }

  public String getURL() {
    return this.documentURI;
  }

  private HTMLElement body;

  public void setBody(final HTMLElement body) {
    synchronized (this) {
      this.body = body;
    }
  }

  public void allInvalidated(final boolean forgetRenderStates) {
    if (forgetRenderStates) {
      synchronized (this.treeLock) {
        // Need to invalidate all children up to
        // this point.
        this.forgetRenderState();
        // TODO: this might be ineffcient.
        final ArrayList<Node> nl = this.nodeList;
        if (nl != null) {
          final Iterator<Node> i = nl.iterator();
          while (i.hasNext()) {
            final Object node = i.next();
            if (node instanceof HTMLElementImpl) {
              ((HTMLElementImpl) node).forgetStyle(true);
            }
          }
        }
      }
    }
    this.allInvalidated();
  }

  public StyleSheetList getStyleSheets() {
    return styleSheetManager.constructStyleSheetList();
  }

  private final ArrayList<DocumentNotificationListener> documentNotificationListeners = new ArrayList<>(1);

  /**
   * Adds a document notification listener, which is informed about changes to
   * the document.
   *
   * @param listener
   *          An instance of {@link DocumentNotificationListener}.
   */
  public void addDocumentNotificationListener(final DocumentNotificationListener listener) {
    final ArrayList<DocumentNotificationListener> listenersList = this.documentNotificationListeners;
    synchronized (listenersList) {
      listenersList.add(listener);
    }
  }

  public void removeDocumentNotificationListener(final DocumentNotificationListener listener) {
    final ArrayList<DocumentNotificationListener> listenersList = this.documentNotificationListeners;
    synchronized (listenersList) {
      listenersList.remove(listener);
    }
  }

  public void sizeInvalidated(final NodeImpl node) {
    final ArrayList<DocumentNotificationListener> listenersList = this.documentNotificationListeners;
    int size;
    synchronized (listenersList) {
      size = listenersList.size();
    }
    // Traverse list outside synchronized block.
    // (Shouldn't call listener methods in synchronized block.
    // Deadlock is possible). But assume list could have
    // been changed.
    for (int i = 0; i < size; i++) {
      try {
        final DocumentNotificationListener dnl = listenersList.get(i);
        dnl.sizeInvalidated(node);
      } catch (final IndexOutOfBoundsException iob) {
        // ignore
      }
    }
  }

  /**
   * Called if something such as a color or decoration has changed. This would
   * be something which does not affect the rendered size, and can be
   * revalidated with a simple repaint.
   *
   * @param node
   */
  public void lookInvalidated(final NodeImpl node) {
    final ArrayList<DocumentNotificationListener> listenersList = this.documentNotificationListeners;
    int size;
    synchronized (listenersList) {
      size = listenersList.size();
    }
    // Traverse list outside synchronized block.
    // (Shouldn't call listener methods in synchronized block.
    // Deadlock is possible). But assume list could have
    // been changed.
    for (int i = 0; i < size; i++) {
      try {
        final DocumentNotificationListener dnl = listenersList.get(i);
        dnl.lookInvalidated(node);
      } catch (final IndexOutOfBoundsException iob) {
        // ignore
      }
    }

  }

  /**
   * Changed if the position of the node in a parent has changed.
   *
   * @param node
   */
  public void positionInParentInvalidated(final NodeImpl node) {
    final ArrayList<DocumentNotificationListener> listenersList = this.documentNotificationListeners;
    int size;
    synchronized (listenersList) {
      size = listenersList.size();
    }
    // Traverse list outside synchronized block.
    // (Shouldn't call listener methods in synchronized block.
    // Deadlock is possible). But assume list could have
    // been changed.
    for (int i = 0; i < size; i++) {
      try {
        final DocumentNotificationListener dnl = listenersList.get(i);
        dnl.positionInvalidated(node);
      } catch (final IndexOutOfBoundsException iob) {
        // ignore
      }
    }
  }

  /**
   * This is called when the node has changed, but it is unclear if it's a size
   * change or a look change. An attribute change should trigger this.
   *
   * @param node
   */
  public void invalidated(final NodeImpl node) {
    final ArrayList<DocumentNotificationListener> listenersList = this.documentNotificationListeners;
    int size;
    synchronized (listenersList) {
      size = listenersList.size();
    }
    // Traverse list outside synchronized block.
    // (Shouldn't call listener methods in synchronized block.
    // Deadlock is possible). But assume list could have
    // been changed.
    for (int i = 0; i < size; i++) {
      try {
        final DocumentNotificationListener dnl = listenersList.get(i);
        dnl.invalidated(node);
      } catch (final IndexOutOfBoundsException iob) {
        // ignore
      }
    }
  }

  /**
   * This is called when children of the node might have changed.
   *
   * @param node
   */
  public void structureInvalidated(final NodeImpl node) {
    final ArrayList<DocumentNotificationListener> listenersList = this.documentNotificationListeners;
    int size;
    synchronized (listenersList) {
      size = listenersList.size();
    }
    // Traverse list outside synchronized block.
    // (Shouldn't call listener methods in synchronized block.
    // Deadlock is possible). But assume list could have
    // been changed.
    for (int i = 0; i < size; i++) {
      try {
        final DocumentNotificationListener dnl = listenersList.get(i);
        dnl.structureInvalidated(node);
      } catch (final IndexOutOfBoundsException iob) {
        // ignore
      }
    }
  }

  public void nodeLoaded(final NodeImpl node) {
    final ArrayList<DocumentNotificationListener> listenersList = this.documentNotificationListeners;
    int size;
    synchronized (listenersList) {
      size = listenersList.size();
    }
    // Traverse list outside synchronized block.
    // (Shouldn't call listener methods in synchronized block.
    // Deadlock is possible). But assume list could have
    // been changed.
    for (int i = 0; i < size; i++) {
      try {
        final DocumentNotificationListener dnl = listenersList.get(i);
        dnl.nodeLoaded(node);
      } catch (final IndexOutOfBoundsException iob) {
        // ignore
      }
    }
  }

  public void externalScriptLoading(final NodeImpl node) {
    final ArrayList<DocumentNotificationListener> listenersList = this.documentNotificationListeners;
    int size;
    synchronized (listenersList) {
      size = listenersList.size();
    }
    // Traverse list outside synchronized block.
    // (Shouldn't call listener methods in synchronized block.
    // Deadlock is possible). But assume list could have
    // been changed.
    for (int i = 0; i < size; i++) {
      try {
        final DocumentNotificationListener dnl = listenersList.get(i);
        dnl.externalScriptLoading(node);
      } catch (final IndexOutOfBoundsException iob) {
        // ignore
      }
    }
  }

  /**
   * Informs listeners that the whole document has been invalidated.
   */
  public void allInvalidated() {
    final ArrayList<DocumentNotificationListener> listenersList = this.documentNotificationListeners;
    int size;
    synchronized (listenersList) {
      size = listenersList.size();
    }
    // Traverse list outside synchronized block.
    // (Shouldn't call listener methods in synchronized block.
    // Deadlock is possible). But assume list could have
    // been changed.
    for (int i = 0; i < size; i++) {
      try {
        final DocumentNotificationListener dnl = listenersList.get(i);
        dnl.allInvalidated();
      } catch (final IndexOutOfBoundsException iob) {
        // ignore
      }
    }
  }

  @Override
  protected @NonNull RenderState createRenderState(final RenderState prevRenderState) {
    return new StyleSheetRenderState(this);
  }

  private final Map<String, ImageInfo> imageInfos = new HashMap<>(4);
  private final ImageEvent BLANK_IMAGE_EVENT = new ImageEvent(this, new ImageResponse());

  /**
   * Loads images asynchronously such that they are shared if loaded
   * simultaneously from the same URI. Informs the listener immediately if an
   * image is already known.
   *
   * @param relativeUri
   * @param imageListener
   */
  protected void loadImage(final String relativeUri, final ImageListener imageListener) {
    final HtmlRendererContext rcontext = this.getHtmlRendererContext();
    if ((rcontext == null) || !rcontext.isImageLoadingEnabled()) {
      // Ignore image loading when there's no renderer context.
      // Consider Cobra users who are only using the parser.
      imageListener.imageLoaded(BLANK_IMAGE_EVENT);
      return;
    }
    try {
      final URL url = this.getFullURL(relativeUri);
      final String urlText = url.toExternalForm();
      final Map<String, ImageInfo> map = this.imageInfos;
      ImageEvent event = null;
      synchronized (map) {
        final ImageInfo info = map.get(urlText);
        if (info != null) {
          if (info.loaded) {
            // TODO: This can't really happen because ImageInfo
            // is removed right after image is loaded.
            event = info.imageEvent;
          } else {
            info.addListener(imageListener);
          }
        } else {
          final UserAgentContext uac = rcontext.getUserAgentContext();
          final NetworkRequest httpRequest = uac.createHttpRequest();
          final ImageInfo newInfo = new ImageInfo();
          map.put(urlText, newInfo);
          newInfo.addListener(imageListener);
          httpRequest.addNetworkRequestListener(netEvent -> {
            if (httpRequest.getReadyState() == NetworkRequest.STATE_COMPLETE) {
              final ImageResponse imageResponse = httpRequest.getResponseImage();
              final ImageEvent newEvent = new ImageEvent(HTMLDocumentImpl.this, imageResponse);
              ImageListener[] listeners;
              synchronized (map) {
                newInfo.imageEvent = newEvent;
                newInfo.loaded = true;
                listeners = newInfo.getListeners();
                // Must remove from map in the locked block
                // that got the listeners. Otherwise a new
                // listener might miss the event??
                map.remove(urlText);
              }
              if (listeners != null) {
                final int llength = listeners.length;
                for (int i = 0; i < llength; i++) {
                  // Call holding no locks
                  listeners[i].imageLoaded(newEvent);
                }
              }
            } else if (httpRequest.getReadyState() == NetworkRequest.STATE_ABORTED) {
              ImageListener[] listeners;
              synchronized (map) {
                newInfo.loaded = true;
                listeners = newInfo.getListeners();
                // Must remove from map in the locked block
                // that got the listeners. Otherwise a new
                // listener might miss the event??
                map.remove(urlText);
              }
              if (listeners != null) {
                final int llength = listeners.length;
                for (int i = 0; i < llength; i++) {
                  // Call holding no locks
                  listeners[i].imageAborted();
                }
              }
            }
          });

          SecurityUtil.doPrivileged(() -> {
            try {
              httpRequest.open("GET", url);
              httpRequest.send(null, new Request(url, RequestKind.Image));
            } catch (final java.io.IOException thrown) {
              logger.log(Level.WARNING, "loadImage()", thrown);
            }
            return null;
          });
        }
      }
      if (event != null) {
        // Call holding no locks.
        imageListener.imageLoaded(event);
      }
    } catch (final MalformedURLException mfe) {
      imageListener.imageLoaded(BLANK_IMAGE_EVENT);
      return;
    }
  }

  private Function onloadHandler;
  private final List<Function> onloadHandlers = new ArrayList<>();

  public Function getOnloadHandler() {
    return onloadHandler;
  }

  public void setOnloadHandler(final Function onloadHandler) {
    this.onloadHandler = onloadHandler;
  }

  @Override
  public Object setUserData(final String key, final Object data, final UserDataHandler handler) {
    // if (org.lobobrowser.html.parser.HtmlParser.MODIFYING_KEY.equals(key) && data == Boolean.FALSE) {
    // dispatchLoadEvent();
    // }
    return super.setUserData(key, data, handler);
  }

  private void dispatchLoadEvent() {
    final Function onloadHandler = this.onloadHandler;
    if (onloadHandler != null) {
      // TODO: onload event object?
      throw new UnsupportedOperationException();
      // TODO: Use the event dispatcher
      // Executor.executeFunction(this, onloadHandler, null);
    }

    // final Event loadEvent = new Event("load", getBody()); // TODO: What should be the target for this event?
    // dispatchEventToHandlers(loadEvent, onloadHandlers);

    final Event domContentLoadedEvent = new Event("DOMContentLoaded", getBody()); // TODO: What should be the target for this event?
    dispatchEvent(domContentLoadedEvent);

    window.domContentLoaded(domContentLoadedEvent);
  }

  protected EventTargetManager getEventTargetManager() {
    return window.getEventTargetManager();
  }

  @Override
  protected Node createSimilarNode() {
    return new HTMLDocumentImpl(this.ucontext, this.rcontext, this.reader, this.documentURI, this.contentType);
  }

  private static class ImageInfo {
    // Access to this class is synchronized on imageInfos.
    public ImageEvent imageEvent;
    public boolean loaded;
    private final ArrayList<ImageListener> listeners = new ArrayList<>(1);

    void addListener(final ImageListener listener) {
      this.listeners.add(listener);
    }

    ImageListener[] getListeners() {
      return this.listeners.toArray(ImageListener.EMPTY_ARRAY);
    }
  }

  /**
   * Tag class that also notifies document when text is written to an open
   * buffer.
   *
   * @author J. H. S.
   */
  private class LocalWritableLineReader extends WritableLineReader {
    /**
     * @param reader
     */
    public LocalWritableLineReader(final LineNumberReader reader) {
      super(reader);
    }

    /**
     * @param reader
     */
    public LocalWritableLineReader(final Reader reader) {
      super(reader);
    }

    @Override
    public void write(final String text) throws IOException {
      super.write(text);
      if ("".equals(text)) {
        openBufferChanged(text);
      }
    }
  }

  @HideFromJS
  public void addLoadHandler(final Function handler) {
    onloadHandlers.add(handler);
  }

  @HideFromJS
  public void removeLoadHandler(final Function handler) {
    onloadHandlers.remove(handler);
  }

  private List<Runnable> jobs = new LinkedList<>();
  private final AtomicInteger registeredJobs = new AtomicInteger(0);
  private final AtomicInteger layoutBlockingJobs = new AtomicInteger(0);
  private final Semaphore doneAllJobs = new Semaphore(0);
  private final AtomicBoolean stopRequested = new AtomicBoolean(false);
  private int oldPendingTaskId = -1;

  @HideFromJS
  public void stopEverything() {
    if (stopRequested.get()) {
      throw new IllegalStateException("Stop requested twice!");
    }
    stopRequested.set(true);
    if (modificationsStarted.get()) {
      boolean done = false;
      while (!done) {
        try {
          doneAllJobs.acquire();
          done = true;
        } catch (final InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  @HideFromJS
  public void addJob(final Runnable job, final boolean layoutBlocker) {
    addJob(job, layoutBlocker, 1);
  }

  @HideFromJS
  public void addJob(final Runnable job, final boolean layoutBlocker, final int incr) {
    synchronized (jobs) {
      registeredJobs.addAndGet(incr);
      if (layoutBlocker) {
        layoutBlockingJobs.addAndGet(incr);
      }

      jobs.add(job);

      // Added into synch block because of the JS Uniq task change. (old Id should be protected from parallel mod)
      if (modificationsOver.get()) {
        // TODO: temp hack. Not sure if spawning an entirely new thread is right. But it helps with a deadlock in
        // test_script_iframe_load (test number 3)
        // new Thread() {
        // public void run() {
        // runAllPending();
        // };
        // }.start();

        // TODO: temp hack 2. This seems more legitimate than hack #1.
        /*
        window.addJSTask(new JSRunnableTask(0, "todo: quick check to run all pending jobs", () -> {
            runAllPending();
        }));
        */

        // TODO: temp hack 3. This seems more legitimate than hack #1 and optimisation over #2.
        oldPendingTaskId = window.addJSUniqueTask(oldPendingTaskId, new JSRunnableTask(0, "todo: quick check to run all pending jobs",
            () -> {
              runAllPending();
            }));
        // runAllPending();
      }
    }
  }

  private void runAllPending() {
    boolean done = false;
    while (!done && !stopRequested.get()) {
      List<Runnable> jobsCopy;
      synchronized (jobs) {
        jobsCopy = jobs;
        jobs = new LinkedList<>();
      }
      jobsCopy.forEach(j -> j.run());
      synchronized (jobs) {
        done = jobs.size() == 0;
      }
    }
    doneAllJobs.release();
  }

  private Holder classifiedRules = null;
  private static final StyleSheet recommendedStyle = parseStyle(CSSNorm.stdStyleSheet(), StyleSheet.Origin.AGENT, false);
  private static final StyleSheet userAgentStyle = parseStyle(CSSNorm.userStyleSheet(), StyleSheet.Origin.AGENT, false);
  private static final StyleSheet recommendedStyleXML = parseStyle(CSSNorm.stdStyleSheet(), StyleSheet.Origin.AGENT, true);
  private static final StyleSheet userAgentStyleXML = parseStyle(CSSNorm.userStyleSheet(), StyleSheet.Origin.AGENT, true);
  private void updateStyleRules() {
    synchronized (treeLock) {
      if (classifiedRules == null) {
        final List<StyleSheet> jSheets = new ArrayList<>();
        jSheets.add(isXML() ? recommendedStyleXML : recommendedStyle);
        jSheets.add(isXML() ? userAgentStyleXML : userAgentStyle);
        jSheets.addAll(styleSheetManager.getEnabledJStyleSheets());
        classifiedRules = AnalyzerUtil.getClassifiedRules(jSheets, new MediaSpec("screen"));
      }
    }
  }

  ElementMatcher getMatcher() {
    return isXML() ? xhtmlMatcher : stdMatcher;
  }

  /**
   * Visits all elements and computes their styles. This is faster than
   * computing them separately when needed. Note: If styles were to be stored as
   * soft / weak references, this method will lose its value.
   */
  @HideFromJS
  public void primeNodeData() {
    visit((node) -> {
      if (node instanceof HTMLElementImpl) {
        HTMLElementImpl he = (HTMLElementImpl) node;
        he.getCurrentStyle();
      }
    });
  }

  Holder getClassifiedRules() {
    synchronized (treeLock) {
      if (classifiedRules == null) {
        updateStyleRules();
      }
      return classifiedRules;
    }
  }

  final static ElementMatcher xhtmlMatcher = new ElementMatcherSafeCS();
  final static ElementMatcher stdMatcher = new ElementMatcherSafeStd();

  private static StyleSheet parseStyle(final String cssdata, final StyleSheet.Origin origin, final boolean isXML) {
    try {
      final StyleSheet newsheet = CSSParserFactory.getInstance().parse(cssdata, null, null, SourceType.EMBEDDED, null);
      newsheet.setOrigin(origin);
      return newsheet;
    } catch (IOException | CSSException e) {
      throw new RuntimeException(e);
    }
  }


  // TODO: Synchronize?
  @HideFromJS
  public void markJobsFinished(final int numJobs, final boolean layoutBlocker) {
    final int curr = registeredJobs.addAndGet(-numJobs);
    final int layoutBlockers = layoutBlocker ? layoutBlockingJobs.addAndGet(-numJobs) : layoutBlockingJobs.get();
    if (layoutBlocked.get()) {
      if (layoutBlockers == 0) {
        layoutBlocked.set(false);
        allInvalidated();
      }
    }
    if (curr < 0) {
      throw new IllegalStateException("More jobs over than registered!");
    } else if (curr == 0) {
      if (!stopRequested.get() && !loadOver.get()) {
        loadOver.set(true);
        dispatchLoadEvent();
        // System.out.println("In " + baseURI);
        // System.out.println("  calling window.jobsFinished()");
        rcontext.jobsFinished();
        window.jobsFinished();
      }
    }
  }

  private final AtomicBoolean modificationsStarted = new AtomicBoolean(false);
  private final AtomicBoolean modificationsOver = new AtomicBoolean(false);
  private final AtomicBoolean loadOver = new AtomicBoolean(false);
  public final AtomicBoolean layoutBlocked = new AtomicBoolean(true);

  @HideFromJS
  public void finishModifications() {
    StyleElements.normalizeHTMLTree(this);
    // TODO: Not sure if this should be run in new thread. But this blocks the UI sometimes when it is in the same thread, and a network request hangs.
    //       There is a race condition here, when iframes are involved.
    //       The thread creation can probably be removed as part of GH #140
    new Thread(() -> {
      modificationsStarted.set(true);
      runAllPending();
      modificationsOver.set(true);
    }).start();

    // This is to trigger a check in the no external resource case.
    // On second thoughts, this may not be required. The window load event need only be fired if there is a script
    // On third thoughs, this also affects frame that embed iframes
    markJobsFinished(0, false);

    /* Nodes.forEachNode(document, node -> {
      if (node instanceof NodeImpl) {
        final NodeImpl element = (NodeImpl) node;
        Object oldData = element.getUserData(org.lobobrowser.html.parser.HtmlParser.MODIFYING_KEY);
        if (oldData == null || !oldData.equals(Boolean.FALSE)) {
          element.setUserData(org.lobobrowser.html.parser.HtmlParser.MODIFYING_KEY, Boolean.FALSE, null);
        }
      }
    });*/
  }

  final class StyleSheetManager {

    private volatile List<JStyleSheetWrapper> styleSheets = null;

    final StyleSheetBridge bridge = new StyleSheetBridge() {

      public void notifyStyleSheetChanged(final CSSStyleSheet styleSheet) {
        final Node ownerNode = styleSheet.getOwnerNode();
        if (ownerNode != null) {
          final boolean disabled = styleSheet.getDisabled();
          if (ownerNode instanceof HTMLStyleElementImpl) {
            final HTMLStyleElementImpl htmlStyleElement = (HTMLStyleElementImpl) ownerNode;
            if (htmlStyleElement.getDisabled() != disabled) {
              htmlStyleElement.setDisabledImpl(disabled);
            }
          } else if (ownerNode instanceof HTMLLinkElementImpl) {
            final HTMLLinkElementImpl htmlLinkElement = (HTMLLinkElementImpl) ownerNode;
            if (htmlLinkElement.getDisabled() != disabled) {
              htmlLinkElement.setDisabledImpl(disabled);
            }
          }
        }
        invalidateStyles();
        allInvalidated();
      }

      public List<JStyleSheetWrapper> getDocStyleSheets() {
        return getDocStyleSheetList();
      }

    };

    private List<JStyleSheetWrapper> getDocStyleSheetList() {
      synchronized (this) {
        if (styleSheets == null) {
          styleSheets = new ArrayList<>();
          final List<JStyleSheetWrapper> docStyles = new ArrayList<>();
          synchronized (treeLock) {
            scanElementStyleSheets(docStyles, HTMLDocumentImpl.this);
          }
          styleSheets.addAll(docStyles);
          // System.out.println("Found stylesheets: " + this.styleSheets.size());
        }
        return this.styleSheets;
      }
    }

    private void scanElementStyleSheets(final List<JStyleSheetWrapper> styles, final Node node) {
      if (node instanceof LinkStyle) {
        final LinkStyle linkStyle = (LinkStyle) node;
        final JStyleSheetWrapper sheet = (JStyleSheetWrapper) linkStyle.getSheet();
        if (sheet != null) {
          styles.add(sheet);
        }
      }

      if (node.hasChildNodes()) {
        final NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
          scanElementStyleSheets(styles, nodeList.item(i));
        }
      }
    }

    private volatile List<cz.vutbr.web.css.StyleSheet> enabledJStyleSheets = null;

    // TODO enabled style sheets can be cached
    List<cz.vutbr.web.css.StyleSheet> getEnabledJStyleSheets() {
      synchronized (this) {
        if (enabledJStyleSheets != null) {
          return enabledJStyleSheets;
        }
        final List<JStyleSheetWrapper> documentStyles = this.getDocStyleSheetList();
        final List<cz.vutbr.web.css.StyleSheet> jStyleSheets = new ArrayList<>();
        for (final JStyleSheetWrapper style : documentStyles) {
          if ((!style.getDisabled()) && (style.getJStyleSheet() != null)) {
            jStyleSheets.add(style.getJStyleSheet());
          }
        }
        enabledJStyleSheets = jStyleSheets;
        return jStyleSheets;
      }
    }

    void invalidateStyles() {
      synchronized (treeLock) {
        this.styleSheets = null;
        getDocStyleSheetList();
      }
      synchronized (this) {
        this.enabledJStyleSheets = null;
      }
      synchronized (treeLock) {
        HTMLDocumentImpl.this.classifiedRules = null;
      }
      // System.out.println("Stylesheets set to null");
      allInvalidated(true);
    }

    StyleSheetList constructStyleSheetList() {
      return JStyleSheetWrapper.getStyleSheets(bridge);
    }

  }

  @Override
  public void addEventListener(final String type, final EventListener listener, final boolean useCapture) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeEventListener(final String type, final EventListener listener, final boolean useCapture) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean dispatchEvent(final org.w3c.dom.events.Event evt) throws EventException {
    // TODO Auto-generated method stub
    return false;
  }

  public Event createEvent(final String type ) {
    return new Event(type, this);
  }

  public Range createRange() {
    return new RangeImpl(this);
  }

  public boolean hasFocus() {
    // TODO: Plug
    return true;
  }
}
