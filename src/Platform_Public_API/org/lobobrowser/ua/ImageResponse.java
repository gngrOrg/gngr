/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2015 Uproot Labs India Pvt Ltd

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

 */

package org.lobobrowser.ua;

import java.awt.Image;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

public final class ImageResponse {
  public static enum State {loading, loaded, error}

  public final State state;
  public final @Nullable Image img;

  public ImageResponse() {
    this.state = State.loading;
    this.img = null;
  }

  public ImageResponse(final State state, final @Nullable Image img) {
    this.state = state;
    this.img = img;
  }

  public boolean isDecoded() {
    if (state == State.loaded) {
      assert(img != null);
      @NonNull Image imgLocal = img;
      return imgLocal.getWidth(null) >= 0 && imgLocal.getHeight(null) >= 0;
    } else {
      return false;
    }
  }

  public boolean isReadyToPaint() {
    if (state == State.loaded) {
      assert(img != null);
      @NonNull Image imgLocal = img;
      return imgLocal.getWidth(null) >= 0 && imgLocal.getHeight(null) >= 0;
    } else {
      return state != State.loading;
    }
  }

  @Override
  public String toString() {
    return "ImageResponse: " + state + ", " + img;
  }
}
