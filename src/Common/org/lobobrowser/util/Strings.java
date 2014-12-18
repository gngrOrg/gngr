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
package org.lobobrowser.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class Strings {
  private static final MessageDigest MESSAGE_DIGEST;
  public static final String[] EMPTY_ARRAY = new String[0];

  static {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (final NoSuchAlgorithmException err) {
      throw new IllegalStateException();
    }
    MESSAGE_DIGEST = md;
  }

  private Strings() {
  }

  public static int compareVersions(final String version1, final String version2, final boolean startsWithDigits) {
    if (version1 == null) {
      return version2 == null ? 0 : -1;
    } else if (version2 == null) {
      return +1;
    }
    if (startsWithDigits) {
      final String v1prefix = leadingDigits(version1);
      final String v2prefix = leadingDigits(version2);
      if (v1prefix.length() == 0) {
        if (v2prefix.length() == 0) {
          return 0;
        }
        return -1;
      } else if (v2prefix.length() == 0) {
        return +1;
      }
      int diff;
      try {
        diff = Integer.parseInt(v1prefix) - Integer.parseInt(v2prefix);
      } catch (final NumberFormatException nfe) {
        diff = 0;
      }
      if (diff == 0) {
        return compareVersions(version1.substring(v1prefix.length()), version2.substring(v2prefix.length()), false);
      }
      return diff;
    } else {
      final String v1prefix = leadingNonDigits(version1);
      final String v2prefix = leadingNonDigits(version2);
      if (v1prefix.length() == 0) {
        if (v2prefix.length() == 0) {
          return 0;
        }
        return -1;
      } else if (v2prefix.length() == 0) {
        return +1;
      }
      final int diff = v1prefix.compareTo(v2prefix);
      if (diff == 0) {
        return compareVersions(version1.substring(v1prefix.length()), version2.substring(v2prefix.length()), true);
      }
      return diff;
    }
  }

  public static String leadingDigits(final String text) {
    final int length = text.length();
    StringBuffer buffer = null;
    for (int i = 0; i < length; i++) {
      final char ch = text.charAt(i);
      if (!Character.isDigit(ch)) {
        break;
      }
      if (buffer == null) {
        buffer = new StringBuffer(3);
      }
      buffer.append(ch);
    }
    return buffer == null ? "" : buffer.toString();
  }

  public static String leadingNonDigits(final String text) {
    final int length = text.length();
    StringBuffer buffer = null;
    for (int i = 0; i < length; i++) {
      final char ch = text.charAt(i);
      if (Character.isDigit(ch)) {
        break;
      }
      if (buffer == null) {
        buffer = new StringBuffer(3);
      }
      buffer.append(ch);
    }
    return buffer == null ? "" : buffer.toString();
  }

  public static boolean isBlank(final String text) {
    return (text == null) || "".equals(text);
  }

  public static int countLines(final String text) {
    int startIdx = 0;
    int lineCount = 1;
    for (;;) {
      final int lbIdx = text.indexOf('\n', startIdx);
      if (lbIdx == -1) {
        break;
      }
      lineCount++;
      startIdx = lbIdx + 1;
    }
    return lineCount;
  }

  public static boolean isJavaIdentifier(final String id) {
    if (id == null) {
      return false;
    }
    final int len = id.length();
    if (len == 0) {
      return false;
    }
    if (!Character.isJavaIdentifierStart(id.charAt(0))) {
      return false;
    }
    for (int i = 1; i < len; i++) {
      if (!Character.isJavaIdentifierPart(id.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  public static String getJavaStringLiteral(final String text) {
    final StringBuffer buf = new StringBuffer();
    buf.append('"');
    final int len = text.length();
    for (int i = 0; i < len; i++) {
      final char ch = text.charAt(i);
      switch (ch) {
      case '\\':
        buf.append("\\\\");
        break;
      case '\n':
        buf.append("\\n");
        break;
      case '\r':
        buf.append("\\r");
        break;
      case '\t':
        buf.append("\\t");
        break;
      case '"':
        buf.append("\\\"");
        break;
      default:
        buf.append(ch);
        break;
      }
    }
    buf.append('"');
    return buf.toString();
  }

  public static String getJavaIdentifier(final String candidateID) {
    final int len = candidateID.length();
    final StringBuffer buf = new StringBuffer();
    for (int i = 0; i < len; i++) {
      final char ch = candidateID.charAt(i);
      final boolean good = i == 0 ? Character.isJavaIdentifierStart(ch) : Character.isJavaIdentifierPart(ch);
      if (good) {
        buf.append(ch);
      } else {
        buf.append('_');
      }
    }
    return buf.toString();
  }

  private static final String HEX_CHARS = "0123456789ABCDEF";

  public static String getMD5(final String source) {
    byte[] bytes;
    try {
      bytes = source.getBytes("UTF-8");
    } catch (final java.io.UnsupportedEncodingException ue) {
      throw new IllegalStateException(ue);
    }
    byte[] result;
    synchronized (MESSAGE_DIGEST) {
      MESSAGE_DIGEST.update(bytes);
      result = MESSAGE_DIGEST.digest();
    }
    final char[] resChars = new char[32];
    final int len = result.length;
    for (int i = 0; i < len; i++) {
      final byte b = result[i];
      final int lo4 = b & 0x0F;
      final int hi4 = (b & 0xF0) >> 4;
      resChars[i * 2] = HEX_CHARS.charAt(hi4);
      resChars[(i * 2) + 1] = HEX_CHARS.charAt(lo4);
    }
    return new String(resChars);
  }

  public static String getHash32(final String source) throws UnsupportedEncodingException {
    final String md5 = getMD5(source);
    return md5.substring(0, 8);
  }

  public static String getHash64(final String source) throws UnsupportedEncodingException {
    final String md5 = getMD5(source);
    return md5.substring(0, 16);
  }

  public static int countChars(final String text, final char ch) {
    final int len = text.length();
    int count = 0;
    for (int i = 0; i < len; i++) {
      if (ch == text.charAt(i)) {
        count++;
      }
    }
    return count;
  }

  // public static boolean isTrimmable(char ch) {
  // switch(ch) {
  // case ' ':
  // case '\t':
  // case '\r':
  // case '\n':
  // return true;
  // }
  // return false;
  // }
  //
  // /**
  // * Trims blanks, line breaks and tabs.
  // * @param text
  // * @return
  // */
  // public static String trim(String text) {
  // int len = text.length();
  // int startIdx;
  // for(startIdx = 0; startIdx < len; startIdx++) {
  // char ch = text.charAt(startIdx);
  // if(!isTrimmable(ch)) {
  // break;
  // }
  // }
  // int endIdx;
  // for(endIdx = len; --endIdx > startIdx; ) {
  // char ch = text.charAt(endIdx);
  // if(!isTrimmable(ch)) {
  // break;
  // }
  // }
  // return text.substring(startIdx, endIdx + 1);
  // }

  /**
   * Removes the double-quotes at the beginning and end of a string. If input
   * string doesn't have the double-quote character at beginning or end, it is
   * returned unchanged.
   */
  public static String unquote(final String text) {
    final int length = text.length();
    if (length >= 2) {
      if ((text.charAt(0) == '"') && (text.charAt(length - 1) == '"')) {
        return text.substring(1, length - 1);
      }
    }
    return text;
  }

  /**
   * Removes the single-quotes at the beginning and end of a string. If input
   * string doesn't have the single-quote character at beginning or end, it is
   * returned unchanged.
   */
  public static String unquoteSingle(final String text) {
    final int length = text.length();
    if (length >= 2) {
      if ((text.charAt(0) == '\'') && (text.charAt(length - 1) == '\'')) {
        return text.substring(1, length - 1);
      }
    }
    return text;
  }

  public static String[] split(final String phrase) {
    final int length = phrase.length();
    final ArrayList<String> wordList = new ArrayList<>();
    StringBuffer word = null;
    for (int i = 0; i < length; i++) {
      final char ch = phrase.charAt(i);
      switch (ch) {
      case ' ':
      case '\t':
      case '\r':
      case '\n':
        if (word != null) {
          wordList.add(word.toString());
          word = null;
        }
        break;
      default:
        if (word == null) {
          word = new StringBuffer();
        }
        word.append(ch);
      }
    }
    if (word != null) {
      wordList.add(word.toString());
    }
    return wordList.toArray(EMPTY_ARRAY);
  }

  public static String truncate(final String text, final int maxLength) {
    if (text == null) {
      return null;
    }
    if (text.length() <= maxLength) {
      return text;
    }
    return text.substring(0, Math.max(maxLength - 3, 0)) + "...";
  }

  public static String strictHtmlEncode(final String rawText, final boolean quotes) {
    final StringBuffer output = new StringBuffer();
    final int length = rawText.length();
    for (int i = 0; i < length; i++) {
      final char ch = rawText.charAt(i);
      switch (ch) {
      case '&':
        output.append("&amp;");
        break;
      case '"':
        if (quotes) {
          output.append("&quot;");
        } else {
          output.append(ch);
        }
        break;
      case '<':
        output.append("&lt;");
        break;
      case '>':
        output.append("&gt;");
        break;
      default:
        output.append(ch);
      }
    }
    return output.toString();
  }

  public static String trimForAlphaNumDash(final String rawText) {
    final int length = rawText.length();
    for (int i = 0; i < length; i++) {
      final char ch = rawText.charAt(i);
      if (((ch >= 'a') && (ch <= 'z')) || ((ch >= 'A') && (ch <= 'Z')) || ((ch >= '0') && (ch <= '9')) || (ch == '-')) {
        continue;
      }
      return rawText.substring(0, i);
    }
    return rawText;
  }

  public static String getCRLFString(final String original) {
    if (original == null) {
      return null;
    }
    final int length = original.length();
    final StringBuffer buffer = new StringBuffer();
    boolean lastSlashR = false;
    for (int i = 0; i < length; i++) {
      final char ch = original.charAt(i);
      switch (ch) {
      case '\r':
        lastSlashR = true;
        break;
      case '\n':
        lastSlashR = false;
        buffer.append("\r\n");
        break;
      default:
        if (lastSlashR) {
          lastSlashR = false;
          buffer.append("\r\n");
        }
        buffer.append(ch);
        break;
      }
    }
    return buffer.toString();
  }
}
