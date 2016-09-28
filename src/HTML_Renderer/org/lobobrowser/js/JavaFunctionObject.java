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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lobobrowser.util.Objects;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrappedException;
import org.w3c.dom.DOMException;

public class JavaFunctionObject extends ScriptableObject implements Function {
  private static final long serialVersionUID = 3716471130167741876L;
  private static final Logger logger = Logger.getLogger(JavaFunctionObject.class.getName());
  private static final boolean loggableInfo = logger.isLoggable(Level.INFO);
  private final String methodName;
  private final String className;
  private final ArrayList<Method> methods = new ArrayList<>();

  public JavaFunctionObject(final String name, final String className) {
    super();
    this.methodName = name;
    this.className = className;

    // TODO: Review
    // Quick hack for issue #98
    defineProperty("call", new Callable() {

      public Object call(final Context cx, final Scriptable scope, final Scriptable thisObj, final Object[] args) {
        if ((args.length > 0) && (args[0] instanceof JavaObjectWrapper)) {
          final JavaObjectWrapper javaObjectWrapper = (JavaObjectWrapper) args[0];
          return JavaFunctionObject.this.call(cx, scope, javaObjectWrapper, Arrays.copyOfRange(args, 1, args.length));
        } else {
          throw new RuntimeException("Unexpected condition");
        }
      }

    }, org.mozilla.javascript.ScriptableObject.READONLY);
  }

  public void addMethod(final Method m) {
    this.methods.add(m);
  }

  @Override
  public String getClassName() {
    return "JavaFunctionObject";
  }

  /*
  private static String getTypeName(final Object object) {
    return object == null ? "[null]" : object.getClass().getName();
  }*/

  private final static class MethodAndArguments {
    private final Method method;
    private final Object[] args;

    public MethodAndArguments(final Method method, final Object[] args) {
      this.method = method;
      this.args = args;
    }

    public Object invoke(final Object javaObject) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      return method.invoke(javaObject, args);
    }

