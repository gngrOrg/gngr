/*
GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The Lobo Project

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Contact info: lobochief@users.sourceforge.net
 */
package org.lobobrowser.html.renderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.domimpl.AnonymousNodeImpl;
import org.lobobrowser.html.domimpl.HTMLElementImpl;
import org.lobobrowser.html.domimpl.ModelNode;
import org.lobobrowser.html.domimpl.NodeImpl;
import org.lobobrowser.html.domimpl.TextImpl;
import org.lobobrowser.html.style.BorderInfo;
import org.lobobrowser.html.style.HtmlInsets;
import org.lobobrowser.html.style.HtmlLength;
import org.lobobrowser.html.style.HtmlValues;
import org.lobobrowser.html.style.JStyleProperties;
import org.lobobrowser.html.style.RenderState;
import org.lobobrowser.html.style.RenderThreadState;
import org.lobobrowser.ua.UserAgentContext;

final class TableMatrix {
  private final ArrayList<Row> ROWS = new ArrayList<>();
  private final ArrayList<RowGroup> ROW_GROUPS = new ArrayList<>();
  private final ArrayList<@NonNull RAbstractCell> ALL_CELLS = new ArrayList<>();
  private final HTMLElementImpl tableElement;
  private final UserAgentContext uaContext;
  private final HtmlRendererContext rendererContext;
  private final FrameContext frameContext;
  private final RElement relement;
  private final RenderableContainer container;

  private ColSizeInfo[] columnSizes;
  private RowSizeInfo[] rowSizes;
  private int tableWidth;
  private int tableHeight;

  /*
   * This is so that we can draw the lines inside the table that appear when a
   * border attribute is used.
   */
  private int hasOldStyleBorder;

  /**
   * @param element
   */
  public TableMatrix(final HTMLElementImpl element, final UserAgentContext uaContext, final HtmlRendererContext rcontext,
      final FrameContext frameContext,
      final RenderableContainer tableAsContainer, final RElement relement) {
    this.tableElement = element;
    this.uaContext = uaContext;
    this.rendererContext = rcontext;
    this.frameContext = frameContext;
    this.relement = relement;
    this.container = tableAsContainer;
  }

  @Override
  public void finalize() throws Throwable {
    super.finalize();
  }

  public int getNumRows() {
    return this.ROWS.size();
  }

  public int getNumColumns() {
    return this.columnSizes.length;
  }

  /**
   * @return Returns the tableHeight.
   */
  public int getTableHeight() {
    return this.tableHeight;
  }

  /**
   * @return Returns the tableWidth.
   */
  public int getTableWidth() {
    return this.tableWidth;
  }

  // private int border;
  private int cellSpacingY;
  private int cellSpacingX;
  private int widthsOfExtras;
  private int heightsOfExtras;
  private HtmlLength tableWidthLength;
  private ArrayList<RowGroupSizeInfo> rowGroupSizes;

  /**
   * Called on every relayout. Element children might have changed.
   */
  public void reset(final Insets insets, final int availWidth, final int availHeight) {
    // TODO: Incorporate into build() and calculate
    // sizes properly based on parameters.
    ROW_GROUPS.clear();
    ROWS.clear();
    ALL_CELLS.clear();
    rowGroupSizes = null;
    // TODO: Does it need this old-style border?
    final int border = getBorderAttribute();
    final int cellSpacing = getCellSpacingAttribute();

    this.cellSpacingX = cellSpacing;
    this.cellSpacingY = cellSpacing;

    this.tableWidthLength = TableMatrix.getWidthLength(this.tableElement, availWidth);

    this.populateRows();
    this.adjustForCellSpans();
    this.createSizeArrays();

    // Calculate widths of extras
    final ColSizeInfo[] columnSizes = this.columnSizes;
    final int numCols = columnSizes.length;
    int widthsOfExtras = insets.left + insets.right + ((numCols + 1) * cellSpacing);
    if (border > 0) {
      widthsOfExtras += (numCols * 2);
    }
    this.widthsOfExtras = widthsOfExtras;

    // Calculate heights of extras
    final RowSizeInfo[] rowSizes = this.rowSizes;
    final int numRows = rowSizes.length;
    int heightsOfExtras = insets.top + insets.bottom + ((numRows + 1) * cellSpacing);
    if (border > 0) {
      heightsOfExtras += (numRows * 2);
    }
    this.heightsOfExtras = heightsOfExtras;
    this.hasOldStyleBorder = border > 0 ? 1 : 0;
  }

  private int getCellSpacingAttribute() {
    int cellSpacing = 0;
    final String cellSpacingText = this.tableElement.getAttribute("cellspacing");
    if (cellSpacingText != null) {
      try {
        // TODO: cellSpacing can be a percentage as well
        cellSpacing = Integer.parseInt(cellSpacingText);
        if (cellSpacing < 0) {
          cellSpacing = 0;
        }
      } catch (final NumberFormatException nfe) {
        System.out.println("Exception while parsing cellSpacing: " + nfe);
        // ignore
      }
    }
    return cellSpacing;
  }

  private int getBorderAttribute() {
    int border = 0;
    final String borderText = this.tableElement.getAttribute("border");
    if (borderText != null) {
      if (borderText.length() == 0) {
        border = 1;
      } else {
        try {
          border = Integer.parseInt(borderText);
          if (border < 0) {
            border = 0;
          }
        } catch (final NumberFormatException nfe) {
          System.out.println("Exception while parsing border: " + nfe);
          // ignore
        }
      }
    }
    return border;
  }

  public void build(final int availWidth, final int availHeight, final boolean sizeOnly) {
    final int hasBorder = this.hasOldStyleBorder;
    this.determineColumnSizes(hasBorder, this.cellSpacingX, this.cellSpacingY, availWidth);
    this.determineRowSizes(hasBorder, this.cellSpacingY, availHeight, sizeOnly);
  }

  private static HtmlLength getWidthLength(final HTMLElementImpl element, final int availWidth) {
    try {
      final JStyleProperties props = element.getCurrentStyle();
      final String widthText = props.getWidth();
      if (widthText == null) {
        // TODO: convert attributes to CSS properties
        final String widthAttr = element.getAttribute("width");
        if (widthAttr == null) {
          return null;
        }
        return new HtmlLength(HtmlValues.getPixelSize(widthAttr, element.getRenderState(), 0, availWidth));
      } else {
        return new HtmlLength(HtmlValues.getPixelSize(widthText, element.getRenderState(), 0, availWidth));
      }
    } catch (final NumberFormatException err) {
      System.out.println("Exception while parsing width: " + err);
      return null;
    }
  }

  private static HtmlLength getHeightLength(final HTMLElementImpl element, final int availHeight) {
    try {
      final JStyleProperties props = element.getCurrentStyle();
      final String heightText = props.getHeight();
      if (heightText == null) {
        final String ha = element.getAttribute("height");
        if (ha == null) {
          return null;
        } else {
          return new HtmlLength(HtmlValues.getPixelSize(ha, element.getRenderState(), 0, availHeight));
        }
      } else {
        return new HtmlLength(HtmlValues.getPixelSize(heightText, element.getRenderState(), 0, availHeight));
      }
    } catch (final NumberFormatException err) {
      System.out.println("Exception while parsing height: " + err);
      return null;
    }
  }

  static Insets getCSSInsets(final RenderState rs) {
    final BorderInfo borderInfo = rs.getBorderInfo();
    final HtmlInsets elemBorderHtmlInsets = borderInfo == null ? null : borderInfo.insets;
    return elemBorderHtmlInsets == null ? RBlockViewport.ZERO_INSETS : elemBorderHtmlInsets.getAWTInsets(0, 0, 0, 0, 0, 0, 0, 0);
  }

  private static final class RowGroup {
    final ArrayList<Row> rows = new ArrayList<>();
    private final HTMLElementImpl rowGroupElem;
    final BorderOverrider borderOverrider = new BorderOverrider();

    public RowGroup(final HTMLElementImpl rowGroupElem) {
      this.rowGroupElem = rowGroupElem;
    }

    void add(final Row row) {
      rows.add(row);
      row.rowGroup = this;
    }

    public void finish() {
      final int numRows = rows.size();
      int minCellBorderLeft = -1;
      int minCellBorderRight = -1;
      for (int i = 0; i < numRows; i++) {
        final Row r = rows.get(i);
        final int cellBorderLeftMost = r.getCellBorderLeftMost();
        if ((minCellBorderLeft == -1) || (cellBorderLeftMost < minCellBorderLeft)) {
          minCellBorderLeft = cellBorderLeftMost;
        }
        final int cellBorderRightMost = r.getCellBorderRightMost();
        if ((minCellBorderRight == -1) || (cellBorderRightMost < minCellBorderRight)) {
          minCellBorderRight = cellBorderRightMost;
        }
      }
      final int minCellBorderTop = rows.get(0).minCellBorderTop;
      final int minCellBorderBottom = rows.get(0).minCellBorderBottom;

      final Insets groupBorderInsets = rowGroupElem == null ? null : getCSSInsets(rowGroupElem.getRenderState());

      if (groupBorderInsets != null) {
        if (groupBorderInsets.top <= minCellBorderTop) {
          borderOverrider.topOverridden = true;
        } else {
          final Row firstRow = rows.get(0);
          for (final VirtualCell cell : firstRow.cells) {
            // TODO: Only override if cells border is less than minCellBorderTop (?)
            cell.getActualCell().borderOverrider.topOverridden = true;
          }
        }

        if (groupBorderInsets.bottom <= minCellBorderBottom) {
          borderOverrider.bottomOverridden = true;
        } else {
          final Row lastRow = rows.get(rows.size() - 1);
          for (final VirtualCell cell : lastRow.cells) {
            // TODO: Only override if cells border is less than minCellBorderBottom (?)
            cell.getActualCell().borderOverrider.bottomOverridden = true;
          }
        }

        if (groupBorderInsets.left <= minCellBorderLeft) {
          borderOverrider.leftOverridden = true;
        } else {
          for (final Row row : rows) {
            row.getLeftMostCell().getActualCell().borderOverrider.leftOverridden = true;
          }
        }
        if (groupBorderInsets.right <= minCellBorderRight) {
          borderOverrider.rightOverridden = true;
        } else {
          for (final Row row : rows) {
            row.getRightMostCell().getActualCell().borderOverrider.rightOverridden = true;
          }
        }
      }
    }

