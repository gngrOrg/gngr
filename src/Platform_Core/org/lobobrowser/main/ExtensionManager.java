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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;

import javax.swing.SwingUtilities;

import org.lobobrowser.clientlet.Clientlet;
import org.lobobrowser.clientlet.ClientletRequest;
import org.lobobrowser.clientlet.ClientletResponse;
import org.lobobrowser.ua.NavigationEvent;
import org.lobobrowser.ua.NavigationVetoException;
import org.lobobrowser.ua.NavigatorEventType;
import org.lobobrowser.ua.NavigatorExceptionEvent;
import org.lobobrowser.ua.NavigatorFrame;
import org.lobobrowser.ua.NavigatorWindow;
import org.lobobrowser.ua.RequestType;
import org.lobobrowser.util.JoinableTask;

/**
 * Manages platform extensions.
 */
public class ExtensionManager {
  public static final String ZIPENTRY_PROTOCOL = "zipentry";
  private static final Logger logger = Logger.getLogger(ExtensionManager.class.getName());
  private static final ExtensionManager instance = new ExtensionManager();
  private static final String EXT_DIR_NAME = "ext";

  // Note: We do not synchronize around the extensions collection,
  // given that it is fully built in the constructor.
  private final Map<String, Extension> extensionById = new HashMap<>();
  private final SortedSet<Extension> extensions = new TreeSet<>();
  private final ArrayList<URL> libraryURLs = new ArrayList<>();

  private ExtensionManager() {
    this.createExtensionsAndLibraries(getExtDirs(), getExtFiles());
  }

