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

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.main.PlatformInit;
import org.lobobrowser.primary.gui.SearchDialog;
import org.lobobrowser.primary.gui.prefs.PreferencesDialog;
import org.lobobrowser.primary.settings.ToolsSettings;
import org.lobobrowser.ua.NavigationEntry;
import org.lobobrowser.ua.NavigatorWindow;
import org.lobobrowser.ua.RequestType;

public class ActionPool {
  private static final Logger logger = Logger.getLogger(ActionPool.class.getName());
  private final ComponentSource componentSource;
  private final NavigatorWindow window;
  private final Collection<EnableableAction> enableableActions;

  public final Action requestManagerAction = new AbstractAction("Req Mgr") {
    private static final long serialVersionUID = -5964377537581420944L;

    {
      putValue(MNEMONIC_KEY, KeyEvent.VK_R);
    }

    public void actionPerformed(final ActionEvent e) {
      window.getTopFrame().manageRequests(e.getSource());
    }
  };

  public final GoAction goAction = new GoAction();
  public final SearchAction searchAction = new SearchAction();
  public final ExitAction exitAction = new ExitAction();
  public final AboutAction aboutAction = new AboutAction();
  public final BackAction backAction = new BackAction();
  public final ForwardAction forwardAction = new ForwardAction();
  public final ReloadAction reloadAction = new ReloadAction();
  public final StopAction stopAction = new StopAction();
  public final CopyAction copyAction = new CopyAction();
  // public final BookmarksAction bookmarksAction = new BookmarksAction();
  public final BackMoreAction backMoreAction = new BackMoreAction();
  public final ForwardMoreAction forwardMoreAction = new ForwardMoreAction();
  public final RecentHostsAction recentHostsAction = new RecentHostsAction();
  public final SourceAction sourceAction = new SourceAction();
  public final ConsoleAction consoleAction = new ConsoleAction();
  public final AddBookmarkAction addBookmarkAction = new AddBookmarkAction();
  public final SearchBookmarksAction searchBookmarksAction = new SearchBookmarksAction();
  public final ShowBookmarksAction showBookmarksAction = new ShowBookmarksAction();
  public final ListExtensionsAction listExtensionsAction = new ListExtensionsAction();
  public final OpenFileAction openFileAction = new OpenFileAction();
  public final BlankWindowAction blankWindowAction = new BlankWindowAction();
  public final ClonedWindowAction clonedWindowAction = new ClonedWindowAction();
  public final PreferencesAction preferencesAction = new PreferencesAction();

  public ActionPool(final ComponentSource componentSource, final NavigatorWindow window) {
    super();
    this.componentSource = componentSource;
    this.window = window;
    final Collection<EnableableAction> actions = new LinkedList<>();
    this.enableableActions = actions;
    actions.add(this.backAction);
    actions.add(this.forwardAction);
    actions.add(this.reloadAction);
    actions.add(this.backMoreAction);
    actions.add(this.forwardMoreAction);
    actions.add(this.recentHostsAction);
    actions.add(this.sourceAction);
  }

  public void updateEnabling() {
    for (final EnableableAction action : this.enableableActions) {
      action.updateEnabling();
    }
  }

  public Action createNavigateAction(final String fullURL) {
    try {
      return new NavigateAction(new URL(fullURL));
    } catch (final java.net.MalformedURLException mfu) {
      logger.log(Level.WARNING, "createNavigateAction()", mfu);
      throw new IllegalStateException(mfu);
    }
  }

  public Action createNavigateAction(final @NonNull URL url) {
    return new NavigateAction(url);
  }

  public Action createBookmarkNavigateAction(final @NonNull URL url) {
    return new BookmarkNavigateAction(url);
  }

  public Action createGoToAction(final NavigationEntry entry) {
    return new GoToAction(entry);
  }

  public Action addUrlPrefixNavigateAction(final String urlPrefix, final boolean urlEncode) {
    final EnableableAction action = new UrlPrefixNavigateAction(urlPrefix, urlEncode);
    this.enableableActions.add(action);
    return action;
  }

