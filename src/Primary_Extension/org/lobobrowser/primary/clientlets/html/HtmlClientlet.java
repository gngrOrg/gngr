/*
    GNU GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The Lobo Project

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public
    License as published by the Free Software Foundation; either
    verion 2 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Contact info: lobochief@users.sourceforge.net
 */
/*
 * Created on May 14, 2005
 */
package org.lobobrowser.primary.clientlets.html;

import java.awt.event.ActionEvent;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.clientlet.Clientlet;
import org.lobobrowser.clientlet.ClientletContext;
import org.lobobrowser.clientlet.ClientletException;
import org.lobobrowser.clientlet.ClientletResponse;
import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.domimpl.DocumentNotificationListener;
import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.domimpl.NodeImpl;
import org.lobobrowser.html.gui.HtmlPanel;
import org.lobobrowser.html.parser.DocumentBuilderImpl;
import org.lobobrowser.html.parser.InputSourceImpl;
import org.lobobrowser.ua.NavigatorFrame;
import org.lobobrowser.ua.RequestType;
import org.lobobrowser.util.Strings;
import org.lobobrowser.util.Urls;
import org.lobobrowser.util.io.RecordedInputStream;
import org.w3c.dom.Element;
import org.w3c.dom.html.HTMLElement;

/**
 * @author J. H. S.
 */
public final class HtmlClientlet implements Clientlet {
  private static final Logger logger = Logger.getLogger(HtmlClientlet.class.getName());
  private static final Set<String> NON_VISIBLE_ELEMENTS = new HashSet<>();

  // Maximum buffer size required to determine if a reload due
  // to Http-Equiv is necessary.
  private static final int MAX_IS_BUFFER_SIZE = 1024 * 100;

