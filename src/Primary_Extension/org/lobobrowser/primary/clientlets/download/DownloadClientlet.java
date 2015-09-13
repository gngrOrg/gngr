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
package org.lobobrowser.primary.clientlets.download;

import java.io.InputStream;

import org.lobobrowser.clientlet.CancelClientletException;
import org.lobobrowser.clientlet.Clientlet;
import org.lobobrowser.clientlet.ClientletContext;
import org.lobobrowser.clientlet.ClientletException;
import org.lobobrowser.clientlet.ClientletResponse;
import org.lobobrowser.primary.gui.download.DownloadDialog;
import org.lobobrowser.request.SilentUserAgentContextImpl;
import org.lobobrowser.ua.UserAgentContext;
import org.lobobrowser.util.Strings;
import org.lobobrowser.util.Urls;

public final class DownloadClientlet implements Clientlet {

  public void process(final ClientletContext context) throws ClientletException {
    final ClientletResponse response = context.getResponse();
    final java.net.URL url = response.getResponseURL();
    if (url.getProtocol().equals("file") && "".equals(url.getHost())) {
      final String shorterPath = Strings.truncate(Urls.getNoRefForm(url), 64);
      context.getNavigatorFrame().alert("There are no extensions that can render\r\n" + shorterPath + ".");
      throw new CancelClientletException("cancel");
    }
    if (!"GET".equals(response.getLastRequestMethod())) {
      final String shorterPath = Strings.truncate(Urls.getNoRefForm(url), 64);
      context.getNavigatorFrame().alert("Cannot download document that is not accessed with method GET:\r\n" + shorterPath + ".");
      throw new CancelClientletException("cancel");
    }
    // Load a bit of content to determine transfer speed
    int transferSpeed = -1;
    final int contentLength = response.getContentLength();
    if (contentLength > 0) {
      try (final InputStream in = response.getInputStream()) {
        final long baseTime = System.currentTimeMillis();
        final long maxElapsed = 1000;
        final byte[] buffer = new byte[4096];
        int numRead;
        int totalRead = 0;
        while (((System.currentTimeMillis() - baseTime) < maxElapsed) && ((numRead = in.read(buffer)) != -1)) {
          totalRead += numRead;
        }
        // Note: This calculation depends on content not being stored in cache.
        // It works just because downloads are not stored in the cache.
        final long elapsed = System.currentTimeMillis() - baseTime;
        if (elapsed > 0) {
          transferSpeed = (int) Math.round((double) totalRead / elapsed);
        }
      } catch (final java.io.IOException ioe) {
        throw new ClientletException(ioe);
      }
    }
    final UserAgentContext uaContext = new SilentUserAgentContextImpl(context.getNavigatorFrame());
    final DownloadDialog dialog = new DownloadDialog(response, url, transferSpeed, uaContext);
    dialog.setTitle("Download " + Urls.getNoRefForm(url));
    dialog.pack();
    dialog.setLocationByPlatform(true);
    dialog.setVisible(true);

    // Cancel current transfer
    throw new CancelClientletException("download");
  }
}
