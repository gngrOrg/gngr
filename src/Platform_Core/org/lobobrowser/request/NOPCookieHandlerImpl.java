package org.lobobrowser.request;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A CookieHandler that doesn't set or get cookies. The idea is to handle
 * cookies in a place where better context is available. The methods in this
 * interface get called from a global context where not much information about
 * the request is available.
 *
 * @author hrj
 *
 */
public class NOPCookieHandlerImpl extends CookieHandler {
  final Map<String, List<String>> emptyMap = new HashMap<>();

  @Override
  public Map<String, List<String>> get(final URI uri, final Map<String, List<String>> requestHeaders) throws IOException {
    // Intentionally left blank
    return emptyMap;
  }

  @Override
  public void put(final URI uri, final Map<String, List<String>> responseHeaders) throws IOException {
    // Intentionally left blank
  }
}
