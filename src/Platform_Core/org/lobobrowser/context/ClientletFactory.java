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
package org.lobobrowser.context;

import java.util.LinkedList;
import java.util.List;

import org.lobobrowser.clientlet.Clientlet;
import org.lobobrowser.clientlet.ClientletRequest;
import org.lobobrowser.clientlet.ClientletResponse;
import org.lobobrowser.clientlet.ClientletSelector;
import org.lobobrowser.security.GenericLocalPermission;

public class ClientletFactory {
  // private static final Logger logger =
  // Logger.getLogger(ClientletFactory.class.getName());
  private static ClientletFactory instance;

  private ClientletFactory() {
    this.addClientletSelector(new CoreClientletSelector());
  }

  public static ClientletFactory getInstance() {
    if (instance == null) {
      synchronized (ClientletFactory.class) {
        if (instance == null) {
          instance = new ClientletFactory();
        }
      }
    }
    return instance;
  }

  private final List<ClientletSelector> selectors = new LinkedList<>();

  public void addClientletSelector(final ClientletSelector selector) {
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(GenericLocalPermission.EXT_GENERIC);
    }
    synchronized (this) {
      this.selectors.add(0, selector);
    }
  }

  public Clientlet getClientlet(final ClientletRequest request, final ClientletResponse response) {
    synchronized (this) {
      for (final ClientletSelector selector : this.selectors) {
        final Clientlet clientlet = selector.select(request, response);
        if (clientlet == null) {
          continue;
        }
        return clientlet;
      }
    }
    throw new IllegalStateException("No clientlets found for response: " + response + ".");
  }
}
