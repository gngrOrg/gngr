/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2026 gngr authors

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.
*/

package org.lobobrowser.harness;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.html.BrowserFrame;
import org.lobobrowser.html.FormInput;
import org.lobobrowser.html.HtmlObject;
import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.domimpl.FrameNode;
import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.gui.HtmlPanel;
import org.lobobrowser.html.parser.DocumentBuilderImpl;
import org.lobobrowser.html.parser.InputSourceImpl;
import org.lobobrowser.ua.UserAgentContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.html.HTMLCollection;
import org.w3c.dom.html.HTMLElement;
import org.w3c.dom.html.HTMLLinkElement;

/**
 * HtmlRendererContext implementation for headless render testing.
 */
public class HarnessHtmlRendererContext implements HtmlRendererContext {
  private static final Logger logger = Logger.getLogger(HarnessHtmlRendererContext.class.getName());
  private static final HTMLCollection EMPTY_FRAMES = new HTMLCollection() {
    @Override
    public int getLength() {
      return 0;
    }

    @Override
    public Node item(final int index) {
      return null;
    }

    @Override
    public Node namedItem(final String name) {
      return null;
    }
  };

  private final HtmlPanel htmlPanel;
  private final UserAgentContext userAgentContext;
  private final HarnessHtmlRendererContext parent;
  private final BrowserFrame ownerFrame;
  private Runnable jobFinishedHandler;
  private volatile String currentURL = "";

  public HarnessHtmlRendererContext(final HtmlPanel htmlPanel, final UserAgentContext userAgentContext) {
    this(htmlPanel, userAgentContext, null, null);
  }

  private HarnessHtmlRendererContext(
      final HtmlPanel htmlPanel,
      final UserAgentContext userAgentContext,
      final HarnessHtmlRendererContext parent,
      final BrowserFrame ownerFrame) {
    this.htmlPanel = htmlPanel;
    this.userAgentContext = userAgentContext;
    this.parent = parent;
    this.ownerFrame = ownerFrame;
  }

  public HtmlPanel getHtmlPanel() {
    return htmlPanel;
  }

  public void setCurrentURL(final String currentURL) {
    this.currentURL = currentURL;
  }

  @Override
  public void navigate(final @NonNull URL url, final String target) {
    if (target != null && !target.isEmpty()) {
      if ("_parent".equalsIgnoreCase(target)) {
        if (parent != null) {
          parent.navigate(url, null);
          return;
        }
      } else if ("_top".equalsIgnoreCase(target)) {
        final HtmlRendererContext top = getTop();
        if (top != this) {
          top.navigate(url, null);
          return;
        }
      } else if (!"_self".equalsIgnoreCase(target) && !"_blank".equalsIgnoreCase(target)) {
        final HTMLCollection frames = getTop().getFrames();
        final Node frameNode = frames != null ? frames.namedItem(target) : null;
        if (frameNode instanceof FrameNode) {
          final BrowserFrame bf = ((FrameNode) frameNode).getBrowserFrame();
          if (bf != null && bf.getHtmlRendererContext() != this) {
            bf.loadURL(url);
            return;
          }
        }
      }
    }
    if ((target == null || target.isEmpty() || "_self".equalsIgnoreCase(target)) && this.ownerFrame != null) {
      this.ownerFrame.loadURL(url);
      return;
    }
    logger.info("navigate() called to: " + url + " target: " + target);
  }

  @Override
  public void linkClicked(final HTMLElement linkNode, final @NonNull URL url, final String target) {
  }

  @Override
  public HTMLCollection getFrames() {
    final Object rootNode = this.htmlPanel.getRootNode();
    if (rootNode instanceof HTMLDocumentImpl) {
      return ((HTMLDocumentImpl) rootNode).getFrames();
    }
    return EMPTY_FRAMES;
  }

  @Override
  public void submitForm(final String method, final @NonNull URL action, final String target, final String enctype, final FormInput[] formInputs) {
  }

