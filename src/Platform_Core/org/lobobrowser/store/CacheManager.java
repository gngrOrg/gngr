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
 * Created on Jun 12, 2005
 */
package org.lobobrowser.store;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lobobrowser.security.GenericLocalPermission;
import org.lobobrowser.util.LRUCache;
import org.lobobrowser.util.Strings;
import org.lobobrowser.util.Urls;
import org.lobobrowser.util.io.IORoutines;

/**
 * @author J. H. S.
 */
public final class CacheManager implements Runnable {
  private static final Logger logger = Logger.getLogger(CacheManager.class.getName());
  private static final int AFTER_SWEEP_SLEEP = 5 * 60 * 1000;
  private static final int INITIAL_SLEEP = 30 * 1000;
  private static final int DELETE_TOLERANCE = 60 * 1000;
  private static final long MAX_CACHE_SIZE = 100000000;

  private final LRUCache transientCache = new LRUCache(1000000);

  /**
   *
   */
  private CacheManager() {
    super();
    final Thread t = new Thread(this, "CacheManager");
    t.setDaemon(true);
    t.setPriority(Thread.MIN_PRIORITY);
    t.start();
  }

  private static CacheManager instance;

  public static CacheManager getInstance() {
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(GenericLocalPermission.EXT_GENERIC);
    }
    if (instance == null) {
      synchronized (CacheManager.class) {
        if (instance == null) {
          instance = new CacheManager();
        }
      }
    }
    return instance;
  }

  public void putTransient(final URL url, final Object value, final int approxSize) {
    final String key = Urls.getNoRefForm(url);
    synchronized (this.transientCache) {
      this.transientCache.put(key, value, approxSize);
    }
  }

  public Object getTransient(final URL url) {
    final String key = Urls.getNoRefForm(url);
    synchronized (this.transientCache) {
      return this.transientCache.get(key);
    }
  }

  public void removeTransient(final URL url) {
    final String key = Urls.getNoRefForm(url);
    synchronized (this.transientCache) {
      this.transientCache.remove(key);
    }
  }

  public void setMaxTransientCacheSize(final int approxMaxSize) {
    synchronized (this.transientCache) {
      this.transientCache.setApproxMaxSize(approxMaxSize);
    }
  }

  public int getMaxTransientCacheSize() {
    synchronized (this.transientCache) {
      return this.transientCache.getApproxMaxSize();
    }
  }

  public CacheInfo getTransientCacheInfo() {
    long approxSize;
    int numEntries;
    List<?> entryInfo;
    synchronized (this.transientCache) {
      approxSize = this.transientCache.getApproxSize();
      numEntries = this.transientCache.getNumEntries();
      entryInfo = this.transientCache.getEntryInfoList();
    }
    return new CacheInfo(approxSize, numEntries, entryInfo);
  }

  public static void putPersistent(final URL url, final byte[] rawContent, final boolean isDecoration) throws IOException {
    final File cacheFile = getCacheFile(url, isDecoration);
    synchronized (getLock(cacheFile)) {
      final File parent = cacheFile.getParentFile();
      if ((parent != null) && !parent.exists()) {
        parent.mkdirs();
      }
      final FileOutputStream fout = new FileOutputStream(cacheFile);
      try {
        fout.write(rawContent);
      } finally {
        fout.close();
      }
    }
  }

  public static byte[] getPersistent(final URL url, final boolean isDecoration) throws IOException {
    // We don't return an InputStream because further synchronization
    // would be needed to prevent concurrent writes into the file.
    final File cacheFile = getCacheFile(url, isDecoration);
    synchronized (getLock(cacheFile)) {
      cacheFile.setLastModified(System.currentTimeMillis());
      try {
        return IORoutines.load(cacheFile);
      } catch (final java.io.FileNotFoundException fnf) {
        return null;
      }
    }
  }

  public static boolean removePersistent(final URL url, final boolean isDecoration) throws IOException {
    final File cacheFile = getCacheFile(url, isDecoration);
    synchronized (getLock(cacheFile)) {
      return cacheFile.delete();
    }
  }

  public static JarFile getJarFile(final URL url) throws java.io.IOException {
    final File cacheFile = getCacheFile(url, false);
    synchronized (getLock(cacheFile)) {
      if (!cacheFile.exists()) {
        if (Urls.isLocalFile(url)) {
          return new JarFile(url.getFile());
        }
        throw new java.io.FileNotFoundException("JAR file cannot be obtained for a URL that is not cached locally: " + url + ".");
      }
      cacheFile.setLastModified(System.currentTimeMillis());
      return new JarFile(cacheFile);
    }
  }

  private static File getCacheFile(final URL url, final boolean isDecoration) throws IOException {
    // Use file, not path, because query string matters in caching.
    final String urlFile = url.getFile();
    final String urlText = Urls.getNoRefForm(url);
    final int lastSlashIdx = urlFile.lastIndexOf('/');
    String simpleName = lastSlashIdx == -1 ? urlFile : urlFile.substring(lastSlashIdx + 1);
    if (simpleName.length() > 16) {
      simpleName = simpleName.substring(0, 16);
    }
    final String normalizedName = Strings.getJavaIdentifier(simpleName);
    final String hash = Strings.getMD5(urlText);
    String fileName = normalizedName + "_" + hash;
    if (isDecoration) {
      fileName += ".decor";
    }
    // TODO: Use lowercase hostname (for case-insensitive match)
    return StorageManager.getInstance().getContentCacheFile(url.getHost(), fileName);
  }

  private static Object getLock(final File file) throws IOException {
    return ("cm:" + file.getCanonicalPath()).intern();
  }

  /**
   * Touches the cache file corresponding to the given URL and returns
   * <code>true</code> if the file exists.
   */
  public static boolean checkCacheFile(final URL url, final boolean isDecoration) throws IOException {
    final File file = getCacheFile(url, isDecoration);
    synchronized (getLock(file)) {
      if (file.exists()) {
        file.setLastModified(System.currentTimeMillis());
        return true;
      }
      return false;
    }
  }

  public void run() {
    try {
      Thread.sleep(INITIAL_SLEEP);
    } catch (final InterruptedException ie) {
      // ignore
    }
    for (;;) {
      try {
        this.sweepCache();
        Thread.sleep(AFTER_SWEEP_SLEEP);
      } catch (final Exception err) {
        logger.log(Level.SEVERE, "run()", err);
        try {
          Thread.sleep(AFTER_SWEEP_SLEEP);
        } catch (final java.lang.InterruptedException ie) {
          // ignore
        }
      }
    }
  }

  private static long getMaxCacheSize() {
    return MAX_CACHE_SIZE;
  }

  private void sweepCache() throws Exception {
    final CacheStoreInfo sinfo = this.getCacheStoreInfo();
    if (logger.isLoggable(Level.INFO)) {
      logger.info("sweepCache(): Cache size is " + sinfo.getLength() + " with a max of " + getMaxCacheSize()
          + ". The number of cache files is " + sinfo.getFileInfos().length + ".");
    }
    long oversize = sinfo.getLength() - getMaxCacheSize();
    if (oversize > 0) {
      final CacheFileInfo[] finfos = sinfo.getFileInfos();
      // Sort in ascending order of modification
      Arrays.sort(finfos);
      final long okToDeleteBeforeThis = System.currentTimeMillis() - DELETE_TOLERANCE;
      for (final CacheFileInfo finfo : finfos) {
        try {
          Thread.yield();
          synchronized (getLock(finfo.getFile())) {
            final long lastModified = finfo.getLastModified();
            if (lastModified < okToDeleteBeforeThis) {
              Thread.sleep(1);
              final long time1 = System.currentTimeMillis();
              finfo.delete();
              final long time2 = System.currentTimeMillis();
              if (logger.isLoggable(Level.INFO)) {
                logger.info("sweepCache(): Removed " + finfo + " in " + (time2 - time1) + " ms.");
              }
              oversize -= finfo.getInitialLength();
              if (oversize <= 0) {
                break;
              }
            }
          }
        } catch (final Exception thrown) {
          logger.log(Level.WARNING, "sweepCache()", thrown);
        }
      }
    }
  }

  private CacheStoreInfo getCacheStoreInfo() throws IOException {
    final CacheStoreInfo csinfo = new CacheStoreInfo();
    final File cacheRoot = StorageManager.getInstance().getCacheRoot();
    populateCacheStoreInfo(csinfo, cacheRoot);
    return csinfo;
  }

  private void populateCacheStoreInfo(final CacheStoreInfo csinfo, final File directory) {
    final File[] files = directory.listFiles();
    if (files == null) {
      // TODO: For large directories, java.nio.file.Files.newDirectoryStream() is supposedly faster.
      logger.severe("populateCacheStoreInfo(): Unexpected: '" + directory + "' is not a directory.");
      return;
    }
    if (files.length == 0) {
      directory.delete();
    }
    for (final File file : files) {
      Thread.yield();
      if (file.isDirectory()) {
        this.populateCacheStoreInfo(csinfo, file);
      } else {
        csinfo.addCacheFile(file);
      }
    }
  }

}
