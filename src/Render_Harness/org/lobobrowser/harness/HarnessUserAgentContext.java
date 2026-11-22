/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2026 gngr authors

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.
*/

package org.lobobrowser.harness;

import java.net.URL;
import java.security.Policy;
import java.util.Locale;

import org.lobobrowser.ua.NetworkRequest;
import org.lobobrowser.ua.UserAgentContext;

/**
 * Minimal UserAgentContext for the Render Harness.
 * Permits all network requests, enables external CSS and images, but disables JavaScript.
 */
public class HarnessUserAgentContext implements UserAgentContext {

  @Override
  public boolean isRequestPermitted(final Request request) {
    return true;
  }

  @Override
  public NetworkRequest createHttpRequest() {
    return new HarnessNetworkRequest();
  }

  @Override
  public String getAppCodeName() {
    return "gngr";
  }

  @Override
  public String getAppName() {
    return "gngr";
  }

  @Override
  public String getAppVersion() {
    return "1.0";
  }

  @Override
  public String getAppMinorVersion() {
    return "0";
  }

  @Override
  public String getBrowserLanguage() {
    return Locale.getDefault().getLanguage();
  }

  @Override
  public boolean isCookieEnabled() {
    return false;
  }

  @Override
  public boolean isScriptingEnabled() {
    return false;
  }

  @Override
  public boolean isExternalCSSEnabled() {
    return true;
  }

  @Override
  public boolean isInternalCSSEnabled() {
    return true;
  }

  @Override
  public String getPlatform() {
    return System.getProperty("os.name");
  }

  @Override
  public String getUserAgent() {
    return "Mozilla/5.0 (compatible; gngr/1.0; RenderHarness)";
  }

  @Override
  public String getCookie(final URL url) {
    return "";
  }

  @Override
  public void setCookie(final URL url, final String cookieSpec) {
  }

  @Override
  public Policy getSecurityPolicy() {
    return null;
  }

  @Override
  public int getScriptingOptimizationLevel() {
    return -1;
  }

  @Override
  public boolean isMedia(final String mediaName) {
    return "screen".equalsIgnoreCase(mediaName) || "all".equalsIgnoreCase(mediaName);
  }

  @Override
  public String getVendor() {
    return "gngr";
  }

  @Override
  public String getProduct() {
    return "gngr";
  }
}