  public void addBookmark() {
    final NavigationEntry entry = window.getCurrentNavigationEntry();
    if (entry != null) {
      final java.net.URL url = entry.getUrl();
      final BookmarksHistory history = BookmarksHistory.getInstance();
      BookmarkInfo existingInfo = history.getExistingInfo(url.toExternalForm());
      if (existingInfo == null) {
        existingInfo = new BookmarkInfo();
        existingInfo.setUrl(url);
        existingInfo.setTitle(entry.getTitle());
        existingInfo.setDescription(entry.getDescription());
      }
      final java.awt.Window awtWindow = window.getAwtWindow();
      if (!(awtWindow instanceof java.awt.Frame)) {
        throw new java.lang.IllegalStateException("Bookmaks dialog only supported when an AWT Frame is available.");
      }
      final AddBookmarkDialog dialog = new AddBookmarkDialog((java.awt.Frame) awtWindow, true, existingInfo);
      dialog.setTitle("Add/Edit Bookmark");
      dialog.setLocationByPlatform(true);
      // dialog.setLocationRelativeTo(window.getAwtFrame());
      dialog.setResizable(false);
      dialog.pack();
      dialog.setVisible(true);
      final BookmarkInfo info = dialog.getBookmarkInfo();
      if (info != null) {
        history.addAsRecent(info.getUrl(), info);
        history.save();
      }
    }
  }

  public void searchBookmarks() {
    final java.awt.Window awtWindow = window.getAwtWindow();
    if (!(awtWindow instanceof java.awt.Frame)) {
      throw new java.lang.IllegalStateException("Search dialog only supported when an AWT Frame is available.");
    }
    final SearchDialog dialog = new SearchDialog((java.awt.Frame) awtWindow, true,
        "Keywords will be matched against URL, title, description and tags.");
    dialog.setTitle("Search Bookmarks");
    dialog.setLocationByPlatform(true);
    dialog.setResizable(false);
    dialog.setSize(new java.awt.Dimension(200, 100));
    dialog.setVisible(true);
    final String keywordsText = dialog.getSearchKeywords();
    if (keywordsText != null) {
      try {
        window.getTopFrame().navigate("about:bookmark-search?" + java.net.URLEncoder.encode(keywordsText, "UTF-8"));
      } catch (final Exception thrown) {
        throw new IllegalStateException("not expected", thrown);
      }
    }
  }

  public void showPreferences() {
    final java.awt.Window awtWindow = window.getAwtWindow();
    if (!(awtWindow instanceof java.awt.Frame)) {
      throw new java.lang.IllegalStateException("Preferences dialog only supported when an AWT Frame is available.");
    }
    final PreferencesDialog dialog = new PreferencesDialog((java.awt.Frame) awtWindow);
    dialog.setTitle("Preferences");
    dialog.setLocationByPlatform(true);
    dialog.setSize(new java.awt.Dimension(600, 400));
    dialog.setVisible(true);
  }

  abstract class EnableableAction extends AbstractAction {
    private static final long serialVersionUID = -8020235092808317096L;

    public abstract void updateEnabling();
  }

  class UrlPrefixNavigateAction extends EnableableAction {
    private static final long serialVersionUID = 1891756423914184152L;
    private final String urlPrefix;
    private final boolean urlEncode;

    public UrlPrefixNavigateAction(final String urlPrefix, final boolean urlEncode) {
      super();
      this.urlPrefix = urlPrefix;
      this.urlEncode = urlEncode;
    }

    @Override
    public void updateEnabling() {
      final NavigationEntry entry = window.getCurrentNavigationEntry();
      this.setEnabled((entry != null) && !entry.getUrl().toExternalForm().startsWith(this.urlPrefix));
    }

    public void actionPerformed(final ActionEvent e) {
      final NavigationEntry entry = window.getCurrentNavigationEntry();
      if (entry == null) {
        return;
      }
      try {
        final String roughLocation = this.urlPrefix
            + (this.urlEncode ? URLEncoder.encode(entry.getUrl().toExternalForm(), "UTF-8") : entry.getUrl().toExternalForm());
        componentSource.navigate(roughLocation, RequestType.PROGRAMMATIC);
      } catch (final java.io.UnsupportedEncodingException uee) {
        // not expected - ignore
      }
    }
  }

  class GoAction extends AbstractAction {
    private static final long serialVersionUID = -1290364681806794717L;

