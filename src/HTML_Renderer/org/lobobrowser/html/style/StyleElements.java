package org.lobobrowser.html.style;

import java.util.Vector;

import org.lobobrowser.html.domimpl.HTMLElementImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cz.vutbr.web.css.StyleSheet;

/**
 * Borrowed from CSSBox HTMLNorm.java This class provides a mechanism of
 * converting some HTML presentation atributes to the CSS styles and other
 * methods related to HTML specifics.
 */
public class StyleElements {

  public static StyleSheet convertAttributesToStyles(final Node n) {
    if (n.getNodeType() == Node.ELEMENT_NODE) {
      final HTMLElementImpl el = (HTMLElementImpl) n;
      //Analyze HTML attributes
      String attrs = "";
      final String tagName = el.getTagName();
      if ("TABLE".equalsIgnoreCase(tagName)) {
        //setting table and cell borders
        attrs = getTableElementStyle(el, attrs);
      } else if ("FONT".equalsIgnoreCase(tagName)) {
        //Text properties
        attrs = getFontElementStyle(el, attrs);
      } else if ("CANVAS".equalsIgnoreCase(tagName)) {
        attrs = getCanvasElementStyle(el, attrs);
      } else if ("IMG".equalsIgnoreCase(tagName)) {
        attrs = getElementDimensionStyle(el, attrs);
      }

      if (attrs.length() > 0) {
        return CSSUtilities.jParseInlineStyle(attrs, null, el, false);
      }
    }
    return null;
  }

  private static String getCanvasElementStyle(HTMLElementImpl el, String attrs) {
    final Node widthNode = el.getAttributes().getNamedItem("width");
    if (widthNode != null) {
      attrs += "width: " + pixelise(widthNode.getNodeValue()) + ";";
    } else {
      attrs += "width: 300px;";
    }

    final Node heightNode = el.getAttributes().getNamedItem("height");
    if (heightNode != null) {
      attrs += "height: " + pixelise(heightNode.getNodeValue()) + ";";
    } else {
      attrs += "height: 150px;";
    }

    return attrs;
  }

  private static String getElementDimensionStyle(HTMLElementImpl el, String attrs) {
    final Node widthNode = el.getAttributes().getNamedItem("width");
    if (widthNode != null) {
      attrs += "width: " + pixelise(widthNode.getNodeValue()) + ";";
    }

    final Node heightNode = el.getAttributes().getNamedItem("height");
    if (heightNode != null) {
      attrs += "height: " + pixelise(heightNode.getNodeValue()) + ";";
    }

    return attrs;
  }

  private static String pixelise(final String value) {
    try {
      @SuppressWarnings("unused")
      final int ignored = Integer.parseInt(value);

      return value + "px";
    } catch (NumberFormatException e) {
      return value;
    }
  }

  private static String getTableElementStyle(final Element el, String attrs) {
    String border = "0";
    String frame = "void";

    //borders
    if (el.getAttributes().getNamedItem("border") != null) {
      border = el.getAttribute("border");
      if (!border.equals("0")) {
        frame = "border";
      }
    }
    if (el.getAttributes().getNamedItem("frame") != null) {
      frame = el.getAttribute("frame").toLowerCase();
    }

    if (!border.equals("0")) {
      final String fstyle = "border-@-style:solid;border-@-width:" + border + "px;";
      if (frame.equals("above")) {
        attrs = attrs + applyBorders(fstyle, "top");
      }
      if (frame.equals("below")) {
        attrs = attrs + applyBorders(fstyle, "bottom");
      }
      if (frame.equals("hsides")) {
        attrs = attrs + applyBorders(fstyle, "left");
        attrs = attrs + applyBorders(fstyle, "right");
      }
      if (frame.equals("lhs")) {
        attrs = attrs + applyBorders(fstyle, "left");
      }
      if (frame.equals("rhs")) {
        attrs = attrs + applyBorders(fstyle, "right");
      }
      if (frame.equals("vsides")) {
        attrs = attrs + applyBorders(fstyle, "top");
        attrs = attrs + applyBorders(fstyle, "bottom");
      }
      if (frame.equals("box")) {
        attrs = appAllBorders(attrs, fstyle);
      }
      if (frame.equals("border")) {
        attrs = appAllBorders(attrs, fstyle);
      }
    }
    return attrs;
  }

