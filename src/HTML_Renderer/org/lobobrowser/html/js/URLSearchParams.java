package org.lobobrowser.html.js;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.lobobrowser.js.AbstractScriptableDelegate;
import org.lobobrowser.js.HideFromJS;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Implementation of the WHATWG URLSearchParams API exposed to JavaScript.
 */
public class URLSearchParams extends AbstractScriptableDelegate {
  private static final class Entry {
    final String name;
    String value;

    Entry(final String name, final String value) {
      this.name = name;
      this.value = value;
    }
  }

  private final List<Entry> entries = new ArrayList<>();
  private final Scriptable scope;
  private final Runnable onChange;

  public URLSearchParams() {
    this("", null, null);
  }

  public URLSearchParams(final String init) {
    this(init, null, null);
  }

  public URLSearchParams(final String init, final Scriptable scope, final Runnable onChange) {
    this.scope = scope;
    this.onChange = onChange;
    parseQuery(init);
  }

  public URLSearchParams(final URLSearchParams other, final Scriptable scope) {
    this.scope = scope;
    this.onChange = null;
    if (other != null) {
      for (final Entry entry : other.entries) {
        this.entries.add(new Entry(entry.name, entry.value));
      }
    }
  }

  @HideFromJS
  void resetFromQuery(final String query) {
    this.entries.clear();
    parseQuery(query);
  }

  private void parseQuery(final String query) {
    if (query == null || query.isEmpty()) {
      return;
    }
    final String stripped = query.charAt(0) == '?' ? query.substring(1) : query;
    if (stripped.isEmpty()) {
      return;
    }
    final String[] pairs = stripped.split("&", -1);
    for (final String pair : pairs) {
      if (pair.isEmpty()) {
        continue;
      }
      final int eqIdx = pair.indexOf('=');
      final String rawName;
      final String rawValue;
      if (eqIdx >= 0) {
        rawName = pair.substring(0, eqIdx);
        rawValue = pair.substring(eqIdx + 1);
      } else {
        rawName = pair;
        rawValue = "";
      }
      entries.add(new Entry(decodeFormComponent(rawName), decodeFormComponent(rawValue)));
    }
  }

  private static String decodeFormComponent(final String s) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream(s.length());
    for (int i = 0; i < s.length(); i++) {
      final char ch = s.charAt(i);
      if (ch == '+') {
        out.write(' ');
      } else if (ch == '%' && i + 2 < s.length()) {
        final int hi = Character.digit(s.charAt(i + 1), 16);
        final int lo = Character.digit(s.charAt(i + 2), 16);
        if (hi >= 0 && lo >= 0) {
          out.write((hi << 4) | lo);
          i += 2;
          continue;
        }
        writeUtf8Char(out, ch);
      } else {
        writeUtf8Char(out, ch);
      }
    }
    return new String(out.toByteArray(), StandardCharsets.UTF_8);
  }

  private static void writeUtf8Char(final ByteArrayOutputStream out, final char ch) {
    if (ch < 0x80) {
      out.write(ch);
    } else {
      final byte[] bytes = String.valueOf(ch).getBytes(StandardCharsets.UTF_8);
      out.write(bytes, 0, bytes.length);
    }
  }

  private static String encodeFormComponent(final String s) {
    try {
      return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8.name());
    } catch (final UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

  private void notifyChanged() {
    if (onChange != null) {
      onChange.run();
    }
  }

  public void append(final String name, final String value) {
    entries.add(new Entry(name == null ? "" : name, value == null ? "" : value));
    notifyChanged();
  }

  public void delete(final String name) {
    boolean changed = false;
    final Iterator<Entry> it = entries.iterator();
    while (it.hasNext()) {
      if (it.next().name.equals(name)) {
        it.remove();
        changed = true;
      }
    }
    if (changed) {
      notifyChanged();
    }
  }

  public String get(final String name) {
    for (final Entry entry : entries) {
      if (entry.name.equals(name)) {
        return entry.value;
      }
    }
    return null;
  }

  public Object getAll(final String name) {
    final List<String> matches = new ArrayList<>();
    for (final Entry entry : entries) {
      if (entry.name.equals(name)) {
        matches.add(entry.value);
      }
    }
    final Context ctx = Context.getCurrentContext();
    if (scope != null && ctx != null) {
      return ctx.newArray(scope, matches.toArray());
    }
    return matches.toArray(new String[0]);
  }

  public boolean has(final String name) {
    for (final Entry entry : entries) {
      if (entry.name.equals(name)) {
        return true;
      }
    }
    return false;
  }

  public void set(final String name, final String value) {
    final String safeName = name == null ? "" : name;
    final String safeValue = value == null ? "" : value;
    boolean found = false;
    final Iterator<Entry> it = entries.iterator();
    while (it.hasNext()) {
      final Entry entry = it.next();
      if (entry.name.equals(safeName)) {
        if (!found) {
          entry.value = safeValue;
          found = true;
        } else {
          it.remove();
        }
      }
    }
    if (!found) {
      entries.add(new Entry(safeName, safeValue));
    }
    notifyChanged();
  }

  public void sort() {
    entries.sort(Comparator.comparing(e -> e.name));
    notifyChanged();
  }

  public int getSize() {
    return entries.size();
  }

  @Override
  public String toString() {
    if (entries.isEmpty()) {
      return "";
    }
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < entries.size(); i++) {
      if (i > 0) {
        sb.append('&');
      }
      final Entry entry = entries.get(i);
      sb.append(encodeFormComponent(entry.name));
      sb.append('=');
      sb.append(encodeFormComponent(entry.value));
    }
    return sb.toString();
  }
}
