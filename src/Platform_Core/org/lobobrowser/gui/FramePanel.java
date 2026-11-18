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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Window;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.clientlet.ClientletRequest;
import org.lobobrowser.clientlet.ClientletResponse;
import org.lobobrowser.clientlet.ComponentContent;
import org.lobobrowser.main.ExtensionManager;
import org.lobobrowser.main.PlatformInit;
import org.lobobrowser.request.ClientletRequestHandler;
import org.lobobrowser.request.ClientletRequestImpl;
import org.lobobrowser.request.DomainValidation;
import org.lobobrowser.request.RequestEngine;
import org.lobobrowser.request.RequestHandler;
import org.lobobrowser.request.SilentUserAgentContextImpl;
import org.lobobrowser.security.GenericLocalPermission;
import org.lobobrowser.security.RequestManager;
import org.lobobrowser.ua.NavigationEntry;
import org.lobobrowser.ua.NavigationEvent;
import org.lobobrowser.ua.NavigationListener;
import org.lobobrowser.ua.NavigationVetoException;
import org.lobobrowser.ua.NavigatorFrame;
import org.lobobrowser.ua.NavigatorProgressEvent;
import org.lobobrowser.ua.NetworkRequest;
import org.lobobrowser.ua.ParameterInfo;
import org.lobobrowser.ua.RequestType;
import org.lobobrowser.ua.TargetType;
import org.lobobrowser.ua.UserAgentContext;
import org.lobobrowser.ua.UserAgentContext.Request;
import org.lobobrowser.util.ArrayUtilities;
import org.lobobrowser.util.Items;
import org.lobobrowser.util.SecurityUtil;
import org.lobobrowser.util.WrapperException;
import org.lobobrowser.util.gui.WrapperLayout;

/**
 * A browser frame panel. It may be used as any other Swing component. The
 * {@link #navigate(String)} method may be invoked to load content into the
 * browser frame.
 * <p>
 * Content types supported depend on available browser extensions.
 * <p>
 * It's recommended that <code>FramePanel</code>s be placed in windows that
 * extend {@link AbstractBrowserWindow} or implement {@link BrowserWindow}. Such
 * windows may receive navigation notifications via {@link WindowCallback}.
 * <p>
 * A frame panel with navigation controls and a status bar can be obtained with
 * {@link BrowserPanel}.
 *
 * @see PlatformInit#init(boolean, boolean)
 */
public class FramePanel extends JPanel implements NavigatorFrame {
  private static final long serialVersionUID = -8873769110035409639L;
  private static final Logger logger = Logger.getLogger(FramePanel.class.getName());
  private final String windowId;
  private final NavigationEngine navigationEngine = new NavigationEngine();
  private final FramePanel knownParentFrame;
  private final Collection<NavigationListener> navigationListeners = new ArrayList<>();
  private final Collection<ResponseListener> responseListeners = new ArrayList<>();
  private final Collection<ContentListener> contentListeners = new ArrayList<>();
  private final Object propertiesMonitor = new Object();

  private NavigatorFrame openerFrame;
  private Window topFrameWindow;

  /**
   * Constructs a FramePanel specifying a "window" ID.
   */
  public FramePanel(final String windowId) {
    this.knownParentFrame = null;
    this.windowId = windowId;
    this.setLayout(WrapperLayout.getInstance());
    this.setBackground(Color.WHITE);
    this.setOpaque(true);
  }

  /**
   * Constructs a FramePanel specifying a non-null parent frame. This
   * constructor is useful when navigation in the new frame must occur before
   * the frame is added to the GUI component hierarchy.
   */
  public FramePanel(final FramePanel parentFrame) {
    this.knownParentFrame = parentFrame;
    this.windowId = null;
    this.setLayout(WrapperLayout.getInstance());
    this.setBackground(Color.WHITE);
    this.setOpaque(true);
  }

  /**
   * Constructs a standalone <code>FramePanel</code> that can be added to any
   * Swing window or component. Note that the FramePanel should be part of a
   * Swing or AWT window before it becomes functional.
   */
  public FramePanel() {
    this((FramePanel) null);
  }

  public void setOpenerFrame(final NavigatorFrame opener) {
    this.openerFrame = opener;
  }

  /**
   * Causes an event to be fired. This method is for internal use.
   *
   * @param response
   *          A clientlet response.
   */
  public void informResponseProcessed(final ClientletResponse response) {
    this.dispatchResponseProcessed(new ResponseEvent(this, response));
  }

