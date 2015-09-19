package org.lobobrowser.html.style;

import java.util.ArrayList;
import java.util.List;

import org.lobobrowser.html.domimpl.HTMLElementImpl;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

import cz.vutbr.web.css.NodeData;
import cz.vutbr.web.css.StyleSheet;
import cz.vutbr.web.domassign.DirectAnalyzer;

public final class LocalJStyleProperties extends JStyleProperties {

  private final HTMLElementImpl element;

  public LocalJStyleProperties(final HTMLElementImpl element) {
    super(element, false);
    this.element = element;
  }

  @Override
  public void setAzimuth(final String azimuth) throws DOMException {
    updateInlineStyle("azimuth", azimuth);
  }

  @Override
  public void setBackground(final String background) throws DOMException {
    updateInlineStyle("background", background);
  }

  @Override
  public void setBackgroundAttachment(final String backgroundAttachment) throws DOMException {
    updateInlineStyle("background-attachment", backgroundAttachment);
  }

  @Override
  public void setBackgroundColor(final String backgroundColor) throws DOMException {
    updateInlineStyle("background-color", backgroundColor);
  }

  @Override
  public void setBackgroundImage(final String backgroundImage) throws DOMException {
    updateInlineStyle("background-image", backgroundImage);
  }

  @Override
  public void setBackgroundPosition(final String backgroundPosition) throws DOMException {
    updateInlineStyle("background-position", backgroundPosition);
  }

  @Override
  public void setBackgroundRepeat(final String backgroundRepeat) throws DOMException {
    updateInlineStyle("background-repeat", backgroundRepeat);
  }

  @Override
  public void setBorder(final String border) throws DOMException {
    updateInlineStyle("border", border);
  }

  @Override
  public void setBorderCollapse(final String borderCollapse) throws DOMException {
    updateInlineStyle("border-collapse", borderCollapse);

  }

  @Override
  public void setBorderColor(final String borderColor) throws DOMException {
    updateInlineStyle("border-color", borderColor);
  }

  @Override
  public void setBorderSpacing(final String borderSpacing) throws DOMException {
    updateInlineStyle("border-spacing", borderSpacing);
  }

  @Override
  public void setBorderStyle(final String borderStyle) throws DOMException {
    updateInlineStyle("border-style", borderStyle);
  }

  @Override
  public void setBorderTop(final String borderTop) throws DOMException {
    updateInlineStyle("border-top", borderTop);
  }

  @Override
  public void setBorderRight(final String borderRight) throws DOMException {
    updateInlineStyle("border-right", borderRight);
  }

  @Override
  public void setBorderBottom(final String borderBottom) throws DOMException {
    updateInlineStyle("border-bottom", borderBottom);
  }

  @Override
  public void setBorderLeft(final String borderLeft) throws DOMException {
    updateInlineStyle("border-left", borderLeft);
  }

  @Override
  public void setBorderTopColor(final String borderTopColor) throws DOMException {
    updateInlineStyle("border-top-color", borderTopColor);
  }

  @Override
  public void setBorderRightColor(final String borderRightColor) throws DOMException {
    updateInlineStyle("border-right-color", borderRightColor);
  }

  @Override
  public void setBorderBottomColor(final String borderBottomColor) throws DOMException {
    updateInlineStyle("border-bottom-color", borderBottomColor);
  }

  @Override
  public void setBorderLeftColor(final String borderLeftColor) throws DOMException {
    updateInlineStyle("border-left-color", borderLeftColor);
  }

  @Override
  public void setBorderTopStyle(final String borderTopStyle) throws DOMException {
    updateInlineStyle("border-top-style", borderTopStyle);
  }

  @Override
  public void setBorderRightStyle(final String borderRightStyle) throws DOMException {
    updateInlineStyle("border-right-style", borderRightStyle);
  }

  @Override
  public void setBorderBottomStyle(final String borderBottomStyle) throws DOMException {
    updateInlineStyle("border-bottom-style", borderBottomStyle);
  }

  @Override
  public void setBorderLeftStyle(final String borderLeftStyle) throws DOMException {
    updateInlineStyle("border-left-style", borderLeftStyle);
  }

  @Override
  public void setBorderTopWidth(final String borderTopWidth) throws DOMException {
    updateInlineStyle("border-top-width", borderTopWidth);
  }

  @Override
  public void setBorderRightWidth(final String borderRightWidth) throws DOMException {
    updateInlineStyle("border-right-width", borderRightWidth);
  }

  @Override
  public void setBorderBottomWidth(final String borderBottomWidth) throws DOMException {
    updateInlineStyle("border-bottom-width", borderBottomWidth);
  }

  @Override
  public void setBorderLeftWidth(final String borderLeftWidth) throws DOMException {
    updateInlineStyle("border-left-width", borderLeftWidth);
  }

