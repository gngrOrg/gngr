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

/**
 * @author J. H. S.
 */
public class UserAgentImpl implements UserAgent {
  private static final UserAgentImpl instance = new UserAgentImpl();

  // private static final Logger logger =
  // Logger.getLogger(UserAgentImpl.class.getName());

  private UserAgentImpl() {
  }

  public static UserAgentImpl getInstance() {
    return instance;
  }

  private static final String UAName = "";
  private static final String UAMajorVersion = "";
  private static final String UAMinorVersion = "";
  private static final String UAString = ""; // TODO: Update to UAName + "/" + UAMajorVersion

  public String getName() {
    return UAName;
  }

  public String getMajorVersion() {
    return UAMajorVersion;
  }

  public String getMinorVersion() {
    return UAMinorVersion;
  }

  public String getVersion() {
    return this.getMajorVersion() + "." + this.getMinorVersion();
  }

  // private volatile String textValue = null;

  public String getUserAgentString() {
    return UAString;
  }

  @Override
  public String toString() {
    return this.getUserAgentString();
  }

  /**
   * Removes cached user agent string.
   */
  public void invalidateUserAgent() {
    // this.textValue = null;
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
    return "";
  }

  /*
  private static String getOs() {
    return "";
  } */

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
