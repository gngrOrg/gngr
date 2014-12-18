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
package org.lobobrowser.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.lobobrowser.clientlet.Clientlet;
import org.lobobrowser.clientlet.ClientletRequest;
import org.lobobrowser.clientlet.ClientletResponse;
import org.lobobrowser.clientlet.ClientletSelector;
import org.lobobrowser.ua.ConnectionProcessor;
import org.lobobrowser.ua.NavigationEvent;
import org.lobobrowser.ua.NavigationListener;
import org.lobobrowser.ua.NavigationVetoException;
import org.lobobrowser.ua.NavigatorErrorListener;
import org.lobobrowser.ua.NavigatorExceptionEvent;
import org.lobobrowser.ua.NavigatorExtension;
import org.lobobrowser.ua.NavigatorExtensionContext;
import org.lobobrowser.ua.NavigatorWindow;
import org.lobobrowser.ua.UserAgent;
import org.lobobrowser.util.ArrayUtilities;
import org.lobobrowser.util.EventDispatch2;

/**
 * Encapsulates a browser extension or plugin.
 */
public class Extension implements Comparable<Object>, NavigatorExtensionContext {
  private static final String ATTRIBUTE_EXTENSION_CLASS = "extension.class";
  private static final String ATTRIBUTE_EXTENSION_PRIORITY = "extension.priority";
  private static final String EXTENSION_PROPERTIES_FILE = "gngr-extension.properties";
  private static final String PRIMARY_EXTENSION_FILE_NAME = "primary.jar";

  /**
   * The minimum priority.
   */
  public static final int LOW_PRIORITY = 0;

  /**
   * The highest priority, only available to the default platform extension.
   */
  public static final int PRIMARY_EXTENSION_PRIORITY = 10;

  /**
   * The highest priority allowed for non-primary platform extensions.
   */
  public static final int HIGH_PRIORITY = 9;

  /**
   * The default priority.
   */
  public static final int NORMAL_PRIORITY = 5;

  private final int priority;
  private final File extRoot;
  private final String extClassName;
  private final String extId;
  private final boolean isPrimary;

  // TODO: Move these collections to ExtensionManager.
  // More efficient. Consider removal of extensions.

  private final Collection<ClientletSelector> clientletSelectors;
  private final Collection<ConnectionProcessor> connectionProcessors;
  private final Collection<NavigationListener> navigationListeners;
  private final EventDispatch2 EVENT = new NavigatorErrorEventDispatch();

  public static boolean isExtension(final File root) {
    if (root.isDirectory()) {
      final File propsFile = new File(root, EXTENSION_PROPERTIES_FILE);
      return propsFile.exists();
    } else {
      try (
          final JarFile jarFile = new JarFile(root)) {
        final JarEntry jarEntry = jarFile.getJarEntry(EXTENSION_PROPERTIES_FILE);
        return jarEntry != null;
      } catch (final IOException e) {
        return false;
      }
    }
  }

  public Extension(final Properties mattribs, final ClassLoader parentClassLoader) {
    this.extRoot = null;
    this.extClassName = mattribs.getProperty(ATTRIBUTE_EXTENSION_CLASS);
    this.extId = extClassName;
    this.clientletSelectors = new LinkedList<>();
    this.connectionProcessors = new ArrayList<>();
    this.navigationListeners = new ArrayList<>();
    this.priority = PRIMARY_EXTENSION_PRIORITY;
    this.isPrimary = true;
  }

  public Extension(final File extRoot) throws IOException {
    this.clientletSelectors = new LinkedList<>();
    this.connectionProcessors = new ArrayList<>();
    this.navigationListeners = new ArrayList<>();
    this.extRoot = extRoot;
    JarFile jarFile;
    InputStream propsInputStream;
    if (extRoot.isDirectory()) {
      this.isPrimary = false;
      jarFile = null;
      this.extId = extRoot.getName();
      final File propsFile = new File(extRoot, EXTENSION_PROPERTIES_FILE);
      propsInputStream = propsFile.exists() ? new FileInputStream(propsFile) : null;
    } else {
      jarFile = new JarFile(extRoot);
      this.isPrimary = extRoot.getName().toLowerCase().equals(PRIMARY_EXTENSION_FILE_NAME);
      final String name = extRoot.getName();
      final int dotIdx = name.lastIndexOf('.');
      this.extId = dotIdx == -1 ? name : name.substring(0, dotIdx);
      final JarEntry jarEntry = jarFile.getJarEntry(EXTENSION_PROPERTIES_FILE);
      propsInputStream = jarEntry == null ? null : jarFile.getInputStream(jarEntry);
    }
    final boolean isLibrary = propsInputStream == null;
    if (!isLibrary) {
      final Properties mattribs = new Properties();
      try {
        mattribs.load(propsInputStream);
      } finally {
        propsInputStream.close();
      }
      final String extClassName = mattribs.getProperty(ATTRIBUTE_EXTENSION_CLASS);
      if (extClassName == null) {
        throw new IOException("Property " + ATTRIBUTE_EXTENSION_CLASS + " missing in " + EXTENSION_PROPERTIES_FILE + ", part of " + extRoot
            + ".");
      }
      this.extClassName = extClassName;
      final String priorityText = mattribs.getProperty(ATTRIBUTE_EXTENSION_PRIORITY);
      if (priorityText != null) {
        final int tp = Integer.parseInt(priorityText.trim());
        this.priority = Math.min(HIGH_PRIORITY, Math.max(LOW_PRIORITY, tp));
      } else {
        this.priority = NORMAL_PRIORITY;
      }
    } else {
      this.extClassName = null;
      this.priority = PRIMARY_EXTENSION_PRIORITY;
    }

    if (jarFile != null) {
      jarFile.close();
    }
  }

