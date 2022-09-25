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
 * Created on Jun 1, 2005
 */
package org.lobobrowser.request;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.javatuples.Pair;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.lobobrowser.main.PlatformInit;
import org.lobobrowser.store.RestrictedStore;
import org.lobobrowser.store.StorageManager;

import info.gngr.db.tables.Cookies;
import info.gngr.db.tables.records.CookiesRecord;

/**
 * @author J. H. S.
 */
public class CookieStore {
  private static final CookieStore instance = new CookieStore();

  private static final Logger logger = Logger.getLogger(CookieStore.class.getName());

  private final Map<String, Map<Pair<String, String>, CookieValue>> transientMapByHost = new HashMap<>();

  private CookieStore() {
  }

  public static CookieStore getInstance() {
    return instance;
  }

  public void saveCookie(final URI url, final String cookieSpec) {
    final String urlHostName = url.getHost();
    if (logger.isLoggable(Level.INFO)) {
      logger.info("saveCookie(): host=" + urlHostName + ",cookieSpec=[" + cookieSpec + "]");
    }
    final Optional<CookieDetails> cookieDetailsOpt = CookieParsing.parseCookieSpec(url, cookieSpec);
    if (cookieDetailsOpt.isPresent()) {
      final CookieDetails cookieDetails = cookieDetailsOpt.get();
      if (PlatformInit.getInstance().debugOn) {
        System.out.println("Cookie details: " + cookieDetails);
      }

      if (cookieDetails.name == null) {
        logger.log(Level.SEVERE, "saveCookie(): Invalid name in cookie spec from '" + urlHostName + "'");
        return;
      }

      if (!cookieDetails.isValidDomain()) {
        logger.log(Level.SEVERE, "saveCookie(): Invalid domain in cookie spec from '" + urlHostName + "'");
        return;
      }

      this.saveCookie(cookieDetails);
    }
  }

  private void saveCookie(final CookieDetails cookieDetails) {
    final String name = cookieDetails.name;
    final String domain = cookieDetails.getEffectiveDomain();
    final String domainTL = domain.toLowerCase();
    final Optional<java.util.Date> expiresOpt = cookieDetails.getExpiresDate();
    if (logger.isLoggable(Level.INFO)) {
      logger.info("saveCookie(): " + cookieDetails);
    }
    final Optional<Long> expiresLongOpt = expiresOpt.map(e -> e.getTime());
    final CookieValue cookieValue = new CookieValue(cookieDetails.name, cookieDetails.value, cookieDetails.getEffectivePath(),
        expiresLongOpt, cookieDetails.secure, cookieDetails.httpOnly, getMonotonicTime());
    synchronized (this) {
      // Always save a transient cookie. It acts as a cache.
      Map<Pair<String, String>, CookieValue> hostMap = this.transientMapByHost.get(domainTL);
      if (hostMap == null) {
        hostMap = new HashMap<>(2);
        this.transientMapByHost.put(domainTL, hostMap);
      }
      hostMap.put(new Pair<>(cookieDetails.name, cookieDetails.getEffectivePath()), cookieValue);
    }
    if (expiresLongOpt.isPresent()) {
      final DSLContext userDB = StorageManager.getInstance().getDB();
      userDB
          .mergeInto(Cookies.COOKIES)
          .values(domainTL, name, cookieValue.getValue(), cookieValue.getPath(), true, true, cookieValue.getCreationTime(),
              cookieValue.getExpires().orElse(null))
          .execute();
    }
  }

  // This should be 1000 * 1000, but for optimization has been converted to 1024*1024
  // TODO: This seems like premature optimisation, since cookies are not that frequently stored.
  private static final long MILLION_LIKE = 1024 * 1024;

  private long previousTimeNanos = System.currentTimeMillis() * MILLION_LIKE;

  private long getMonotonicTime() {
    final long previousTimeMillis = previousTimeNanos / MILLION_LIKE;
    final long currentMillis = System.currentTimeMillis();
    if (previousTimeMillis == currentMillis) {
      previousTimeNanos += 1;
      return previousTimeNanos;
    } else {
      final long currentNanos = currentMillis * MILLION_LIKE;
      previousTimeNanos = currentNanos;
      return currentNanos;
    }
  }

  /* Path-match algorithm as per section 5.4.1 of RFC 6264. */
  private static boolean pathMatch(final String cookiePath, final String requestPath) {
    if (cookiePath.equals(requestPath)) {
      return true;
    } else if (requestPath.startsWith(cookiePath)) {
      return ((cookiePath.charAt(cookiePath.length() - 1) == '/') || (requestPath.charAt(cookiePath.length()) == '/'));
    } else {
      return false;
    }
  }

