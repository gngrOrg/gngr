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
 * Created on Mar 12, 2005
 */
package org.lobobrowser.store;

import static org.jooq.impl.DSL.using;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jooq.DSLContext;
import org.lobobrowser.security.LocalSecurityPolicy;
import org.lobobrowser.security.StoreHostPermission;

/**
 * * @author J. H. S.
 */
public class StorageManager implements Runnable {
  private static final Logger logger = Logger.getLogger(StorageManager.class.getName());
  private static final long HOST_STORE_QUOTA = 200 * 1024;
  // Note that the installer makes assumptions about these names.
  private static final String HOST_STORE_DIR = "HostStore";
  private static final String CACHE_DIR = "cache";
  private static final String CONTENT_DIR = "content";
  private static final String SETTINGS_DIR = "settings";
  private static final StorageManager instance = new StorageManager();
  private final File storeDirectory;
  private final File cacheRootDirectory;
  public final String userDBPath;

  public static StorageManager getInstance() {
    return instance;
  }

  private StorageManager() {
    this.storeDirectory = LocalSecurityPolicy.STORE_DIRECTORY;
    this.cacheRootDirectory = new File(this.storeDirectory, CACHE_DIR);
    if (!this.storeDirectory.exists()) {
      this.storeDirectory.mkdirs();
    }

    userDBPath = new File(storeDirectory, "user.h2").getAbsolutePath();

  }

  private DSLContext userDB;
  private Connection dbConnection;