  public String getId() {
    return this.extId;
  }

  public URL getCodeSource() throws java.net.MalformedURLException {
    return this.extRoot.toURI().toURL();
  }

  public boolean isPrimaryExtension() {
    return this.isPrimary;
  }

  private ClassLoader classLoader;
  private NavigatorExtension platformExtension;

  public void initClassLoader(final ClassLoader parentClassLoader) throws java.net.MalformedURLException, ClassNotFoundException,
  IllegalAccessException, InstantiationException {
    ClassLoader classLoader;
    if (extRoot != null) {
      final URL url = this.extRoot.toURI().toURL();
      final URL[] urls = new URL[] { url };
      classLoader = new ExtensionClassLoader(urls, parentClassLoader);
    } else {
      classLoader = parentClassLoader;
    }
    NavigatorExtension pe = null;
    if (extClassName != null) {
      final Class<?> extClass = classLoader.loadClass(extClassName);
      pe = (NavigatorExtension) extClass.newInstance();
    }
    synchronized (this) {
      this.classLoader = classLoader;
      this.platformExtension = pe;
    }
  }

  public ClassLoader getClassLoader() {
    synchronized (this) {
      return this.classLoader;
    }
  }

  /**
   * Gets the {@link org.lobobrowser.ua.NavigatorExtension} implementation. It
   * may return <code>null</code> in the case of a library.
   */
  public NavigatorExtension getNavigatorExtension() {
    synchronized (this) {
      return this.platformExtension;
    }
  }

  public void initExtension() {
    doWithClassLoader(() -> {
      final NavigatorExtension pe = this.platformExtension;
      if (pe != null) {
        pe.init(this);
      }
      return null;
    });
  }

  public void initExtensionWindow(final NavigatorWindow wcontext) {
    doWithClassLoader(() -> {
      final NavigatorExtension pe = this.platformExtension;
      if (pe != null) {
        pe.windowOpening(wcontext);
      }
      return null;
    });
  }

  public void shutdownExtensionWindow(final NavigatorWindow wcontext) {
    doWithClassLoader(() -> {
      final NavigatorExtension pe = this.platformExtension;
      if (pe != null) {
        pe.windowClosing(wcontext);
      }
      return null;
    });
  }

  public void close() throws java.io.IOException {
  }

