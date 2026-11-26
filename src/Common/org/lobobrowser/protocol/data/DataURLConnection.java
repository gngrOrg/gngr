package org.lobobrowser.protocol.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * http://www.ietf.org/rfc/rfc2397.txt
 *
 *
 * dataurl := "data:" [ mediatype ] [ ";base64" ] "," data mediatype := [ type
 * "/" subtype ] *( ";" parameter ) data := *urlchar parameter := attribute "="
 * value
 *
 *
 * @author toenz
 *
 */
public class DataURLConnection extends URLConnection {

  private final LinkedHashMap<String, String> headerMap = new LinkedHashMap<>();
  private List<Map.Entry<String, String>> headerEntries = Collections.emptyList();
  private byte[] content = new byte[0];

  protected DataURLConnection(final URL url) {
    super(url);
    loadHeaderMap();
  }

  @Override
  public void connect() throws IOException {
  }

  private void loadHeaderMap() {
    this.headerMap.clear();
    final String path = getURL().getPath();
    int index2 = path.indexOf(',');
    if (index2 == -1) {
      index2 = path.lastIndexOf(';');
    }
    final String value = path.substring(index2 + 1);
    final String mediatype;
    if (index2 == -1) {
      mediatype = "text/plain;charset=US-ASCII";
    } else {
      mediatype = path.substring(0, index2).trim();
    }

    boolean base64 = false;
    String[] split = mediatype.split("[;]");
    if (split.length == 0) {
      split = new String[] { "text/plain" };
    } else {
      split[0] = split[0].trim();
      if (split[0].isEmpty()) {
        split[0] = "text/plain";
      }
    }

    final StringBuilder contentTypeBuilder = new StringBuilder(split[0]);
    final LinkedHashMap<String, String> params = new LinkedHashMap<>();
    for (int i = 1; i < split.length; i++) {
      final String token = split[i].trim();
      final int eqIdx = token.indexOf('=');
      if (eqIdx >= 0) {
        final String attr = token.substring(0, eqIdx).trim().toLowerCase(Locale.ROOT);
        final String rawVal = token.substring(eqIdx + 1).trim();
        final String decodedVal = decodePercentToString(rawVal);
        if (!attr.isEmpty()) {
          params.put(attr, decodedVal);
          contentTypeBuilder.append(';').append(attr).append('=').append(decodedVal);
        }
      } else if (token.equalsIgnoreCase("base64")) {
        base64 = true;
      }
    }

    this.headerMap.put("content-type", contentTypeBuilder.toString());
    this.headerMap.putAll(params);
    this.headerEntries = new ArrayList<>(this.headerMap.entrySet());

    if (base64) {
      final String base64Value = removeSpaceCharacters(value);
      try {
        this.content = Base64.getDecoder().decode(base64Value);
      } catch (final IllegalArgumentException iae) {
        // TODO: Fix for GH #15, but need to verify if this is specified by a standard
        final String percentDecoded = decodePercentToString(value);
        this.content = Base64.getDecoder().decode(removeSpaceCharacters(percentDecoded));
      }
    } else {
      this.content = decodeUrl(value.toCharArray());
    }
  }

  private static String decodePercentToString(final String value) {
    return new String(decodeUrl(value.toCharArray()), StandardCharsets.UTF_8);
  }

  private static String removeSpaceCharacters(String value) {
    value = value.replace("\n", "");
    value = value.replace("\r", "");
    value = value.replace(" ", "");
    value = value.replace("\t", "");
    value = value.replace("\f", "");
    return value;
  }

  @Override
  public int getContentLength() {
    return content.length;
  }

  @Override
  public String getHeaderField(final int n) {
    return (n >= 0 && n < headerEntries.size()) ? headerEntries.get(n).getValue() : null;
  }

  @Override
  public String getHeaderFieldKey(final int n) {
    return (n >= 0 && n < headerEntries.size()) ? headerEntries.get(n).getKey() : null;
  }

  @Override
  public String getHeaderField(final String name) {
    return name == null ? null : headerMap.get(name.toLowerCase(Locale.ROOT));
  }

  @Override
  public Map<String, List<String>> getHeaderFields() {
    final Map<String, List<String>> fields = new LinkedHashMap<>();
    for (final Map.Entry<String, String> entry : headerEntries) {
      fields.put(entry.getKey(), Collections.singletonList(entry.getValue()));
    }
    return Collections.unmodifiableMap(fields);
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new ByteArrayInputStream(content);
  }

  public static final byte[] decodeUrl(final char[] chars) {
    final char ESCAPE_CHAR = '%';
    if (chars == null) {
      return null;
    }
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    for (int i = 0; i < chars.length; i++) {
      final char c = chars[i];
      if (c == ESCAPE_CHAR) {
        if (i + 2 < chars.length) {
          final int u = digit16(chars[i + 1]);
          final int l = digit16(chars[i + 2]);
          if (u >= 0 && l >= 0) {
            buffer.write((u << 4) | l);
            i += 2;
            continue;
          }
        }
        buffer.write(ESCAPE_CHAR);
      } else if (c <= 0x7F) {
        buffer.write(c);
      } else {
        final String str;
        if (Character.isHighSurrogate(c) && (i + 1 < chars.length) && Character.isLowSurrogate(chars[i + 1])) {
          str = new String(new char[] { c, chars[++i] });
        } else {
          str = String.valueOf(c);
        }
        final byte[] utf8Bytes = str.getBytes(StandardCharsets.UTF_8);
        buffer.write(utf8Bytes, 0, utf8Bytes.length);
      }
    }
    return buffer.toByteArray();
  }

  private static int digit16(final char c) {
    if ((c >= '0') && (c <= '9')) {
      return c - '0';
    } else if ((c >= 'A') && (c <= 'F')) {
      return (10 + c) - 'A';
    } else if ((c >= 'a') && (c <= 'f')) {
      return (10 + c) - 'a';
    } else {
      return -1;
    }
  }
}
