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

package org.lobobrowser.request;

import java.util.HashSet;
import java.util.Set;

public class VerifiedHostsStore {
  private static final VerifiedHostsStore instance = new VerifiedHostsStore();
  private final Set<String> hosts = new HashSet<>();

  public static VerifiedHostsStore getInstance() {
    return instance;
  }

  public VerifiedHostsStore() {
    super();
  }

  public boolean contains(final String host) {
    synchronized (this.hosts) {
      return this.hosts.contains(host);
    }
  }

  public void add(final String host) {
    synchronized (this.hosts) {
      this.hosts.add(host);
    }
  }
}