    @Nullable HtmlInsets getGroupBorderInsets() {
      final BorderInfo borderInfo = rowGroupElem == null ? null : rowGroupElem.getRenderState().getBorderInfo();
      return borderInfo == null ? null : borderOverrider.get(borderInfo.insets);
    }

  }

  private static final class Row {
    final ArrayList<VirtualCell> cells = new ArrayList<>();
    final HTMLElementImpl rowGroupElem;
    RowGroup rowGroup;

    // TODO: Add getters and make private for the following four
    public boolean firstInGroup;
    public boolean lastInGroup;
    public int maxCellBorderTop = 0;
    public int maxCellBorderBottom = 0;
    int minCellBorderBottom = -1;
    int minCellBorderTop = -1;
    int rowIndex;

    Row(final HTMLElementImpl rowGroup) {
      this.rowGroupElem = rowGroup;
    }

    VirtualCell getLeftMostCell() {
      return cells.get(0);
    }

    VirtualCell getRightMostCell() {
      return cells.get(cells.size() - 1);
    }

    int getCellBorderRightMost() {
      return getCSSInsets(getLeftMostCell().getActualCell().getRenderState()).right;
    }

    int getCellBorderLeftMost() {
      return getCSSInsets(getLeftMostCell().getActualCell().getRenderState()).left;
    }

    void add(final @Nullable VirtualCell cell) {
      if (cell != null) {
        final RAbstractCell ac = cell.getActualCell();
        final @NonNull RenderState rs = ac.getRenderState();
        BorderInfo binfo = rs.getBorderInfo();
        if (binfo != null) {
          final HtmlInsets bi = binfo.insets;
          if (bi != null) {
            if (bi.top > maxCellBorderTop) {
              maxCellBorderTop = bi.top;
            }
            if ((bi.top < minCellBorderTop) || (minCellBorderTop == -1)) {
              minCellBorderTop = bi.top;
            }
            if (bi.bottom > maxCellBorderBottom) {
              maxCellBorderBottom = bi.bottom;
            }
            if ((bi.bottom < minCellBorderBottom) || (minCellBorderBottom == -1)) {
              minCellBorderBottom = bi.bottom;
            }
          }
        }
      }
      cells.add(cell);
    }

    public void add(int nc, VirtualCell virtualCell) {
      cells.add(nc, virtualCell);
    }

    public int size() {
      return cells.size();
    }

    public VirtualCell get(int c) {
      return cells.get(c);
    }
  }

  /** A class that helps map elements to children (or their delegates). It automatically takes care of
   *  non-existing parents by creating a place holder.
   *  For example, helps map table rows to virtual cells (which are delegates for table columns).
   */
  private static final class TableRelation {
    private final Map<HTMLElementImpl, Row> elementToRow = new HashMap<>(2);
    private Row currentFallbackRow = null;
    private final ArrayList<Row> listOfRows;
    private final ArrayList<RowGroup> listOfRowGroups;

    public TableRelation(final ArrayList<Row> listOfRows, final ArrayList<RowGroup> listOfRowGroups) {
      this.listOfRows = listOfRows;
      this.listOfRowGroups = listOfRowGroups;
    }

    void associate(final HTMLElementImpl rowGroupElem, final HTMLElementImpl rowElem, final VirtualCell cell) {
      Row row;
      if (rowElem != null) {
        currentFallbackRow = null;
        row = elementToRow.get(rowElem);
        if (row == null) {
          row = createRow(rowGroupElem);
          elementToRow.put(rowElem, row);
        }
      } else {
        // Doesn't have a parent. Let's add a list just for itself.
        if (currentFallbackRow != null) {
          row = currentFallbackRow;
        } else {
          row = createRow(rowGroupElem);
          currentFallbackRow = row;
        }
      }
      row.add(cell);
    }

    private Row createRow(final HTMLElementImpl rowGroupElem) {
      final Row row = new Row(rowGroupElem);
      row.rowIndex = this.listOfRows.size();
      this.listOfRows.add(row);
      return row;
    }

    void finish() {
      HTMLElementImpl prevRowGroupElem = null;
      RowGroup currentRowGroup = null;
      int numRows = listOfRows.size();
      for (int i = 0; i < numRows; i++)  {
        final Row row = listOfRows.get(i);
        row.firstInGroup = (i == 0) || (row.rowGroupElem != prevRowGroupElem);
        row.lastInGroup = (i == numRows - 1) || (listOfRows.get(i+1).rowGroupElem != row.rowGroupElem);
        if (row.firstInGroup) {
          currentRowGroup = new RowGroup(row.rowGroupElem);
          this.listOfRowGroups.add(currentRowGroup);
        }
        assert(currentRowGroup != null);
        currentRowGroup.add(row);
        if (row.lastInGroup) {
          currentRowGroup.finish();
        }
        prevRowGroupElem = row.rowGroupElem;
      }
    }
  }

  /**
   * Populates the ROWS and ALL_CELLS collections.
   */
  private ArrayList<HTMLElementImpl> populateRows() {
    final HTMLElementImpl te = this.tableElement;
    final ArrayList<HTMLElementImpl> rowElements = new ArrayList<>();
    final NodeImpl[] tChildren = te.getChildrenArray();
    final TableRelation rowRelation = new TableRelation(this.ROWS, this.ROW_GROUPS);

    if (tChildren != null) {
      for (final NodeImpl cn : tChildren) {
        if (cn instanceof HTMLElementImpl) {
          final HTMLElementImpl ce = (HTMLElementImpl) cn;
          final int display = ce.getRenderState().getDisplay();
          if (display == RenderState.DISPLAY_TABLE_ROW_GROUP || display == RenderState.DISPLAY_TABLE_HEADER_GROUP
              || display == RenderState.DISPLAY_TABLE_FOOTER_GROUP) {
            processRowGroup(ce, rowRelation);
          } else if (display == RenderState.DISPLAY_TABLE_ROW) {
            processRow(ce, null, rowRelation);
          } else if (display == RenderState.DISPLAY_TABLE_CELL) {
            processCell(ce, null, null, rowRelation);
          } else if (display != RenderState.DISPLAY_TABLE_COLUMN && display != RenderState.DISPLAY_TABLE_COLUMN_GROUP) {
            addAnonCell(rowRelation, null, null, cn);
          }
        } else if (cn instanceof TextImpl) {
          addAnonTextCell(rowRelation, null, null, (TextImpl) cn);
        }
      }
    }

    rowRelation.finish();

    {
      // Find the max insets among row group elements
      maxRowGroupLeft = 0;
      maxRowGroupRight = 0;
      for (final RowGroup rowGroup : this.ROW_GROUPS) {
        final HtmlInsets groupInsets = rowGroup.getGroupBorderInsets();
        if (groupInsets != null) {
          if (groupInsets.left > maxRowGroupLeft) {
            maxRowGroupLeft = groupInsets.left;
          }
          if (groupInsets.right > maxRowGroupRight) {
            maxRowGroupRight = groupInsets.right;
          }
        }
      }
    }

    return rowElements;
  }

  private void processCell(HTMLElementImpl ce, HTMLElementImpl rowGroupElem, HTMLElementImpl rowElem, TableRelation rowRelation) {
    RTableCell ac = new RTableCell(ce, this.uaContext, this.rendererContext, this.frameContext, this.container);
    ac.setParent(this.relement);
    ce.setUINode(ac);
    final VirtualCell vc = new VirtualCell(ac, true);
    ac.setTopLeftVirtualCell(vc);
    rowRelation.associate(rowGroupElem, rowElem, vc);
    this.ALL_CELLS.add(ac);
  }

  private void processRow(HTMLElementImpl rowE, HTMLElementImpl rowGroupElem, TableRelation rowRelation) {
    final NodeImpl[] rChildren = rowE.getChildrenArray();
    if (rChildren != null) {
      for (final NodeImpl cn : rChildren) {
        if (cn instanceof HTMLElementImpl) {
          final HTMLElementImpl ce = (HTMLElementImpl) cn;
          final int display = ce.getRenderState().getDisplay();
          if (display == RenderState.DISPLAY_TABLE_CELL) {
            processCell(ce, rowGroupElem, rowE, rowRelation);
          } else {
            addAnonCell(rowRelation, rowGroupElem, rowE, cn);
          }
        } else if (cn instanceof TextImpl) {
          addAnonTextCell(rowRelation, rowGroupElem, rowE, (TextImpl) cn);
        }
      }
    }
  }

