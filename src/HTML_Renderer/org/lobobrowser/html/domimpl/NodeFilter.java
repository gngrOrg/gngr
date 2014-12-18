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
 * Created on Oct 8, 2005
 */
package org.lobobrowser.html.domimpl;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.html.HTMLFrameElement;
import org.w3c.dom.html.HTMLIFrameElement;
import org.w3c.dom.html.HTMLLinkElement;

public interface NodeFilter {
  public boolean accept(Node node);

  static final class ImageFilter implements NodeFilter {
    public boolean accept(final Node node) {
      return "IMG".equalsIgnoreCase(node.getNodeName());
    }
  }

  static final class AppletFilter implements NodeFilter {
    public boolean accept(final Node node) {
      // TODO: "OBJECT" elements that are applets too.
      return "APPLET".equalsIgnoreCase(node.getNodeName());
    }
  }

  static final class LinkFilter implements NodeFilter {
    public boolean accept(final Node node) {
      return node instanceof HTMLLinkElement;
    }
  }

  static final class AnchorFilter implements NodeFilter {
    public boolean accept(final Node node) {
      final String nodeName = node.getNodeName();
      return "A".equalsIgnoreCase(nodeName) || "ANCHOR".equalsIgnoreCase(nodeName);
    }
  }

  static final class FormFilter implements NodeFilter {
    public boolean accept(final Node node) {
      final String nodeName = node.getNodeName();
      return "FORM".equalsIgnoreCase(nodeName);
    }
  }

  static final class FrameFilter implements NodeFilter {
    public boolean accept(final Node node) {
      return (node instanceof HTMLFrameElement) || (node instanceof HTMLIFrameElement);
    }
  }

  // private class BodyFilter implements NodeFilter {
  // public boolean accept(Node node) {
  // return node instanceof org.w3c.dom.html2.HTMLBodyElement;
  // }
  // }

  static final class ElementNameFilter implements NodeFilter {
    private final String name;

    public ElementNameFilter(final String name) {
      this.name = name;
    }

    public boolean accept(final Node node) {
      // TODO: Case sensitive?
      return (node instanceof Element) && this.name.equals(((Element) node).getAttribute("name"));
    }
  }

  static final class ElementFilter implements NodeFilter {
    public ElementFilter() {
    }

    public boolean accept(final Node node) {
      return node instanceof Element;
    }
  }

  static final class TagNameFilter implements NodeFilter {
    private final String name;

    public TagNameFilter(final String name) {
      this.name = name;
    }

    public boolean accept(final Node node) {
      if (!(node instanceof Element)) {
        return false;
      }
      final String n = this.name;
      return n.equalsIgnoreCase(((Element) node).getTagName());
    }
  }

}
