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
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.lobobrowser.primary.gui.StringListControl;
import org.lobobrowser.primary.gui.SwingTasks;
import org.lobobrowser.settings.GeneralSettings;

public class GeneralSettingsUI extends AbstractSettingsUI {
  private static final long serialVersionUID = -3806746723795038605L;
  private final GeneralSettings settings = GeneralSettings.getInstance();
  /*
  private final FormField ieVersionField;
  private final FormField mozillaVersionField;
  private final CheckBoxPanel ieSpoofPanel;
  private final FormPanel mozPanel;
  private final FormPanel iePanel;
   */
  private final StringListControl startupPagesStringListControl;

  public GeneralSettingsUI() {
    /*
    this.ieVersionField = new FormField(FieldType.TEXT, "MSIE Version:");
    this.mozillaVersionField = new FormField(FieldType.TEXT, "Mozilla Version:");
    this.mozillaVersionField.setToolTip("Mozilla compatibility version.");
    final FormPanel iePanel = new FormPanel();
    this.iePanel = iePanel;
    iePanel.addField(this.ieVersionField);
    iePanel.setBorder(new EmptyBorder(1, 8, 8, 0));
    this.ieSpoofPanel = new CheckBoxPanel("Include \"MSIE\" in User-Agent header.", iePanel);
    this.mozPanel = new FormPanel();
    mozPanel.addField(this.mozillaVersionField);
     */
    this.startupPagesStringListControl = new StringListControl();
    this.startupPagesStringListControl.setEditListCaption("You may provide up to " + MAX_STARTUP_PAGES + " startup URLs, one per line.");
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    this.add(this.getStartupGroupBox());

    this.add(SwingTasks.createVerticalFill());
    this.add(new JLabel(
        "<html><p><b>Note</b>: The startup setting is currently ignored and defaults to <b><i>about:welcome</i></b>.</p>" +
            "<br/>" +
            "<p>The welcome page has important warnings about this release and we would like the user to be aware of them.</p>" +
            "<br/>" +
            "<p>We will enable this setting in a future release.</p></html>"));

    this.add(Box.createRigidArea(new Dimension(8, 8)));
    // this.add(this.getUserAgentGroupBox());
    this.add(SwingTasks.createVerticalFill());
    this.loadSettings();
    // this.ieSpoofPanel.updateEnabling();
  }

  private static final int MAX_STARTUP_PAGES = 4;

  private Component getStartupGroupBox() {
    final Box startupGroupBox = new Box(BoxLayout.Y_AXIS);
    startupGroupBox.setBorder(new TitledBorder(new EtchedBorder(), "Startup"));
    final Box startupPagesBox = new Box(BoxLayout.X_AXIS);
    final JLabel pagesLabel = new JLabel("Pages:");
    pagesLabel.setToolTipText("Up to " + MAX_STARTUP_PAGES + " pages launched when you first run the browser.");
    startupPagesBox.add(pagesLabel);
    startupPagesBox.add(this.startupPagesStringListControl);
    startupGroupBox.add(startupPagesBox);
    return startupGroupBox;
  }

  /*
  private Component getUserAgentGroupBox() {
    final JPanel groupBox = new JPanel();
    groupBox.setPreferredSize(new Dimension(400, 100));
    groupBox.setLayout(new BoxLayout(groupBox, BoxLayout.Y_AXIS));
    groupBox.setBorder(new TitledBorder(new EtchedBorder(), "User Agent"));
    groupBox.add(this.getIECheckBoxPanel());
    groupBox.add(this.getMozVersionPanel());
    return groupBox;
  }

  private Component getIECheckBoxPanel() {
    return this.ieSpoofPanel;
  }

  private Component getMozVersionPanel() {
    return this.mozPanel;
  }
   */

  @Override
  public void restoreDefaults() {
    this.settings.restoreDefaults();
    this.loadSettings();
  }

  @Override
  public void save() {
    final GeneralSettings settings = this.settings;
    /*
    settings.setSpoofIE(this.ieSpoofPanel.isSelected());
    settings.setIeVersion(this.ieVersionField.getValue());
    settings.setMozVersion(this.mozillaVersionField.getValue());
     */
    settings.setStartupURLs(this.startupPagesStringListControl.getStrings());
    settings.save();
  }

  private void loadSettings() {
    final GeneralSettings settings = this.settings;
    /*
    this.ieSpoofPanel.setSelected(settings.isSpoofIE());
    this.ieVersionField.setValue(settings.getIeVersion());
    this.mozillaVersionField.setValue(settings.getMozVersion());
    this.mozPanel.revalidate();
    this.iePanel.revalidate(); */
    this.startupPagesStringListControl.setStrings(settings.getStartupURLs());
  }
}
