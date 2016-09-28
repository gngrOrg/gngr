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

import org.lobobrowser.primary.gui.AbstractItemEditor;
import org.lobobrowser.primary.gui.FieldType;
import org.lobobrowser.primary.gui.FormField;
import org.lobobrowser.primary.gui.FormPanel;
import org.lobobrowser.primary.gui.ValidationException;
import org.lobobrowser.primary.settings.SearchEngine;
import org.lobobrowser.util.Strings;
import org.lobobrowser.util.gui.WrapperLayout;

public class SearchEngineEditor extends AbstractItemEditor<SearchEngine> {
  private static final long serialVersionUID = -954134608217263931L;
  private final FormPanel formPanel = new FormPanel();
  private final FormField nameField = new FormField(FieldType.TEXT);
  private final FormField descriptionField = new FormField(FieldType.TEXT);
  private final FormField baseUrlField = new FormField(FieldType.TEXT);
  private final FormField queryParameterField = new FormField(FieldType.TEXT);

  public SearchEngineEditor() {
    this.nameField.setCaption("Name:");
    this.descriptionField.setCaption("Description:");
    this.baseUrlField.setCaption("Base URL:");
    this.baseUrlField.setToolTip("The search URL, excluding the query parameter.");
    this.queryParameterField.setCaption("Query Parameter:");
    this.queryParameterField.setToolTip("The name of the URL query parameter that is assigned the search string.");
    this.formPanel.addField(this.nameField);
    this.formPanel.addField(this.descriptionField);
    this.formPanel.addField(this.baseUrlField);
    this.formPanel.addField(this.queryParameterField);
    this.setLayout(WrapperLayout.getInstance());
    this.add(this.formPanel);
  }

  @Override
  public SearchEngine getItem() {
    return new SearchEngine(this.nameField.getValue(), this.descriptionField.getValue(), this.baseUrlField.getValue(),
        this.queryParameterField.getValue());
  }

  @Override
  public void setItem(final SearchEngine item) {
    this.nameField.setValue(item.getName());
    this.descriptionField.setValue(item.getDescription());
    this.baseUrlField.setValue(item.getBaseUrl());
    this.queryParameterField.setValue(item.getQueryParameter());
    this.formPanel.revalidate();
  }

  @Override
  public void validateItem() throws ValidationException {
    if (Strings.isBlank(this.nameField.getValue()) || Strings.isBlank(this.baseUrlField.getValue())
        || Strings.isBlank(this.queryParameterField.getValue())) {
      throw new ValidationException("Name, base URL and query parameter are required.");
    }
  }

}
