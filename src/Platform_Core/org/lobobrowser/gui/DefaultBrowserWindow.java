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
package org.lobobrowser.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.RenderingHints;

import javax.swing.BoxLayout;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.lobobrowser.main.ExtensionManager;
import org.lobobrowser.ua.NavigatorFrame;

/**
 * The default window class used by the browser.
 *
 * @see WindowFactory
 */
public class DefaultBrowserWindow extends AbstractBrowserWindow {
  private static final long serialVersionUID = -2359502974718245509L;
  private final FramePanel framePanel;
  private final NavigatorWindowImpl windowContext;

  public DefaultBrowserWindow(final boolean hasMenuBar, final boolean hasAddressBar, final boolean hasToolBar, final boolean hasStatusBar,
      final NavigatorWindowImpl windowContext) throws HeadlessException {

    // Maximise window
    setExtendedState(getExtendedState() | Frame.MAXIMIZED_BOTH);

    // TODO: SECURITY: Security needed in this class to prevent removal of all
    // window components??
    this.windowContext = windowContext;
    this.framePanel = windowContext.getFramePanel();
    ExtensionManager.getInstance().initExtensionsWindow(windowContext);
    final Object componentLock = windowContext.getComponentLock();
    if (hasMenuBar) {
      final JMenuBar menuBar = new JMenuBar();
      this.setJMenuBar(menuBar);
      synchronized (componentLock) {
        for (final JMenu menu : windowContext.getMenus()) {
          menuBar.add(menu);
        }
        // Collection<JMenuItem> sharedMenuItems =
        // windowContext.getSharedMenuItems();
        // if(sharedMenuItems.size() > 0) {
        // JMenu extensionsMenu = new JMenu("Extensions");
        // menuBar.add(extensionsMenu);
        // for(JMenuItem menuItem : sharedMenuItems) {
        // extensionsMenu.add(menuItem);
        // }
        // }
      }
    }
    final Container contentPane = this.getContentPane();
    contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
    if (hasAddressBar) {
      final AddressBarPanel abp = new AddressBarPanel();
      contentPane.add(abp);
      synchronized (componentLock) {
        for (final Component c : windowContext.getAddressBarComponents()) {
          abp.add(c);
        }
      }
    }
    if (hasToolBar) {
      synchronized (componentLock) {
        for (final Component c : windowContext.getToolBars()) {
          contentPane.add(c);
        }
      }
      final SharedToolBarPanel stbp = new SharedToolBarPanel();
      contentPane.add(stbp);
      synchronized (componentLock) {
        for (final Component c : windowContext.getSharedToolbarComponents()) {
          stbp.add(c);
        }
      }
    }
    contentPane.add(new FillerComponent(this.framePanel, false));
    if (hasStatusBar) {
      final StatusBarPanel statusBar = new StatusBarPanel();
      contentPane.add(statusBar);
      synchronized (componentLock) {
        for (final Component c : windowContext.getStatusBarComponents()) {
          statusBar.add(c);
        }
      }
    }
  }

  public NavigatorFrame getTopFrame() {
    return this.framePanel;
  }

  @Override
  public FramePanel getTopFramePanel() {
    return this.framePanel;
  }

  @Override
  public WindowCallback getWindowCallback() {
    return this.windowContext;
  }

  // private Image lastDbImage;
  // private Dimension lastDbSize;
  //
  // private Image getDoubleBufferingImage(Dimension size) {
  // Dimension lastDbSize = this.lastDbSize;
  // Image newImage = this.lastDbImage;
  // if(lastDbSize == null || lastDbSize.width != size.width ||
  // lastDbSize.height != size.height) {
  // newImage = this.createImage(size.width, size.height);
  // this.lastDbImage = newImage;
  // this.lastDbSize = size;
  // }
  // return newImage;
  // }

  @Override
  public void update(final Graphics g) {
    if (g instanceof Graphics2D) {
      final Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
    super.update(g);
    // Rectangle clipBounds = g.getClipBounds();
    // Dimension size = this.getSize();
    // Image dbi = this.getDoubleBufferingImage(size);
    // this.paint(dbi.getGraphics());
    // g.drawImage(dbi, 0, 0, size.width, size.height, clipBounds.x,
    // clipBounds.y, clipBounds.width, clipBounds.height, this);
  }

  @Override
  public void setSize(final Dimension d) {
    unmaximise();
    super.setSize(d);
  }

  @Override
  public void setSize(final int width, final int height) {
    unmaximise();
    super.setSize(width, height);
  }

  private void unmaximise() {
    setExtendedState(getExtendedState() & (~Frame.MAXIMIZED_BOTH));
  }
}
