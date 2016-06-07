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
 * Created on Dec 3, 2005
 */
package org.lobobrowser.html.domimpl;

import java.util.ArrayList;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.html.js.PropertyName;
import org.lobobrowser.html.style.HtmlLength;
import org.lobobrowser.html.style.HtmlValues;
import org.lobobrowser.html.style.JStyleProperties;
import org.lobobrowser.html.style.RenderState;
import org.lobobrowser.html.style.TableRenderState;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.html.HTMLCollection;
import org.w3c.dom.html.HTMLElement;
import org.w3c.dom.html.HTMLTableCaptionElement;
import org.w3c.dom.html.HTMLTableElement;
import org.w3c.dom.html.HTMLTableSectionElement;

public class HTMLTableElementImpl extends HTMLAbstractUIElement implements HTMLTableElement {

  public HTMLTableElementImpl() {
    super("TABLE");
  }

  public HTMLTableElementImpl(final String name) {
    super(name);
  }

  private HTMLTableCaptionElement caption;

  public HTMLTableCaptionElement getCaption() {
    return this.caption;
  }

  public void setCaption(final HTMLTableCaptionElement caption) throws DOMException {
    this.caption = caption;
  }

  private HTMLTableSectionElement thead;

  public HTMLTableSectionElement getTHead() {
    return this.thead;
  }

  public void setTHead(final HTMLTableSectionElement tHead) throws DOMException {
    this.thead = tHead;
  }

  private HTMLTableSectionElement tfoot;

  public HTMLTableSectionElement getTFoot() {
    return this.tfoot;
  }

  public void setTFoot(final HTMLTableSectionElement tFoot) throws DOMException {
    this.tfoot = tFoot;
  }

  public HTMLCollection getRows() {
    // TODO: filter by display: table-row
    return new DescendentHTMLCollection(this, new NodeNameFilter("TR"), this.treeLock, false);
  }

  @PropertyName("tBodies")
  public HTMLCollection getTBodies() {
    // TODO: filter by display: table-row-group
    return new DescendentHTMLCollection(this, new NodeNameFilter("TBODY"), this.treeLock, false);
  }

  public String getAlign() {
    return this.getAttribute("align");
  }

  public void setAlign(final String align) {
    this.setAttribute("align", align);
  }

  public String getBgColor() {
    return this.getAttribute("bgcolor");
  }

  public void setBgColor(final String bgColor) {
    this.setAttribute("bgcolor", bgColor);
  }

  public String getBorder() {
    return this.getAttribute("border");
  }

  public void setBorder(final String border) {
    this.setAttribute("border", border);
  }

  public String getCellPadding() {
    return this.getAttribute("cellpadding");
  }

  public void setCellPadding(final String cellPadding) {
    this.setAttribute("cellpadding", cellPadding);
  }

  public String getCellSpacing() {
    return this.getAttribute("cellspacing");
  }

  public void setCellSpacing(final String cellSpacing) {
    this.setAttribute("cellspacing", cellSpacing);
  }

  public String getFrame() {
    return this.getAttribute("frame");
  }

  public void setFrame(final String frame) {
    this.setAttribute("frame", frame);
  }

  public String getRules() {
    return this.getAttribute("rules");
  }

  public void setRules(final String rules) {
    this.setAttribute("rules", rules);
  }

  public String getSummary() {
    return this.getAttribute("summary");
  }

  public void setSummary(final String summary) {
    this.setAttribute("summary", summary);
  }

  public String getWidth() {
    return this.getAttribute("width");
  }

  public void setWidth(final String width) {
    this.setAttribute("width", width);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.renderer.RenderableContext#getHeightLength()
   */
  public HtmlLength getHeightLength(final int availHeight) {
    try {
      final JStyleProperties props = this.getCurrentStyle();
      final String heightText = props.getHeight();
      if (heightText == null) {
        // TODO: convert attributes to CSS properties
        return new HtmlLength(HtmlValues.getPixelSize(this.getAttribute("height"), this.getRenderState(), 0, availHeight));
      } else {
        return new HtmlLength(HtmlValues.getPixelSize(heightText, this.getRenderState(), 0, availHeight));
      }
    } catch (final NumberFormatException err) {
      System.out.println("Number format exception: " + err);
      return null;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.renderer.RenderableContext#getWidthLength()
   */
  public HtmlLength getWidthLength(final int availWidth) {
    try {
      final JStyleProperties props = this.getCurrentStyle();
      final String widthText = props.getWidth();
      if (widthText == null) {
        // TODO: convert attributes to CSS properties
        return new HtmlLength(HtmlValues.getPixelSize(this.getAttribute("width"), this.getRenderState(), 0, availWidth));
      } else {
        return new HtmlLength(HtmlValues.getPixelSize(widthText, this.getRenderState(), 0, availWidth));
      }
    } catch (final NumberFormatException err) {
      System.out.println("Number format exception: " + err);
      return null;
    }
  }

  public HTMLElement createTHead() {
    final org.w3c.dom.Document doc = this.document;
    return doc == null ? null : (HTMLElement) doc.createElement("thead");
  }

  public void deleteTHead() {
    this.removeChildren(new NodeNameFilter("THEAD"));
  }

  public HTMLElement createTFoot() {
    final org.w3c.dom.Document doc = this.document;
    return doc == null ? null : (HTMLElement) doc.createElement("tfoot");
  }

  public void deleteTFoot() {
    this.removeChildren(new NodeNameFilter("TFOOT"));
  }

  public HTMLElement createCaption() {
    final org.w3c.dom.Document doc = this.document;
    return doc == null ? null : (HTMLElement) doc.createElement("caption");
  }

  public void deleteCaption() {
    this.removeChildren(new NodeNameFilter("CAPTION"));
  }

  /**
   * Inserts a row at the index given. If <code>index</code> is <code>-1</code>,
   * the row is appended as the last row.
   */
  public HTMLElement insertRow(final int index) throws DOMException {
    final org.w3c.dom.Document doc = this.document;
    if (doc == null) {
      throw new DOMException(DOMException.WRONG_DOCUMENT_ERR, "Orphan element");
    }
    final HTMLElement rowElement = (HTMLElement) doc.createElement("TR");
    synchronized (this.treeLock) {
      if (index == -1) {
        this.appendChild(rowElement);
        return rowElement;
      }
      final ArrayList<Node> nl = this.nodeList;
      if (nl != null) {
        final int size = nl.size();
        int trcount = 0;
        for (int i = 0; i < size; i++) {
          final Node node = nl.get(i);
          if ("TR".equalsIgnoreCase(node.getNodeName())) {
            if (trcount == index) {
              this.insertAt(rowElement, i);
              return rowElement;
            }
            trcount++;
          }
        }
      } else {
        this.appendChild(rowElement);
        return rowElement;
      }
    }
    throw new DOMException(DOMException.INDEX_SIZE_ERR, "Index out of range");
  }

  public void deleteRow(final int index) throws DOMException {
    synchronized (this.treeLock) {
      final ArrayList<Node> nl = this.nodeList;
      if (nl != null) {
        final int size = nl.size();
        int trcount = 0;
        for (int i = 0; i < size; i++) {
          final Node node = nl.get(i);
          if ("TR".equalsIgnoreCase(node.getNodeName())) {
            if (trcount == index) {
              this.removeChildAt(i);
              return;
            }
            trcount++;
          }
        }
      }
    }
    throw new DOMException(DOMException.INDEX_SIZE_ERR, "Index out of range");
  }

  @Override
  protected @NonNull RenderState createRenderState(final RenderState prevRenderState) {
    return new TableRenderState(prevRenderState, this);
  }
}
