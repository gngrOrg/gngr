/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2014 Uproot Labs India Pvt Ltd

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

 */

package org.lobobrowser.html.style;

import java.net.MalformedURLException;
import java.net.URL;

import org.lobobrowser.js.AbstractScriptableDelegate;
import org.lobobrowser.js.HideFromJS;
import org.lobobrowser.util.Urls;
import org.w3c.dom.css.CSS2Properties;

import cz.vutbr.web.css.CSSProperty;
import cz.vutbr.web.css.NodeData;
import cz.vutbr.web.css.Term;
import cz.vutbr.web.csskit.TermURIImpl;

abstract public class JStyleProperties extends AbstractScriptableDelegate implements CSS2Properties {
  private String overlayColor;
  final private CSS2PropertiesContext context;
  // TODO: this flag can be removed when the layout can handle empty strings
  // currently there is only a check for null and not for empty string
  final protected boolean nullIfAbsent;

  public JStyleProperties(final CSS2PropertiesContext context, final boolean nullIfAbsent) {
    this.context = context;
    this.nullIfAbsent = nullIfAbsent;
  }

  //TODO All the methods that are not implemented need more detailed understanding.
  // most of them are short hand properties and they need to be constructed from the long
  // forms of the respective properties
  public String getAzimuth() {
    return helperTryBoth("azimuth");
  }

  public String getBackground() {
    // TODO need to implement this method. GH #143
    return "";
  }

  public String getBackgroundAttachment() {
    return helperGetProperty("background-attachment");
  }

  public String getBackgroundColor() {
    return helperTryBoth("background-color");
  }

  public String getBackgroundImage() {
    // TODO
    // need to check if upstream can provide the absolute url of
    //  the image so that it can directly be passed.
    String quotedUri = null;
    final TermURIImpl t = (TermURIImpl) getNodeData().getValue("background-image", false);
    if (t != null) {
      URL finalUrl = null;
      try {
        finalUrl = Urls.createURL(t.getBase(), t.getValue());
      } catch (final MalformedURLException e) {
        e.printStackTrace();
      }
      quotedUri = finalUrl == null ? null : finalUrl.toString();
    }
    return quotedUri == null ? null : "url(" + quotedUri + ")";
  }

  public String getBackgroundPosition() {
    return helperGetValue("background-position");
  }

  public String getBackgroundRepeat() {
    return helperGetProperty("background-repeat");
  }

  public String getBorder() {
    // TODO need to implement this method
    throw new UnsupportedOperationException();
  }

  public String getBorderCollapse() {
    return helperGetProperty("border-collapse");
  }

  public String getBorderColor() {
    // TODO need to implement this method
    throw new UnsupportedOperationException();
  }

  public String getBorderSpacing() {
    return helperGetValue("border-spacing");
  }

  public String getBorderStyle() {
    // TODO need to implement this method
    throw new UnsupportedOperationException();
  }

  public String getBorderTop() {
    // TODO need to implement this method
    throw new UnsupportedOperationException();
  }

  public String getBorderRight() {
    // TODO need to implement this method
    throw new UnsupportedOperationException();
  }

  public String getBorderBottom() {
    // TODO need to implement this method
    throw new UnsupportedOperationException();
  }

  public String getBorderLeft() {
    // TODO need to implement this method
    throw new UnsupportedOperationException();
  }

  public String getBorderTopColor() {
    return helperTryBoth("border-top-color");
  }

  public String getBorderRightColor() {
    return helperTryBoth("border-right-color");
  }

  public String getBorderBottomColor() {
    return helperTryBoth("border-bottom-color");
  }

  public String getBorderLeftColor() {
    return helperTryBoth("border-left-color");
  }

  public String getBorderTopStyle() {
    return helperGetProperty("border-top-style");
  }

  public String getBorderRightStyle() {
    return helperGetProperty("border-right-style");
  }

  public String getBorderBottomStyle() {
    return helperGetProperty("border-bottom-style");
  }

  public String getBorderLeftStyle() {
    return helperGetProperty("border-left-style");
  }

