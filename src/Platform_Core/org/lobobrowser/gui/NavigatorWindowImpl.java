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
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.JMenu;
import javax.swing.SwingUtilities;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.clientlet.ClientletResponse;
import org.lobobrowser.clientlet.ComponentContent;
import org.lobobrowser.main.ExtensionManager;
import org.lobobrowser.ua.NavigationEntry;
import org.lobobrowser.ua.NavigatorEvent;
import org.lobobrowser.ua.NavigatorEventType;
import org.lobobrowser.ua.NavigatorFrame;
import org.lobobrowser.ua.NavigatorProgressEvent;
import org.lobobrowser.ua.NavigatorWindow;
import org.lobobrowser.ua.NavigatorWindowEvent;
import org.lobobrowser.ua.NavigatorWindowListener;
import org.lobobrowser.ua.ParameterInfo;
import org.lobobrowser.ua.RequestType;
import org.lobobrowser.ua.TargetType;
import org.lobobrowser.ua.UserAgent;
import org.lobobrowser.util.EventDispatch2;
import org.lobobrowser.util.Urls;

/**
 * Default implementation of the {@link NavigatorWindow} interface.
 */
public class NavigatorWindowImpl implements NavigatorWindow, WindowCallback {
  private static final Logger logger = Logger.getLogger(NavigatorWindowImpl.class.getName());
  private static final int HGAP = 4;
  private static final int VGAP = 2;

  private final FramePanel framePanel;
  private final Properties requestedProperties;
  private final String windowId;
  private final AbstractBrowserWindow browserWindow;
  private final Map<String, JMenu> menusById = new HashMap<>();
  private final Collection<JMenu> menus = new LinkedList<>();
  // private final Collection<JMenuItem> sharedMenuItems = new
  // LinkedList<JMenuItem>();
  private final Collection<Component> addressBarComponents = new LinkedList<>();
  private final Collection<Component> sharedToolbarComponents = new LinkedList<>();
  private final Collection<Component> statusBarComponents = new LinkedList<>();
  private final Collection<Component> toolBars = new LinkedList<>();

  private volatile boolean launched = false;
  // private volatile boolean disposingProgressWindow = false;

  private static volatile WindowFactory windowFactory = DefaultWindowFactory.getInstance();

  /**
   * Changes the {@link WindowFactory} that is used to create browser windows.
   */
  public static void setWindowFactory(final WindowFactory wf) {
    windowFactory = wf;
  }

  /**
   * Constructs a PlatformWindowContextImpl. It starts out by showing a progress
   * window. Later a new browser window is obtained given the windowId, or
   * created.
   */
  public NavigatorWindowImpl(final NavigatorFrame openerFrame, final String windowId, final Properties properties) {
    this.requestedProperties = properties;
    this.windowId = windowId;
    final WindowFactory wf = windowFactory;
    if (wf == null) {
      throw new IllegalStateException("Global WindowFactory is null.");
    }
    final AbstractBrowserWindow window = wf.getExistingWindow(windowId);
    FramePanel framePanel = null;
    if (window != null) {
      framePanel = window.getTopFramePanel();
      if (framePanel == null) {
        throw new IllegalStateException("Window with ID " + windowId + " exists but its top frame is null.");
      }
    } else {
      framePanel = FramePanelFactorySource.getInstance().getActiveFactory().createFramePanel(windowId);
      framePanel.setOpenerFrame(openerFrame);
    }
    this.framePanel = framePanel;
    // Starts out as progress window.
    // We allow documents to override window properties, but
    // it can also be the case that such methods as alert() are
    // invoked while the document loads.
    if (window != null) {
      this.browserWindow = window;
      this.launched = true;
    } else {
      final AbstractBrowserWindow newWindow = wf.createWindow(this.windowId, properties, this);
      this.browserWindow = newWindow;
    }
  }

  public boolean isClosed() {
    return !this.browserWindow.isDisplayable();
  }

  public FramePanel getFramePanel() {
    return this.framePanel;
  }

  private void showWindow() {
    final AbstractBrowserWindow window = this.browserWindow;
    if (!window.isVisible()) {
      window.setVisible(true);
    }
    window.toFront();
  }