  private void processRowGroup(HTMLElementImpl rowGroupElem, TableRelation rowRelation) {
    final NodeImpl[] rChildren = rowGroupElem.getChildrenArray();
    if (rChildren != null) {
      for (final NodeImpl cn : rChildren) {
        if (cn instanceof HTMLElementImpl) {
          final HTMLElementImpl ce = (HTMLElementImpl) cn;
          final int display = ce.getRenderState().getDisplay();
          if (display == RenderState.DISPLAY_TABLE_ROW) {
            processRow(ce, rowGroupElem, rowRelation);
          } else {
            addAnonCell(rowRelation, rowGroupElem, null, cn);
          }
        } else if (cn instanceof TextImpl) {
          addAnonTextCell(rowRelation, rowGroupElem, null, (TextImpl) cn);
        }
      }
    }
  }

  private void addAnonTextCell(final TableRelation rowRelation, HTMLElementImpl rowGroupElem, HTMLElementImpl rowElem, final TextImpl tn) {
    if (!tn.isElementContentWhitespace()) {
      addAnonCell(rowRelation, rowGroupElem, rowElem, tn);
    }
  }

  private void addAnonCell(final TableRelation rowRelation, HTMLElementImpl rowGroupElem, HTMLElementImpl rowElem, final NodeImpl node) {
    final AnonymousNodeImpl acn = new AnonymousNodeImpl(node.getParentNode());
    acn.appendChildSilently(node);
    final RAnonTableCell ac = new RAnonTableCell(acn, this.uaContext, this.rendererContext, this.frameContext, this.container);
    ac.setParent(this.relement);
    acn.setUINode(ac);
    final VirtualCell vc = new VirtualCell(ac, true);
    ac.setTopLeftVirtualCell(vc);
    rowRelation.associate(rowGroupElem, rowElem, vc);
    this.ALL_CELLS.add(ac);
  }

  /**
   * Based on colspans and rowspans, creates additional virtual cells from
   * actual table cells.
   */
  private void adjustForCellSpans() {
    final ArrayList<Row> rows = this.ROWS;
    int numRows = rows.size();
    for (int r = 0; r < numRows; r++) {
      final Row row = rows.get(r);
      int numCols = row.size();
      for (int c = 0; c < numCols; c++) {
        final VirtualCell vc = row.get(c);
        if ((vc != null) && vc.isTopLeft()) {
          final RAbstractCell ac = vc.getActualCell();
          int colspan = ac.getColSpan();
          if (colspan < 1) {
            colspan = 1;
          }
          int rowspan = ac.getRowSpan();
          if (rowspan < 1) {
            rowspan = 1;
          }

          // Can't go beyond last row (Fix bug #2022584)
          final int targetRows = r + rowspan;
          if (numRows < targetRows) {
            rowspan = numRows - r;
            ac.setRowSpan(rowspan);
          }

          numRows = rows.size();
          for (int y = 0; y < rowspan; y++) {
            if ((colspan > 1) || (y > 0)) {
              // Get row
              final int nr = r + y;
              final Row newRow = rows.get(nr);

              // Insert missing cells in row
              final int xstart = y == 0 ? 1 : 0;

              // Insert virtual cells, potentially
              // shifting others to the right.
              for (int cc = xstart; cc < colspan; cc++) {
                final int nc = c + cc;
                while (newRow.size() < nc) {
                  newRow.add(null);
                }
                newRow.add(nc, new VirtualCell(ac, false));
              }
              if (row == newRow) {
                numCols = row.size();
              }
            }
          }
        }
      }
    }

    // Adjust row and column of virtual cells
    for (int r = 0; r < numRows; r++) {
      final Row row = rows.get(r);
      final int numCols = row.size();
      for (int c = 0; c < numCols; c++) {
        final VirtualCell vc = row.get(c);
        if (vc != null) {
          vc.setColumn(c);
          vc.setRow(r);
        }
      }
    }
  }

  /**
   * Populates the columnSizes and rowSizes arrays, setting htmlLength in each
   * element.
   */
  private void createSizeArrays() {
    int numCols = 0;
    final ArrayList<Row> rows = this.ROWS;
    final int numRows = rows.size();

    {
      final RowSizeInfo[] rowSizes = new RowSizeInfo[numRows];
      this.rowSizes = rowSizes;
      for (int i = 0; i < numRows; i++) {
        final Row row = rows.get(i);
        final int numColsInThisRow = row.size();
        if (numColsInThisRow > numCols) {
          numCols = numColsInThisRow;
        }
        final RowSizeInfo rowSizeInfo = new RowSizeInfo();
        rowSizes[i] = rowSizeInfo;

        HtmlLength bestHeightLength = null;
        for (int x = 0; x < numColsInThisRow; x++) {
          final VirtualCell vc = row.get(x);
          if (vc != null) {
            final HtmlLength vcHeightLength = vc.getHeightLength();
            if ((vcHeightLength != null) && vcHeightLength.isPreferredOver(bestHeightLength)) {
              bestHeightLength = vcHeightLength;
            }
            rowSizeInfo.offsetX = maxRowGroupLeft;
          }
        }
        rowSizeInfo.htmlLength = bestHeightLength;

        @Nullable HtmlInsets rowGroupInsets = row.rowGroup.getGroupBorderInsets();
        if (row.firstInGroup && rowGroupInsets != null) {
          rowSizeInfo.marginTop = Math.max(0, rowGroupInsets.top);
        }
        if (row.lastInGroup && rowGroupInsets != null) {
          rowSizeInfo.marginBottom = Math.max(0, rowGroupInsets.bottom - row.maxCellBorderBottom);
        }
      }
    }

    final ColSizeInfo[] columnSizes = new ColSizeInfo[numCols];
    this.columnSizes = columnSizes;
    for (int i = 0; i < numCols; i++) {
      HtmlLength bestWidthLength = null;

      // Cells with colspan==1 first.
      for (int y = 0; y < numRows; y++) {
        final Row row = rows.get(y);
        VirtualCell vc;
        try {
          vc = row.get(i);
        } catch (final IndexOutOfBoundsException iob) {
          vc = null;
        }
        if (vc != null) {
          final RAbstractCell ac = vc.getActualCell();
          if (ac.getColSpan() == 1) {
            final HtmlLength vcWidthLength = vc.getWidthLength();
            if ((vcWidthLength != null) && vcWidthLength.isPreferredOver(bestWidthLength)) {
              bestWidthLength = vcWidthLength;
            }
          }
        }
      }
      // Now cells with colspan>1.
      if (bestWidthLength == null) {
        for (int y = 0; y < numRows; y++) {
          final Row row = rows.get(y);
          VirtualCell vc;
          try {
            vc = row.get(i);
          } catch (final IndexOutOfBoundsException iob) {
            vc = null;
          }
          if (vc != null) {
            final RAbstractCell ac = vc.getActualCell();
            if (ac.getColSpan() > 1) {
              final HtmlLength vcWidthLength = vc.getWidthLength();
              if ((vcWidthLength != null) && vcWidthLength.isPreferredOver(bestWidthLength)) {
                bestWidthLength = vcWidthLength;
              }
            }
          }
        }
      }
      final ColSizeInfo colSizeInfo = new ColSizeInfo();
      colSizeInfo.htmlLength = bestWidthLength;
      columnSizes[i] = colSizeInfo;
    }
  }

  /**
   * Determines the size of each column, and the table width. Does the
   * following:
   * <ol>
   * <li>Determine tentative widths. This is done by looking at declared column
   * widths, any table width, and filling in the blanks. No rendering is done.
   * The tentative width of columns with no declared width is zero.
   *
   * <li>Render all cell blocks. It uses the tentative widths from the previous
   * step as a desired width. The resulting width is considered a sort of
   * minimum. If the column width is not defined, use a NOWRAP override flag to
   * render.
   *
   * <li>Check if cell widths are too narrow for the rendered width. In the case
   * of columns without a declared width, check if they are too wide.
   *
   * <li>Finally, adjust widths considering the expected max table size. Columns
   * are layed out again if necessary to determine if they can really be shrunk.
   * </ol>
   *
   * @param renderState
   * @param border
   * @param cellSpacingX
   * @param cellSpacingY
   * @param availWidth
   */
  private void determineColumnSizes(final int hasBorder, final int cellSpacingX, final int cellSpacingY, final int availWidth) {
    final HtmlLength tableWidthLength = this.tableWidthLength;
    int tableWidth;
    boolean widthKnown;
    if (tableWidthLength != null) {
      tableWidth = tableWidthLength.getLength(availWidth);
      widthKnown = true;
    } else {
      tableWidth = availWidth;
      widthKnown = false;
    }
    tableWidth -= (this.maxRowGroupLeft + this.maxRowGroupRight) / 2;

    final ColSizeInfo[] columnSizes = this.columnSizes;
    final int widthsOfExtras = this.widthsOfExtras;
    int cellAvailWidth = tableWidth - widthsOfExtras;
    if (cellAvailWidth < 0) {
      tableWidth += (-cellAvailWidth);
      cellAvailWidth = 0;
    }

    // Determine tentative column widths based on specified cell widths

    determineTentativeSizes(columnSizes, widthsOfExtras, cellAvailWidth, widthKnown);

    // Pre-layout cells. This will give the minimum width of each cell,
    // in addition to the minimum height.

    this.preLayout(hasBorder, cellSpacingX, cellSpacingY, widthKnown);

    // Increases column widths if they are less than minimums of each cell.

    adjustForLayoutWidths(columnSizes, hasBorder, cellSpacingX, widthKnown);

    // Adjust for expected total width

    this.adjustWidthsForExpectedMax(columnSizes, cellAvailWidth, widthKnown);
  }

