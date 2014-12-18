package org.lobobrowser.request;

import java.security.Policy;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lobobrowser.ua.NavigatorFrame;
import org.lobobrowser.ua.NetworkRequest;
import org.lobobrowser.ua.UserAgentContext;

public class UserAgentContextImpl implements UserAgentContext {
  private static final Logger logger = Logger.getLogger(UserAgentContextImpl.class.getName());
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

  public UserAgentContextImpl(final NavigatorFrame frame) {
    this.frame = frame;
  }

  public boolean isMedia(final String mediaName) {
    return mediaNames.contains(mediaName.toLowerCase());
  }

  public void warn(final String message, final Throwable throwable) {
    logger.log(Level.WARNING, message, throwable);
  }

  public void error(final String message, final Throwable throwable) {
    logger.log(Level.SEVERE, message, throwable);
  }

  public void warn(final String message) {
    logger.warning(message);
  }

  public void error(final String message) {
    logger.log(Level.SEVERE, message);
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
    return UserAgentImpl.getInstance().getName();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.HtmlParserContext#getAppMinorVersion()
   */
  public String getAppMinorVersion() {
    return UserAgentImpl.getInstance().getMinorVersion();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.HtmlParserContext#getAppName()
   */
  public String getAppName() {
    return UserAgentImpl.getInstance().getName();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.HtmlParserContext#getAppVersion()
   */
  public String getAppVersion() {
    return UserAgentImpl.getInstance().getMajorVersion();
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
    return System.getProperty("os.name");
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.HtmlParserContext#getUserAgent()
   */
  public String getUserAgent() {
    return UserAgentImpl.getInstance().getUserAgentString();
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
    return "The Lobo Project";
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
    return false;
  }
}