  public HtmlClientlet() {
    super();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.clientlet.Clientlet#parse(org.xamjwg.dom.XDocument)
   */
  public void process(final ClientletContext cc) throws ClientletException {
    this.processImpl(cc, null, null);
  }

  private void processImpl(final ClientletContext cc, final Map<String, String> httpEquivData, RecordedInputStream rin)
      throws ClientletException {
    // This method may be executed twice, depending on http-equiv meta elements.
    try {
      final ClientletResponse response = cc.getResponse();
      final boolean charsetProvided = response.isCharsetProvided();
      final String contentLanguage = response.getHeader("Content-Language");
      Set<Locale> locales = contentLanguage == null ? null : HtmlClientlet.extractLocales(contentLanguage);
      RefreshInfo refresh = null;
      final Iterator<String> hi = response.getHeaderNames();
      // TODO: What is the behavior if you have
      // a Refresh header and also a Refresh HTTP-EQUIV?
      while (hi.hasNext()) {
        final String headerName = hi.next();
        final String[] headerValues = response.getHeaders(headerName);
        if ((headerValues != null) && (headerValues.length > 0)) {
          if ("refresh".equalsIgnoreCase(headerName)) {
            refresh = HtmlClientlet.extractRefresh(headerValues[headerValues.length - 1]);
          }
        }
      }
      String httpEquivCharset = null;
      if (httpEquivData != null) {
        final Iterator<Map.Entry<String, String>> i = httpEquivData.entrySet().iterator();
        while (i.hasNext()) {
          final Map.Entry<String, String> entry = i.next();
          final String httpEquiv = entry.getKey();
          final String content = entry.getValue();
          if (content != null) {
            if ("content-type".equalsIgnoreCase(httpEquiv)) {
              httpEquivCharset = HtmlClientlet.extractCharset(response.getResponseURL(), content);
            } else if ("refresh".equalsIgnoreCase(httpEquiv)) {
              refresh = HtmlClientlet.extractRefresh(content);
            } else if ("content-language".equalsIgnoreCase(httpEquiv)) {
              locales = HtmlClientlet.extractLocales(content);
            }
          }
        }
      }
      final HtmlRendererContextImpl rcontext = HtmlRendererContextImpl.getHtmlRendererContext(cc.getNavigatorFrame());
      final DocumentBuilderImpl builder = new DocumentBuilderImpl(rcontext.getUserAgentContext(), rcontext);
      if (rin == null) {
        final InputStream in = response.getInputStream();
        rin = in instanceof RecordedInputStream ? (RecordedInputStream) in : new RecordedInputStream(in, MAX_IS_BUFFER_SIZE);
        rin.mark(Short.MAX_VALUE);
      } else {
        rin.reset();
      }
      final URL responseURL = response.getResponseURL();
      final String uri = responseURL.toExternalForm();
      String charset;
      if (!charsetProvided) {
        charset = httpEquivCharset;
      } else {
        // See bug # 2051468. A charset provided
        // in headers takes precendence.
        charset = response.getCharset();
      }
      if (charset == null) {
        // charset = "ISO-8859-1";
        charset = "utf-8";
      }
      if (logger.isLoggable(Level.INFO)) {
        logger.info("process(): charset=" + charset + " for URI=[" + uri + "]");
      }
      final InputSourceImpl is = new InputSourceImpl(rin, uri, charset);
      final HTMLDocumentImpl document = (HTMLDocumentImpl) builder.createDocument(is, response.getContentType());
      document.setLocales(locales);
      final String referrer = cc.getRequest().getReferrer();
      document.setReferrer(referrer == null ? "" : referrer);
      final HtmlPanel panel = rcontext.getHtmlPanel();
      // Create a listener that will switch to rendering when appropriate.
      final HtmlContent content = new HtmlContent(document, panel, rin, charset);
      final LocalDocumentNotificationListener listener = new LocalDocumentNotificationListener(document, panel, rcontext, cc, content,
          httpEquivData == null);
      document.addDocumentNotificationListener(listener);
      // Set resulting content before parsing
      // to enable incremental rendering.
      // The load() call starts parsing.
      try {
        document.load(false);
      } catch (final HttpEquivRetryException retry) {
        if (logger.isLoggable(Level.INFO)) {
          logger.info("processImpl(): Resetting due to META http-equiv: " + uri);
        }
        // This is a recursive call, but it doesn't go further
        // than one level deep.
        this.processImpl(cc, retry.getHttpEquivData(), rin);
        return;
      }
      // We're done parsing, but let's make sure
      // the listener actually renderered the document.
      listener.ensureSwitchedToRendering();

      // Scroll to see anchor.
      /* This is now disabled, because we now layout lazily. There is no point in scrolling until the layout is done.
      final String ref = responseURL.getRef();
      if ((ref != null) && (ref.length() != 0)) {
        panel.scrollToElement(ref);
      }*/

      if (refresh != null) {
        final String destUri = refresh.destinationUrl;
        final java.net.URL currentURL = response.getResponseURL();
        @NonNull URL destURL;
        if (destUri == null) {
          destURL = currentURL;
        } else {
          destURL = Urls.createURL(currentURL, destUri);
        }
        final java.awt.event.ActionListener action = new java.awt.event.ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            final NavigatorFrame frame = cc.getNavigatorFrame();
            if (frame.getComponentContent() == content) {
              // Navigate only if the original document is there.
              // TODO: Address bar shouldn't change if it's being edited.
              // TODO: A nagivation action should cancel this altogether.
              frame.navigate(destURL, RequestType.PROGRAMMATIC);
            }
          }
        };
        int waitMillis = refresh.waitSeconds * 1000;
        if (waitMillis <= 0) {
          waitMillis = 1;
        }
        final javax.swing.Timer timer = new javax.swing.Timer(waitMillis, action);
        timer.setRepeats(false);
        timer.start();
      }
    } catch (final Exception err) {
      throw new ClientletException(err);
    }
  }

  private static String extractCharset(final java.net.URL responseURL, final String contentType) {
    final StringTokenizer tok = new StringTokenizer(contentType, ";");
    if (tok.hasMoreTokens()) {
      tok.nextToken();
      while (tok.hasMoreTokens()) {
        final String assignment = tok.nextToken().trim();
        final int eqIdx = assignment.indexOf('=');
        if (eqIdx != -1) {
          final String varName = assignment.substring(0, eqIdx).trim();
          if ("charset".equalsIgnoreCase(varName)) {
            final String varValue = assignment.substring(eqIdx + 1);
            return Strings.unquote(varValue.trim());
          }
        }
      }
    }
    return null;
  }

  private static Set<Locale> extractLocales(final String contentLanguage) {
    final Set<Locale> locales = new HashSet<>(3);
    final StringTokenizer tok = new StringTokenizer(contentLanguage, ",");
    while (tok.hasMoreTokens()) {
      final String lang = tok.nextToken().trim();
      locales.add(new Locale(lang));
    }
    return locales;
  }

  private final static RefreshInfo extractRefresh(final String refresh) {
    String delayText = null;
    String urlText = null;
    final StringTokenizer tok = new StringTokenizer(refresh, ";");
    if (tok.hasMoreTokens()) {
      delayText = tok.nextToken().trim();
      while (tok.hasMoreTokens()) {
        final String assignment = tok.nextToken().trim();
        final int eqIdx = assignment.indexOf('=');
        if (eqIdx != -1) {
          final String varName = assignment.substring(0, eqIdx).trim();
          if ("url".equalsIgnoreCase(varName)) {
            final String varValue = assignment.substring(eqIdx + 1);
            urlText = Strings.unquote(varValue.trim());
          }
        } else {
          urlText = Strings.unquote(assignment);
        }
      }
    }
    int delay;
    try {
      delay = Integer.parseInt(delayText);
    } catch (final NumberFormatException nfe) {
      logger.warning("extractRefresh(): Bad META refresh delay: " + delayText + ".");
      delay = 0;
    }
    return new RefreshInfo(delay, urlText);
  }

  private static class LocalDocumentNotificationListener implements DocumentNotificationListener {
    private static final int MAX_WAIT = 7000;
    private final HTMLDocumentImpl document;
    private final HtmlPanel htmlPanel;
    private final long startTimestamp;
    private final HtmlRendererContext rcontext;
    private final ClientletContext ccontext;
    private final HtmlContent content;
    private final boolean detectHttpEquiv;
    private boolean hasVisibleElements = false;
    private boolean hasSwitchedToRendering = false;
    private Collection<HTMLElement> httpEquivElements;

    public LocalDocumentNotificationListener(final HTMLDocumentImpl doc, final HtmlPanel panel, final HtmlRendererContext rcontext,
        final ClientletContext cc,
        final HtmlContent content, final boolean detectHttpEquiv) {
      this.document = doc;
      this.startTimestamp = System.currentTimeMillis();
      this.htmlPanel = panel;
      this.rcontext = rcontext;
      this.ccontext = cc;
      this.content = content;
      this.detectHttpEquiv = detectHttpEquiv;
    }

    public void allInvalidated() {
    }

    public void externalScriptLoading(final NodeImpl node) {
      // We can expect this to occur only in the parser thread.
      if (this.hasVisibleElements) {
        this.ensureSwitchedToRendering();
      }
    }

    public void invalidated(final NodeImpl node) {
    }

    public void lookInvalidated(final NodeImpl node) {
    }

    private void addHttpEquivElement(final HTMLElement element) {
      Collection<HTMLElement> httpEquivElements = this.httpEquivElements;
      if (httpEquivElements == null) {
        httpEquivElements = new LinkedList<>();
        this.httpEquivElements = httpEquivElements;
      }
      httpEquivElements.add(element);
    }

    public void nodeLoaded(final NodeImpl node) {
      // We can expect this to occur only in the parser thread.
      if (this.detectHttpEquiv) {
        if (node instanceof HTMLElement) {
          final HTMLElement element = (HTMLElement) node;
          final String tagName = element.getTagName();
          if ("meta".equalsIgnoreCase(tagName)) {
            final String httpEquiv = element.getAttribute("http-equiv");
            if (httpEquiv != null) {
              this.addHttpEquivElement(element);
            }
          }
          if ("head".equalsIgnoreCase(tagName) || "script".equalsIgnoreCase(tagName) || "html".equalsIgnoreCase(tagName)) {
            // Note: SCRIPT is checked as an optimization. We do not want
            // scripts to be processed twice. HTML is checked because
            // sometimes sites don't put http-equiv in HEAD, e.g.
            // http://baidu.com.
            final Map<String, String> httpEquiv = this.getHttpEquivData();
            if ((httpEquiv != null) && (httpEquiv.size() > 0)) {
              throw new HttpEquivRetryException(httpEquiv);
            }
          }
        }
      }
      if (!this.hasVisibleElements) {
        if (LocalDocumentNotificationListener.mayBeVisibleElement(node)) {
          this.hasVisibleElements = true;
        }
      }
      if (this.hasVisibleElements && ((System.currentTimeMillis() - this.startTimestamp) > MAX_WAIT)) {
        this.ensureSwitchedToRendering();
      }
    }

    public void positionInvalidated(final NodeImpl node) {
    }

    public void sizeInvalidated(final NodeImpl node) {
    }

    public void structureInvalidated(final NodeImpl node) {
    }

    private final static boolean mayBeVisibleElement(final NodeImpl node) {
      if (node instanceof HTMLElement) {
        final HTMLElement element = (HTMLElement) node;
        final boolean visible = !NON_VISIBLE_ELEMENTS.contains(element.getTagName().toLowerCase());
        if (visible && logger.isLoggable(Level.INFO)) {
          logger.info("mayBeVisibleElement(): Found possibly visible element: " + element.getTagName());
        }
        return visible;
      } else {
        return false;
      }
    }

    public void ensureSwitchedToRendering() {
      synchronized (this) {
        if (this.hasSwitchedToRendering) {
          return;
        }
        this.hasSwitchedToRendering = true;
      }
      final HTMLDocumentImpl document = this.document;
      document.removeDocumentNotificationListener(this);
      SwingUtilities.invokeLater(() -> {
        // Should have nicer effect (less flicker) in GUI thread.
        htmlPanel.setDocument(document, rcontext);
        ccontext.setResultingContent(content);
      });
    }

    private Map<String, String> getHttpEquivData() {
      final Collection<HTMLElement> httpEquivElements = this.httpEquivElements;
      if (httpEquivElements == null) {
        return null;
      }
      final Map<String, String> httpEquivData = new HashMap<>(0);
      for (final Element element : httpEquivElements) {
        final String httpEquiv = element.getAttribute("http-equiv");
        if (httpEquiv != null) {
          final String content = element.getAttribute("content");
          httpEquivData.put(httpEquiv, content);
        }
      }
      return httpEquivData;
    }
  }

  private static class RefreshInfo {
    public final int waitSeconds;
    public final String destinationUrl;

    public RefreshInfo(final int waitSeconds, final String destinationUrl) {
      super();
      this.waitSeconds = waitSeconds;
      this.destinationUrl = destinationUrl;
    }
  }

  private static class HttpEquivRetryException extends RuntimeException {
    private static final long serialVersionUID = 3943944816518046414L;
    private final Map<String, String> httpEquivData;

    public HttpEquivRetryException(final Map<String, String> httpEquiv) {
      super();
      this.httpEquivData = httpEquiv;
    }

    public Map<String, String> getHttpEquivData() {
      return httpEquivData;
    }
  }
}