    @Override
    public String toString() {
      return "MethodAndArguments [method=" + method + ", args=" + Arrays.toString(args) + "]";
    }
  }

  private MethodAndArguments getExactMethod(final Object[] args) {
    final ArrayList<Method> methods = this.methods;
    final int size = methods.size();
    for (int i = 0; i < size; i++) {
      final Method m = methods.get(i);
      final Class<?>[] parameterTypes = m.getParameterTypes();
      if (args == null) {
        if ((parameterTypes == null) || (parameterTypes.length == 0)) {
          return new MethodAndArguments(m, null);
        }
      } else if (parameterTypes != null) {
        if (args.length == parameterTypes.length) {
          if (Objects.areSameTo(args, parameterTypes)) {
            return new MethodAndArguments(m, args);
          }
        } else if ((parameterTypes.length == 1) && parameterTypes[0].isArray()) {
          final Class<?> arrayType = parameterTypes[0].getComponentType();
          final boolean allSame = true;
          for (int j = 0; j < args.length; j++) {
            if (!Objects.isSameOrBox(args[j], arrayType)) {
              break;
            }
          }
          if (allSame) {
            final Object[] argsInArray = (Object[]) Array.newInstance(arrayType, args.length);
            for (int j = 0; j < args.length; j++) {
              argsInArray[j] = args[j];
            }
            return new MethodAndArguments(m, new Object[] { argsInArray });
          }

        }
      }
    }
    return null;
  }

  private MethodAndArguments getBestMethod(final Object[] args) {
    final MethodAndArguments exactMethod = getExactMethod(args);
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
          return new MethodAndArguments(m, new Object[0]);
        }
      } else if ((parameterTypes != null) && (args.length >= parameterTypes.length)) {
        if (Objects.areAssignableTo(args, parameterTypes)) {
          final Object[] actualArgs = convertArgs(args, parameterTypes.length, parameterTypes);
          return new MethodAndArguments(m, actualArgs);
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
    if (matchingMethod == null) {
      return null;
    } else {
      final Class<?>[] actualArgTypes = matchingMethod.getParameterTypes();
      final Object[] actualArgs = convertArgs(args, matchingNumParams, actualArgTypes);
      return new MethodAndArguments(matchingMethod, actualArgs);
    }
  }

  private static Object[] convertArgs(final Object[] args, final int numConvert, final Class<?>[] actualArgTypes) {
    final JavaScript manager = JavaScript.getInstance();
    final Object[] actualArgs = args == null ? new Object[0] : new Object[numConvert];
    if (args != null) {
      for (int i = 0; i < numConvert; i++) {
        final Object arg = args[i];
        actualArgs[i] = manager.getJavaObject(arg, actualArgTypes[i]);
      }
    }
    return actualArgs;
  }

  public Object call(final Context cx, final Scriptable scope, final Scriptable thisObj, final Object[] args) {
    final MethodAndArguments methodAndArguments = this.getBestMethod(args);
    if (methodAndArguments == null) {
      throw new EvaluatorException("No method matching " + this.methodName + " with " + (args == null ? 0 : args.length) + " arguments in "
          + className + " .");
    }
    final JavaScript manager = JavaScript.getInstance();
    try {
      if (thisObj instanceof JavaObjectWrapper) {
        final JavaObjectWrapper jcw = (JavaObjectWrapper) thisObj;
        // if(linfo) {
        // Object javaObject = jcw.getJavaObject();
        // logger.info("call(): Calling method " + method.getName() +
        // " on object " + javaObject + " of type " +
        // this.getTypeName(javaObject));
        // }
        final Object raw = methodAndArguments.invoke(jcw.getJavaObject());
        return manager.getJavascriptObject(raw, scope);
      } else {
        // if (args[0] instanceof Function ) {
        // Function func = (Function) args[0];
        // Object raw = func.call(cx, scope, scope, Arrays.copyOfRange(args, 1,
        // args.length));
        // return manager.getJavascriptObject(raw, scope);
        // } else {
        final Object raw = methodAndArguments.invoke(thisObj);
        return manager.getJavascriptObject(raw, scope);
        // }

        // Based on http://stackoverflow.com/a/16479685/161257
        // return call(cx, scope, getParentScope(), args);
      }
    } catch (final IllegalAccessException iae) {
      throw new IllegalStateException("Unable to call " + this.methodName + ".", iae);
    } catch (final InvocationTargetException ite) {
      if (ite.getCause() instanceof DOMException) {
        final DOMException domException = (DOMException) ite.getCause();
        throw new WrappedException(domException);
      }
      throw new WrappedException(
          new InvocationTargetException(ite.getCause(), "Unable to call " + this.methodName + " on " + thisObj + "."));
    } catch (final IllegalArgumentException iae) {
      final StringBuffer argTypes = new StringBuffer();
      for (int i = 0; i < methodAndArguments.args.length; i++) {
        if (i > 0) {
          argTypes.append(", ");
        }
        argTypes.append(methodAndArguments.args[i] == null ? "<null>" : methodAndArguments.args[i].getClass().getName());
      }
      throw new WrappedException(new IllegalArgumentException("Unable to call " + this.methodName + " in " + className
          + ". Argument types: " + argTypes + "." + "\n  on method: " + methodAndArguments.method,
          iae));
    }
  }

  @Override
  public java.lang.Object getDefaultValue(final java.lang.Class<?> hint) {
    if (loggableInfo) {
      logger.info("getDefaultValue(): hint=" + hint + ",this=" + this);
    }
    if ((hint == null) || String.class.equals(hint)) {
      return "function " + this.methodName;
    } else {
      return super.getDefaultValue(hint);
    }
  }

  public Scriptable construct(final Context cx, final Scriptable scope, final Object[] args) {
    throw new UnsupportedOperationException();
  }
}