  private static String appAllBorders(String attrs, final String fstyle) {
    attrs = attrs + applyBorders(fstyle, "left");
    attrs = attrs + applyBorders(fstyle, "right");
    attrs = attrs + applyBorders(fstyle, "top");
    attrs = attrs + applyBorders(fstyle, "bottom");
    return attrs;
  }

  private static String getFontElementStyle(final Element el, String attrs) {
    if (el.getAttributes().getNamedItem("color") != null) {
      attrs = attrs + "color: " + el.getAttribute("color") + ";";
    }
    if (el.getAttributes().getNamedItem("face") != null) {
      attrs = attrs + "font-family: " + el.getAttribute("face") + ";";
    }
    if (el.getAttributes().getNamedItem("size") != null) {
      final String sz = el.getAttribute("size");
      String ret = "normal";
      if (sz.equals("1")) {
        ret = "xx-small";
      } else if (sz.equals("2")) {
        ret = "x-small";
      } else if (sz.equals("3")) {
        ret = "small";
      } else if (sz.equals("4")) {
        ret = "normal";
      } else if (sz.equals("5")) {
        ret = "large";
      } else if (sz.equals("6")) {
        ret = "x-large";
      } else if (sz.equals("7")) {
        ret = "xx-large";
      } else if (sz.startsWith("+")) {
        final int sn = Integer.parseInt(sz.substring(1));
        if ((sn > 0) && (sn <= 7)) {
          ret = (100 + (sn * 20)) + "%";
        }
      } else if (sz.startsWith("-")) {
        final int sn = Integer.parseInt(sz.substring(1));
        if ((sn > 0) && (sn <= 7)) {
          ret = (100 - (sn * 10)) + "%";
        }
      }
      attrs = attrs + "font-size: " + ret;
    }
    return attrs;
  }

  private static String applyBorders(final String template, final String dir) {
    return template.replaceAll("@", dir);
  }

  //======== Normalize the dom =============================

  /**
   * Provides a cleanup of a HTML DOM tree according to the HTML syntax
   * restrictions. Currently, following actions are implemented:
   * <ul>
   * <li>Table cleanup
   * <ul>
   * <li>elements that are not acceptable within a table are moved before the table</li>
   * </ul>
   * </li>
   * </ul>
   *
   * @param doc
   *          the processed DOM Document.
   */
  public static void normalizeHTMLTree(final Document doc) {
    //normalize tables
    final NodeList tables = doc.getElementsByTagName("table");
    for (int i = 0; i < tables.getLength(); i++) {
      final Vector<Node> nodes = new Vector<>();
      recursiveFindBadNodesInTable(tables.item(i), null, nodes);
      for (final Node n : nodes) {
        moveSubtreeBefore(n, tables.item(i));
      }
    }
  }

  /**
   * Finds all the nodes in a table that cannot be contained in the table
   * according to the HTML syntax.
   *
   * @param n
   *          table root
   * @param cellroot
   *          last cell root
   * @param nodes
   *          resulting list of nodes
   */
  private static void recursiveFindBadNodesInTable(final Node n, final Node cellroot, final Vector<Node> nodes) {
    Node cell = cellroot;

    if (n.getNodeType() == Node.ELEMENT_NODE) {
      final String tag = n.getNodeName().toLowerCase();
      if (tag.equals("table")) {
        if (cell != null) { //do not enter nested tables
          return;
        }
      } else if (tag.equals("tbody") || tag.equals("thead") || tag.equals("tfoot")
          || tag.equals("tr") || tag.equals("col") || tag.equals("colgroup")) {
      } else if (tag.equals("td") || tag.equals("th") || tag.equals("caption")) {
        cell = n;
      } else { //other elements
        if (cell == null) {
          nodes.add(n);
          return;
        }
      }
    } else if (n.getNodeType() == Node.TEXT_NODE) { //other nodes
      if ((cell == null) && (n.getNodeValue().trim().length() > 0)) {
        nodes.add(n);
        return;
      }
    }

    final NodeList child = n.getChildNodes();
    for (int i = 0; i < child.getLength(); i++) {
      recursiveFindBadNodesInTable(child.item(i), cell, nodes);
    }
  }

  private static void moveSubtreeBefore(final Node root, final Node ref) {
    root.getParentNode().removeChild(root);
    ref.getParentNode().insertBefore(root, ref);
  }

}