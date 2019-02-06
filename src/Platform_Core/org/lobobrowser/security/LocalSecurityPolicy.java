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
/*
 * Created on May 31, 2005
 */
package org.lobobrowser.security;

import java.awt.AWTPermission;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.NetPermission;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLPermission;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.SecurityPermission;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.PropertyPermission;
import java.util.StringTokenizer;

import javax.net.ssl.SSLPermission;

import org.h2.engine.Constants;
import org.lobobrowser.main.ExtensionManager;
import org.lobobrowser.main.PlatformInit;
import org.lobobrowser.request.DomainValidation;
import org.lobobrowser.store.StorageManager;
import org.lobobrowser.util.io.Files;

public class LocalSecurityPolicy extends Policy {
  private static final String JAVA_HOME = System.getProperty("java.home");
  private static final String JAVA_CLASS_PATH = System.getProperty("java.class.path");
  private static final String PATH_SEPARATOR = System.getProperty("path.separator");

  /**
   * Directory where gngr should save files. Any files saved here have
   * privileges of a remote file.
   */
  public static final File STORE_DIRECTORY;

  private static final String DEFAULT_PROFILE = "default";
  private static final String STORE_DIR_NAME = ".gngr";
  private static final String STORE_DIRECTORY_CANONICAL;
  private static final LocalSecurityPolicy instance = new LocalSecurityPolicy();
  private static final Collection<Permission> BASE_PRIVILEGE = new LinkedList<>();
  private static final String recursiveSuffix = File.separator + "-";

  private static final Collection<Permission> CORE_PERMISSIONS = new LinkedList<>();
  private static final Collection<Permission> CP_READ_PERMISSIONS = new LinkedList<>();
  private static final Collection<Permission> EXTENSION_PERMISSIONS = new LinkedList<>();

  private static String JAVA_HOME_URL;

  static {
    try {
      JAVA_HOME_URL = new File(JAVA_HOME).toURI().toURL().toExternalForm();
    } catch (MalformedURLException e) {
      throw new RuntimeException("Couldn't parse Java Home path: " + JAVA_HOME);
    }
    final File homeDir = new File(System.getProperty("user.home"));
    final File settingsDir = Files.joinPaths(homeDir, STORE_DIR_NAME, DEFAULT_PROFILE);
    STORE_DIRECTORY = settingsDir;
    String settingsCanonical = "";
    try {
      settingsCanonical = settingsDir.getCanonicalPath();
    } catch (final IOException ioe) {
      ioe.printStackTrace(System.err);
    }
    STORE_DIRECTORY_CANONICAL = settingsCanonical;

    final Collection<Permission> permissions = BASE_PRIVILEGE;

    // //Note: This means extensions have access to private field values at the moment.
    // //Required by JavaFX runtime at the time of this writing.
    // permissions.add(new java.lang.reflect.ReflectPermission("suppressAccessChecks"));

    permissions.add(new PropertyPermission("*", "read,write"));
    permissions.add(new AWTPermission("*"));
    permissions.add(new HistoryPermission());

    // We need to compute this early. But we can add them to core later
    addExtensionPermissions(EXTENSION_PERMISSIONS);
    initCorePermissions();

    // Note: execute needed to launch external browser.
    // permissions.add(new FilePermission("<<ALL FILES>>", "read,write,delete,execute"));

  }

