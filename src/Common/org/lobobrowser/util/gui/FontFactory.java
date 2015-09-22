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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
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
  private static final FontFactory instance = new FontFactory();
  private final Set<String> fontFamilies = new HashSet<>(40);
  private final Map<FontKey, Font> fontMap = new HashMap<>(50);

  /**
   *
   */
  private FontFactory() {
    final boolean liflag = loggableFine;
    final String[] ffns = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    final Set<String> fontFamilies = this.fontFamilies;
    synchronized (this) {
      for (final String ffn : ffns) {
        if (liflag) {
          logger.fine("FontFactory(): family=" + ffn);
        }
        fontFamilies.add(ffn.toLowerCase());
      }
    }
  }

  public static final FontFactory getInstance() {
    return instance;
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
      this.registeredFonts.put(fontName.toLowerCase(), f);
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
      this.registeredFonts.remove(fontName.toLowerCase());
    }
  }

  public Font getFont(final String fontFamily, final String fontStyle, final String fontVariant, final String fontWeight,
      final float fontSize, final Set<Locale> locales,
      final Integer superscript) {
    final FontKey key = new FontKey(fontFamily, fontStyle, fontVariant, fontWeight, fontSize, locales, superscript);
    synchronized (this) {
      Font font = this.fontMap.get(key);
      if (font == null) {
        font = this.createFont(key);
        this.fontMap.put(key, font);
      }
      return font;
    }
  }

  private String defaultFontName = "SansSerif";

  public String getDefaultFontName() {
    return defaultFontName;
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
  public void setDefaultFontName(final String defaultFontName) {
    if (defaultFontName == null) {
      throw new IllegalArgumentException("defaultFontName cannot be null");
    }
    this.defaultFontName = defaultFontName;
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
        final String face = Strings.unquoteSingle(tok.nextToken().trim());
        final String faceTL = face.toLowerCase();
        if (registeredFonts.containsKey(faceTL)) {
          baseFont = registeredFonts.get(faceTL);
          break;
        } else if (fontFamilies.contains(faceTL)) {
          matchingFace = faceTL;
          break;
        } else if ("monospace".equals(faceTL)){
          baseFont = Font.decode("monospaced");
        }
      }
    }
    int fontStyle = Font.PLAIN;
    if ("italic".equalsIgnoreCase(key.fontStyle)) {
      fontStyle |= Font.ITALIC;
    }
    if ("bold".equalsIgnoreCase(key.fontWeight) || "bolder".equalsIgnoreCase(key.fontWeight)) {
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

  private static Font createFont(final String name, final int style, final int size) {
    return StyleContext.getDefaultStyleContext().getFont(name, style, size);
    // Proprietary Sun API. Maybe shouldn't use it. Works well for Chinese.
    // return FontManager.getCompositeFontUIResource(new Font(name, style,
    // size));
  }

  private static class FontKey {
    public final String fontFamily;
    public final String fontStyle;
    public final String fontVariant;
    public final String fontWeight;
    public final float fontSize;
    public final Set<Locale> locales;
    public final Integer superscript;

    /**
     * @param fontFamily
     * @param fontStyle
     * @param fontVariant
     * @param fontWeight
     * @param fontSize
     */
    public FontKey(final String fontFamily, final String fontStyle, final String fontVariant, final String fontWeight,
        final float fontSize, final Set<Locale> locales, final Integer superscript) {
      this.fontFamily = fontFamily == null ? null : fontFamily.intern();
      this.fontStyle = fontStyle == null ? null : fontStyle.intern();
      this.fontVariant = fontVariant == null ? null : fontVariant.intern();
      this.fontWeight = fontWeight == null ? null : fontWeight.intern();
      this.fontSize = fontSize;
      this.locales = locales;
      this.superscript = superscript;
    }

    @Override
    public boolean equals(final Object other) {
      if (other == this) {
        // Quick check.
        return true;
      }
      FontKey ors;
      try {
        ors = (FontKey) other;
      } catch (final ClassCastException cce) {
        // Not expected
        return false;
      }
      // Note that we use String.intern() for all string fields,
      // so we can do instance comparisons.
      return (this.fontSize == ors.fontSize) && (this.fontFamily == ors.fontFamily) && (this.fontStyle == ors.fontStyle)
          && (this.fontWeight == ors.fontWeight) && (this.fontVariant == ors.fontVariant) && (this.superscript == ors.superscript)
          && java.util.Objects.equals(this.locales, ors.locales);
    }

    private int cachedHash = -1;

    @Override
    public int hashCode() {
      int ch = this.cachedHash;
      if (ch != -1) {
        // Object is immutable - caching is ok.
        return ch;
      }
      String ff = this.fontFamily;
      if (ff == null) {
        ff = "";
      }
      String fw = this.fontWeight;
      if (fw == null) {
        fw = "";
      }
      String fs = this.fontStyle;
      if (fs == null) {
        fs = "";
      }
      final Integer ss = this.superscript;
      ch = ff.hashCode() ^ fw.hashCode() ^ fs.hashCode() ^ (int) this.fontSize ^ (ss == null ? 0 : ss.intValue());
      this.cachedHash = ch;
      return ch;
    }

    @Override
    public String toString() {
      return "FontKey[family=" + this.fontFamily + ",size=" + this.fontSize + ",style=" + this.fontStyle + ",weight=" + this.fontWeight
          + ",variant=" + this.fontVariant + ",superscript=" + this.superscript + "]";
    }
  }
}