/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The XAMJ Project

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Contact info: lobochief@users.sourceforge.net
 */
package org.lobobrowser.html;

import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.ua.UserAgentContext;
import org.w3c.dom.html.HTMLCollection;
import org.w3c.dom.html.HTMLElement;
import org.w3c.dom.html.HTMLLinkElement;

/**
 * Abstract implementation of the {@link HtmlRendererContext} interface with
 * blank methods, provided for developer convenience.
 */
public abstract class AbstractHtmlRendererContext implements HtmlRendererContext {

  public void alert(final String message) {
  }

  public void back() {
  }

  public void blur() {
  }

  public void close() {
  }

  public boolean confirm(final String message) {
    return false;
  }

  public BrowserFrame createBrowserFrame() {
    return null;
  }

  public void focus() {
  }

  public String getDefaultStatus() {
    return null;
  }

  public HTMLCollection getFrames() {
    return null;
  }

  public HtmlObject getHtmlObject(final HTMLElement element) {
    return null;
  }

  public String getName() {
    return null;
  }

  public HtmlRendererContext getOpener() {
    return null;
  }

  public HtmlRendererContext getParent() {
    return null;
  }

  public String getStatus() {
    return null;
  }

  public HtmlRendererContext getTop() {
    return null;
  }

  public UserAgentContext getUserAgentContext() {
    return null;
  }

  /**
   * Returns false unless overridden.
   */
  public boolean isClosed() {
    return false;
  }

  /**
   * Returns true unless overridden.
   */
  public boolean isImageLoadingEnabled() {
    return true;
  }

  /**
   * Returns false unless overridden.
   */
  public boolean isVisitedLink(final HTMLLinkElement link) {
    return false;
  }

  /*
  public void linkClicked(final HTMLElement linkNode, final @NonNull URL url, final String target) {
  }

  public void navigate(final URL url, final String target) {
  }*/

  /**
   * Returns true unless overridden.
   */
  public boolean onContextMenu(final HTMLElement element, final MouseEvent event) {
    return true;
  }

  public void onMouseOut(final HTMLElement element, final MouseEvent event) {
  }

  public void onMouseOver(final HTMLElement element, final MouseEvent event) {
  }

  public HtmlRendererContext open(final String absoluteUrl, final String windowName, final String windowFeatures, final boolean replace) {
    return null;
  }

  public HtmlRendererContext open(final @NonNull URL url, final String windowName, final String windowFeatures, final boolean replace) {
    return null;
  }

  public String prompt(final String message, final String inputDefault) {
    return null;
  }

  public void reload() {
  }

  public void scroll(final int x, final int y) {
  }

  public void setDefaultStatus(final String value) {
  }

  public void setOpener(final HtmlRendererContext opener) {
  }

  public void setStatus(final String message) {
  }

  /*
  public void submitForm(final String method, final @NonNull URL action, final String target, final String enctype, final FormInput[] formInputs) {
  }
  */

  /**
   * Returns true unless overridden.
   */
  public boolean onDoubleClick(final HTMLElement element, final MouseEvent event) {
    return true;
  }

  /**
   * Returns true unless overridden.
   */
  public boolean onMouseClick(final HTMLElement element, final MouseEvent event) {
    return true;
  }

  public void scrollBy(final int x, final int y) {
  }

  public void resizeBy(final int byWidth, final int byHeight) {
  }

  public void resizeTo(final int width, final int height) {
  }

  public void forward() {
  }

  public String getCurrentURL() {
    return null;
  }

  public int getHistoryLength() {
    return 0;
  }

  public Optional<String> getNextURL() {
    return Optional.empty();
  }

  public Optional<String> getPreviousURL() {
    return null;
  }

  public void goToHistoryURL(final String url) {
  }

  public void moveInHistory(final int offset) {
  }
}
