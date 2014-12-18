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
package org.lobobrowser.util;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * A cache with least-recently-used policy. Note that this class is not thread
 * safe by itself.
 */
public class LRUCache implements java.io.Serializable {
  private static final long serialVersionUID = 940427225784212823L;
  private int approxMaxSize;

  private final Map<Object, OrderedValue> cacheMap = new HashMap<>();
  private volatile transient EventDispatch2 removalEvent;

  /**
   * Ascending timestamp order. First is least recently used.
   */
  private final TreeSet<OrderedValue> timedSet = new TreeSet<>();
  private int currentSize = 0;

  public LRUCache(final int approxMaxSize) {
    this.approxMaxSize = approxMaxSize;
    this.removalEvent = new RemovalDispatch();
  }

  private void readObject(final java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    in.defaultReadObject();
    // Need to initialize transient fields here.
    this.removalEvent = new RemovalDispatch();
  }

  public int getApproxMaxSize() {
    return approxMaxSize;
  }

  public void setApproxMaxSize(final int approxMaxSize) {
    this.approxMaxSize = approxMaxSize;
  }

  public void put(final Object key, final Object value, final int approxSize) {
    if (approxSize > this.approxMaxSize) {
      // Can't be inserted.
      return;
    }
    OrderedValue ordVal = this.cacheMap.get(key);
    if (ordVal != null) {
      if (ordVal.value != value) {
        this.removalEvent.fireEvent(new RemovalEvent(this, ordVal.value));
      }
      this.currentSize += (approxSize - ordVal.approximateSize);
      this.timedSet.remove(ordVal);
      ordVal.approximateSize = approxSize;
      ordVal.value = value;
      ordVal.touch();
      this.timedSet.add(ordVal);
    } else {
      ordVal = new OrderedValue(key, value, approxSize);
      this.cacheMap.put(key, ordVal);
      this.timedSet.add(ordVal);
      this.currentSize += approxSize;
    }
    while (this.currentSize > this.approxMaxSize) {
      this.removeLRU();
    }
  }

  private void removeLRU() {
    final OrderedValue ordVal = this.timedSet.first();
    if (ordVal != null) {
      this.removalEvent.fireEvent(new RemovalEvent(this, ordVal.value));
      if (this.timedSet.remove(ordVal)) {
        this.cacheMap.remove(ordVal.key);
        this.currentSize -= ordVal.approximateSize;
      } else {
        throw new IllegalStateException("Could not remove existing tree node.");
      }
    } else {
      throw new IllegalStateException("Cannot remove LRU since the cache is empty.");
    }
  }

  public Object get(final Object key) {
    final OrderedValue ordVal = this.cacheMap.get(key);
    if (ordVal != null) {
      this.timedSet.remove(ordVal);
      ordVal.touch();
      this.timedSet.add(ordVal);
      return ordVal.value;
    } else {
      return null;
    }
  }

  public Object remove(final Object key) {
    final OrderedValue ordVal = this.cacheMap.get(key);
    if (ordVal != null) {
      this.removalEvent.fireEvent(new RemovalEvent(this, ordVal.value));
      this.currentSize -= ordVal.approximateSize;
      this.timedSet.remove(ordVal);
      return ordVal.value;
    } else {
      return null;
    }
  }

  public void addRemovalListener(final RemovalListener listener) {
    this.removalEvent.addListener(listener);
  }

  public void removeRemovalListener(final RemovalListener listener) {
    this.removalEvent.removeListener(listener);
  }

  public int getApproxSize() {
    return this.currentSize;
  }

  public int getNumEntries() {
    return this.cacheMap.size();
  }

  public List<EntryInfo> getEntryInfoList() {
    final List<EntryInfo> list = new ArrayList<>();
    final Iterator<OrderedValue> i = this.cacheMap.values().iterator();
    while (i.hasNext()) {
      final OrderedValue ov = i.next();
      final Object value = ov.value;
      final Class<? extends Object> vc = value == null ? null : value.getClass();
      list.add(new EntryInfo(vc, ov.approximateSize));
    }
    return list;
  }

  public static class EntryInfo {
    public final Class<? extends Object> valueClass;
    public final int approximateSize;

    public EntryInfo(final Class<? extends Object> valueClass, final int approximateSize) {
      super();
      this.valueClass = valueClass;
      this.approximateSize = approximateSize;
    }

    @Override
    public String toString() {
      final Class<? extends Object> vc = this.valueClass;
      final String vcName = vc == null ? "<none>" : vc.getName();
      return "[class=" + vcName + ",approx-size=" + this.approximateSize + "]";
    }
  }

  private class OrderedValue implements Comparable<OrderedValue>, java.io.Serializable {
    private static final long serialVersionUID = 340227625744215821L;
    private long timestamp;
    private int approximateSize;
    private Object value;
    private final Object key;

    private OrderedValue(final Object key, final Object value, final int approxSize) {
      this.key = key;
      this.value = value;
      this.approximateSize = approxSize;
      this.touch();
    }

    private final void touch() {
      this.timestamp = System.currentTimeMillis();
    }

    public int compareTo(final OrderedValue arg0) {
      if (this == arg0) {
        return 0;
      }
      final OrderedValue other = arg0;
      final long diff = this.timestamp - other.timestamp;
      if (diff > 0) {
        return +1;
      } else if (diff < 0) {
        return -1;
      }
      int hc1 = System.identityHashCode(this);
      int hc2 = System.identityHashCode(other);
      if (hc1 == hc2) {
        hc1 = System.identityHashCode(this.value);
        hc2 = System.identityHashCode(other.value);
      }
      return hc1 - hc2;
    }
  }

  private class RemovalDispatch extends EventDispatch2 {
    @Override
    protected void dispatchEvent(final EventListener listener, final EventObject event) {
      ((RemovalListener) listener).removed((RemovalEvent) event);
    }
  }
}
