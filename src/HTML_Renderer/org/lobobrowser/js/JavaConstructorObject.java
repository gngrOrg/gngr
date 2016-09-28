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
package org.lobobrowser.js;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrappedException;
import org.w3c.dom.DOMException;

public class JavaConstructorObject extends ScriptableObject implements Function {
  private static final long serialVersionUID = 5536902327235533789L;
  private final JavaClassWrapper classWrapper;
  private final JavaInstantiator instantiator;
  private final String name;

  public JavaConstructorObject(final String name, final JavaClassWrapper classWrapper) {
    this.name = name;
    this.classWrapper = classWrapper;
    this.instantiator = new SimpleInstantiator(classWrapper);
  }

  public JavaConstructorObject(final String name, final JavaClassWrapper classWrapper, final JavaInstantiator instantiator) {
    this.name = name;
    this.classWrapper = classWrapper;
    this.instantiator = instantiator;
  }

  @Override
  public String getClassName() {
    return this.name;
  }

  public Object call(final Context cx, final Scriptable scope, final Scriptable thisObj, final Object[] args) {
    // TODO: Implement this, or atleast remove the wrapped exception.
    //       The exception is being wrapped so that web-platform-tests don't timeout; timeouts are slowing down testing.
    throw new WrappedException(new UnsupportedOperationException());
  }

  public Scriptable construct(final Context cx, final Scriptable scope, final Object[] args) {
    try {
      final Object javaObject = this.instantiator.newInstance(args);
      final Scriptable newObject = new JavaObjectWrapper(this.classWrapper, javaObject);
      newObject.setParentScope(scope);
      return newObject;
    } catch (final DOMException err) {
      throw new WrappedException(err);
    } catch (final EcmaError err) {
      throw err;
    } catch (final Exception err) {
      throw new IllegalStateException(err);
    }
  }

  @Override
  public java.lang.Object getDefaultValue(final java.lang.Class<?> hint) {
    // null is passed as hint when converting to string, hence adding it as an extra condition.
    if (String.class.equals(hint) || (hint == null)) {
      return "function " + this.name;
    } else {
      return super.getDefaultValue(hint);
    }
  }

  public static class SimpleInstantiator implements JavaInstantiator {
    private final JavaClassWrapper classWrapper;

    public SimpleInstantiator(final JavaClassWrapper classWrapper) {
      super();
      this.classWrapper = classWrapper;
    }

    public Object newInstance(final Object[] args) throws InstantiationException, IllegalAccessException {
      return this.classWrapper.newInstance();
    }
  }

  @Override
  public boolean hasInstance(final Scriptable instance) {
    return classWrapper.hasInstance(instance);
  }
}
