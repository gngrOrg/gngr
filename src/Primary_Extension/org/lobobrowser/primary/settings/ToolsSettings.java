package org.lobobrowser.primary.settings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lobobrowser.store.StorageManager;

public class ToolsSettings implements Serializable {
  private static final Logger logger = Logger.getLogger(ToolsSettings.class.getName());
  private static final ToolsSettings instance;
  private static final long serialVersionUID = 22574500006000800L;

  private Collection<SearchEngine> searchEngines;
  private SearchEngine selectedSearchEngine;
  private java.io.File downloadDirectory;
  private java.io.File openFileDirectory;

  static {
    ToolsSettings ins = null;
    try {
      ins = (ToolsSettings) StorageManager.getInstance().retrieveSettings(ToolsSettings.class.getSimpleName(),
          ToolsSettings.class.getClassLoader());
    } catch (final Exception err) {
      logger.log(Level.WARNING, "getInstance(): Unable to retrieve settings.", err);
    }
    if (ins == null) {
      ins = new ToolsSettings();
    }
    instance = ins;
  }

  private ToolsSettings() {
    this.restoreDefaults();
  }

  public void restoreDefaults() {
    final List<SearchEngine> searchEngines = getDefaultSearchEngines();
    this.searchEngines = searchEngines;
    this.selectedSearchEngine = searchEngines.get(0);
    /*
    final String userHome = System.getProperty("user.home");
    final File userHomeDirectory = new File(userHome);
    this.downloadDirectory = new File(userHomeDirectory, "download");
    if (!this.downloadDirectory.exists()) {
      this.downloadDirectory.mkdirs();
    }*/
  }

  public static ToolsSettings getInstance() {
    return instance;
  }

  private static List<SearchEngine> getDefaultSearchEngines() {
    final List<SearchEngine> searchEngines = new ArrayList<>();
    searchEngines.add(duckDuckGoSearch());
    searchEngines.add(wikipediaSearch());
    searchEngines.add(googleWebSearch());
    searchEngines.add(yahooWebSearch());
    return searchEngines;
  }

  private static SearchEngine googleWebSearch() {
    return new SearchEngine("Google Web Search", "Google's main search engine.", "https://google.com/search", "q");
  }

  private static SearchEngine yahooWebSearch() {
    return new SearchEngine("Yahoo! Web Search", "Yahoo's web search engine.", "https://search.yahoo.com/search", "p");
  }

  private static SearchEngine duckDuckGoSearch() {
    return new SearchEngine("Duck Duck Go", "Duck Duck Go.", "https://duckduckgo.com/html", "q");
  }

  private static SearchEngine wikipediaSearch() {
    return new SearchEngine("Wikipedia", "English Wikipedia article search.", "https://en.wikipedia.org/wiki/Special:Search", "search");
  }

  public void save() {
    try {
      StorageManager.getInstance().saveSettings(this.getClass().getSimpleName(), this);
    } catch (final java.io.IOException ioe) {
      logger.log(Level.WARNING, "Unable to save settings: " + this.getClass().getSimpleName() + ".", ioe);
    }
  }

  public Collection<SearchEngine> getSearchEngines() {
    return this.searchEngines;
  }

  public void setSearchEngines(final Collection<SearchEngine> searchEngines) {
    this.searchEngines = searchEngines;
  }

  public SearchEngine getSelectedSearchEngine() {
    return selectedSearchEngine;
  }

  public void setSelectedSearchEngine(final SearchEngine selectedSearchEngine) {
    this.selectedSearchEngine = selectedSearchEngine;
  }

  public java.io.File getDownloadDirectory() {
    return downloadDirectory;
  }

  public void setDownloadDirectory(final java.io.File downloadDirectory) {
    this.downloadDirectory = downloadDirectory;
  }

  public java.io.File getOpenFileDirectory() {
    return openFileDirectory;
  }

  public void setOpenFileDirectory(final java.io.File openFileDirectory) {
    this.openFileDirectory = openFileDirectory;
  }
}
