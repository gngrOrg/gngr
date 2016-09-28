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
/*
 * Created on Jun 19, 2005
 */
package org.lobobrowser.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.HeadlessException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import org.lobobrowser.request.ClientletRequestHandler;
import org.lobobrowser.request.UserAgentImpl;
import org.lobobrowser.ua.NavigatorProgressEvent;
import org.lobobrowser.ua.UserAgentContext;

/**
 * Progress window shown before a window is opened.
 */
public class ProgressWindow extends JFrame {
  private static final long serialVersionUID = 7625806507431580931L;
  private final OpenProgressBar statusProgressBar;
  private final JLabel statusLabel;

  /**
   * @throws java.awt.HeadlessException
   */
  public ProgressWindow() throws HeadlessException {
    super(UserAgentImpl.getInstance().getName());
    this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    final UserAgentContext uaContext = null; // TODO
    final ImageIcon windowIcon = DefaultWindowFactory.getInstance().getDefaultImageIcon(uaContext);
    if (windowIcon != null) {
      this.setIconImage(windowIcon.getImage());
    }
    final Container contentPane = this.getContentPane();
    contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
    final JPanel topPanel = new JPanel();
    contentPane.add(topPanel);
    contentPane.add(Box.createRigidArea(new Dimension(1, 18)));
    final Border bevelBorder = new BevelBorder(BevelBorder.LOWERED);
    final Border emptyBorder = new EmptyBorder(10, 10, 10, 10);
    final Border border = new CompoundBorder(bevelBorder, emptyBorder);
    topPanel.setBorder(border);
    topPanel.setLayout(new BorderLayout());

    this.statusProgressBar = new OpenProgressBar();
    this.statusProgressBar.setStringPainted(true);
    this.statusLabel = new JLabel();

    topPanel.add(statusLabel, BorderLayout.NORTH);
    topPanel.add(statusProgressBar, BorderLayout.CENTER);
  }

  public void updateProgress(final NavigatorProgressEvent event) {
    statusLabel.setText(ClientletRequestHandler.getProgressMessage(event.getProgressType(), event.getUrl()));
    final int value = event.getCurrentValue();
    final int max = event.getMaxValue();
    if (max == -1) {
      statusProgressBar.setIndeterminate(true);
      statusProgressBar.setValue(value);
      statusProgressBar.setString(getSizeText(value));
      statusProgressBar.repaint();
    } else {
      statusProgressBar.setIndeterminate(false);
      statusProgressBar.setValue(value);
      statusProgressBar.setMaximum(max);
      if ((value == 0) || (max == 0)) {
        statusProgressBar.setString("");
      } else {
        final int percent = (value * 100) / max;
        statusProgressBar.setString(String.valueOf(percent) + "%");
      }
      statusProgressBar.repaint();
    }
  }

  private static double round1(final double value) {
    return Math.round(value * 10.0) / 10.0;
  }

  private static String getSizeText(final int numBytes) {
    if (numBytes < 1024) {
      return "";
    } else {
      final double numK = numBytes / 1024.0;
      if (numK < 1024) {
        return round1(numK) + " Kb";
      } else {
        final double numM = numK / 1024.0;
        if (numM < 1024) {
          return round1(numM) + " Mb";
        } else {
          final double numG = numM / 1024.0;
          return round1(numG) + " Gb";
        }
      }
    }
  }

}
