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
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class FormPanel extends JComponent {
  private static final long serialVersionUID = 6987655087065214688L;
  private final Collection<FormField> fields = new ArrayList<>();
  private boolean fieldsInvalid = false;

  public FormPanel() {
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
  }

  public void addField(final FormField field) {
    // Call in GUI thread only.
    this.fields.add(field);
    this.fieldsInvalid = true;
  }

  @Override
  public void revalidate() {
    this.fieldsInvalid = true;
    super.revalidate();
  }

  private int minLabelWidth = 0;

  public int getMinLabelWidth() {
    return minLabelWidth;
  }

  public void setMinLabelWidth(final int minLabelWidth) {
    this.minLabelWidth = minLabelWidth;
  }

  private void populateComponents() {
    this.removeAll();
    int maxWidth = this.minLabelWidth;
    final Collection<JLabel> labels = new ArrayList<>();
    boolean firstTime = true;
    for (final FormField field : this.fields) {
      if (firstTime) {
        firstTime = false;
      } else {
        this.add(Box.createRigidArea(new Dimension(1, 4)));
      }
      final JLabel label = field.getLabel();
      label.setEnabled(this.isEnabled());
      labels.add(label);
      label.setHorizontalAlignment(SwingConstants.RIGHT);
      final String tooltip = field.getToolTip();
      if (tooltip != null) {
        label.setToolTipText(tooltip);
      }
      final Dimension prefSize = label.getPreferredSize();
      if (prefSize.width > maxWidth) {
        maxWidth = prefSize.width;
      }
      final JComponent entryPanel = new JPanel();
      entryPanel.setOpaque(false);
      entryPanel.setLayout(new BoxLayout(entryPanel, BoxLayout.X_AXIS));
      entryPanel.add(label);
      // entryPanel.add(new FillerComponent(label, new Dimension(100, 24), new
      // Dimension(100, 24), new Dimension(100, 24)));
      entryPanel.add(Box.createRigidArea(new Dimension(4, 1)));
      final Component editor = field.getFieldEditor();
      // Dimension eps = editor.getPreferredSize();
      // editor.setPreferredSize(new Dimension(100, eps.height));
      editor.setEnabled(this.isEnabled());
      entryPanel.add(editor);
      final Dimension epps = entryPanel.getPreferredSize();
      entryPanel.setPreferredSize(new Dimension(100, epps.height));
      this.add(entryPanel);
    }
    for (final JLabel label : labels) {
      final Dimension psize = label.getPreferredSize();
      final Dimension newSize = new Dimension(maxWidth, psize.height);
      label.setPreferredSize(newSize);
      label.setMinimumSize(newSize);
      label.setMaximumSize(newSize);
    }
    this.fieldsInvalid = false;
  }

  @Override
  public void doLayout() {
    if (this.fieldsInvalid) {
      this.populateComponents();
    }
    super.doLayout();
  }

  @Override
  public Dimension getPreferredSize() {
    if (this.fieldsInvalid) {
      this.populateComponents();
    }
    return super.getPreferredSize();
  }

  @Override
  public Dimension getMinimumSize() {
    if (this.fieldsInvalid) {
      this.populateComponents();
    }
    return super.getMinimumSize();
  }

  @Override
  public Dimension getMaximumSize() {
    if (this.fieldsInvalid) {
      this.populateComponents();
    }
    return super.getMaximumSize();
  }
}