  @Override
  public void setBorderWidth(final String borderWidth) throws DOMException {
    updateInlineStyle("border-width", borderWidth);
  }

  @Override
  public void setBottom(final String bottom) throws DOMException {
    updateInlineStyle("bottom", bottom);
  }

  @Override
  public void setCaptionSide(final String captionSide) throws DOMException {
    updateInlineStyle("caption-side", captionSide);
  }

  @Override
  public void setClear(final String clear) throws DOMException {
    updateInlineStyle("clear", clear);
  }

  @Override
  public void setClip(final String clip) throws DOMException {
    updateInlineStyle("clip", clip);
  }

  @Override
  public void setColor(final String color) throws DOMException {
    updateInlineStyle("color", color);
  }

  @Override
  public void setContent(final String content) throws DOMException {
    updateInlineStyle("content", content);
  }

  @Override
  public void setCounterIncrement(final String counterIncrement) throws DOMException {
    updateInlineStyle("counter-increment", counterIncrement);
  }

  @Override
  public void setCounterReset(final String counterReset) throws DOMException {
    updateInlineStyle("counter-reset", counterReset);
  }

  @Override
  public void setCue(final String cue) throws DOMException {
    updateInlineStyle("cue", cue);
  }

  @Override
  public void setCueAfter(final String cueAfter) throws DOMException {
    updateInlineStyle("cue-after", cueAfter);
  }

  @Override
  public void setCueBefore(final String cueBefore) throws DOMException {
    updateInlineStyle("cue-before", cueBefore);
  }

  @Override
  public void setCursor(final String cursor) throws DOMException {
    updateInlineStyle("cursor", cursor);
  }

  @Override
  public void setDirection(final String direction) throws DOMException {
    updateInlineStyle("direction", direction);
  }

  @Override
  public void setDisplay(final String display) throws DOMException {
    updateInlineStyle("display", display);
  }

  @Override
  public void setElevation(final String elevation) throws DOMException {
    updateInlineStyle("elevation", elevation);
  }

  @Override
  public void setEmptyCells(final String emptyCells) throws DOMException {
    updateInlineStyle("empty-cells", emptyCells);
  }

  @Override
  public void setCssFloat(final String cssFloat) throws DOMException {
    updateInlineStyle("css-float", cssFloat);
  }

  @Override
  public void setFont(final String font) throws DOMException {
    updateInlineStyle("font", font);
  }

  @Override
  public void setFontFamily(final String fontFamily) throws DOMException {
    updateInlineStyle("font-family", fontFamily);
  }

  @Override
  public void setFontSize(final String fontSize) throws DOMException {
    updateInlineStyle("font-size", fontSize);
  }

  @Override
  public void setFontSizeAdjust(final String fontSizeAdjust) throws DOMException {
    updateInlineStyle("font-size-adjust", fontSizeAdjust);
  }

  @Override
  public void setFontStretch(final String fontStretch) throws DOMException {
    updateInlineStyle("font-stretch", fontStretch);
  }

  @Override
  public void setFontStyle(final String fontStyle) throws DOMException {
    updateInlineStyle("font-style", fontStyle);
  }

  @Override
  public void setFontVariant(final String fontVariant) throws DOMException {
    updateInlineStyle("font-Variant", fontVariant);
  }

  @Override
  public void setFontWeight(final String fontWeight) throws DOMException {
    updateInlineStyle("font-weight", fontWeight);
  }

  @Override
  public void setHeight(final String height) throws DOMException {
    updateInlineStyle("height", height);
  }

  @Override
  public void setLeft(final String left) throws DOMException {
    updateInlineStyle("left", left);
  }

  @Override
  public void setLetterSpacing(final String letterSpacing) throws DOMException {
    updateInlineStyle("letter-spacing", letterSpacing);
  }

  @Override
  public void setLineHeight(final String lineHeight) throws DOMException {
    updateInlineStyle("line-height", lineHeight);
  }

  @Override
  public void setListStyle(final String listStyle) throws DOMException {
    updateInlineStyle("list-Style", listStyle);
  }

  @Override
  public void setListStyleImage(final String listStyleImage) throws DOMException {
    updateInlineStyle("list-style-image", listStyleImage);
  }

  @Override
  public void setListStylePosition(final String listStylePosition) throws DOMException {
    updateInlineStyle("list-style-position", listStylePosition);
  }

  @Override
  public void setListStyleType(final String listStyleType) throws DOMException {
    updateInlineStyle("list-style-type", listStyleType);
  }

  @Override
  public void setMargin(final String margin) throws DOMException {
    updateInlineStyle("margin", margin);
  }

  @Override
  public void setMarginTop(final String marginTop) throws DOMException {
    updateInlineStyle("margin-top", marginTop);
  }

