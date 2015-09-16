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
 * Created on Nov 27, 2005
 */
package org.lobobrowser.html.domimpl;

import org.lobobrowser.html.style.CSSUtilities;
import org.lobobrowser.ua.UserAgentContext;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.css.CSSStyleSheet;
import org.w3c.dom.html.HTMLStyleElement;
import org.w3c.dom.stylesheets.LinkStyle;

import co.uproot.css.domimpl.JStyleSheetWrapper;
import cz.vutbr.web.css.StyleSheet;

public class HTMLStyleElementImpl extends HTMLElementImpl implements HTMLStyleElement, LinkStyle {
  private JStyleSheetWrapper styleSheet;

  public HTMLStyleElementImpl() {
    super("STYLE", true);
  }

  public HTMLStyleElementImpl(final String name) {
    super(name, true);
  }

  private boolean disabled;

  public boolean getDisabled() {
    return this.disabled;
  }

  public void setDisabled(final boolean disabled) {
    this.disabled = disabled;
    final CSSStyleSheet sheet = this.styleSheet;
    if (sheet != null) {
      sheet.setDisabled(disabled);
    }
  }

  //TODO hide from JS
  public void setDisabledImpl(final boolean disabled) {
    this.disabled = disabled;
  }

  public String getMedia() {
    return this.getAttribute("media");
  }

  public void setMedia(final String media) {
    this.setAttribute("media", media);
  }

  public String getType() {
    return this.getAttribute("type");
  }

  public void setType(final String type) {
    this.setAttribute("type", type);
  }

  // TODO: This should probably not be a nop. We should probably be handling changes to inner text.
  @Override
  protected void appendInnerTextImpl(final StringBuffer buffer) {
    // nop
  }

  @Override
  public void setAttribute(final String name, final String value) throws DOMException {
    super.setAttribute(name, value);
    if (isAttachedToDocument()) {
      final String nameLowerCase = name.toLowerCase();
      if ("type".equals(nameLowerCase) || "media".equals(nameLowerCase) || "title".equals(nameLowerCase)) {
        this.disabled = false;
        this.processStyle();
      }
    }
  }

  private String getOnlyText() {
    final NodeList nl = this.getChildNodes();
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < nl.getLength(); i++) {
      final Node n = nl.item(i);
      if (n.getNodeType() == Node.TEXT_NODE) {
        final Text textNode = (Text) n;
        sb.append(textNode.getTextContent());
      }
    }
    return sb.toString();
  }

  private boolean isAllowedType() {
    final String type = this.getType();
    return ((type == null) || (type.trim().length() == 0) || (type.equalsIgnoreCase("text/css")));
  }

  // TODO: check if this method can be made private
  protected void processStyle() {
    if (isAttachedToDocument()) {
      /* check if type == "text/css" or no, empty value is also allowed as well.
       if it is something other than empty or "text/css" set the style sheet to null
       we need not check for the media type here, jStyle parser should take care of this.
       */
      if (isAllowedType()) {
        final UserAgentContext uacontext = this.getUserAgentContext();
        if (uacontext.isInternalCSSEnabled()) {
          final HTMLDocumentImpl doc = (HTMLDocumentImpl) this.getOwnerDocument();
          final JStyleSheetWrapper newStyleSheet = processStyleHelper();
          newStyleSheet.setDisabled(this.disabled);
          this.styleSheet = newStyleSheet;
          doc.styleSheetManager.invalidateStyles();
        }
      } else {
        this.detachStyleSheet();
      }
    }
  }

  private JStyleSheetWrapper processStyleHelper() {
    final HTMLDocumentImpl doc = (HTMLDocumentImpl) this.getOwnerDocument();
    // TODO a sanity check can be done for the media type while setting it to the style sheet
    // as in is it a valid media type or not
    try {
      final String text = this.getOnlyText();
      final String processedText = CSSUtilities.preProcessCss(text);
      final String baseURI = doc.getBaseURI();
      // TODO if the new StyleSheet contains any @import rules, then we should queue them for further processing. GH #137
      final StyleSheet jSheet = CSSUtilities.jParseStyleSheet(this, baseURI, processedText, doc.getUserAgentContext());
      return new JStyleSheetWrapper(jSheet, this.getMedia(), null, this.getType(), this.getTitle(), this, doc.styleSheetManager.bridge);
    } catch (final Exception err) {
      this.warn("Unable to parse style sheet", err);
    }
    return this.getEmptyStyleSheet();
  }

  private JStyleSheetWrapper getEmptyStyleSheet() {
    final HTMLDocumentImpl doc = (HTMLDocumentImpl) this.getOwnerDocument();
    return new JStyleSheetWrapper(CSSUtilities.getEmptyStyleSheet(), this.getMedia(), null, this.getType(), this.getTitle(), this,
        doc.styleSheetManager.bridge);
  }

  private void detachStyleSheet() {
    if (this.styleSheet != null) {
      this.styleSheet.setOwnerNode(null);
      this.styleSheet = null;
      final HTMLDocumentImpl doc = (HTMLDocumentImpl) this.getOwnerDocument();
      doc.styleSheetManager.invalidateStyles();
    }
  }

  public CSSStyleSheet getSheet() {
    return this.styleSheet;
  }

  @Override
  protected void handleChildListChanged() {
    this.processStyle();
  }

  @Override
  protected void handleDocumentAttachmentChanged() {
    if (isAttachedToDocument()) {
      this.processStyle();
    } else {
      this.detachStyleSheet();
    }
  }

}
