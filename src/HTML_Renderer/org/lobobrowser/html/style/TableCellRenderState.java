package org.lobobrowser.html.style;

import java.awt.Color;
import java.net.MalformedURLException;

import org.lobobrowser.html.domimpl.HTMLElementImpl;
import org.lobobrowser.html.domimpl.HTMLTableCellElementImpl;
import org.lobobrowser.html.domimpl.HTMLTableRowElementImpl;
import org.lobobrowser.util.gui.ColorFactory;
import org.w3c.dom.css.CSS2Properties;
import org.w3c.dom.html.HTMLElement;
import org.w3c.dom.html.HTMLTableElement;

public class TableCellRenderState extends StyleSheetRenderState {
  public TableCellRenderState(final RenderState prevRenderState, final HTMLElementImpl element) {
    super(prevRenderState, element);
  }

  private int alignXPercent = -1;
  private int alignYPercent = -1;
  private BackgroundInfo backgroundInfo = INVALID_BACKGROUND_INFO;

  @Override
  public void invalidate() {
    super.invalidate();
    this.alignXPercent = -1;
    this.alignYPercent = -1;
    this.backgroundInfo = INVALID_BACKGROUND_INFO;
    this.paddingInsets = INVALID_INSETS;
  }

  @Override
  public int getAlignXPercent() {
    int axp = this.alignXPercent;
    if (axp != -1) {
      return axp;
    }
    final CSS2Properties props = this.getCssProperties();
    if (props != null) {
      final String textAlign = props.getTextAlign();
      if ((textAlign != null) && (textAlign.length() != 0)) {
        return super.getAlignXPercent();
      }
    }
    // Parent already knows about "align" attribute, but override because of TH.
    String align = this.element.getAttribute("align");
    final HTMLElement element = this.element;
    HTMLElement rowElement = null;
    final Object parent = element.getParentNode();
    if (parent instanceof HTMLElement) {
      rowElement = (HTMLElement) parent;
    }
    if ((align == null) || (align.length() == 0)) {
      if (rowElement != null) {
        align = rowElement.getAttribute("align");
        if ((align != null) && (align.length() == 0)) {
          align = null;
        }
      } else {
        align = null;
      }
    }
    if (align == null) {
      if ("TH".equalsIgnoreCase(element.getNodeName())) {
        axp = 50;
      } else {
        axp = 0;
      }
    } else if ("center".equalsIgnoreCase(align) || "middle".equalsIgnoreCase(align)) {
      axp = 50;
    } else if ("left".equalsIgnoreCase(align)) {
      axp = 0;
    } else if ("right".equalsIgnoreCase(align)) {
      axp = 100;
    } else {
      // TODO: justify, etc.
      axp = 0;
    }
    this.alignXPercent = axp;
    return axp;
  }

  @Override
  public int getAlignYPercent() {
    int ayp = this.alignYPercent;
    if (ayp != -1) {
      return ayp;
    }
    final CSS2Properties props = this.getCssProperties();
    if (props != null) {
      final String textAlign = props.getVerticalAlign();
      if ((textAlign != null) && (textAlign.length() != 0)) {
        return super.getAlignYPercent();
      }
    }
    String valign = this.element.getAttribute("valign");
    final HTMLElement element = this.element;
    HTMLElement rowElement = null;
    final Object parent = element.getParentNode();
    if (parent instanceof HTMLElement) {
      rowElement = (HTMLElement) parent;
    }
    if ((valign == null) || (valign.length() == 0)) {
      if (rowElement != null) {
        valign = rowElement.getAttribute("valign");
        if ((valign != null) && (valign.length() == 0)) {
          valign = null;
        }
      } else {
        valign = null;
      }
    }
    if (valign == null) {
      ayp = 50;
    } else if ("top".equalsIgnoreCase(valign)) {
      ayp = 0;
    } else if ("middle".equalsIgnoreCase(valign) || "center".equalsIgnoreCase(valign)) {
      ayp = 50;
    } else if ("bottom".equalsIgnoreCase(valign)) {
      ayp = 100;
    } else {
      // TODO: baseline, etc.
      ayp = 50;
    }
    this.alignYPercent = ayp;
    return ayp;
  }

