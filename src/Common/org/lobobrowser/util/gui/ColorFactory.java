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
 * Created on Apr 17, 2005
 */
package org.lobobrowser.util.gui;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author J. H. S.
 */
public class ColorFactory {
  private static final Logger logger = Logger.getLogger(ColorFactory.class.getName());
  public static final Color TRANSPARENT = new Color(0, 0, 0, 0);
  private static ColorFactory instance;
  private final Map<String, Color> colorMap = new HashMap<>(256);

  private ColorFactory() {
    final Map<String, Color> colorMap = this.colorMap;
    synchronized (this) {
      colorMap.put("transparent", TRANSPARENT);
      // http://www.w3schools.com/css/css_colornames.asp
      colorMap.put("aliceblue", new Color(0xf0f8ff));
      colorMap.put("antiquewhite", new Color(0xfaebd7));
      colorMap.put("aqua", new Color(0x00ffff));
      colorMap.put("aquamarine", new Color(0x7fffd4));
      colorMap.put("azure", new Color(0xf0ffff));
      colorMap.put("beige", new Color(0xf5f5dc));
      colorMap.put("bisque", new Color(0xffe4c4));
      colorMap.put("black", new Color(0x000000));
      colorMap.put("blanchedalmond", new Color(0xffebcd));
      colorMap.put("blue", new Color(0x0000ff));
      colorMap.put("blueviolet", new Color(0x8a2be2));
      colorMap.put("brown", new Color(0xa52a2a));
      colorMap.put("burlywood", new Color(0xdeb887));
      colorMap.put("cadetblue", new Color(0x5f9ea0));
      colorMap.put("chartreuse", new Color(0x7fff00));
      colorMap.put("chocolate", new Color(0xd2691e));
      colorMap.put("coral", new Color(0xff7f50));
      colorMap.put("cornflowerblue", new Color(0x6495ed));
      colorMap.put("cornsilk", new Color(0xfff8dc));
      colorMap.put("crimson", new Color(0xdc143c));
      colorMap.put("cyan", new Color(0x00ffff));
      colorMap.put("darkblue", new Color(0x00008b));
      colorMap.put("darkcyan", new Color(0x008b8b));
      colorMap.put("darkgoldenrod", new Color(0xb8860b));
      colorMap.put("darkgray", new Color(0xa9a9a9));
      colorMap.put("darkgrey", new Color(0xa9a9a9));
      colorMap.put("darkgreen", new Color(0x006400));
      colorMap.put("darkkhaki", new Color(0xbdb76b));
      colorMap.put("darkmagenta", new Color(0x8b008b));
      colorMap.put("darkolivegreen", new Color(0x556b2f));
      colorMap.put("darkorange", new Color(0xff8c00));
      colorMap.put("darkorchid", new Color(0x9932cc));
      colorMap.put("darkred", new Color(0x8b0000));
      colorMap.put("darksalmon", new Color(0xe9967a));
      colorMap.put("darkseagreen", new Color(0x8fbc8f));
      colorMap.put("darkslateblue", new Color(0x483d8b));
      colorMap.put("darkslategray", new Color(0x2f4f4f));
      colorMap.put("darkslategrey", new Color(0x2f4f4f));
      colorMap.put("darkturquoise", new Color(0x00ced1));
      colorMap.put("darkviolet", new Color(0x9400d3));
      colorMap.put("deeppink", new Color(0xff1493));
      colorMap.put("deepskyblue", new Color(0x00bfff));
      colorMap.put("dimgray", new Color(0x696969));
      colorMap.put("dimgrey", new Color(0x696969));
      colorMap.put("dodgerblue", new Color(0x1e90ff));
      colorMap.put("firebrick", new Color(0xb22222));
      colorMap.put("floralwhite", new Color(0xfffaf0));
      colorMap.put("forestgreen", new Color(0x228b22));
      colorMap.put("fuchsia", new Color(0xff00ff));
      colorMap.put("gainsboro", new Color(0xdcdcdc));
      colorMap.put("ghostwhite", new Color(0xf8f8ff));
      colorMap.put("gold", new Color(0xffd700));
      colorMap.put("goldenrod", new Color(0xdaa520));
      colorMap.put("gray", new Color(0x808080));
      colorMap.put("grey", new Color(0x808080));
      colorMap.put("green", new Color(0x008000));
      colorMap.put("greenyellow", new Color(0xadff2f));
      colorMap.put("honeydew", new Color(0xf0fff0));
      colorMap.put("hotpink", new Color(0xff69b4));
      colorMap.put("indianred", new Color(0xcd5c5c));
      colorMap.put("indigo", new Color(0x4b0082));
      colorMap.put("ivory", new Color(0xfffff0));
      colorMap.put("khaki", new Color(0xf0e68c));
      colorMap.put("lavender", new Color(0xe6e6fa));
      colorMap.put("lavenderblush", new Color(0xfff0f5));
      colorMap.put("lawngreen", new Color(0x7cfc00));
      colorMap.put("lemonchiffon", new Color(0xfffacd));
      colorMap.put("lightblue", new Color(0xadd8e6));
      colorMap.put("lightcoral", new Color(0xf08080));
      colorMap.put("lightcyan", new Color(0xe0ffff));
      colorMap.put("lightgoldenrodyellow", new Color(0xfafad2));
      colorMap.put("lightgray", new Color(0xd3d3d3));
      colorMap.put("lightgrey", new Color(0xd3d3d3));
      colorMap.put("lightgreen", new Color(0x90ee90));
      colorMap.put("lightpink", new Color(0xffb6c1));
      colorMap.put("lightsalmon", new Color(0xffa07a));
      colorMap.put("lightseagreen", new Color(0x20b2aa));
      colorMap.put("lightskyblue", new Color(0x87cefa));
      colorMap.put("lightslategray", new Color(0x778899));
      colorMap.put("lightslategrey", new Color(0x778899));
      colorMap.put("lightsteelblue", new Color(0xb0c4de));
      colorMap.put("lightyellow", new Color(0xffffe0));
      colorMap.put("lime", new Color(0x00ff00));
      colorMap.put("limegreen", new Color(0x32cd32));
      colorMap.put("linen", new Color(0xfaf0e6));
      colorMap.put("magenta", new Color(0xff00ff));
      colorMap.put("maroon", new Color(0x800000));
      colorMap.put("mediumaquamarine", new Color(0x66cdaa));
      colorMap.put("mediumblue", new Color(0x0000cd));
      colorMap.put("mediumorchid", new Color(0xba55d3));
      colorMap.put("mediumpurple", new Color(0x9370d8));
      colorMap.put("mediumseagreen", new Color(0x3cb371));
      colorMap.put("mediumslateblue", new Color(0x7b68ee));
      colorMap.put("mediumspringgreen", new Color(0x00fa9a));
      colorMap.put("mediumturquoise", new Color(0x48d1cc));
      colorMap.put("mediumvioletred", new Color(0xc71585));
      colorMap.put("midnightblue", new Color(0x191970));
      colorMap.put("mintcream", new Color(0xf5fffa));
      colorMap.put("mistyrose", new Color(0xffe4e1));
      colorMap.put("moccasin", new Color(0xffe4b5));
      colorMap.put("navajowhite", new Color(0xffdead));
      colorMap.put("navy", new Color(0x000080));
      colorMap.put("oldlace", new Color(0xfdf5e6));
      colorMap.put("olive", new Color(0x808000));
      colorMap.put("olivedrab", new Color(0x6b8e23));
      colorMap.put("orange", new Color(0xffa500));
      colorMap.put("orangered", new Color(0xff4500));
      colorMap.put("orchid", new Color(0xda70d6));
      colorMap.put("palegoldenrod", new Color(0xeee8aa));
      colorMap.put("palegreen", new Color(0x98fb98));
      colorMap.put("paleturquoise", new Color(0xafeeee));
      colorMap.put("palevioletred", new Color(0xd87093));
      colorMap.put("papayawhip", new Color(0xffefd5));
      colorMap.put("peachpuff", new Color(0xffdab9));
      colorMap.put("peru", new Color(0xcd853f));
      colorMap.put("pink", new Color(0xffc0cb));
      colorMap.put("plum", new Color(0xdda0dd));
      colorMap.put("powderblue", new Color(0xb0e0e6));
      colorMap.put("purple", new Color(0x800080));
      colorMap.put("red", new Color(0xff0000));
      colorMap.put("rosybrown", new Color(0xbc8f8f));
      colorMap.put("royalblue", new Color(0x4169e1));
      colorMap.put("saddlebrown", new Color(0x8b4513));
      colorMap.put("salmon", new Color(0xfa8072));
      colorMap.put("sandybrown", new Color(0xf4a460));
      colorMap.put("seagreen", new Color(0x2e8b57));
      colorMap.put("seashell", new Color(0xfff5ee));
      colorMap.put("sienna", new Color(0xa0522d));
      colorMap.put("silver", new Color(0xc0c0c0));
      colorMap.put("skyblue", new Color(0x87ceeb));
      colorMap.put("slateblue", new Color(0x6a5acd));
      colorMap.put("slategray", new Color(0x708090));
      colorMap.put("slategrey", new Color(0x708090));
      colorMap.put("snow", new Color(0xfffafa));
      colorMap.put("springgreen", new Color(0x00ff7f));
      colorMap.put("steelblue", new Color(0x4682b4));
      colorMap.put("tan", new Color(0xd2b48c));
      colorMap.put("teal", new Color(0x008080));
      colorMap.put("thistle", new Color(0xd8bfd8));
      colorMap.put("tomato", new Color(0xff6347));
      colorMap.put("turquoise", new Color(0x40e0d0));
      colorMap.put("violet", new Color(0xee82ee));
      colorMap.put("wheat", new Color(0xf5deb3));
      colorMap.put("white", new Color(0xffffff));
      colorMap.put("whitesmoke", new Color(0xf5f5f5));
      colorMap.put("yellow", new Color(0xffff00));
      colorMap.put("yellowgreen", new Color(0x9acd32));
    }
  }

