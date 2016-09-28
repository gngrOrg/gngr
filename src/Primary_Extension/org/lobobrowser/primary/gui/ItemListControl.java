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
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.eclipse.jdt.annotation.Nullable;

public class ItemListControl<T> extends JComponent {
  private static final long serialVersionUID = 3251022502906426556L;
  private final JComboBox<T> comboBox;
  private final ItemEditorFactory<T> itemEditorFactory;

  public ItemListControl(final ItemEditorFactory<T> ief) {
    this.itemEditorFactory = ief;
    this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    this.comboBox = new JComboBox<>();
    this.comboBox.setPreferredSize(new Dimension(100, 24));
    this.comboBox.setEditable(false);
    final JButton editButton = new JButton();
    editButton.setAction(new EditAction(false));
    editButton.setText("Edit");
    final JButton addButton = new JButton();
    addButton.setAction(new EditAction(true));
    addButton.setText("Add");
    final JButton removeButton = new JButton();
    removeButton.setAction(new RemoveAction());
    removeButton.setText("Remove");
    this.add(this.comboBox);
    this.add(editButton);
    this.add(addButton);
    this.add(removeButton);
  }

  public void setItems(final Collection<T> items) {
    final JComboBox<T> comboBox = this.comboBox;
    comboBox.removeAllItems();
    for (final T item : items) {
      comboBox.addItem(item);
    }
  }

  private T getSelectedItem() {
    @SuppressWarnings("unchecked")
    final T selectedItem = (T) this.comboBox.getSelectedItem();
    return selectedItem;
  }

  private void addItem(final T item) {
    this.comboBox.addItem(item);
    this.comboBox.setSelectedItem(item);
  }

  private void replaceSelectedItem(final T item) {
    final int index = this.comboBox.getSelectedIndex();
    if (index != -1) {
      this.comboBox.removeItemAt(index);
    }
    this.comboBox.addItem(item);
    this.comboBox.setSelectedItem(item);
  }

  private void removeSelectedItem() {
    final int index = this.comboBox.getSelectedIndex();
    if (index != -1) {
      this.comboBox.removeItemAt(index);
    }
  }

  public Collection<T> getItems() {
    final Collection<T> items = new ArrayList<>();
    final int count = this.comboBox.getItemCount();
    for (int i = 0; i < count; i++) {
      items.add(this.comboBox.getItemAt(i));
    }
    return items;
  }

  private String editListCaption;

  public void setEditorCaption(final String caption) {
    this.editListCaption = caption;
  }

  private class RemoveAction extends AbstractAction {
    private static final long serialVersionUID = -2348978279945841470L;

    public void actionPerformed(final ActionEvent e) {
      if (JOptionPane.showConfirmDialog(ItemListControl.this, "Are you sure you want to remove the selected item?", "Confirm",
          JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
        removeSelectedItem();
      }
    }
  }

  private class EditAction extends AbstractAction {
    private static final long serialVersionUID = -4963725376330300896L;
    private final boolean add;

    public EditAction(final boolean add) {
      this.add = add;
    }

    public void actionPerformed(final ActionEvent e) {
      final Frame parentFrame = SwingTasks.getFrame(ItemListControl.this);
      ItemEditorDialog<T> dialog;
      if (parentFrame != null) {
        dialog = new ItemEditorDialog<>(parentFrame, itemEditorFactory);
      } else {
        final Dialog parentDialog = SwingTasks.getDialog(ItemListControl.this);
        dialog = new ItemEditorDialog<>(parentDialog, itemEditorFactory);
      }
      dialog.setModal(true);
      dialog.setTitle(this.add ? "Add Item" : "Edit Item");
      dialog.setCaption(editListCaption);
      dialog.pack();
      final Dimension size = dialog.getSize();
      if (size.width > 400) {
        dialog.setSize(new Dimension(400, size.height));
      }
      dialog.setLocationByPlatform(true);
      if (!this.add) {
        dialog.setItem(getSelectedItem());
      }
      dialog.setVisible(true);
      final @Nullable T item = dialog.getResultingItem();
      if (item != null) {
        if (this.add) {
          addItem(item);
        } else {
          replaceSelectedItem(item);
        }
      }
    }
  }
}