  @Override
  public BackgroundInfo getBackgroundInfo() {
    BackgroundInfo binfo = this.backgroundInfo;
    if (binfo != INVALID_BACKGROUND_INFO) {
      return binfo;
    }
    // Apply style based on deprecated attributes.
    binfo = super.getBackgroundInfo();
    final HTMLTableCellElementImpl element = (HTMLTableCellElementImpl) this.element;
    HTMLTableRowElementImpl rowElement = null;
    final Object parentNode = element.getParentNode();
    if (parentNode instanceof HTMLTableRowElementImpl) {
      rowElement = (HTMLTableRowElementImpl) parentNode;
    }
    if ((binfo == null) || (binfo.backgroundColor == null)) {
      String bgColor = element.getBgColor();
      if ((bgColor == null) || "".equals(bgColor)) {
        if (rowElement != null) {
          bgColor = rowElement.getBgColor();
        }
      }
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

  private HTMLTableElement getTableElement() {
    org.w3c.dom.Node ancestor = this.element.getParentNode();
    while ((ancestor != null) && !(ancestor instanceof HTMLTableElement)) {
      ancestor = ancestor.getParentNode();
    }
    return (HTMLTableElement) ancestor;
  }

  @Override
  public HtmlInsets getPaddingInsets() {
    HtmlInsets insets = this.paddingInsets;
    if (insets != INVALID_INSETS) {
      return insets;
    }
    insets = super.getPaddingInsets();
    if (insets == null) {
      final HTMLTableElement tableElement = this.getTableElement();
      if (tableElement == null) {
        // Return without caching
        return null;
      }
      String cellPaddingText = tableElement.getAttribute("cellpadding");
      if ((cellPaddingText != null) && (cellPaddingText.length() != 0)) {
        cellPaddingText = cellPaddingText.trim();
        int cellPadding;
        int cellPaddingType;
        if (cellPaddingText.endsWith("%")) {
          cellPaddingType = HtmlInsets.TYPE_PERCENT;
          try {
            cellPadding = Integer.parseInt(cellPaddingText.substring(0, cellPaddingText.length() - 1));
          } catch (final NumberFormatException nfe) {
            cellPadding = 0;
          }
        } else {
          cellPaddingType = HtmlInsets.TYPE_PIXELS;
          try {
            cellPadding = Integer.parseInt(cellPaddingText);
          } catch (final NumberFormatException nfe) {
            cellPadding = 0;
          }
        }
        insets = new HtmlInsets();
        insets.top = insets.left = insets.right = insets.bottom = cellPadding;
        insets.topType = insets.leftType = insets.rightType = insets.bottomType = cellPaddingType;
      }
    }
    this.paddingInsets = insets;
    return insets;
  }

  @Override
  public int getWhiteSpace() {
    // Overrides super.
    if (RenderThreadState.getState().overrideNoWrap) {
      return WS_NOWRAP;
    }
    final Integer ws = this.iWhiteSpace;
    if (ws != null) {
      return ws.intValue();
    }
    final JStyleProperties props = this.getCssProperties();
    final String whiteSpaceText = props == null ? null : props.getWhiteSpace();
    int wsValue;
    if (whiteSpaceText == null) {
      final HTMLElementImpl element = this.element;
      if ((element != null) && element.getAttributeAsBoolean("nowrap")) {
        wsValue = WS_NOWRAP;
      } else {
        final RenderState prs = this.prevRenderState;
        if (prs != null) {
          wsValue = prs.getWhiteSpace();
        } else {
          wsValue = WS_NORMAL;
        }
      }
    } else {
      final String whiteSpaceTextTL = whiteSpaceText.toLowerCase();
      if ("nowrap".equals(whiteSpaceTextTL)) {
        wsValue = WS_NOWRAP;
      } else if ("pre".equals(whiteSpaceTextTL)) {
        wsValue = WS_PRE;
      } else {
        wsValue = WS_NORMAL;
      }
    }
    if (wsValue == WS_NOWRAP) {
      // In table cells, if the width is defined as an absolute value,
      // nowrap has no effect (IE and FireFox behavior).
      final HTMLElementImpl element = this.element;
      String width = props == null ? null : props.getWidth();
      if (width == null) {
        width = element.getAttribute("width");
        if ((width != null) && (width.length() > 0) && !width.endsWith("%")) {
          wsValue = WS_NORMAL;
        }
      } else {
        if (!width.trim().endsWith("%")) {
          wsValue = WS_NORMAL;
        }
      }
    }
    this.iWhiteSpace = new Integer(wsValue);
    return wsValue;
  }

}