  public static final ColorFactory getInstance() {
    if (instance == null) {
      synchronized (ColorFactory.class) {
        if (instance == null) {
          instance = new ColorFactory();
        }
      }
    }
    return instance;
  }

  private static final String RGB_START = "rgb(";
  private static final String RGBA_START = "rgba(";

  public boolean isColor(final String colorSpec) {
    if (colorSpec.startsWith("#")) {
      return true;
    }
    final String normalSpec = colorSpec.toLowerCase();
    if (normalSpec.startsWith(RGB_START)) {
      return true;
    }
    synchronized (this) {
      return colorMap.containsKey(normalSpec);
    }
  }

  public Color getColor(final String colorSpec) {
    final String normalSpec = colorSpec.toLowerCase();
    synchronized (this) {
      Color color = colorMap.get(normalSpec);
      if (color == null) {
        if (normalSpec.startsWith(RGB_START)) {
          // CssParser produces this format.
          final int endIdx = normalSpec.lastIndexOf(')');
          final String commaValues = endIdx == -1 ? normalSpec.substring(RGB_START.length()) : normalSpec.substring(RGB_START.length(),
              endIdx);
          final StringTokenizer tok = new StringTokenizer(commaValues, ",");
          int r = 0, g = 0, b = 0;
          if (tok.hasMoreTokens()) {
            final String rstr = tok.nextToken().trim();
            try {
              r = Integer.parseInt(rstr);
            } catch (final NumberFormatException nfe) {
              // ignore
            }
            if (tok.hasMoreTokens()) {
              final String gstr = tok.nextToken().trim();
              try {
                g = Integer.parseInt(gstr);
              } catch (final NumberFormatException nfe) {
                // ignore
              }
              if (tok.hasMoreTokens()) {
                final String bstr = tok.nextToken().trim();
                try {
                  b = Integer.parseInt(bstr);
                } catch (final NumberFormatException nfe) {
                  // ignore
                }
              }
            }
          }
          color = new Color(r, g, b);
        } else if (normalSpec.startsWith("#")) {
          // TODO: OPTIMIZE: It would be more efficient to
          // create new Color(hex), but CssParser doesn't
          // give us values formatted with "#" either way.
          final int len = normalSpec.length();
          final int[] rgba = new int[4];
          rgba[3] = 255;
          if (len == 4) {
            for (int i = 1; i < 4; i++) {
              final String hexText = normalSpec.substring(i, i + Math.min(1, len - i));
              try {
                final int singleDigitValue = Integer.parseInt(hexText, 16);
                rgba[i-1] = (singleDigitValue << 4) | singleDigitValue;
              } catch (final NumberFormatException nfe) {
                // Ignore
              }
            }

          } else {
            for (int i = 0; i < rgba.length; i++) {
              final int idx = (2 * i) + 1;
              if (idx < len) {
                final String hexText = normalSpec.substring(idx, idx + Math.min(2, len - idx));
                try {
                  rgba[i] = Integer.parseInt(hexText, 16);
                } catch (final NumberFormatException nfe) {
                  // Ignore
                }
              }
            }
          }
          color = new Color(rgba[0], rgba[1], rgba[2], rgba[3]);
        } else if (normalSpec.startsWith(RGBA_START)) {
          final int endIdx = normalSpec.lastIndexOf(')');
          final String commaValues = endIdx == -1 ? normalSpec.substring(RGBA_START.length()) : normalSpec.substring(RGBA_START.length(),
              endIdx);
          final StringTokenizer tok = new StringTokenizer(commaValues, ",");
          try {
            if (tok.hasMoreTokens()) {
              final String rstr = tok.nextToken().trim();
              final int r = Integer.parseInt(rstr);
              if (tok.hasMoreTokens()) {
                final String gstr = tok.nextToken().trim();
                final int g = Integer.parseInt(gstr);
                if (tok.hasMoreTokens()) {
                  final String bstr = tok.nextToken().trim();
                  final int b = Integer.parseInt(bstr);
                  if (tok.hasMoreTokens()) {
                    final String astr = tok.nextToken().trim();
                    final float a = Float.parseFloat(astr);
                    color = new Color(r / 255.0f, g / 255.0f, b / 255.0f, a);
                  }
                }
              }
            }
          } catch (final NumberFormatException nfe) {
            // ignore
          }
        } else {
          if (logger.isLoggable(Level.INFO)) {
            logger.warning("getColor(): Color spec [" + normalSpec + "] unknown.");
          }
          return Color.RED;
        }
        colorMap.put(normalSpec, color);
      }
      return color;
    }
  }

}
