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
 * Created on Jun 19, 2005
 */
package org.lobobrowser.primary.clientlets;

import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import org.lobobrowser.util.BaseClassLoader;
import org.lobobrowser.util.CollectionUtilities;
import org.lobobrowser.util.GenericURLConnection;
import org.lobobrowser.util.io.IORoutines;

/**
 * @author J. H. S.
 */
public class ArchiveClassLoader extends BaseClassLoader {
  private static final Logger logger = Logger.getLogger(ArchiveClassLoader.class.getName());

  /**
   * @author J. H. S.
   */
  public class LocalURLStreamHandler extends java.net.URLStreamHandler {
    private final String resourceName;

    /**
     *
     */
    public LocalURLStreamHandler(final String resourceName) {
      super();
      this.resourceName = resourceName;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.net.URLStreamHandler#openConnection(java.net.URL)
     */
    @Override
    protected URLConnection openConnection(final URL u) throws IOException {
      return new GenericURLConnection(u, getResourceAsStreamImpl(this.resourceName));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.net.URLStreamHandler#openConnection(java.net.URL,
     * java.net.Proxy)
     */
    @Override
    protected URLConnection openConnection(final URL u, final Proxy p) throws IOException {
      return this.openConnection(u);
    }
  }

  private final ArchiveInfo[] archiveInfos;

  /**
   * @param targetParent
   */
  ArchiveClassLoader(final java.util.Collection<ArchiveInfo> archiveInfos) throws IOException {
    super(ArchiveClassLoader.class.getClassLoader());
    this.archiveInfos = archiveInfos.toArray(ArchiveInfo.EMPTY_ARRAY);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.ClassLoader#findClass(java.lang.String)
   */
  @Override
  protected Class<?> findClass(final String arg0) throws ClassNotFoundException {
    final String subPath = arg0.replace('.', '/') + ".class";
    final ArchiveInfo[] ainfos = this.archiveInfos;
    final int len = ainfos.length;
    byte[] classBytes = null;
    final ArchiveInfo[] foundAinfo = new ArchiveInfo[1];
    for (int i = 0; i < len; i++) {
      final ArchiveInfo ainfo = ainfos[i];
      try {
        final JarFile jarFile = ainfo.getJarFile();
        classBytes = AccessController.doPrivileged(new PrivilegedAction<byte[]>() {
          public byte[] run() {
            try {
              final ZipEntry entry = jarFile.getEntry(subPath);
              if (entry == null) {
                return null;
              }
              try (final java.io.InputStream in = jarFile.getInputStream(entry)) {
                final byte[] bytes = IORoutines.loadExact(in, (int) entry.getSize());
                foundAinfo[0] = ainfo;
                return bytes;
              }
            } catch (final IOException ioe) {
              return null;
            }
          }
        });
      } catch (final IOException ioe2) {
        continue;
      }
      if (classBytes != null) {
        break;
      }
    }
    if (classBytes == null) {
      throw new ClassNotFoundException("I/O error or entry not found: " + subPath);
    }
    // TODO Signers Certificates
    final CodeSource cs = new CodeSource(foundAinfo[0].url, new java.security.cert.Certificate[0]);
    return this.defineClass(arg0, classBytes, 0, classBytes.length, cs);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.ClassLoader#findResource(java.lang.String)
   */
  @Override
  protected URL findResource(final String name) {
    try {
      return AccessController.doPrivileged(new PrivilegedAction<java.net.URL>() {
        public java.net.URL run() {
          try {
            return new java.net.URL(null, "volatile:" + name, new LocalURLStreamHandler(name));
          } catch (final java.net.MalformedURLException mfu) {
            throw new IllegalStateException(mfu.getMessage());
          }
        }
      });
    } catch (final RuntimeException err) {
      logger.log(Level.SEVERE, "findResource()", err);
      throw err;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.ClassLoader#findResources(java.lang.String)
   */
  @Override
  protected Enumeration<URL> findResources(final String name) throws IOException {
    final URL url = this.findResource(name);
    if (url != null) {
      return CollectionUtilities.getIteratorEnumeration(Collections.singletonList(url).iterator());
    } else {
      return CollectionUtilities.getEmptyEnumeration();
    }
  }

  private java.io.InputStream getResourceAsStreamImpl(final String resourceName) {
    final ArchiveInfo[] ainfos = this.archiveInfos;
    final int len = ainfos.length;
    InputStream in = null;
    for (int i = 0; i < len; i++) {
      final ArchiveInfo ainfo = ainfos[i];
      try {
        final JarFile jarFile = ainfo.getJarFile();
        in = AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
          public InputStream run() {
            try {
              final ZipEntry entry = jarFile.getEntry(resourceName);
              if (entry == null) {
                return null;
              }
              return jarFile.getInputStream(entry);
            } catch (final IOException ioe) {
              return null;
            }
          }
        });
      } catch (final IOException ioe2) {
        continue;
      }
      if (in != null) {
        break;
      }
    }
    if (in == null) {
      final ClassLoader parent = this.getParent();
      return parent != null ? parent.getResourceAsStream(resourceName) : null;
    } else {
      return in;
    }
  }
}
