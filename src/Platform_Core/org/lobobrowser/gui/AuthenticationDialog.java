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
package org.lobobrowser.gui;

import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.net.PasswordAuthentication;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * Dialog used in HTTP and proxy authentication.
 */
public class AuthenticationDialog extends JDialog {
  private static final long serialVersionUID = 5601837809153264164L;
  private final JTextField userNameField = new JTextField();
  private final JPasswordField passwordField = new JPasswordField();

  public AuthenticationDialog(final Frame owner) throws HeadlessException {
    super(owner);
    this.init();
  }

  public AuthenticationDialog(final Dialog owner) throws HeadlessException {
    super(owner);
    this.init();
  }

  private void init() {
    final Container contentPane = this.getContentPane();
    contentPane.setLayout(new FlowLayout());

    final Box rootBox = new Box(BoxLayout.Y_AXIS);
    rootBox.setBorder(new EmptyBorder(4, 4, 4, 4));

    final Box userNameBox = new Box(BoxLayout.X_AXIS);
    final JLabel userNameLabel = new JLabel("User name:");
    final int unph = userNameLabel.getPreferredSize().height;
    userNameLabel.setPreferredSize(new Dimension(100, unph));
    userNameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    userNameBox.add(userNameLabel);
    userNameBox.add(Box.createRigidArea(new Dimension(4, 1)));
    userNameBox.add(this.userNameField);
    userNameBox.setPreferredSize(new Dimension(300, unph + 4));

    final Box passwordBox = new Box(BoxLayout.X_AXIS);
    final JLabel passwordLabel = new JLabel("Password:");
    final int pwph = passwordLabel.getPreferredSize().height;
    passwordLabel.setPreferredSize(new Dimension(100, pwph));
    passwordLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    passwordBox.add(passwordLabel);
    passwordBox.add(Box.createRigidArea(new Dimension(4, 1)));
    passwordBox.add(this.passwordField);
    passwordBox.setPreferredSize(new Dimension(300, pwph + 4));

    final Box buttonBox = new Box(BoxLayout.X_AXIS);
    final JButton okButton = new JButton();
    okButton.setAction(new OkAction());
    okButton.setText("OK");
    final JButton cancelButton = new JButton();
    cancelButton.setAction(new CancelAction());
    cancelButton.setText("Cancel");
    buttonBox.add(Box.createHorizontalGlue());
    buttonBox.add(okButton);
    buttonBox.add(Box.createHorizontalStrut(4));
    buttonBox.add(cancelButton);
    buttonBox.add(Box.createHorizontalGlue());

    rootBox.add(userNameBox);
    rootBox.add(Box.createVerticalStrut(2));
    rootBox.add(passwordBox);
    rootBox.add(Box.createVerticalStrut(4));
    rootBox.add(buttonBox);

    contentPane.add(rootBox);
  }

  public void setUserName(final String userName) {
    this.userNameField.setText(userName);
    this.passwordField.grabFocus();
  }

  private PasswordAuthentication authentication;

  public PasswordAuthentication getAuthentication() {
    return this.authentication;
  }

  private class OkAction extends AbstractAction {
    private static final long serialVersionUID = 3308644732677944619L;

    public void actionPerformed(final ActionEvent e) {
      authentication = new PasswordAuthentication(userNameField.getText(), passwordField.getPassword());
      AuthenticationDialog.this.dispose();
    }
  }

  private class CancelAction extends AbstractAction {
    private static final long serialVersionUID = 703637268854289240L;

    public void actionPerformed(final ActionEvent e) {
      authentication = null;
      AuthenticationDialog.this.dispose();
    }
  }
}
