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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author J. H. S.
 */
public class ColorFactory {
  private static final Logger logger = Logger.getLogger(ColorFactory.class.getName());
  public static final Color TRANSPARENT = new Color(0, 0, 0, 0);
  static final int DEFAULT_MAX_CACHE_SIZE = 1024;
  private static final String RGB_START = "rgb(";
  private static final String RGBA_START = "rgba(";
  private static final Map<String, Color> NAMED_COLORS;

  static {
    final Map<String, Color> named = new HashMap<>(256);
    named.put("transparent", TRANSPARENT);
    // https://www.w3.org/TR/css-color-3/#svg-color + CSS Color Level 4 rebeccapurple
    named.put("aliceblue", new Color(0xf0f8ff));
    named.put("antiquewhite", new Color(0xfaebd7));
    named.put("aqua", new Color(0x00ffff));
    named.put("aquamarine", new Color(0x7fffd4));
    named.put("azure", new Color(0xf0ffff));
    named.put("beige", new Color(0xf5f5dc));
    named.put("bisque", new Color(0xffe4c4));
    named.put("black", new Color(0x000000));
    named.put("blanchedalmond", new Color(0xffebcd));
    named.put("blue", new Color(0x0000ff));
    named.put("blueviolet", new Color(0x8a2be2));
    named.put("brown", new Color(0xa52a2a));
    named.put("burlywood", new Color(0xdeb887));
    named.put("cadetblue", new Color(0x5f9ea0));
    named.put("chartreuse", new Color(0x7fff00));
    named.put("chocolate", new Color(0xd2691e));
    named.put("coral", new Color(0xff7f50));
    named.put("cornflowerblue", new Color(0x6495ed));
    named.put("cornsilk", new Color(0xfff8dc));
    named.put("crimson", new Color(0xdc143c));
    named.put("cyan", new Color(0x00ffff));
    named.put("darkblue", new Color(0x00008b));
    named.put("darkcyan", new Color(0x008b8b));
    named.put("darkgoldenrod", new Color(0xb8860b));
    named.put("darkgray", new Color(0xa9a9a9));
    named.put("darkgrey", new Color(0xa9a9a9));
    named.put("darkgreen", new Color(0x006400));
    named.put("darkkhaki", new Color(0xbdb76b));
    named.put("darkmagenta", new Color(0x8b008b));
    named.put("darkolivegreen", new Color(0x556b2f));
    named.put("darkorange", new Color(0xff8c00));
    named.put("darkorchid", new Color(0x9932cc));
    named.put("darkred", new Color(0x8b0000));
    named.put("darksalmon", new Color(0xe9967a));
    named.put("darkseagreen", new Color(0x8fbc8f));
    named.put("darkslateblue", new Color(0x483d8b));
    named.put("darkslategray", new Color(0x2f4f4f));
    named.put("darkslategrey", new Color(0x2f4f4f));
    named.put("darkturquoise", new Color(0x00ced1));
    named.put("darkviolet", new Color(0x9400d3));
    named.put("deeppink", new Color(0xff1493));
    named.put("deepskyblue", new Color(0x00bfff));
    named.put("dimgray", new Color(0x696969));
    named.put("dimgrey", new Color(0x696969));
    named.put("dodgerblue", new Color(0x1e90ff));
    named.put("firebrick", new Color(0xb22222));
    named.put("floralwhite", new Color(0xfffaf0));
    named.put("forestgreen", new Color(0x228b22));
    named.put("fuchsia", new Color(0xff00ff));
    named.put("gainsboro", new Color(0xdcdcdc));
    named.put("ghostwhite", new Color(0xf8f8ff));
    named.put("gold", new Color(0xffd700));
    named.put("goldenrod", new Color(0xdaa520));
    named.put("gray", new Color(0x808080));
    named.put("grey", new Color(0x808080));
    named.put("green", new Color(0x008000));
    named.put("greenyellow", new Color(0xadff2f));
    named.put("honeydew", new Color(0xf0fff0));
    named.put("hotpink", new Color(0xff69b4));
    named.put("indianred", new Color(0xcd5c5c));
    named.put("indigo", new Color(0x4b0082));
    named.put("ivory", new Color(0xfffff0));
    named.put("khaki", new Color(0xf0e68c));
    named.put("lavender", new Color(0xe6e6fa));
    named.put("lavenderblush", new Color(0xfff0f5));
    named.put("lawngreen", new Color(0x7cfc00));
    named.put("lemonchiffon", new Color(0xfffacd));
    named.put("lightblue", new Color(0xadd8e6));
    named.put("lightcoral", new Color(0xf08080));
    named.put("lightcyan", new Color(0xe0ffff));
    named.put("lightgoldenrodyellow", new Color(0xfafad2));
    named.put("lightgray", new Color(0xd3d3d3));
    named.put("lightgrey", new Color(0xd3d3d3));
    named.put("lightgreen", new Color(0x90ee90));
    named.put("lightpink", new Color(0xffb6c1));
    named.put("lightsalmon", new Color(0xffa07a));
    named.put("lightseagreen", new Color(0x20b2aa));
    named.put("lightskyblue", new Color(0x87cefa));
    named.put("lightslategray", new Color(0x778899));
    named.put("lightslategrey", new Color(0x778899));
    named.put("lightsteelblue", new Color(0xb0c4de));
    named.put("lightyellow", new Color(0xffffe0));
    named.put("lime", new Color(0x00ff00));
    named.put("limegreen", new Color(0x32cd32));
    named.put("linen", new Color(0xfaf0e6));
    named.put("magenta", new Color(0xff00ff));
    named.put("maroon", new Color(0x800000));
    named.put("mediumaquamarine", new Color(0x66cdaa));
    named.put("mediumblue", new Color(0x0000cd));
    named.put("mediumorchid", new Color(0xba55d3));
    named.put("mediumpurple", new Color(0x9370d8));
    named.put("mediumseagreen", new Color(0x3cb371));
    named.put("mediumslateblue", new Color(0x7b68ee));
    named.put("mediumspringgreen", new Color(0x00fa9a));
    named.put("mediumturquoise", new Color(0x48d1cc));
    named.put("mediumvioletred", new Color(0xc71585));
    named.put("midnightblue", new Color(0x191970));
    named.put("mintcream", new Color(0xf5fffa));
    named.put("mistyrose", new Color(0xffe4e1));
    named.put("moccasin", new Color(0xffe4b5));
    named.put("navajowhite", new Color(0xffdead));
    named.put("navy", new Color(0x000080));
    named.put("oldlace", new Color(0xfdf5e6));
    named.put("olive", new Color(0x808000));
    named.put("olivedrab", new Color(0x6b8e23));
    named.put("orange", new Color(0xffa500));
    named.put("orangered", new Color(0xff4500));
    named.put("orchid", new Color(0xda70d6));
    named.put("palegoldenrod", new Color(0xeee8aa));
    named.put("palegreen", new Color(0x98fb98));
    named.put("paleturquoise", new Color(0xafeeee));
    named.put("palevioletred", new Color(0xd87093));
    named.put("papayawhip", new Color(0xffefd5));
    named.put("peachpuff", new Color(0xffdab9));
    named.put("peru", new Color(0xcd853f));
    named.put("pink", new Color(0xffc0cb));
    named.put("plum", new Color(0xdda0dd));
    named.put("powderblue", new Color(0xb0e0e6));
    named.put("purple", new Color(0x800080));
    named.put("rebeccapurple", new Color(0x663399));
    named.put("red", new Color(0xff0000));
    named.put("rosybrown", new Color(0xbc8f8f));
    named.put("royalblue", new Color(0x4169e1));
    named.put("saddlebrown", new Color(0x8b4513));
    named.put("salmon", new Color(0xfa8072));
    named.put("sandybrown", new Color(0xf4a460));
    named.put("seagreen", new Color(0x2e8b57));
    named.put("seashell", new Color(0xfff5ee));
    named.put("sienna", new Color(0xa0522d));
    named.put("silver", new Color(0xc0c0c0));
    named.put("skyblue", new Color(0x87ceeb));
    named.put("slateblue", new Color(0x6a5acd));
    named.put("slategray", new Color(0x708090));
    named.put("slategrey", new Color(0x708090));
    named.put("snow", new Color(0xfffafa));
    named.put("springgreen", new Color(0x00ff7f));
    named.put("steelblue", new Color(0x4682b4));
    named.put("tan", new Color(0xd2b48c));
    named.put("teal", new Color(0x008080));
    named.put("thistle", new Color(0xd8bfd8));
    named.put("tomato", new Color(0xff6347));
    named.put("turquoise", new Color(0x40e0d0));
    named.put("violet", new Color(0xee82ee));
    named.put("wheat", new Color(0xf5deb3));
    named.put("white", new Color(0xffffff));
    named.put("whitesmoke", new Color(0xf5f5f5));
    named.put("yellow", new Color(0xffff00));
    named.put("yellowgreen", new Color(0x9acd32));
    NAMED_COLORS = Collections.unmodifiableMap(named);
  }