  void resetAsNavigator(final Properties overridingProperties) {
    // Invoke in GUI thread
    if (this.launched) {
      return;
    }
    this.launched = true;

    final AbstractBrowserWindow window = this.browserWindow;
    // Come up with combination properties object
    if (overridingProperties != null) {
      Properties original = this.requestedProperties;
      if (original == null) {
        original = new Properties();
      }
      original.putAll(overridingProperties);
      final WindowFactory wf = windowFactory;
      if (wf == null) {
        throw new IllegalStateException("Global WindowFactory is null.");
      }
      wf.overrideProperties(window, original);
    }

    // Initialize title
    final NavigationEntry currentEntry = this.getCurrentNavigationEntry();
    if (currentEntry != null) {
      String title = currentEntry.getTitle();
      if (title == null) {
        title = Urls.getNoRefForm(currentEntry.getUrl());
      }
      window.setTitle(title);
    }

    showWindow();
  }

  public void close() {
    final Window window = this.browserWindow;
    if (window != null) {
      window.dispose();
    }
  }

  /**
   * @param windowFeatures
   *          Window features formatted as in the window.open() method of
   *          Javascript.
   */
  public static NavigatorWindowImpl createFromWindowFeatures(final NavigatorFrame openerFrame, final String windowId,
      final String windowFeatures) {
    // Transform into properties file format.
    return new NavigatorWindowImpl(openerFrame, windowId, getPropertiesFromWindowFeatures(windowFeatures));
  }

  public static Properties getPropertiesFromWindowFeatures(final String windowFeatures) {
    final String lineBreak = System.getProperty("line.separator");
    final StringBuffer buffer = new StringBuffer();
    final StringTokenizer tok = new StringTokenizer(windowFeatures, ",");
    while (tok.hasMoreTokens()) {
      final String token = tok.nextToken();
      buffer.append(token);
      buffer.append(lineBreak);
    }
    final Properties props = new Properties();
    final byte[] bytes = buffer.toString().getBytes();
    final ByteArrayInputStream in = new ByteArrayInputStream(bytes);
    try {
      props.load(in);
    } catch (final IOException ioe) {
      // impossible
      logger.log(Level.SEVERE, "unexpected", ioe);
    }
    return props;
  }

  public void navigate(final String urlOrPath) throws java.net.MalformedURLException {
    this.framePanel.navigate(urlOrPath);
  }

  public void navigate(final @NonNull URL url, final String method, final ParameterInfo paramInfo) {
    this.framePanel.navigate(url, method, paramInfo, TargetType.SELF, RequestType.PROGRAMMATIC);
  }

  public void handleError(final NavigatorFrame frame, final ClientletResponse response, final Throwable exception,
      final RequestType requestType) {
    ExtensionManager.getInstance().handleError(frame, response, exception, requestType);
    // Also inform as if document rendering.
    this.handleDocumentRendering(frame, response, null);
  }

  private volatile NavigatorFrame latestAccessedFrame = null;

