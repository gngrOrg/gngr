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
package org.lobobrowser.primary.gui.prefs;

import java.awt.Component;
import java.util.Collection;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;

import org.lobobrowser.primary.gui.AbstractItemEditor;
import org.lobobrowser.primary.gui.ItemEditorFactory;
import org.lobobrowser.primary.gui.ItemListControl;
import org.lobobrowser.primary.gui.SwingTasks;
import org.lobobrowser.primary.settings.SearchEngine;
import org.lobobrowser.primary.settings.ToolsSettings;

public class ToolsSettingsUI extends AbstractSettingsUI {
  private static final long serialVersionUID = -5143806324711270675L;
  private final ToolsSettings settings = ToolsSettings.getInstance();
  private final ItemListControl<SearchEngine> searchEngineListControl;

  public ToolsSettingsUI() {
    final ItemEditorFactory<SearchEngine> factory = new ItemEditorFactory<SearchEngine>() {
      public AbstractItemEditor<SearchEngine> createItemEditor() {
        return new SearchEngineEditor();
      }
    };
    this.searchEngineListControl = new ItemListControl<>(factory);
    this.searchEngineListControl.setEditorCaption("Please enter search engine information below.");
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    this.add(this.getSearchEnginePane());
    this.add(SwingTasks.createVerticalFill());
    this.loadSettings();
  }

  private Component getSearchEnginePane() {
    final Box innerBox = new Box(BoxLayout.X_AXIS);
    innerBox.add(new JLabel("Search Engines:"));
    innerBox.add(this.searchEngineListControl);
    final Box groupBox = SwingTasks.createGroupBox(BoxLayout.Y_AXIS, "Search");
    groupBox.add(innerBox);
    return groupBox;
  }

  @Override
  public void restoreDefaults() {
    this.settings.restoreDefaults();
    this.loadSettings();
  }

  @Override
  public void save() {
    final ToolsSettings settings = this.settings;
    final Collection<SearchEngine> items = this.searchEngineListControl.getItems();
    settings.setSearchEngines(items);
    settings.save();
  }

  private void loadSettings() {
    final ToolsSettings settings = this.settings;
    this.searchEngineListControl.setItems(settings.getSearchEngines());
  }
}