  public String getBorderTopWidth() {
    final String width = helperTryBoth("border-top-width");
    // TODO
    // temp hack to support border thin/medium/thick
    // need to implement it at the place where it is actually being processed
    return border2Pixel(width);
  }

  public String getBorderRightWidth() {
    final String width = helperTryBoth("border-right-width");
    // TODO
    // temp hack to support border thin/medium/thick
    // need to implement it at the place where it is actually being processed
    return border2Pixel(width);
  }

  public String getBorderBottomWidth() {
    final String width = helperTryBoth("border-bottom-width");
    // TODO
    // temp hack to support border thin/medium/thick
    // need to implement it at the place where it is actually being processed
    return border2Pixel(width);
  }

  public String getBorderLeftWidth() {
    final String width = helperTryBoth("border-left-width");
    // TODO
    // temp hack to support border thin/medium/thick
    // need to implement it at the place where it is actually being processed
    return border2Pixel(width);
  }

  // TODO
  // temp hack to support border thin/medium/thick
  // this method should be removed once it is implemented where border is actually processed
  private static String border2Pixel(final String width) {
    if (width != null) {
      if ("thin".equalsIgnoreCase(width)) {
        return HtmlValues.BORDER_THIN_SIZE;
      }
      if ("medium".equalsIgnoreCase(width)) {
        return HtmlValues.BORDER_MEDIUM_SIZE;
      }
      if ("thick".equalsIgnoreCase(width)) {
        return HtmlValues.BORDER_THICK_SIZE;
      }
    }
    return width;
  }

  public String getBorderWidth() {
    // TODO need to implement this method
    throw new UnsupportedOperationException();
  }

  public String getBottom() {
    return helperTryBoth("bottom");
  }

  public String getCaptionSide() {
    return helperGetProperty("caption-side");
  }

  public String getClear() {
    return helperGetProperty("clear");
  }

  public String getClip() {
    return helperTryBoth("clip");
  }

  public String getColor() {
    return helperTryBoth("color");
  }

  public String getContent() {
    return helperTryBoth("content");
  }

  public String getCounterIncrement() {
    return helperTryBoth("couter-increment");
  }

  public String getCounterReset() {
    return helperTryBoth("couter-reset");
  }

  public String getCue() {
    // TODO need to implement this method
    throw new UnsupportedOperationException();
  }

  public String getCueAfter() {
    return helperTryBoth("cue-after");
  }

  public String getCueBefore() {
    return helperTryBoth("cue-before");
  }

  public String getCursor() {
    return helperGetProperty("cursor");
  }

  public String getDirection() {
    return helperGetProperty("direction");
  }

  public String getDisplay() {
    return helperGetProperty("display");
  }

  public String getElevation() {
    return helperTryBoth("elevation");
  }

  public String getEmptyCells() {
    return helperGetProperty("empty-cells");
  }

  public String getCssFloat() {
    return this.getFloat();
  }

  public String getFont() {
    // TODO need to implement this method
    throw new UnsupportedOperationException();
  }

  public String getFontFamily() {
    return helperTryBoth("font-family");
  }

  public String getFontSize() {
    return helperTryBoth("font-size");
  }

  public String getFontSizeAdjust() {
    return helperTryBoth("font-adjust");
  }

  public String getFontStretch() {
    return helperGetProperty("font-stretch");
  }

  public String getFontStyle() {
    return helperGetProperty("font-style");
  }

  public String getFontVariant() {
    return helperGetProperty("font-variant");
  }

  public String getFontWeight() {
    return helperGetProperty("font-weight");
  }

  public String getHeight() {
    return helperGetValue("height");
  }

  public String getLeft() {
    return helperTryBoth("left");
  }

  public String getLetterSpacing() {
    return helperTryBoth("letter-spacing");
  }

  public String getLineHeight() {
    return helperTryBoth("line-height");
  }

