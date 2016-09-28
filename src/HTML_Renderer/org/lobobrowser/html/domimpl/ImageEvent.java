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
package org.lobobrowser.html.domimpl;

import java.util.EventObject;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.ua.ImageResponse;

public class ImageEvent extends EventObject {
  private static final long serialVersionUID = 6412430882522254948L;
  public @NonNull final ImageResponse imageResponse;

  public ImageEvent(final Object source, final @NonNull ImageResponse imageResponse) {
    super(source);
    this.imageResponse = imageResponse;
  }

  @Override
  public String toString() {
    return "Image Event: " + source + ", response: " + imageResponse;
  }
}
