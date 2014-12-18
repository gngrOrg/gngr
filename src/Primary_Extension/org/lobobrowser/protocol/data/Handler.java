package org.lobobrowser.protocol.data;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

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
public class Handler extends URLStreamHandler {

  @Override
  protected URLConnection openConnection(final URL url) throws IOException {
    return new DataURLConnection(url);
  }

  @Override
  protected void parseURL(final URL u, final String spec, final int start, final int limit) {
    final int index = spec.toLowerCase().indexOf(":");
    final String protocol = "data";
    final String path = spec.substring(index + 1);

    setURL(u, protocol,
        /* host */null,
        /* port */80,
        /* authority */null,
        /* userinfo */null,
        /* path */path,
        /* query */null,
        /* ref */null);
  }

  @Override
  protected String toExternalForm(final URL u) {
    return u.getProtocol() + ":" + u.getPath();
  }

}