  private static final class InstanceHolder {
    static final ColorFactory INSTANCE = new ColorFactory();
  }

  private final int maxCacheSize;
  private final Map<String, Color> colorCache;

  private ColorFactory() {
    this(DEFAULT_MAX_CACHE_SIZE);
  }

  ColorFactory(final int maxCacheSize) {
    if (maxCacheSize < 0) {
      throw new IllegalArgumentException("maxCacheSize cannot be negative: " + maxCacheSize);
    }
    this.maxCacheSize = maxCacheSize;
    this.colorCache = new LinkedHashMap<String, Color>(64, 0.75f, true) {
      private static final long serialVersionUID = 1L;

      @Override
      protected boolean removeEldestEntry(final Map.Entry<String, Color> eldest) {
        return size() > ColorFactory.this.maxCacheSize;
      }
    };
  }

  public static final ColorFactory getInstance() {
    return InstanceHolder.INSTANCE;
  }

  int getCacheSize() {
    synchronized (this.colorCache) {
      return this.colorCache.size();
    }
  }

  public boolean isColor(final String colorSpec) {
    if (colorSpec == null) {
      return false;
    }
    final String normalSpec = colorSpec.trim().toLowerCase(Locale.ROOT);
    if (normalSpec.isEmpty()) {
      return false;
    }
    if (NAMED_COLORS.containsKey(normalSpec)) {
      return true;
    }
    synchronized (this.colorCache) {
      if (this.colorCache.containsKey(normalSpec)) {
        return true;
      }
    }
    return parseColor(normalSpec) != null;
  }