  @Override
  public void setMarginRight(final String marginRight) throws DOMException {
    updateInlineStyle("margin-right", marginRight);
  }

  @Override
  public void setMarginBottom(final String marginBottom) throws DOMException {
    updateInlineStyle("margin-bottom", marginBottom);
  }

  @Override
  public void setMarginLeft(final String marginLeft) throws DOMException {
    updateInlineStyle("margin-left", marginLeft);
  }

  @Override
  public void setMarkerOffset(final String markerOffset) throws DOMException {
    updateInlineStyle("marker-offset", markerOffset);
  }

  @Override
  public void setMarks(final String marks) throws DOMException {
    updateInlineStyle("marks", marks);
  }

  @Override
  public void setMaxHeight(final String maxHeight) throws DOMException {
    updateInlineStyle("max-height", maxHeight);
  }

  @Override
  public void setMaxWidth(final String maxWidth) throws DOMException {
    updateInlineStyle("max-width", maxWidth);
  }

  @Override
  public void setMinHeight(final String minHeight) throws DOMException {
    updateInlineStyle("min-height", minHeight);
  }

  @Override
  public void setMinWidth(final String minWidth) throws DOMException {
    updateInlineStyle("min-width", minWidth);
  }

  @Override
  public void setOrphans(final String orphans) throws DOMException {
    updateInlineStyle("orphans", orphans);
  }

  @Override
  public void setOutline(final String outline) throws DOMException {
    updateInlineStyle("outline", outline);
  }

  @Override
  public void setOutlineColor(final String outlineColor) throws DOMException {
    updateInlineStyle("outline-color", outlineColor);
  }

  @Override
  public void setOutlineStyle(final String outlineStyle) throws DOMException {
    updateInlineStyle("outline-style", outlineStyle);
  }

  @Override
  public void setOutlineWidth(final String outlineWidth) throws DOMException {
    updateInlineStyle("outline-width", outlineWidth);
  }

  @Override
  public void setOverflow(final String overflow) throws DOMException {
    updateInlineStyle("overflow", overflow);
  }

  @Override
  public void setPadding(final String padding) throws DOMException {
    updateInlineStyle("padding", padding);
  }

  @Override
  public void setPaddingTop(final String paddingTop) throws DOMException {
    updateInlineStyle("padding-top", paddingTop);
  }

  @Override
  public void setPaddingRight(final String paddingRight) throws DOMException {
    updateInlineStyle("padding-right", paddingRight);
  }

  @Override
  public void setPaddingBottom(final String paddingBottom) throws DOMException {
    updateInlineStyle("padding-bottom", paddingBottom);
  }

  @Override
  public void setPaddingLeft(final String paddingLeft) throws DOMException {
    updateInlineStyle("padding-left", paddingLeft);
  }

  @Override
  public void setPage(final String page) throws DOMException {
    updateInlineStyle("page", page);
  }

  @Override
  public void setPageBreakAfter(final String pageBreakAfter) throws DOMException {
    updateInlineStyle("page-break-after", pageBreakAfter);
  }

  @Override
  public void setPageBreakBefore(final String pageBreakBefore) throws DOMException {
    updateInlineStyle("page-break-before", pageBreakBefore);
  }

  @Override
  public void setPageBreakInside(final String pageBreakInside) throws DOMException {
    updateInlineStyle("page-break-inside", pageBreakInside);
  }

  @Override
  public void setPause(final String pause) throws DOMException {
    updateInlineStyle("pause", pause);
  }

  @Override
  public void setPauseAfter(final String pauseAfter) throws DOMException {
    updateInlineStyle("pause-after", pauseAfter);
  }

  @Override
  public void setPauseBefore(final String pauseBefore) throws DOMException {
    updateInlineStyle("pause-before", pauseBefore);
  }

  @Override
  public void setPitch(final String pitch) throws DOMException {
    updateInlineStyle("pitch", pitch);
  }

  @Override
  public void setPitchRange(final String pitchRange) throws DOMException {
    updateInlineStyle("pitch-range", pitchRange);
  }

  @Override
  public void setPlayDuring(final String playDuring) throws DOMException {
    updateInlineStyle("play-during", playDuring);
  }

  @Override
  public void setPosition(final String position) throws DOMException {
    updateInlineStyle("position", position);
  }

  @Override
  public void setQuotes(final String quotes) throws DOMException {
    updateInlineStyle("quotes", quotes);
  }

  @Override
  public void setRichness(final String richness) throws DOMException {
    updateInlineStyle("richness", richness);
  }

  @Override
  public void setRight(final String right) throws DOMException {
    updateInlineStyle("right", right);
  }

  @Override
  public void setSize(final String size) throws DOMException {
    updateInlineStyle("size", size);
  }

