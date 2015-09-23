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

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import org.lobobrowser.util.Objects;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

public class JavaScript {
  // TODO: Should the instance be specific to thread? GH #122
  private static JavaScript instance = new JavaScript();
  // objectMap must be a map that uses weak keys
  // and refers to values using weak references.
  // Keys are java objects other than ScriptableDelegate instances.
  private final WeakHashMap<Object, WeakReference<JavaObjectWrapper>> javaObjectToWrapper = new WeakHashMap<>();

  public static JavaScript getInstance() {
    return instance;
  }

  public static void init() {
    ContextFactory.initGlobal(new ContextFactory() {
      @Override
      protected Context makeContext() {
        throw new UnsupportedOperationException("Internal error. Global context factory should not be used.");
      }
    });
  }

  /**
   * Returns an object that may be used by the Javascript engine.
   *
   * @param raw
   * @return
   */
  public Object getJavascriptObject(final Object raw, final Scriptable scope) {
    if ((raw instanceof String) || (raw instanceof Scriptable)) {
      return raw;
    } else if (raw == null) {
      return null;
    } else if (raw.getClass().isPrimitive()) {
      return raw;
    } else if (raw.getClass().isArray()) {
      return raw;
    } else if (raw instanceof ScriptableDelegate) {
      // Classes that implement ScriptableDelegate retain
      // the JavaScript object. Reciprocal linking cannot
      // be done with weak hash maps and without leaking.
      synchronized (this) {
        final ScriptableDelegate delegate = (ScriptableDelegate) raw;
        Scriptable javascriptObject = delegate.getScriptable();
        if (javascriptObject == null) {
          final JavaObjectWrapper jow = new JavaObjectWrapper(JavaClassWrapperFactory.getInstance().getClassWrapper(raw.getClass()), raw);
          javascriptObject = jow;
          jow.setParentScope(scope);
          delegate.setScriptable(jow);
        } else {
          javascriptObject.setParentScope(scope);
        }
        return javascriptObject;
      }
    } else if (Objects.isBoxClass(raw.getClass())) {
      return raw;
    } else {
      synchronized (this.javaObjectToWrapper) {
        // WeakHashMaps will retain keys if the value refers to the key.
        // That's why we need to refer to the value weakly too.
        // TODO: The above comment should probably be moved to the context of the code further below
        //       where the value is being inserted into the map.
        //       This part of the code is simply querying an existing value.
        final WeakReference<?> valueRef = this.javaObjectToWrapper.get(raw);
        JavaObjectWrapper jow = null;
        if (valueRef != null) {
          jow = (JavaObjectWrapper) valueRef.get();
        }
        if (jow == null) {
          final Class<? extends Object> javaClass = raw.getClass();
          final JavaClassWrapper wrapper = JavaClassWrapperFactory.getInstance().getClassWrapper(javaClass);
          jow = new JavaObjectWrapper(wrapper, raw);
          this.javaObjectToWrapper.put(raw, new WeakReference<>(jow));
        }
        jow.setParentScope(scope);
        return jow;
      }
    }
  }

  /*
  private static String getStringValue(final Object object) {
    if (object instanceof Undefined) {
      return "undefined";
    } else if (object instanceof Scriptable) {
      return (String) ((Scriptable) object).getDefaultValue(String.class);
    } else {
      return String.valueOf(object);
    }
  }
  */

  public Object getJavaObject(final Object javascriptObject, final Class<?> type) {
    if (javascriptObject instanceof JavaObjectWrapper) {
      final Object rawJavaObject = ((JavaObjectWrapper) javascriptObject).getJavaObject();
      if (String.class == type) {
        return String.valueOf(rawJavaObject);
      } else {
        return rawJavaObject;
      }
    }
    // Relying on Rhino to do the right thing.
    // TODO: Remove the older implementation archived below.
    return Context.jsToJava(javascriptObject, type);
  }

  /*
  public Object getJavaObject(final Object javascriptObject, final Class<?> type) {
    if (type == Boolean.TYPE) {
      if ((javascriptObject == null) || (javascriptObject == Undefined.instance)) {
        return Boolean.FALSE;
      }
    }
    if (javascriptObject instanceof JavaObjectWrapper) {
      final Object rawJavaObject = ((JavaObjectWrapper) javascriptObject).getJavaObject();
      if (String.class == type) {
        return String.valueOf(rawJavaObject);
      } else {
        return rawJavaObject;
      }
    } else if (javascriptObject == null) {
      return null;
    } else if (type == String.class) {
      if (javascriptObject instanceof String) {
        return javascriptObject;
      } else if (javascriptObject instanceof Double) {
        final String text = String.valueOf(javascriptObject);
        if (text.endsWith(".0")) {
          return text.substring(0, text.length() - 2);
        } else {
          return text;
        }
      } else {
        return getStringValue(javascriptObject);
      }
    } else if ((type == int.class) || (type == Integer.class)) {
      if (javascriptObject instanceof Double) {
        return new Integer(((Double) javascriptObject).intValue());
      } else if (javascriptObject instanceof Integer) {
        return javascriptObject;
      } else if (javascriptObject instanceof String) {
        return Integer.valueOf((String) javascriptObject);
      } else if (javascriptObject instanceof Short) {
        return new Integer(((Short) javascriptObject).shortValue());
      } else if (javascriptObject instanceof Long) {
        return new Integer(((Long) javascriptObject).intValue());
      } else if (javascriptObject instanceof Float) {
        return new Integer(((Float) javascriptObject).intValue());
      } else {
        return javascriptObject;
      }
    } else {
      return javascriptObject;
    }
  }
  */

  // Convenience method; useful when debugging
  public static String getStackTrace() {
    return org.mozilla.javascript.ScriptRuntime.constructError("", "").getScriptStackTrace();
  }
}
