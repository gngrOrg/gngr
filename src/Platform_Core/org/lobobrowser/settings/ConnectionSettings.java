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
/*
 * Created on Jul 9, 2005
 */
package org.lobobrowser.settings;

import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lobobrowser.security.GenericLocalPermission;
import org.lobobrowser.store.StorageManager;
import org.lobobrowser.util.io.NetRoutines;

/**
 * Connection settings. This is a singleton class with an instance obtained by
 * calling {@link #getInstance()}.
 */
public class ConnectionSettings implements java.io.Serializable {
  private static final Logger logger = Logger.getLogger(ConnectionSettings.class.getName());
  private static final ConnectionSettings instance;
  private static final long serialVersionUID = 22574500000000301L;

  private Proxy.Type proxyType = Proxy.Type.DIRECT;
  private InetSocketAddress socketAddress = null;
  private String userName;
  private String password;
  private boolean authenticated;
  private boolean disableProxyForLocalAddresses;

  private transient Proxy proxy;

  static {
    ConnectionSettings ins = null;
    try {
      ins = (ConnectionSettings) StorageManager.getInstance().retrieveSettings(ConnectionSettings.class.getSimpleName(),
          ConnectionSettings.class.getClassLoader());
    } catch (final Exception err) {
      logger.log(Level.WARNING, "getInstance(): Unable to retrieve settings.", err);
    }
    if (ins == null) {
      ins = new ConnectionSettings();
    }
    instance = ins;
  }

  private ConnectionSettings() {
    restoreDefaults();
  }

  public void restoreDefaults() {
    this.proxyType = Proxy.Type.DIRECT;
    this.userName = "";
    this.password = "";
    this.authenticated = false;
    this.socketAddress = null;
    this.disableProxyForLocalAddresses = true;
    synchronized (this) {
      this.proxy = null;
    }
  }

  /**
   * Gets the class singleton.
   */
  public static ConnectionSettings getInstance() {
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(GenericLocalPermission.EXT_GENERIC);
    }
    return instance;
  }

  /**
   * Gets a non-<code>null</code> <code>Proxy</code> insteance.
   */
  public Proxy getProxy(final String host) {
    synchronized (this) {
      if (this.proxy == null) {
        final InetSocketAddress sa = this.socketAddress;
        if ((this.proxyType == Proxy.Type.DIRECT) || (sa == null)) {
          this.proxy = Proxy.NO_PROXY;
        } else {
          this.proxy = new Proxy(this.proxyType, sa);
        }
      }
      Proxy proxy = this.proxy;
      if ((proxy != Proxy.NO_PROXY) && this.disableProxyForLocalAddresses) {
        if (NetRoutines.isLocalAddress(host)) {
          proxy = Proxy.NO_PROXY;
        }
      }
      return proxy;
    }
  }

  public PasswordAuthentication getPasswordAuthentication() {
    final String userName = this.userName;
    final String password = this.password;
    if (!this.isAuthenticated() || (userName == null) || (password == null)) {
      return null;
    }
    return new PasswordAuthentication(userName, password.toCharArray());
  }

  /**
   * @return Returns the authenticated.
   */
  public boolean isAuthenticated() {
    return authenticated;
  }

  /**
   * @param authenticated
   *          The authenticated to set.
   */
  public void setAuthenticated(final boolean authenticated) {
    this.authenticated = authenticated;
    synchronized (this) {
      this.proxy = null;
    }
  }

  /**
   * @return Returns the password.
   */
  public String getPassword() {
    return password;
  }

  /**
   * @param password
   *          The password to set.
   */
  public void setPassword(final String password) {
    this.password = password;
    synchronized (this) {
      this.proxy = null;
    }
  }

  /**
   * @return Returns the userName.
   */
  public String getUserName() {
    return userName;
  }

  /**
   * @param userName
   *          The userName to set.
   */
  public void setUserName(final String userName) {
    this.userName = userName;
    synchronized (this) {
      this.proxy = null;
    }
  }

  /**
   * @return Returns the proxyType.
   */
  public Proxy.Type getProxyType() {
    return proxyType;
  }

  /**
   * @param proxyType
   *          The proxyType to set.
   */
  public void setProxyType(final Proxy.Type proxyType) {
    this.proxyType = proxyType;
    synchronized (this) {
      this.proxy = null;
    }
  }

  /**
   * @return Returns the socketAddress.
   */
  public InetSocketAddress getInetSocketAddress() {
    return socketAddress;
  }

  /**
   * @param socketAddress
   *          The socketAddress to set.
   */
  public void setInetSocketAddress(final InetSocketAddress socketAddress) {
    this.socketAddress = socketAddress;
    synchronized (this) {
      this.proxy = null;
    }
  }

  public boolean isDisableProxyForLocalAddresses() {
    return disableProxyForLocalAddresses;
  }

  public void setDisableProxyForLocalAddresses(final boolean disableProxyForLocalAddresses) {
    this.disableProxyForLocalAddresses = disableProxyForLocalAddresses;
    synchronized (this) {
      this.proxy = null;
    }
  }

  public void save() {
    try {
      StorageManager.getInstance().saveSettings(this.getClass().getSimpleName(), this);
    } catch (final java.io.IOException ioe) {
      logger.log(Level.WARNING, "save(): Unable to save settings", ioe);
    }
  }
}
