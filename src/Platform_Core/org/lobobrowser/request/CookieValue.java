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
 * Created on Jun 2, 2005
 */
package org.lobobrowser.request;

import java.util.Optional;

/**
 * @author J. H. S.
 */
public class CookieValue implements Comparable<CookieValue> {
  private final String name;
  private final String value;
  private final String path;
  private final Optional<Long> expirationTimeOpt;
  private final boolean secure;
  private final boolean httpOnly;
  private final long creationTime;

  public CookieValue(final String name, final String value, final String path, final Optional<Long> expirationTimeOpt,
      final boolean secure, final boolean httpOnly, final long creationTime) {
    this.name = name;
    this.value = value;
    this.path = path;
    this.expirationTimeOpt = expirationTimeOpt;
    this.secure = secure;
    this.httpOnly = httpOnly;
    this.creationTime = creationTime;
  }

  public String getName() {
    return this.name;
  }

  public String getValue() {
    return this.value;
  }

  public Optional<Long> getExpires() {
    return expirationTimeOpt;
  }

  public String getPath() {
    return path;
  }

  public boolean isExpired() {
    final Optional<Long> expOpt = getExpires();
    /* we use `less than or equal to` to check the time instead of just `less than` because
     * System.currentTimeMillis() is not fine grained. If a cookie has max-age set to 0 for example,
     * we need to ensure that it expires immediately, even if System.currentTimeMillis() doesn't
     * return a newer value.
     */
    return expOpt.map(exp -> exp.longValue() <= System.currentTimeMillis()).orElse(false);
  }

  @Override
  public String toString() {
    return "CookieValue[name=" + name + " value=" + value + ",path=" + path + ",expiration=" + expirationTimeOpt + ",creationTime="
        + creationTime + "]";
  }

  /* Returns true if the secure flag is valid for the given protocol type */

  public boolean checkSecure(final boolean secureProtocol) {
    return !secure || secureProtocol;
  }

  /**
   * Orders cookies by these rules (section 5.4, point 2 of RFC 6265) - cookies
   * with longer paths are smaller - if path lengths are same, cookies with
   * earlier creation time are smaller
   */
  public int compareTo(final CookieValue other) {
    final int pathCompare = ((Integer) (path.length())).compareTo(other.path.length());
    if (pathCompare == 0) {
      return ((Long) creationTime).compareTo(other.creationTime);
    } else {
      return -pathCompare;
    }
  }

  public long getCreationTime() {
    return creationTime;
  }
}
