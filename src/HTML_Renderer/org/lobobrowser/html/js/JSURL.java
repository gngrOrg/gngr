package org.lobobrowser.html.js;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lobobrowser.js.AbstractScriptableDelegate;
import org.lobobrowser.util.Urls;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

/**
 * Implementation of the WHATWG URL API exposed as global {@code URL} in JavaScript.
 */
public class JSURL extends AbstractScriptableDelegate {
  private static final Pattern SCHEME_PATTERN = Pattern.compile("^([A-Za-z][A-Za-z0-9+.-]*):(.*)$", Pattern.DOTALL);

  private String protocol;
  private String username;
  private String password;
  private String hostname;
  private String port;
  private String pathname;
  private String search;
  private String hash;
  private boolean opaque;
  private final URLSearchParams searchParams;

  public JSURL(final String url, final String base, final Scriptable scope) {
    parse(url, base);
    this.searchParams = new URLSearchParams(this.search, scope, this::syncSearchFromParams);
  }

  private void syncSearchFromParams() {
    final String serialized = searchParams.toString();
    this.search = serialized.isEmpty() ? "" : "?" + serialized;
  }

  private static String trimAsciiControl(final String s) {
    if (s == null) {
      return null;
    }
    int start = 0;
    int end = s.length();
    while (start < end && s.charAt(start) <= 0x20) {
      start++;
    }
    while (end > start && s.charAt(end - 1) <= 0x20) {
      end--;
    }
    return s.substring(start, end);
  }

  private static boolean isSpecialScheme(final String schemeLower) {
    return "http".equals(schemeLower)
        || "https".equals(schemeLower)
        || "ws".equals(schemeLower)
        || "wss".equals(schemeLower)
        || "ftp".equals(schemeLower)
        || "file".equals(schemeLower);
  }

  private static int defaultPortForScheme(final String schemeLower) {
    switch (schemeLower) {
      case "http":
      case "ws":
        return 80;
      case "https":
      case "wss":
        return 443;
      case "ftp":
        return 21;
      default:
        return -1;
    }
  }

  private static String toJavaURLScheme(final String schemeLower) {
    if ("ws".equals(schemeLower)) {
      return "http";
    }
    if ("wss".equals(schemeLower)) {
      return "https";
    }
    return schemeLower;
  }

  private void copyFrom(final JSURL other) {
    this.protocol = other.protocol;
    this.username = other.username;
    this.password = other.password;
    this.hostname = other.hostname;
    this.port = other.port;
    this.pathname = other.pathname;
    this.search = other.search;
    this.hash = other.hash;
    this.opaque = other.opaque;
  }

  private void parse(final String rawInput, final String rawBase) {
    final String input = trimAsciiControl(rawInput);
    if (input == null) {
      throw ScriptRuntime.constructError("TypeError", "Invalid URL: null");
    }

    JSURL baseParsed = null;
    if (rawBase != null) {
      final String trimmedBase = trimAsciiControl(rawBase);
      if (trimmedBase == null || !SCHEME_PATTERN.matcher(trimmedBase).matches()) {
        throw ScriptRuntime.constructError("TypeError", "Invalid base URL: " + rawBase);
      }
      baseParsed = new JSURL(trimmedBase, null, null);
    }

    final Matcher schemeMatcher = SCHEME_PATTERN.matcher(input);
    if (schemeMatcher.matches()) {
      final String schemeLower = schemeMatcher.group(1).toLowerCase(Locale.ROOT);
      final String afterColon = schemeMatcher.group(2);
      if (!isSpecialScheme(schemeLower)) {
        parseOpaque(schemeLower, afterColon);
      } else {
        parseSpecialAbsolute(schemeLower, input);
      }
      return;
    }

    if (baseParsed == null) {
      throw ScriptRuntime.constructError("TypeError", "Invalid URL: " + rawInput);
    }

    if (input.isEmpty()) {
      copyFrom(baseParsed);
      this.hash = "";
      return;
    }

    if (input.charAt(0) == '#') {
      copyFrom(baseParsed);
      setHash(input);
      return;
    }

    if (baseParsed.opaque) {
      throw ScriptRuntime.constructError("TypeError", "Invalid URL: " + rawInput);
    }

    if (input.charAt(0) == '?') {
      copyFrom(baseParsed);
      final int hashIdx = input.indexOf('#');
      if (hashIdx >= 0) {
        setSearchInternal(input.substring(0, hashIdx));
        setHash(input.substring(hashIdx));
      } else {
        setSearchInternal(input);
        this.hash = "";
      }
      return;
    }

    final String baseScheme = baseParsed.protocol.substring(0, baseParsed.protocol.length() - 1);
    final String javaBaseScheme = toJavaURLScheme(baseScheme);
    final String baseForJava = javaBaseScheme.equals(baseScheme)
        ? baseParsed.getHref()
        : javaBaseScheme + baseParsed.getHref().substring(baseScheme.length());
    try {
      final URL baseJavaUrl = new URL(baseForJava);
      final URL resolved = Urls.createURL(baseJavaUrl, input);
      populateFromJavaURL(baseScheme, resolved);
    } catch (final MalformedURLException e) {
      throw ScriptRuntime.constructError("TypeError", "Invalid URL: " + rawInput);
    }
  }

