package org.lobobrowser.html.js;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.js.AbstractScriptableDelegate;
import org.w3c.dom.Document;

public class Location extends AbstractScriptableDelegate {
  private static final Logger logger = Logger.getLogger(Location.class.getName());
  private final Window window;

  Location(final Window window) {
    this.window = window;
  }

  private URL getURL() {
    URL url;
    try {
      final Document document = this.window.getDocumentNode();
      url = document == null ? null : new URL(document.getDocumentURI());
    } catch (final java.net.MalformedURLException mfu) {
      url = null;
    }
    return url;
  }

  public String getHash() {
    final URL url = this.getURL();
    return url == null ? null : url.getRef();
  }

  public String getHost() {
    final URL url = this.getURL();
    if (url == null) {
      return null;
    }
    return url.getHost() + (url.getPort() == -1 ? "" : ":" + url.getPort());
  }

  public String getHostname() {
    final URL url = this.getURL();
    if (url == null) {
      return null;
    }
    return url.getHost();
  }

  public String getPathname() {
    final URL url = this.getURL();
    return url == null ? null : url.getPath();
  }

  public String getPort() {
    final URL url = this.getURL();
    if (url == null) {
      return null;
    }
    final int port = url.getPort();
    return port == -1 ? null : String.valueOf(port);
  }

  public String getProtocol() {
    final URL url = this.getURL();
    if (url == null) {
      return null;
    }
    return url.getProtocol() + ":";
  }

  public String getSearch() {
    final URL url = this.getURL();
    final String query = url == null ? null : url.getQuery();
    // Javascript requires "?" in its search string.
    return query == null ? "" : "?" + query;
  }

  private String target;

  public String getTarget() {
    return this.target;
  }

  public void setTarget(final String value) {
    this.target = value;
  }

  public String getHref() {
    final Document document = this.window.getDocumentNode();
    return document == null ? null : document.getDocumentURI();
  }

  public void setHref(final String uri) {
    final HtmlRendererContext rcontext = this.window.getHtmlRendererContext();
    if (rcontext != null) {
      try {
        URL url;
        final Document document = this.window.getDocumentNode();
        if (document instanceof HTMLDocumentImpl) {
          final HTMLDocumentImpl docImpl = (HTMLDocumentImpl) document;
          url = docImpl.getFullURL(uri);
        } else {
          url = new URL(uri);
        }
        rcontext.navigate(url, this.target);
      } catch (final java.net.MalformedURLException mfu) {
        logger.log(Level.WARNING, "setHref(): Malformed location: [" + uri + "].", mfu);
      }
    }
  }

  public void reload() {
    // TODO: This is not really reload.
    final Document document = this.window.getDocumentNode();
    if (document instanceof HTMLDocumentImpl) {
      final HTMLDocumentImpl docImpl = (HTMLDocumentImpl) document;
      final HtmlRendererContext rcontext = docImpl.getHtmlRendererContext();
      if (rcontext != null) {
        rcontext.reload();
      } else {
        docImpl.warn("reload(): No renderer context in Location's document.");
      }
    }
  }

  public void replace(final String href) {
    this.setHref(href);
  }

  @Override
  public String toString() {
    // This needs to be href. Callers
    // rely on that.
    return this.getHref();
  }
}
