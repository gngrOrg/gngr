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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A thread-safe cache with least-recently-used eviction policy bounded by an
 * approximate byte budget. Removal notifications are dispatched outside the
 * internal cache lock.
 */
public class LRUCache implements java.io.Serializable {
  private static final long serialVersionUID = 2L;
  private int approxMaxSize;

  private final LinkedHashMap<Object, CacheEntry> cacheMap = new LinkedHashMap<>(16, 0.75f, true);
  private long currentSize = 0;

  private transient Object lock;
  private volatile transient EventDispatch2 removalEvent;

  public LRUCache(final int approxMaxSize) {
    if (approxMaxSize < 0) {
      throw new IllegalArgumentException("approxMaxSize must be non-negative: " + approxMaxSize);
    }
    this.approxMaxSize = approxMaxSize;
    this.lock = new Object();
    this.removalEvent = new RemovalDispatch();
  }

  private void writeObject(final java.io.ObjectOutputStream out) throws java.io.IOException {
    synchronized (this.lock) {
      out.defaultWriteObject();
    }
  }

  private void readObject(final java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    in.defaultReadObject();
    // Need to initialize transient fields here.
    this.lock = new Object();
    this.removalEvent = new RemovalDispatch();
  }

  public int getApproxMaxSize() {
    synchronized (this.lock) {
      return this.approxMaxSize;
    }
  }

  public void setApproxMaxSize(final int approxMaxSize) {
    if (approxMaxSize < 0) {
      throw new IllegalArgumentException("approxMaxSize must be non-negative: " + approxMaxSize);
    }
    final List<Object> removedValues;
    synchronized (this.lock) {
      this.approxMaxSize = approxMaxSize;
      removedValues = this.evictOverflowLocked(null);
    }
    this.fireRemovalEvents(removedValues);
  }

  public void put(final Object key, final Object value, final int approxSize) {
    if (approxSize < 0) {
      throw new IllegalArgumentException("approxSize must be non-negative: " + approxSize);
    }
    List<Object> removedValues = null;
    synchronized (this.lock) {
      if (approxSize > this.approxMaxSize) {
        // Evict any stale entry for this key; oversized entry cannot be inserted.
        final CacheEntry existing = this.cacheMap.remove(key);
        if (existing != null) {
          this.currentSize -= existing.approximateSize;
          removedValues = appendRemovedValue(removedValues, existing.value);
        }
      } else {
        final CacheEntry existing = this.cacheMap.get(key);
        if (existing != null) {
          if (existing.value != value) {
            removedValues = appendRemovedValue(removedValues, existing.value);
          }
          this.currentSize += ((long) approxSize - existing.approximateSize);
          existing.approximateSize = approxSize;
          existing.value = value;
        } else {
          this.cacheMap.put(key, new CacheEntry(value, approxSize));
          this.currentSize += approxSize;
        }
        removedValues = this.evictOverflowLocked(removedValues);
      }
    }
    this.fireRemovalEvents(removedValues);
  }

  private List<Object> evictOverflowLocked(List<Object> removedValues) {
    if (this.currentSize <= this.approxMaxSize || this.cacheMap.isEmpty()) {
      return removedValues;
    }
    final Iterator<Map.Entry<Object, CacheEntry>> it = this.cacheMap.entrySet().iterator();
    while (this.currentSize > this.approxMaxSize && it.hasNext()) {
      final CacheEntry eldest = it.next().getValue();
      it.remove();
      this.currentSize -= eldest.approximateSize;
      removedValues = appendRemovedValue(removedValues, eldest.value);
    }
    return removedValues;
  }

  private static List<Object> appendRemovedValue(List<Object> removedValues, final Object value) {
    if (removedValues == null) {
      removedValues = new ArrayList<>(1);
    }
    removedValues.add(value);
    return removedValues;
  }

  private void fireRemovalEvents(final List<Object> removedValues) {
    if (removedValues == null) {
      return;
    }
    for (final Object value : removedValues) {
      this.removalEvent.fireEvent(new RemovalEvent(this, value));
    }
  }

  public Object get(final Object key) {
    synchronized (this.lock) {
      final CacheEntry entry = this.cacheMap.get(key);
      return entry != null ? entry.value : null;
    }
  }

  public Object remove(final Object key) {
    final CacheEntry entry;
    synchronized (this.lock) {
      entry = this.cacheMap.remove(key);
      if (entry != null) {
        this.currentSize -= entry.approximateSize;
      }
    }
    if (entry != null) {
      this.removalEvent.fireEvent(new RemovalEvent(this, entry.value));
      return entry.value;
    }
    return null;
  }

  public void addRemovalListener(final RemovalListener listener) {
    this.removalEvent.addListener(listener);
  }

  public void removeRemovalListener(final RemovalListener listener) {
    this.removalEvent.removeListener(listener);
  }

  public int getApproxSize() {
    synchronized (this.lock) {
      return (int) this.currentSize;
    }
  }

  public int getNumEntries() {
    synchronized (this.lock) {
      return this.cacheMap.size();
    }
  }

  public List<EntryInfo> getEntryInfoList() {
    synchronized (this.lock) {
      final List<EntryInfo> list = new ArrayList<>(this.cacheMap.size());
      for (final CacheEntry entry : this.cacheMap.values()) {
        final Object value = entry.value;
        final Class<? extends Object> vc = value == null ? null : value.getClass();
        list.add(new EntryInfo(vc, entry.approximateSize));
      }
      return list;
    }
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

  private static final class CacheEntry implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Object value;
    private int approximateSize;

    private CacheEntry(final Object value, final int approxSize) {
      this.value = value;
      this.approximateSize = approxSize;
    }
  }

  private class RemovalDispatch extends EventDispatch2 {
    @Override
    protected void dispatchEvent(final EventListener listener, final EventObject event) {
      ((RemovalListener) listener).removed((RemovalEvent) event);
    }
  }
}
