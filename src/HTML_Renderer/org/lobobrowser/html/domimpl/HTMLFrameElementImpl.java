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
 * Created on Jan 28, 2006
 */
package org.lobobrowser.html.domimpl;

import java.net.MalformedURLException;

import org.lobobrowser.html.BrowserFrame;
import org.lobobrowser.html.js.Window;
import org.lobobrowser.ua.UserAgentContext.Request;
import org.lobobrowser.ua.UserAgentContext.RequestKind;
import org.w3c.dom.Document;
import org.w3c.dom.html.HTMLFrameElement;

public class HTMLFrameElementImpl extends HTMLElementImpl implements HTMLFrameElement, FrameNode {
  private volatile BrowserFrame browserFrame;

  public HTMLFrameElementImpl(final String name, final boolean noStyleSheet) {
    super(name, noStyleSheet);
  }

  public HTMLFrameElementImpl(final String name) {
    super(name);
  }

  public void setBrowserFrame(final BrowserFrame frame) {
    this.browserFrame = frame;
    loadURL();
  }

  private void loadURL() {
    final String src = getAttribute("src");
    if (src != null) {
      try {
        final java.net.URL fullURL = getFullURL(src);
        if (getUserAgentContext().isRequestPermitted(new Request(fullURL, RequestKind.Frame))) {
          browserFrame.loadURL(fullURL);
        }
      } catch (final MalformedURLException mfu) {
        logger.warning("Frame URI=[" + src + "] is malformed.");
      }
    }
  }

  public BrowserFrame getBrowserFrame() {
    return this.browserFrame;
  }

  public String getFrameBorder() {
    return this.getAttribute("frameBorder");
  }

  public void setFrameBorder(final String frameBorder) {
    this.setAttribute("frameBorder", frameBorder);
  }

  public String getLongDesc() {
    return this.getAttribute("longdesc");
  }

  public void setLongDesc(final String longDesc) {
    this.setAttribute("longdesc", longDesc);
  }

  public String getMarginHeight() {
    return this.getAttribute("marginHeight");
  }

  public void setMarginHeight(final String marginHeight) {
    this.setAttribute("marginHeight", marginHeight);
  }

  public String getMarginWidth() {
    return this.getAttribute("marginWidth");
  }

  public void setMarginWidth(final String marginWidth) {
    this.setAttribute("marginWidth", marginWidth);
  }

  public String getName() {
    return this.getAttribute("name");
  }

  public void setName(final String name) {
    this.setAttribute("name", name);
  }

  private boolean noResize;

  public boolean getNoResize() {
    return this.noResize;
  }

  public void setNoResize(final boolean noResize) {
    this.noResize = noResize;
  }

  public String getScrolling() {
    return this.getAttribute("scrolling");
  }

  public void setScrolling(final String scrolling) {
    this.setAttribute("scrolling", scrolling);
  }

  public String getSrc() {
    return this.getAttribute("src");
  }

  public void setSrc(final String src) {
    this.setAttribute("src", src);
  }

  public Document getContentDocument() {
    final BrowserFrame frame = this.browserFrame;
    if (frame == null) {
      // Not loaded yet
      return null;
    }
    return frame.getContentDocument();
  }

  public Window getContentWindow() {
    final BrowserFrame frame = this.browserFrame;
    if (frame == null) {
      // Not loaded yet
      return null;
    }
    return Window.getWindow(frame.getHtmlRendererContext());
  }

}