  public synchronized DSLContext getDB() {
    if (userDB == null) {
      try {
        Class.forName("org.h2.Driver");
        dbConnection = DriverManager.getConnection("jdbc:h2:" + userDBPath, "sa", "");
        userDB = using(dbConnection);
        initDB(userDB);
      } catch (SQLException | ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    return userDB;
  }

  private void initDB(final DSLContext userDB) {
    final int tableCount = getTableCount(userDB);

    if (tableCount == 0) {
      final InputStream schemaStream = getClass().getResourceAsStream("/info/gngr/schema.sql");
      try (
        final Scanner scanner = new Scanner(schemaStream, "UTF-8")) {
        final String text = scanner.useDelimiter("\\A").next();
        userDB.execute(text);
      }
    }

  }

  private static int getTableCount(final DSLContext userDB) {
    // TODO: https://stackoverflow.com/questions/24741761/how-to-check-if-a-table-exists-in-jooq
    final int tableCount =
        userDB
            .selectCount()
            .from("INFORMATION_SCHEMA.TABLES")
            .where("TABLE_SCHEMA = 'PUBLIC'")
            .fetchOne().value1();
    return tableCount;
  }

  private boolean threadStarted = false;

  private void ensureThreadStarted() {
    if (!this.threadStarted) {
      synchronized (this) {
        if (!this.threadStarted) {
          final Thread t = new Thread(this, "StorageManager");
          t.setDaemon(true);
          t.setPriority(Thread.MIN_PRIORITY);
          t.start();
          this.threadStarted = true;
        }
      }
    }
  }

  public File getAppHome() {
    return this.storeDirectory;
  }

  private static final String NO_HOST = "$NO_HOST$";

  public File getCacheHostDirectory(String hostName) throws IOException {
    CacheManager.getInstance();
    final File cacheDir = this.getCacheRoot();
    if ((hostName == null) || "".equals(hostName)) {
      hostName = NO_HOST;
    }
    return new File(cacheDir, normalizedFileName(hostName));
  }

  public File getContentCacheFile(final String hostName, final String fileName) throws IOException {
    final File domainDir = this.getCacheHostDirectory(hostName);
    final File xamjDir = new File(domainDir, CONTENT_DIR);
    return new File(xamjDir, fileName);
  }

  public File getCacheRoot() {
    return this.cacheRootDirectory;
  }

  private final Map<String, RestrictedStore> restrictedStoreCache = new HashMap<>();

  /**
   * @param hostName
   *          should be canonicalized to lower case
   */
  public RestrictedStore getRestrictedStore(String hostName, final boolean createIfNotExists) throws IOException {
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(StoreHostPermission.forHost(hostName));
    }
    if ((hostName == null) || "".equals(hostName)) {
      hostName = NO_HOST;
    }
    final String normHost = hostName;
    RestrictedStore store;
    synchronized (this) {
      store = this.restrictedStoreCache.get(normHost);
      if (store == null) {
        store = AccessController.doPrivileged(new PrivilegedAction<RestrictedStore>() {
          // Reason: Since we are checking StoreHostPermission previously,
          // this is fine.
          public RestrictedStore run() {
            final File hostStoreDir = new File(storeDirectory, HOST_STORE_DIR);
            final File domainDir = new File(hostStoreDir, normalizedFileName(normHost));
            if (!createIfNotExists && !domainDir.exists()) {
              return null;
            }
            try {
              return new RestrictedStore(domainDir, HOST_STORE_QUOTA);
            } catch (final IOException ioe) {
              throw new IllegalStateException(ioe);
            }
          }
        });
        if (store != null) {
          this.restrictedStoreCache.put(normHost, store);
        }
      }
    }
    if (store != null) {
      this.ensureThreadStarted();
    }
    return store;
  }

  public File getSettingsDirectory() {
    return new File(this.storeDirectory, SETTINGS_DIR);
  }

  public void saveSettings(final String name, final Serializable data) throws IOException {
    final File dir = this.getSettingsDirectory();
    if (!dir.exists()) {
      dir.mkdirs();
    }
    final File file = new File(dir, name);
    try (
        final OutputStream out = new FileOutputStream(file);
        final BufferedOutputStream bos = new BufferedOutputStream(out);
        final ObjectOutputStream oos = new ObjectOutputStream(bos);) {
      oos.writeObject(data);
      oos.flush();
    }
  }

  public Serializable retrieveSettings(final String name, final ClassLoader classLoader) throws IOException, ClassNotFoundException {
    final File dir = this.getSettingsDirectory();
    if (!dir.exists()) {
      return null;
    }
    final File file = new File(dir, name);
    if (!file.exists()) {
      return null;
    }
    try (
        final InputStream in = new FileInputStream(file);
        final BufferedInputStream bin = new BufferedInputStream(in);
        final ObjectInputStream ois = new ClassLoaderObjectInputStream(bin, classLoader);) {
      return (Serializable) ois.readObject();
    } catch (final InvalidClassException ice) {
      ice.printStackTrace();
      return null;
    }
  }

  // public Collection getBroadRestrictedStores(String hostName) throws
  // IOException {
  // SecurityManager sm = System.getSecurityManager();
  // if(sm != null) {
  // sm.checkPermission(HostPermission.forHost(hostName));
  // }
  // File hostStoreDir = new File(this.settingsDirectory, HOST_STORE_DIR);
  // if(hostName == null || "".equals(hostName)) {
  // hostName = NO_HOST;
  // File domainDir = new File(hostStoreDir, normalizedFileName(hostName));
  // return Collections.singleton(new RestrictedStore(domainDir,
  // HOST_STORE_QUOTA));
  // }
  // else {
  // Collection restrictedStores = new LinkedList();
  // File[] domainDirs = hostStoreDir.listFiles(new
  // CookieHostFilenameFilter(hostName));
  // if(domainDirs != null) {
  // for(int i = 0; i < domainDirs.length; i++) {
  // restrictedStores.add(new RestrictedStore(domainDirs[i], HOST_STORE_QUOTA));
  // }
  // }
  // return restrictedStores;
  // }
  // }

  static String normalizedFileName(final String hostName) {
    return hostName;
  }

  static String getHostName(final String fileName) {
    return fileName;
  }

  private static final int MANAGED_STORE_UPDATE_DELAY = 1000 * 60 * 5; /* 5 minutes */

  public void run() {
    for (;;) {
      try {
        Thread.sleep(MANAGED_STORE_UPDATE_DELAY);
        RestrictedStore[] stores;
        synchronized (this) {
          stores = this.restrictedStoreCache.values().toArray(new RestrictedStore[0]);
        }
        for (final RestrictedStore store : stores) {
          Thread.yield();
          store.updateSizeFile();
        }
      } catch (final Exception err) {
        logger.log(Level.SEVERE, "run()", err);
        try {
          Thread.sleep(MANAGED_STORE_UPDATE_DELAY);
        } catch (final java.lang.InterruptedException ie) {
          // Ignore this time.
        }
      }
    }
  }

  public synchronized void shutdown() {
    if (dbConnection != null) {
      try {
        dbConnection.close();
      } catch (final SQLException e) {
        // Since we are shutting down, we shouldn't bubble any exception, to let other modules shutdown gracefully
        e.printStackTrace();
      }
    }
  }
}