  private static void initCorePermissions() {
    CORE_PERMISSIONS.add(new SocketPermission("*", "connect,resolve,listen,accept"));
    CORE_PERMISSIONS.add(new RuntimePermission("createClassLoader"));
    CORE_PERMISSIONS.add(new RuntimePermission("getClassLoader"));
    CORE_PERMISSIONS.add(new RuntimePermission("exitVM"));
    CORE_PERMISSIONS.add(new RuntimePermission("setIO"));
    CORE_PERMISSIONS.add(new RuntimePermission("setContextClassLoader"));
    CORE_PERMISSIONS.add(new RuntimePermission("enableContextClassLoaderOverride"));
    CORE_PERMISSIONS.add(new RuntimePermission("setFactory"));
    CORE_PERMISSIONS.add(new RuntimePermission("accessClassInPackage.*"));
    CORE_PERMISSIONS.add(new RuntimePermission("defineClassInPackage.*"));
    CORE_PERMISSIONS.add(new RuntimePermission("accessDeclaredMembers"));
    CORE_PERMISSIONS.add(new RuntimePermission("getStackTrace"));
    CORE_PERMISSIONS.add(new RuntimePermission("preferences"));
    CORE_PERMISSIONS.add(new RuntimePermission("modifyThreadGroup"));
    CORE_PERMISSIONS.add(new RuntimePermission("getProtectionDomain"));
    CORE_PERMISSIONS.add(new RuntimePermission("shutdownHooks"));
    CORE_PERMISSIONS.add(new RuntimePermission("modifyThread"));
    CORE_PERMISSIONS.add(new RuntimePermission("com.sun.media.jmc.accessMedia"));
    // loadLibrary necessary in Java 6, in particular loadLibrary.sunmscapi.
    CORE_PERMISSIONS.add(new RuntimePermission("loadLibrary.*"));
    CORE_PERMISSIONS.add(new NetPermission("setDefaultAuthenticator"));
    CORE_PERMISSIONS.add(new NetPermission("setCookieHandler"));
    CORE_PERMISSIONS.add(new NetPermission("specifyStreamHandler"));
    CORE_PERMISSIONS.add(new SSLPermission("setHostnameVerifier"));
    CORE_PERMISSIONS.add(new SSLPermission("getSSLSessionContext"));
    CORE_PERMISSIONS.add(new SecurityPermission("putProviderProperty.*"));
    CORE_PERMISSIONS.add(new SecurityPermission("insertProvider.*"));
    CORE_PERMISSIONS.add(new SecurityPermission("removeProvider.*"));
    CORE_PERMISSIONS.add(new java.util.logging.LoggingPermission("control", null));
    CORE_PERMISSIONS.add(GenericLocalPermission.EXT_GENERIC);

    // For stopping JS Scheduler
    CORE_PERMISSIONS.add(new RuntimePermission("stopThread"));

    copyPermissions(EXTENSION_PERMISSIONS, CORE_PERMISSIONS);
    addStoreDirectoryPermissions(CORE_PERMISSIONS);

    {
      /* Allows resources to be loaded from class path.
         This is only required while running in Eclipse (that is when the URL Class Loader loads the resources and checks
         for file access permission).
         This might be broader than currently required, but it is not very potent either.
         A future strategy might be to pick only resource paths and give permissions on those.
       */
      final StringTokenizer strTokenizer = new StringTokenizer(JAVA_CLASS_PATH, PATH_SEPARATOR);
      while (strTokenizer.hasMoreTokens()) {
        final String pathElement = strTokenizer.nextToken();
        if (new File(pathElement).isDirectory()) {
          final FilePermission fp = new FilePermission(pathElement + recursiveSuffix, "read");
          CP_READ_PERMISSIONS.add(fp);
        } else {
          final FilePermission fp = new FilePermission(pathElement, "read");
          CP_READ_PERMISSIONS.add(fp);
        }
      }
      CORE_PERMISSIONS.addAll(CP_READ_PERMISSIONS);

      // Java 9 early access requires this while loading resources in Swing internal code.
      // TODO: This could be reported upstream. The Swing code should call doPrivileged().
      //       Alternatively, check if it is still required when final release of Java 9 is available.
      CORE_PERMISSIONS.add(new FilePermission(JAVA_HOME + recursiveSuffix, "read"));
    }

  }

  private static void addStoreDirectoryPermissions(final Collection<Permission> permissions) {
    permissions.add(new FilePermission(STORE_DIRECTORY_CANONICAL + recursiveSuffix, "read, write, delete"));
  }

