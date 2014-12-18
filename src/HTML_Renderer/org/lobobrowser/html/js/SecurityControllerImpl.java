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
package org.lobobrowser.html.js;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.GeneratedClassLoader;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.SecurityController;

public class SecurityControllerImpl extends SecurityController {
  private final java.net.URL url;
  private final java.security.Policy policy;
  private final CodeSource codesource;

  public SecurityControllerImpl(final java.net.URL url, final Policy policy) {
    this.url = url;
    this.policy = policy;
    this.codesource = new CodeSource(this.url, (java.security.cert.Certificate[]) null);
  }

  @Override
  public Object callWithDomain(final Object securityDomain, final Context ctx, final Callable callable, final Scriptable scope,
      final Scriptable thisObj, final Object[] args) {
    if (securityDomain == null) {
      // TODO: Investigate
      return callable.call(ctx, scope, thisObj, args);
    } else {
      final PrivilegedAction<?> action = () -> callable.call(ctx, scope, thisObj, args);
      final ProtectionDomain protectionDomain = (ProtectionDomain) securityDomain;
      final AccessControlContext acctx = new AccessControlContext(new ProtectionDomain[] { protectionDomain });
      return AccessController.doPrivileged(action, acctx);
    }
  }

  @Override
  public GeneratedClassLoader createClassLoader(final ClassLoader parent, final Object staticDomain) {
    return new LocalSecureClassLoader(parent);
  }

  @Override
  public Object getDynamicSecurityDomain(final Object securityDomain) {
    final Policy policy = this.policy;
    if (policy == null) {
      // TODO: The check for null may not be required anymore.
      throw new RuntimeException("No policy has been set in a security controller!");
      // return Policy.getPolicy();
      // return null;
    } else {
      final PermissionCollection permissions = this.policy.getPermissions(codesource);
      return new ProtectionDomain(codesource, permissions);
    }
  }

  private class LocalSecureClassLoader extends SecureClassLoader implements GeneratedClassLoader {
    public LocalSecureClassLoader(final ClassLoader parent) {
      super(parent);
    }

    public Class<?> defineClass(final String name, final byte[] b) {
      return this.defineClass(name, b, 0, b.length, codesource);
    }

    public void linkClass(final Class<?> clazz) {
      super.resolveClass(clazz);
    }
  }
}