  @Override
  public BrowserFrame createBrowserFrame() {
    return new HarnessBrowserFrame();
  }

  final class HarnessBrowserFrame implements BrowserFrame {
    private final HtmlPanel subPanel = RenderHarnessMain.createOffscreenHtmlPanel();
    private final HarnessHtmlRendererContext subContext =
        new HarnessHtmlRendererContext(subPanel, userAgentContext, HarnessHtmlRendererContext.this, this);
    private volatile Document doc;
    private volatile boolean loading = false;
    private volatile boolean loadAttempted = false;

    boolean isLoading() {
      return loading;
    }

    boolean isLoadAttempted() {
      return loadAttempted;
    }

    @Override
    public Component getComponent() {
      return subPanel;
    }

    @Override
    public void loadURL(final @NonNull URL url) {
      loading = true;
      HarnessNetworkRequest.incrementActiveRequests();
      boolean finishCalled = false;
      try {
        if ("data".equalsIgnoreCase(url.getProtocol())) {
          RenderHarnessMain.registerProtocolHandlers();
        }
        final URLConnection conn = url.openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", userAgentContext.getUserAgent());
        if (conn instanceof HttpURLConnection) {
          ((HttpURLConnection) conn).setInstanceFollowRedirects(true);
        }

        final String rawContentType = conn.getContentType();
        final String charset = HarnessServer.resolveCharset(rawContentType);
        final String mimeType = HarnessServer.resolveMimeType(rawContentType, url);

        final HTMLDocumentImpl parsedDoc;
        try (InputStream in = conn.getInputStream()) {
          final InputSourceImpl is = new InputSourceImpl(in, url.toExternalForm(), charset);
          final DocumentBuilderImpl builder = new DocumentBuilderImpl(userAgentContext, subContext);
          parsedDoc = (HTMLDocumentImpl) builder.createDocument(is, mimeType);
          parsedDoc.load(false);
        }

        this.doc = parsedDoc;
        subContext.setCurrentURL(url.toExternalForm());
        runOnEdtSync(() -> subPanel.setDocument(parsedDoc, subContext));
        parsedDoc.finishModifications();
        finishCalled = true;
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        logger.log(Level.WARNING, "Interrupted loading frame URL: " + url, ie);
      } catch (Exception e) {
        logger.log(Level.WARNING, "Error loading frame URL: " + url, e);
      } finally {
        loading = false;
        loadAttempted = true;
        HarnessNetworkRequest.decrementActiveRequests();
        if (!finishCalled) {
          subContext.jobsFinished();
        }
      }
    }

    @Override
    public Document getContentDocument() {
      return doc;
    }

    @Override
    public void setContentDocument(final Document d) {
      this.doc = d;
      this.loadAttempted = true;
      try {
        runOnEdtSync(() -> subPanel.setDocument(d, subContext));
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        logger.log(Level.WARNING, "Interrupted setting frame content document", ie);
      } catch (InvocationTargetException ite) {
        logger.log(Level.WARNING, "Error setting frame content document", ite);
      }
    }

    @Override
    public HtmlRendererContext getHtmlRendererContext() {
      return subContext;
    }

    @Override
    public void setDefaultMarginInsets(final Insets insets) {
    }

    @Override
    public void setDefaultOverflowX(final int overflowX) {
      subPanel.setDefaultOverflowX(overflowX);
    }

    @Override
    public void setDefaultOverflowY(final int overflowY) {
      subPanel.setDefaultOverflowY(overflowY);
    }
  }

  private static void runOnEdtSync(final Runnable runnable) throws InterruptedException, InvocationTargetException {
    if (SwingUtilities.isEventDispatchThread()) {
      runnable.run();
    } else {
      SwingUtilities.invokeAndWait(runnable);
    }
  }

  @Override
  public UserAgentContext getUserAgentContext() {
    return userAgentContext;
  }

