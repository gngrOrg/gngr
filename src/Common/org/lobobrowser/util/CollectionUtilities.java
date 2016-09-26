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
 * Created on Jun 9, 2005
 */
package org.lobobrowser.util;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * @author J. H. S.
 */
public class CollectionUtilities {
  /**
   *
   */
  private CollectionUtilities() {
    super();
  }

  public static <T> Enumeration<T> getIteratorEnumeration(final Iterator<T> i) {
    return new Enumeration<T>() {
      public boolean hasMoreElements() {
        return i.hasNext();
      }

      public T nextElement() {
        return i.next();
      }
    };
  }

  public static <T> Enumeration<T> getEmptyEnumeration() {
    return new Enumeration<T>() {
      public boolean hasMoreElements() {
        return false;
      }

      public T nextElement() {
        throw new NoSuchElementException("Trying to get element of an empty enumeration");
      }
    };
  }

  public static <T> Iterator<T> iteratorUnion(final Iterator<T>[] iterators) {
    return new Iterator<T>() {
      private int iteratorIndex = 0;
      private Iterator<T> current = iterators.length > 0 ? iterators[0] : null;

      public boolean hasNext() {
        for (;;) {
          if (current == null) {
            return false;
          }
          if (current.hasNext()) {
            return true;
          }
          iteratorIndex++;
          current = iteratorIndex >= iterators.length ? null : iterators[iteratorIndex];
        }
      }

      public T next() {
        for (;;) {
          if (this.current == null) {
            throw new NoSuchElementException();
          }
          try {
            return this.current.next();
          } catch (final NoSuchElementException nse) {
            this.iteratorIndex++;
            this.current = this.iteratorIndex >= iterators.length ? null : iterators[this.iteratorIndex];
          }
        }
      }

      public void remove() {
        if (this.current == null) {
          throw new NoSuchElementException();
        }
        this.current.remove();
      }
    };
  }

  public static <T> Collection<T> reverse(final Collection<T> collection) {
    final LinkedList<T> newCollection = new LinkedList<>();
    final Iterator<T> i = collection.iterator();
    while (i.hasNext()) {
      newCollection.addFirst(i.next());
    }
    return newCollection;
  }

  public static <T> Iterator<T> singletonIterator(final T item) {
    return new Iterator<T>() {
      private boolean gotItem = false;

      public boolean hasNext() {
        return !this.gotItem;
      }

      public T next() {
        if (this.gotItem) {
          throw new NoSuchElementException();
        }
        this.gotItem = true;
        return item;
      }

      public void remove() {
        if (!this.gotItem) {
          this.gotItem = true;
        } else {
          throw new NoSuchElementException();
        }
      }
    };
  }

  private static Iterator<Object> emptyIterator = new Iterator<Object>() {
    public boolean hasNext() {
      return false;
    }

    public Object next() {
      throw new NoSuchElementException();
    }

    public void remove() {
      throw new NoSuchElementException();
    }
  };

  @SuppressWarnings("unchecked")
  public static <T> Iterator<T> emptyIterator() {
    return (Iterator<T>) emptyIterator;
  }

  public static class ListReverser<T> implements Iterable<T> {
    private final ListIterator<T> listIterator;

    public ListReverser(final List<T> wrappedList) {
      this.listIterator = wrappedList.listIterator(wrappedList.size());
    }

    public Iterator<T> iterator() {
      return new Iterator<T>() {

        public boolean hasNext() {
          return listIterator.hasPrevious();
        }

        public T next() {
          return listIterator.previous();
        }

        public void remove() {
          listIterator.remove();
        }

      };
    }
  }

  public static <T> Iterator<T> reverseIterator(final List<T> sr) {
    return new ListReverser<>(sr).iterator();
  }

  // Filter iterator adapted from an implementation found in http://erikras.com/2008/01/18/the-filter-pattern-java-conditional-abstraction-with-iterables/
  public static <@NonNull T> Iterator<T> filter(final Iterator<T> iterator, final FilterFunction<T> filterFunction) {
    return new FilterIterator<>(iterator, filterFunction);
  }

  public static interface FilterFunction<T> {
    public boolean passes(T object);
  }

  public static class FilterIterator<@NonNull T> implements Iterator<@NonNull T> {
    private final Iterator<@NonNull T> iterator;
    private @Nullable T next;
    private final FilterFunction<T> filterFunction;

    private FilterIterator(final Iterator<T> iterator, final FilterFunction<T> filterFunction) {
      this.iterator = iterator;
      this.filterFunction = filterFunction;
      toNext();
    }

    public boolean hasNext() {
      return next != null;
    }

    public T next() {
      final @Nullable T lNext = this.next;
      if (lNext != null) {
        final @NonNull T returnValue = lNext;
        toNext();
        return returnValue;
      } else {
        throw new NoSuchElementException();
      }
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    private void toNext() {
      next = null;
      while (iterator.hasNext()) {
        final T item = iterator.next();
        if (filterFunction.passes(item)) {
          next = item;
          break;
        }
      }
    }
  }
}