  public String getListStyle() {
    final String listStyleType = getListStyleType();
    final String listStylePosition = getListStylePosition();
    final StringBuilder listStyle = new StringBuilder();

    if ((listStyleType != null) && !("null".equals(listStyleType))) {
      listStyle.append(listStyleType);
    }

    if ((listStylePosition != null) && !("null".equals(listStylePosition))) {
      listStyle.append(" " + listStylePosition);
    }

    final String listStyleText = listStyle.toString().trim();
    return listStyleText.length() == 0 ? null : listStyleText;
  }

  public String getListStyleImage() {
    return helperTryBoth("list-style-image");
  }

  public String getListStylePosition() {
    return helperGetProperty("list-style-position");
  }

  public String getListStyleType() {
    return helperGetProperty("list-style-type");
  }

  public String getMargin() {
    // TODO need to implement this method
    throw new UnsupportedOperationException();
  }

  public String getMarginTop() {
    return helperTryBoth("margin-top");
  }

  public String getMarginRight() {
    return helperTryBoth("margin-right");
  }

  public String getMarginBottom() {
    return helperTryBoth("margin-bottom");
  }

  public String getMarginLeft() {
    return helperTryBoth("margin-left");
  }

  public String getMarkerOffset() {
    return helperTryBoth("marker-offset");
  }

  public String getMarks() {
    return helperGetProperty("marks");
  }

  public String getMaxHeight() {
    return helperTryBoth("max-height");
  }

  public String getMaxWidth() {
    return helperTryBoth("max-width");
  }

  public String getMinHeight() {
    return helperTryBoth("min-height");
  }

  public String getMinWidth() {
    return helperTryBoth("min-width");
  }

  public String getOrphans() {
    return helperGetValue("orphans");
  }

  public String getOutline() {
    // TODO need to implement this method
    throw new UnsupportedOperationException();
  }

  public String getOutlineColor() {
    return helperTryBoth("outline-color");
  }

  public String getOutlineStyle() {
    return helperGetProperty("outline-style");
  }

  //TODO add support for thick/think/medium
  public String getOutlineWidth() {
    final String width = helperTryBoth("outline-border");
    return border2Pixel(width);
  }

  public String getOverflow() {
    return helperGetProperty("overflow");
  }

  public String getPadding() {
    // TODO need to implement this method
    throw new UnsupportedOperationException();
  }

  public String getPaddingTop() {
    return helperGetValue("padding-top");
  }

  public String getPaddingRight() {
    return helperGetValue("padding-right");
  }

  public String getPaddingBottom() {
    return helperGetValue("padding-bottom");
  }

  public String getPaddingLeft() {
    return helperGetValue("padding-left");
  }

  public String getPage() {
    // TODO need to implement this method
    throw new UnsupportedOperationException();
  }

  public String getPageBreakAfter() {
    return helperGetProperty("page-break-after");
  }

  public String getPageBreakBefore() {
    return helperGetProperty("page-break-before");
  }

  public String getPageBreakInside() {
    return helperGetProperty("page-break-inside");
  }

  public String getPause() {
    return helperGetValue("pause");
  }

  public String getPauseAfter() {
    return helperGetValue("pause-after");
  }

  public String getPauseBefore() {
    return helperGetValue("pause-before");
  }

  public String getPitch() {
    return helperTryBoth("pitch");
  }

  public String getPitchRange() {
    return helperGetValue("pitchRange");
  }

  public String getPlayDuring() {
    return helperTryBoth("play-during");
  }

  public String getPosition() {
    return helperGetProperty("position");
  }

  public String getQuotes() {
    return helperTryBoth("quotes");
  }

  public String getRichness() {
    return helperGetValue("richness");
  }

  public String getRight() {
    return helperTryBoth("right");
  }

  public String getSize() {
    // TODO need to implement this method
    throw new UnsupportedOperationException();
  }

  public String getSpeak() {
    return helperGetProperty("speak");
  }

  public String getSpeakHeader() {
    return helperGetProperty("speak-header");
  }

  public String getSpeakNumeral() {
    return helperGetProperty("speak-numeral");
  }

