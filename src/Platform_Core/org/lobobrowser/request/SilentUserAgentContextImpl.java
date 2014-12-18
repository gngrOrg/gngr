package org.lobobrowser.request;

import java.security.Policy;
import java.util.HashSet;
import java.util.Set;

import org.lobobrowser.ua.NavigatorFrame;
import org.lobobrowser.ua.NetworkRequest;
import org.lobobrowser.ua.UserAgentContext;

public class SilentUserAgentContextImpl implements UserAgentContext {
  private static final Set<String> mediaNames = new HashSet<>();
  private final NavigatorFrame frame;

  static {
    // Media names supported here
    final Set<String> mn = mediaNames;
    mn.add("screen");
    mn.add("tv");
    mn.add("tty");
    mn.add("all");
  }

  public SilentUserAgentContextImpl(final NavigatorFrame frame) {
    this.frame = frame;
    if (frame == null) {
      throw new IllegalArgumentException("frame should not be null");
    }
  }

  public boolean isMedia(final String mediaName) {
    return mediaNames.contains(mediaName.toLowerCase());
  }

  public NetworkRequest createHttpRequest() {
    return this.frame.createNetworkRequest();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.HtmlParserContext#getAppCodeName()
   */
  public String getAppCodeName() {
    return "";
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.HtmlParserContext#getAppMinorVersion()
   */
  public String getAppMinorVersion() {
    return "";
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.HtmlParserContext#getAppName()
   */
  public String getAppName() {
    return "";
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.HtmlParserContext#getAppVersion()
   */
  public String getAppVersion() {
    return "";
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.HtmlParserContext#getBrowserLanguage()
   */
  public String getBrowserLanguage() {
    return "EN"; // TODO
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.HtmlParserContext#getPlatform()
   */
  public String getPlatform() {
    return "";
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.HtmlParserContext#getUserAgent()
   */
  public String getUserAgent() {
    return "";
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.HtmlParserContext#isCookieEnabled()
   */
  public boolean isCookieEnabled() {
    // TODO: Settings
    return true;
  }

  public String getCookie(final java.net.URL url) {
    // Requires privileges.
    System.out.println("Get cookie: " + url);
    return RequestEngine.getInstance().getCookie(url);
  }

  public boolean isScriptingEnabled() {
    // TODO: Settings
    return true;
  }

  public void setCookie(final java.net.URL url, final String cookieSpec) {
    // Requires privileges.
    RequestEngine.getInstance().setCookie(url, cookieSpec);
  }

  public Policy getSecurityPolicy() {
    return org.lobobrowser.security.LocalSecurityPolicy.getInstance();
  }

  public int getScriptingOptimizationLevel() {
    // TODO: Settings
    return -1;
  }

  public String getVendor() {
    return "";
  }

  public String getProduct() {
    return this.getAppName();
  }

  public boolean isExternalCSSEnabled() {
    return true;
  }

  public boolean isInternalCSSEnabled() {
    return true;
  }

  public boolean isRequestPermitted(final Request request) {
    return frame.isRequestPermitted(request);
  }
}