  /**
   * Gets cookies belonging exactly to the host name given, not to a broader
   * domain.
   */
  private List<CookieValue> getCookiesStrict(final String protocol, final String hostName, String path) {
    final String hostNameTL = hostName.toLowerCase();
    if ((path == null) || (path.length() == 0)) {
      path = "/"; // TODO: Confirm that this is correct. Issue #14 in browserTesting
    }
    final boolean secureProtocol = "https".equalsIgnoreCase(protocol);
    final boolean liflag = logger.isLoggable(Level.INFO);
    // final Set<String> transientCookieNames = new HashSet<>();
    final Set<Pair<String, String>> transientCookieNames = new HashSet<>();
    final List<CookieValue> selectedCookies = new LinkedList<>();
    synchronized (this) {
      final Map<Pair<String, String>, CookieValue> hostMap = this.transientMapByHost.get(hostNameTL);
      if (hostMap != null) {
        final Iterator<Map.Entry<Pair<String, String>, CookieValue>> i = hostMap.entrySet().iterator();
        while (i.hasNext()) {
          final Map.Entry<Pair<String, String>, CookieValue> entry = i.next();
          final CookieValue cookieValue = entry.getValue();
          if (cookieValue.isExpired()) {
            if (liflag) {
              logger.info("getCookiesStrict(): Cookie " + entry.getKey() + " from " + hostNameTL + " expired: " + cookieValue.getExpires());
            }
          } else {
            if (pathMatch(cookieValue.getPath(), path)) {
              if (cookieValue.checkSecure(secureProtocol)) {
                final Pair<String, String> cookieNameAndPath = entry.getKey();
                // transientCookieNames.add(new Pair<>(cookieName, cookieValue.getPath()));
                transientCookieNames.add(cookieNameAndPath);
                selectedCookies.add(cookieValue);
              }
            } else {
              if (liflag) {
                logger.info("getCookiesStrict(): Skipping cookie " + cookieValue + " since it does not match path " + path);
              }
            }
          }
        }
      }
    }
    try {
      final RestrictedStore store = StorageManager.getInstance().getRestrictedStore(hostNameTL, false);
      if (store != null) {
        final DSLContext userDB = StorageManager.getInstance().getDB();
        final Result<CookiesRecord> cookieResult = userDB.selectFrom(Cookies.COOKIES)
            .where(Cookies.COOKIES.HOSTNAME.eq(hostNameTL))
            .fetch();

        if (cookieResult.isNotEmpty()) {
          final CookiesRecord cookiesRecord = cookieResult.get(0);
          final String cookieName = cookiesRecord.getName();
          final CookieValue cookieValue = new CookieValue(
              cookiesRecord.getName(),
              cookiesRecord.getValue(),
              cookiesRecord.getPath(),
              Optional.ofNullable(cookiesRecord.getExpirationtime()),
              cookiesRecord.getSecure(), cookiesRecord.getHttponly(),
              cookiesRecord.getCreationtime()
              );
          if (!transientCookieNames.contains(new Pair<>(cookieName, cookieValue.getPath()))) {
            if (cookieValue.isExpired()) {
              if (logger.isLoggable(Level.INFO)) {
                logger.info("getCookiesStrict(): Cookie " + cookieName + " from " + hostName + " expired: " + cookieValue.getExpires());
              }
              cookiesRecord.delete();
            } else {
              if (pathMatch(cookieValue.getPath(), path)) {
                // Found one that is not in main memory. Cache it.
                synchronized (this) {
                  Map<Pair<String, String>, CookieValue> hostMap = this.transientMapByHost.get(hostName);
                  if (hostMap == null) {
                    hostMap = new HashMap<>();
                    this.transientMapByHost.put(hostName, hostMap);
                  }
                  hostMap.put(new Pair<>(cookieName, cookieValue.getPath()), cookieValue);
                }
                if (cookieValue.checkSecure(secureProtocol)) {
                  // Now add cookie to the collection.
                  selectedCookies.add(cookieValue);
                }
              } else {
                if (logger.isLoggable(Level.INFO)) {
                  logger.info("getCookiesStrict(): Skipping cookie " + cookieValue + " since it does not match path " + path);
                }
              }
            }
          }
        }
      }
    } catch (final IOException ioe) {
      logger.log(Level.SEVERE, "getCookiesStrict()", ioe);
    }

    return selectedCookies;
  }

  public Collection<Cookie> getCookies(final String protocol, final String hostName, final String path) {
    // Security provided by RestrictedStore.
    final Collection<String> possibleDomains = DomainValidation.getPossibleDomains(hostName);
    final List<CookieValue> allCookies = new LinkedList<>();
    for (final String domain : possibleDomains) {
      allCookies.addAll(this.getCookiesStrict(protocol, domain, path));
    }
    allCookies.sort(null);
    final List<Cookie> cookies = new LinkedList<>();
    for (final CookieValue cookieValue : allCookies) {
      cookies.add(new Cookie(cookieValue.getName(), cookieValue.getValue()));
    }
    if (logger.isLoggable(Level.INFO)) {
      logger.info("getCookies(): For host=" + hostName + ", found " + cookies.size() + " cookies: " + cookies);
    }
    return cookies;
  }
}
