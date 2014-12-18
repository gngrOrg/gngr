package org.lobobrowser.request;

import java.net.URI;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lobobrowser.util.Strings;

final class CookieDetails {
  private static final Logger logger = Logger.getLogger(CookieDetails.class.getName());

  public CookieDetails(final URI requestURL, final String name, final String value, final String domain, final String path,
      final Optional<Date> expires, final Long maxAge,
      final boolean secure, final boolean httpOnly) {
    this.requestURL = requestURL;
    this.name = name;
    this.value = value;
    this.domain = domain;
    this.path = path;
    this.expires = expires;
    this.maxAge = maxAge;
    this.secure = secure;
    this.httpOnly = httpOnly;
    this.requestHostName = requestURL.getHost();
  }

  final URI requestURL;
  final String requestHostName;
  final String name;
  final String value;
  final String domain;
  private final String path;
  final Optional<Date> expires;
  final Long maxAge;
  final boolean secure, httpOnly;

  final String getEffectivePath() {
    if ((path == null) || (path.length() == 0) || (path.charAt(0) != '/')) {
      return getDefaultPath();
    } else {
      return path;
    }
  }

  /* As per section 5.1.4 of RFC 6265 */
  private String getDefaultPath() {
    final String urlPath = requestURL.getPath();
    if ((urlPath == null) || (urlPath.length() == 0) || (urlPath.charAt(0) != '/')) {
      return "/";
    } else if (Strings.countChars(urlPath, '/') == 1) {
      return "/";
    } else {
      return urlPath.substring(0, urlPath.lastIndexOf('/'));
    }
  }

  final String getEffectiveDomain() {
    if (domain == null) {
      return requestHostName;
    } else if (domain.startsWith(".")) {
      return domain.substring(1);
    } else {
      return domain;
    }
  }

  final Optional<java.util.Date> getExpiresDate() {
    Optional<java.util.Date> expiresDate = Optional.empty();
    if (maxAge != null) {
      if (maxAge <= 0) {
        logger.log(Level.WARNING, "getExpiresDate(): Max-age is negative or zero: " + maxAge + ".");
        expiresDate = Optional.of(new java.util.Date(0));
      } else {
        expiresDate = Optional.of(new java.util.Date(System.currentTimeMillis() + (maxAge * 1000)));
      }
    } else if (expires.isPresent()) {
      return expires;
    }
    return expiresDate;
  }

  boolean isValidDomain() {
    if (domain != null) {
      if ((expires == null) && (maxAge == null) && logger.isLoggable(Level.INFO)) {
        // TODO: Check if this is true:
        // One of the RFCs says transient cookies should not have
        // a domain specified, but websites apparently rely on that,
        // specifically Paypal.
        logger.log(Level.INFO, "Not rejecting transient cookie that specifies domain '" + domain + "'.");
      }
      // if (!Domains.isValidCookieDomain(domain, urlHostName)) {
      if (!DomainValidation.isValidCookieDomain(domain, requestHostName)) {
        logger.log(Level.WARNING, "saveCookie(): Rejecting cookie with invalid domain '" + domain + "' for host '"
            + requestHostName + "'.");
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    return "CookieDetails [name=" + name + ", value=" + value + ", domain=" + domain + ", path=" + path + ", expires=" + expires
        + ", maxAge=" + maxAge + ", effectivePath=" + getEffectivePath() + ", expiresDate=" + getExpiresDate() + "]";
  }

}