  private void parseOpaque(final String schemeLower, final String afterColon) {
    this.protocol = schemeLower + ":";
    this.username = "";
    this.password = "";
    this.hostname = "";
    this.port = "";
    this.opaque = true;

    String rest = afterColon;
    final int hashIdx = rest.indexOf('#');
    if (hashIdx >= 0) {
      setHash(rest.substring(hashIdx));
      rest = rest.substring(0, hashIdx);
    } else {
      this.hash = "";
    }

    final int queryIdx = rest.indexOf('?');
    if (queryIdx >= 0) {
      setSearchInternal(rest.substring(queryIdx));
      rest = rest.substring(0, queryIdx);
    } else {
      this.search = "";
    }

    this.pathname = rest;
  }

  private void parseSpecialAbsolute(final String schemeLower, final String input) {
    final String javaScheme = toJavaURLScheme(schemeLower);
    final String javaInput = javaScheme.equals(schemeLower)
        ? input
        : javaScheme + input.substring(schemeLower.length());
    try {
      final URL parsed = new URL(javaInput);
      populateFromJavaURL(schemeLower, parsed);
    } catch (final MalformedURLException e) {
      throw ScriptRuntime.constructError("TypeError", "Invalid URL: " + input);
    }
  }

  private void populateFromJavaURL(final String schemeLower, final URL url) {
    this.protocol = schemeLower + ":";
    this.opaque = false;

    final String userInfo = url.getUserInfo();
    if (userInfo == null || userInfo.isEmpty()) {
      this.username = "";
      this.password = "";
    } else {
      final int colonIdx = userInfo.indexOf(':');
      if (colonIdx >= 0) {
        this.username = userInfo.substring(0, colonIdx);
        this.password = userInfo.substring(colonIdx + 1);
      } else {
        this.username = userInfo;
        this.password = "";
      }
    }

    final String rawHost = url.getHost();
    this.hostname = rawHost == null ? "" : rawHost.toLowerCase(Locale.ROOT);

    final int p = url.getPort();
    if (p == -1 || p == defaultPortForScheme(schemeLower)) {
      this.port = "";
    } else {
      this.port = String.valueOf(p);
    }

    final String rawPath = url.getPath();
    this.pathname = (rawPath == null || rawPath.isEmpty()) ? "/" : rawPath;

    final String rawQuery = url.getQuery();
    this.search = (rawQuery == null || rawQuery.isEmpty()) ? "" : "?" + rawQuery;

    final String rawRef = url.getRef();
    this.hash = (rawRef == null || rawRef.isEmpty()) ? "" : "#" + rawRef;
  }

  private void setSearchInternal(final String value) {
    if (value == null || value.isEmpty() || "?".equals(value)) {
      this.search = "";
    } else if (value.charAt(0) == '?') {
      this.search = value;
    } else {
      this.search = "?" + value;
    }
  }

  public String getHref() {
    if (opaque) {
      return protocol + pathname + search + hash;
    }
    final StringBuilder sb = new StringBuilder();
    sb.append(protocol).append("//");
    if (!username.isEmpty() || !password.isEmpty()) {
      sb.append(username);
      if (!password.isEmpty()) {
        sb.append(':').append(password);
      }
      sb.append('@');
    }
    sb.append(getHost());
    sb.append(pathname);
    sb.append(search);
    sb.append(hash);
    return sb.toString();
  }

  public void setHref(final String value) {
    parse(value, null);
    this.searchParams.resetFromQuery(this.search);
  }

  public String getOrigin() {
    if (opaque || "file:".equals(protocol)) {
      return "null";
    }
    return protocol + "//" + getHost();
  }

  public String getProtocol() {
    return protocol;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getHost() {
    return port.isEmpty() ? hostname : hostname + ":" + port;
  }

  public String getHostname() {
    return hostname;
  }

  public String getPort() {
    return port;
  }

  public String getPathname() {
    return pathname;
  }

  public String getSearch() {
    return search;
  }

  public void setSearch(final String value) {
    setSearchInternal(value);
    this.searchParams.resetFromQuery(this.search);
  }

  public String getHash() {
    return hash;
  }

  public void setHash(final String value) {
    if (value == null || value.isEmpty() || "#".equals(value)) {
      this.hash = "";
    } else if (value.charAt(0) == '#') {
      this.hash = value;
    } else {
      this.hash = "#" + value;
    }
  }

  public URLSearchParams getSearchParams() {
    return searchParams;
  }

  @Override
  public String toString() {
    return getHref();
  }

  public String toJSON() {
    return getHref();
  }
}
