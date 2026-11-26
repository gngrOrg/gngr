package org.lobobrowser.ua;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides information about the user agent (browser) driving the parser and/or
 * renderer.
 * <p>
 * A simple implementation of this interface is provided in
 * {@link org.lobobrowser.html.test.SimpleUserAgentContext}.
 *
 * @see HtmlRendererContext#getUserAgentContext()
 * @see org.lobobrowser.html.parser.DocumentBuilderImpl#DocumentBuilderImpl(UserAgentContext)
 */
public interface UserAgentContext {
  public enum RequestKind {
    /*
     * persistenceId values are persisted as 2-bit slot indices in H2 PERMISSIONS
     * (slot 0 is reserved for the host default; kind slots are 1..15).
     * Never change or reuse an existing persistenceId; if a constant is
     * removed, retire its persistenceId permanently.
     */
    Image("Img", 1),
    CSS("CSS", 2),
    Cookie("Cookie", 3),
    JavaScript("JS", 4),
    Frame("Frame", 5),
    XHR("XHR", 6),
    Referrer("Referrer", 7),
    UnsecuredHTTP("Unsecured HTTP", 8);

    public final String shortName;
    public final int persistenceId;

    RequestKind(final String shortName, final int persistenceId) {
      this.shortName = shortName;
      this.persistenceId = persistenceId;
    }

    private static final RequestKind[] VALUES = RequestKind.values();

    static {
      final Set<Integer> seenIds = new HashSet<>();
      for (final RequestKind kind : VALUES) {
        if (kind.persistenceId < 1) {
          throw new IllegalStateException("RequestKind.persistenceId must be >= 1: " + kind + " (" + kind.persistenceId + ")");
        }
        if (!seenIds.add(kind.persistenceId)) {
          throw new IllegalStateException("Duplicate RequestKind.persistenceId: " + kind.persistenceId + " on " + kind);
        }
      }
    }

    /**
     * Returns the {@link RequestKind} for a runtime enum ordinal.
     * <p>
     * For transient in-memory indexing only; never use ordinals for persistence
     * (use {@link #persistenceId} instead).
     */
    public static RequestKind forOrdinal(final int o) {
      return VALUES[o];
    }

    public static int numKinds() {
      return VALUES.length;
    }
  }

  static public class Request {
    final public RequestKind kind;
    final public URL url;

    public Request(final URL url, final RequestKind kind) {
      this.kind = kind;
      this.url = url;
    }

    @Override
    public String toString() {
      return kind.toString() + ": " + url;
    }
  }

  public boolean isRequestPermitted(final Request request);

  /**
   * Creates an instance of {@link org.lobobrowser.html.HttpRequest} which can
   * be used by the renderer to load images, scripts, external style sheets, and
   * implement the Javascript XMLHttpRequest class (AJAX).
   */
  public NetworkRequest createHttpRequest();

  /**
   * Gets browser "code" name.
   */
  public String getAppCodeName();

  /**
   * Gets browser application name.
   */
  public String getAppName();

  /**
   * Gets browser application version.
   */
  public String getAppVersion();

  /**
   * Gets browser application minor version.
   */
  public String getAppMinorVersion();

  /**
   * Gets browser language code. See <a
   * href="http://en.wikipedia.org/wiki/List_of_ISO_639-1_codes">ISO 639-1
   * codes</a>.
   */
  public String getBrowserLanguage();

  /**
   * Returns a boolean value indicating whether cookies are enabled in the user
   * agent. This value is used for reporting purposes only. TODO: Remove
   */
  public boolean isCookieEnabled();

  /**
   * Returns a boolean value indicating whether scripting is enabled in the user
   * agent. If this value is <code>false</code>, the parser will not process
   * scripts and Javascript element attributes will have no effect. TODO: Remove
   */
  public boolean isScriptingEnabled();

  /**
   * Returns a boolean value indicating whether remote (non-inline) CSS
   * documents should be loaded. TODO: Remove
   */
  public boolean isExternalCSSEnabled();

  /**
   * Returns a boolean value indicating whether STYLE tags should be processed.
   * TODO: Remove
   */
  public boolean isInternalCSSEnabled();

  /**
   * Gets the name of the user's operating system.
   */
  public String getPlatform();

  /**
   * Should return the string used in the User-Agent header.
   */
  public String getUserAgent();

  /**
   * Method used to implement Javascript <code>document.cookie</code> property.
   */
  public String getCookie(java.net.URL url);

  /**
   * Method used to implement <code>document.cookie</code> property.
   *
   * @param cookieSpec
   *          Specification of cookies, as they would appear in the Set-Cookie
   *          header value of HTTP.
   */
  public void setCookie(java.net.URL url, String cookieSpec);

  /**
   * Gets the security policy for scripting. Return <code>null</code> if
   * JavaScript code is trusted.
   */
  public java.security.Policy getSecurityPolicy();

  /**
   * Gets the scripting optimization level, which is a value equivalent to
   * Rhino's optimization level.
   */
  public int getScriptingOptimizationLevel();

  /**
   * Returns true if the current media matches the name provided.
   *
   * @param mediaName
   *          Media name, which may be <code>screen</code>, <code>tty</code>,
   *          etc. (See <a href=
   *          "http://www.w3.org/TR/REC-html40/types.html#type-media-descriptors"
   *          >HTML Specification</a>).
   */
  public boolean isMedia(String mediaName);

  public String getVendor();

  public String getProduct();
}