  public Color getColor(final String colorSpec) {
    if (colorSpec == null) {
      return null;
    }
    final String normalSpec = colorSpec.trim().toLowerCase(Locale.ROOT);
    if (normalSpec.isEmpty()) {
      return null;
    }
    final Color named = NAMED_COLORS.get(normalSpec);
    if (named != null) {
      return named;
    }
    synchronized (this.colorCache) {
      final Color cached = this.colorCache.get(normalSpec);
      if (cached != null) {
        return cached;
      }
      final Color parsed = parseColor(normalSpec);
      if (parsed != null) {
        if (this.maxCacheSize > 0) {
          this.colorCache.put(normalSpec, parsed);
        }
        return parsed;
      }
    }
    if (logger.isLoggable(Level.INFO)) {
      logger.warning("getColor(): Color spec [" + normalSpec + "] unknown.");
    }
    return null;
  }

  private static Color parseColor(final String normalSpec) {
    if (normalSpec == null || normalSpec.isEmpty()) {
      return null;
    }
    final Color named = NAMED_COLORS.get(normalSpec);
    if (named != null) {
      return named;
    }
    if (normalSpec.startsWith("#")) {
      return parseHexColor(normalSpec);
    }
    if (normalSpec.startsWith(RGB_START) && normalSpec.endsWith(")")) {
      return parseRgbColor(normalSpec);
    }
    if (normalSpec.startsWith(RGBA_START) && normalSpec.endsWith(")")) {
      return parseRgbaColor(normalSpec);
    }
    return null;
  }