  public String getSpeakPunctuation() {
    return helperGetProperty("speak-punctuation");
  }

  public String getSpeechRate() {
    return helperTryBoth("speech-rate");
  }

  public String getStress() {
    return helperGetValue("stress");
  }

  public String getTableLayout() {
    return helperGetProperty("table-layout");
  }

  public String getTextAlign() {
    return helperGetProperty("text-align");
  }

  public String getTextDecoration() {
    return helperTryBoth("text-decoration");
  }

  public String getTextIndent() {
    return helperGetValue("text-indent");
  }

  public String getTextShadow() {
    // TODO need to implement this method
    throw new UnsupportedOperationException();
  }

  public String getTextTransform() {
    return helperGetProperty("text-transform");
  }

  public String getTop() {
    return helperTryBoth("top");
  }

  public String getUnicodeBidi() {
    return helperGetProperty("unicode-bidi");
  }

  public String getVerticalAlign() {
    return helperGetProperty("vertical-align");
  }

  public String getVisibility() {
    return helperGetProperty("visibility");
  }

  public String getVoiceFamily() {
    return helperTryBoth("voice-family");
  }

  public String getVolume() {
    return helperTryBoth("volume");
  }

  public String getWhiteSpace() {
    return helperGetProperty("white-space");
  }

  public String getWidows() {
    return helperGetValue("widows");
  }

  public String getWidth() {
    return helperGetValue("width");
  }

  public String getWordSpacing() {
    return helperTryBoth("word-spacing");
  }

  public String getZIndex() {
    // TODO
    // refer to issue #77
    // According to the specs ZIndex value has to be integer but
    // jStyle Parser returns an float.
    // until then this is just a temp hack.
    final String zIndex = helperGetValue("z-index");
    float fZIndex = 0.0f;
    if (zIndex != null) {
      try {
        fZIndex = Float.parseFloat(zIndex);
      } catch (final NumberFormatException err) {
        err.printStackTrace();
      }
    }
    final int iZIndex = (int) fZIndex;
    return "" + iZIndex;
  }

  public String getOverlayColor() {
    return this.overlayColor;
  }

  public void setOverlayColor(final String value) {
    this.overlayColor = value;
    this.context.informLookInvalid();
  }

  // TODO references to this in internal code can use a more specific method.
  //      (we can implement specific methods like we have for other properties)
  @HideFromJS
  public String getPropertyValueInternal(final String string) {
    return helperGetProperty(string);
  }

  public String getPropertyValue(final String string) {
    return helperTryBoth(string);
  }

  public String getFloat() {
    return helperGetProperty("float");
  }

  abstract protected NodeData getNodeData();

  private String helperGetValue(final String propertyName) {
    final NodeData nodeData = getNodeData();
    if (nodeData != null) {
      final Term<?> value = nodeData.getValue(propertyName, true);
      // The trim() is a temporary work around for #154
      return value == null ? null : value.toString().trim();
    } else {
      return nullIfAbsent ? null : "";
    }
  }

  private String helperGetProperty(final String propertyName) {
    final NodeData nodeData = getNodeData();
    if (nodeData != null) {
      final CSSProperty property = nodeData.getProperty(propertyName, true);
      // final CSSProperty property = nodeData.getProperty(propertyName);
      return property == null ? null : property.toString();
    } else {
      return nullIfAbsent ? null : "";
    }
  }

  @HideFromJS
  public String helperTryBoth(final String propertyName) {
    // These two implementations were deprecated after the changes in https://github.com/radkovo/jStyleParser/issues/50

    /* Original
    final String value = helperGetValue(propertyName);
    return value == null ? helperGetProperty(propertyName) : value;
    */

    /* Corrected (equivalent to below implementation, but less optimal)
    final String property = helperGetProperty(propertyName);
    return property == null || property.isEmpty() ? helperGetValue(propertyName) : property;
    */

    final NodeData nodeData = getNodeData();
    if (nodeData == null) {
      return null;
    }
    return nodeData.getAsString(propertyName, true);
  }
}
