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
 * Created on Nov 19, 2005
 */
package org.lobobrowser.html.domimpl;

import java.util.ArrayList;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.html.js.Executor;
import org.lobobrowser.html.js.Window;
import org.lobobrowser.html.style.ImageRenderState;
import org.lobobrowser.html.style.RenderState;
import org.lobobrowser.ua.ImageResponse;
import org.lobobrowser.ua.ImageResponse.State;
import org.mozilla.javascript.Function;
import org.w3c.dom.UserDataHandler;
import org.w3c.dom.html.HTMLImageElement;

public class HTMLImageElementImpl extends HTMLAbstractUIElement implements HTMLImageElement {
  public HTMLImageElementImpl() {
    super("IMG");
  }

  public HTMLImageElementImpl(final String name) {
    super(name);
  }

  public String getName() {
    return this.getAttribute("name");
  }

  public void setName(final String name) {
    this.setAttribute("name", name);
  }

  public String getAlign() {
    return this.getAttribute("align");
  }

  public void setAlign(final String align) {
    this.setAttribute("align", align);
  }

  public String getAlt() {
    return this.getAttribute("alt");
  }

  public void setAlt(final String alt) {
    this.setAttribute("alt", alt);
  }

  public String getBorder() {
    return this.getAttribute("border");
  }

  public void setBorder(final String border) {
    this.setAttribute("border", border);
  }

  /*
  public int getHeight() {
    final UINode r = this.uiNode;
    return r == null ? 0 : r.getBounds().height;
  }

  public void setHeight(final int height) {
    this.setAttribute("height", String.valueOf(height));
  }

  public int getHspace() {
    return this.getAttributeAsInt("hspace", 0);
  }

  public void setHspace(final int hspace) {
    this.setAttribute("hspace", String.valueOf("hspace"));
  } */

  public String getHeight() {
    final UINode r = this.uiNode;
    final int height = r == null ? 0 : r.getBounds().height;
    return String.valueOf(height);
  }

  public void setHeight(final String height) {
    this.setAttribute("height", height);
  }

  public String getHspace() {
    return this.getAttribute("hspace");
  }

  public void setHspace(final String hspace) {
    this.setAttribute("hspace", hspace);
  }

  public boolean getIsMap() {
    return this.getAttributeAsBoolean("isMap");
  }

  public void setIsMap(final boolean isMap) {
    this.setAttribute("isMap", isMap ? "isMap" : null);
  }

  public String getLongDesc() {
    return this.getAttribute("longDesc");
  }

  public void setLongDesc(final String longDesc) {
    this.setAttribute("longDesc", longDesc);
  }

  public String getSrc() {
    return this.getAttribute("src");
  }

  /**
   * Sets the image URI and starts to load the image. Note that an
   * HtmlRendererContext should be available to the HTML document for images to
   * be loaded.
   */
  public void setSrc(final String src) {
    this.setAttribute("src", src);
  }

  public String getUseMap() {
    return this.getAttribute("useMap");
  }

  public void setUseMap(final String useMap) {
    this.setAttribute("useMap", useMap);
  }

  /* public int getVspace() {
    return this.getAttributeAsInt("vspace", 0);
  }

  public void setVspace(final int vspace) {
    this.setAttribute("vspace", String.valueOf(vspace));
  } */

  public String getVspace() {
    return this.getAttribute("vspace");
  }

  public void setVspace(final String vspace) {
    this.setAttribute("vspace", vspace);
  }

  /* public int getWidth() {
    final UINode r = this.uiNode;
    return r == null ? 0 : r.getBounds().width;
  }

  public void setWidth(final int width) {
    this.setAttribute("width", String.valueOf(width));
  }*/

  public String getWidth() {
    final UINode r = this.uiNode;
    final int width = r == null ? 0 : r.getBounds().width;
    return String.valueOf(width);
  }

  public void setWidth(final String width) {
    this.setAttribute("width", width);
  }

  @Override
  protected void handleAttributeChanged(final String name, final String oldValue, final String newValue) {
    super.handleAttributeChanged(name, oldValue, newValue);
    if ("src".equals(name)) {
      ((HTMLDocumentImpl) document).addJob(() -> loadImage(getSrc()), false);
    }
  }

