package org.lobobrowser.protocol.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Base64;
import java.util.HashMap;

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

  private final HashMap<String, String> headerMap = new HashMap<>();
  private byte[] content = new byte[0];

  protected DataURLConnection(final URL url) {
    super(url);
    loadHeaderMap();
  }

  @Override
  public void connect() throws IOException {
  }

  private void loadHeaderMap() {
    final String UTF8 = "UTF-8";
    this.headerMap.clear();
    final String path = getURL().getPath();
    int index2 = path.indexOf(",");
    if (index2 == -1) {
      index2 = path.lastIndexOf(";");
    }
    String value = path.substring(index2 + 1).trim();
    String mediatype;
    if (index2 == -1) {
      mediatype = "text/plain;charset=US-ASCII";
    } else {
      mediatype = path.substring(0, index2).trim();
    }
    boolean base64 = false;
    String[] split = mediatype.split("[;]");
    if (split.length == 0) {
      split = new String[] {"text/plain"};
    } else if (split[0].equals("")) {
      split[0] = "text/plain";
    }

    this.headerMap.put("content-type", split[0]);

    try {
      for (int i = 1; i < split.length; i++) {
        if (split[i].contains("=")) {
          final int index = split[i].indexOf("=");
          final String attr = split[i].substring(0, index);
          final String v = split[i].substring(index + 1);
          this.headerMap.put(attr, java.net.URLDecoder.decode(v, UTF8));
        } else if (split[i].equalsIgnoreCase("base64")) {
          base64 = true;
        }
      }
      String charset = this.getHeaderField("charset");
      if (charset == null) {
        charset = UTF8;
      }
      value = removeSpaceCharacters(value);
      if (base64) {

        try {
          this.content = Base64.getDecoder().decode(value);
        } catch (final IllegalArgumentException iae) {
          // TODO: Fix for GH #15, but need to verify if this is specified by a standard
          value = URLDecoder.decode(value, "UTF-8");
          this.content = Base64.getDecoder().decode(value);
        }
      } else {
        this.content = decodeUrl(value.toCharArray());
      }
    } catch (final IOException e) {
      e.printStackTrace();
    }

  }

  private static String removeSpaceCharacters(String value) {
    value = value.replace("\n", "");
    value = value.replace("\r", "");
    value = value.replace(" ", "");
    value = value.replace("\t", "");
    return value;
  }

  @Override
  public int getContentLength() {
    return content.length;
  }

  @Override
  public String getHeaderField(final int n) {
    // TODO: Looks highly inefficient to convert the keyset to array every time!
    return headerMap.get(headerMap.keySet().toArray()[n]);
  }

  @Override
  public String getHeaderField(final String name) {
    return headerMap.get(name);
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
      final int b = chars[i];
      if (b == '+') {
        buffer.write(' ');
      } else if (b == ESCAPE_CHAR) {
        try {
          final int u = digit16(chars[++i]);
          final int l = digit16(chars[++i]);
          buffer.write((char) ((u << 4) + l));
        } catch (final ArrayIndexOutOfBoundsException e) {
          throw new RuntimeException("Invalid URL encoding: ", e);
        }
      } else {
        buffer.write(b);
      }
    }
    return buffer.toByteArray();
  }

  private static int digit16(final char c) {
    if ((c >= 'A') && (c <= 'Z')) {
      return (10 + c) - 'A';
    } else if ((c >= 'a') && (c <= 'z')) {
      return (10 + c) - 'a';
    } else {
      return c - '0';
    }
  }
}
