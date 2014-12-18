package org.lobobrowser.main;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class ZipEntryHandler extends URLStreamHandler {
  private final Map<String, byte[]> entryBufMap = new HashMap<>();

  ZipEntryHandler(final ZipInputStream is) throws IOException {
    processStream(is);
  }

  public void processStream(final ZipInputStream is) throws IOException {
    final byte[] tempBuffer = new byte[8192 * 2];
    ZipEntry nextEntry = null;
    do {
      nextEntry = is.getNextEntry();
      if (nextEntry != null) {
        if (!nextEntry.isDirectory()) {
          {
            int bytesRead = -1;
            final ByteArrayOutputStream streamBuilder = new ByteArrayOutputStream();
            while ((bytesRead = is.read(tempBuffer)) != -1) {
              streamBuilder.write(tempBuffer, 0, bytesRead);
            }
            // System.out.println(nextEntry.getName());
            entryBufMap.put(nextEntry.getName(), streamBuilder.toByteArray());
          }
        }
        is.closeEntry();
      }
    } while (nextEntry != null);
  }

  @Override
  protected URLConnection openConnection(final URL u) throws IOException {
    // System.out.println("Request: " + u);
    if (ExtensionManager.ZIPENTRY_PROTOCOL.equals(u.getProtocol())) {
      // TODO: Check url host for faster processing. Or use a common zip handler
      final String uStr = u.toExternalForm();
      final String requestName = uStr.substring(uStr.indexOf("!/") + 2);
      if (requestName.length() == 0) {
        return new URLConnection(u) {

          @Override
          public void connect() throws IOException {
            // TODO Auto-generated method stub

          }
        };
      }
      final byte[] entry = entryBufMap.get(requestName);
      if (requestName.contains("jooq")) {
        System.out.println("req: " + requestName + " entry: " + entry);
      }
      return entry == null ? null : new URLConnection(u) {

        @Override
        public void connect() throws IOException {
        }

        @Override
        public InputStream getInputStream() throws IOException {
          return new ByteArrayInputStream(entry);
        }
      };
    } else {
      return null;
    }

  }
}