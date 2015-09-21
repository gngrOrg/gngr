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
 * Created on Apr 17, 2005
 */
package org.lobobrowser.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * @author J. H. S.
 */
public class ArrayUtilities {

  /**
   *
   */
  private ArrayUtilities() {
    super();
  }

  public static <T> T[] copy(final Collection<T> collection, final Class<T> clazz) {
    @SuppressWarnings("unchecked")
    final T[] castedArray = (T[]) Array.newInstance(clazz, collection.size());
    return collection.toArray(castedArray);
  }

  /**
   * Slightly more efficient version of the below version. Since it doesn't need
   * to create an array if size is zero. But that is so only if the passed array
   * is a static zero sized array
   */
  public static <T> T[] copySynched(final Collection<T> collection, final Object syncObj, final T[] initArray) {
    synchronized (syncObj) {
      return collection.toArray(initArray);
    }
  }

  public static <T> T[] copySynched(final Collection<T> collection, final Object syncObj, final Class<T> clazz) {
    synchronized (syncObj) {
      return copy(collection, clazz);
    }
  }

  /**
   * For each element of collection, the supplied function is called. The
   * collection is copied in a synchronized block, to avoid concurrent
   * modifications.
   *
   * @param syncObj
   *          The object to synchronize upon.
   * @param func
   *          The function to call on each element.
   */
  public static <T, E extends Throwable> void forEachSynched(final Collection<T> collection, final Object syncObj,
      final Consumer<T> consumer) throws E {
    if (collection.size() > 0) {
      @SuppressWarnings("unchecked")
      final Class<T> clazz = (Class<T>) collection.iterator().next().getClass();
      final T[] copy = copySynched(collection, syncObj, clazz);
      for (final T element : copy) {
        consumer.accept(element);
      }
    }
  }

  public static <T> Iterator<T> iterator(final T[] array, final int offset, final int length) {
    return new ArrayIterator<>(array, offset, length);
  }

  private static class ArrayIterator<T> implements Iterator<T> {
    private final T[] array;
    private final int top;
    private int offset;

    public ArrayIterator(final T[] array, final int offset, final int length) {
      this.array = array;
      this.offset = offset;
      this.top = offset + length;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
      return this.offset < this.top;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#next()
     */
    public T next() {
      return this.array[this.offset++];
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#remove()
     */
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  public static <T> boolean contains(final T[] ts, final T t) {
    for (final T e: ts) {
      if (java.util.Objects.equals(e, t)) {
        return true;
      }
    }
    return false;
  }
}
