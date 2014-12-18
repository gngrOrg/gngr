package org.lobobrowser.util;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class Items {
  private Items() {
  }

  private static Map<Object, Map<String, Object>> sourceMap = new WeakHashMap<>();

  public static Object getItem(final Object source, final String name) {
    final Map<Object, Map<String, Object>> sm = sourceMap;
    synchronized (sm) {
      final Map<String, Object> itemMap = sm.get(source);
      if (itemMap == null) {
        return null;
      }
      return itemMap.get(name);
    }
  }

  public static void setItem(final Object source, final String name, final Object value) {
    final Map<Object, Map<String, Object>> sm = sourceMap;
    synchronized (sm) {
      Map<String, Object> itemMap = sm.get(source);
      if (itemMap == null) {
        itemMap = new HashMap<>(1);
        sm.put(source, itemMap);
      }
      itemMap.put(name, value);
    }
  }
}
