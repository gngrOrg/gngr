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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;

import org.eclipse.jdt.annotation.Nullable;

public class ItemEditorDialog<T> extends JDialog {
  private static final long serialVersionUID = -5624516092024297352L;
  private final JLabel captionLabel = new JLabel();
  private final JButton okButton = new JButton();
  private final JButton cancelButton = new JButton();
  private final AbstractItemEditor<T> editor;

  public ItemEditorDialog(final Dialog owner, final ItemEditorFactory<T> factory) throws HeadlessException {
    super(owner);
    this.editor = factory.createItemEditor();
    this.init();
  }

  public ItemEditorDialog(final Frame owner, final ItemEditorFactory<T> factory) throws HeadlessException {
    super(owner);
    this.editor = factory.createItemEditor();
    this.init();
  }

  private void init() {
    this.captionLabel.setPreferredSize(new Dimension(Short.MAX_VALUE, 32));
    this.captionLabel.setAlignmentX(0.0f);
    this.captionLabel.setBorder(new EmptyBorder(8, 0, 8, 0));
    this.okButton.setAction(new OkAction());
    this.okButton.setText("OK");
    this.cancelButton.setAction(new CancelAction());
    this.cancelButton.setText("Cancel");

    // this.editor.setBorder(new BevelBorder(BevelBorder.RAISED));

    final Box rootBox = new Box(BoxLayout.Y_AXIS);
    rootBox.setBorder(new EmptyBorder(4, 4, 4, 4));
    rootBox.add(this.captionLabel);
    rootBox.add(this.editor);
    rootBox.add(this.createButtonPanel());

    final Container contentPane = this.getContentPane();
    contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
    contentPane.add(rootBox);
  }

  public void setCaption(final String caption) {
    this.captionLabel.setText(caption);
  }

  public void setItem(final T item) {
    this.editor.setItem(item);
  }

  private Component createButtonPanel() {
    final Box panel = new Box(BoxLayout.X_AXIS);
    // panel.setBorder(new BevelBorder(BevelBorder.LOWERED));
    panel.setBorder(new EmptyBorder(4, 4, 4, 4));
    panel.add(Box.createGlue());
    panel.add(this.okButton);
    panel.add(Box.createRigidArea(new Dimension(4, 1)));
    panel.add(this.cancelButton);
    panel.add(Box.createGlue());
    return panel;
  }

  private @Nullable T resultingItem;

  public @Nullable T getResultingItem() {
    return this.resultingItem;
  }

  private class OkAction extends AbstractAction {
    private static final long serialVersionUID = -4808011697369530582L;

    public void actionPerformed(final ActionEvent e) {
      try {
        editor.validateItem();
      } catch (final ValidationException ve) {
        JOptionPane.showMessageDialog(ItemEditorDialog.this, ve.getMessage());
        return;
      }
      resultingItem = editor.getItem();
      ItemEditorDialog.this.dispose();
    }
  }

  private class CancelAction extends AbstractAction {
    private static final long serialVersionUID = 3129498045750775250L;

    public void actionPerformed(final ActionEvent e) {
      resultingItem = null;
      ItemEditorDialog.this.dispose();
    }
  }
}
