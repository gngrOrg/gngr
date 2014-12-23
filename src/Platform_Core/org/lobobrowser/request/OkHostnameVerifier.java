/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.lobobrowser.request;

import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.security.auth.x500.X500Principal;

/**
 * A HostnameVerifier consistent with <a
 * href="http://www.ietf.org/rfc/rfc2818.txt">RFC 2818</a>.
 */
public final class OkHostnameVerifier implements HostnameVerifier {
  public static final OkHostnameVerifier INSTANCE = new OkHostnameVerifier();

  /**
   * Quick and dirty pattern to differentiate IP addresses from hostnames. This
   * is an approximation of Android's private InetAddress#isNumeric API.
   *
   * <p>
   * This matches IPv6 addresses as a hex string containing at least one colon,
   * and possibly including dots after the first colon. It matches IPv4
   * addresses as strings containing only decimal digits and dots. This pattern
   * matches strings like "a:.23" and "54" that are neither IP addresses nor
   * hostnames; they will be verified as IP addresses (which is a more strict
   * verification).
   */
  private static final Pattern VERIFY_AS_IP_ADDRESS = Pattern.compile(
      "([0-9a-fA-F]*:[0-9a-fA-F:.]*)|([\\d.]+)");

  private static final int ALT_DNS_NAME = 2;
  private static final int ALT_IPA_NAME = 7;

  OkHostnameVerifier() {
  }

  public boolean verify(final String host, final SSLSession session) {
    try {
      final Certificate[] certificates = session.getPeerCertificates();
      return verify(host, (X509Certificate) certificates[0]);
    } catch (final SSLException e) {
      return false;
    }
  }

  public static boolean verify(final String host, final X509Certificate certificate) {
    return verifyAsIpAddress(host)
        ? verifyIpAddress(host, certificate)
        : verifyHostName(host, certificate);
  }

  static boolean verifyAsIpAddress(final String host) {
    return VERIFY_AS_IP_ADDRESS.matcher(host).matches();
  }

  /**
   * Returns true if {@code certificate} matches {@code ipAddress}.
   */
  private static boolean verifyIpAddress(final String ipAddress, final X509Certificate certificate) {
    for (final String altName : getSubjectAltNames(certificate, ALT_IPA_NAME)) {
      if (ipAddress.equalsIgnoreCase(altName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if {@code certificate} matches {@code hostName}.
   */
  private static boolean verifyHostName(String hostName, final X509Certificate certificate) {
    hostName = hostName.toLowerCase(Locale.US);
    boolean hasDns = false;
    for (final String altName : getSubjectAltNames(certificate, ALT_DNS_NAME)) {
      hasDns = true;
      if (verifyHostName(hostName, altName)) {
        return true;
      }
    }

    if (!hasDns) {
      final X500Principal principal = certificate.getSubjectX500Principal();
      // RFC 2818 advises using the most specific name for matching.
      final String cn = new DistinguishedNameParser(principal).findMostSpecific("cn");
      if (cn != null) {
        return verifyHostName(hostName, cn);
      }
    }

    return false;
  }

  private static List<String> getSubjectAltNames(final X509Certificate certificate, final int type) {
    final List<String> result = new ArrayList<>();
    try {
      final Collection<?> subjectAltNames = certificate.getSubjectAlternativeNames();
      if (subjectAltNames == null) {
        return Collections.emptyList();
      }
      for (final Object subjectAltName : subjectAltNames) {
        final List<?> entry = (List<?>) subjectAltName;
        if ((entry == null) || (entry.size() < 2)) {
          continue;
        }
        final Integer altNameType = (Integer) entry.get(0);
        if (altNameType == null) {
          continue;
        }
        if (altNameType == type) {
          final String altName = (String) entry.get(1);
          if (altName != null) {
            result.add(altName);
          }
        }
      }
      return result;
    } catch (final CertificateParsingException e) {
      return Collections.emptyList();
    }
  }

  /**
   * Returns true if {@code hostName} matches the name or pattern {@code cn}.
   *
   * @param hostName
   *          lowercase host name.
   * @param cn
   *          certificate host name. May include wildcards like
   *          {@code *.android.com}.
   */
  public static boolean verifyHostName(final String hostName, String cn) {
    // Check length == 0 instead of .isEmpty() to support Java 5.
    if ((hostName == null) || (hostName.length() == 0) || (cn == null) || (cn.length() == 0)) {
      return false;
    }

    cn = cn.toLowerCase(Locale.US);

    if (!cn.contains("*")) {
      return hostName.equals(cn);
    }

    if (cn.startsWith("*.") && hostName.regionMatches(0, cn, 2, cn.length() - 2)) {
      return true; // "*.foo.com" matches "foo.com"
    }

    final int asterisk = cn.indexOf('*');
    final int dot = cn.indexOf('.');
    if (asterisk > dot) {
      return false; // malformed; wildcard must be in the first part of the cn
    }

    if (!hostName.regionMatches(0, cn, 0, asterisk)) {
      return false; // prefix before '*' doesn't match
    }

    final int suffixLength = cn.length() - (asterisk + 1);
    final int suffixStart = hostName.length() - suffixLength;
    if (hostName.indexOf('.', asterisk) < suffixStart) {
      return false; // wildcard '*' can't match a '.'
    }

    if (!hostName.regionMatches(suffixStart, cn, asterisk + 1, suffixLength)) {
      return false; // suffix after '*' doesn't match
    }

    return true;
  }
}
