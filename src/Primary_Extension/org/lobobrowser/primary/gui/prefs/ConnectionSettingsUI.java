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
import java.net.InetSocketAddress;
import java.net.Proxy;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.lobobrowser.primary.gui.FieldType;
import org.lobobrowser.primary.gui.FormField;
import org.lobobrowser.primary.gui.FormPanel;
import org.lobobrowser.primary.gui.SwingTasks;
import org.lobobrowser.primary.gui.ValidationException;
import org.lobobrowser.settings.ConnectionSettings;

public class ConnectionSettingsUI extends AbstractSettingsUI {
  private static final long serialVersionUID = 4456678024374314397L;
  private final ConnectionSettings settings = ConnectionSettings.getInstance();
  private final JRadioButton noProxyRadioButton = new JRadioButton();
  private final JRadioButton httpProxyRadioButton = new JRadioButton();
  private final JRadioButton socksProxyRadioButton = new JRadioButton();
  private final JCheckBox authenticatedCheckBox = new JCheckBox();
  private final JCheckBox bypassLocalCheckBox = new JCheckBox();
  private final Box proxyHostArea = new Box(BoxLayout.Y_AXIS);
  private final FormPanel authenticationPanel = new FormPanel();
  private final FormField userNameField = new FormField(FieldType.TEXT);
  private final FormField passwordField = new FormField(FieldType.PASSWORD);
  private final FormPanel hostPortPanel = new FormPanel();
  private final FormField hostField = new FormField(FieldType.TEXT);
  private final FormField portField = new FormField(FieldType.TEXT);

  public ConnectionSettingsUI() {
    this.noProxyRadioButton.addChangeListener(new ChangeListener() {
      public void stateChanged(final ChangeEvent e) {
        updateEnabling();
      }
    });
    this.authenticatedCheckBox.addChangeListener(new ChangeListener() {
      public void stateChanged(final ChangeEvent e) {
        updateEnabling();
      }
    });
    this.noProxyRadioButton.setText("Direct connection (no proxy)");
    this.httpProxyRadioButton.setText("HTTP proxy");
    this.socksProxyRadioButton.setText("SOCKS proxy");
    this.authenticatedCheckBox.setText("Authenticate with proxy server.");
    this.bypassLocalCheckBox.setText("Bypass proxy for local addresses.");
    this.userNameField.setCaption("User name:");
    this.passwordField.setCaption("Password:");
    this.authenticationPanel.addField(this.userNameField);
    this.authenticationPanel.addField(this.passwordField);
    this.hostField.setCaption("Host:");
    this.portField.setCaption("Port:");
    this.hostPortPanel.addField(this.hostField);
    this.hostPortPanel.addField(this.portField);

    final ButtonGroup group = new ButtonGroup();
    group.add(this.noProxyRadioButton);
    group.add(this.httpProxyRadioButton);
    group.add(this.socksProxyRadioButton);
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    this.add(Box.createRigidArea(new Dimension(8, 8)));
    this.add(this.getProxyBox());
    this.add(SwingTasks.createVerticalFill());
    this.loadSettings();
    this.updateEnabling();
  }

  private void updateEnabling() {
    SwingTasks.setNestedEnabled(this.proxyHostArea, !this.noProxyRadioButton.isSelected());
    SwingTasks.setNestedEnabled(this.authenticationPanel, this.authenticatedCheckBox.isSelected());
  }

  private Component getProxyBox() {
    final Box radioBox = new Box(BoxLayout.Y_AXIS);
    radioBox.setPreferredSize(new Dimension(600, 200));
    radioBox.add(this.noProxyRadioButton);
    radioBox.add(this.httpProxyRadioButton);
    radioBox.add(this.socksProxyRadioButton);

    final Box radioBoxExpander = new Box(BoxLayout.X_AXIS);
    radioBoxExpander.add(radioBox);
    radioBoxExpander.add(Box.createGlue());

    final Box box = SwingTasks.createGroupBox(BoxLayout.Y_AXIS, "Proxy");
    box.add(radioBoxExpander);
    box.add(this.getProxyHostArea());
    return box;
  }

  private Component getProxyHostArea() {
    final Box checkBoxBox = new Box(BoxLayout.Y_AXIS);
    checkBoxBox.setPreferredSize(new Dimension(600, 200));
    checkBoxBox.add(this.bypassLocalCheckBox);
    checkBoxBox.add(this.authenticatedCheckBox);

    final Box checkBoxBoxExpander = new Box(BoxLayout.X_AXIS);
    checkBoxBoxExpander.add(checkBoxBox);
    checkBoxBoxExpander.add(Box.createHorizontalGlue());

    final Box box = this.proxyHostArea;
    box.setBorder(new EmptyBorder(8, 16, 8, 8));
    box.add(this.hostPortPanel);
    box.add(checkBoxBoxExpander);
    box.add(this.authenticationPanel);
    return box;
  }

  @Override
  public void restoreDefaults() {
    this.settings.restoreDefaults();
    this.loadSettings();
  }

  @Override
  public void save() throws ValidationException {
    final ConnectionSettings settings = this.settings;
    Proxy.Type proxyType;
    if (this.noProxyRadioButton.isSelected()) {
      proxyType = Proxy.Type.DIRECT;
    } else if (this.httpProxyRadioButton.isSelected()) {
      proxyType = Proxy.Type.HTTP;
    } else if (this.socksProxyRadioButton.isSelected()) {
      proxyType = Proxy.Type.SOCKS;
    } else {
      throw new IllegalStateException("not expected");
    }
    settings.setProxyType(proxyType);
    settings.setAuthenticated(this.authenticatedCheckBox.isSelected());
    settings.setUserName(this.userNameField.getValue());
    settings.setPassword(this.passwordField.getValue());
    settings.setDisableProxyForLocalAddresses(this.bypassLocalCheckBox.isSelected());
    final String host = this.hostField.getValue();
    if ("".equals(host) && (proxyType != Proxy.Type.DIRECT)) {
      throw new ValidationException("To set up a proxy, a host name must be provided.");
    }
    int port;
    try {
      port = Integer.parseInt(this.portField.getValue());
    } catch (final NumberFormatException nfe) {
      if (proxyType != Proxy.Type.DIRECT) {
        throw new ValidationException("The port must be a number.");
      } else {
        port = 0;
      }
    }
    final InetSocketAddress socketAddress = new InetSocketAddress(host, port);
    settings.setInetSocketAddress(socketAddress);
    settings.save();
  }

  private void loadSettings() {
    final ConnectionSettings settings = this.settings;
    switch (settings.getProxyType()) {
    case DIRECT:
      this.noProxyRadioButton.setSelected(true);
      break;
    case HTTP:
      this.httpProxyRadioButton.setSelected(true);
      break;
    case SOCKS:
      this.socksProxyRadioButton.setSelected(true);
      break;
    }
    this.authenticatedCheckBox.setSelected(settings.isAuthenticated());
    this.userNameField.setValue(settings.getUserName());
    this.passwordField.setValue(settings.getPassword());
    this.bypassLocalCheckBox.setSelected(settings.isDisableProxyForLocalAddresses());
    final InetSocketAddress socketAddress = settings.getInetSocketAddress();
    if (socketAddress == null) {
      this.hostField.setValue("");
      this.portField.setValue("");
    } else {
      this.hostField.setValue(socketAddress.getHostName());
      this.portField.setValue(String.valueOf(socketAddress.getPort()));
    }
    this.authenticationPanel.revalidate();
    this.hostPortPanel.revalidate();
  }
}
