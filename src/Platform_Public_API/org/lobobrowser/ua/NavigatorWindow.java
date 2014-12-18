/*
Copyright 1994-2006 The Lobo Project. All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer. Redistributions in binary form must
reproduce the above copyright notice, this list of conditions and the following
disclaimer in the documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE LOBO PROJECT ``AS IS'' AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
EVENT SHALL THE FREEBSD PROJECT OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.lobobrowser.ua;

import java.awt.Component;
import java.util.List;

/**
 * This interface represents a navigator window.
 */
public interface NavigatorWindow {
  /**
   * Adds a top-level menu to the window.
   *
   * @param menuId
   *          A globally unique ID for the menu.
   * @param menu
   *          A JMenu instance.
   * @see #getMenu(String)
   */
  public void addMenu(String menuId, javax.swing.JMenu menu);

  /**
   * Gets a menu previously added, typically by another extension with higher
   * priority.
   *
   * @param menuId
   *          The unique ID of the menu. The convention in Lobo is to use
   *          "lobo." followed by the name of the menu in lower case, with any
   *          spaces converted to dots. For example, the ID of the File menu
   *          should be "lobo.file". The ID of the Page Services menu should be
   *          "lobo.page.services".
   * @return A JMenu instance.
   */
  public javax.swing.JMenu getMenu(String menuId);

  /**
   * Adds a "tool bar" component to the window. The preferred height of the tool
   * bar is used, whereas its width will be set roughly to the width of the
   * window.
   *
   * @param toolBar
   *          A AWT or Swing lightweight.
   */
  public void addToolBar(java.awt.Component toolBar);

  /**
   * Adds a component to the shared tool bar. The preferred width of the
   * component is used, whereas its height will be set roughly to the height of
   * the shared tool bar.
   *
   * @param toolBarComponent
   * @see #createGlueComponent(Component, boolean)
   */
  public void addSharedToolBarComponent(java.awt.Component toolBarComponent);

  /**
   * Adds a component to the status bar. The preferred width of the component is
   * used, whereas its height will be set roughly to the height of the status
   * bar.
   *
   * @param statusBarComponent
   * @see #createGlueComponent(Component, boolean)
   */
  public void addStatusBarComponent(java.awt.Component statusBarComponent);

  /**
   * Adds a component to the address bar. The preferred width of the component
   * is used, whereas its height will be set roughly to the height of the status
   * bar.
   *
   * @param addressBarComponent
   * @see #createGlueComponent(Component, boolean)
   */
  public void addAddressBarComponent(java.awt.Component addressBarComponent);

  /**
   * Adds a listener of window events.
   *
   * @param listener
   *          A listener instance.
   */
  public void addNavigatorWindowListener(NavigatorWindowListener listener);

  /**
   * Removes a listener previously added with
   * {@link #addNavigatorWindowListener(NavigatorWindowListener)}
   *
   * @param listener
   */
  public void removeNavigatorWindowListener(NavigatorWindowListener listener);

  /**
   * Gets the top frame of this window.
   */
  public NavigatorFrame getTopFrame();

  /**
   * Creates a component wrapper that expands to fill its parent's available
   * space. It only works if the parent uses a Swing <code>BoxLayout</code>.
   * Examples of components that are wrapped this way are the address combo box
   * and the status message component.
   *
   * @param wrappedComponent
   *          The component that is wrapped by the glue box.
   * @param usingMaxSize
   *          Whether the adjacent components have a maximum size that the
   *          container should try to use. If this argument is
   *          <code>false</code>, it is assumed that the adjacent components can
   *          be shrunk to their minimum sizes.
   */
  public Component createGlueComponent(Component wrappedComponent, boolean usingMaxSize);

  /**
   * Creates a gap component that should be placed between toolbar, address bar
   * or status bar components.
   */
  public Component createGap();

  /**
   * Closes the window.
   */
  public void dispose();

  /**
   * Gets the navigator for the window.
   */
  public UserAgent getUserAgent();

  public boolean canBack();

  public boolean canForward();

  public boolean back();

  public boolean forward();

  public boolean canReload();

  public boolean reload();

  public boolean stop();

  public boolean canCopy();

  public boolean copy();

  public boolean hasSource();

  /**
   * Navigates to a {@link NavigationEntry} belonging to navigation history in
   * the current session. without generating a new entry, in much the same way
   * that {@link #back()} and {@link #forward()} work.
   *
   * @param entry
   *          A existing <code>NavigationEntry</code>.
   * @return True if the operation succeeded.
   */
  public boolean goTo(NavigationEntry entry);

  public List<NavigationEntry> getBackNavigationEntries();

  public List<NavigationEntry> getForwardNavigationEntries();

  public NavigationEntry getCurrentNavigationEntry();

  /**
   * Gets the <code>java.awt.Frame</code> instance associated with this
   * <code>NavigatorWindow</code>. In most cases this method will return an
   * instance of <code>javax.swing.JFrame</code>.
   */
  public java.awt.Window getAwtWindow();
}
