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
 * Created on Mar 13, 2005
 */
package org.lobobrowser.request;

import org.lobobrowser.ua.UserAgent;

public class OrigUserAgentImpl implements UserAgent {
  private static final UserAgent instance = new OrigUserAgentImpl();

  private OrigUserAgentImpl() {
  }

  public static UserAgent getInstance() {
    return instance;
  }

  public String getName() {
    return "Lobo";
  }

  public String getMajorVersion() {
    return "0";
  }

  public String getMinorVersion() {
    return "98.5";
  }

  public String getVersion() {
    return this.getMajorVersion() + "." + this.getMinorVersion();
  }

  public String getJavaVersion() {
    return System.getProperty("java.version");
  }

  private volatile String textValue = null;

  public String getUserAgentString() {
    final String tv = this.textValue;
    if (tv == null) {
      /*
      final GeneralSettings settings = AccessController.doPrivileged(new java.security.PrivilegedAction<GeneralSettings>() {
        public GeneralSettings run() {
          return GeneralSettings.getInstance();
        }
      });
      final boolean spoofIE = settings.isSpoofIE();
      final String ieVersion = settings.getIeVersion();
      tv = "Mozilla/" + settings.getMozVersion() + " (compatible" + (spoofIE ? "; MSIE " + ieVersion : "") + "; " + getOs() + ") "
          + this.getName() + "/" + this.getVersion();
       */
      this.textValue = tv;
    }
    return tv;
  }

  @Override
  public String toString() {
    return this.getUserAgentString();
  }

  /**
   * Removes cached user agent string.
   */
  public void invalidateUserAgent() {
    this.textValue = null;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.clientlet.UserAgent#getNameAndVersion()
   */
  public String getNameAndVersion() {
    return this.getName() + " " + this.getVersion();
  }

  public String getInfoUrl() {
    return "http://lobobrowser.org";
  }

  /*
  private static String getOs() {
    return System.getProperty("os.name") + " " + System.getProperty("os.version");
  }*/

  // //Note: This is not being used, but generally use of Strings a WeakHashMap
  // //keys should be revised.
  // private Map<String,String> sessionIDMap = new WeakHashMap<String,String>();
  //
  // public String getSessionID(java.net.URL url) {
  // //TODO: Should be a LRU cache instead of a weak hash map.
  // String host = url.getHost();
  // String key = url.getProtocol().toLowerCase() + "#" + (host == null ? "" :
  // host.toLowerCase());
  // synchronized(this) {
  // String sessionID = this.sessionIDMap.get(key);
  // if(sessionID == null) {
  // StringBuffer rawIdentifier = new StringBuffer(key);
  // rawIdentifier.append(ID.getGlobalProcessID());
  // sessionID = ID.getHexString(ID.getMD5Bytes(rawIdentifier.toString()));
  // this.sessionIDMap.put(key, sessionID);
  // }
  // return sessionID;
  // }
  // }
}
