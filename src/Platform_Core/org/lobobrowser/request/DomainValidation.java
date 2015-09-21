package org.lobobrowser.request;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.util.Urls;

import de.malkusch.whoisServerList.publicSuffixList.PublicSuffixList;
import de.malkusch.whoisServerList.publicSuffixList.PublicSuffixListFactory;

public final class DomainValidation {

  public static boolean isValidCookieDomain(String domain, final String requestHostName) {
    String plainDomain;
    if (!domain.startsWith(".")) {
      // Valid domains must start with a dot according to RFC 2109, but
      // RFC 2965 specifies a dot is prepended in the Set-Cookie2 header.
      plainDomain = domain;
      domain = "." + domain;
    } else {
      plainDomain = domain.substring(1);
    }
    final String plainDomainTL = plainDomain.toLowerCase();
    final String hostNameTL = requestHostName.toLowerCase();
    if (plainDomainTL.equals(hostNameTL)) {
      return true;
    } else {
      if (!hostNameTL.endsWith(plainDomainTL)) {
        return false;
      } else {
        // plainDomainTL is a suffix of hostName TL. Now ensure the first non-common character is a '.',
        // and there is a residual character after that
        final int nonCommonLength = hostNameTL.length() - plainDomainTL.length();
        final boolean residualCharacterExists = nonCommonLength >= 2;
        if (!residualCharacterExists) {
          return false;
        } else {
          final char firstNonCommonCharacter = hostNameTL.charAt(nonCommonLength - 1);
          if (firstNonCommonCharacter != '.') {
            return false;
          }
        }
      }
    }

    return !isPublicSuffix(plainDomain);
  }

  private static final PublicSuffixList suffixList = new PublicSuffixListFactory().build();

  /**
   * Returns true if the given domain is a public suffix.
   *
   * @param domain
   *          The domain to check. Expected <b>not</b> to have any leading or
   *          trailing '.'
   * @return true if the given domain is a public suffix
   */
  public static boolean isPublicSuffix(final String domain) {
    return suffixList.isPublicSuffix(domain);
  }

  /**
   * Returns a collection of domains that are acceptable for cookies originating
   * from the given hostname
   */
  public static Collection<String> getPossibleDomains(final String hostName) {
    // TODO: reuse collection object instead of creating a new one per recursive call.
    final Collection<String> domains = new LinkedList<>();
    domains.add(hostName);
    final int dotIdx = hostName.indexOf('.', 1);
    if (dotIdx == -1) {
      return domains;
    }
    final String testDomain = hostName.substring(dotIdx);
    if (!isValidCookieDomain(testDomain, hostName)) {
      return domains;
    }
    domains.addAll(getPossibleDomains(testDomain.substring(1)));
    return domains;
  }

  public static boolean isLikelyHostName(final String name) {
    final String nameTL = name.toLowerCase();
    if (nameTL.startsWith("www.")) {
      return true;
    }
    if (endsWithGTLD(name)) {
      return true;
    }
    final int lastDotIdx = nameTL.lastIndexOf('.');
    if (lastDotIdx == -1) {
      return false;
    }
    // Check for country code.
    return lastDotIdx == (nameTL.length() - 3);
  }

  private static boolean endsWithGTLD(final String name) {
    if (name.length() == 0) {
      return false;
    } else if (isPublicSuffix(name)) {
      return true;
    } else {
      final int sepIndex = name.indexOf('.');
      if (sepIndex < 0) {
        return false;
      } else {
        return endsWithGTLD(name.substring(sepIndex + 1));
      }
    }
  }

  public static @NonNull URL guessURL(URL baseURL, String spec) throws MalformedURLException {
    URL finalURL;
    try {
      if (baseURL != null) {
        final int colonIdx = spec.indexOf(':');
        final String newProtocol = colonIdx == -1 ? null : spec.substring(0, colonIdx);
        if ((newProtocol != null) && !newProtocol.equalsIgnoreCase(baseURL.getProtocol())) {
          baseURL = null;
        }
      }
      finalURL = Urls.createURL(baseURL, spec);
    } catch (final MalformedURLException mfu) {
      spec = spec.trim();
      final int idx = spec.indexOf(':');
      if (idx == -1) {
        final int slashIdx = spec.indexOf('/');
        if (slashIdx == 0) {
          // A file, absolute
          finalURL = new URL("file:" + spec);
        } else {
          if (slashIdx == -1) {
            // No slash, no colon, must be host.
            finalURL = new URL(baseURL, "http://" + spec);
          } else {
            final String possibleHost = spec.substring(0, slashIdx).toLowerCase();
            finalURL = guessProtocol(baseURL, spec, possibleHost);
          }
        }
      } else {
        if (idx == 1) {
          // Likely a drive
          finalURL = new URL(baseURL, "file:" + spec);
        } else {
          final String possibleHost = spec.substring(0, idx).toLowerCase();
          finalURL = guessProtocol(baseURL, spec, possibleHost);
        }
      }
    }
    if (!"".equals(finalURL.getHost()) && (finalURL.toExternalForm().indexOf(' ') != -1)) {
      throw new MalformedURLException("There are blanks in the URL: " + finalURL.toExternalForm());
    }
    return finalURL;
  }

  private static URL guessProtocol(final URL baseURL, final String spec, final String possibleHost) throws MalformedURLException {
    if (DomainValidation.isLikelyHostName(possibleHost)) {
      // TODO: Use https when possible
      return new URL(baseURL, "http://" + spec);
    } else {
      // TODO: Should file URLs be guessed? Probably not.
      return new URL(baseURL, "file:" + spec);
    }
  }

  public static @NonNull URL guessURL(final String spec) throws MalformedURLException {
    return guessURL(null, spec);
  }
}
