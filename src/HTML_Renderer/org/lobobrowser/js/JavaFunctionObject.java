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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lobobrowser.util.Objects;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrappedException;

public class JavaFunctionObject extends ScriptableObject implements Function {
  private static final Logger logger = Logger.getLogger(JavaFunctionObject.class.getName());
  private static final boolean loggableInfo = logger.isLoggable(Level.INFO);
  private final String className;
  private final ArrayList<Method> methods = new ArrayList<>();

  public JavaFunctionObject(final String name) {
    super();
    this.className = name;
  }

  public void addMethod(final Method m) {
    this.methods.add(m);
  }

  @Override
  public String getClassName() {
    return "JavaFunctionObject";
  }

  private static String getTypeName(final Object object) {
    return object == null ? "[null]" : object.getClass().getName();
  }

  private Method getExactMethod(final Object[] args) {
    final ArrayList<Method> methods = this.methods;
    final int size = methods.size();
    for (int i = 0; i < size; i++) {
      final Method m = methods.get(i);
      final Class<?>[] parameterTypes = m.getParameterTypes();
      if (args == null) {
        if ((parameterTypes == null) || (parameterTypes.length == 0)) {
          return m;
        }
      } else if ((parameterTypes != null) && (args.length == parameterTypes.length)) {
        if (Objects.areSameTo(args, parameterTypes)) {
          return m;
        }
      }
    }
    return null;
  }

  private Method getBestMethod(final Object[] args) {
    final Method exactMethod = getExactMethod(args);
    if (exactMethod != null) {
      return exactMethod;
    }

    final ArrayList<Method> methods = this.methods;
    final int size = methods.size();
    int matchingNumParams = 0;
    Method matchingMethod = null;
    for (int i = 0; i < size; i++) {
      final Method m = methods.get(i);
      final Class<?>[] parameterTypes = m.getParameterTypes();
      if (args == null) {
        if ((parameterTypes == null) || (parameterTypes.length == 0)) {
          return m;
        }
      } else if ((parameterTypes != null) && (args.length >= parameterTypes.length)) {
        if (Objects.areAssignableTo(args, parameterTypes)) {
          return m;
        }
        if ((matchingMethod == null) || (parameterTypes.length > matchingNumParams)) {
          matchingNumParams = parameterTypes.length;
          matchingMethod = m;
        }
      }
    }
    if (size == 0) {
      throw new IllegalStateException("zero methods");
    }
    return matchingMethod;
  }

  public Object call(final Context cx, final Scriptable scope, final Scriptable thisObj, final Object[] args) {
    final Method method = this.getBestMethod(args);
    if (method == null) {
      throw new EvaluatorException("No method matching " + this.className + " with " + (args == null ? 0 : args.length) + " arguments.");
    }
    final Class<?>[] actualArgTypes = method.getParameterTypes();
    final int numParams = actualArgTypes.length;
    final Object[] actualArgs = args == null ? new Object[0] : new Object[numParams];
    final boolean linfo = loggableInfo;
    final JavaScript manager = JavaScript.getInstance();
    for (int i = 0; i < numParams; i++) {
      final Object arg = args[i];
      final Object actualArg = manager.getJavaObject(arg, actualArgTypes[i]);
      if (linfo) {
        logger.info("call(): For method=" + method.getName() + ": Converted arg=" + arg + " (type=" + getTypeName(arg)
            + ") into actualArg=" + actualArg + ". Type expected by method is " + actualArgTypes[i].getName() + ".");
      }
      actualArgs[i] = actualArg;
    }
    try {
      if (thisObj instanceof JavaObjectWrapper) {
        final JavaObjectWrapper jcw = (JavaObjectWrapper) thisObj;
        // if(linfo) {
        // Object javaObject = jcw.getJavaObject();
        // logger.info("call(): Calling method " + method.getName() +
        // " on object " + javaObject + " of type " +
        // this.getTypeName(javaObject));
        // }
        final Object raw = method.invoke(jcw.getJavaObject(), actualArgs);
        // System.out.println("Invoked.");
        return manager.getJavascriptObject(raw, scope);
      } else {
        // if (args[0] instanceof Function ) {
        // Function func = (Function) args[0];
        // Object raw = func.call(cx, scope, scope, Arrays.copyOfRange(args, 1,
        // args.length));
        // return manager.getJavascriptObject(raw, scope);
        // } else {
        final Object raw = method.invoke(thisObj, actualArgs);
        return manager.getJavascriptObject(raw, scope);
        // }

        // Based on http://stackoverflow.com/a/16479685/161257
        // return call(cx, scope, getParentScope(), args);
      }
    } catch (final IllegalAccessException iae) {
      throw new IllegalStateException("Unable to call " + this.className + ".", iae);
    } catch (final InvocationTargetException ite) {
      // throw new WrappedException(new
      // InvocationTargetException(ite.getCause(), "Unable to call " +
      // this.className + " on " + jcw.getJavaObject() + "."));
      throw new WrappedException(new InvocationTargetException(ite.getCause(), "Unable to call " + this.className + " on " + thisObj + "."));
    } catch (final IllegalArgumentException iae) {
      final StringBuffer argTypes = new StringBuffer();
      for (int i = 0; i < actualArgs.length; i++) {
        if (i > 0) {
          argTypes.append(", ");
        }
        argTypes.append(actualArgs[i] == null ? "<null>" : actualArgs[i].getClass().getName());
      }
      throw new WrappedException(new IllegalArgumentException("Unable to call " + this.className + ". Argument types: " + argTypes + ".",
          iae));
    }
  }

  @Override
  public java.lang.Object getDefaultValue(final java.lang.Class<?> hint) {
    if (loggableInfo) {
      logger.info("getDefaultValue(): hint=" + hint + ",this=" + this);
    }
    if ((hint == null) || String.class.equals(hint)) {
      return "function " + this.className;
    } else {
      return super.getDefaultValue(hint);
    }
  }

  public Scriptable construct(final Context cx, final Scriptable scope, final Object[] args) {
    throw new UnsupportedOperationException();
  }
}
