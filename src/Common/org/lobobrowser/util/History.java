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
 * Created on Jun 6, 2005
 */
package org.lobobrowser.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author J. H. S.
 */
// TODO: Looks like it is not used
public class History implements java.io.Serializable {
  private static final long serialVersionUID = 2257845000800300100L;

  private transient ArrayList<String> historySequence;

  private final SortedSet<String> historySortedSet = new TreeSet<>();
  private final Map<String, TimedEntry> historyMap = new HashMap<>();
  private final SortedSet<TimedEntry> historyTimedSet = new TreeSet<>();

  private int sequenceCapacity;
  private int commonEntriesCapacity;

  private transient int sequenceIndex;

  /**
   * @param sequenceCapacity
   * @param commonEntriesCapacity
   */
  public History(final int sequenceCapacity, final int commonEntriesCapacity) {
    super();
    this.historySequence = new ArrayList<>();
    this.sequenceIndex = -1;
    this.sequenceCapacity = sequenceCapacity;
    this.commonEntriesCapacity = commonEntriesCapacity;
  }

  private void readObject(final java.io.ObjectInputStream in) throws ClassNotFoundException, java.io.IOException {
    this.historySequence = new ArrayList<>();
    this.sequenceIndex = -1;
    in.defaultReadObject();
  }

  /**
   * @return Returns the commonEntriesCapacity.
   */
  public int getCommonEntriesCapacity() {
    return commonEntriesCapacity;
  }

  /**
   * @param commonEntriesCapacity
   *          The commonEntriesCapacity to set.
   */
  public void setCommonEntriesCapacity(final int commonEntriesCapacity) {
    this.commonEntriesCapacity = commonEntriesCapacity;
  }

  /**
   * @return Returns the sequenceCapacity.
   */
  public int getSequenceCapacity() {
    return sequenceCapacity;
  }

  /**
   * @param sequenceCapacity
   *          The sequenceCapacity to set.
   */
  public void setSequenceCapacity(final int sequenceCapacity) {
    this.sequenceCapacity = sequenceCapacity;
  }

  public String getCurrentItem() {
    if (this.sequenceIndex >= 0) {
      return this.historySequence.get(this.sequenceIndex);
    } else {
      return null;
    }
  }

  public String back() {
    if (this.sequenceIndex > 0) {
      this.sequenceIndex--;
      return this.getCurrentItem();
    } else {
      return null;
    }
  }

  public String forward() {
    if ((this.sequenceIndex + 1) < this.historySequence.size()) {
      this.sequenceIndex++;
      return this.getCurrentItem();
    } else {
      return null;
    }
  }

  public Collection<String> getRecentItems(final int maxNumItems) {
    final Collection<String> items = new LinkedList<>();
    final Iterator<TimedEntry> i = this.historyTimedSet.iterator();
    int count = 0;
    while (i.hasNext() && (count++ < maxNumItems)) {
      final TimedEntry entry = i.next();
      items.add(entry.value);
    }
    return items;
  }

  public Collection<String> getHeadMatchItems(final String item, final int maxNumItems) {
    final String[] array = ArrayUtilities.copy(this.historySortedSet, String.class);
    final int idx = Arrays.binarySearch(array, item);
    final int startIdx = idx >= 0 ? idx : (-idx - 1);
    int count = 0;
    final Collection<String> items = new LinkedList<>();
    for (int i = startIdx; (i < array.length) && (count++ < maxNumItems); i++) {
      final String potentialItem = array[i];
      if (potentialItem.startsWith(item)) {
        items.add(potentialItem);
      } else {
        break;
      }
    }
    return items;
  }

  public void addAsRecentOnly(final String item) {
    TimedEntry entry = this.historyMap.get(item);
    if (entry != null) {
      this.historyTimedSet.remove(entry);
      entry.touch();
      this.historyTimedSet.add(entry);
    } else {
      entry = new TimedEntry(item);
      this.historyTimedSet.add(entry);
      this.historyMap.put(item, entry);
      this.historySortedSet.add(item);
      if (this.historyTimedSet.size() > this.commonEntriesCapacity) {
        // Most outdated goes last
        final TimedEntry entryToRemove = this.historyTimedSet.last();
        this.historyMap.remove(entryToRemove.value);
        this.historySortedSet.remove(entryToRemove.value);
        this.historyTimedSet.remove(entryToRemove);
      }
    }
  }

  public void addItem(final String item, final boolean updateAsRecent) {
    final int newIndex = this.sequenceIndex + 1;

    while (newIndex >= this.historySequence.size()) {
      this.historySequence.add(null);
    }
    this.historySequence.set(newIndex, item);
    this.sequenceIndex = newIndex;

    final int expectedSize = newIndex + 1;
    while (this.historySequence.size() > expectedSize) {
      this.historySequence.remove(expectedSize);
    }

    while (this.historySequence.size() > this.sequenceCapacity) {
      this.historySequence.remove(0);
      this.sequenceIndex--;
    }

    if (updateAsRecent) {
      this.addAsRecentOnly(item);
    }
  }

  private class TimedEntry implements Comparable<TimedEntry>, java.io.Serializable {
    private static final long serialVersionUID = 2257845000000000200L;
    private long timestamp = System.currentTimeMillis();
    private final String value;

    /**
     * @param value
     */
    public TimedEntry(final String value) {
      this.value = value;
    }

    public void touch() {
      this.timestamp = System.currentTimeMillis();
    }

    @Override
    public boolean equals(final Object obj) {
      final TimedEntry other = (TimedEntry) obj;
      return other.value.equals(this.value);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final TimedEntry other) {
      if (this.equals(other)) {
        return 0;
      }
      final long time1 = this.timestamp;
      final long time2 = other.timestamp;
      if (time1 > time2) {
        // More recent goes first
        return -1;
      } else if (time2 > time1) {
        return +1;
      } else {
        final int diff = System.identityHashCode(this) - System.identityHashCode(other);
        if (diff == 0) {
          return +1;
        }
        return diff;
      }
    }
  }

}
