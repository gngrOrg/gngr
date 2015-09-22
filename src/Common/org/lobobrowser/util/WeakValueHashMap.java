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
/*
 * Created on Oct 8, 2005
 */
package org.lobobrowser.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;

public class WeakValueHashMap<K, @Nullable V> implements Map<K, V> {
  private final Map<K, LocalWeakReference> map = new HashMap<>();
  private final ReferenceQueue<V> queue = new ReferenceQueue<>();

  public WeakValueHashMap() {
    super();
  }

  public int size() {
    return this.map.size();
  }

  public boolean isEmpty() {
    return this.map.isEmpty();
  }

  public boolean containsKey(final Object key) {
    return map.containsKey(key);
  }

  public boolean containsValue(final Object value) {
    throw new UnsupportedOperationException();
  }

  public V get(final Object key) {
    this.checkQueue();
    final LocalWeakReference wf = this.map.get(key);
    return wf == null ? null : wf.get();
  }

  public V put(final K key, final V value) {
    this.checkQueue();
    return this.putImpl(key, value);
  }

  private final V putImpl(final K key, final V value) {
    if (value == null) {
      throw new IllegalArgumentException("null values not accepted");
    }
    final LocalWeakReference ref = new LocalWeakReference(key, value);
    final LocalWeakReference oldWf = this.map.put(key, ref);
    return oldWf == null ? null : oldWf.get();
  }

  public V remove(final Object key) {
    this.checkQueue();
    final LocalWeakReference wf = this.map.remove(key);
    return wf == null ? null : wf.get();
  }

  public void putAll(final Map<? extends K, ? extends V> t) {
    this.checkQueue();
    t.forEach((k, v) -> this.putImpl(k, v));
  }

  public void clear() {
    this.checkQueue();
    this.map.clear();
  }

  public Set<K> keySet() {
    return this.map.keySet();
  }

  @SuppressWarnings("unchecked")
  private final void checkQueue() {
    final ReferenceQueue<V> queue = this.queue;
    LocalWeakReference ref;
    while ((ref = (LocalWeakReference) queue.poll()) != null) {
      this.map.remove(ref.getKey());
    }
  }

  public Collection<V> values() {
    checkQueue();
    final Stream<V> m =
        this.map.values().stream()
            .map(t -> t == null ? null : t.get())
            .filter(t -> t != null);
    return m.collect(Collectors.toList());
  }

  public Set<Entry<K, V>> entrySet() {
    throw new UnsupportedOperationException();
  }

  private class LocalWeakReference extends WeakReference<V> {
    private final K key;

    public LocalWeakReference(final K key, final V target) {
      super(target, queue);
      this.key = key;
    }

    public K getKey() {
      return key;
    }

    /*
    public boolean equals(final Object other) {
      final K target1 = this.get();
      final Object target2 = other instanceof LocalWeakReference ? ((LocalWeakReference) other).get() : null;
      return Objects.equals(target1, target2);
    }

    public int hashCode() {
      final Object target = this.get();
      return target == null ? 0 : target.hashCode();
    }*/
  }
}