  private static void addExtensionPermissions(final Collection<Permission> extensionPermissions) {
    Arrays.stream(ExtensionManager.getExtDirs()).forEach(f -> {
      extensionPermissions.add(new FilePermission(f.getAbsolutePath() + recursiveSuffix, "read"));
    });
    Arrays.stream(ExtensionManager.getExtFiles()).forEach(f -> {
      extensionPermissions.add(new FilePermission(f.getAbsolutePath() + recursiveSuffix, "read"));
    });
  }

  private static void copyPermissions(final Collection<Permission> source, final Collection<Permission> destination) {
    for (final Permission p : source) {
      destination.add(p);
    }
  }

  private static void copyPermissions(final Collection<Permission> source, final PermissionCollection destination) {
    for (final Permission p : source) {
      destination.add(p);
    }
  }

  /**
   * Adds permissions to the base set of permissions assigned to privileged
   * code, i.e. code loaded from the local system rather than a remote location.
   * This method must be called before a security manager has been set.
   *
   * @param permission
   *          A <code>Permission<code> instance.
   */
  public static void addPrivilegedPermission(final Permission permission) {
    BASE_PRIVILEGE.add(permission);
  }

  /**
   *
   */
  private LocalSecurityPolicy() {
  }

  public static LocalSecurityPolicy getInstance() {
    return instance;
  }

  public static boolean hasHost(final java.net.URL url) {
    final String host = url.getHost();
    return (host != null) && !"".equals(host);
  }

