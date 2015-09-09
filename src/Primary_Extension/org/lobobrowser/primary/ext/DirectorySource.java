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

import javax.swing.JMenu;
import javax.swing.JMenuItem;

public class DirectorySource {
  private final ActionPool actionPool;

  DirectorySource(final ActionPool actionPool) {
    this.actionPool = actionPool;
  }

  public JMenu getDirectoryMenu() {
    final JMenu searchMenu = new JMenu("Search");
    searchMenu.setMnemonic('S');
    searchMenu.add(this.getDuckDuckGoSearchMenu());
    searchMenu.add(this.getGoogleSearchMenu());
    searchMenu.add(this.getYahooSearchMenu());

    final JMenu newsMenu = new JMenu("News & Blogs");
    newsMenu.setMnemonic('N');
    newsMenu.add(this.getTechNewsMenu());
    newsMenu.add(this.getYahooNewsMenu());
    newsMenu.add(this.getGoogleNewsMenu());
    newsMenu.add(this.getCnnMenu());
    newsMenu.add(this.getDiggMenu());

    final JMenu infoMenu = new JMenu("Information");
    infoMenu.setMnemonic('I');
    infoMenu.add(this.getWikipediaMenu());
    infoMenu.add(this.getWiktionaryMenu());

    final JMenu softwareMenu = new JMenu("Software");
    softwareMenu.setMnemonic('S');
    softwareMenu.add(this.getGitHubMenu());
    softwareMenu.add(this.getSourceforgeMenu());

    final JMenu menu = new JMenu("Directory");
    menu.setMnemonic('D');
    menu.add(searchMenu);
    menu.add(newsMenu);
    menu.add(infoMenu);
    menu.add(softwareMenu);
    return menu;
  }

  private JMenuItem getDuckDuckGoSearchMenu() {
    return ComponentSource.menuItem("DuckDuckGo", this.actionPool.createNavigateAction("https://duckduckgo.com/html"));
  }

  private JMenuItem getGoogleSearchMenu() {
    return ComponentSource.menuItem("Google", this.actionPool.createNavigateAction("https://google.com"));
  }

  private JMenuItem getYahooSearchMenu() {
    return ComponentSource.menuItem("Yahoo!", this.actionPool.createNavigateAction("https://search.yahoo.com"));
  }

  private JMenuItem getYahooNewsMenu() {
    return ComponentSource.menuItem("Yahoo! News", this.actionPool.createNavigateAction("https://news.yahoo.com"));
  }

  private JMenuItem getGoogleNewsMenu() {
    return ComponentSource.menuItem("Google News", this.actionPool.createNavigateAction("https://news.google.com"));
  }

  private JMenuItem getCnnMenu() {
    return ComponentSource.menuItem("CNN", this.actionPool.createNavigateAction("http://cnn.com"));
  }

  private JMenu getTechNewsMenu() {
    final JMenu menu = new JMenu("Tech News");
    menu.add(ComponentSource.menuItem("Hacker News", this.actionPool.createNavigateAction("https://news.ycombinator.com/news")));
    menu.add(ComponentSource.menuItem("Slashdot", this.actionPool.createNavigateAction("https://slashdot.org")));
    menu.add(ComponentSource.menuItem("LWN", this.actionPool.createNavigateAction("https://lwn.net/")));
    menu.add(ComponentSource.menuItem("DZone", this.actionPool.createNavigateAction("http://dzone.com")));
    return menu;
  }

  private JMenuItem getDiggMenu() {
    return ComponentSource.menuItem("Digg.com", this.actionPool.createNavigateAction("http://digg.com"));
  }

  private JMenuItem getWikipediaMenu() {
    return ComponentSource.menuItem("Wikipedia", this.actionPool.createNavigateAction("https://wikipedia.org"));
  }

  private JMenuItem getWiktionaryMenu() {
    return ComponentSource.menuItem("Wiktionary", this.actionPool.createNavigateAction("https://wiktionary.org"));
  }

  private JMenuItem getSourceforgeMenu() {
    return ComponentSource.menuItem("SourceForge", this.actionPool.createNavigateAction("http://sourceforge.net"));
  }

  private JMenuItem getGitHubMenu() {
    return ComponentSource.menuItem("GitHub", this.actionPool.createNavigateAction("https://github.com"));
  }

}
