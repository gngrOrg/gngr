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
package org.lobobrowser.primary.ext;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.text.DefaultEditorKit;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.gui.ConsoleModel;
import org.lobobrowser.main.OS;
import org.lobobrowser.main.PlatformInit;
import org.lobobrowser.primary.settings.SearchEngine;
import org.lobobrowser.primary.settings.ToolsSettings;
import org.lobobrowser.request.ClientletRequestHandler;
import org.lobobrowser.ua.NavigationEntry;
import org.lobobrowser.ua.NavigatorProgressEvent;
import org.lobobrowser.ua.NavigatorWindow;
import org.lobobrowser.ua.NavigatorWindowEvent;
import org.lobobrowser.ua.NavigatorWindowListener;
import org.lobobrowser.ua.ProgressType;
import org.lobobrowser.ua.RequestType;
import org.lobobrowser.util.Timing;

public class ComponentSource implements NavigatorWindowListener {
  private static final Logger logger = Logger.getLogger(ComponentSource.class.getName());
  private static final int PREFERRED_MAX_MENU_SIZE = 20;

  private final NavigatorWindow window;
  private final AddressField addressField;
  private final JLabel statusMessageComponent;
  private final ProgressBar progressBar;
  private final JMenu recentBookmarksMenu;
  private final JMenu taggedBookmarksMenu;
  private final JMenu backMoreMenu;
  private final JMenu forwardMoreMenu;
  private final JMenu recentHostsMenu;
  private final JMenu searchersMenu;
  private final JButton searchButton;
  private final JButton reqManagerButton;
  private final ActionPool actionPool;
  private final DirectorySource directorySource;  
  
  // Mask for Key Stroke 
  public static final int CMD_CTRL_KEY_MASK = PlatformInit.OS_NAME == OS.MAC ? Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
      : InputEvent.CTRL_MASK;

  public ComponentSource(final NavigatorWindow window) {
    super();
    this.actionPool = new ActionPool(this, window);
    this.directorySource = new DirectorySource(this.actionPool);
    this.window = window;
    this.addressField = new AddressField(this);
    this.progressBar = new ProgressBar();
    this.statusMessageComponent = new JLabel();
    this.searchButton = this.getSearchButton();
    this.reqManagerButton = getRequestManagerButton();
    this.updateSearchButtonTooltip();
    final JMenu bookmarksMenu = new JMenu("Recent Bookmarks");
    this.recentBookmarksMenu = bookmarksMenu;
    bookmarksMenu.setMnemonic('R');
    bookmarksMenu.addMenuListener(new MenuAdapter() {
      @Override
      public void menuSelected(final MenuEvent e) {
        populateRecentBookmarks();
      }
    });
    final JMenu taggedBookmarksMenu = new JMenu("Tagged Bookmarks");
    this.taggedBookmarksMenu = taggedBookmarksMenu;
    taggedBookmarksMenu.setMnemonic('T');
    taggedBookmarksMenu.setToolTipText("Shows up to " + PREFERRED_MAX_MENU_SIZE + " tags with up to " + PREFERRED_MAX_MENU_SIZE
        + " recent bookmarks each.");
    taggedBookmarksMenu.addMenuListener(new MenuAdapter() {
      @Override
      public void menuSelected(final MenuEvent e) {
        populateTaggedBookmarks();
      }
    });
    final JMenu backMoreMenu = new JMenu();
    // BackMoreAction only used for enabling
    backMoreMenu.setAction(actionPool.backMoreAction);
    backMoreMenu.addMenuListener(new MenuAdapter() {
      @Override
      public void menuSelected(final MenuEvent e) {
        populateBackMore();
      }
    });
    this.backMoreMenu = backMoreMenu;
    backMoreMenu.setText("Back To");
    final JMenu forwardMoreMenu = new JMenu();
    // ForwardMoreAction only used for enabling
    forwardMoreMenu.setAction(actionPool.forwardMoreAction);
    forwardMoreMenu.addMenuListener(new MenuAdapter() {
      @Override
      public void menuSelected(final MenuEvent e) {
        populateForwardMore();
      }
    });
    this.forwardMoreMenu = forwardMoreMenu;
    forwardMoreMenu.setText("Forward To");
    final JMenu recentHostsMenu = new JMenu();
    recentHostsMenu.addMenuListener(new MenuAdapter() {
      @Override
      public void menuSelected(final MenuEvent e) {
        populateRecentHosts();
      }
    });
    this.recentHostsMenu = recentHostsMenu;
    recentHostsMenu.setAction(this.actionPool.recentHostsAction);
    recentHostsMenu.setText("Recent Hosts");
    final JMenu searchersMenu = new JMenu();
    searchersMenu.addMenuListener(new MenuAdapter() {
      @Override
      public void menuSelected(final MenuEvent e) {
        populateSearchers();
      }
    });
    this.searchersMenu = searchersMenu;
    searchersMenu.setText("Searching With");
    searchersMenu.setToolTipText("Select the search engine that is used by the Search button in the address bar.");
  }

