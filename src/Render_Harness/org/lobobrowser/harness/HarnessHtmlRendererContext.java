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
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.html.BrowserFrame;
import org.lobobrowser.html.FormInput;
import org.lobobrowser.html.HtmlObject;
import org.lobobrowser.html.HtmlRendererContext;
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

  private final HtmlPanel htmlPanel;
  private final UserAgentContext userAgentContext;
  private Runnable jobFinishedHandler;
  private String currentURL = "";

  public HarnessHtmlRendererContext(final HtmlPanel htmlPanel, final UserAgentContext userAgentContext) {
    this.htmlPanel = htmlPanel;
    this.userAgentContext = userAgentContext;
  }

  public HtmlPanel getHtmlPanel() {
    return htmlPanel;
  }

  public void setCurrentURL(final String currentURL) {
    this.currentURL = currentURL;
  }

  @Override
  public void navigate(final @NonNull URL url, final String target) {
    logger.info("navigate() called to: " + url + " target: " + target);
  }

  @Override
  public void linkClicked(final HTMLElement linkNode, final @NonNull URL url, final String target) {
  }

  @Override
  public HTMLCollection getFrames() {
    return new HTMLCollection() {
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
  }

  @Override
  public void submitForm(final String method, final @NonNull URL action, final String target, final String enctype, final FormInput[] formInputs) {
  }

  @Override
  public BrowserFrame createBrowserFrame() {
    return new BrowserFrame() {
      private final HtmlPanel subPanel = new HtmlPanel();
      private Document doc;

      @Override
      public Component getComponent() {
        return subPanel;
      }

      @Override
      public void loadURL(final @NonNull URL url) {
        try {
          final URLConnection conn = url.openConnection();
          final DocumentBuilderImpl builder = new DocumentBuilderImpl(userAgentContext, HarnessHtmlRendererContext.this);
          try (InputStream in = conn.getInputStream()) {
            final InputSourceImpl is = new InputSourceImpl(in, url.toExternalForm(), "UTF-8");
            doc = builder.parse(is);
            subPanel.setDocument(doc, HarnessHtmlRendererContext.this);
          }
        } catch (Exception e) {
          logger.log(Level.WARNING, "Error loading frame URL: " + url, e);
        }
      }

      @Override
      public Document getContentDocument() {
        return doc;
      }

      @Override
      public void setContentDocument(final Document d) {
        this.doc = d;
        subPanel.setDocument(d, HarnessHtmlRendererContext.this);
      }

      @Override
      public HtmlRendererContext getHtmlRendererContext() {
        return HarnessHtmlRendererContext.this;
      }

      @Override
      public void setDefaultMarginInsets(final Insets insets) {
      }

      @Override
      public void setDefaultOverflowX(final int overflowX) {
      }

      @Override
      public void setDefaultOverflowY(final int overflowY) {
      }
    };
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
    return null;
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
    return this;
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
    final Runnable handler = this.jobFinishedHandler;
    if (handler != null) {
      handler.run();
    }
  }

  @Override
  public void setJobFinishedHandler(final Runnable runnable) {
    this.jobFinishedHandler = runnable;
  }
}