  private static Color parseHexColor(final String normalSpec) {
    final int hexLen = normalSpec.length() - 1;
    if (hexLen == 3 || hexLen == 4) {
      final int r = hexDigit(normalSpec.charAt(1));
      final int g = hexDigit(normalSpec.charAt(2));
      final int b = hexDigit(normalSpec.charAt(3));
      final int a = hexLen == 4 ? hexDigit(normalSpec.charAt(4)) : 15;
      if ((r | g | b | a) < 0) {
        return null;
      }
      return new Color((r << 4) | r, (g << 4) | g, (b << 4) | b, (a << 4) | a);
    }
    if (hexLen == 6 || hexLen == 8) {
      final int r = hexByte(normalSpec, 1);
      final int g = hexByte(normalSpec, 3);
      final int b = hexByte(normalSpec, 5);
      final int a = hexLen == 8 ? hexByte(normalSpec, 7) : 255;
      if ((r | g | b | a) < 0) {
        return null;
      }
      return new Color(r, g, b, a);
    }
    return null;
  }

  private static int hexByte(final String spec, final int offset) {
    final int hi = hexDigit(spec.charAt(offset));
    final int lo = hexDigit(spec.charAt(offset + 1));
    if (hi < 0 || lo < 0) {
      return -1;
    }
    return (hi << 4) | lo;
  }

  private static int hexDigit(final char ch) {
    if (ch >= '0' && ch <= '9') {
      return ch - '0';
    }
    if (ch >= 'a' && ch <= 'f') {
      return (ch - 'a') + 10;
    }
    if (ch >= 'A' && ch <= 'F') {
      return (ch - 'A') + 10;
    }
    return -1;
  }

  private static Color parseRgbColor(final String normalSpec) {
    final String inner = normalSpec.substring(RGB_START.length(), normalSpec.length() - 1);
    final String[] parts = inner.split(",", -1);
    if (parts.length != 3) {
      return null;
    }
    final Integer r = parseRgbChannel(parts[0]);
    final Integer g = parseRgbChannel(parts[1]);
    final Integer b = parseRgbChannel(parts[2]);
    if (r == null || g == null || b == null) {
      return null;
    }
    return new Color(r.intValue(), g.intValue(), b.intValue());
  }

  private static Color parseRgbaColor(final String normalSpec) {
    final String inner = normalSpec.substring(RGBA_START.length(), normalSpec.length() - 1);
    final String[] parts = inner.split(",", -1);
    if (parts.length != 4) {
      return null;
    }
    final Integer r = parseRgbChannel(parts[0]);
    final Integer g = parseRgbChannel(parts[1]);
    final Integer b = parseRgbChannel(parts[2]);
    final Integer a = parseAlphaChannel(parts[3]);
    if (r == null || g == null || b == null || a == null) {
      return null;
    }
    return new Color(r.intValue(), g.intValue(), b.intValue(), a.intValue());
  }

  private static Integer parseRgbChannel(final String part) {
    final String trimmed = part.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    if (trimmed.endsWith("%")) {
      final Float pct = parseCssFloat(trimmed.substring(0, trimmed.length() - 1));
      if (pct == null) {
        return null;
      }
      final float clamped = Math.max(0.0f, Math.min(100.0f, pct.floatValue()));
      return Integer.valueOf(Math.round(clamped * 2.55f));
    }
    try {
      final int value = Integer.parseInt(trimmed);
      final int clamped = Math.max(0, Math.min(255, value));
      return Integer.valueOf(clamped);
    } catch (final NumberFormatException nfe) {
      return null;
    }
  }

  private static Integer parseAlphaChannel(final String part) {
    final String trimmed = part.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    final boolean percent = trimmed.endsWith("%");
    final String numberPart = percent ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    final Float parsed = parseCssFloat(numberPart);
    if (parsed == null) {
      return null;
    }
    final float alpha = percent ? parsed.floatValue() / 100.0f : parsed.floatValue();
    final float clamped = Math.max(0.0f, Math.min(1.0f, alpha));
    return Integer.valueOf(Math.round(clamped * 255.0f));
  }

  private static Float parseCssFloat(final String text) {
    final String trimmed = text.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    final char lastChar = trimmed.charAt(trimmed.length() - 1);
    if (lastChar == 'f' || lastChar == 'F' || lastChar == 'd' || lastChar == 'D') {
      return null;
    }
    if (trimmed.indexOf('x') >= 0 || trimmed.indexOf('X') >= 0) {
      return null;
    }
    try {
      final float value = Float.parseFloat(trimmed);
      if (Float.isNaN(value) || Float.isInfinite(value)) {
        return null;
      }
      return Float.valueOf(value);
    } catch (final NumberFormatException nfe) {
      return null;
    }
  }
}
