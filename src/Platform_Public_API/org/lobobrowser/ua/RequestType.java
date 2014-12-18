/*
    GNU GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The Lobo Project

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public
    License as published by the Free Software Foundation; either
    version 2 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Contact info: lobochief@users.sourceforge.net
 */
package org.lobobrowser.ua;

/** Denotes a type of navigation request. */
public enum RequestType {
  /** Navigation is due to a user click. */
  CLICK,
  /**
   * Navigation is due to a programmatic call, such as a script invocation.
   */
  PROGRAMMATIC,
  /**
   * Navigation is due to a programmatic call, such as a script invocation, but
   * it was initiated by a user click.
   */
  PROGRAMMATIC_FROM_CLICK,
  /**
   * Navigation is due to user entering a location in the address bar.
   */
  ADDRESS_BAR,
  /** Navigation is due to a soft reload. */
  SOFT_RELOAD,
  /** Navigation is due to a hard reload. */
  HARD_RELOAD,
  /**
   * Navigation is due to back, forward, or other history item request.
   */
  HISTORY,
  /**
   * Reserved for downloads. It should not be seen in frame requests.
   */
  DOWNLOAD,
  /**
   * Programmatic navigation is intended to open a window.
   */
  OPEN_WINDOW,
  /**
   * Navigation is intended to open a window, but the action was initiated by a
   * user click.
   */
  OPEN_WINDOW_FROM_CLICK,
  /** Request loads frame content. */
  FRAME,
  /** Request is due to form submission */
  FORM,
  /**
   * Request is intended to load a page/document element, such as an image or
   * style sheet. This type of request should not be seen in frame navigation.
   */
  ELEMENT,
  /**
   * Not a request.
   */
  NONE
}
