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
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

public class SimpleTextEditDialog extends JDialog {
  private static final long serialVersionUID = -5913284205529798401L;
  private final JLabel captionLabel = new JLabel();
  private final JTextArea textArea = new JTextArea();
  private final JButton okButton = new JButton();
  private final JButton cancelButton = new JButton();

  public SimpleTextEditDialog(final Frame parent) {
    super(parent);
    this.init();
  }

  public SimpleTextEditDialog(final Dialog parent) {
    super(parent);
    this.init();
  }

  public void setCaption(final String text) {
    this.captionLabel.setText(text);
  }

  public void setText(final String text) {
    this.textArea.setText(text);
  }

  public String getText() {
    return this.textArea.getText();
  }

  private void init() {
    this.captionLabel.setPreferredSize(new Dimension(Short.MAX_VALUE, 32));
    this.captionLabel.setAlignmentX(0.0f);
    this.captionLabel.setBorder(new EmptyBorder(8, 0, 8, 0));
    this.textArea.setPreferredSize(new Dimension(1, Short.MAX_VALUE));
    final Container contentPane = this.getContentPane();
    contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
    contentPane.add(this.captionLabel);
    contentPane.add(new JScrollPane(this.textArea));
    contentPane.add(this.createButtonPanel());
    this.textArea.setEditable(true);
    this.okButton.setAction(new OkAction());
    this.okButton.setText("OK");
    this.cancelButton.setAction(new CancelAction());
    this.cancelButton.setText("Cancel");
  }

  private Component createButtonPanel() {
    final Box panel = new Box(BoxLayout.X_AXIS);
    panel.setPreferredSize(new Dimension(Short.MAX_VALUE, 0));
    panel.setBorder(new EmptyBorder(4, 4, 4, 4));
    panel.add(Box.createGlue());
    panel.add(this.okButton);
    panel.add(Box.createRigidArea(new Dimension(4, 1)));
    panel.add(this.cancelButton);
    panel.add(Box.createGlue());
    return panel;
  }

  private String resultingText;

  public String getResultingText() {
    return this.resultingText;
  }

  private class OkAction extends AbstractAction {
    private static final long serialVersionUID = -2233622154576810769L;

    public void actionPerformed(final ActionEvent e) {
      resultingText = textArea.getText();
      SimpleTextEditDialog.this.dispose();
    }
  }

  private class CancelAction extends AbstractAction {
    private static final long serialVersionUID = 4935669525318703851L;

    public void actionPerformed(final ActionEvent e) {
      resultingText = null;
      SimpleTextEditDialog.this.dispose();
    }
  }
}