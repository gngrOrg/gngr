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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class AddressField extends JComboBox<String> {
  private static final long serialVersionUID = 3726432852226425553L;
  private final ComponentSource componentSource;

  public AddressField(final ComponentSource cs) {
    this.componentSource = cs;
    this.setEditable(true);
    final TextFieldComboBoxEditor editor = new TextFieldComboBoxEditor();
    this.setEditor(editor);
    editor.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(final KeyEvent e) {
        onKeyReleased(e);
      }

      @Override
      public void keyPressed(final KeyEvent e) {
        onKeyPressed(e);
      }
    });

    editor.addMouseListener(new MouseListener() {

      @Override
      public void mouseReleased(MouseEvent e) {
        processMouseEvent(e);
      }

      @Override
      public void mousePressed(MouseEvent e) {
        processMouseEvent(e);
      }

      @Override
      public void mouseExited(MouseEvent e) {
        processMouseEvent(e);
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        processMouseEvent(e);
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        processMouseEvent(e);
      }
    });

    this.addPopupMenuListener(new PopupMenuListener() {
      public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
        onBeforePopupVisible();
      }

      public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
      }

      public void popupMenuCanceled(final PopupMenuEvent e) {
      }
    });
    this.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        final String cmd = event.getActionCommand();
        if ("comboBoxEdited".equals(cmd)) {
          onEdited(event.getModifiers());
        } else if ("comboBoxChanged".equals(cmd)) {
        }
      }
    });

    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_L, ComponentSource.CMD_CTRL_KEY_MASK), "edit URL");

    getActionMap().put("edit URL", new AbstractAction() {

      private static final long serialVersionUID = 891701932843814767L;

      public void actionPerformed(final ActionEvent e) {
        requestFocus();
        getEditor().selectAll();
      }
    });

    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_K, ComponentSource.CMD_CTRL_KEY_MASK), "search Keyword");

    getActionMap().put("search Keyword", new AbstractAction() {

      private static final long serialVersionUID = -6652427897850176208L;

      public void actionPerformed(final ActionEvent e) {
        requestFocus();
        setText("?");
      }
    });

    // This needed the first time to set a reasonable popup size.
    this.onBeforePopupVisible();
  }

  public String getText() {
    if (this.isEditable()) {
      return (String) this.getEditor().getItem();
    } else {
      return String.valueOf(this.getSelectedItem());
    }
  }

  public void setText(final String text) {
    final JComboBox<String> combo = this;
    final boolean editable = this.isEditable();
    if (editable) {
      combo.getEditor().setItem(text);
    }
  }

  public void setUrl(final java.net.URL url) {
    this.setText(url == null ? "" : url.toExternalForm());
  }

  private void onBeforePopupVisible() {
    if ((comboInvalid || comboHasHeadMatches) && !populatingMatches) {
      populateCombo(this.getText());
    }
  }

  private boolean comboInvalid = true;
  private boolean comboHasHeadMatches = false;
  private boolean populatingMatches = false;

  private void populateCombo(final String comboBoxText) {
    // Expected to be called in GUI thread.
    this.populatingMatches = true;
    try {
      final JComboBox<String> urlComboBox = this;
      urlComboBox.removeAllItems();
      final Collection<String> recentUrls = ComponentSource.getRecentLocations(30);
      for (final String url : recentUrls) {
        urlComboBox.addItem(url);
      }
      this.setText(comboBoxText);
      this.comboHasHeadMatches = false;
      this.comboInvalid = false;
    } finally {
      this.populatingMatches = false;
    }
  }

  private void onEdited(final int modifiers) {
    // if(this.getText().length() != 0) {
    // this.componentSource.navigateOrSearch();
    // }
  }

  private void onKeyReleased(final KeyEvent event) {
    final AddressField urlComboBox = this;
    final char releasedChar = event.getKeyChar();
    if (validPopupChar(releasedChar)) {
      final String urlText = urlComboBox.getText();
      final Collection<String> headMatches = ComponentSource.getPotentialMatches(urlText, 30);
      if (headMatches.size() == 0) {
        if (urlComboBox.isPopupVisible()) {
          urlComboBox.hidePopup();
        }
      } else {
        populatingMatches = true;
        try {
          urlComboBox.removeAllItems();
          final Iterator<String> i = headMatches.iterator();
          while (i.hasNext()) {
            final String matchUrl = i.next();
            urlComboBox.addItem(matchUrl);
          }
          comboHasHeadMatches = true;
          if (!urlComboBox.isPopupVisible()) {
            urlComboBox.showPopup();
          }
          urlComboBox.setSelectedItem(null);
          urlComboBox.setText(urlText);
        } finally {
          populatingMatches = false;
        }
      }
    }

  }

  private void onKeyPressed(final KeyEvent event) {
    final AddressField urlComboBox = this;
    if (event.getKeyCode() == KeyEvent.VK_ENTER) {
      final String urlText = urlComboBox.getText();
      if (urlText.length() != 0) {
        this.componentSource.navigateOrSearch();
      }
    }
  }

  private static boolean validPopupChar(final char ch) {
    return Character.isLetterOrDigit(ch) || (ch == '.') || (ch == '/');
  }
}
