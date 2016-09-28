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

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

public class PreferencesTree extends JTree {
  private static final long serialVersionUID = -6816581073312582184L;

  public PreferencesTree() {
    final TreeNode rootNode = createRootNode();
    this.setModel(new DefaultTreeModel(rootNode));
    this.setRootVisible(false);
  }

  public void initSelection() {
    this.addSelectionRow(0);
  }

  private static TreeNode createRootNode() {
    final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    root.add(new DefaultMutableTreeNode(getGeneralSettingsInfo()));
    root.add(new DefaultMutableTreeNode(getConnectionSettingsInfo()));
    root.add(new DefaultMutableTreeNode(getToolsSettingsInfo()));
    return root;
  }

  private static SettingsInfo getGeneralSettingsInfo() {
    return new SettingsInfo() {
      public AbstractSettingsUI createSettingsUI() {
        return new GeneralSettingsUI();
      }

      public String getDescription() {
        return "General browser settings.";
      }

      public String getName() {
        return "General";
      }

      @Override
      public String toString() {
        return this.getName();
      }
    };
  }

  private static SettingsInfo getConnectionSettingsInfo() {
    return new SettingsInfo() {
      public AbstractSettingsUI createSettingsUI() {
        return new ConnectionSettingsUI();
      }

      public String getDescription() {
        return "Network connection settings.";
      }

      public String getName() {
        return "Connection";
      }

      @Override
      public String toString() {
        return this.getName();
      }
    };
  }

  private static SettingsInfo getToolsSettingsInfo() {
    return new SettingsInfo() {
      public AbstractSettingsUI createSettingsUI() {
        return new ToolsSettingsUI();
      }

      public String getDescription() {
        return "Tools settings.";
      }

      public String getName() {
        return "Tools";
      }

      @Override
      public String toString() {
        return this.getName();
      }
    };
  }
}
