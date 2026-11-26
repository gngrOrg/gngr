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

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.text.StyleContext;

import org.lobobrowser.util.Strings;

/** Note: Undocumented class? */
//import sun.font.FontManager;

/**
 * @author J. H. S.
 */
public class FontFactory {
  private static final Logger logger = Logger.getLogger(FontFactory.class.getName());
  private static final boolean loggableFine = logger.isLoggable(Level.FINE);
  static final int DEFAULT_MAX_CACHE_SIZE = 512;

  private static final class InstanceHolder {
    static final FontFactory INSTANCE = new FontFactory();
  }

  private final int maxCacheSize;
  private final Set<String> fontFamilies = new HashSet<>(40);
  private final Map<FontKey, Font> fontMap;

  private FontFactory() {
    this(DEFAULT_MAX_CACHE_SIZE);
  }

  FontFactory(final int maxCacheSize) {
    if (maxCacheSize < 0) {
      throw new IllegalArgumentException("maxCacheSize cannot be negative: " + maxCacheSize);
    }
    this.maxCacheSize = maxCacheSize;
    this.fontMap = new LinkedHashMap<FontKey, Font>(64, 0.75f, true) {
      private static final long serialVersionUID = 1L;

      @Override
      protected boolean removeEldestEntry(final Map.Entry<FontKey, Font> eldest) {
        return size() > FontFactory.this.maxCacheSize;
      }
    };
    final boolean liflag = loggableFine;
    final String[] ffns = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    final Set<String> fontFamilies = this.fontFamilies;
    synchronized (this) {
      for (final String ffn : ffns) {
        if (liflag) {
          logger.fine("FontFactory(): family=" + ffn);
        }
        fontFamilies.add(ffn.toLowerCase(Locale.ROOT));
      }
    }
  }

  public static final FontFactory getInstance() {
    return InstanceHolder.INSTANCE;
  }

  int getCacheSize() {
    synchronized (this) {
      return this.fontMap.size();
    }
  }

  private final Map<String, Font> registeredFonts = new HashMap<>(0);

  /**
   * Registers a font family. It does not close the stream provided. Fonts
   * should be registered before the renderer has a chance to cache document
   * font specifications.
   *
   * @param fontName
   *          The name of a font as it would appear in a font-family
   *          specification.
   * @param fontFormat
   *          Should be {@link Font#TRUETYPE_FONT}.
   */
  public void registerFont(final String fontName, final int fontFormat, final java.io.InputStream fontStream)
      throws java.awt.FontFormatException,
      java.io.IOException {
    final Font f = Font.createFont(fontFormat, fontStream);
    synchronized (this) {
      this.registeredFonts.put(fontName.toLowerCase(Locale.ROOT), f);
      this.fontMap.clear();
    }
  }

  /**
   * Unregisters a font previously registered with
   * {@link #registerFont(String, int, java.io.InputStream)}.
   *
   * @param fontName
   *          The font name to be removed.
   */
  public void unregisterFont(final String fontName) {
    synchronized (this) {
      this.registeredFonts.remove(fontName.toLowerCase(Locale.ROOT));
      this.fontMap.clear();
    }
  }

  /**
   * Resolves and caches an AWT {@link Font} matching the requested CSS font properties.
   * Note: {@code fontVariant} is accepted for API compatibility because {@link Font} /
   * {@link TextAttribute} has no small-caps attribute.
   */
  public Font getFont(final String fontFamily, final String fontStyle, final String fontVariant, final String fontWeight,
      final float fontSize, final Set<Locale> locales,
      final Integer superscript) {
    final FontKey key = new FontKey(fontFamily, fontStyle, fontWeight, fontSize, locales, superscript);
    synchronized (this) {
      Font font = this.fontMap.get(key);
      if (font == null) {
        font = this.createFont(key);
        if (this.maxCacheSize > 0) {
          this.fontMap.put(key, font);
        }
      }
      return font;
    }
  }

  private String defaultFontName = "SansSerif";

  public synchronized String getDefaultFontName() {
    return this.defaultFontName;
  }

  /**
   * Sets the default font name to be used when a name is unrecognized or when a
   * font is determined not to be capable of diplaying characters from a given
   * language. This should be the name of a font that can display unicode text
   * across all or most languages.
   *
   * @param defaultFontName
   *          The name of a font.
   */
  public synchronized void setDefaultFontName(final String defaultFontName) {
    if (defaultFontName == null) {
      throw new IllegalArgumentException("defaultFontName cannot be null");
    }
    if (!Objects.equals(this.defaultFontName, defaultFontName)) {
      this.defaultFontName = defaultFontName;
      this.fontMap.clear();
    }
  }

  private final Font createFont(final FontKey key) {
    final Font font = createFont_Impl(key);
    return superscriptFont(font, key.superscript);
  }

  public static Font superscriptFont(final Font baseFont, final Integer newSuperscript) {
    if (newSuperscript == null) {
      return baseFont;
    }
    Integer fontSuperScript = (Integer) baseFont.getAttributes().get(TextAttribute.SUPERSCRIPT);
    if (fontSuperScript == null) {
      fontSuperScript = new Integer(0);
    }
    if (fontSuperScript.equals(newSuperscript)) {
      return baseFont;
    } else {
      final Map<TextAttribute, Integer> additionalAttributes = new HashMap<>();
      additionalAttributes.put(TextAttribute.SUPERSCRIPT, newSuperscript);
      return baseFont.deriveFont(additionalAttributes);
    }
  }

