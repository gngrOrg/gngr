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

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrappedException;

public class JavaObjectWrapper extends ScriptableObject {
  private static final Logger logger = Logger.getLogger(JavaObjectWrapper.class.getName());
  private static final boolean loggableInfo = logger.isLoggable(Level.INFO);
  private final Object delegate;
  private final JavaClassWrapper classWrapper;

  @Override
  public void setParentScope(final Scriptable m) {
    if (m == this) {
      // TODO: This happens when running jQuery 2
      super.setParentScope(null);
    } else {
      super.setParentScope(m);
    }
  }

  public JavaObjectWrapper(final JavaClassWrapper classWrapper) throws InstantiationException, IllegalAccessException {
    this.classWrapper = classWrapper;
    // Retaining a strong reference, but note
    // that the object wrapper map uses weak keys
    // and weak values.
    final Object delegate = this.classWrapper.newInstance();
    this.delegate = delegate;
  }

  public JavaObjectWrapper(final JavaClassWrapper classWrapper, final Object delegate) {
    if (delegate == null) {
      throw new IllegalArgumentException("Argument delegate cannot be null.");
    }
    this.classWrapper = classWrapper;
    // Retaining a strong reference, but note
    // that the object wrapper map uses weak keys
    // and weak values.
    this.delegate = delegate;
  }

  /**
   * Returns the Java object.
   *
   * @return An object or <code>null</code> if garbage collected.
   */
  public Object getJavaObject() {
    // Cannot retain delegate with a strong reference.
    return this.delegate;
  }

  @Override
  public String getClassName() {
    return this.classWrapper.getClassName();
  }

  @Override
  public Object get(final int index, final Scriptable start) {
    final PropertyInfo pinfo = this.classWrapper.getIntegerIndexer();
    if (pinfo == null) {
      return super.get(index, start);
    } else {
      try {
        final Method getter = pinfo.getGetter();
        if (getter == null) {
          throw new EvaluatorException("Indexer is write-only");
        }
        // Cannot retain delegate with a strong reference.
        final Object javaObject = this.getJavaObject();
        if (javaObject == null) {
          throw new IllegalStateException("Java object (class=" + this.classWrapper + ") is null.");
        }
        final Object raw = getter.invoke(javaObject, new Object[] { new Integer(index) });
        if (raw == null) {
          // Return this instead of null.
          return Scriptable.NOT_FOUND;
        }
        return JavaScript.getInstance().getJavascriptObject(raw, this.getParentScope());
      } catch (final Exception err) {
        throw new WrappedException(err);
      }
    }
  }

  @Override
  public Object get(final String name, final Scriptable start) {
    final PropertyInfo pinfo = this.classWrapper.getProperty(name);
    if (pinfo != null) {
      final Method getter = pinfo.getGetter();
      if (getter == null) {
        throw new EvaluatorException("Property '" + name + "' is not readable");
      }
      try {
        // Cannot retain delegate with a strong reference.
        final Object javaObject = this.getJavaObject();
        if (javaObject == null) {
          throw new IllegalStateException("Java object (class=" + this.classWrapper + ") is null.");
        }
        final Object val = getter.invoke(javaObject, (Object[]) null);
        return JavaScript.getInstance().getJavascriptObject(val, start.getParentScope());
      } catch (final Exception err) {
        throw new WrappedException(err);
      }
    } else {
      final Function f = this.classWrapper.getFunction(name);
      if (f != null) {
        return f;
      } else {
        // Should check properties set in context
        // first. Consider element IDs should not
        // override Window variables set by user.
        final Object result = super.get(name, start);
        if (result != Scriptable.NOT_FOUND) {
          return result;
        }
        final PropertyInfo ni = this.classWrapper.getNameIndexer();
        if (ni != null) {
          final Method getter = ni.getGetter();
          if (getter != null) {
            // Cannot retain delegate with a strong reference.
            final Object javaObject = this.getJavaObject();
            if (javaObject == null) {
              throw new IllegalStateException("Java object (class=" + this.classWrapper + ") is null.");
            }
            try {
              final Object val = getter.invoke(javaObject, new Object[] { name });
              if (val == null) {
                // There might not be an indexer setter.
                return super.get(name, start);
              } else {
                return JavaScript.getInstance().getJavascriptObject(val, start.getParentScope());
              }
            } catch (final Exception err) {
              throw new WrappedException(err);
            }
          }
        }
        return Scriptable.NOT_FOUND;
      }
    }
  }

