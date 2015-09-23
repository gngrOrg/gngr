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
 * Created on Dec 4, 2005
 */
package org.lobobrowser.html.domimpl;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.html.style.RenderState;
import org.lobobrowser.html.style.TableCellRenderState;
import org.w3c.dom.html.HTMLTableCellElement;

public class HTMLTableCellElementImpl extends HTMLAbstractUIElement implements HTMLTableCellElement {
  public HTMLTableCellElementImpl(final String name) {
    super(name);
  }

  public int getCellIndex() {
    // TODO Cell index in row
    return 0;
  }

  public String getAbbr() {
    return this.getAttribute("abbr");
  }

  public void setAbbr(final String abbr) {
    this.setAttribute("abbr", abbr);
  }

  public String getAlign() {
    return this.getAttribute("align");
  }

  public void setAlign(final String align) {
    this.setAttribute("align", align);
  }

  public String getAxis() {
    return this.getAttribute("axis");
  }

  public void setAxis(final String axis) {
    this.setAttribute("axis", axis);
  }

  public String getBgColor() {
    return this.getAttribute("bgcolor");
  }

  public void setBgColor(final String bgColor) {
    this.setAttribute("bgcolor", bgColor);
  }

  public String getCh() {
    return this.getAttribute("ch");
  }

  public void setCh(final String ch) {
    this.setAttribute("ch", ch);
  }

  public String getChOff() {
    return this.getAttribute("choff");
  }

  public void setChOff(final String chOff) {
    this.setAttribute("choff", chOff);
  }

  public int getColSpan() {
    final String colSpanText = this.getAttribute("colspan");
    if (colSpanText == null) {
      return 1;
    } else {
      try {
        return Integer.parseInt(colSpanText);
      } catch (final NumberFormatException nfe) {
        return 1;
      }
    }
  }

  public void setColSpan(final int colSpan) {
    this.setAttribute("colspan", String.valueOf(colSpan));
  }

  public String getHeaders() {
    return this.getAttribute("headers");
  }

  public void setHeaders(final String headers) {
    this.setAttribute("headers", headers);
  }

  public String getHeight() {
    return this.getAttribute("height");
  }

  public void setHeight(final String height) {
    this.setAttribute("height", height);
  }

  public boolean getNoWrap() {
    return "nowrap".equalsIgnoreCase(this.getAttribute("nowrap"));
  }

  public void setNoWrap(final boolean noWrap) {
    this.setAttribute("nowrap", noWrap ? "nowrap" : null);
  }

  public int getRowSpan() {
    final String rowSpanText = this.getAttribute("rowspan");
    if (rowSpanText == null) {
      return 1;
    } else {
      try {
        return Integer.parseInt(rowSpanText);
      } catch (final NumberFormatException nfe) {
        return 1;
      }
    }
  }

  public void setRowSpan(final int rowSpan) {
    this.setAttribute("rowspan", String.valueOf(rowSpan));
  }

  public String getScope() {
    return this.getAttribute("scope");
  }

  public void setScope(final String scope) {
    this.setAttribute("scope", scope);
  }

  public String getVAlign() {
    return this.getAttribute("valign");
  }

  public void setVAlign(final String vAlign) {
    this.setAttribute("valign", vAlign);
  }

  public String getWidth() {
    return this.getAttribute("width");
  }

  public void setWidth(final String width) {
    this.setAttribute("width", width);
  }

  @Override
  protected @NonNull RenderState createRenderState(final RenderState prevRenderState) {
    return new TableCellRenderState(prevRenderState, this);
  }
}
