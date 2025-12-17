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
package org.lobobrowser.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

public class ClassLoaderObjectInputStream extends ObjectInputStream {

  private final ClassLoader classLoader;
  private final Class<?> expectedClass;

  public ClassLoaderObjectInputStream(
          final InputStream in,
          final ClassLoader classLoader) throws IOException {
    this(in, classLoader, null);
  }

  public ClassLoaderObjectInputStream(
          final InputStream in,
          final ClassLoader classLoader,
          final Class<?> expectedClass) throws IOException {
    super(in);
    this.classLoader = classLoader;
    this.expectedClass = expectedClass;
  }

  @Override
  protected Class<?> resolveClass(final ObjectStreamClass desc)
          throws IOException, ClassNotFoundException {

    Class<?> resolved =
        Class.forName(desc.getName(), false, this.classLoader);

    if (expectedClass != null && !expectedClass.isAssignableFrom(resolved)) {
      throw new InvalidClassException(
          "Unexpected class during deserialization",
          desc.getName()
      );
    }

    return resolved;
  }
}