  @Override
  public void put(final int index, final Scriptable start, final Object value) {
    final PropertyInfo pinfo = this.classWrapper.getIntegerIndexer();
    if (pinfo == null) {
      super.put(index, start, value);
    } else {
      try {
        final Method setter = pinfo.getSetter();
        if (setter == null) {
          throw new EvaluatorException("Indexer is read-only");
        }
        Object actualValue;
        actualValue = JavaScript.getInstance().getJavaObject(value, pinfo.getPropertyType());
        setter.invoke(this.getJavaObject(), new Object[] { new Integer(index), actualValue });
      } catch (final Exception err) {
        throw new WrappedException(err);
      }
    }
  }

  @Override
  public void put(final String name, final Scriptable start, final Object value) {
    if (value instanceof org.mozilla.javascript.Undefined) {
      super.put(name, start, value);
    } else {
      final PropertyInfo pinfo = this.classWrapper.getProperty(name);
      if (pinfo != null) {
        final Method setter = pinfo.getSetter();
        if (setter == null) {
          throw new EvaluatorException("Property '" + name + "' is not settable in " + this.classWrapper.getClassName() + ".");
        }
        try {
          Object actualValue;
          actualValue = JavaScript.getInstance().getJavaObject(value, pinfo.getPropertyType());
          setter.invoke(this.getJavaObject(), new Object[] { actualValue });
        } catch (final IllegalArgumentException iae) {
          final Exception newException = new IllegalArgumentException("Property named '" + name + "' could not be set with value " + value
              + ".",
              iae);
          throw new WrappedException(newException);
        } catch (final Exception err) {
          throw new WrappedException(err);
        }
      } else {
        final PropertyInfo ni = this.classWrapper.getNameIndexer();
        if (ni != null) {
          final Method setter = ni.getSetter();
          if (setter != null) {
            try {
              Object actualValue;
              actualValue = JavaScript.getInstance().getJavaObject(value, ni.getPropertyType());
              setter.invoke(this.getJavaObject(), new Object[] { name, actualValue });
            } catch (final Exception err) {
              throw new WrappedException(err);
            }
          } else {
            super.put(name, start, value);
          }
        } else {
          super.put(name, start, value);
        }
      }
    }
  }

  public static Function getConstructor(final String className, final JavaClassWrapper classWrapper, final Scriptable scope) {
    return new JavaConstructorObject(className, classWrapper);
  }

  public static Function getConstructor(final String className, final JavaClassWrapper classWrapper, final Scriptable scope,
      final JavaInstantiator instantiator) {
    return new JavaConstructorObject(className, classWrapper, instantiator);
  }

  @Override
  public java.lang.Object getDefaultValue(final java.lang.Class hint) {
    if (loggableInfo) {
      logger.info("getDefaultValue(): hint=" + hint + ",this=" + this.getJavaObject());
    }
    if ((hint == null) || String.class.equals(hint)) {
      final Object javaObject = this.getJavaObject();
      if (javaObject == null) {
        throw new IllegalStateException("Java object (class=" + this.classWrapper + ") is null.");
      }
      return javaObject.toString();
    } else if (Number.class.isAssignableFrom(hint)) {
      final Object javaObject = this.getJavaObject();
      if (javaObject instanceof Number) {
        return javaObject;
      } else if (javaObject instanceof String) {
        return Double.valueOf((String) javaObject);
      } else {
        return super.getDefaultValue(hint);
      }
    } else {
      return super.getDefaultValue(hint);
    }
  }

  @Override
  public String toString() {
    final Object javaObject = this.getJavaObject();
    final String type = javaObject == null ? "<null>" : javaObject.getClass().getName();
    return "JavaObjectWrapper[object=" + this.getJavaObject() + ",type=" + type + "]";
  }

  @Override
  public boolean hasInstance(final Scriptable instance) {
    if ((instance instanceof JavaObjectWrapper) && (this.getJavaObject() instanceof Class)) {
      final JavaObjectWrapper instanceObj = (JavaObjectWrapper) instance;
      final Class myClass = (Class) this.getJavaObject();
      return myClass.isInstance(instanceObj.getJavaObject());
    } else {
      return super.hasInstance(instance);
    }
  }
}
