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
 * Created on Feb 5, 2006
 */
package org.lobobrowser.primary.clientlets.html;

import java.awt.Component;
import java.awt.Insets;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.html.BrowserFrame;
import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.ua.NavigatorFrame;
import org.lobobrowser.ua.RequestType;
import org.lobobrowser.ua.TargetType;

public class BrowserFrameImpl implements BrowserFrame {
  private static final Logger logger = Logger.getLogger(BrowserFrameImpl.class.getName());
  private final NavigatorFrame frame;
  private final HtmlRendererContextImpl rcontext;

  public BrowserFrameImpl(final NavigatorFrame frame, final HtmlRendererContext parentrcontext) {
    if (logger.isLoggable(Level.INFO)) {
      logger.info("BrowserFrameImpl(): frame=" + frame + ",parentrcontext=" + parentrcontext);
    }
    this.frame = frame;
    this.rcontext = HtmlRendererContextImpl.getHtmlRendererContext(frame);
  }

  public HtmlRendererContext getHtmlRendererContext() {
    return this.rcontext;
  }

  public Component getComponent() {
    return this.frame.getComponent();
  }

  public org.w3c.dom.Document getContentDocument() {
    return this.rcontext.getContentDocument();
  }

  public void setContentDocument(final org.w3c.dom.Document d) {
    this.rcontext.setContentDocument(d);
  }

  public void loadURL(final @NonNull URL url) {
    if (logger.isLoggable(Level.INFO)) {
      logger.info("loadURL(): frame=" + frame + ",url=" + url);
    }
    this.frame.navigate(url, "GET", null, TargetType.SELF, RequestType.FRAME);
  }

  public void setDefaultMarginInsets(final Insets insets) {
    this.frame.setProperty("defaultMarginInsets", insets);
  }

  public void setDefaultOverflowX(final int overflowX) {
    this.frame.setProperty("defaultOverflowX", overflowX);
  }

  public void setDefaultOverflowY(final int overflowY) {
    this.frame.setProperty("defaultOverflowY", overflowY);
  }
}
