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
package org.lobobrowser.html.domimpl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.js.Window;
import org.lobobrowser.html.style.CSSUtilities;
import org.lobobrowser.ua.UserAgentContext;
import org.lobobrowser.util.Urls;
import org.w3c.dom.css.CSSStyleSheet;
import org.w3c.dom.html.HTMLLinkElement;
import org.w3c.dom.stylesheets.LinkStyle;

import co.uproot.css.domimpl.JStyleSheetWrapper;
import cz.vutbr.web.css.StyleSheet;

public class HTMLLinkElementImpl extends HTMLAbstractUIElement implements HTMLLinkElement, LinkStyle {
  private static final Logger logger = Logger.getLogger(HTMLLinkElementImpl.class.getName());
  private static final boolean loggableInfo = logger.isLoggable(Level.INFO);
  private JStyleSheetWrapper styleSheet;

  public HTMLLinkElementImpl(final String name) {
    super(name);
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

  public String getHref() {
    final String href = this.getAttribute("href");
    return href == null ? "" : Urls.removeControlCharacters(href);
  }

  public void setHref(final String href) {
    this.setAttribute("href", href);
  }

  public String getHreflang() {
    return this.getAttribute("hreflang");
  }

  public void setHreflang(final String hreflang) {
    this.setAttribute("hreflang", hreflang);
  }

  public String getMedia() {
    return this.getAttribute("media");
  }

  public void setMedia(final String media) {
    this.setAttribute("media", media);
  }

  public String getRel() {
    return this.getAttribute("rel");
  }

  public void setRel(final String rel) {
    this.setAttribute("rel", rel);
  }

  public String getRev() {
    return this.getAttribute("rev");
  }

  public void setRev(final String rev) {
    this.setAttribute("rev", rev);
  }

  public String getTarget() {
    final String target = this.getAttribute("target");
    if (target != null) {
      return target;
    }
    final HTMLDocumentImpl doc = (HTMLDocumentImpl) this.document;
    return doc == null ? null : doc.getDefaultTarget();
  }

  public void setTarget(final String target) {
    this.setAttribute("target", target);
  }

  public String getType() {
    return this.getAttribute("type");
  }

  public void setType(final String type) {
    this.setAttribute("type", type);
  }

  // TODO can go in Urls util class.
  private boolean isWellFormedURL() {
    final HTMLDocumentImpl doc = (HTMLDocumentImpl) this.getOwnerDocument();
    try {
      final URL baseURL = new URL(doc.getBaseURI());
      // we call createURL just to check whether it throws an exception
      // if the URL is not well formed.
      Urls.createURL(baseURL, this.getHref());
      return true;
    } catch (final MalformedURLException mfe) {
      this.warn("Will not parse CSS. URI=[" + this.getHref() + "] with BaseURI=[" + doc.getBaseURI()
          + "] does not appear to be a valid URI.");
      return false;
    }
  }

  /*private Optional<URL> getAbsoluteURL() {
    final HtmlRendererContext rcontext = this.getHtmlRendererContext();
    if (rcontext != null) {
      final String href = this.getHref();
      if (href != null && href.length() > 0) {
        if (href.startsWith("javascript:")) {
          return Optional.empty();
        } else {
          try {
            return Optional.ofNullable(this.getFullURL(href));
          } catch (final MalformedURLException mfu) {
            this.warn("Malformed URI: [" + href + "].", mfu);
          }
        }
      }
    }
    return Optional.empty();
  }*/

  // simplified version
  private Optional<URL> getAbsoluteURL() {
    final String href = this.getHref();
    if (href.startsWith("javascript:")) {
      return Optional.empty();
    } else {
      try {
        return Optional.ofNullable(this.getFullURL(href));
      } catch (final MalformedURLException mfu) {
        this.warn("Malformed URI: [" + href + "].", mfu);
      }
    }
    return Optional.empty();
  }

  public String getAbsoluteHref() {
    // TODO: Use Either in getAbsoluteURL and use the branch type for javascript
    return getAbsoluteURL().map(u -> u.toExternalForm()).orElse(getHref());

    /*
    final HtmlRendererContext rcontext = this.getHtmlRendererContext();
    if (rcontext != null) {
      final String href = this.getHref();
      if ((href != null) && (href.length() > 0)) {
        if (href.startsWith("javascript:")) {
          return href;
        } else {
          try {
            final URL url = this.getFullURL(href);
            return url == null ? null : url.toExternalForm();
          } catch (final MalformedURLException mfu) {
            this.warn("Malformed URI: [" + href + "].", mfu);
          }
        }
      }
    }
    return null; */
  }

  // TODO: Hide from JS
  // TODO: Should HTMLLinkElement actually support navigation? The Link element seems to be conflated with <a> elements
  public boolean navigate() {

    // If there is no href attribute, chromium only dispatches the handlers without starting a navigation
    final String hrefAttr = this.getAttribute("href");
    if (hrefAttr == null) {
      // final boolean eventResult = dispatchEvent(new Event("click", this));
      // System.out.println("event result: " + eventResult);
      return false;
    }

    if (this.disabled) {
      return false;
    }
    System.out.println("Consider Navigating to: " + getHref());
    // final String href = getAbsoluteHref();
    final String href = getHref();
    if (href.startsWith("#")) {
      // TODO: Scroll to the element. Issue #101
    } else if (href.startsWith("javascript:")) {

      final String script = href.substring(11);
      System.out.println("Evaling script: " + script);

      // evalInScope adds the JS task
      ((Window) (((HTMLDocumentImpl) document).getDefaultView())).evalInScope(script);

      /*
      ((Window) (((HTMLDocumentImpl) document).getDefaultView())).addJSTask(new JSRunnableTask(0, () -> {
       Executor.executeFunction(this, f, null);
      }));*/
    } else {
      final Optional<URL> urlOpt = getAbsoluteURL();
      if (urlOpt.isPresent()) {
        final HtmlRendererContext rcontext = this.getHtmlRendererContext();
        final String target = this.getTarget();
        rcontext.linkClicked(this, urlOpt.get(), target);
        return true;

      }
    }
    return false;
    /*
    final HtmlRendererContext rcontext = this.getHtmlRendererContext();
    if (rcontext != null) {
      final String href = this.getHref();
      if ((href != null) && (href.length() > 0)) {
        final String target = this.getTarget();
        try {
          final URL url = this.getFullURL(href);
          if (url == null) {
            this.warn("Unable to resolve URI: [" + href + "].");
          } else {
            rcontext.linkClicked(this, url, target);
          }
        } catch (final MalformedURLException mfu) {
          this.warn("Malformed URI: [" + href + "].", mfu);
        }
      }
    }*/
  }

  /*
   * Not used anymore after removal of createRenderState. However, it can be re-implemented using
   * HTMLElementImple.elementMatchCondition.
   * Note that there are privacy implications here. It is better to understand them before
   * re-implementing.
  private java.awt.Color getLinkColor() {
    final HTMLDocument doc = (HTMLDocument) this.document;
    if (doc != null) {
      final HTMLBodyElement body = (HTMLBodyElement) doc.getBody();
      if (body != null) {
        final String vlink = body.getVLink();
        final String link = body.getLink();
        if (vlink != null || link != null) {
          final HtmlRendererContext rcontext = this.getHtmlRendererContext();
          if (rcontext != null) {
            final boolean visited = rcontext.isVisitedLink(this);
            final String colorText = visited ? vlink : link;
            if (colorText != null) {
              return ColorFactory.getInstance().getColor(colorText);
            }
          }
        }
      }
    }
    return java.awt.Color.BLUE;
  }*/

  /*
  protected RenderState createRenderState(RenderState prevRenderState) {
    if (this.hasAttribute("href")) {
      // Removed the following three as part of #135
      // prevRenderState = new TextDecorationRenderState(prevRenderState, RenderState.MASK_TEXTDECORATION_UNDERLINE);
      // prevRenderState = new ColorRenderState(prevRenderState, this.getLinkColor());
      // prevRenderState = new CursorRenderState(prevRenderState, Optional.of(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)));
    }
    return super.createRenderState(prevRenderState);
  }*/

  @Override
  public String toString() {
    // Javascript code often depends on this being exactly href. See js9.html.
    // To change, perhaps add method to AbstractScriptableDelegate.
    // Chromium 37 and FF 32 both return the full url
    // return this.getHref();
    return getAbsoluteHref();
  }

  /**
   * Sets the owner node to null so as to update the old reference of the
   * stylesheet held by JS
   */
  private void detachStyleSheet() {
    if (this.styleSheet != null) {
      this.styleSheet.setOwnerNode(null);
      this.styleSheet = null;
      final HTMLDocumentImpl doc = (HTMLDocumentImpl) this.getOwnerDocument();
      doc.styleSheetManager.invalidateStyles();
    }
  }

  private boolean isSameRel(final String name, final String oldValue) {
    if ("rel".equals(name)) {
      if (this.isSameAttributeValue("rel", oldValue)) {
        return true;
      }
    }
    return false;
  }

  private boolean isSameHref(final String name, final String oldValue) {
    if ("href".equals(name)) {
      if (this.isSameAttributeValue("href", oldValue)) {
        return true;
      }
    }
    return false;
  }

  private boolean isSameAttributeValue(final String name, final String oldValue) {
    final String newValue = this.getAttribute(name);
    if (oldValue == null) {
      return newValue == null;
    } else {
      return oldValue.equals(newValue);
    }
  }

  private String getCleanRel() {
    final String rel = this.getRel();
    return rel == null ? null : rel.trim().toLowerCase();
  }

  private boolean isStyleSheet() {
    final String rel = this.getCleanRel();
    return ((rel != null) && (rel.equals("stylesheet")));
  }

  private boolean isAltStyleSheet() {
    final String rel = this.getCleanRel();
    return ((rel != null) && (rel.equals("alternate stylesheet")));
  }

  private boolean isAllowedRel() {
    return ((isStyleSheet()) || (isAltStyleSheet()));
  }

  private boolean isAllowedType() {
    final String type = this.getType();
    return ((type == null) || (type.trim().length() == 0) || (type.equalsIgnoreCase("text/css")));
  }

  private void processLink() {
    final HTMLDocumentImpl doc = (HTMLDocumentImpl) this.getOwnerDocument();
    try {
      final UserAgentContext uacontext = this.getUserAgentContext();
      if (uacontext.isExternalCSSEnabled()) {
        try {
          final boolean liflag = loggableInfo;
          final long time1 = liflag ? System.currentTimeMillis() : 0;
          try {
            final String href = this.getHref();
            final StyleSheet jSheet = CSSUtilities.jParse(this, href, doc, doc.getBaseURI(), false);
            if (this.styleSheet != null) {
              this.styleSheet.setJStyleSheet(jSheet);
            } else {
              final JStyleSheetWrapper styleSheet = new JStyleSheetWrapper(jSheet, this.getMedia(), href, this.getType(), this.getTitle(),
                  this, doc.styleSheetManager.bridge);
              this.styleSheet = styleSheet;
            }
            this.styleSheet.setDisabled(this.isAltStyleSheet() | this.disabled);
            doc.styleSheetManager.invalidateStyles();
          } finally {
            if (liflag) {
              final long time2 = System.currentTimeMillis();
              logger.info("processLink(): Loaded and parsed CSS (or attempted to) at URI=[" + this.getHref() + "] in " + (time2 - time1)
                  + " ms.");
            }
          }
        } catch (final MalformedURLException mfe) {
          this.detachStyleSheet();
          this.warn("Will not parse CSS. URI=[" + this.getHref() + "] with BaseURI=[" + doc.getBaseURI()
              + "] does not appear to be a valid URI.");
        } catch (final Throwable err) {
          this.warn("Unable to parse CSS. URI=[" + this.getHref() + "].", err);
        }
      }
    } finally {
      doc.markJobsFinished(1);
    }
  }

  private void deferredProcess() {
    processLinkHelper(true);
  }

  private void processLinkHelper(final boolean defer) {
    // according to firefox, whenever the URL is not well formed, the style sheet has to be null
    // and in all other cases an empty style sheet has to be set till the link resource can be fetched
    // and processed. But however the style sheet is not in ready state till it is processed. This is
    // indicated by setting the jStyleSheet of the JStyleSheetWrapper to null.
    final HTMLDocumentImpl doc = (HTMLDocumentImpl) this.getOwnerDocument();
    if (isAttachedToDocument() && isWellFormedURL() && isAllowedRel() && isAllowedType()) {
      if (defer) {
        this.styleSheet = this.getEmptyStyleSheet();
        doc.styleSheetManager.invalidateStyles();
        //TODO need to think how to schedule this. refer issue #69
        doc.addJob(() -> this.processLinkHelper(false));
      } else {
        processLink();
      }
    } else {
      this.detachStyleSheet();
      if (!defer) {
        doc.markJobsFinished(1);
      }
    }
  }

  private JStyleSheetWrapper getEmptyStyleSheet() {
    final HTMLDocumentImpl doc = (HTMLDocumentImpl) this.getOwnerDocument();
    return new JStyleSheetWrapper(null, this.getMedia(), this.getHref(), this.getType(), this.getTitle(), this,
        doc.styleSheetManager.bridge);
  }

  public CSSStyleSheet getSheet() {
    return this.styleSheet;
  }

  @Override
  protected void handleDocumentAttachmentChanged() {
    deferredProcess();
  }

  @Override
  protected void handleAttributeChanged(final String name, final String oldValue, final String newValue) {
    super.handleAttributeChanged(name, oldValue, newValue);

    // TODO according to firefox's behavior whenever a valid attribute is
    // changed on the element the disabled flag is set to false. Need to
    // verify with the specs.
    // TODO check for all the attributes associated with an link element
    // according to firefox if the new value of rel/href is the same as the
    // old one then, the nothing has to be done. In all other cases the link element
    // has to be re-processed.
    if (isSameRel(name, oldValue) || isSameHref(name, oldValue)) {
      return;
    } else if ("rel".equals(name) || "href".equals(name) || "type".equals(name) || "media".equals(name)) {
      this.disabled = false;
      this.detachStyleSheet();
      this.processLinkHelper(true);
    }
  }
}
