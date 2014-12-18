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

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

public class FormField {
  public final FieldType type;

  public FormField(final FieldType type) {
    this(FieldType.TEXT, "");
  }

  public FormField(final FieldType type, final String caption) {
    this(type, caption, true);
  }

  public FormField(final FieldType type, final String caption, final boolean editable) {
    this.type = type;
    this.setCaption(caption);
    this.setEditable(editable);
  }

  public FormField(final FieldType type, final boolean editable) {
    this.type = type;
    this.setEditable(editable);
  }

  public String getCaption() {
    return this.getLabel().getText();
  }

  public boolean isEditable() {
    final JComponent fe = this.getFieldEditor();
    if (fe instanceof JTextComponent) {
      return ((JTextComponent) fe).isEditable();
    } else {
      return false;
    }
  }

  public FieldType getType() {
    return type;
  }

  public String getValue() {
    final JComponent fe = this.getFieldEditor();
    if (fe instanceof JTextComponent) {
      return ((JTextComponent) fe).getText();
    } else {
      return null;
    }
  }

  public void setValue(final String value) {
    final JComponent fe = this.getFieldEditor();
    if (fe instanceof JTextComponent) {
      ((JTextComponent) fe).setText(value);
    }
  }

  public String getToolTip() {
    return this.getFieldEditor().getToolTipText();
  }

  public void setToolTip(final String tooltip) {
    this.getFieldEditor().setToolTipText(tooltip);
  }

  public void setEditable(final boolean editable) {
    final JComponent fe = this.getFieldEditor();
    if (fe instanceof JTextComponent) {
      ((JTextComponent) fe).setEditable(editable);
    }
  }

  public void setCaption(final String caption) {
    this.getLabel().setText(caption);
  }

  private JLabel label;

  public JLabel getLabel() {
    JLabel label = this.label;
    if (label != null) {
      return label;
    }
    label = new JLabel();
    this.label = label;
    return label;
  }

  private JComponent fieldEditor;

  public JComponent getFieldEditor() {
    JComponent fe = this.fieldEditor;
    if (fe != null) {
      return fe;
    }
    switch (this.type) {
    case TEXT:
      final JTextField textField = new JTextField();
      fe = textField;
      break;
    case PASSWORD:
      final JPasswordField pwdField = new JPasswordField();
      pwdField.setEchoChar('*');
      fe = pwdField;
      break;
    default:
      throw new IllegalArgumentException("type=" + this.type);
    }
    this.fieldEditor = fe;
    return fe;
  }
}