    public void actionPerformed(final ActionEvent e) {
      componentSource.go();
    }
  }

  class SearchAction extends AbstractAction {
    private static final long serialVersionUID = -736644335817165661L;

    public void actionPerformed(final ActionEvent e) {
      componentSource.search();
    }
  }

  class ExitAction extends AbstractAction {
    private static final long serialVersionUID = -8690289288514639028L;

    public void actionPerformed(final ActionEvent e) {
      window.dispose();
    }
  }

  class BlankWindowAction extends AbstractAction {
    private static final long serialVersionUID = -2907691954325659197L;

    public void actionPerformed(final ActionEvent e) {
      try {
        window.getTopFrame().open("about:blank");
      } catch (final java.net.MalformedURLException mfu) {
        throw new IllegalStateException("not expected", mfu);
      }
    }
  }

  class ClonedWindowAction extends EnableableAction {
    private static final long serialVersionUID = 7849950571587529087L;

    @Override
    public void updateEnabling() {
      final NavigationEntry entry = window.getCurrentNavigationEntry();
      this.setEnabled((entry != null) && entry.getMethod().equals("GET"));
    }

    public void actionPerformed(final ActionEvent e) {
      final NavigationEntry entry = window.getCurrentNavigationEntry();
      if ((entry != null) && entry.getMethod().equals("GET")) {
        window.getTopFrame().open(entry.getUrl());
      }
    }
  }

  class OpenFileAction extends AbstractAction {
    private static final long serialVersionUID = 2075598538762531969L;

    public void actionPerformed(final ActionEvent e) {
      final JFileChooser fileChooser = new JFileChooser();
      fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      final ToolsSettings settings = ToolsSettings.getInstance();
      final java.io.File directory = settings.getOpenFileDirectory();
      if (directory != null) {
        fileChooser.setSelectedFile(directory);
      }
      final int returnValue = fileChooser.showOpenDialog(window.getTopFrame().getComponent());
      if (returnValue == JFileChooser.APPROVE_OPTION) {
        final java.io.File selectedFile = fileChooser.getSelectedFile();
        componentSource.navigate(selectedFile.toURI().toString(), RequestType.PROGRAMMATIC);
        settings.setOpenFileDirectory(selectedFile);
        settings.save();
      }
    }
  }

  class AboutAction extends AbstractAction {
    private static final long serialVersionUID = 2320751811484772090L;
    final Properties relProps = PlatformInit.getInstance().relProps;
    public void actionPerformed(final ActionEvent e) {
      window.getTopFrame().alert(
          "gngr, a pure java web browser.\r\n"
              + "Version " + relProps.getProperty(PlatformInit.RELEASE_VERSION_STRING) + "\r\n"
              + "Published on: " + relProps.getProperty(PlatformInit.RELEASE_VERSION_RELEASE_DATE) + "\r\n"
              + "copyright (c) 2014  Uproot Labs\r\n"
              + "copyright (c) 2005, 2008 The Lobo Project.\r\n"
              + window.getUserAgent().getInfoUrl());

    }
  }

  class BackAction extends EnableableAction {
    private static final long serialVersionUID = 876416274529605321L;

    @Override
    public void updateEnabling() {
      this.setEnabled(window.canBack());
    }

    public void actionPerformed(final ActionEvent e) {
      window.back();
    }
  }

  class ForwardAction extends EnableableAction {
    private static final long serialVersionUID = 1544065228410858406L;

    @Override
    public void updateEnabling() {
      this.setEnabled(window.canForward());
    }

    public void actionPerformed(final ActionEvent e) {
      window.forward();
    }
  }

  class ReloadAction extends EnableableAction {
    private static final long serialVersionUID = 6950917394367022620L;

    @Override
    public void updateEnabling() {
      this.setEnabled(window.canReload());
    }

    public void actionPerformed(final ActionEvent e) {
      window.reload();
    }
  }

  class StopAction extends AbstractAction {
    private static final long serialVersionUID = -4716824000957477580L;

    public void actionPerformed(final ActionEvent e) {
      window.stop();
    }
  }

  class CopyAction extends EnableableAction {
    private static final long serialVersionUID = -821273079984979618L;