  /**
   * This method sets the tentative actual sizes of columns (rows) based on
   * specified widths (heights) if available.
   *
   * @param columnSizes
   * @param widthsOfExtras
   * @param cellAvailWidth
   */
  private static void determineTentativeSizes(final ColSizeInfo[] columnSizes, final int widthsOfExtras, final int cellAvailWidth,
      final boolean setNoWidthColumns) {
    final int numCols = columnSizes.length;

    // Look at percentages first
    int widthUsedByPercent = 0;
    for (int i = 0; i < numCols; i++) {
      final ColSizeInfo colSizeInfo = columnSizes[i];
      final HtmlLength widthLength = colSizeInfo.htmlLength;
      if ((widthLength != null) && (widthLength.getLengthType() == HtmlLength.LENGTH)) {
        final int actualSizeInt = widthLength.getLength(cellAvailWidth);
        widthUsedByPercent += actualSizeInt;
        colSizeInfo.actualSize = actualSizeInt;
      }
    }

    // Look at columns with absolute sizes
    int widthUsedByAbsolute = 0;
    int numNoWidthColumns = 0;
    for (int i = 0; i < numCols; i++) {
      final ColSizeInfo colSizeInfo = columnSizes[i];
      final HtmlLength widthLength = colSizeInfo.htmlLength;
      if ((widthLength != null) && (widthLength.getLengthType() != HtmlLength.LENGTH)) {
        // TODO: MULTI-LENGTH not supported
        final int actualSizeInt = widthLength.getRawValue();
        widthUsedByAbsolute += actualSizeInt;
        colSizeInfo.actualSize = actualSizeInt;
      } else if (widthLength == null) {
        numNoWidthColumns++;
      }
    }

    // Tentative width of all columns without a declared
    // width is set to zero. The pre-render will determine
    // a better size.

    // // Assign all columns without widths now
    // int widthUsedByUnspecified = 0;
    // if(setNoWidthColumns) {
    // int remainingWidth = cellAvailWidth - widthUsedByAbsolute -
    // widthUsedByPercent;
    // if(remainingWidth > 0) {
    // for(int i = 0; i < numCols; i++) {
    // SizeInfo colSizeInfo = columnSizes[i];
    // HtmlLength widthLength = colSizeInfo.htmlLength;
    // if(widthLength == null) {
    // int actualSizeInt = remainingWidth / numNoWidthColumns;
    // widthUsedByUnspecified += actualSizeInt;
    // colSizeInfo.actualSize = actualSizeInt;
    // }
    // }
    // }
    // }

    // Contract if necessary. This is done again later, but this is
    // an optimization, as it may prevent re-layout. It is only done
    // if all columns have some kind of declared width.

    if (numNoWidthColumns == 0) {
      int totalWidthUsed = widthUsedByPercent + widthUsedByAbsolute;
      int difference = totalWidthUsed - cellAvailWidth;
      // See if absolutes need to be contracted
      if (difference > 0) {
        if (widthUsedByAbsolute > 0) {
          int expectedAbsoluteWidthTotal = widthUsedByAbsolute - difference;
          if (expectedAbsoluteWidthTotal < 0) {
            expectedAbsoluteWidthTotal = 0;
          }
          final double ratio = (double) expectedAbsoluteWidthTotal / widthUsedByAbsolute;
          for (int i = 0; i < numCols; i++) {
            final ColSizeInfo sizeInfo = columnSizes[i];
            final HtmlLength widthLength = columnSizes[i].htmlLength;
            if ((widthLength != null) && (widthLength.getLengthType() != HtmlLength.LENGTH)) {
              final int oldActualSize = sizeInfo.actualSize;
              final int newActualSize = (int) Math.round(oldActualSize * ratio);
              sizeInfo.actualSize = newActualSize;
              totalWidthUsed += (newActualSize - oldActualSize);
            }
          }
          difference = totalWidthUsed - cellAvailWidth;
        }

        // See if percentages need to be contracted
        if (difference > 0) {
          if (widthUsedByPercent > 0) {
            int expectedPercentWidthTotal = widthUsedByPercent - difference;
            if (expectedPercentWidthTotal < 0) {
              expectedPercentWidthTotal = 0;
            }
            final double ratio = (double) expectedPercentWidthTotal / widthUsedByPercent;
            for (int i = 0; i < numCols; i++) {
              final ColSizeInfo sizeInfo = columnSizes[i];
              final HtmlLength widthLength = columnSizes[i].htmlLength;
              if ((widthLength != null) && (widthLength.getLengthType() == HtmlLength.LENGTH)) {
                final int oldActualSize = sizeInfo.actualSize;
                final int newActualSize = (int) Math.round(oldActualSize * ratio);
                sizeInfo.actualSize = newActualSize;
                totalWidthUsed += (newActualSize - oldActualSize);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Expands column sizes according to layout sizes.
   */
  private static void adjustForLayoutWidths(final ColSizeInfo[] columnSizes, final int hasBorder, final int cellSpacing,
      final boolean tableWidthKnown) {
    final int numCols = columnSizes.length;
    for (int i = 0; i < numCols; i++) {
      final ColSizeInfo si = columnSizes[i];
      if (si.actualSize < si.layoutSize) {
        si.actualSize = si.layoutSize;
      }
      if (si.fullActualSize < si.fullLayoutSize) {
        si.fullActualSize = si.fullLayoutSize;
      }
      // else if(si.htmlLength == null) {
      // // For cells without a declared width, see if
      // // their tentative width is a bit too big.
      // if(si.actualSize > si.layoutSize) {
      // si.actualSize = si.layoutSize;
      // }
      // }
    }
  }

  private void layoutColumn(final ColSizeInfo[] columnSizes, final ColSizeInfo colSize, final int col, final int cellSpacingX, final int hasBorder) {
    final RowSizeInfo[] rowSizes = this.rowSizes;
    final ArrayList<Row> rows = this.ROWS;
    final int numRows = rows.size();
    final int actualSize = colSize.actualSize;
    colSize.layoutSize = 0;
    for (int rowIndx = 0; rowIndx < numRows;) {
      // SizeInfo rowSize = rowSizes[row];
      final Row row = rows.get(rowIndx);
      VirtualCell vc = null;
      try {
        vc = row.get(col);
      } catch (final IndexOutOfBoundsException iob) {
        vc = null;
      }
      final RAbstractCell ac = vc == null ? null : vc.getActualCell();
      if (ac != null) {
        if (ac.getVirtualRow() == rowIndx) {
          // Only process actual cells with a row
          // beginning at the current row being processed.
          final int colSpan = ac.getColSpan();
          if (colSpan > 1) {
            final int firstCol = ac.getVirtualColumn();
            final int cellExtras = (colSpan - 1) * (cellSpacingX + (2 * hasBorder));
            int vcActualWidth = cellExtras;
            for (int x = 0; x < colSpan; x++) {
              vcActualWidth += columnSizes[firstCol + x].actualSize;
            }
            // TODO: better height possible
            final Dimension size = ac.doCellLayout(vcActualWidth, 0, true, true, true);
            final int vcRenderWidth = size.width;

            final int denominator = (vcActualWidth - cellExtras);
            int newTentativeCellWidth;
            if (denominator > 0) {
              newTentativeCellWidth = (actualSize * (vcRenderWidth - cellExtras)) / denominator;
            } else {
              newTentativeCellWidth = (vcRenderWidth - cellExtras) / colSpan;
            }
            if (newTentativeCellWidth > colSize.layoutSize) {
              colSize.layoutSize = newTentativeCellWidth;
            }
            final int rowSpan = ac.getRowSpan();
            final int vch = (size.height - ((rowSpan - 1) * (this.cellSpacingY + (2 * hasBorder)))) / rowSpan;
            for (int y = 0; y < rowSpan; y++) {
              if (rowSizes[rowIndx + y].minSize < vch) {
                rowSizes[rowIndx + y].minSize = vch;
              }
            }
          } else {
            // TODO: better height possible
            final Dimension size = ac.doCellLayout(actualSize, 0, true, true, true);
            if (size.width > colSize.layoutSize) {
              colSize.layoutSize = size.width;
            }

            @NonNull Insets cbi = ac.getBorderInsets();
            final int cellFullLayoutWidth = size.width + cbi.left + cbi.right;
            if (cellFullLayoutWidth > colSize.fullLayoutSize) {
              colSize.fullLayoutSize = cellFullLayoutWidth;
            }

            final int rowSpan = ac.getRowSpan();
            final int vch = (size.height - ((rowSpan - 1) * (this.cellSpacingY + (2 * hasBorder)))) / rowSpan;
            for (int y = 0; y < rowSpan; y++) {
              if (rowSizes[rowIndx + y].minSize < vch) {
                rowSizes[rowIndx + y].minSize = vch;
              }
            }
          }
        }
      }
      // row = (ac == null ? row + 1 : ac.getVirtualRow() + ac.getRowSpan());
      rowIndx++;
    }
  }

  private int adjustWidthsForExpectedMax(final ColSizeInfo[] columnSizes, final int cellAvailWidth, final boolean expand) {
    final int hasBorder = this.hasOldStyleBorder;
    final int cellSpacingX = this.cellSpacingX;
    int currentTotal = 0;
    final int numCols = columnSizes.length;
    for (int i = 0; i < numCols; i++) {
      currentTotal += columnSizes[i].fullActualSize;
    }
    int difference = currentTotal - (this.widthsOfExtras + cellAvailWidth);
    // int difference = currentTotal - (cellAvailWidth);
    if ((difference > 0) || ((difference < 0) && expand)) {
      // First, try to contract/expand columns with no width
      int noWidthTotal = 0;
      int numNoWidth = 0;
      for (int i = 0; i < numCols; i++) {
        if (columnSizes[i].htmlLength == null) {
          numNoWidth++;
          noWidthTotal += columnSizes[i].fullActualSize;
        }
      }
      if (numNoWidth > 0) {
        // TODO: This is not shrinking correctly.
        int expectedNoWidthTotal = noWidthTotal - difference - this.widthsOfExtras;
        if (expectedNoWidthTotal < 0) {
          expectedNoWidthTotal = 0;
        }
        final double ratio = ((double) expectedNoWidthTotal) / noWidthTotal;
        int noWidthCount = 0;
        for (int i = 0; i < numCols; i++) {
          final ColSizeInfo sizeInfo = columnSizes[i];
          if (sizeInfo.htmlLength == null) {
            final int oldActualSize = sizeInfo.fullActualSize;
            int newActualSize;
            if (++noWidthCount == numNoWidth) {
              // Last column without a width.
              final int currentDiff = currentTotal - cellAvailWidth;
              newActualSize = oldActualSize - currentDiff;
              if (newActualSize < 0) {
                newActualSize = 0;
              }
            } else {
              newActualSize = (int) Math.round(oldActualSize * ratio);
            }
            sizeInfo.actualSize = newActualSize;
            if (newActualSize < sizeInfo.fullLayoutSize) {
              // See if it actually fits.
              this.layoutColumn(columnSizes, sizeInfo, i, cellSpacingX, hasBorder);
              if (newActualSize < sizeInfo.layoutSize) {
                // Didn't fit.
                newActualSize = sizeInfo.layoutSize;
                sizeInfo.actualSize = newActualSize;
              }
            }
            currentTotal += (newActualSize - oldActualSize);
          }
        }
        difference = currentTotal - cellAvailWidth;
      }

      // See if absolutes need to be contracted
      if ((difference > 0) || ((difference < 0) && expand)) {
        int absoluteWidthTotal = 0;
        for (int i = 0; i < numCols; i++) {
          final HtmlLength widthLength = columnSizes[i].htmlLength;
          if ((widthLength != null) && (widthLength.getLengthType() != HtmlLength.LENGTH)) {
            absoluteWidthTotal += columnSizes[i].fullActualSize;
          }
        }
        if (absoluteWidthTotal > 0) {
          int expectedAbsoluteWidthTotal = absoluteWidthTotal - difference - this.widthsOfExtras;
          if (expectedAbsoluteWidthTotal < 0) {
            expectedAbsoluteWidthTotal = 0;
          }
          final double ratio = ((double) expectedAbsoluteWidthTotal) / absoluteWidthTotal;
          for (int i = 0; i < numCols; i++) {
            final ColSizeInfo sizeInfo = columnSizes[i];
            final HtmlLength widthLength = columnSizes[i].htmlLength;
            if ((widthLength != null) && (widthLength.getLengthType() != HtmlLength.LENGTH)) {
              final int oldActualSize = sizeInfo.fullActualSize;
              int newActualSize = (int) Math.round(oldActualSize * ratio);
              sizeInfo.actualSize = newActualSize;
              if (newActualSize < sizeInfo.fullLayoutSize) {
                // See if it actually fits.
                this.layoutColumn(columnSizes, sizeInfo, i, cellSpacingX, hasBorder);
                if (newActualSize < sizeInfo.layoutSize) {
                  // Didn't fit.
                  newActualSize = sizeInfo.layoutSize;
                  sizeInfo.actualSize = newActualSize;
                }
              }
              currentTotal += (newActualSize - oldActualSize);
            }
          }
          difference = currentTotal - cellAvailWidth;
        }

        // See if percentages need to be contracted
        if ((difference > 0) || ((difference < 0) && expand)) {
          int percentWidthTotal = 0;
          for (int i = 0; i < numCols; i++) {
            final HtmlLength widthLength = columnSizes[i].htmlLength;
            if ((widthLength != null) && (widthLength.getLengthType() == HtmlLength.LENGTH)) {
              percentWidthTotal += columnSizes[i].actualSize;
            }
          }
          if (percentWidthTotal > 0) {
            int expectedPercentWidthTotal = percentWidthTotal - difference;
            if (expectedPercentWidthTotal < 0) {
              expectedPercentWidthTotal = 0;
            }
            final double ratio = (double) expectedPercentWidthTotal / percentWidthTotal;
            for (int i = 0; i < numCols; i++) {
              final ColSizeInfo sizeInfo = columnSizes[i];
              final HtmlLength widthLength = columnSizes[i].htmlLength;
              if ((widthLength != null) && (widthLength.getLengthType() == HtmlLength.LENGTH)) {
                final int oldActualSize = sizeInfo.actualSize;
                int newActualSize = (int) Math.round(oldActualSize * ratio);
                sizeInfo.actualSize = newActualSize;
                if (newActualSize < sizeInfo.layoutSize) {
                  // See if it actually fits.
                  this.layoutColumn(columnSizes, sizeInfo, i, cellSpacingX, hasBorder);
                  if (newActualSize < sizeInfo.layoutSize) {
                    // Didn't fit.
                    newActualSize = sizeInfo.layoutSize;
                    sizeInfo.actualSize = newActualSize;
                  }
                }
                currentTotal += (newActualSize - oldActualSize);
              }
            }
          }
        }
      }
    } else {
      if (expand) {
        for (int i = 0; i < numCols; i++) {
          final ColSizeInfo sizeInfo = columnSizes[i];
          sizeInfo.actualSize = sizeInfo.fullActualSize;
        }
      }
    }
    return currentTotal;
  }

  /**
   * This method renders each cell using already set actual column widths. It
   * sets minimum row heights based on this.
   */
  private final void preLayout(final int hasBorder, final int cellSpacingX, final int cellSpacingY, final boolean tableWidthKnown) {
    // TODO: Fix for table without width that has a subtable with width=100%.
    // TODO: Maybe it can be addressed when NOWRAP is implemented.
    // TODO: Maybe it's possible to eliminate this pre-layout altogether.

    final ColSizeInfo[] colSizes = this.columnSizes;
    final RowSizeInfo[] rowSizes = this.rowSizes;

    // Initialize minSize in rows
    final int numRows = rowSizes.length;
    for (int i = 0; i < numRows; i++) {
      rowSizes[i].minSize = 0;
    }

    // Initialize layoutSize in columns
    final int numCols = colSizes.length;
    for (int i = 0; i < numCols; i++) {
      colSizes[i].layoutSize = 0;
      colSizes[i].fullLayoutSize = 0;
    }

    for (@NonNull RAbstractCell cell: this.ALL_CELLS) {
      final int col = cell.getVirtualColumn();
      final int colSpan = cell.getColSpan();
      int cellsTotalWidth;
      int cellsUsedWidth;
      boolean widthDeclared = false;
      if (colSpan > 1) {
        cellsUsedWidth = 0;
        for (int x = 0; x < colSpan; x++) {
          final ColSizeInfo colSize = colSizes[col + x];
          if (colSize.htmlLength != null) {
            widthDeclared = true;
          }
          cellsUsedWidth += colSize.actualSize;
        }
        cellsTotalWidth = cellsUsedWidth + ((colSpan - 1) * (cellSpacingX + (2 * hasBorder)));
      } else {
        final ColSizeInfo colSize = colSizes[col];
        if (colSize.htmlLength != null) {
          widthDeclared = true;
        }
        cellsUsedWidth = cellsTotalWidth = colSize.actualSize;
      }

      // TODO: A tentative height could be used here: Height of
      // table divided by number of rows.

      java.awt.Dimension size;
      final RenderThreadState state = RenderThreadState.getState();
      final boolean prevOverrideNoWrap = state.overrideNoWrap;
      try {
        if (!prevOverrideNoWrap) {
          state.overrideNoWrap = !widthDeclared;
        }
        size = cell.doCellLayout(cellsTotalWidth, 0, true, true, true);
      } finally {
        state.overrideNoWrap = prevOverrideNoWrap;
      }
      // Set render widths
      final int cellLayoutWidth = size.width;
      @NonNull Insets cbi = cell.getBorderInsets();
      final int cellFullLayoutWidth = size.width + cbi.left + cbi.right;
      if (colSpan > 1) {
        // TODO: set fullLayoutSize
        if (cellsUsedWidth > 0) {
          final double ratio = (double) cellLayoutWidth / cellsUsedWidth;
          for (int x = 0; x < colSpan; x++) {
            final ColSizeInfo si = colSizes[col + x];
            final int newLayoutSize = (int) Math.round(si.actualSize * ratio);
            if (si.layoutSize < newLayoutSize) {
              si.layoutSize = newLayoutSize;
            }
          }
        } else {
          final int newLayoutSize = cellLayoutWidth / colSpan;
          for (int x = 0; x < colSpan; x++) {
            final ColSizeInfo si = colSizes[col + x];
            if (si.layoutSize < newLayoutSize) {
              si.layoutSize = newLayoutSize;
            }
          }
        }
      } else {
        final ColSizeInfo colSizeInfo = colSizes[col];
        if (colSizeInfo.layoutSize < cellLayoutWidth) {
          colSizeInfo.layoutSize = cellLayoutWidth;
        }
        if (colSizeInfo.fullLayoutSize < cellFullLayoutWidth) {
          colSizeInfo.fullLayoutSize = cellFullLayoutWidth;
        }
      }

      // Set minimum heights
      final int actualCellHeight = size.height;
      final int row = cell.getVirtualRow();
      final int rowSpan = cell.getRowSpan();
      if (rowSpan > 1) {
        final int vch = (actualCellHeight - ((rowSpan - 1) * (cellSpacingY + (2 * hasBorder)))) / rowSpan;
        for (int y = 0; y < rowSpan; y++) {
          if (rowSizes[row + y].minSize < vch) {
            rowSizes[row + y].minSize = vch;
          }
        }
      } else {
        if (rowSizes[row].minSize < actualCellHeight) {
          rowSizes[row].minSize = actualCellHeight;
        }
      }
    }
  }

  private void determineRowSizes(final int hasBorder, final int cellSpacing, final int availHeight, final boolean sizeOnly) {
    final HtmlLength tableHeightLength = TableMatrix.getHeightLength(this.tableElement, availHeight);
    int tableHeight;
    final RowSizeInfo[] rowSizes = this.rowSizes;
    final int numRows = rowSizes.length;
    final int heightsOfExtras = this.heightsOfExtras;
    if (tableHeightLength != null) {
      tableHeight = tableHeightLength.getLength(availHeight);
      this.determineRowSizesFixedTH(hasBorder, cellSpacing, availHeight, tableHeight, sizeOnly);
    } else {
      tableHeight = heightsOfExtras;
      for (int row = 0; row < numRows; row++) {
        tableHeight += rowSizes[row].minSize;
      }
      this.determineRowSizesFlexibleTH(hasBorder, cellSpacing, availHeight, sizeOnly);
    }
  }

  private void determineRowSizesFixedTH(final int hasBorder, final int cellSpacing, final int availHeight, final int tableHeight,
      final boolean sizeOnly) {
    final RowSizeInfo[] rowSizes = this.rowSizes;
    final int numRows = rowSizes.length;
    final int heightsOfExtras = this.heightsOfExtras;
    int cellAvailHeight = tableHeight - heightsOfExtras;
    if (cellAvailHeight < 0) {
      cellAvailHeight = 0;
    }

    // Look at percentages first

    int heightUsedbyPercent = 0;
    int otherMinSize = 0;
    for (int i = 0; i < numRows; i++) {
      final RowSizeInfo rowSizeInfo = rowSizes[i];
      final HtmlLength heightLength = rowSizeInfo.htmlLength;
      if ((heightLength != null) && (heightLength.getLengthType() == HtmlLength.LENGTH)) {
        int actualSizeInt = heightLength.getLength(cellAvailHeight);
        if (actualSizeInt < rowSizeInfo.minSize) {
          actualSizeInt = rowSizeInfo.minSize;
        }
        heightUsedbyPercent += actualSizeInt;
        rowSizeInfo.actualSize = actualSizeInt;
      } else {
        otherMinSize += rowSizeInfo.minSize;
      }
    }

    // Check if rows with percent are bigger than they should be

    if ((heightUsedbyPercent + otherMinSize) > cellAvailHeight) {
      final double ratio = (double) (cellAvailHeight - otherMinSize) / heightUsedbyPercent;
      for (int i = 0; i < numRows; i++) {
        final RowSizeInfo rowSizeInfo = rowSizes[i];
        final HtmlLength heightLength = rowSizeInfo.htmlLength;
        if ((heightLength != null) && (heightLength.getLengthType() == HtmlLength.LENGTH)) {
          final int actualSize = rowSizeInfo.actualSize;
          final int prevActualSize = actualSize;
          int newActualSize = (int) Math.round(prevActualSize * ratio);
          if (newActualSize < rowSizeInfo.minSize) {
            newActualSize = rowSizeInfo.minSize;
          }
          heightUsedbyPercent += (newActualSize - prevActualSize);
          rowSizeInfo.actualSize = newActualSize;
        }
      }
    }

    // Look at rows with absolute sizes

    int heightUsedByAbsolute = 0;
    int noHeightMinSize = 0;
    int numNoHeightColumns = 0;
    for (int i = 0; i < numRows; i++) {
      final RowSizeInfo rowSizeInfo = rowSizes[i];
      final HtmlLength heightLength = rowSizeInfo.htmlLength;
      if ((heightLength != null) && (heightLength.getLengthType() != HtmlLength.LENGTH)) {
        // TODO: MULTI-LENGTH not supported
        int actualSizeInt = heightLength.getRawValue();
        if (actualSizeInt < rowSizeInfo.minSize) {
          actualSizeInt = rowSizeInfo.minSize;
        }
        heightUsedByAbsolute += actualSizeInt;
        rowSizeInfo.actualSize = actualSizeInt;
      } else if (heightLength == null) {
        numNoHeightColumns++;
        noHeightMinSize += rowSizeInfo.minSize;
      }
    }

    // Check if absolute sizing is too much

    if ((heightUsedByAbsolute + heightUsedbyPercent + noHeightMinSize) > cellAvailHeight) {
      final double ratio = (double) (cellAvailHeight - noHeightMinSize - heightUsedbyPercent) / heightUsedByAbsolute;
      for (int i = 0; i < numRows; i++) {
        final RowSizeInfo rowSizeInfo = rowSizes[i];
        final HtmlLength heightLength = rowSizeInfo.htmlLength;
        if ((heightLength != null) && (heightLength.getLengthType() != HtmlLength.LENGTH)) {
          final int actualSize = rowSizeInfo.actualSize;
          final int prevActualSize = actualSize;
          int newActualSize = (int) Math.round(prevActualSize * ratio);
          if (newActualSize < rowSizeInfo.minSize) {
            newActualSize = rowSizeInfo.minSize;
          }
          heightUsedByAbsolute += (newActualSize - prevActualSize);
          rowSizeInfo.actualSize = newActualSize;
        }
      }
    }

    // Assign all rows without heights now

    final int remainingHeight = cellAvailHeight - heightUsedByAbsolute - heightUsedbyPercent;
    int heightUsedByRemaining = 0;
    for (int i = 0; i < numRows; i++) {
      final RowSizeInfo rowSizeInfo = rowSizes[i];
      final HtmlLength heightLength = rowSizeInfo.htmlLength;
      if (heightLength == null) {
        int actualSizeInt = remainingHeight / numNoHeightColumns;
        if (actualSizeInt < rowSizeInfo.minSize) {
          actualSizeInt = rowSizeInfo.minSize;
        }
        heightUsedByRemaining += actualSizeInt;
        rowSizeInfo.actualSize = actualSizeInt;
      }
    }

    // Calculate actual table width

    final int totalUsed = heightUsedByAbsolute + heightUsedbyPercent + heightUsedByRemaining;
    if (totalUsed >= cellAvailHeight) {
      this.tableHeight = totalUsed + heightsOfExtras;
    } else {
      // Rows too short; expand them
      final double ratio = (double) cellAvailHeight / totalUsed;
      for (int i = 0; i < numRows; i++) {
        final RowSizeInfo rowSizeInfo = rowSizes[i];
        final int actualSize = rowSizeInfo.actualSize;
        rowSizeInfo.actualSize = (int) Math.round(actualSize * ratio);
      }
      this.tableHeight = tableHeight;
    }

    // TODO:
    // This final render is probably unnecessary. Avoid exponential rendering
    // by setting a single height of subcell. Verify that IE only sets height
    // of subcells when height of row or table are specified.

    this.finalLayout(hasBorder, cellSpacing, sizeOnly);
  }

  private void determineRowSizesFlexibleTH(final int hasBorder, final int cellSpacing, final int availHeight, final boolean sizeOnly) {
    final RowSizeInfo[] rowSizes = this.rowSizes;
    final int numRows = rowSizes.length;
    final int heightsOfExtras = this.heightsOfExtras;

    // Look at rows with absolute sizes
    int heightUsedByAbsolute = 0;
    int percentSum = 0;
    for (int i = 0; i < numRows; i++) {
      final RowSizeInfo rowSizeInfo = rowSizes[i];
      final HtmlLength heightLength = rowSizeInfo.htmlLength;
      if ((heightLength != null) && (heightLength.getLengthType() == HtmlLength.PIXELS)) {
        // TODO: MULTI-LENGTH not supported
        int actualSizeInt = heightLength.getRawValue();
        if (actualSizeInt < rowSizeInfo.minSize) {
          actualSizeInt = rowSizeInfo.minSize;
        }
        heightUsedByAbsolute += actualSizeInt;
        rowSizeInfo.actualSize = actualSizeInt;
      } else if ((heightLength != null) && (heightLength.getLengthType() == HtmlLength.LENGTH)) {
        percentSum += heightLength.getRawValue();
      }
    }

    // Look at rows with no specified heights
    int heightUsedByNoSize = 0;

    // Set sizes to in row height
    for (int i = 0; i < numRows; i++) {
      final RowSizeInfo rowSizeInfo = rowSizes[i];
      final HtmlLength widthLength = rowSizeInfo.htmlLength;
      if (widthLength == null) {
        final int actualSizeInt = rowSizeInfo.minSize;
        heightUsedByNoSize += actualSizeInt;
        rowSizeInfo.actualSize = actualSizeInt;
      }
    }

    // Calculate actual total cell width
    final int expectedTotalCellHeight = (int) Math.round((heightUsedByAbsolute + heightUsedByNoSize) / (1 - (percentSum / 100.0)));

    // Set widths of columns with percentages
    int heightUsedByPercent = 0;
    for (int i = 0; i < numRows; i++) {
      final RowSizeInfo rowSizeInfo = rowSizes[i];
      final HtmlLength heightLength = rowSizeInfo.htmlLength;
      if ((heightLength != null) && (heightLength.getLengthType() == HtmlLength.LENGTH)) {
        int actualSizeInt = heightLength.getLength(expectedTotalCellHeight);
        if (actualSizeInt < rowSizeInfo.minSize) {
          actualSizeInt = rowSizeInfo.minSize;
        }
        heightUsedByPercent += actualSizeInt;
        rowSizeInfo.actualSize = actualSizeInt;
      }
    }

    // Set width of table
    this.tableHeight = heightUsedByAbsolute + heightUsedByNoSize + heightUsedByPercent + heightsOfExtras;

    // Do a final layouts to set actual cell sizes
    this.finalLayout(hasBorder, cellSpacing, sizeOnly);
  }

  /**
   * This method layouts each cell using already set actual column widths. It
   * sets minimum row heights based on this.
   */
  private final void finalLayout(final int hasBorder, final int cellSpacing, final boolean sizeOnly) {
    // finalLayout needs to adjust actualSize of columns and rows
    // given that things might change as we layout one last time.
    final ColSizeInfo[] colSizes = this.columnSizes;
    final RowSizeInfo[] rowSizes = this.rowSizes;
    for (@NonNull RAbstractCell cell : this.ALL_CELLS) {
      final int col = cell.getVirtualColumn();
      final int colSpan = cell.getColSpan();
      int totalCellWidth;
      if (colSpan > 1) {
        totalCellWidth = (colSpan - 1) * (cellSpacing + (2 * hasBorder));
        for (int x = 0; x < colSpan; x++) {
          totalCellWidth += colSizes[col + x].actualSize;
        }
      } else {
        totalCellWidth = colSizes[col].actualSize;
      }
      final int row = cell.getVirtualRow();
      final int rowSpan = cell.getRowSpan();
      int totalCellHeight;
      if (rowSpan > 1) {
        totalCellHeight = (rowSpan - 1) * (cellSpacing + (2 * hasBorder));
        for (int y = 0; y < rowSpan; y++) {
          totalCellHeight += rowSizes[row + y].actualSize;
        }
      } else {
        totalCellHeight = rowSizes[row].actualSize;
      }
      final Dimension size = cell.doCellLayout(totalCellWidth, totalCellHeight, true, true, sizeOnly);
      if (size.width > totalCellWidth) {
        if (colSpan == 1) {
          colSizes[col].actualSize = size.width;
        } else {
          colSizes[col].actualSize += (size.width - totalCellWidth);
        }
      }
      if (size.height > totalCellHeight) {
        if (rowSpan == 1) {
          rowSizes[row].actualSize = size.height;
        } else {
          rowSizes[row].actualSize += (size.height - totalCellHeight);
        }
      }
    }
  }

  // public final void adjust() {
  // // finalRender needs to adjust actualSize of columns and rows
  // // given that things might change as we render one last time.
  // int hasBorder = this.hasOldStyleBorder;
  // int cellSpacingX = this.cellSpacingX;
  // int cellSpacingY = this.cellSpacingY;
  // ArrayList allCells = this.ALL_CELLS;
  // SizeInfo[] colSizes = this.columnSizes;
  // SizeInfo[] rowSizes = this.rowSizes;
  // int numCells = allCells.size();
  // for(int i = 0; i < numCells; i++) {
  // RTableCell cell = (RTableCell) allCells.get(i);
  // int col = cell.getVirtualColumn();
  // int colSpan = cell.getColSpan();
  // int totalCellWidth;
  // if(colSpan > 1) {
  // totalCellWidth = (colSpan - 1) * (cellSpacingX + 2 * hasBorder);
  // for(int x = 0; x < colSpan; x++) {
  // totalCellWidth += colSizes[col + x].actualSize;
  // }
  // }
  // else {
  // totalCellWidth = colSizes[col].actualSize;
  // }
  // int row = cell.getVirtualRow();
  // int rowSpan = cell.getRowSpan();
  // int totalCellHeight;
  // if(rowSpan > 1) {
  // totalCellHeight = (rowSpan - 1) * (cellSpacingY + 2 * hasBorder);
  // for(int y = 0; y < rowSpan; y++) {
  // totalCellHeight += rowSizes[row + y].actualSize;
  // }
  // }
  // else {
  // totalCellHeight = rowSizes[row].actualSize;
  // }
  // cell.adjust();
  // Dimension size = cell.getSize();
  // if(size.width > totalCellWidth) {
  // if(colSpan == 1) {
  // colSizes[col].actualSize = size.width;
  // }
  // else {
  // colSizes[col].actualSize += (size.width - totalCellWidth);
  // }
  // }
  // if(size.height > totalCellHeight) {
  // if(rowSpan == 1) {
  // rowSizes[row].actualSize = size.height;
  // }
  // else {
  // rowSizes[row].actualSize += (size.height - totalCellHeight);
  // }
  // }
  // }
  // }

  /**
   * Sets bounds of each cell's component, and sums up table width and height.
   */
  public final void doLayout(final Insets insets) {

    // Set row offsets

    final RowSizeInfo[] rowSizes = this.rowSizes;
    final int numRows = rowSizes.length;
    int yoffset = insets.top;
    final int cellSpacingY = this.cellSpacingY;
    final int hasBorder = this.hasOldStyleBorder;
    for (int i = 0; i < numRows; i++) {
      yoffset += cellSpacingY;
      yoffset += hasBorder;
      final RowSizeInfo rowSizeInfo = rowSizes[i];
      yoffset += rowSizeInfo.marginTop;
      rowSizeInfo.offsetY = yoffset;
      rowSizeInfo.insetLeft = insets.left;
      rowSizeInfo.insetRight = insets.right;
      yoffset += rowSizeInfo.actualSize;
      yoffset += hasBorder;
      yoffset += rowSizeInfo.marginBottom;
    }
    this.tableHeight = yoffset + cellSpacingY + insets.bottom;

    // Set column offsets

    final ColSizeInfo[] colSizes = this.columnSizes;
    final int numColumns = colSizes.length;
    int xoffset = insets.left;
    final int cellSpacingX = this.cellSpacingX;
    for (int i = 0; i < numColumns; i++) {
      xoffset += cellSpacingX;
      xoffset += hasBorder;
      final ColSizeInfo colSizeInfo = colSizes[i];
      colSizeInfo.offsetX = xoffset;
      xoffset += colSizeInfo.actualSize;
      xoffset += hasBorder;
    }
    this.tableWidth = xoffset + cellSpacingX + insets.right + (maxRowGroupRight / 2);

    // Set offsets of each cell

    for (@NonNull RAbstractCell cell : this.ALL_CELLS) {
      cell.setCellBounds(colSizes, rowSizes, hasBorder, cellSpacingX, cellSpacingY);
    }
    this.rowGroupSizes = prepareRowGroupSizes();
  }

  static class RTableRowGroup extends BaseElementRenderable {

    public RTableRowGroup(RenderableContainer container, ModelNode modelNode, UserAgentContext ucontext, final BorderOverrider borderOverrider) {
      super(container, modelNode, ucontext);
      this.borderOverrider.copyFrom(borderOverrider);
    }

    @Override
    public Iterator<@NonNull ? extends Renderable> getRenderables(boolean topFirst) {
      return null;
    }

    @Override
    public RenderableSpot getLowestRenderableSpot(int x, int y) {
      return null;
    }

    @Override
    public boolean onMouseReleased(MouseEvent event, int x, int y) {
      return false;
    }

    @Override
    public boolean onMouseDisarmed(MouseEvent event) {
      return false;
    }

    @Override
    public boolean onDoubleClick(MouseEvent event, int x, int y) {
      return false;
    }

    @Override
    public void repaint() {
      container.repaint(x, y, width, height);
    }

    @Override
    public void repaint(ModelNode modelNode) {
      // TODO Auto-generated method stub
    }

    @Override
    public Color getPaintedBackgroundColor() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    protected void paintShifted(Graphics g) {
      // TODO Auto-generated method stub
    }

    @Override
    protected void doLayout(int availWidth, int availHeight, boolean sizeOnly) {
      // TODO Auto-generated method stub
    }

    @Override
    public @NonNull Insets getBorderInsets() {
      return borderOverrider.get(super.getBorderInsets());
    }
  }

  public final void paint(final Graphics g, final Dimension size) {
    // Paint row group backgrounds
    for (final RowGroupSizeInfo rgsi : rowGroupSizes) {
      rgsi.prePaintBackground(g);
    }

    for (final @NonNull RAbstractCell cell : this.ALL_CELLS) {
      // Should clip table cells, just in case.
      final Graphics newG = g.create(cell.x, cell.y, cell.width, cell.height);
      try {
        cell.paint(newG);
      } finally {
        newG.dispose();
      }
    }

    if (this.hasOldStyleBorder > 0) {
      // // Paint table border
      //
      // int tableWidth = this.tableWidth;
      // int tableHeight = this.tableHeight;
      // g.setColor(Color.BLACK); //TODO: Actual border color
      // int x = insets.left;
      // int y = insets.top;
      // for(int i = 0; i < border; i++) {
      // g.drawRect(x + i, y + i, tableWidth - i * 2 - 1, tableHeight - i * 2 -
      // 1);
      // }

      // Paint cell borders

      g.setColor(Color.GRAY);
      for (@NonNull RAbstractCell cell : this.ALL_CELLS) {
        final int cx = cell.getX() - 1;
        final int cy = cell.getY() - 1;
        final int cwidth = cell.getWidth() + 1;
        final int cheight = cell.getHeight() + 1;
        g.drawRect(cx, cy, cwidth, cheight);
      }
    }

    // Paint row group borders
    for (final RowGroupSizeInfo rgsi : rowGroupSizes) {
      rgsi.prePaintBorder(g);
    }
  }

  // Called during paint
  private ArrayList<RowGroupSizeInfo> prepareRowGroupSizes() {
    final ArrayList<RowGroupSizeInfo> rowGroupSizes = new ArrayList<>();
    {
      final RowSizeInfo[] rowSizesLocal = this.rowSizes;
      for (final RowGroup rowGroup : this.ROW_GROUPS) {
        if (rowGroup.rowGroupElem != null) {
          final Row firstRow = rowGroup.rows.get(0);
          final Row lastRow = rowGroup.rows.get(rowGroup.rows.size() - 1);
          final RowSizeInfo firstRowSize = rowSizesLocal[firstRow.rowIndex];
          final RowSizeInfo lastRowSize = rowSizesLocal[lastRow.rowIndex];

          final int groupHeight = lastRowSize.actualSize + lastRowSize.offsetY - (firstRowSize.offsetY);
          final int groupWidth = this.tableWidth - (firstRowSize.insetRight + firstRowSize.insetLeft);
          final RTableRowGroup rRowGroup = new RTableRowGroup(this.container, firstRow.rowGroupElem, this.uaContext,
              rowGroup.borderOverrider);
          final int x = firstRowSize.offsetX + firstRowSize.insetLeft;
          final int y = firstRowSize.offsetY;
          rRowGroup.setX(x);
          rRowGroup.setY(y);
          rRowGroup.setWidth(groupWidth);
          rRowGroup.setHeight(groupHeight);
          rRowGroup.applyStyle(groupWidth, groupHeight, true);
          final RowGroupSizeInfo rgsi = new RowGroupSizeInfo(groupWidth, groupHeight, rRowGroup, x, y);
          rowGroupSizes.add(rgsi);
        }
      }
    }
    return rowGroupSizes;
  }

  // public boolean paintSelection(Graphics g, boolean inSelection,
  // RenderableSpot startPoint, RenderableSpot endPoint) {
  // ArrayList allCells = this.ALL_CELLS;
  // int numCells = allCells.size();
  // for(int i = 0; i < numCells; i++) {
  // RTableCell cell = (RTableCell) allCells.get(i);
  // Rectangle bounds = cell.getBounds();
  // int offsetX = bounds.x;
  // int offsetY = bounds.y;
  // g.translate(offsetX, offsetY);
  // try {
  // boolean newInSelection = cell.paintSelection(g, inSelection, startPoint,
  // endPoint);
  // if(inSelection && !newInSelection) {
  // return false;
  // }
  // inSelection = newInSelection;
  // } finally {
  // g.translate(-offsetX, -offsetY);
  // }
  // }
  // return inSelection;
  // }
  //
  // public boolean extractSelectionText(StringBuffer buffer, boolean
  // inSelection, RenderableSpot startPoint, RenderableSpot endPoint) {
  // ArrayList allCells = this.ALL_CELLS;
  // int numCells = allCells.size();
  // for(int i = 0; i < numCells; i++) {
  // RTableCell cell = (RTableCell) allCells.get(i);
  // boolean newInSelection = cell.extractSelectionText(buffer, inSelection,
  // startPoint, endPoint);
  // if(inSelection && !newInSelection) {
  // return false;
  // }
  // inSelection = newInSelection;
  // }
  // return inSelection;
  // }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.renderer.BoundableRenderable#getRenderablePoint(int,
   * int)
   */
  public RenderableSpot getLowestRenderableSpot(final int x, final int y) {
    for (@NonNull RAbstractCell cell : this.ALL_CELLS) {
      final Rectangle bounds = cell.getVisualBounds();
      if (bounds.contains(x, y)) {
        final RenderableSpot rp = cell.getLowestRenderableSpot(x - bounds.x, y - bounds.y);
        if (rp != null) {
          return rp;
        }
      }
    }
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.html.renderer.BoundableRenderable#onMouseClick(java.awt.event
   * .MouseEvent, int, int)
   */
  public boolean onMouseClick(final MouseEvent event, final int x, final int y) {
    for (@NonNull RAbstractCell cell : this.ALL_CELLS) {
      final Rectangle bounds = cell.getVisualBounds();
      if (bounds.contains(x, y)) {
        if (!cell.onMouseClick(event, x - bounds.x, y - bounds.y)) {
          return false;
        }
        break;
      }
    }
    return true;
  }

  public boolean onDoubleClick(final MouseEvent event, final int x, final int y) {
    for (@NonNull RAbstractCell cell : this.ALL_CELLS) {
      final Rectangle bounds = cell.getVisualBounds();
      if (bounds.contains(x, y)) {
        if (!cell.onDoubleClick(event, x - bounds.x, y - bounds.y)) {
          return false;
        }
        break;
      }
    }
    return true;
  }

  private BoundableRenderable armedRenderable;
  private int maxRowGroupLeft;
  private int maxRowGroupRight;

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.html.renderer.BoundableRenderable#onMouseDisarmed(java.awt.event
   * .MouseEvent)
   */
  public boolean onMouseDisarmed(final MouseEvent event) {
    final BoundableRenderable ar = this.armedRenderable;
    if (ar != null) {
      this.armedRenderable = null;
      return ar.onMouseDisarmed(event);
    } else {
      return true;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.html.renderer.BoundableRenderable#onMousePressed(java.awt.event
   * .MouseEvent, int, int)
   */
  public boolean onMousePressed(final MouseEvent event, final int x, final int y) {
    final ArrayList<RAbstractCell> allCells = this.ALL_CELLS;
    final int numCells = allCells.size();
    for (int i = 0; i < numCells; i++) {
      final RAbstractCell cell = allCells.get(i);
      final Rectangle bounds = cell.getVisualBounds();
      if (bounds.contains(x, y)) {
        if (!cell.onMousePressed(event, x - bounds.x, y - bounds.y)) {
          this.armedRenderable = cell;
          return false;
        }
        break;
      }
    }
    return true;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.html.renderer.BoundableRenderable#onMouseReleased(java.awt.event
   * .MouseEvent, int, int)
   */
  public boolean onMouseReleased(final MouseEvent event, final int x, final int y) {
    final ArrayList<RAbstractCell> allCells = this.ALL_CELLS;
    final int numCells = allCells.size();
    boolean found = false;
    for (int i = 0; i < numCells; i++) {
      final RAbstractCell cell = allCells.get(i);
      final Rectangle bounds = cell.getVisualBounds();
      if (bounds.contains(x, y)) {
        found = true;
        final BoundableRenderable oldArmedRenderable = this.armedRenderable;
        if ((oldArmedRenderable != null) && (cell != oldArmedRenderable)) {
          oldArmedRenderable.onMouseDisarmed(event);
          this.armedRenderable = null;
        }
        if (!cell.onMouseReleased(event, x - bounds.x, y - bounds.y)) {
          return false;
        }
        break;
      }
    }
    if (!found) {
      final BoundableRenderable oldArmedRenderable = this.armedRenderable;
      if (oldArmedRenderable != null) {
        oldArmedRenderable.onMouseDisarmed(event);
        this.armedRenderable = null;
      }
    }
    return true;
  }

  Iterator<@NonNull RAbstractCell> getCells() {
    return this.ALL_CELLS.iterator();
  }

  static final class ColSizeInfo {
    HtmlLength htmlLength;
    int actualSize;
    int fullActualSize; // Full size including border and padding
    int layoutSize;
    int fullLayoutSize; // Full size including border and padding
    int minSize;
    int offsetX;
  }

  static final class RowSizeInfo {
    int insetLeft;
    int insetRight;
    HtmlLength htmlLength;
    int actualSize;
    int minSize;

    int offsetX;
    int offsetY;

    int marginTop;
    int marginBottom;
  }

  private static final class RowGroupSizeInfo {
    private final int height;
    private final int width;
    private final int x;
    private final int y;

    private final @NonNull RTableRowGroup r;

    RowGroupSizeInfo(final int width, final int height, final @NonNull RTableRowGroup r, final int x, final int y) {
      this.height = height;
      this.width = width;
      this.r = r;
      this.x = x;
      this.y = y;
    }


    void prePaintBackground(final Graphics g) {
      final Insets bi = r.getBorderInsets();
      final ModelNode rowGroupElem = r.getModelNode();
      r.prePaintBackground(g, width - (bi.left/2), height, x, y, rowGroupElem, rowGroupElem.getRenderState(), bi);
    }

    void prePaintBorder(final Graphics g) {
      final Insets bi = r.getBorderInsets();
      r.prePaintBorder(g, width + (bi.left)/2  + bi.right, height + bi.top + bi.bottom, x - bi.left, y - bi.top , bi);
    }
  }

  public Iterator<@NonNull RTableRowGroup> getRowGroups() {
    return this.rowGroupSizes.stream().map(rgs -> rgs.r).iterator();
  }
}