  public Component[] getAddressBarComponents() {
    return new Component[] { this.getBackButton(), this.window.createGap(), this.getForwardButton(), this.window.createGap(),
        this.getStopButton(), this.window.createGap(), this.getRefreshButton(), this.window.createGap(),
        this.window.createGlueComponent(this.addressField, true), this.window.createGap(), this.getGoButton(), this.window.createGap(),
        this.searchButton, this.window.createGap(), reqManagerButton };
  }

  public Component[] getStatusBarComponents() {
    return new Component[] { this.window.createGap(), this.getStatusMessageComponent(), this.window.createGap(), this.getProgressBar(),
        this.window.createGap() };
  }

  public JMenu getFileMenu() {
    final JMenu openMenu = new JMenu("Open");
    openMenu.setMnemonic('O');
    openMenu.add(menuItem("New Window", 'N', KeyStroke.getKeyStroke(KeyEvent.VK_N, CMD_CTRL_KEY_MASK),
        this.actionPool.blankWindowAction));
    openMenu.add(menuItem("Cloned Window", 'C', this.actionPool.clonedWindowAction));
    final JMenuItem fileMenuItem = menuItem("File...", 'F', KeyStroke.getKeyStroke(KeyEvent.VK_O, CMD_CTRL_KEY_MASK), this.actionPool.openFileAction);
    // TODO enable the menu item once access control UI is implemented
    fileMenuItem.setEnabled(false);
    openMenu.add(fileMenuItem);

    final JMenu menu = new JMenu("File");
    menu.setMnemonic('F');

    menu.add(openMenu);
    menu.addSeparator();
    menu.add(menuItem("Close", 'C', KeyStroke.getKeyStroke(KeyEvent.VK_W, CMD_CTRL_KEY_MASK), this.actionPool.exitAction));

    return menu;
  }

  public JMenu getEditMenu() {
    final JMenu menu = new JMenu("Edit");
    menu.setMnemonic('E');

    menu.add(menuItem("Cut", 'X', KeyStroke.getKeyStroke(KeyEvent.VK_X, CMD_CTRL_KEY_MASK), new DefaultEditorKit.CutAction()));
    menu.add(menuItem("Copy", 'C', KeyStroke.getKeyStroke(KeyEvent.VK_C, CMD_CTRL_KEY_MASK), new DefaultEditorKit.CopyAction()));
    menu.add(menuItem("Paste", 'V', KeyStroke.getKeyStroke(KeyEvent.VK_V, CMD_CTRL_KEY_MASK), new DefaultEditorKit.PasteAction()));

    return menu;
  }

  public JMenu getViewMenu() {
    final JMenu menu = new JMenu("View");
    menu.setMnemonic('V');

    menu.add(menuItem("Page Source", 'S', KeyStroke.getKeyStroke(KeyEvent.VK_U, CMD_CTRL_KEY_MASK), this.actionPool.sourceAction));
    menu.add(menuItem("Console", 'C', this.actionPool.consoleAction));

    return menu;
  }

