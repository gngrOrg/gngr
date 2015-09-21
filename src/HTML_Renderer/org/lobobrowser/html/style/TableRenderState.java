/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The XAMJ Project

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
package org.lobobrowser.html.style;

import java.awt.Color;
import java.net.MalformedURLException;

import org.lobobrowser.html.domimpl.HTMLElementImpl;
import org.lobobrowser.html.domimpl.HTMLTableElementImpl;
import org.lobobrowser.util.gui.ColorFactory;

public class TableRenderState extends StyleSheetRenderState {
  public TableRenderState(final RenderState prevRenderState, final HTMLElementImpl element) {
    super(prevRenderState, element);
  }

  @Override
  protected int getDefaultDisplay() {
    return DISPLAY_TABLE;
  }

  private BackgroundInfo backgroundInfo = INVALID_BACKGROUND_INFO;

  @Override
  public void invalidate() {
    super.invalidate();
    this.backgroundInfo = INVALID_BACKGROUND_INFO;
  }

  // TODO: This could be removed after #158 is implemented
  @Override
  public BackgroundInfo getBackgroundInfo() {
    BackgroundInfo binfo = this.backgroundInfo;
    if (binfo != INVALID_BACKGROUND_INFO) {
      return binfo;
    }
    // Apply style based on deprecated attributes.
    binfo = super.getBackgroundInfo();
    final HTMLTableElementImpl element = (HTMLTableElementImpl) this.element;
    if ((binfo == null) || (binfo.backgroundColor == null)) {
      final String bgColor = element.getBgColor();
      if ((bgColor != null) && !"".equals(bgColor)) {
        final Color bgc = ColorFactory.getInstance().getColor(bgColor);
        if (binfo == null) {
          binfo = new BackgroundInfo();
        }
        binfo.backgroundColor = bgc;
      }
    }
    if ((binfo == null) || (binfo.backgroundImage == null)) {
      final String background = element.getAttribute("background");
      if ((background != null) && !"".equals(background)) {
        if (binfo == null) {
          binfo = new BackgroundInfo();
        }
        try {
          binfo.backgroundImage = this.document.getFullURL(background);
        } catch (final MalformedURLException mfe) {
          throw new IllegalArgumentException(mfe);
        }
      }
    }
    this.backgroundInfo = binfo;
    return binfo;
  }

  /* This is being handled by attribute to style mechanism, but keeping the method here for future reference, in case that mechanism is not complete
  public BorderInfo getBorderInfo() {
    BorderInfo binfo = this.borderInfo;
    if (binfo != INVALID_BORDER_INFO) {
      return binfo;
    }
    binfo = super.getBorderInfo();
    if (binfo == null
        || (binfo.topStyle == HtmlValues.BORDER_STYLE_NONE && binfo.bottomStyle == HtmlValues.BORDER_STYLE_NONE
            && binfo.leftStyle == HtmlValues.BORDER_STYLE_NONE && binfo.rightStyle == HtmlValues.BORDER_STYLE_NONE)) {
      if (binfo == null) {
        binfo = new BorderInfo();
      }
      final HTMLElementImpl element = this.element;
      if (element != null) {
        String border = element.getAttribute("border");
        if (border != null) {
          border = border.trim();
          int value;
          int valueType;
          if (border.endsWith("%")) {
            valueType = HtmlInsets.TYPE_PERCENT;
            try {
              value = Integer.parseInt(border.substring(0, border.length() - 1));
            } catch (final NumberFormatException nfe) {
              value = 0;
            }
          } else {
            valueType = HtmlInsets.TYPE_PIXELS;
            try {
              value = Integer.parseInt(border);
            } catch (final NumberFormatException nfe) {
              value = 0;
            }
          }
          final HtmlInsets borderInsets = new HtmlInsets();
          borderInsets.top = borderInsets.left = borderInsets.right = borderInsets.bottom = value;
          borderInsets.topType = borderInsets.leftType = borderInsets.rightType = borderInsets.bottomType = valueType;
          binfo.insets = borderInsets;
          if (binfo.topColor == null) {
            binfo.topColor = Color.LIGHT_GRAY;
          }
          if (binfo.leftColor == null) {
            binfo.leftColor = Color.LIGHT_GRAY;
          }
          if (binfo.rightColor == null) {
            binfo.rightColor = Color.GRAY;
          }
          if (binfo.bottomColor == null) {
            binfo.bottomColor = Color.GRAY;
          }
          if (value != 0) {
            binfo.topStyle = binfo.leftStyle = binfo.rightStyle = binfo.bottomStyle = HtmlValues.BORDER_STYLE_SOLID;
          }
        }
      }
    }
    this.borderInfo = binfo;
    return binfo;
  }*/

}