  @Override
  public HtmlObject getHtmlObject(final HTMLElement element) {
    return null;
  }

  @Override
  public boolean onMiddleClick(final HTMLElement element, final MouseEvent event) {
    return true;
  }

  @Override
  public boolean onContextMenu(final HTMLElement element, final MouseEvent event) {
    return true;
  }

  @Override
  public boolean onMouseClick(final HTMLElement element, final MouseEvent event) {
    return true;
  }

  @Override
  public boolean onDoubleClick(final HTMLElement element, final MouseEvent event) {
    return true;
  }

  @Override
  public void onMouseOver(final HTMLElement element, final MouseEvent event) {
  }

  @Override
  public void onMouseOut(final HTMLElement element, final MouseEvent event) {
  }

  @Override
  public boolean isImageLoadingEnabled() {
    return true;
  }

  @Override
  public void alert(final String message) {
  }

  @Override
  public void back() {
  }

  @Override
  public void blur() {
  }

  @Override
  public void close() {
  }

  @Override
  public boolean confirm(final String message) {
    return false;
  }

  @Override
  public void focus() {
  }

  @Override
  @Deprecated
  public HtmlRendererContext open(final String absoluteUrl, final String windowName, final String windowFeatures, final boolean replace) {
    return null;
  }

  @Override
  public HtmlRendererContext open(final @NonNull URL url, final String windowName, final String windowFeatures, final boolean replace) {
    return null;
  }

  @Override
  public String prompt(final String message, final String inputDefault) {
    return inputDefault;
  }

  @Override
  public void scroll(final int x, final int y) {
  }

  @Override
  public void scrollBy(final int x, final int y) {
  }

  @Override
  public void resizeTo(final int width, final int height) {
    htmlPanel.setSize(width, height);
    htmlPanel.setBounds(0, 0, width, height);
  }

  @Override
  public void resizeBy(final int byWidth, final int byHeight) {
    final int newW = htmlPanel.getWidth() + byWidth;
    final int newH = htmlPanel.getHeight() + byHeight;
    resizeTo(newW, newH);
  }

  @Override
  public boolean isClosed() {
    return false;
  }

  @Override
  public String getDefaultStatus() {
    return "";
  }

  @Override
  public void setDefaultStatus(final String value) {
  }

  @Override
  public String getName() {
    return "harness";
  }

  @Override
  public HtmlRendererContext getParent() {
    return this.parent;
  }

  @Override
  public HtmlRendererContext getOpener() {
    return null;
  }

  @Override
  public void setOpener(final HtmlRendererContext opener) {
  }

  @Override
  public String getStatus() {
    return "";
  }

  @Override
  public void setStatus(final String message) {
  }

  @Override
  public HtmlRendererContext getTop() {
    return this.parent == null ? this : this.parent.getTop();
  }

  @Override
  public boolean isVisitedLink(final HTMLLinkElement link) {
    return false;
  }

  @Override
  public void reload() {
  }

  @Override
  public int getHistoryLength() {
    return 1;
  }

  @Override
  public String getCurrentURL() {
    return currentURL;
  }

  @Override
  public Optional<String> getNextURL() {
    return Optional.empty();
  }

  @Override
  public Optional<String> getPreviousURL() {
    return Optional.empty();
  }

  @Override
  public void forward() {
  }

  @Override
  public void moveInHistory(final int offset) {
  }

  @Override
  public void goToHistoryURL(final String url) {
  }

  @Override
  public void setCursor(final Optional<Cursor> cursorOpt) {
  }

  @Override
  public void jobsFinished() {
    final Runnable handler;
    synchronized (this) {
      handler = this.jobFinishedHandler;
      this.jobFinishedHandler = null;
    }
    if (handler != null) {
      handler.run();
    }
  }

  @Override
  public synchronized void setJobFinishedHandler(final Runnable runnable) {
    this.jobFinishedHandler = runnable;
  }
}