  public static ExtensionManager getInstance() {
    // This security check should be enough, provided
    // ExtensionManager instances are not retained.
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(org.lobobrowser.security.GenericLocalPermission.EXT_GENERIC);
    }
    return instance;
  }

  public static File[] getExtDirs() {
    File[] extDirs;
    final String extDirsProperty = System.getProperty("ext.dirs");
    if (extDirsProperty == null) {
      final Optional<File> appDirOpt = PlatformInit.getInstance().getApplicationDirectory();
      if (appDirOpt.isPresent()) {
        extDirs = new File[] { new File(appDirOpt.get(), EXT_DIR_NAME) };
      } else {
        extDirs = new File[0];
      }
    } else {
      final StringTokenizer tok = new StringTokenizer(extDirsProperty, ",");
      final ArrayList<File> extDirsList = new ArrayList<>();
      while (tok.hasMoreTokens()) {
        final String token = tok.nextToken();
        extDirsList.add(new File(token.trim()));
      }
      extDirs = extDirsList.toArray(new File[0]);
    }
    return extDirs;
  }

  public static File[] getExtFiles() {
    File[] extFiles;
    final String extFilesPropertySystem = System.getProperty("ext.files");
    final String extFilesProperty = extFilesPropertySystem == null ? System.getProperty("jnlp.ext.files") : extFilesPropertySystem;
    if (extFilesProperty == null) {
      extFiles = new File[0];
    } else {
      final StringTokenizer tok = new StringTokenizer(extFilesProperty, ",");
      final ArrayList<File> extFilesList = new ArrayList<>();
      while (tok.hasMoreTokens()) {
        final String token = tok.nextToken();
        extFilesList.add(new File(token.trim()));
      }
      extFiles = extFilesList.toArray(new File[0]);
    }
    return extFiles;
  }

  private void addExtension(final File file) throws java.io.IOException {
    if (!file.exists()) {
      logger.warning("addExtension(): File " + file + " does not exist.");
      return;
    }
    if (Extension.isExtension(file)) {
      addExtension(new Extension(file));
    } else {
      libraryURLs.add(file.toURI().toURL());
    }
  }

  private void addExtension(final Extension ei) {
    this.extensionById.put(ei.getId(), ei);
    if (logger.isLoggable(Level.FINE)) {
      logger.fine("createExtensions(): Loaded extension: " + ei);
    }
    extensions.add(ei);
  }

  private void createExtensionsAndLibraries(final File[] extDirs, final File[] extFiles) {
    final Collection<Extension> extensions = this.extensions;
    final Map<String, Extension> extensionById = this.extensionById;
    extensions.clear();
    extensionById.clear();
    final List<URL> libraryEntryURLs = new LinkedList<>();

    addFlatExtensions();

    for (final File extDir : extDirs) {
      if (!extDir.exists()) {
        logger.warning("createExtensions(): Directory '" + extDir + "' not found.");
        if (PlatformInit.getInstance().isCodeLocationDirectory()) {
          logger
              .warning("createExtensions(): The application code location is a directory, which means the application is probably being run from an IDE. Additional setup is required. Please refer to README.txt file.");
        }
        continue;
      }
      if (extDir.isFile()) {
        // Check if it is a jar. We will load jars from inside this jar.
        try (
            final JarFile jf = new JarFile(extDir);) {
          // We can't close jf, because the class loader will load files lazily.
          for (final JarEntry jarEntry : (Iterable<JarEntry>) jf.stream()::iterator) {
            if (!jarEntry.isDirectory() && jarEntry.getName().endsWith(".jar")) {
              System.out.println("Found entry: " + jarEntry.getName());
              final InputStream jfIS = jf.getInputStream(jarEntry);
              final URL libURL = makeZipEntryURL(extDir.getName(), jfIS, jarEntry.getName());
              libraryEntryURLs.add(libURL);
            }
          }
        } catch (final IOException e) {
          logger.warning("Couldn't open: " + extDir);
          e.printStackTrace();
        }
      } else {
        final File[] extRoots = extDir.listFiles(new ExtFileFilter());
        if ((extRoots == null) || (extRoots.length == 0)) {
          logger.warning("createExtensions(): No potential extensions found in " + extDir + " directory.");
          continue;
        }
        addAllFileExtensions(extRoots);
      }
    }
    addAllFileExtensions(extFiles);

    if (this.extensionById.size() == 0) {
      logger.warning("createExtensions(): No extensions found. This is indicative of a setup error. Extension directories scanned are: "
          + Arrays.asList(extDirs) + ".");
    }

    loadExtensions(extensions, libraryURLs);
  }

  private void addFlatExtensions() {
    // TODO: in future, avoid using the flat-extensions file. All resources matching
    // a standard name like "lobo-extension.properties" can be automatically fetched
    // using ClassLoader.getResources() method. Uno needs to implement it though (needs URL magic).
    final ClassLoader loader = getClass().getClassLoader();
    final InputStream indexStream = getClass().getResourceAsStream("/flat-extensions");
    if (indexStream != null) {
      final BufferedReader indexReader = new BufferedReader(new InputStreamReader(indexStream));

      try {
        String propertyFileName;
        while ((propertyFileName = indexReader.readLine()) != null) {
          final InputStream propertyStream = loader.getResourceAsStream(propertyFileName);
          final Properties extensionAttributes = new Properties();
          extensionAttributes.load(propertyStream);
          addExtension(new Extension(extensionAttributes, loader));
        }
      } catch (final IOException e) {
        logger.log(Level.SEVERE, "Error while reading embedded resources", e);
      }
    }
  }

  /*
  private void addEmbeddedJars(final List<URL> libraryEntryURLs) {
    final ClassLoader loader = getClass().getClassLoader();
    final InputStream indexStream = getClass().getResourceAsStream("/jar-index");
    System.out.println("jar-index: " + indexStream);
    if (indexStream != null) {
      final BufferedReader indexReader = new BufferedReader(new InputStreamReader(indexStream));

      try {
        String fileName;
        while ((fileName = indexReader.readLine()) != null) {
          System.out.println("Filename: " + fileName);
          final InputStream entryStream = loader.getResourceAsStream(fileName);
          final URL entryURL = makeZipEntryURL("embedded-jar", entryStream, fileName);
          if (fileName.endsWith("Primary_Extension.jar")) {
            final Properties extensionAttributes = new Properties();
            extensionAttributes.put("extension.class", "org.lobobrowser.primary.ext.ExtensionImpl");
            addExtension(new Extension(entryURL, extensionAttributes, loader));
          } else {
            libraryEntryURLs.add(entryURL);
          }
        }
      } catch (IOException e) {
        logger.log(Level.SEVERE, "Error while reading embedded resources", e);
      }
    }
  }*/

  private static URL makeZipEntryURL(final String dirName, final InputStream jfIS, final String entryName) throws IOException,
      MalformedURLException {
    final String urlSpec = ZIPENTRY_PROTOCOL + "://" + dirName + "/" + entryName + "!/";
    final URL libURL = new URL(null, urlSpec, new ZipEntryHandler(new ZipInputStream(jfIS)));
    return libURL;
  }

  private void loadExtensions(final Collection<Extension> extensions,
      final Collection<URL> libraryURLCollection) {
    // Get the system class loader
    final ClassLoader rootClassLoader = this.getClass().getClassLoader();

    final URLClassLoader librariesCL = new URLClassLoader(libraryURLCollection.toArray(new URL[0]), rootClassLoader);

    // Initialize class loader in each extension, using librariesCL as
    // the parent class loader. Extensions are initialized in parallel.
    final Collection<JoinableTask> tasks = new ArrayList<>();
    final PlatformInit pm = PlatformInit.getInstance();
    for (final Extension ei : extensions) {
      final Extension fei = ei;
      // Initialize rest of them in parallel.
      final JoinableTask task = new JoinableTask() {
        @Override
        public void execute() {
          try {
            fei.initClassLoader(librariesCL);
          } catch (final Exception err) {
            logger.log(Level.WARNING, "Unable to create class loader for " + fei + ".", err);
          }
        }

        @Override
        public String toString() {
          return "createExtensions:" + fei;
        }
      };
      tasks.add(task);
      pm.scheduleTask(task);
    }

    // Join tasks to make sure all extensions are initialized at this point.
    for (final JoinableTask task : tasks) {
      try {
        task.join();
      } catch (final InterruptedException ie) {
        // TODO
        // ignore
      }
    }
  }

  private void addAllFileExtensions(final File[] extRoots) {
    for (final File file : extRoots) {
      try {
        this.addExtension(file);
      } catch (final IOException ioe) {
        logger.log(Level.WARNING, "createExtensions(): Unable to load '" + file + "'.", ioe);
      }
    }
  }

  /*
  public ClassLoader getClassLoader(final String extensionId) {
    final Extension ei = this.extensionById.get(extensionId);
    if (ei != null) {
      return ei.getClassLoader();
    } else {
      return null;
    }
  }*/

  public void initExtensions() {
    final Collection<JoinableTask> tasks = new ArrayList<>();
    final PlatformInit pm = PlatformInit.getInstance();
    for (final Extension ei : this.extensions) {
      final JoinableTask task = new JoinableTask() {
        @Override
        public void execute() {
          ei.initExtension();
        }

        @Override
        public String toString() {
          return "initExtensions:" + ei;
        }
      };
      tasks.add(task);
      pm.scheduleTask(task);
    }
    // Join all tasks before returning
    for (final JoinableTask task : tasks) {
      try {
        task.join();
      } catch (final InterruptedException ie) {
        // ignore
      }
    }
  }

  public void initExtensionsWindow(final NavigatorWindow context) {
    // This must be done sequentially due to menu lookup infrastructure.
    for (final Extension ei : this.extensions) {
      try {
        ei.initExtensionWindow(context);
      } catch (final Exception err) {
        logger.log(Level.SEVERE, "initExtensionsWindow(): Extension could not properly initialize a new window.", err);
      }
    }
  }

  public void shutdownExtensionsWindow(final NavigatorWindow context) {
    // This must be done sequentially due to menu lookup infrastructure.
    for (final Extension ei : this.extensions) {
      try {
        ei.shutdownExtensionWindow(context);
      } catch (final Exception err) {
        logger.log(Level.SEVERE, "initExtensionsWindow(): Extension could not properly process window shutdown.", err);
      }
    }
  }

  public Clientlet getClientlet(final ClientletRequest request, final ClientletResponse response) {
    final Collection<Extension> extensions = this.extensions;
    // Call all plugins once to see if they can select the response.
    for (final Extension ei : extensions) {
      try {
        final Clientlet clientlet = ei.getClientlet(request, response);
        if (clientlet != null) {
          return clientlet;
        }
      } catch (final Exception thrown) {
        logger.log(Level.SEVERE, "getClientlet(): Extension " + ei + " threw exception.", thrown);
      }
    }

    // None handled it. Call the last resort handlers in reverse order.
    for (final Extension ei : org.lobobrowser.util.CollectionUtilities.reverse(extensions)) {
      try {
        final Clientlet clientlet = ei.getLastResortClientlet(request, response);
        if (clientlet != null) {
          return clientlet;
        }
      } catch (final Exception thrown) {
        logger.log(Level.SEVERE, "getClientlet(): Extension " + ei + " threw exception.", thrown);
      }
    }
    return null;
  }

  public void handleError(final NavigatorFrame frame, final ClientletResponse response, final Throwable exception,
      final RequestType requestType) {
    final NavigatorExceptionEvent event = new NavigatorExceptionEvent(this, NavigatorEventType.ERROR_OCCURRED, frame, response, exception,
        requestType);
    SwingUtilities.invokeLater(() -> {
      final Collection<Extension> ext = extensions;
      // Call all plugins once to see if they can select the response.
      boolean dispatched = false;
      for (final Extension ei : ext) {
        if (ei.handleError(event)) {
          dispatched = true;
        }
      }
      if (!dispatched && logger.isLoggable(Level.INFO)) {
        logger.log(Level.WARNING, "No error handlers found for error that occurred while processing response=[" + response + "].",
            exception);
      }
    });
  }

  public void dispatchBeforeNavigate(final NavigationEvent event) throws NavigationVetoException {
    for (final Extension ei : extensions) {
      try {
        ei.dispatchBeforeLocalNavigate(event);
      } catch (final NavigationVetoException nve) {
        throw nve;
      } catch (final Exception other) {
        logger.log(Level.SEVERE, "dispatchBeforeNavigate(): Extension threw an unexpected exception.", other);
      }
    }
  }

  public void dispatchBeforeLocalNavigate(final NavigationEvent event) throws NavigationVetoException {
    for (final Extension ei : extensions) {
      try {
        ei.dispatchBeforeLocalNavigate(event);
      } catch (final NavigationVetoException nve) {
        throw nve;
      } catch (final Exception other) {
        logger.log(Level.SEVERE, "dispatchBeforeLocalNavigate(): Extension threw an unexpected exception.", other);
      }
    }
  }

  public void dispatchBeforeWindowOpen(final NavigationEvent event) throws NavigationVetoException {
    for (final Extension ei : extensions) {
      try {
        ei.dispatchBeforeWindowOpen(event);
      } catch (final NavigationVetoException nve) {
        throw nve;
      } catch (final Exception other) {
        logger.log(Level.SEVERE, "dispatchBeforeWindowOpen(): Extension threw an unexpected exception.", other);
      }
    }
  }

  public URLConnection dispatchPreConnection(URLConnection connection) {
    for (final Extension ei : extensions) {
      try {
        connection = ei.dispatchPreConnection(connection);
      } catch (final Exception other) {
        logger.log(Level.SEVERE, "dispatchPreConnection(): Extension threw an unexpected exception.", other);
      }
    }
    return connection;
  }

  public URLConnection dispatchPostConnection(URLConnection connection) {
    for (final Extension ei : extensions) {
      try {
        connection = ei.dispatchPostConnection(connection);
      } catch (final Exception other) {
        logger.log(Level.SEVERE, "dispatchPostConnection(): Extension threw an unexpected exception.", other);
      }
    }
    return connection;
  }

  private static class ExtFileFilter implements FileFilter {
    public boolean accept(final File file) {
      return file.isDirectory() || file.getName().toLowerCase().endsWith(".jar");
    }
  }
}
