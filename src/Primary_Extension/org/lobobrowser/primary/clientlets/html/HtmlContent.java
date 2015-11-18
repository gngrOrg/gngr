/*
    GNU GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The Lobo Project

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public
    License as published by the Free Software Foundation; either
    version 2 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Contact info: lobochief@users.sourceforge.net
 */
package org.lobobrowser.primary.clientlets.html;

import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lobobrowser.clientlet.ComponentContent;
import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.gui.HtmlPanel;
import org.lobobrowser.util.io.BufferExceededException;
import org.lobobrowser.util.io.RecordedInputStream;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLDocument;
import org.w3c.dom.html.HTMLElement;

public class HtmlContent implements ComponentContent {
  private static final Logger logger = Logger.getLogger(HtmlContent.class.getName());
  private final HTMLDocument document;
  private final HtmlPanel panel;
  private final RecordedInputStream ris;
  private final String charset;
  private final String sourceCode;

  public HtmlContent(final HTMLDocument document, final HtmlPanel panel, final RecordedInputStream ris, final String charset) {
    super();
    this.document = document;
    this.panel = panel;
    this.ris = ris;
    this.charset = charset;
    this.sourceCode = null;
  }

  public HtmlContent(final HTMLDocument document, final HtmlPanel panel, final String sourceCode) {
    super();
    this.document = document;
    this.panel = panel;
    this.ris = null;
    this.charset = null;
    this.sourceCode = sourceCode;
  }

  public boolean canCopy() {
    return this.panel.hasSelection();
  }

  public boolean copy() {
    return this.panel.copy();
  }

  public Component getComponent() {
    return this.panel;
  }

  public String getSourceCode() {
    try {
      final RecordedInputStream ris = this.ris;
      if (ris != null) {
        final byte[] bytesSoFar = ris.getBytesRead();
        try {
          return new String(bytesSoFar, this.charset);
        } catch (final java.io.UnsupportedEncodingException uee) {
          return "[Error: " + uee + "]";
        }
      } else {
        return this.sourceCode;
      }
    } catch (final BufferExceededException bee) {
      return "[Error: Document content too large.]";
    }
  }

  public String getTitle() {
    return this.document.getTitle();
  }

  public String getDescription() {
    final NodeList nodeList = this.document.getElementsByTagName("meta");
    if (nodeList == null) {
      return null;
    }
    final int length = nodeList.getLength();
    for (int i = 0; i < length; i++) {
      final Node node = nodeList.item(i);
      if (node instanceof HTMLElement) {
        final HTMLElement element = (HTMLElement) node;
        final String name = element.getAttribute("name");
        if ((name != null) && name.equalsIgnoreCase("description")) {
          return element.getAttribute("description");
        }
      }
    }
    return null;
  }

  public void addNotify() {
  }

  public void navigatedNotify() {
    ((HTMLDocumentImpl) document).finishModifications();
  }

  public void removeNotify() {
  }

  public Object getContentObject() {
    return this.document;
  }

  public String getMimeType() {
    return "text/html";
  }

  public void setProperty(final String name, final Object value) {
    if ("defaultOverflowX".equals(name) && (value instanceof Integer)) {
      this.panel.setDefaultOverflowX((Integer) value);
    } else if ("defaultOverflowY".equals(name) && (value instanceof Integer)) {
      this.panel.setDefaultOverflowY((Integer) value);
    } else {
      if (logger.isLoggable(Level.INFO)) {
        logger.info("setProperty(): Unknown property: " + name);
      }
    }
  }

  @Override
  public boolean isReadyToPaint() {
    return this.panel.isReadyToPaint();
  }

  @Override
  public void disableRenderHints() {
    this.panel.disableRenderHints();
  }
}