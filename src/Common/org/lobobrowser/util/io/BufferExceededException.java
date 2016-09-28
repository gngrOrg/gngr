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
package org.lobobrowser.util.io;

public class BufferExceededException extends Exception {
  private static final long serialVersionUID = -3105309904365634760L;

  public BufferExceededException() {
    super();
  }

  public BufferExceededException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public BufferExceededException(final String message) {
    super(message);
  }

  public BufferExceededException(final Throwable cause) {
    super(cause);
  }
}
