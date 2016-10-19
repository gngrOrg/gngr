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
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import javax.swing.ComboBoxEditor;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/*
 * Created on Jun 6, 2005
 */

/**
 * @author J. H. S.
 */
public class TextFieldComboBoxEditor implements ComboBoxEditor {
  private final JTextField textField;
  private boolean inNotification = false;

  public TextFieldComboBoxEditor() {
    this.textField = new JTextField();
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.swing.ComboBoxEditor#getEditorComponent()
   */
  public Component getEditorComponent() {
    return this.textField;
  }

  // private Object item;

  /*
   * (non-Javadoc)
   *
   * @see javax.swing.ComboBoxEditor#setItem(java.lang.Object)
   */
  public void setItem(final Object arg0) {
    // this.item = arg0;
    if (!this.inNotification) {
      this.textField.setText(arg0 == null ? "" : String.valueOf(arg0));
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.swing.ComboBoxEditor#getItem()
   */
  public Object getItem() {
    return this.textField.getText();
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.swing.ComboBoxEditor#selectAll()
   */
  public void selectAll() {
    this.textField.selectAll();
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * javax.swing.ComboBoxEditor#addActionListener(java.awt.event.ActionListener)
   */
  public void addActionListener(final ActionListener arg0) {
    this.textField.addActionListener(arg0);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * javax.swing.ComboBoxEditor#removeActionListener(java.awt.event.ActionListener
   * )
   */
  public void removeActionListener(final ActionListener arg0) {
    this.textField.removeActionListener(arg0);
  }

  public void addKeyListener(final java.awt.event.KeyListener listener) {
    this.textField.addKeyListener(listener);
  }

  public void addMouseListener(MouseListener listener) {
    this.textField.addMouseListener(listener);
  }

  public void removeKeyListener(final java.awt.event.KeyListener listener) {
    this.textField.removeKeyListener(listener);
  }

  public void addChangeListener(final ChangeListener listener) {
    this.textField.getDocument().addDocumentListener(new DocumentListener() {
      public void changedUpdate(final DocumentEvent e) {
        TextFieldComboBoxEditor.this.inNotification = true;
        try {
          listener.stateChanged(new ChangeEvent(TextFieldComboBoxEditor.this));
        } finally {
          TextFieldComboBoxEditor.this.inNotification = false;
        }
      }

      public void insertUpdate(final DocumentEvent e) {
        TextFieldComboBoxEditor.this.inNotification = true;
        try {
          listener.stateChanged(new ChangeEvent(TextFieldComboBoxEditor.this));
        } finally {
          TextFieldComboBoxEditor.this.inNotification = false;
        }
      }

      public void removeUpdate(final DocumentEvent e) {
        TextFieldComboBoxEditor.this.inNotification = true;
        try {
          listener.stateChanged(new ChangeEvent(TextFieldComboBoxEditor.this));
        } finally {
          TextFieldComboBoxEditor.this.inNotification = false;
        }
      }
    });
  }

}
