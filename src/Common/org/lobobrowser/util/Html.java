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
package org.lobobrowser.util;

public class Html {
  public static String textToHTML(final String text) {
    if (text == null) {
      return null;
    }
    final int length = text.length();
    boolean prevSlashR = false;
    final StringBuffer out = new StringBuffer();
    for (int i = 0; i < length; i++) {
      final char ch = text.charAt(i);
      switch (ch) {
      case '\r':
        if (prevSlashR) {
          out.append("<br>");
        }
        prevSlashR = true;
        break;
      case '\n':
        prevSlashR = false;
        out.append("<br>");
        break;
      case '"':
        if (prevSlashR) {
          out.append("<br>");
          prevSlashR = false;
        }
        out.append("&quot;");
        break;
      case '<':
        if (prevSlashR) {
          out.append("<br>");
          prevSlashR = false;
        }
        out.append("&lt;");
        break;
      case '>':
        if (prevSlashR) {
          out.append("<br>");
          prevSlashR = false;
        }
        out.append("&gt;");
        break;
      case '&':
        if (prevSlashR) {
          out.append("<br>");
          prevSlashR = false;
        }
        out.append("&amp;");
        break;
      default:
        if (prevSlashR) {
          out.append("<br>");
          prevSlashR = false;
        }
        out.append(ch);
        break;
      }
    }
    return out.toString();
  }

  /**
   * Escapes HTML special characters for safe interpolation in text nodes and
   * quoted attributes. Uses {@code &#39;} for single quotes because gngr's
   * {@code HtmlParser} does not define the {@code &apos;} named entity.
   */
  public static String escapeHtml(final String text) {
    if (text == null) {
      return "";
    }
    final int length = text.length();
    final StringBuilder out = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      final char ch = text.charAt(i);
      switch (ch) {
      case '&':
        out.append("&amp;");
        break;
      case '<':
        out.append("&lt;");
        break;
      case '>':
        out.append("&gt;");
        break;
      case '"':
        out.append("&quot;");
        break;
      case '\'':
        out.append("&#39;");
        break;
      default:
        out.append(ch);
        break;
      }
    }
    return out.toString();
  }
}