  public static boolean isLocal(final java.net.URL url) {
    // Should return true only if we are sure
    // the file has either been downloaded by
    // the user, was distributed with the OS,
    // or was distributed with the browser.
    if (url == null) {
      return false;
    }
    final String scheme = url.getProtocol();
    if ("http".equalsIgnoreCase(scheme)) {
      return false;
    } else if ("file".equalsIgnoreCase(scheme)) {
      if (hasHost(url)) {
        return false;
      }
      if (unoMatch(url)) {
        return true;
      }
      // Files under the settings directory (e.g. cached JARs)
      // are considered remote.
      final String filePath = url.getPath();
      final Boolean result = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
        public Boolean run() {
          final File file = new File(filePath);
          try {
            final String canonical = file.getCanonicalPath();
            return !canonical.startsWith(STORE_DIRECTORY_CANONICAL);
          } catch (final java.io.IOException ioe) {
            ioe.printStackTrace(System.err);
            return false;
          }
        }
      });
      return result.booleanValue();
    } else if ("jar".equalsIgnoreCase(scheme)) {
      final String path = url.getPath();
      final int emIdx = path.lastIndexOf('!');
      final String subUrlString = emIdx == -1 ? path : path.substring(0, emIdx);
      try {
        final URL subUrl = new URL(subUrlString);
        return isLocal(subUrl);
      } catch (final java.net.MalformedURLException mfu) {
        return false;
      }
    } else if (ExtensionManager.ZIPENTRY_PROTOCOL.equalsIgnoreCase(scheme)) {
      return true;
    } else if ("jrt".equals(scheme)) {
      return true;
    } else {
      return false;
    }
  }

  private final static URL unoPath;
  static {
    URL unoPathTemp = null;
    try {
      final Class<?> unoClass = ClassLoader.getSystemClassLoader().loadClass("uno.Uno");
      unoPathTemp = unoClass.getProtectionDomain().getCodeSource().getLocation();
    } catch (final ClassNotFoundException e) {
      // ignore
    } finally {
      unoPath = unoPathTemp;
    }
  }

  private static boolean unoMatch(final URL url) {
    if (unoPath != null) {
      return unoPath.equals(url);
    } else {
      return false;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.security.Policy#getPermissions(java.security.CodeSource)
   */
  @Override
  public PermissionCollection getPermissions(final CodeSource codesource) {
    if (codesource == null) {
      // throw new AccessControlException("codesource was null");
      // Open issue GH: #117
      final Permissions permissions = new Permissions();
      // permissions.add(new AllPermission()); // TODO: Whoa! all permissions?
      permissions.add(new PropertyPermission("*", "read"));
      return permissions;
    }

    if (PlatformInit.getInstance().debugOn) {
      System.out.println("Codesource: " + codesource.getLocation());
      if (codesource.getCodeSigners() != null) {
        System.out.println("  signers: " + codesource.getCodeSigners().length);
      }
    }

    // TODO: Important: This was required after switching to JDK Rhino. This
    // method gets called twice:
    // once with proper codesource and once with null. The second call needs
    // accessClassInPackage.sun.org.mozilla.javascript.internal.
    /* Doesn't seem to be required anymore!
    if (codesource == null) {
      final Permissions permissions = new Permissions();
      // Update: We are using Mozilla rhino latest version, and this is not required anymore
      // But some permission has to be returned.
      // permissions.add(new RuntimePermission("accessClassInPackage.sun.org.mozilla.javascript.internal"));
      // System.err.println("No Codesource:");
      // Thread.dumpStack();
      //permissions.add(new RuntimePermission("setContextClassLoader"));
      for (final Permission p : BASE_PRIVILEGE) {
        permissions.add(p);
      }
      permissions.add(StoreHostPermission.forHost("localhost"));
      return permissions;
    } */
    final URL location = codesource.getLocation();
    if (location == null) {
      throw new AccessControlException("No location for codesource=" + codesource);
    }

    final boolean isLocal = isLocal(location);

    final Permissions permissions = new Permissions();
    if (isLocal) {
      final String path = location.toExternalForm();

      if (path.endsWith("slf4j-api-1.7.25.jar")) {
        permissions.add(new PropertyPermission("slf4j.*", "read"));
        permissions.add(new PropertyPermission("java.vendor.url", "read"));
      } else if (path.endsWith("jstyleparser-3.3-SNAPSHOT.jar")) {
        permissions.add(new PropertyPermission("slf4j.*", "read"));
        permissions.add(new PropertyPermission("java.vendor.url", "read"));
      } else if (path.endsWith("h2-1.4.196.jar")) {
        final String userDBPath = StorageManager.getInstance().userDBPath;
        permissions.add(new FilePermission(STORE_DIRECTORY_CANONICAL, "read"));

        // h2 accesses the the userDBPath on Windows
        permissions.add(new FilePermission(userDBPath, "read, write, delete"));

        // TODO: Request h2 to provide this list
        final String[] h2Suffixes = new String[] {
            Constants.SUFFIX_LOCK_FILE,
            Constants.SUFFIX_PAGE_FILE,
            Constants.SUFFIX_MV_FILE,
            Constants.SUFFIX_TEMP_FILE,
            Constants.SUFFIX_TRACE_FILE,
            Constants.SUFFIX_MV_FILE + Constants.SUFFIX_MV_STORE_NEW_FILE,
            Constants.SUFFIX_MV_FILE + Constants.SUFFIX_MV_STORE_TEMP_FILE,
            Constants.SUFFIX_DB_FILE,
            ".data.db",
        };
        for (final String suffix : h2Suffixes) {
          permissions.add(new FilePermission(userDBPath + suffix, "read, write, delete"));
        }
        permissions.add(new PropertyPermission("line.separator", "read"));
        permissions.add(new PropertyPermission("file.separator", "read"));
        permissions.add(new PropertyPermission("file.encoding", "read"));
        permissions.add(new PropertyPermission("java.specification.version", "read"));
        permissions.add(new PropertyPermission("h2.*", "read"));
        permissions.add(new RuntimePermission("shutdownHooks"));
        // TODO: Questionable
        permissions.add(new PropertyPermission("user.home", "read"));
      } else if (path.endsWith("jooq-3.4.2.jar")) {
        permissions.add(new PropertyPermission("org.jooq.settings", "read"));
      } else if (unoMatch(location)) {
        permissions.add(new FilePermission(unoPath.getPath(), "read"));
      } else if (path.endsWith("core.jar") || path.contains("Common") || path.contains("Primary_Extension")
          || path.contains("HTML_Renderer")) {

        copyPermissions(CORE_PERMISSIONS, permissions);
        copyPermissions(BASE_PRIVILEGE, permissions);

        // These allow request headers to be read. Useful for reading cache related headers, etc
        permissions.add(new URLPermission("http:*", "GET:*"));
        permissions.add(new URLPermission("https:*", "GET:*"));

        // Custom permissions
        permissions.add(StoreHostPermission.forURL(location)); // TODO: Check if really required
        permissions.add(new RuntimePermission("com.sun.media.jmc.accessMedia"));

        // Added due to OkHttp
        permissions.add(new NetPermission("getProxySelector"));
        permissions.add(new NetPermission("getCookieHandler"));

      } else if (path.endsWith("sac.jar")) {
        permissions.add(new PropertyPermission("org.w3c.css.sac.parser", "read"));
      } else if (path.endsWith("js.jar")) {
        permissions.add(new PropertyPermission("java.vm.name", "read"));
        permissions.add(new PropertyPermission("line.separator", "read"));
        permissions.add(new PropertyPermission("rhino.stack.style", "read"));
        permissions.add(new RuntimePermission("getClassLoader"));

      } else if (path.endsWith("okhttp-urlconnection-3.13.1.jar")) {
        permissions.add(new SocketPermission("*", "connect,resolve,listen,accept"));
        permissions.add(new RuntimePermission("modifyThread"));
      } else if (path.endsWith("okhttp-3.13.1.jar")) {
        permissions.add(new NetPermission("getProxySelector"));
        permissions.add(new PropertyPermission("okhttp.*", "read"));
        permissions.add(new SocketPermission("*", "connect,resolve,listen,accept"));
        permissions.add(new RuntimePermission("modifyThread"));
      } else if (path.startsWith(JAVA_HOME_URL) || path.startsWith("jrt:/java") || path.startsWith("jrt:/jdk")) {
        // This is to allow libraries to be loaded by JDK classes. Required for SSL libraries for example.
        permissions.add(new FilePermission(JAVA_HOME + recursiveSuffix, "read,execute"));

        permissions.add(new RuntimePermission("loadLibrary.sunec"));

        permissions.add(new RuntimePermission("accessClassInPackage.*"));
        permissions.add(new SecurityPermission("putProviderProperty.*"));

        // For GH-248
        permissions.add(GenericLocalPermission.EXT_GENERIC);
        permissions.add(new FilePermission(STORE_DIRECTORY_CANONICAL, "read"));
        permissions.add(new FilePermission(STORE_DIRECTORY_CANONICAL + recursiveSuffix, "read,write"));
        permissions.add(new RuntimePermission("setContextClassLoader"));

        permissions.add(new PropertyPermission("*", "read"));
        permissions.add(new RuntimePermission("shutdownHooks"));
        copyPermissions(CP_READ_PERMISSIONS, permissions);
      } else if (path.startsWith("jrt:/jdk")) {
        permissions.add(new RuntimePermission("accessClassInPackage.sun.*"));
      }
    } else {
      // TODO: Check why the following are required and add comments for each
      // permissions.add(new PropertyPermission("java.version", "read"));
      // permissions.add(new PropertyPermission("os.name", "read"));
      // permissions.add(new PropertyPermission("line.separator", "read"));
      // permissions.add(new SocketPermission(location.getHost(), "connect,resolve"));

      // TODO: Security: This permission should not be given, but it's required
      // by compiled JavaFX runtime at the moment (2/20/2008).
      // permissions.add(new AWTPermission("accessEventQueue"));

      final String hostName = location.getHost();
      // Get possible cookie domains for current location
      // and allow managed store access there.
      final Collection<String> domains = DomainValidation.getPossibleDomains(hostName);
      domains.forEach(domain -> permissions.add(StoreHostPermission.forHost(domain)));
    }

    if (PlatformInit.getInstance().debugOn) {
      System.out.println("Returning permissions: " + permissions);
    }

    return permissions;
  }

}
