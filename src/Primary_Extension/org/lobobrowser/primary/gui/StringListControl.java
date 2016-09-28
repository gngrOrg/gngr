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
package org.lobobrowser.primary.gui;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;

public class StringListControl extends JComponent {
  private static final long serialVersionUID = -2386603668956131744L;
  private final JComboBox<String> comboBox;

  public StringListControl() {
    this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    this.comboBox = new JComboBox<>();
    this.comboBox.setEditable(false);
    final JButton editButton = new JButton();
    editButton.setAction(new EditAction());
    editButton.setText("Edit List");
    this.add(this.comboBox);
    this.add(editButton);
  }

  private String[] strings;

  public void setStrings(final String[] strings) {
    this.strings = strings;
    final JComboBox<String> comboBox = this.comboBox;
    comboBox.removeAllItems();
    for (final String string : strings) {
      comboBox.addItem(string);
    }
  }

  public String[] getStrings() {
    return this.strings;
  }

  public String getStringsAsText() {
    final String lineSeparator = System.getProperty("line.separator");
    final String[] strings = this.strings;
    if (strings == null) {
      return null;
    }
    final StringBuffer buffer = new StringBuffer();
    for (final String string : strings) {
      buffer.append(string);
      buffer.append(lineSeparator);
    }
    return buffer.toString();
  }

  public void setStringsFromText(final String text) {
    try {
      final BufferedReader reader = new BufferedReader(new StringReader(text));
      String line;
      final ArrayList<String> stringsAL = new ArrayList<>();
      while ((line = reader.readLine()) != null) {
        stringsAL.add(line);
      }
      this.setStrings(stringsAL.toArray(new String[0]));
    } catch (final java.io.IOException ioe) {
      throw new IllegalStateException("not expected", ioe);
    }
  }

  private String editListCaption;

  public void setEditListCaption(final String caption) {
    this.editListCaption = caption;
  }

  private class EditAction extends AbstractAction {
    private static final long serialVersionUID = -3588446306085995091L;

    public void actionPerformed(final ActionEvent e) {
      final Frame parentFrame = SwingTasks.getFrame(StringListControl.this);
      SimpleTextEditDialog dialog;
      if (parentFrame != null) {
        dialog = new SimpleTextEditDialog(parentFrame);
      } else {
        final Dialog parentDialog = SwingTasks.getDialog(StringListControl.this);
        dialog = new SimpleTextEditDialog(parentDialog);
      }
      dialog.setModal(true);
      dialog.setTitle("Edit List");
      dialog.setCaption(editListCaption);
      dialog.setSize(new Dimension(400, 300));
      dialog.setLocationByPlatform(true);
      dialog.setText(getStringsAsText());
      dialog.setVisible(true);
      final String text = dialog.getResultingText();
      if (text != null) {
        setStringsFromText(text);
      }
    }
  }
}
