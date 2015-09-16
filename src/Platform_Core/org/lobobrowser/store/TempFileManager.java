/*
    GNU GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The Lobo Project

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public
    License as published by the Free Software Foundation; either
    version 2 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Contact info: lobochief@users.sourceforge.net
 */
package org.lobobrowser.store;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

import org.lobobrowser.security.LocalSecurityPolicy;

// TODO: This doesn't seem to be used anymore. Remove?
class TempFileManager {
  private static TempFileManager instance;
  private static final String GENERAL_PREFIX = "gngr-";
  private static final long ONE_DAY = 24L * 60 * 60 * 1000;
  private static final long ONE_MONTH = 30L * ONE_DAY;
  private static final long THIRTY_YEARS = 30L * 365 * ONE_DAY;
  private static final String FILE_PREFIX = GENERAL_PREFIX + ((System.currentTimeMillis() - THIRTY_YEARS) / 1000) + "-";

  private final File TEMP_DIRECTORY;
  private final ReferenceQueue<JarFile> REFERENCE_QUEUE = new ReferenceQueue<>();
  private final Map<String, LocalWeakReference> wrByPath = new HashMap<>();

  private int counter = 0;

  public static TempFileManager getInstance() {
    // Do it this way to allow other statics to initialize.
    if (instance == null) {
      synchronized (TempFileManager.class) {
        if (instance == null) {
          instance = new TempFileManager();
        }
      }
    }
    return instance;
  }

  private TempFileManager() {
    Runtime.getRuntime().addShutdownHook(new ShutdownThread());
    final File tempDirectory = new File(LocalSecurityPolicy.STORE_DIRECTORY, "tmp");
    TEMP_DIRECTORY = tempDirectory;
    if (!tempDirectory.exists()) {
      tempDirectory.mkdirs();
    }
    final File[] files = tempDirectory.listFiles();
    if (files != null) {
      // Cleanup files theoretically left by previously running instance.
      for (final File file : files) {
        final String name = file.getName();
        if (name.startsWith(GENERAL_PREFIX) && !name.startsWith(FILE_PREFIX)) {
          // We can't really assume only one instance of the application
          // is running. Need to be a little lenient about deleting these.
          if (file.lastModified() < (System.currentTimeMillis() - ONE_MONTH)) {
            file.delete();
          }
        }
      }
    }
  }

  private void shutdownCleanup() {
    final File[] files = TEMP_DIRECTORY.listFiles();
    if (files != null) {
      for (final File file : files) {
        try {
          final String name = file.getName();
          if (name.startsWith(FILE_PREFIX)) {
            final String canonical = file.getCanonicalPath();
            synchronized (this) {
              // Need to close these JAR files, otherwise
              // deletion does not happen in Windows.
              final LocalWeakReference wr = this.wrByPath.get(canonical);
              final JarFile jarFile = wr.get();
              if (jarFile != null) {
                jarFile.close();
              }
            }
            file.delete();
          }
        } catch (final java.io.IOException ioe) {
          // ignore
        }
      }
    }
  }

  public JarFile createJarFile(final byte[] bytes) throws java.io.IOException {
    // Dequeue and clean up first
    for (;;) {
      final Reference<? extends JarFile> ref = REFERENCE_QUEUE.poll();
      if (ref == null) {
        break;
      }
      final String canonical = ((LocalWeakReference) ref).canonicalPath;
      new File(canonical).delete();
      synchronized (this) {
        this.wrByPath.remove(canonical);
      }
    }
    final File file = this.newTempFile();
    try (
      final OutputStream out = new FileOutputStream(file)) {
      out.write(bytes);
    }
    final JarFile jarFile = new JarFile(file);
    final String canonical = file.getCanonicalPath();
    final LocalWeakReference wr = new LocalWeakReference(jarFile, REFERENCE_QUEUE, canonical);
    synchronized (this) {
      // This serves simply to retain the weak reference.
      this.wrByPath.put(canonical, wr);
    }
    return jarFile;
  }

  public File newTempFile() throws IOException {
    synchronized (this) {
      final int newCounter = this.counter++;
      final File file = new File(this.TEMP_DIRECTORY, FILE_PREFIX + newCounter);
      return file;
    }
  }

  private static class LocalWeakReference extends WeakReference<JarFile> {
    public final String canonicalPath;

    public LocalWeakReference(final JarFile referent, final ReferenceQueue<? super JarFile> q, final String canonicalPath) {
      super(referent, q);
      this.canonicalPath = canonicalPath;
    }
  }

  private class ShutdownThread extends Thread {
    @Override
    public void run() {
      shutdownCleanup();
    }
  }
}
