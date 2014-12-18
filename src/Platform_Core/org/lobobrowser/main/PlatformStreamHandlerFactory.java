/*
    GNU GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The Lobo Project

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public
    License as published by the Free Software Foundation; either
    verion 2 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Contact info: lobochief@users.sourceforge.net
 */
package org.lobobrowser.main;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Global URL stream handler factory used by the browser.
 *
 * @see PlatformInit#initProtocols()
 */
public class PlatformStreamHandlerFactory implements URLStreamHandlerFactory {
  private static final PlatformStreamHandlerFactory instance = new PlatformStreamHandlerFactory();
  private final Collection<URLStreamHandlerFactory> factories = new ArrayList<>();

  public static PlatformStreamHandlerFactory getInstance() {
    return instance;
  }

  public void addFactory(final URLStreamHandlerFactory factory) {
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkSetFactory();
    }
    final Collection<URLStreamHandlerFactory> factories = this.factories;
    synchronized (factories) {
      factories.add(factory);
    }
  }

  public URLStreamHandler createURLStreamHandler(final String protocol) {
    final Collection<URLStreamHandlerFactory> factories = this.factories;
    synchronized (factories) {
      for (final URLStreamHandlerFactory f : factories) {
        final URLStreamHandler handler = f.createURLStreamHandler(protocol);
        if (handler != null) {
          return handler;
        }
      }
    }
    return null;
  }
}
