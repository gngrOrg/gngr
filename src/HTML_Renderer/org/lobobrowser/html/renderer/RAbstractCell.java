package org.lobobrowser.html.renderer;

import java.awt.Dimension;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.domimpl.NodeImpl;
import org.lobobrowser.html.style.RenderState;
import org.lobobrowser.ua.UserAgentContext;

public abstract class RAbstractCell extends RBlock {

  public RAbstractCell(NodeImpl modelNode, int listNesting, UserAgentContext pcontext, HtmlRendererContext rcontext,
      FrameContext frameContext, RenderableContainer parentContainer) {
    super(modelNode, listNesting, pcontext, rcontext, frameContext, parentContainer);
  }

  public abstract void setCellBounds(final TableMatrix.ColSizeInfo[] colSizes, final TableMatrix.RowSizeInfo[] rowSizes, final int hasBorder, final int cellSpacingX, final int cellSpacingY);

  public abstract String getWidthText();

  public abstract String getHeightText();

  public abstract void setRowSpan(final int rowSpan);

  public abstract int getRowSpan();

  public abstract int getColSpan();

  protected abstract Dimension doCellLayout(final int width, final int height, final boolean expandWidth, final boolean expandHeight,
      final boolean sizeOnly);

  abstract @NonNull RenderState getRenderState();

  private VirtualCell topLeftVirtualCell;

  public void setTopLeftVirtualCell(final VirtualCell vc) {
    this.topLeftVirtualCell = vc;
  }

  public VirtualCell getTopLeftVirtualCell() {
    return this.topLeftVirtualCell;
  }

  /**
   * @return Returns the virtualColumn.
   */
  public int getVirtualColumn() {
    final VirtualCell vc = this.topLeftVirtualCell;
    return vc == null ? 0 : vc.getColumn();
  }

  /**
   * @return Returns the virtualRow.
   */
  public int getVirtualRow() {
    final VirtualCell vc = this.topLeftVirtualCell;
    return vc == null ? 0 : vc.getRow();
  }

}