  public JMenu getBookmarksMenu() {
    final JMenu menu = new JMenu("Bookmarks");
    menu.setMnemonic('B');
    menu.add(menuItem("Add Bookmark", 'A', "ctrl shift a", this.actionPool.addBookmarkAction));
    menu.add(this.recentBookmarksMenu);
    menu.add(this.taggedBookmarksMenu);
    menu.add(menuItem("Search Bookmarks", 'S', this.actionPool.searchBookmarksAction));
    menu.add(menuItem("Show All Bookmarks", 'S', this.actionPool.showBookmarksAction));
    return menu;
  }

  public JMenu getNavigationMenu() {
    final JMenu menu = new JMenu("Navigation");
    menu.setMnemonic('N');

    if (PlatformInit.OS_NAME == OS.MAC) {
      menu.add(menuItem("Back", 'B', KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, CMD_CTRL_KEY_MASK), this.actionPool.backAction));
      menu.add(
          menuItem("Forward", 'F', KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, CMD_CTRL_KEY_MASK), this.actionPool.forwardAction));
      menu.add(menuItem("Stop", 'S', KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), this.actionPool.stopAction));
    } else {
      menu.add(menuItem("Back", 'B', "ctrl B", this.actionPool.backAction));
      menu.add(menuItem("Forward", 'F', this.actionPool.forwardAction));
      menu.add(menuItem("Stop", 'S', KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), this.actionPool.stopAction));
    }

    final JMenuItem reloadMenuItem = menuItem("Reload", 'R', KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), this.actionPool.reloadAction);
    reloadMenuItem.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ctrl R"), "reload action");
    reloadMenuItem.getActionMap().put("reload action", this.actionPool.reloadAction);
    menu.add(reloadMenuItem);

    menu.addSeparator();
    menu.add(this.backMoreMenu);
    menu.add(this.forwardMoreMenu);
    menu.add(this.recentHostsMenu);

    return menu;
  }

  public JMenu getToolsMenu() {
    final JMenu menu = new JMenu("Tools");
    menu.setMnemonic('T');
    menu.add(this.searchersMenu);

    menu.add((PlatformInit.OS_NAME == OS.MAC)
        ? menuItem("Preferences...", 'P', KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, CMD_CTRL_KEY_MASK),
            this.actionPool.preferencesAction)
        : menu.add(menuItem("Preferences...", 'P', this.actionPool.preferencesAction)));

    return menu;

  }

  public JMenu getDirectoryMenu() {
    return this.directorySource.getDirectoryMenu();
  }

  public JMenu getExtensionsMenu() {
    final JMenu menu = new JMenu("Extensions");
    menu.setMnemonic('x');
    menu.add(menuItem("List Extensions", 'L', this.actionPool.listExtensionsAction));
    return menu;
  }

  public JMenu getPageServicesMenu() {
    final JMenu menu = new JMenu("Page Services");
    menu.setMnemonic('P');
    menu.add(this.navMenuItem("Links In (Google)", "Performs a Google 'link' search.", "https://www.google.com/search?q=link:", true));
    menu.add(this.navMenuItem("Similar Pages (Google)", "Performs a Google 'related' search.", "https://www.google.com/search?q=related:",
        true));
    menu.add(this.navMenuItem("Wayback Machine", "Takes you to historical snapshots of the site via archive.org.",
        "https://web.archive.org/web/*/", false));
    return menu;
  }

  public JMenu getHelpMenu() {
    final JMenu menu = new JMenu("Help");
    menu.setMnemonic('H');

    menu.add(menuItem("About gngr", 'A', this.actionPool.aboutAction));
    menu.addSeparator();
    menu.add(menuItem("Project Home Page", this.actionPool.createNavigateAction("https://gngr.info")));

    return menu;
  }

  private Component getBackButton() {
    final JButton button = new JButton();
    button.setAction(this.actionPool.backAction);
    button.setIcon(IconFactory.getInstance().getIcon("/images/back.gif"));
    button.setToolTipText("Back");
    return button;
  }

  private Component getForwardButton() {
    final JButton button = new JButton();
    button.setAction(this.actionPool.forwardAction);
    button.setIcon(IconFactory.getInstance().getIcon("/images/forward.gif"));
    button.setToolTipText("Forward");
    return button;
  }

  private Component getStopButton() {
    final JButton button = new JButton();
    button.setAction(this.actionPool.stopAction);
    button.setIcon(IconFactory.getInstance().getIcon("/images/stop.gif"));
    button.setToolTipText("Stop");
    return button;
  }

  private Component getRefreshButton() {
    final JButton button = new JButton();
    button.setAction(this.actionPool.reloadAction);
    button.setIcon(IconFactory.getInstance().getIcon("/images/refresh.gif"));
    button.setToolTipText("Refresh");
    return button;
  }

  private JButton getRequestManagerButton() {
    final JButton button = new JButton();
    final Action requestManagerAction = this.actionPool.requestManagerAction;
    requestManagerAction.setEnabled(false);
    button.setAction(requestManagerAction);
    button.setToolTipText("Manage requests");
    return button;
  }

  private Component getGoButton() {
    final JButton button = new JButton();
    button.setAction(this.actionPool.goAction);
    button.setIcon(IconFactory.getInstance().getIcon("/images/go.gif"));
    button.setToolTipText("Navigate to URL");
    return button;
  }

  private JButton getSearchButton() {
    final JButton button = new JButton();
    button.setAction(this.actionPool.searchAction);
    button.setIcon(IconFactory.getInstance().getIcon("/images/internet_search.png"));
    return button;
  }

  private void updateSearchButtonTooltip() {
    final JButton button = this.searchButton;
    final ToolsSettings settings = ToolsSettings.getInstance();
    final SearchEngine currentEngine = settings.getSelectedSearchEngine();
    final String name = currentEngine == null ? "[none]" : currentEngine.getName();
    button.setToolTipText("<html><body>Current search engine: " + name + ".</body></html>");
  }

  private Component getStatusMessageComponent() {
    return this.window.createGlueComponent(this.statusMessageComponent, true);
  }

  private Component getProgressBar() {
    return this.progressBar;
  }

  private String defaultStatusMessage;

  public void defaultStatusUpdated(final NavigatorWindowEvent event) {
    final String defaultStatus = event.getMessage();
    this.defaultStatusMessage = event.getMessage();
    if (this.statusMessage == null) {
      this.statusMessageComponent.setText(defaultStatus);
    }
  }

  /**
   * Whether the request should be saved as a recent history entry.
   */
  private static boolean isHistoryRequest(final RequestType requestType) {
    return ((requestType == RequestType.ADDRESS_BAR) || (requestType == RequestType.CLICK));
  }

  public void documentAccessed(final NavigatorWindowEvent event) {
    final java.net.URL url = event.getUrl();

    // TODO: Have a better condition for isManageable, or change requestManager to deal with other protocols as well
    final boolean isManageable = "http".equals(url.getProtocol()) || "https".equals(url.getProtocol());
    reqManagerButton.getAction().setEnabled(isManageable);

    if ("GET".equals(event.getMethod()) && isHistoryRequest(event.getRequestType())) {
      NavigationHistory.getInstance().addAsRecent(url, null);
    }
  }

  public void documentRendering(final NavigatorWindowEvent event) {
    if (logger.isLoggable(Level.INFO)) {
      logger.info("documentRendering(): event=" + event);
    }
    if (this.window.getTopFrame() == event.getNavigatorFrame()) {
      this.clearState();
      this.actionPool.updateEnabling();
    }
  }

  public void setNavigationEntry(final NavigationEntry entry) {
    if (entry != null) {
      if (this.window.getTopFrame() == entry.getNavigatorFrame()) {
        final java.net.URL url = entry.getUrl();
        this.addressField.setUrl(url);
        this.clearState();
        this.actionPool.updateEnabling();
      }
    } else {
      this.clearState();
      this.addressField.setUrl(null);
      this.actionPool.updateEnabling();
    }
  }

  private void clearState() {
    this.statusMessage = null;
    this.defaultStatusMessage = null;
    this.statusMessageComponent.setText("");
  }

  public void progressUpdated(final NavigatorProgressEvent event) {
    if (this.window.getTopFrame() == event.getNavigatorFrame()) {
      this.progressBar.updateProgress(event.getProgressType(), event.getCurrentValue(), event.getMaxValue());

      if (event.getProgressType() == ProgressType.CONNECTING) {
        final java.net.URL url = event.getUrl();
        this.addressField.setUrl(url);
      }
    }
    this.statusMessageComponent.setText(ClientletRequestHandler.getProgressMessage(event.getProgressType(), event.getUrl()));
  }

  private String statusMessage;

  public void statusUpdated(final NavigatorWindowEvent event) {
    final String status = event.getMessage();
    this.statusMessage = status;
    this.statusMessageComponent.setText(status == null ? this.defaultStatusMessage : status);
  }

  public static Collection<String> getPotentialMatches(final String urlPrefix, final int max) {
    final int colonIdx = urlPrefix.indexOf(':');
    String prefix;
    if (colonIdx == -1) {
      // Add http
      prefix = "http://" + urlPrefix;
    } else if (colonIdx == 1) {
      // Must be a Windows file
      prefix = "file://" + urlPrefix;
    } else {
      prefix = urlPrefix;
    }
    Collection<String> headMatches = NavigationHistory.getInstance().getHeadMatchItems(prefix, max);
    if (headMatches.isEmpty()) {
      // Try www
      if ((colonIdx == -1) && !urlPrefix.startsWith("www")) {
        prefix = "http://www." + urlPrefix;
        headMatches = NavigationHistory.getInstance().getHeadMatchItems(prefix, max);
      }
    }
    return headMatches;
  }

  public static Collection<String> getRecentLocations(final int max) {
    return NavigationHistory.getInstance().getRecentItems(max);
  }

  public void navigate(final String roughLocation, final RequestType requestType) {
    try {
      this.window.stop();
      this.clearState();
      this.window.getTopFrame().navigate(roughLocation, requestType);
    } catch (final java.net.MalformedURLException mfu) {
      ExtensionImpl.showError(this.window.getTopFrame(), null, mfu);
    }
  }

  public void navigate(final @NonNull URL url) {
    this.window.stop();
    this.clearState();
    this.window.getTopFrame().navigate(url);
  }

  static JMenuItem menuItem(final String title, final Action action) {
    return menuItem(title, (char) 0, (KeyStroke) null, action);
  }

  static JMenuItem menuItem(final String title, final char mnemonic, final Action action) {
    return menuItem(title, mnemonic, (KeyStroke) null, action);
  }

  static JMenuItem menuItem(final String title, final char mnemonic, final KeyStroke accelerator, final Action action) {
    final JMenuItem item = new JMenuItem();
    item.setAction(action);
    item.setText(title);
    if (mnemonic != 0) {
      item.setMnemonic(mnemonic);
    }
    if (accelerator != null) {
      item.setAccelerator(accelerator);
    }
    return item;
  }

  static JMenuItem menuItem(final String title, final char mnemonic, final String accelerator, final Action action) {
    final KeyStroke keyStroke = accelerator == null ? null : KeyStroke.getKeyStroke(accelerator);
    return menuItem(title, mnemonic, keyStroke, action);
  }

  private JMenuItem navMenuItem(final String title, final String toolTip, final String urlPrefix, final boolean urlEncode) {
    final JMenuItem item = new JMenuItem();
    item.setAction(this.actionPool.addUrlPrefixNavigateAction(urlPrefix, urlEncode));
    item.setText(title);
    item.setToolTipText(toolTip);
    return item;
  }

  public void populateRecentBookmarks() {
    final JMenu bookmarksMenu = this.recentBookmarksMenu;
    bookmarksMenu.removeAll();
    final Collection<HistoryEntry<BookmarkInfo>> historyEntries = BookmarksHistory.getInstance().getRecentEntries(PREFERRED_MAX_MENU_SIZE);
    for (final HistoryEntry<BookmarkInfo> hentry : historyEntries) {
      final BookmarkInfo binfo = hentry.getItemInfo();
      String text = binfo.getTitle();
      final java.net.URL url = binfo.getUrl();
      final String urlText = url.toExternalForm();
      if ((text == null) || (text.length() == 0)) {
        text = urlText;
      }
      final long elapsed = System.currentTimeMillis() - hentry.getTimetstamp();
      text = text + " (" + Timing.getElapsedText(elapsed) + " ago)";
      final Action action = this.actionPool.createBookmarkNavigateAction(url);
      final JMenuItem menuItem = ComponentSource.menuItem(text, action);
      final StringBuffer toolTipText = new StringBuffer();
      toolTipText.append("<html>");
      toolTipText.append(urlText);
      final String description = binfo.getDescription();
      if ((description != null) && (description.length() != 0)) {
        toolTipText.append("<br>");
        toolTipText.append(description);
      }
      menuItem.setToolTipText(toolTipText.toString());
      bookmarksMenu.add(menuItem);
    }
  }

  public void populateTaggedBookmarks() {
    final JMenu bookmarksMenu = this.taggedBookmarksMenu;
    bookmarksMenu.removeAll();
    final Collection<BookmarkInfo> bookmarkInfoList = BookmarksHistory.getInstance().getRecentItemInfo(
        PREFERRED_MAX_MENU_SIZE * PREFERRED_MAX_MENU_SIZE);
    final Map<String, JMenu> tagMenus = new HashMap<>();
    for (final BookmarkInfo binfo : bookmarkInfoList) {
      final java.net.URL url = binfo.getUrl();
      final String urlText = url.toExternalForm();
      final String[] tags = binfo.getTags();
      if (tags != null) {
        for (final String tag : tags) {
          JMenu tagMenu = tagMenus.get(tag);
          if (tagMenu == null) {
            if (tagMenus.size() < PREFERRED_MAX_MENU_SIZE) {
              tagMenu = new JMenu(tag);
              tagMenus.put(tag, tagMenu);
              bookmarksMenu.add(tagMenu);
            }
          }
          if ((tagMenu != null) && (tagMenu.getItemCount() < PREFERRED_MAX_MENU_SIZE)) {
            String text = binfo.getTitle();
            if ((text == null) || (text.length() == 0)) {
              text = urlText;
            }
            final Action action = this.actionPool.createBookmarkNavigateAction(url);
            final JMenuItem menuItem = ComponentSource.menuItem(text, action);
            final StringBuffer toolTipText = new StringBuffer();
            toolTipText.append("<html>");
            toolTipText.append(urlText);
            final String description = binfo.getDescription();
            if ((description != null) && (description.length() != 0)) {
              toolTipText.append("<br>");
              toolTipText.append(description);
            }
            menuItem.setToolTipText(toolTipText.toString());
            tagMenu.add(menuItem);
          }
        }
      }
    }
  }

  public void populateBackMore() {
    final List<NavigationEntry> entries = this.window.getBackNavigationEntries();
    final JMenu backMoreMenu = this.backMoreMenu;
    backMoreMenu.removeAll();
    for (final NavigationEntry entry : entries) {
      final String method = entry.getMethod();
      if ("GET".equals(method)) {
        final String title = entry.getTitle();
        final java.net.URL url = entry.getUrl();
        final String text = (title == null) || (title.length() == 0) ? url.toExternalForm() : title;
        final Action action = this.actionPool.createGoToAction(entry);
        final JMenuItem menuItem = menuItem(text, action);
        menuItem.setToolTipText(url.toExternalForm());
        backMoreMenu.add(menuItem);
      }
    }
    // backMoreMenu.revalidate();
  }

  public void populateForwardMore() {
    final List<NavigationEntry> entries = this.window.getForwardNavigationEntries();
    final JMenu forwardMoreMenu = this.forwardMoreMenu;
    forwardMoreMenu.removeAll();
    for (final NavigationEntry entry : entries) {
      final String method = entry.getMethod();
      if ("GET".equals(method)) {
        final String title = entry.getTitle();
        final java.net.URL url = entry.getUrl();
        final String text = (title == null) || (title.length() == 0) ? url.toExternalForm() : title;
        final Action action = this.actionPool.createGoToAction(entry);
        final JMenuItem menuItem = menuItem(text, action);
        menuItem.setToolTipText(url.toExternalForm());
        forwardMoreMenu.add(menuItem);
      }
    }
  }

  public static boolean hasRecentEntries() {
    return NavigationHistory.getInstance().hasRecentEntries();
  }

  public void populateRecentHosts() {
    final JMenu recentHostsMenu = this.recentHostsMenu;
    recentHostsMenu.removeAll();
    final Collection<HostEntry> hostEntries = NavigationHistory.getInstance().getRecentHostEntries(PREFERRED_MAX_MENU_SIZE);
    for (final HostEntry entry : hostEntries) {
      final String urlText = "http://" + entry.host;
      try {
        final java.net.URL url = new java.net.URL(urlText);
        final long elapsed = System.currentTimeMillis() - entry.timestamp;
        final String menuText = entry.host + " (" + Timing.getElapsedText(elapsed) + " ago)";
        final Action action = this.actionPool.createNavigateAction(url);
        final JMenuItem menuItem = menuItem(menuText, action);
        menuItem.setToolTipText(url.toExternalForm());
        recentHostsMenu.add(menuItem);
      } catch (final java.net.MalformedURLException mfu) {
        logger.log(Level.WARNING, "populateRecentHosts(): Bad URL=" + urlText, mfu);
      }
    }
  }

  public void showSource() {
    String sourceCode = window.getTopFrame().getSourceCode();
    if (sourceCode == null) {
      sourceCode = "";
    }
    final TextViewerWindow window = new TextViewerWindow();
    window.setText(sourceCode);
    window.setSize(new Dimension(600, 400));
    window.setLocationByPlatform(true);
    window.setVisible(true);
  }

  public static void showConsole() {
    final TextViewerWindow window = new TextViewerWindow();
    window.setScrollsOnAppends(true);
    window.setSwingDocument(ConsoleModel.getStandard());
    window.setSize(new Dimension(600, 400));
    window.setLocationByPlatform(true);
    window.setVisible(true);
  }

  public void go() {
    this.navigateOrSearch();
  }

  public void navigateOrSearch() {
    final String addressText = this.addressField.getText().trim();
    if (addressText.charAt(0) == '?') {
      this.search();
    } else {
      final boolean hasPeriod = addressText.indexOf('.') != -1;
      final boolean hasSpace = addressText.indexOf(' ') != -1;
      final boolean hasProtocol = addressText.matches("^[a-z]+:.*");
      if ((!hasProtocol) && ((!hasPeriod) || hasSpace)) {
        try {
          final URL url = new URL("about:confirmSearch?" + addressText);
          this.navigate(url);
        } catch (MalformedURLException e) {
          window.getTopFrame().alert("Malformed search URL.");
        }
      } else {
        this.navigate(addressText, RequestType.ADDRESS_BAR);
      }
    }
  }

  public void search() {
    final ToolsSettings settings = ToolsSettings.getInstance();
    final SearchEngine searchEngine = settings.getSelectedSearchEngine();
    if (searchEngine != null) {
      final String addressText = this.addressField.getText();
      try {
        if (addressText.charAt(0) == '?') {
          assert (addressText.charAt(0) == '?');
          this.navigate(searchEngine.getURL(addressText.substring(1)));
        } else {
        this.navigate(searchEngine.getURL(addressText));
        }
      } catch (final java.net.MalformedURLException mfu) {
        window.getTopFrame().alert("Malformed search URL.");
      }
    }
  }

  private void populateSearchers() {
    final JMenu searchersMenu = this.searchersMenu;
    searchersMenu.removeAll();
    final ToolsSettings settings = ToolsSettings.getInstance();
    final Collection<SearchEngine> searchEngines = settings.getSearchEngines();
    final SearchEngine selectedEngine = settings.getSelectedSearchEngine();
    if (searchEngines != null) {
      for (final SearchEngine se : searchEngines) {
        final SearchEngine finalSe = se;
        final JRadioButtonMenuItem item = new JRadioButtonMenuItem();
        item.setAction(new AbstractAction() {
          private static final long serialVersionUID = -3263394523150719487L;

          public void actionPerformed(final ActionEvent e) {
            settings.setSelectedSearchEngine(finalSe);
            settings.save();
            ComponentSource.this.updateSearchButtonTooltip();
          }
        });
        item.setSelected(se == selectedEngine);
        item.setText(se.getName());
        item.setToolTipText(se.getDescription());
        searchersMenu.add(item);
      }
    }
  }

  public String getAddressBarText() {
    return this.addressField.getText();
  }
}