    @Override
    public void updateEnabling() {
      this.setEnabled(window.canCopy());
    }

    public void actionPerformed(final ActionEvent e) {
      window.copy();
    }
  }

  // class BookmarksAction extends AbstractAction {
  // public void actionPerformed(ActionEvent e) {
  // componentSource.populateBookmarks();
  // }
  // }

  class BackMoreAction extends EnableableAction {
    private static final long serialVersionUID = -6775521598700326951L;

    @Override
    public void updateEnabling() {
      this.setEnabled(window.canBack());
    }

    public void actionPerformed(final ActionEvent e) {
      // Only used for enabling
    }
  }

  class ForwardMoreAction extends EnableableAction {
    private static final long serialVersionUID = -495987330645982580L;

    @Override
    public void updateEnabling() {
      this.setEnabled(window.canForward());
    }

    public void actionPerformed(final ActionEvent e) {
      // Only used for enabling
    }
  }

  class RecentHostsAction extends EnableableAction {
    private static final long serialVersionUID = -8163457877234153277L;

    @Override
    public void updateEnabling() {
      this.setEnabled(ComponentSource.hasRecentEntries());
    }

    public void actionPerformed(final ActionEvent e) {
      componentSource.populateRecentHosts();
    }
  }

  class SourceAction extends EnableableAction {
    private static final long serialVersionUID = -4337186074519005342L;

    @Override
    public void updateEnabling() {
      this.setEnabled(window.hasSource());
    }

    public void actionPerformed(final ActionEvent e) {
      componentSource.showSource();
    }
  }

  class ConsoleAction extends AbstractAction {
    private static final long serialVersionUID = -515678211458153749L;

    public void actionPerformed(final ActionEvent e) {
      ComponentSource.showConsole();
    }
  }

  class AddBookmarkAction extends AbstractAction {
    private static final long serialVersionUID = 2056965796680821875L;

    public void actionPerformed(final ActionEvent e) {
      addBookmark();
    }
  }

  class SearchBookmarksAction extends AbstractAction {
    private static final long serialVersionUID = 3227135764906945380L;

    public void actionPerformed(final ActionEvent e) {
      searchBookmarks();
    }
  }

  class ShowBookmarksAction extends AbstractAction {
    private static final long serialVersionUID = -8180169982665670397L;

    public void actionPerformed(final ActionEvent e) {
      try {
        window.getTopFrame().navigate("about:bookmarks");
      } catch (final java.net.MalformedURLException mfu) {
        throw new IllegalStateException("not expected", mfu);
      }
    }
  }

  // class ManageBookmarksAction extends AbstractAction {
  // public void actionPerformed(ActionEvent e) {
  // componentSource.manageBookmarks();
  // }
  // }

  class NavigateAction extends AbstractAction {
    private static final long serialVersionUID = 7673811833299305183L;
    private final @NonNull URL url;

    public NavigateAction(final @NonNull URL url) {
      this.url = url;
    }

    public void actionPerformed(final ActionEvent e) {
      componentSource.navigate(this.url);
    }
  }

  class BookmarkNavigateAction extends AbstractAction {
    private static final long serialVersionUID = -2300129158399659228L;
    private final @NonNull URL url;

    public BookmarkNavigateAction(final @NonNull URL url) {
      this.url = url;
    }

    public void actionPerformed(final ActionEvent e) {
      BookmarksHistory.getInstance().touch(this.url);
      componentSource.navigate(this.url);
    }
  }

  class GoToAction extends AbstractAction {
    private static final long serialVersionUID = -2171834516485737367L;
    private final NavigationEntry entry;

    public GoToAction(final NavigationEntry entry) {
      this.entry = entry;
    }

    public void actionPerformed(final ActionEvent e) {
      window.goTo(this.entry);
    }
  }

  class ListExtensionsAction extends AbstractAction {
    private static final long serialVersionUID = 7111881447756445698L;

    public void actionPerformed(final ActionEvent e) {
      // TODO
    }
  }

  class PreferencesAction extends AbstractAction {
    private static final long serialVersionUID = -628816577052900379L;

    public void actionPerformed(final ActionEvent e) {
      showPreferences();
    }
  }
}