  public void addClientletSelector(final ClientletSelector cs) {
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(org.lobobrowser.security.GenericLocalPermission.EXT_GENERIC);
    }
    synchronized (this) {
      this.clientletSelectors.add(cs);
    }
  }

  protected <V> V doWithClassLoader(final Callable<V> r) {
    // Need to set the class loader in thread context, otherwise
    // some library classes may not be found.
    final Thread currentThread = Thread.currentThread();
    final ClassLoader prevClassLoader = currentThread.getContextClassLoader();
    final ClassLoader loader = this.classLoader;
    if (loader != null) {
      currentThread.setContextClassLoader(loader);
    }
    try {
      return r.call();
    } catch (final Exception e) {
      throw new Error(e);
    } finally {
      currentThread.setContextClassLoader(prevClassLoader);
    }
  }

  public Clientlet getClientlet(final ClientletRequest request, final ClientletResponse response) {
    return doWithClassLoader(() -> {
      synchronized (this) {
        for (final ClientletSelector cs : this.clientletSelectors) {
          final Clientlet c = cs.select(request, response);
          if (c != null) {
            return c;
          }
        }
      }
      return null;
    });
  }

  public Clientlet getLastResortClientlet(final ClientletRequest request, final ClientletResponse response) {
    return doWithClassLoader(() -> {
      synchronized (this) {
        for (final ClientletSelector cs : this.clientletSelectors) {
          final Clientlet c = cs.lastResortSelect(request, response);
          if (c != null) {
            return c;
          }
        }
      }
      return null;
    });
  }

  public void addNavigatorErrorListener(final NavigatorErrorListener listener) {
    EVENT.addListener(listener);
  }

  public void removeNavigatorErrorListener(final NavigatorErrorListener listener) {
    EVENT.removeListener(listener);
  }

  /**
   * @param event
   * @return True only if the event was dispatched to at least one listener.
   */
  public boolean handleError(final NavigatorExceptionEvent event) {
    // Expected in GUI thread.
    return EVENT.fireEvent(event);
  }

  public void addURLStreamHandlerFactory(final URLStreamHandlerFactory factory) {
    // TODO: Since extensions are intialized in parallel,
    // this is not necessarily done in order of priority.
    org.lobobrowser.main.PlatformStreamHandlerFactory.getInstance().addFactory(factory);
  }

  public UserAgent getUserAgent() {
    return org.lobobrowser.request.UserAgentImpl.getInstance();
  }

  public int compareTo(final Object o) {
    // Reverse order based on priority.
    final Extension other = (Extension) o;
    final int diff = other.priority - this.priority;
    if (diff != 0) {
      return diff;
    }
    if (extRoot != null) {
      return this.extRoot.compareTo(other.extRoot);
    } else {
      return -1;
    }
  }

  @Override
  public int hashCode() {
    return this.priority | this.extRoot.hashCode();
  }

  @Override
  public boolean equals(final Object other) {
    if (!(other instanceof Extension)) {
      return false;
    }
    return ((Extension) other).extRoot.equals(this.extRoot);
  }

  @Override
  public String toString() {
    return "ExtensionInfo[extRoot=" + this.extRoot + "]";
  }

  public void addConnectionProcessor(final ConnectionProcessor processor) {
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(org.lobobrowser.security.GenericLocalPermission.EXT_GENERIC);
    }
    synchronized (this) {
      this.connectionProcessors.add(processor);
    }
  }

  public void addNavigationListener(final NavigationListener listener) {
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(org.lobobrowser.security.GenericLocalPermission.EXT_GENERIC);
    }
    synchronized (this) {
      this.navigationListeners.add(listener);
    }
  }

  public void removeClientletSelector(final ClientletSelector selector) {
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(org.lobobrowser.security.GenericLocalPermission.EXT_GENERIC);
    }
    synchronized (this) {
      this.clientletSelectors.remove(selector);
    }
  }

  public void removeConnectionProcessor(final ConnectionProcessor processor) {
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(org.lobobrowser.security.GenericLocalPermission.EXT_GENERIC);
    }
    synchronized (this) {
      this.connectionProcessors.remove(processor);
    }
  }

  public void removeNavigationListener(final NavigationListener listener) {
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(org.lobobrowser.security.GenericLocalPermission.EXT_GENERIC);
    }
    synchronized (this) {
      this.navigationListeners.remove(listener);
    }
  }

  void dispatchBeforeNavigate(final NavigationEvent event) throws NavigationVetoException {
    // Should not be public
    doWithClassLoader(() -> {
      final NavigationListener[] listeners = ArrayUtilities.copySynched(navigationListeners, this, NavigationListener.EMPTY_ARRAY);
      for (final NavigationListener l : listeners) {
        l.beforeNavigate(event);
      }
      return null;
    });
  }

  void dispatchBeforeLocalNavigate(final NavigationEvent event) throws NavigationVetoException {
    // Should not be public
    doWithClassLoader(() -> {
      final NavigationListener[] listeners = ArrayUtilities.copySynched(navigationListeners, this, NavigationListener.EMPTY_ARRAY);
      for (final NavigationListener l : listeners) {
        l.beforeLocalNavigate(event);
      }
      return null;
    });
  }

  void dispatchBeforeWindowOpen(final NavigationEvent event) throws NavigationVetoException {
    // Should not be public
    doWithClassLoader(() -> {
      final NavigationListener[] listeners = ArrayUtilities.copySynched(navigationListeners, this, NavigationListener.EMPTY_ARRAY);
      for (final NavigationListener l : listeners) {
        l.beforeWindowOpen(event);
      }
      return null;
    });
  }

  URLConnection dispatchPreConnection(final URLConnection connection) {
    // Should not be public
    return doWithClassLoader(() -> {
      final ConnectionProcessor[] processors = ArrayUtilities.copySynched(connectionProcessors, this, ConnectionProcessor.EMPTY_ARRAY);
      URLConnection result = connection;
      for (final ConnectionProcessor processor : processors) {
        result = processor.processPreConnection(connection);
      }
      return result;
    });
  }

  URLConnection dispatchPostConnection(final URLConnection connection) {
    // Should not be public
    return doWithClassLoader(() -> {
      final ConnectionProcessor[] processors = ArrayUtilities.copySynched(connectionProcessors, this, ConnectionProcessor.EMPTY_ARRAY);
      URLConnection result = connection;
      for (final ConnectionProcessor processor : processors) {
        result = processor.processPostConnection(connection);
      }
      return result;
    });
  }

  private static class NavigatorErrorEventDispatch extends EventDispatch2 {
    @Override
    protected void dispatchEvent(final EventListener listener, final EventObject event) {
      ((NavigatorErrorListener) listener).errorOcurred((NavigatorExceptionEvent) event);
    }
  }
}