  private final Font createFont_Impl(final FontKey key) {
    final String fontNames = key.fontFamily;
    String matchingFace = null;
    final Set<String> fontFamilies = this.fontFamilies;
    final Map<String, Font> registeredFonts = this.registeredFonts;
    Font baseFont = null;
    if (fontNames != null) {
      final StringTokenizer tok = new StringTokenizer(fontNames, ",");
      while (tok.hasMoreTokens()) {
        final String face = Strings.unquote(Strings.unquoteSingle(tok.nextToken().trim()));
        final String faceTL = face.toLowerCase(Locale.ROOT);
        if (registeredFonts.containsKey(faceTL)) {
          baseFont = registeredFonts.get(faceTL);
          break;
        } else if (fontFamilies.contains(faceTL)) {
          matchingFace = faceTL;
          break;
        } else if ("monospace".equals(faceTL)) {
          baseFont = Font.decode("monospaced");
          break;
        } else if ("sans-serif".equals(faceTL)) {
          matchingFace = Font.SANS_SERIF;
          break;
        }
      }
    }
    int fontStyle = Font.PLAIN;
    if (isItalicStyle(key.fontStyle)) {
      fontStyle |= Font.ITALIC;
    }
    if (isBoldWeight(key.fontWeight)) {
      fontStyle |= Font.BOLD;
    }
    if (baseFont != null) {
      return baseFont.deriveFont(fontStyle, key.fontSize);
    } else if (matchingFace != null) {
      final Font font = createFont(matchingFace, fontStyle, Math.round(key.fontSize));
      final Set<Locale> locales = key.locales;
      if (locales == null) {
        final Locale locale = Locale.getDefault();
        if (font.canDisplayUpTo(locale.getDisplayLanguage(locale)) == -1) {
          return font;
        }
      } else {
        final Iterator<Locale> i = locales.iterator();
        boolean allMatch = true;
        while (i.hasNext()) {
          final Locale locale = i.next();
          if (font.canDisplayUpTo(locale.getDisplayLanguage(locale)) != -1) {
            allMatch = false;
            break;
          }
        }
        if (allMatch) {
          return font;
        }
      }
      // Otherwise, fall through.
    }
    // Last resort:
    return createFont(this.defaultFontName, fontStyle, Math.round(key.fontSize));
  }

  private static boolean isItalicStyle(final String fontStyle) {
    if (fontStyle == null) {
      return false;
    }
    final String trimmed = fontStyle.trim();
    return "italic".equalsIgnoreCase(trimmed) || "oblique".equalsIgnoreCase(trimmed);
  }

  private static boolean isBoldWeight(final String fontWeight) {
    if (fontWeight == null) {
      return false;
    }
    final String trimmed = fontWeight.trim();
    if (trimmed.isEmpty()) {
      return false;
    }
    if ("bold".equalsIgnoreCase(trimmed) || "bolder".equalsIgnoreCase(trimmed)) {
      return true;
    }
    final char lastChar = trimmed.charAt(trimmed.length() - 1);
    if (lastChar == 'f' || lastChar == 'F' || lastChar == 'd' || lastChar == 'D') {
      return false;
    }
    if (trimmed.indexOf('x') >= 0 || trimmed.indexOf('X') >= 0) {
      return false;
    }
    try {
      final float weight = Float.parseFloat(trimmed);
      return !Float.isNaN(weight) && weight >= 600.0f && weight <= 1000.0f;
    } catch (final NumberFormatException nfe) {
      return false;
    }
  }

  private static Font createFont(final String name, final int style, final int size) {
    return StyleContext.getDefaultStyleContext().getFont(name, style, size);
    // Proprietary Sun API. Maybe shouldn't use it. Works well for Chinese.
    // return FontManager.getCompositeFontUIResource(new Font(name, style,
    // size));
  }

  private static class FontKey {
    public final String fontFamily;
    public final String fontStyle;
    public final String fontWeight;
    public final float fontSize;
    public final Set<Locale> locales;
    public final Integer superscript;
    private final int cachedHash;

    public FontKey(final String fontFamily, final String fontStyle, final String fontWeight,
        final float fontSize, final Set<Locale> locales, final Integer superscript) {
      this.fontFamily = fontFamily;
      this.fontStyle = fontStyle;
      this.fontWeight = fontWeight;
      this.fontSize = fontSize;
      this.locales = locales == null ? null : Collections.unmodifiableSet(new HashSet<>(locales));
      this.superscript = superscript;
      this.cachedHash = Objects.hash(this.fontFamily, this.fontStyle, this.fontWeight,
          Float.valueOf(this.fontSize), this.locales, this.superscript);
    }

    @Override
    public boolean equals(final Object other) {
      if (other == this) {
        return true;
      }
      if (!(other instanceof FontKey)) {
        return false;
      }
      final FontKey ors = (FontKey) other;
      return Float.compare(this.fontSize, ors.fontSize) == 0
          && Objects.equals(this.fontFamily, ors.fontFamily)
          && Objects.equals(this.fontStyle, ors.fontStyle)
          && Objects.equals(this.fontWeight, ors.fontWeight)
          && Objects.equals(this.superscript, ors.superscript)
          && Objects.equals(this.locales, ors.locales);
    }

    @Override
    public int hashCode() {
      return this.cachedHash;
    }

    @Override
    public String toString() {
      return "FontKey[family=" + this.fontFamily + ",size=" + this.fontSize + ",style=" + this.fontStyle + ",weight=" + this.fontWeight
          + ",superscript=" + this.superscript + "]";
    }
  }
}