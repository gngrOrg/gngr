package org.lobobrowser.security;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

import org.lobobrowser.security.PermissionCellButton.ChangeListener;
import org.lobobrowser.security.PermissionSystem.PermissionBoard;
import org.lobobrowser.security.PermissionSystem.PermissionBoard.PermissionRow;
import org.lobobrowser.security.PermissionSystem.PermissionBoard.PermissionRow.PermissionCell;
import org.lobobrowser.ua.UserAgentContext.RequestKind;

public class PermissionTable {

  public static JComponent makeTable(final PermissionSystem system, final String[] columnNames, final String[][] requestData) {
    final List<PermissionCellButton> buttons = new LinkedList<>();
    final ChangeListener listener = () -> {
      buttons.stream().forEach(b -> b.update());
    };
    final JTabbedPane tabPane = new JTabbedPane();
    tabPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    tabPane.setTabPlacement(SwingConstants.RIGHT);
    tabPane.setUI(new BasicTabbedPaneUI() {
      private static final int PADDING_Y = 16;
      private static final int PADDING_X = 16;

      @Override
      protected int calculateTabHeight(final int tabPlacement, final int tabIndex, final int fontHeight) {
        return super.calculateTabHeight(tabPlacement, tabIndex, fontHeight) + PADDING_Y;
      }

      @Override
      protected int calculateTabWidth(final int tabPlacement, final int tabIndex, final java.awt.FontMetrics metrics) {
        return super.calculateTabWidth(tabPlacement, tabIndex, metrics) + PADDING_X;
      }
    });
    tabPane.setFocusable(false);
    system.getBoards().stream().forEachOrdered(board -> {
      final JPanel grid = makeBoardView(board, columnNames, requestData, buttons, listener);
      tabPane.add(board.hostPattern, grid);
    });
    tabPane.setSelectedIndex(tabPane.getTabCount() - 1);
    return tabPane;
  }

  private static JPanel makeBoardView(final PermissionBoard board, final String[] columnNames, final String[][] requestData,
      final List<PermissionCellButton> buttons, final ChangeListener listener) {
	
    final JPanel grid = new JPanel(new GridBagLayout());
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.ipadx = 20;
    gbc.ipady = 4;
    gbc.insets = new Insets(1, 1, 1, 1);
    {
      final PermissionRow row = board.getHeaderRow();
      addRowToGrid(grid, gbc, row, columnNames, listener, buttons);
    }
    {
      final List<Entry<String, PermissionRow>> rows = board.getRows();
      for (int i = 0, numRows = board.getRowCount(); i < numRows; i++) {
        gbc.gridy = i + 1;
        final PermissionRow row = rows.get(i).getValue();
        
        // calculate the values 
        
        addRowToGrid(grid, gbc, row, requestData[i], listener, buttons);
      }
    }
    final JPanel wrapGrid = new JPanel();
    wrapGrid.add(grid);
    wrapGrid.setBorder(new EmptyBorder(16, 16, 16, 16));
    return wrapGrid;
  }

  private static void addRowToGrid(final JPanel grid, final GridBagConstraints gbc, final PermissionRow row, final String[] texts,
      final ChangeListener listener, final List<PermissionCellButton> buttons) {
    for (int j = 0, numCols = RequestKind.numKinds() + 1; j < numCols; j++) {
      PermissionCell cell;
      if (j == 0) {
        gbc.weightx = 1d;
        cell = row.getHostCell();
        
      } else {
        gbc.weightx = 0d;
        cell = row.getRequestCell(j - 1);
      }
      final String text = texts[j].equals("0") ? " " : texts[j];
      final PermissionCellButton button = new PermissionCellButton(cell, text, j == 0, listener);
      gbc.gridx = j;
      grid.add(button, gbc);
      buttons.add(button);
    }
  }
}
