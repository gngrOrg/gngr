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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class SearchDialog extends JDialog {
  private static final long serialVersionUID = -781810905634470419L;
  private final FormField tagsField = new FormField(FieldType.TEXT, "Keywords:");

  public SearchDialog(final Frame owner, final boolean modal, final String keywordsTooltip) throws HeadlessException {
    super(owner, modal);
    this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    this.tagsField.setToolTip(keywordsTooltip);
    final Container contentPane = this.getContentPane();
    contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
    final FormPanel fieldsPanel = new FormPanel();
    fieldsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    fieldsPanel.addField(this.tagsField);
    contentPane.add(fieldsPanel);
    final JComponent buttonsPanel = new JPanel();
    buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
    final JButton okButton = new JButton();
    okButton.setAction(new OkAction());
    okButton.setText("Search");
    final JButton cancelButton = new JButton();
    cancelButton.setAction(new CancelAction());
    cancelButton.setText("Cancel");
    buttonsPanel.add(Box.createHorizontalGlue());
    buttonsPanel.add(okButton);
    buttonsPanel.add(Box.createRigidArea(new Dimension(4, 1)));
    buttonsPanel.add(cancelButton);
    buttonsPanel.add(Box.createHorizontalGlue());
    contentPane.add(buttonsPanel);
    contentPane.add(Box.createRigidArea(new Dimension(1, 4)));
  }

  private String searchKeywords = null;

  public String getSearchKeywords() {
    return this.searchKeywords;
  }

  private class OkAction extends AbstractAction {
    private static final long serialVersionUID = 8109665074729144586L;

    public void actionPerformed(final ActionEvent e) {
      searchKeywords = tagsField.getValue();
      SearchDialog.this.dispose();
    }
  }

  private class CancelAction extends AbstractAction {
    private static final long serialVersionUID = 4533295115864604199L;

    public void actionPerformed(final ActionEvent e) {
      searchKeywords = null;
      SearchDialog.this.dispose();
    }
  }
}