  private Function onload;

  public Function getOnload() {
    return this.getEventFunction(this.onload, "onload");
  }

  public void setOnload(final Function onload) {
    this.onload = onload;
  }

  private @NonNull ImageResponse imageResponse = new ImageResponse();
  private String imageSrc;

  private void loadImage(final String src) {
    final HTMLDocumentImpl document = (HTMLDocumentImpl) this.document;
    if (document != null) {
      synchronized (this.listeners) {
        this.imageSrc = src;
        this.imageResponse = new ImageResponse();
      }
      if (src != null) {
        document.loadImage(src, new LocalImageListener(src));
      } else {
        document.markJobsFinished(1, false);
      }
    }
  }

  @Override
  public Object setUserData(final String key, final Object data, final UserDataHandler handler) {
    if (org.lobobrowser.html.parser.HtmlParser.MODIFYING_KEY.equals(key) && (data != Boolean.TRUE)) {
      ((HTMLDocumentImpl) document).addJob(() -> loadImage(getSrc()), false);
      // this.loadImage(getSrc());
    }
    return super.setUserData(key, data, handler);
  }

  private final ArrayList<ImageListener> listeners = new ArrayList<>(1);

  /**
   * Adds a listener of image loading events. The listener gets called right
   * away if there's already an image.
   *
   * @param listener
   */
  public void addImageListener(final ImageListener listener) {
    final ArrayList<ImageListener> l = this.listeners;
    ImageResponse currentImageResponse;
    synchronized (l) {
      currentImageResponse = this.imageResponse;
      l.add(listener);
    }
    if (currentImageResponse.state != State.loading) {
      // Call listener right away if there's already an
      // image; holding no locks.
      listener.imageLoaded(new ImageEvent(this, currentImageResponse));
      // Should not call onload handler here. That's taken
      // care of otherwise.
    }
  }

  public void removeImageListener(final ImageListener listener) {
    final ArrayList<ImageListener> l = this.listeners;
    synchronized (l) {
      l.remove(listener);
    }
  }

  private void dispatchEvent(final String expectedImgSrc, final ImageEvent event) {
    final ArrayList<ImageListener> l = this.listeners;
    ImageListener[] listenerArray;
    synchronized (l) {
      if (!expectedImgSrc.equals(this.imageSrc)) {
        return;
      }
      this.imageResponse = event.imageResponse;
      // Get array of listeners while holding lock.
      listenerArray = l.toArray(ImageListener.EMPTY_ARRAY);
    }
    final int llength = listenerArray.length;
    for (int i = 0; i < llength; i++) {
      // Inform listener, holding no lock.
      listenerArray[i].imageLoaded(event);
    }
    final Function onload = this.getOnload();
    if (onload != null) {
      // TODO: onload event object?
      final Window window = ((HTMLDocumentImpl) document).getWindow();
      Executor.executeFunction(HTMLImageElementImpl.this, onload, null, window.getContextFactory());
    }
  }

  @Override
  protected @NonNull RenderState createRenderState(final RenderState prevRenderState) {
    return new ImageRenderState(prevRenderState, this);
  }

  private class LocalImageListener implements ImageListener {
    private final String expectedImgSrc;

    public LocalImageListener(final String imgSrc) {
      this.expectedImgSrc = imgSrc;
    }

    public void imageLoaded(final ImageEvent event) {
      dispatchEvent(this.expectedImgSrc, event);
      if (document instanceof HTMLDocumentImpl) {
        final HTMLDocumentImpl htmlDocumentImpl = (HTMLDocumentImpl) document;
        htmlDocumentImpl.markJobsFinished(1, false);
      }
    }

    public void imageAborted() {
      if (document instanceof HTMLDocumentImpl) {
        final HTMLDocumentImpl htmlDocumentImpl = (HTMLDocumentImpl) document;
        htmlDocumentImpl.markJobsFinished(1, false);
      }
    }
  }

  public String getLowSrc() {
    // TODO
    return null;
  }

  public void setLowSrc(final String lowSrc) {
    // TODO
  }
}