  public void handleDocumentAccess(final NavigatorFrame frame, final ClientletResponse response) {
    final NavigatorWindowEvent event = new NavigatorWindowEvent(this, NavigatorEventType.DOCUMENT_ACCESSED, frame, response,
        response.getRequestType());
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        EVENT.fireEvent(event);
      }
    });
  }

  public void handleDocumentAccess(final NavigatorFrame frame, final ClientletResponse response, final boolean okToAddToNavigationList) {
    this.handleDocumentAccess(frame, response);
  }

  public boolean canCopy() {
    return this.framePanel.canCopy();
  }

  public boolean canReload() {
    return this.framePanel.canReload();
  }

  public boolean copy() {
    return this.framePanel.copy();
  }

  public UserAgent getUserAgent() {
    return org.lobobrowser.request.UserAgentImpl.getInstance();
  }

  public void dispose() {
    this.browserWindow.dispose();
  }

  public boolean reload() {
    this.framePanel.reload();
    return true;
  }

  public boolean stop() {
    org.lobobrowser.request.RequestEngine.getInstance().cancelAllRequests();
    return true;
  }

  public void handleDocumentRendering(final NavigatorFrame frame, final ClientletResponse response, final ComponentContent content) {
    if (SwingUtilities.isEventDispatchThread()) {
      this.handleDocumentRenderingImpl(frame, response, content);
    } else {
      SwingUtilities.invokeLater(() -> NavigatorWindowImpl.this.handleDocumentRenderingImpl(frame, response, content));
    }
  }

  private static String getWindowTitle(final ClientletResponse response, final ComponentContent content) {
    String title = content == null ? null : content.getTitle();
    if (title == null) {
      title = response == null ? "No response" : Urls.getNoRefForm(response.getResponseURL());
    }
    return title;
  }

  private void handleDocumentRenderingImpl(final NavigatorFrame frame, final ClientletResponse response, final ComponentContent content) {
    if (frame == this.framePanel) {
      final String title = getWindowTitle(response, content);
      final Object window = this.browserWindow;
      if (window instanceof Frame) {
        ((Frame) window).setTitle(title);
      }
    }
    final RequestType requestType = response == null ? null : response.getRequestType();
    final NavigatorWindowEvent event = new NavigatorWindowEvent(this, NavigatorEventType.DOCUMENT_RENDERING, frame, response, requestType);
    latestAccessedFrame = event.getNavigatorFrame();
    if (!EVENT.fireEvent(event)) {
      logger.warning("handleDocumentRendering(): Did not deliver event to any window: " + event);
    }
  }

  public void updateProgress(final NavigatorProgressEvent event) {
    SwingUtilities.invokeLater(() -> EVENT.fireEvent(event));
  }

  public boolean back() {
    final NavigatorFrame frame = this.latestAccessedFrame;
    if (frame != null) {
      if (frame.back()) {
        return true;
      }
      if (frame == this.framePanel) {
        return false;
      }
    }
    return this.framePanel.back();
  }

  public boolean forward() {
    final NavigatorFrame frame = this.latestAccessedFrame;
    if (frame != null) {
      if (frame.forward()) {
        return true;
      }
      if (frame == this.framePanel) {
        return false;
      }
    }
    return this.framePanel.forward();
  }

  public boolean canForward() {
    final NavigatorFrame frame = this.latestAccessedFrame;
    if (frame != null) {
      if (frame.canForward()) {
        return true;
      }
      if (frame == this.framePanel) {
        return false;
      }
    }
    return this.framePanel.canForward();
  }

  public boolean canBack() {
    final NavigatorFrame frame = this.latestAccessedFrame;
    if (frame != null) {
      if (frame.canBack()) {
        return true;
      }
      if (frame == this.framePanel) {
        return false;
      }
    }
    return this.framePanel.canBack();
  }

  public void toFront() {
    this.browserWindow.toFront();
  }

  public void toBack() {
    this.browserWindow.toBack();
  }

  public NavigatorFrame getTopFrame() {
    return this.framePanel;
  }

  public void statusUpdated(final NavigatorFrame clientletFrame, final String value) {
    final NavigatorWindowEvent event = new NavigatorWindowEvent(NavigatorWindowImpl.this, NavigatorEventType.STATUS_UPDATED,
        clientletFrame, value, RequestType.NONE);
    SwingUtilities.invokeLater(() -> EVENT.fireEvent(event));
  }

  public void defaultStatusUpdated(final NavigatorFrame clientletFrame, final String value) {
    final NavigatorWindowEvent event = new NavigatorWindowEvent(NavigatorWindowImpl.this, NavigatorEventType.STATUS_UPDATED,
        clientletFrame, value, RequestType.NONE);
    SwingUtilities.invokeLater(() -> EVENT.fireEvent(event));
  }

  private String status;
  private String defaultStatus;

  public void setStatus(final NavigatorFrame frame, final String value) {
    String actualStatus;
    synchronized (this) {
      if (!java.util.Objects.equals(this.status, value)) {
        this.status = value;
        actualStatus = value == null ? this.defaultStatus : value;
        final NavigatorWindowEvent event = new NavigatorWindowEvent(this, NavigatorEventType.STATUS_UPDATED, frame, actualStatus,
            RequestType.NONE);
        SwingUtilities.invokeLater(() -> EVENT.fireEvent(event));
      }
    }
  }

  public void setDefaultStatus(final NavigatorFrame frame, final String value) {
    synchronized (this) {
      this.defaultStatus = value;
      if (this.status == null) {
        final String actualStatus = this.defaultStatus;
        final NavigatorWindowEvent event = new NavigatorWindowEvent(this, NavigatorEventType.STATUS_UPDATED, frame, actualStatus,
            RequestType.NONE);
        SwingUtilities.invokeLater(() -> EVENT.fireEvent(event));
      }
    }
  }

  public String getStatus() {
    synchronized (this) {
      return this.status;
    }
  }

  public String getDefaultStatus() {
    synchronized (this) {
      return this.defaultStatus;
    }
  }

  public void addAddressBarComponent(final Component addressBar) {
    synchronized (this) {
      this.addressBarComponents.add(addressBar);
    }
  }

  public void addMenu(final String menuId, final JMenu menu) {
    final Map<String, JMenu> map = this.menusById;
    synchronized (this) {
      if (map.containsKey(menuId)) {
        throw new IllegalArgumentException("Menu " + menuId + " already exists.");
      }
      this.menusById.put(menuId, menu);
      this.menus.add(menu);
    }
  }

  public JMenu getMenu(final String menuId) {
    synchronized (this) {
      return this.menusById.get(menuId);
    }
  }

  public void addSharedToolBarComponent(final Component toolBarComponent) {
    synchronized (this) {
      this.sharedToolbarComponents.add(toolBarComponent);
    }
  }

  public void addStatusBarComponent(final Component statusBarComponent) {
    synchronized (this) {
      this.statusBarComponents.add(statusBarComponent);
    }
  }

  public void addToolBar(final Component toolBar) {
    synchronized (this) {
      this.toolBars.add(toolBar);
    }
  }

  // public void addItemToSharedMenu(JMenuItem menuItem) {
  // synchronized(this) {
  // this.sharedMenuItems.add(menuItem);
  // }
  // }

  private final EventDispatch2 EVENT = new LocalEventDispatch();

  public void addNavigatorWindowListener(final NavigatorWindowListener listener) {
    EVENT.addListener(listener);
  }

  public void removeNavigatorWindowListener(final NavigatorWindowListener listener) {
    EVENT.removeListener(listener);
  }

  public static class LocalEventDispatch extends EventDispatch2 {
    @Override
    protected void dispatchEvent(final EventListener listener, final EventObject event) {
      final NavigatorEvent ne = (NavigatorEvent) event;
      final NavigatorWindowListener nwl = (NavigatorWindowListener) listener;
      switch (ne.getEventType()) {
      case DOCUMENT_ACCESSED:
        nwl.documentAccessed((NavigatorWindowEvent) ne);
        break;
      case DOCUMENT_RENDERING:
        nwl.documentRendering((NavigatorWindowEvent) ne);
        break;
      case PROGRESS_UPDATED:
        nwl.progressUpdated((NavigatorProgressEvent) ne);
        break;
      case STATUS_UPDATED:
        nwl.statusUpdated((NavigatorWindowEvent) ne);
        break;
      case DEFAULT_STATUS_UPDATED:
        nwl.defaultStatusUpdated((NavigatorWindowEvent) ne);
        break;
      default:
        break;
      }
    }
  }

  public Collection<Component> getAddressBarComponents() {
    return addressBarComponents;
  }

  public Collection<JMenu> getMenus() {
    return this.menus;
  }

  // public Collection<JMenuItem> getSharedMenuItems() {
  // return sharedMenuItems;
  // }

  public Collection<Component> getSharedToolbarComponents() {
    return sharedToolbarComponents;
  }

  public Collection<Component> getStatusBarComponents() {
    return statusBarComponents;
  }

  public Collection<Component> getToolBars() {
    return toolBars;
  }

  public Object getComponentLock() {
    return this;
  }

  public Component createGlueComponent(final Component wrappedComponent, final boolean usingMaxSize) {
    return new FillerComponent(wrappedComponent, usingMaxSize);
  }

  public Component createGap() {
    return Box.createRigidArea(new Dimension(HGAP, VGAP));
  }

  public boolean goTo(final NavigationEntry entry) {
    return this.framePanel.goTo(entry);
  }

  public List<NavigationEntry> getBackNavigationEntries() {
    return this.framePanel.getBackNavigationEntries();
  }

  public List<NavigationEntry> getForwardNavigationEntries() {
    return this.framePanel.getForwardNavigationEntries();
  }

  public NavigationEntry getCurrentNavigationEntry() {
    return this.framePanel.getCurrentNavigationEntry();
  }

  public boolean hasSource() {
    return this.framePanel.hasSource();
  }

  public Window getAwtWindow() {
    return this.browserWindow;
  }
}
