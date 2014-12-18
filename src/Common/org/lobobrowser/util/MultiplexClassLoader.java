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
 * Created on Jun 19, 2005
 */
package org.lobobrowser.util;

import java.net.URL;
import java.util.Collection;

/**
 * @author J. H. S.
 */
public class MultiplexClassLoader extends BaseClassLoader {
  private static final ClassLoader[] EMPTY_CLASS_LOADERS = new ClassLoader[0];
  private final ClassLoader[] parentLoaders;

  /**
   * @param parent
   */
  public MultiplexClassLoader(final Collection<ClassLoader> classLoaders) {
    super(null);
    // TODO: Check why input parameter is not being used
    this.parentLoaders = classLoaders.toArray(EMPTY_CLASS_LOADERS);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
   */
  @Override
  public synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
    // First, check if the class has already been loaded
    Class<?> c = findLoadedClass(name);
    if (c == null) {
      try {
        final int len = this.parentLoaders.length;
        if (len == 0) {
          c = findSystemClass(name);
        } else {
          for (int i = 0; i < len; i++) {
            final ClassLoader parent = this.parentLoaders[i];
            try {
              c = parent.loadClass(name);
              if (c != null) {
                return c;
              }
            } catch (final ClassNotFoundException cnfe) {
              // ignore
            }
          }
        }
      } catch (final ClassNotFoundException e) {
        // If still not found, then invoke findClass in order
        // to find the class.
        c = findClass(name);
      }
      if (c == null) {
        c = findClass(name);
      }
    }
    if (resolve) {
      resolveClass(c);
    }
    return c;
  }

  @Override
  public URL getResource(final String name) {
    for (final ClassLoader loader : parentLoaders) {
      final URL url = loader.getResource(name);
      if (url != null) {
        return url;
      }
    }
    return null;
  }

}