  /**
   * Adds a listener of navigation events.
   *
   * @param listener
   *          The listener.
   */
  public void addNavigationListener(final NavigationListener listener) {
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(org.lobobrowser.security.GenericLocalPermission.EXT_GENERIC);
    }
    synchronized (this) {
      this.navigationListeners.add(listener);
    }
  }

  /**
   * Removes a listener of navigation events previously added with
   * {@link #addNavigationListener(NavigationListener)}.
   *
   * @param listener
   *          The listener.
   */
  public void removeNavigationListener(final NavigationListener listener) {
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(org.lobobrowser.security.GenericLocalPermission.EXT_GENERIC);
    }
    synchronized (this) {
      this.navigationListeners.remove(listener);
    }
  }

  /**
   * Adds a listener of content events.
   *
   * @param listener
   *          The listener.
   * @see #getComponentContent()
   */
  public void addContentListener(final ContentListener listener) {
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(org.lobobrowser.security.GenericLocalPermission.EXT_GENERIC);
    }
    synchronized (this) {
      this.contentListeners.add(listener);
    }
  }

  /**
   * Removes a listener of content events previously added with
   * {@link #addNavigationListener(NavigationListener)}.
   *
   * @param listener
   *          The listener.
   */
  public void removeContentListener(final ContentListener listener) {
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(org.lobobrowser.security.GenericLocalPermission.EXT_GENERIC);
    }
    synchronized (this) {
      this.contentListeners.remove(listener);
    }
  }

  /**
   * Adds a listener of response events.
   *
   * @param listener
   *          The listener.
   */
  public void addResponseListener(final ResponseListener listener) {
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(org.lobobrowser.security.GenericLocalPermission.EXT_GENERIC);
    }
    synchronized (this) {
      this.responseListeners.add(listener);
    }
  }

  /**
   * Removes a listener of navigation events previously added with
   * {@link #addNavigationListener(NavigationListener)}.
   *
   * @param listener
   *          The listener.
   */
  public void removeResponseListener(final ResponseListener listener) {
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(org.lobobrowser.security.GenericLocalPermission.EXT_GENERIC);
    }
    synchronized (this) {
      this.responseListeners.remove(listener);
    }
  }

  private void dispatchBeforeNavigate(final NavigationEvent event) throws NavigationVetoException {
    try {
      AccessController.doPrivileged(new PrivilegedAction<Object>() {
        // Reason: Dispatching an event to extensions requires permission to,
        // among other things, setting the context class loader.
        public Object run() {
          try {
            ExtensionManager.getInstance().dispatchBeforeNavigate(event);
          } catch (final NavigationVetoException nve) {
            throw new WrapperException(nve);
          }
          ArrayUtilities.forEachSynched(navigationListeners, this, (listener) -> {
            try {
              listener.beforeNavigate(event);
            } catch (final NavigationVetoException nve) {
              throw new WrapperException(nve);
            }
          });
          return null;
        }
      });
    } catch (final WrapperException we) {
      throw (NavigationVetoException) we.getCause();
    }
  }

  private void dispatchBeforeLocalNavigate(final NavigationEvent event) throws NavigationVetoException {
    try {
      AccessController.doPrivileged(new PrivilegedAction<Object>() {
        // Reason: Dispatching an event to extensions requires permission to,
        // among other things, setting the context class loader.
        public Object run() {
          try {
            ExtensionManager.getInstance().dispatchBeforeLocalNavigate(event);
          } catch (final NavigationVetoException nve) {
            throw new WrapperException(nve);
          }
          ArrayUtilities.forEachSynched(navigationListeners, this, (listener) -> {
            try {
              listener.beforeLocalNavigate(event);
            } catch (final NavigationVetoException nve) {
              throw new WrapperException(nve);
            }
          });
          return null;
        }
      });
    } catch (final WrapperException we) {
      throw (NavigationVetoException) we.getCause();
    }
  }

  private void dispatchBeforeWindowOpen(final NavigationEvent event) throws NavigationVetoException {
    try {
      AccessController.doPrivileged(new PrivilegedAction<Object>() {
        // Reason: Dispatching an event to extensions requires permission to,
        // among other things, setting the context class loader.
        public Object run() {
          try {
            ExtensionManager.getInstance().dispatchBeforeWindowOpen(event);
          } catch (final NavigationVetoException nve) {
            throw new WrapperException(nve);
          }
          ArrayUtilities.forEachSynched(navigationListeners, this, (listener) -> {
            try {
              listener.beforeWindowOpen(event);
            } catch (final NavigationVetoException nve) {
              throw new WrapperException(nve);
            }
          });
          return null;
        }
      });
    } catch (final WrapperException we) {
      throw (NavigationVetoException) we.getCause();
    }
  }

  private void dispatchContentSet(final ContentEvent event) {
    ArrayUtilities.forEachSynched(contentListeners, this, (listener) -> {
      listener.contentSet(event);
    });
  }

  private void dispatchResponseProcessed(final ResponseEvent event) {
    ArrayUtilities.forEachSynched(responseListeners, this, (listener) -> {
      listener.responseProcessed(event);
    });
  }

  /**
   * Gets a {@link WindowCallback} instance that is used to dispatch information
   * during local navigation. The FramePanel tries to find an implementor of the
   * interface among its ancestor components. Unless overridden, this
   * implementation of <code>getWindowCallback</code> only looks for instances
   * of the {@link BrowserWindow} interface.
   */
  protected WindowCallback getWindowCallback() {
    final FramePanel kpf = this.knownParentFrame;
    if (kpf != null) {
      return kpf.getWindowCallback();
    }
    Container parent = this.getParent();
    while ((parent != null) && !(parent instanceof BrowserWindow)) {
      parent = parent.getParent();
    }
    if (parent == null) {
      return null;
    }
    return ((BrowserWindow) parent).getWindowCallback();
  }

  /**
   * Gets the parent frame. This is <code>null<code> for the top-most frame
   * in a window or when the FramePanel is detached.
   */
  public NavigatorFrame getParentFrame() {
    // TODO: Security?
    final NavigatorFrame kpf = this.knownParentFrame;
    if (kpf != null) {
      return kpf;
    }
    Container parent = this.getParent();
    while ((parent != null) && !(parent instanceof NavigatorFrame)) {
      parent = parent.getParent();
    }
    return (NavigatorFrame) parent;
  }

  /**
   * Gets the top-most frame in a window. It may return the current frame if its
   * parent is <code>null</code>.
   */
  public NavigatorFrame getTopFrame() {
    NavigatorFrame current = this;
    for (;;) {
      final NavigatorFrame ancestor = current.getParentFrame();
      if (ancestor == null) {
        return current;
      }
      current = ancestor;
    }
  }

  /**
   * Implements {@link NavigatorFrame#getComponent()}.
   */
  public Component getComponent() {
    return this;
  }

  @Override
  public void paint(final Graphics g) {
    // Unless done this way, duplicate
    // painting occurs for nested frames.
    this.paintComponent(g);
    this.paintBorder(g);
    this.paintChildren(g);
  }

  /**
   * Gets an array of navigation entries that came before the current one.
   */
  public List<NavigationEntry> getBackNavigationEntries() {
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(org.lobobrowser.security.GenericLocalPermission.EXT_GENERIC);
    }
    synchronized (this) {
      return this.navigationEngine.getBackNavigationEntries();
    }
  }

  /**
   * Gets an array of navigation entries that would be visited with consecutive
   * <code>forward</code> calls.
   */
  public List<NavigationEntry> getForwardNavigationEntries() {
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(org.lobobrowser.security.GenericLocalPermission.EXT_GENERIC);
    }
    synchronized (this) {
      return this.navigationEngine.getForwardNavigationEntries();
    }
  }

  /**
   * Determines if the current navigation entry has associated source code.
   */
  public boolean hasSource() {
    // TODO: Security?
    final ComponentContent content = this.content;
    return (content != null) && (content.getSourceCode() != null);
  }

  /**
   * Determines whether there's a selection with content to be copied in this
   * frame.
   */
  public boolean canCopy() {
    // TODO: Security?
    final ComponentContent content = this.content;
    return content == null ? false : content.canCopy();
  }

  /**
   * Copies the selection, if any, to the clipboard. Whether this method is
   * supported depends on content being rendered.
   */
  public boolean copy() {
    // TODO: Security?
    final ComponentContent content = this.content;
    return content == null ? false : content.copy();
  }

  /*
  public final void replaceContent(final Component component) {
    // TODO: Security?
    this.replaceContent(null, new SimpleComponentContent(component));
  }
  */

  /**
   * Replaces the content of the frame. This method can be safely called outside
   * the GUI dispatch thread.
   */
  public void replaceContent(final ClientletResponse response, final ComponentContent content) {
    // Method probably invoked outside GUI thread.
    if (SwingUtilities.isEventDispatchThread()) {
      this.replaceContentImpl(response, content);
    } else {
      // Security note: Need to pass security context of caller
      // into invokeLater task.
      final AccessControlContext context = AccessController.getContext();
      SwingUtilities.invokeLater(() -> {
        final PrivilegedAction<Object> action = () -> {
          FramePanel.this.replaceContentImpl(response, content);
          return null;
        };
        AccessController.doPrivileged(action, context);
      });
    }
  }

  @Override
  protected void addImpl(final Component comp, final Object constraints, final int index) {
    // Check security. Content downloaded off the web should
    // not be allowed to replace frame content at will.
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(GenericLocalPermission.EXT_GENERIC);
    }
    super.addImpl(comp, constraints, index);
  }

  @Override
  public void remove(final Component comp) {
    // Check security. Content downloaded off the web should
    // not be allowed to replace frame content at will.
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(GenericLocalPermission.EXT_GENERIC);
    }
    super.remove(comp);
  }

  @Override
  public void remove(final int index) {
    // Check security. Content downloaded off the web should
    // not be allowed to replace frame content at will.
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(GenericLocalPermission.EXT_GENERIC);
    }
    super.remove(index);
  }

  @Override
  public void removeAll() {
    // Check security. Content downloaded off the web should
    // not be allowed to replace frame content at will.
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(GenericLocalPermission.EXT_GENERIC);
    }
    super.removeAll();
  }

  protected void replaceContentImpl(final ClientletResponse response, final ComponentContent content) {
    // Security note: Currently expected to be private.
    // Always called in GUI thread.
    // removeAll and add will invalidate.
    final ComponentContent oldContent = this.content;
    this.removeAll();
    if (oldContent != null) {
      oldContent.removeNotify();
    }
    if (content != null) {
      final Component component = content.getComponent();
      if (component == null) {
        throw new java.lang.IllegalStateException("Component from " + content + " is null: " + response.getResponseURL() + ".");
      }
      this.add(component);
    }
    // Call to validate will lay out children.
    this.validate();
    this.repaint();

    // Set this at the end, after removal and addition of components has
    // succeded.
    this.content = content;
    if (content != null) {
      content.addNotify();
      this.updateContentProperties(content);
    }

    if (response != null) {
      final String title = content == null ? null : content.getTitle();
      final String description = content == null ? null : content.getDescription();
      final NavigationEntry navigationEntry = NavigationEntry.fromResponse(this, response, title, description);
      if (response.isNewNavigationAction()) {
        synchronized (this) {
          this.navigationEngine.addNavigationEntry(navigationEntry);
        }
      }

      if (content != null) {
        if (PlatformInit.getInstance().debugOn) {
          System.out.println("Navigation over: " + response.getResponseURL());
        }
        content.navigatedNotify();
      }

      final WindowCallback wc = this.getWindowCallback();
      if (wc != null) {
        // It's important that the handleDocumentRendering method be called
        // right after navigationEngine is updated.
        // TODO: Why?
        wc.handleDocumentRendering(this, response, content);
      }
    } else {
      // Notify so that lazy layouting algorithm can know that layouting is not blocked
      if (content != null) {
        content.navigatedNotify();
      }
    }
    this.dispatchContentSet(new ContentEvent(this, content, response));
  }

  /**
   * Clears current content. This method should be invoked in the GUI thread.
   */
  public void clear() {
    this.removeAll();
    this.content = null;
  }

  private Window getWindow() {
    // TODO: Security? Getting parent security?
    final FramePanel kpf = this.knownParentFrame;
    if (kpf != null) {
      return kpf.getWindow();
    }
    Container parent = this.getParent();
    if (parent instanceof FramePanel) {
      return ((FramePanel) parent).getWindow();
    }
    while ((parent != null) && !(parent instanceof Window)) {
      parent = parent.getParent();
    }
    return (Window) parent;
  }

  /**
   * Closes the window this frame belongs to.
   */
  public void closeWindow() {
    // TODO: Security?
    final Window window = this.getWindow();
    if (window != null) {
      window.dispose();
    }
  }

  /**
   * Opens a confirmation dialog.
   */
  public boolean confirm(final String message) {
    return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
      // Reason: We don't want an "Applet Window" message and
      // it's no big deal to allow it here.
      public Boolean run() {
        return JOptionPane.showConfirmDialog(FramePanel.this, message) == JOptionPane.YES_OPTION;
      }
    });
  }

  /**
   * Implements {@link NavigatorFrame#invokeLater(Runnable)}.
   */
  public void invokeLater(final Runnable runnable) {
    SwingUtilities.invokeLater(runnable);
  }

  public final void navigate(final String urlOrPath) throws java.net.MalformedURLException {
    final URL url = DomainValidation.guessURL(urlOrPath);
    this.navigate(url, "GET", null, TargetType.SELF, RequestType.PROGRAMMATIC);
  }

  public final void navigate(final String urlOrPath, final RequestType requestType) throws java.net.MalformedURLException {
    final URL url = DomainValidation.guessURL(urlOrPath);
    this.navigate(url, "GET", null, TargetType.SELF, requestType);
  }

  public void navigate(final @NonNull URL url, final String method, final ParameterInfo paramInfo, final TargetType type,
      final RequestType requestType) {
    this.navigate(url, method, paramInfo, type, requestType, this);
  }

  public void navigate(final @NonNull URL url, final String method, final ParameterInfo paramInfo, final TargetType type,
      final RequestType requestType, final NavigatorFrame originatingFrame) {
    final NavigationEvent event = new NavigationEvent(this, url, method, paramInfo, type, requestType, originatingFrame);
    this.navigate(event);
  }

  private void navigate(final NavigationEvent event) {
    try {
      this.dispatchBeforeNavigate(event);
    } catch (final NavigationVetoException nve) {
      if (logger.isLoggable(Level.INFO)) {
        logger.info("navigateLocal(): Navigation was vetoed: " + nve.getMessage());
      }
      return;
    }
    final TargetType type = event.getTargetType();
    final URL url = event.getURL();
    final String method = event.getMethod();
    final ParameterInfo paramInfo = event.getParamInfo();
    final RequestType requestType = event.getRequestType();
    switch (type) {
    case PARENT:
      final NavigatorFrame parent = this.getParentFrame();
      if (parent != null) {
        parent.navigate(url, method, paramInfo, TargetType.SELF, requestType, this);
      } else {
        this.navigateLocal(event);
      }
      break;
    case TOP:
      final NavigatorFrame top = this.getTopFrame();
      if (top == this) {
        this.navigateLocal(event);
      } else {
        top.navigate(url, method, paramInfo, TargetType.SELF, requestType, this);
      }
      break;
    case SELF:
      this.navigateLocal(event);
      break;
    case BLANK:
      this.open(url, method, paramInfo);
      break;
    }
  }

  private void navigateToHistoryEntry(final @NonNull URL url) {
    this.navigateLocal(url, "GET", RequestType.HISTORY, this);
  }

  protected boolean isOKToAddReferrer(final RequestType requestType) {
    return (requestType == RequestType.CLICK) || (requestType == RequestType.PROGRAMMATIC)
        || (requestType == RequestType.PROGRAMMATIC_FROM_CLICK) || (requestType == RequestType.OPEN_WINDOW)
        || (requestType == RequestType.OPEN_WINDOW_FROM_CLICK) || (requestType == RequestType.FORM);
  }

  private void navigateLocal(final @NonNull URL url, final String method, final RequestType requestType, final FramePanel originatingFrame) {
    final NavigationEvent event = new NavigationEvent(this, url, method, requestType, originatingFrame);
    this.navigateLocal(event);
  }

  private void navigateLocal(final NavigationEvent event) {
	// request Manager reset!!!
    requestManager.reset(event.getURL());
    try {
      this.dispatchBeforeLocalNavigate(event);
    } catch (final NavigationVetoException nve) {
      if (logger.isLoggable(Level.INFO)) {
        logger.info("navigateLocal(): Navigation was vetoed: " + nve.getMessage());
      }
      return;
    }
    String referrer = null;
    final RequestType requestType = event.getRequestType();
    final URL url = event.getURL();
    final String method = event.getMethod();
    final ParameterInfo paramInfo = event.getParamInfo();
    if (this.isOKToAddReferrer(requestType)) {
      // TODO: When child frame does a _top navigate, referrer
      // should apparently be from child.
      final NavigationEntry entry = this.getCurrentNavigationEntry();
      if (entry != null) {
        referrer = entry.getUrl().toExternalForm();
      }
    }
    final ClientletRequest request = new ClientletRequestImpl(false, url, method, paramInfo, null, referrer, null, requestType);
    final UserAgentContext uaContext = new SilentUserAgentContextImpl(this);
    final RequestHandler handler = new ClientletRequestHandler(request, this.getWindowCallback(), this, uaContext);
    SecurityUtil.doPrivileged(() -> {
      // Justification: While requests by untrusted code are generally only
      // allowed on certain hosts, navigation is an exception.
        RequestEngine.getInstance().scheduleRequest(handler);
        return null;
      });
  }

  /**
   * Opens a window and renders the URL or path provided.
   *
   * @return The top frame of the new window.
   */
  public final NavigatorFrame open(final String urlOrPath) throws MalformedURLException {
    final URL url = DomainValidation.guessURL(urlOrPath);
    return this.open(url, (Properties) null);
  }

  /**
   * Opens a window and renders the URL provided.
   *
   * @param url
   *          The URL of the document.
   * @param windowProperties
   *          A Properties object that follows JavaScript Window.open()
   *          conventions.
   * @return The top frame of the new window.
   */
  public final NavigatorFrame open(final @NonNull URL url, final Properties windowProperties) {
    return this.open(url, null, windowProperties);
  }

  /**
   * Opens a window and renders the URL provided.
   *
   * @param url
   *          The URL of the document.
   * @param windowId
   *          A unique ID for the window.
   * @param windowProperties
   *          A Properties object that follows JavaScript Window.open()
   *          conventions.
   * @return The top frame of the new window.
   */
  public final NavigatorFrame open(final @NonNull URL url, final String windowId, final Properties windowProperties) {
    return this.open(url, "GET", null, windowId, windowProperties);
  }

  /**
   * Opens a window and renders the URL provided.
   *
   * @param url
   *          The URL of the document.
   * @return The top frame of the new window.
   */
  public final NavigatorFrame open(final @NonNull URL url) {
    return this.open(url, (Properties) null);
  }

  /**
   * Opens a window and renders the URL provided. This method is called to
   * request that a new window is opened. For example, this method will be
   * invoked on JavaScript Window.open() calls. Override to be informed of such
   * calls.
   *
   * @return The top frame of the new window. The method may return
   *         <code>null</code> if navigation was vetoed by a listener.
   */
  public NavigatorFrame open(final @NonNull URL url, final String method, final ParameterInfo pinfo, final String windowId,
      final Properties windowProperties) {
    final NavigationEvent event = new NavigationEvent(this, url, method, pinfo, TargetType.BLANK, RequestType.OPEN_WINDOW, this);
    try {
      this.dispatchBeforeWindowOpen(event);
    } catch (final NavigationVetoException nve) {
      if (logger.isLoggable(Level.INFO)) {
        logger.info("navigateLocal(): Navigation was vetoed: " + nve.getMessage());
      }
      return null;
    }
    return FramePanel.openWindow(this, url, windowId, windowProperties, method, pinfo);
  }

  /**
   * Opens a window and renders the URL provided.
   *
   * @param url
   *          The URL of the document.
   * @param method
   *          The request method.
   * @param pinfo
   *          Any additional parameter data.
   * @return The top frame of the new window.
   */
  public final NavigatorFrame open(final @NonNull URL url, final String method, final ParameterInfo pinfo) {
    return this.open(url, method, pinfo, null, null);
  }

  /**
   * Static method for opening a window.
   *
   * @return The top frame in the window that was opened.
   */
  public static NavigatorFrame openWindow(final FramePanel opener, final @NonNull URL url, final String windowId, final Properties windowProperties,
      final String method, final ParameterInfo pinfo) {
    final ClientletRequest request = new ClientletRequestImpl(true, url, method, pinfo, RequestType.OPEN_WINDOW);
    final NavigatorWindowImpl wcontext = AccessController.doPrivileged(new PrivilegedAction<NavigatorWindowImpl>() {
      // Reason: Window creation can require special permissions at various
      // levels, e.g. ExtensionManager access and os.version check in Swing.
      public NavigatorWindowImpl run() {
        return new NavigatorWindowImpl(opener, windowId, windowProperties);
      }
    });

    final FramePanel newFrame = wcontext.getFramePanel();
    // ATTENTION!!! Frame Panel request manager reset!
    newFrame.requestManager.reset(url);

    final UserAgentContext uaContext = new SilentUserAgentContextImpl(newFrame);
    final ClientletRequestHandler handler = new ClientletRequestHandler(request, wcontext, newFrame, uaContext);
    SwingUtilities.invokeLater(() -> wcontext.resetAsNavigator(handler.getContextWindowProperties()));

    SecurityUtil.doPrivileged(() -> {
      // Justification: While requests by untrusted code are generally only allowed on certain hosts,
      // navigation is an exception.
        RequestEngine.getInstance().scheduleRequest(handler);
        return null;
      });

    return newFrame;
  }

  /**
   * Opens a message prompt dialog.
   */
  public String prompt(final String message, final String inputDefault) {
    return AccessController.doPrivileged(new PrivilegedAction<String>() {
      // Reason: We don't want an "Applet Window" message and
      // it's no big deal to allow it here.
      public String run() {
        return JOptionPane.showInputDialog(FramePanel.this, message, inputDefault);
      }
    });
  }

  /**
   * Sends the window to the back (blur).
   */
  public void windowToBack() {
    final Window window = this.getWindow();
    if (window != null) {
      window.toBack();
    }
  }

  /**
   * Sends the window to the front and grabs focus for the frame.
   */
  public void windowToFront() {
    final Window window = this.getWindow();
    if (window != null) {
      window.toFront();
    }
    this.grabFocus();
  }

  /**
   * Opens an alert dialog.
   */
  public void alert(final String message) {
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
      // Reason: We don't want an "Applet Window" message and
      // it's no big deal to allow it here.
      public Object run() {
        JOptionPane.showMessageDialog(FramePanel.this, message);
        return null;
      }
    });
  }

  /**
   * Navigates to the given entry without adding the entry to frame history.
   * This is the mechanism that should be used to "go back" to an entry already
   * visited.
   */
  public boolean goTo(final NavigationEntry entry) {
    if (!"GET".equals(entry.getMethod())) {
      throw new IllegalArgumentException("Method only accepts entries with GET method.");
    }
    this.navigateToHistoryEntry(entry.getUrl());
    synchronized (this) {
      return this.navigationEngine.moveTo(entry);
    }
  }

  /**
   * Navigates back.
   */
  public boolean back() {
    return this.moveNavigation(-1);
  }

  /**
   * Navigates forward.
   */
  public boolean forward() {
    return this.moveNavigation(+1);
  }

  private boolean moveNavigation(final int offset) {
    if ((offset == 0) || (offset > 1) || (offset < -1)) {
      throw new IllegalArgumentException("offset: only +1 or -1 are allowed");
    }
    synchronized (this) {
      NavigationEntry newEntry;
      for (;;) {
        newEntry = this.navigationEngine.move(offset);
        if (newEntry == null) {
          return false;
        }
        if (!"GET".equals(newEntry.getMethod())) {
          // back() and forward() only supported for GET.
          continue;
        }
        break;
      }
      this.navigateToHistoryEntry(newEntry.getUrl());
      return true;
    }
  }

  /**
   * Determines whether the frame can navigate forward.
   */
  public boolean canForward() {
    synchronized (this) {
      return this.navigationEngine.hasNextWithGET();
    }
  }

  /**
   * Determines whether the frame can navigate back.
   */
  public boolean canBack() {
    synchronized (this) {
      return this.navigationEngine.hasPrevWithGET();
    }
  }

  /**
   * Reloads the current document.
   */
  public void reload() {
    NavigationEntry entry;
    synchronized (this) {
      entry = this.navigationEngine.getCurrentEntry();
    }
    if (entry != null) {
      final String method = entry.getMethod();
      if (!"GET".equals(method)) {
        final String lineBreak = System.getProperty("line.separator");
        this.alert("Reloading a document not obtained with the GET " + lineBreak + "method is disallowed for security reasons." + lineBreak
            + "The request method of the current page is " + method + ".");

      } else {
        this.navigateLocal(entry.getUrl(), entry.getMethod(), RequestType.SOFT_RELOAD, this);
      }
    }
  }

  /**
   * Determines whether the current document can be reloaded.
   */
  public boolean canReload() {
    NavigationEntry entry;
    synchronized (this) {
      entry = this.navigationEngine.getCurrentEntry();
    }
    // Check for request method or not?
    return entry != null;
  }

  /**
   * Creates a frame that is expected to be used as a child of the current one.
   */
  public NavigatorFrame createFrame() {
    return FramePanelFactorySource.getInstance().getActiveFactory().createFramePanel(this);
  }

  /**
   * Gets the default window status.
   */
  public String getDefaultStatus() {
    final WindowCallback wc = this.getWindowCallback();
    if (wc != null) {
      return wc.getDefaultStatus();
    } else {
      return null;
    }
  }

  public Object getItem(final String name) {
    return Items.getItem(this, name);
  }

  /**
   * Gets the frame that opened the current frame, if any.
   */
  public NavigatorFrame getOpenerFrame() {
    return this.openerFrame;
  }

  /**
   * Gets the current window status.
   */
  public String getStatus() {
    final WindowCallback wc = this.getWindowCallback();
    if (wc != null) {
      return wc.getStatus();
    } else {
      return null;
    }
  }

  /**
   * Gets the window ID if this is the top frame in a window.
   */
  public String getWindowId() {
    return this.windowId;
  }

  /**
   * Determines if the window is closed.
   */
  public boolean isWindowClosed() {
    final Window window = this.getWindow();
    if (window != null) {
      return !window.isDisplayable();
    }
    return true;
  }

  /**
   * Sets the default window status.
   */
  public void setDefaultStatus(final String value) {
    final WindowCallback wc = this.getWindowCallback();
    if (wc != null) {
      wc.setDefaultStatus(this, value);
    }
  }

  public void setItem(final String name, final Object value) {
    Items.setItem(this, name, value);
  }

  /**
   * Sets the window status.
   */
  public void setStatus(final String status) {
    final WindowCallback wc = this.getWindowCallback();
    if (wc != null) {
      wc.setStatus(this, status);
    }
  }

  @Override
  public Dimension getPreferredSize() {
    if (this.isPreferredSizeSet()) {
      return super.getPreferredSize();
    } else {
      return new Dimension(600, 400);
    }
  }

  @Override
  public Dimension getMinimumSize() {
    return new Dimension(1, 1);
  }

  @Override
  public Dimension getMaximumSize() {
    return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
  }

  private ComponentContent content;

  /**
   * Gets the component content currently set in the frame.
   */
  public ComponentContent getComponentContent() {
    // TODO: Security?
    return this.content;
  }

  public String getSourceCode() {
    final ComponentContent content = this.content;
    return content == null ? null : content.getSourceCode();
  }

  public final void navigate(final @NonNull URL url) {
    this.navigate(url, RequestType.PROGRAMMATIC);
  }

  public final void navigate(final @NonNull URL url, final RequestType requestType) {
    this.navigate(url, "GET", null, TargetType.SELF, requestType);
  }

  /**
   * Gets the current navigation entry.
   */
  public NavigationEntry getCurrentNavigationEntry() {
    synchronized (this) {
      return this.navigationEngine.getCurrentEntry();
    }
  }

  public NavigatorProgressEvent getProgressEvent() {
    return this.progressEvent;
  }

  private NavigatorProgressEvent progressEvent;

  public void setProgressEvent(final NavigatorProgressEvent event) {
    this.progressEvent = event;
    if (event != null) {
      final WindowCallback wc = this.getWindowCallback();
      if (wc != null) {
        wc.updateProgress(event);
      }
    }
  }

  public NetworkRequest createNetworkRequest() {
    final UserAgentContext uaContext = new SilentUserAgentContextImpl(this);
    return new org.lobobrowser.context.NetworkRequestImpl(uaContext);
  }

  @Override
  public String toString() {
    return "FramePanel[windowId=" + windowId + ",hashCode=" + this.hashCode() + ",parent=" + this.getParent() + "]";
  }

  public void resizeWindowBy(final int byWidth, final int byHeight) {
    final Window window = this.getWindow();
    if (logger.isLoggable(Level.INFO)) {
      logger.info("resizeWindowBy(): byWidth=" + byWidth + ",byHeight=" + byHeight + "; window=" + window);
    }
    if (window != null) {
      window.setSize(window.getWidth() + byWidth, window.getHeight() + byHeight);
    }
  }

  public void resizeWindowTo(final int width, final int height) {
    final Window window = this.getWindow();
    if (logger.isLoggable(Level.INFO)) {
      logger.info("resizeWindowTo(): width=" + width + ",height=" + height + "; window=" + window);
    }
    if (window != null) {
      window.setSize(width, height);
    }
  }

  public Window getTopFrameWindow() {
    return topFrameWindow;
  }

  public void setTopFrameWindow(final Window topFrameWindow) {
    this.topFrameWindow = topFrameWindow;
  }

  /**
   * Gets an object that is used to represent the current frame content. For
   * example, if the frame is currently showing HTML, this method will probably
   * return an instance of <code>org.w3c.dom.html2.HTMLDocument</code>.
   */
  public Object getContentObject() {
    final ComponentContent content = this.getComponentContent();
    return content == null ? null : content.getContentObject();
  }

  /**
   * Gets a mime type that goes with the object returned by
   * {@link FramePanel#getContentObject()}. This is not necessarily the same as
   * the mime type declared in the headers of the response that produced the
   * current content.
   */
  public String getCurrentMimeType() {
    final ComponentContent content = this.getComponentContent();
    return content == null ? null : content.getMimeType();
  }

  public void linkClicked(final @NonNull URL url, final TargetType targetType, final Object linkObject) {
    final NavigationEvent event = new NavigationEvent(this, url, targetType, RequestType.CLICK, linkObject, this);
    this.navigate(event);
  }

  public int getHistoryLength() {
    synchronized (this) {
      return this.navigationEngine.getLength();
    }
  }

  public Optional<NavigationEntry> getNextNavigationEntry() {
    synchronized (this) {
      final List<NavigationEntry> entries = this.navigationEngine.getForwardNavigationEntries();
      return entries.stream().findFirst();
    }
  }

  public Optional<NavigationEntry> getPreviousNavigationEntry() {
    synchronized (this) {
      final List<NavigationEntry> entries = this.navigationEngine.getBackNavigationEntries();
      return entries.stream().findFirst();
    }
  }

  public void moveInHistory(final int offset) {
    this.moveNavigation(offset);
  }

  public void navigateInHistory(final String absoluteURL) {
    NavigationEntry entry;
    synchronized (this) {
      entry = this.navigationEngine.findEntry(absoluteURL);
    }
    if (entry != null) {
      this.navigateToHistoryEntry(entry.getUrl());
    }
  }

  private Map<String, Object> contentProperties = null;

  public void setProperty(final String name, final Object value) {
    final ComponentContent content = this.getComponentContent();
    synchronized (this.propertiesMonitor) {
      if (content != null) {
        content.setProperty(name, value);
      }
      Map<String, Object> props = this.contentProperties;
      if (props == null) {
        props = new HashMap<>(5);
        this.contentProperties = props;
      }
      props.put(name, value);
    }
  }

  private void updateContentProperties(final ComponentContent content) {
    synchronized (this.propertiesMonitor) {
      final Map<String, Object> props = this.contentProperties;
      if (props != null) {
        final Iterator<Entry<String, Object>> i = props.entrySet().iterator();
        while (i.hasNext()) {
          final Entry<String, Object> entry = i.next();
          content.setProperty(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  // New request manager.
  private final RequestManager requestManager = new RequestManager(this);

  public boolean isRequestPermitted(final Request request) {
    return requestManager.isRequestPermitted(request);
  }

  public void manageRequests(final Object initiator) {
    requestManager.manageRequests((JComponent) initiator);
  }

  public void allowAllFirstPartyRequests() {
    requestManager.allowAllFirstPartyRequests();
  }
}
