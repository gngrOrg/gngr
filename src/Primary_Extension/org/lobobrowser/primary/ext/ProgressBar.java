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

import java.awt.Dimension;
import java.awt.Graphics;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JProgressBar;

import org.lobobrowser.ua.ProgressType;

public class ProgressBar extends JProgressBar {
  private static final long serialVersionUID = -6283186577566949668L;
  private static final Logger logger = Logger.getLogger(ProgressBar.class.getName());

  public ProgressBar() {
    this.setStringPainted(true);
  }

  public void updateProgress(final ProgressType progressType, final int value, final int max) {
    switch (progressType) {
    case NONE:
    case DONE:
      this.setString("");
      this.setIndeterminate(false);
      this.setValue(0);
      this.setMaximum(0);
      break;
    default:
      if (max == -1) {
        this.setIndeterminate(true);
        this.setString(getSizeText(value));
      } else {
        this.setIndeterminate(false);
        this.setValue(value);
        this.setMaximum(max);
        if (max == 0) {
          this.setString("");
        } else {
          this.setString(((value * 100) / max) + "%");
        }
      }
      break;
    }
  }

  private static double round1(final double value) {
    return Math.round(value * 10.0) / 10.0;
  }

  private static String getSizeText(final int numBytes) {
    if (numBytes == 0) {
      return "";
    } else if (numBytes < 1024) {
      return numBytes + " bytes";
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

  @Override
  public Dimension getMinimumSize() {
    return new Dimension(64, 18);
  }

  @Override
  public Dimension getMaximumSize() {
    return new Dimension(128, 100);
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(128, 18);
  }

  @Override
  protected void paintComponent(final Graphics g) {
    try {
      super.paintComponent(g);
    } catch (final Exception err) {
      logger.log(Level.SEVERE, "paintComponent(): Swing bug?", err);
    }
  }
}
