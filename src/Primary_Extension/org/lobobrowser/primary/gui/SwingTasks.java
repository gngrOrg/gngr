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

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

public class SwingTasks {
  private static final String NESTED_ENABLING = "$nested.enabling";

  public static void setNestedEnabled(final JComponent component, final boolean enabled) {
    final Boolean nestedEnabling = (Boolean) component.getClientProperty(NESTED_ENABLING);
    if ((nestedEnabling == null) || (nestedEnabling.booleanValue() != enabled)) {
      component.putClientProperty(NESTED_ENABLING, enabled);
      final Container parent = component.getParent();
      if ((parent == null) || !enabled || parent.isEnabled()) {
        setEnabledRecursive(component, enabled);
      }
    }
  }

  private static void setEnabledRecursive(final JComponent component, final boolean enabled) {
    component.setEnabled(enabled);
    final int count = component.getComponentCount();
    for (int i = 0; i < count; i++) {
      final Component child = component.getComponent(i);
      if (child instanceof JComponent) {
        final JComponent jchild = (JComponent) child;
        if (enabled) {
          final Boolean nestedEnabling = (Boolean) jchild.getClientProperty(NESTED_ENABLING);
          if ((nestedEnabling == null) || nestedEnabling.booleanValue()) {
            setEnabledRecursive(jchild, true);
          }
        } else {
          setEnabledRecursive(jchild, false);
        }
      }
    }
  }

  public static Component createVerticalFill() {
    final Dimension min = new Dimension(0, 0);
    final Dimension pref = new Dimension(0, Short.MAX_VALUE);
    final Dimension max = pref;
    return new Box.Filler(min, pref, max);
  }

  public static Component createHorizontalFill() {
    final Dimension min = new Dimension(0, 0);
    final Dimension pref = new Dimension(Short.MAX_VALUE, 0);
    final Dimension max = pref;
    return new Box.Filler(min, pref, max);
  }

  public static Frame getFrame(final Component component) {
    Container ancestor = component.getParent();
    while ((ancestor != null) && !(ancestor instanceof Frame)) {
      ancestor = ancestor.getParent();
    }
    return (Frame) ancestor;
  }

  public static Dialog getDialog(final Component component) {
    Container ancestor = component.getParent();
    while ((ancestor != null) && !(ancestor instanceof Dialog)) {
      ancestor = ancestor.getParent();
    }
    return (Dialog) ancestor;
  }

  public static Box createGroupBox(final int orientation, final String title) {
    final Box box = new Box(orientation);
    box.setBorder(new TitledBorder(new EtchedBorder(), title));
    return box;
  }
}