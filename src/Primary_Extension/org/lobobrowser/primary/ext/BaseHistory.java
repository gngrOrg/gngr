/*
 GNU GENERAL PUBLIC LICENSE
 Copyright (C) 2006 The Lobo Project

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public
 License as published by the Free Software Foundation; either
 verion 2 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

 Contact info: lobochief@users.sourceforge.net
 */
package org.lobobrowser.primary.ext;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.lobobrowser.util.ArrayUtilities;

public abstract class BaseHistory<T> implements java.io.Serializable {
  private static final long serialVersionUID = 2257845020000200400L;

  protected BaseHistory() {
    super();
  }

  private final SortedSet<@NonNull String> historySortedSet = new TreeSet<>();
  private final Map<String, TimedEntry> historyMap = new HashMap<>();
  private final SortedSet<TimedEntry> historyTimedSet = new TreeSet<>();

  private final int commonEntriesCapacity = 1000;

  public boolean hasRecentEntries() {
    synchronized (this) {
      return this.historyTimedSet.size() > 0;
    }
  }

  public Collection<String> getRecentItems(final int maxNumItems) {
    synchronized (this) {
      final Collection<String> items = new LinkedList<>();
      final Iterator<TimedEntry> i = this.historyTimedSet.iterator();
      int count = 0;
      while (i.hasNext() && (count++ < maxNumItems)) {
        final TimedEntry entry = i.next();
        items.add(entry.value);
      }
      return items;
    }
  }

  public Collection<T> getRecentItemInfo(final int maxNumItems) {
    synchronized (this) {
      final Collection<T> items = new LinkedList<>();
      final Iterator<TimedEntry> i = this.historyTimedSet.iterator();
      int count = 0;
      while (i.hasNext() && (count++ < maxNumItems)) {
        final TimedEntry entry = i.next();
        items.add(entry.itemInfo);
      }
      return items;
    }
  }

  public Collection<HostEntry> getRecentHostEntries(final int maxNumItems) {
    synchronized (this) {
      final Collection<HostEntry> items = new LinkedList<>();
      final Iterator<TimedEntry> i = this.historyTimedSet.iterator();
      final Set<String> hosts = new HashSet<>();
      while (i.hasNext()) {
        final TimedEntry entry = i.next();
        final String host = entry.url.getHost();
        if ((host != null) && (host.length() != 0)) {
          if (!hosts.contains(host)) {
            hosts.add(host);
            if (hosts.size() >= maxNumItems) {
              break;
            }
            items.add(new HostEntry(host, entry.timestamp));
          }
        }
      }
      return items;
    }
  }

  public Collection<HistoryEntry<T>> getAllEntries() {
    synchronized (this) {
      final Collection<HistoryEntry<T>> items = new LinkedList<>();
      final Iterator<TimedEntry> i = this.historyTimedSet.iterator();
      while (i.hasNext()) {
        final TimedEntry entry = i.next();
        items.add(new HistoryEntry<>(entry.url, entry.timestamp, entry.itemInfo));
      }
      return items;
    }
  }

  public Collection<HistoryEntry<T>> getRecentEntries(final int maxNumItems) {
    synchronized (this) {
      final Collection<HistoryEntry<T>> items = new LinkedList<>();
      final Iterator<TimedEntry> i = this.historyTimedSet.iterator();
      while (i.hasNext()) {
        final TimedEntry entry = i.next();
        if (items.size() >= maxNumItems) {
          break;
        }
        items.add(new HistoryEntry<>(entry.url, entry.timestamp, entry.itemInfo));
      }
      return items;
    }
  }

  public Collection<String> getHeadMatchItems(final String itemPrefix, final int maxNumItems) {
    synchronized (this) {
      final String[] array = ArrayUtilities.copy(this.historySortedSet, String.class);
      final int idx = Arrays.binarySearch(array, itemPrefix);
      final int startIdx = idx >= 0 ? idx : (-idx - 1);
      int count = 0;
      final Collection<String> items = new LinkedList<>();
      for (int i = startIdx; (i < array.length) && (count++ < maxNumItems); i++) {
        final String potentialItem = array[i];
        if (potentialItem.startsWith(itemPrefix)) {
          items.add(potentialItem);
        } else {
          break;
        }
      }
      return items;
    }
  }

  public void addAsRecent(final java.net.URL url, final T itemInfo) {
    final @NonNull String item = url.toExternalForm();
    synchronized (this) {
      TimedEntry entry = this.historyMap.get(item);
      if (entry != null) {
        this.historyTimedSet.remove(entry);
        entry.touch();
        entry.itemInfo = itemInfo;
        this.historyTimedSet.add(entry);
      } else {
        entry = new TimedEntry(url, item, itemInfo);
        this.historyTimedSet.add(entry);
        this.historyMap.put(item, entry);
        this.historySortedSet.add(item);
        while (this.historyTimedSet.size() > this.commonEntriesCapacity) {
          // Most outdated goes last
          final TimedEntry entryToRemove = this.historyTimedSet.last();
          this.historyMap.remove(entryToRemove.value);
          this.historySortedSet.remove(entryToRemove.value);
          this.historyTimedSet.remove(entryToRemove);
        }
      }
    }
  }

  public void touch(final java.net.URL url) {
    final String item = url.toExternalForm();
    synchronized (this) {
      final TimedEntry entry = this.historyMap.get(item);
      if (entry != null) {
        this.historyTimedSet.remove(entry);
        entry.touch();
        this.historyTimedSet.add(entry);
      }
    }
  }

  public @Nullable T getExistingInfo(final String item) {
    final TimedEntry entry = this.historyMap.get(item);
    return entry == null ? null : entry.itemInfo;
  }

  private class TimedEntry implements Comparable<TimedEntry>, java.io.Serializable {
    private static final long serialVersionUID = 2257845000000000200L;
    private long timestamp = System.currentTimeMillis();
    private final java.net.URL url;
    private final String value;
    private T itemInfo;

    /**
     * @param url
     */
    public TimedEntry(final java.net.URL url, final String textValue, final T itemInfo) {
      this.itemInfo = itemInfo;
      this.value = textValue;
      this.url = url;
    }

    public void touch() {
      this.timestamp = System.currentTimeMillis();
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj instanceof BaseHistory.TimedEntry) {
        @SuppressWarnings("unchecked")
        final BaseHistory<T>.TimedEntry other = (BaseHistory<T>.TimedEntry) obj;
        return other.value.equals(this.value);
      } else {
        return false;
      }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final TimedEntry arg0) {
      if (this.equals(arg0)) {
        return 0;
      }
      final TimedEntry other = arg0;
      final long time1 = this.timestamp;
      final long time2 = other.timestamp;
      if (time1 > time2) {
        // More recent goes first
        return -1;
      } else if (time2 > time1) {
        return +1;
      } else {
        return this.value.compareTo(other.value);
      }
    }
  }

}