  @Override
  public void setSpeak(final String speak) throws DOMException {
    updateInlineStyle("speak", speak);
  }

  @Override
  public void setSpeakHeader(final String speakHeader) throws DOMException {
    updateInlineStyle("speak-header", speakHeader);
  }

  @Override
  public void setSpeakNumeral(final String speakNumeral) throws DOMException {
    updateInlineStyle("speak-numeral", speakNumeral);
  }

  @Override
  public void setSpeakPunctuation(final String speakPunctuation) throws DOMException {
    updateInlineStyle("speak-punctuation", speakPunctuation);
  }

  @Override
  public void setSpeechRate(final String speechRate) throws DOMException {
    updateInlineStyle("speech-rate", speechRate);
  }

  @Override
  public void setStress(final String stress) throws DOMException {
    updateInlineStyle("stress", stress);
  }

  @Override
  public void setTableLayout(final String tableLayout) throws DOMException {
    updateInlineStyle("table-layout", tableLayout);
  }

  @Override
  public void setTextAlign(final String textAlign) throws DOMException {
    updateInlineStyle("text-align", textAlign);
  }

  @Override
  public void setTextDecoration(final String textDecoration) throws DOMException {
    updateInlineStyle("text-decoration", textDecoration);
  }

  @Override
  public void setTextIndent(final String textIndent) throws DOMException {
    updateInlineStyle("text-indent", textIndent);
  }

  @Override
  public void setTextShadow(final String textShadow) throws DOMException {
    updateInlineStyle("text-shadow", textShadow);
  }

  @Override
  public void setTextTransform(final String textTransform) throws DOMException {
    updateInlineStyle("text-transform", textTransform);
  }

  @Override
  public void setTop(final String top) throws DOMException {
    updateInlineStyle("top", top);
  }

  @Override
  public void setUnicodeBidi(final String unicodeBidi) throws DOMException {
    updateInlineStyle("unicode-bidi", unicodeBidi);
  }

  @Override
  public void setVerticalAlign(final String verticalAlign) throws DOMException {
    updateInlineStyle("vertical-align", verticalAlign);
  }

  @Override
  public void setVisibility(final String visibility) throws DOMException {
    updateInlineStyle("visibility", visibility);
  }

  @Override
  public void setVoiceFamily(final String voiceFamily) throws DOMException {
    updateInlineStyle("voice-family", voiceFamily);
  }

  @Override
  public void setVolume(final String volume) throws DOMException {
    updateInlineStyle("volume", volume);
  }

  @Override
  public void setWhiteSpace(final String whiteSpace) throws DOMException {
    updateInlineStyle("white-space", whiteSpace);
  }

  @Override
  public void setWidows(final String widows) throws DOMException {
    updateInlineStyle("widows", widows);
  }

  @Override
  public void setWidth(final String width) throws DOMException {
    updateInlineStyle("width", width);
  }

  @Override
  public void setWordSpacing(final String wordSpacing) throws DOMException {
    updateInlineStyle("word-spacing", wordSpacing);
  }

  @Override
  public void setZIndex(final String zIndex) throws DOMException {
    updateInlineStyle("z-index", zIndex);
  }

  @Override
  protected NodeData getNodeData() {
    final HTMLElementImpl ele = this.element;
    final String inlineStyle = ele.getAttribute("style");
    if ((inlineStyle != null) && (inlineStyle.length() > 0)) {
      final List<StyleSheet> jSheets = new ArrayList<>();
      final StyleSheet jSheet = CSSUtilities.jParseInlineStyle(inlineStyle, null, ele, true);
      jSheets.add(jSheet);
      final DirectAnalyzer domAnalyser = new cz.vutbr.web.domassign.DirectAnalyzer(jSheets);
      return domAnalyser.getElementStyle(ele, null, "screen");
    }
    return null;
  }

  private void updateInlineStyle(final String propertyName, final String propertyValue) {
    final Element ele = this.element;
    if (ele != null) {
      final StringBuilder sb = new StringBuilder();
      final String inlineStyle = ele.getAttribute("style");
      if ((inlineStyle != null) && (inlineStyle.length() > 0)) {
        final String propertyNameLC = propertyName.toLowerCase();
        final String[] styleDeclarations = inlineStyle.split(";");
        for (final String styleDeclaration : styleDeclarations) {
          final String[] nameValue = styleDeclaration.split(":");
          if (nameValue.length == 2) {
            final String oldPropertyName = nameValue[0].toLowerCase().trim();
            if (!(oldPropertyName.equals(propertyNameLC))) {
              sb.append(styleDeclaration + ";");
            }
          }
        }
      }
      sb.append(propertyName + ":" + propertyValue + ";");
      ele.setAttribute("style", sb.toString());
    }
  }

}
