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
/*
 * Created on Apr 16, 2005
 */
package org.lobobrowser.html.renderer;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.lobobrowser.html.domimpl.ModelNode;
import org.lobobrowser.html.style.RenderState;

import cz.vutbr.web.css.CSSProperty.VerticalAlign;

/**
 * @author J. H. S.
 */
class RLine extends BaseRCollection {
  private final ArrayList<@NonNull Renderable> renderables = new ArrayList<>(8);
  // private final RenderState startRenderState;
  private int baseLineOffset;
  private int desiredMaxWidth;

  /**
   * Offset where next renderable should be placed. This can be different to
   * width.
   */
  private int xoffset;

  private boolean allowOverflow = false;
  private boolean firstAllowOverflowWord = false;

  public RLine(final ModelNode modelNode, final RenderableContainer container, final int x, final int y, final int desiredMaxWidth,
      final int height,
      final boolean initialAllowOverflow) {
    // Note that in the case of RLine, modelNode is the context node
    // at the beginning of the line, not a node that encloses the whole line.
    super(container, modelNode);
    this.x = x;
    this.y = y;
    this.height = height;
    this.desiredMaxWidth = desiredMaxWidth;
    // Layout here can always be "invalidated"
    this.layoutUpTreeCanBeInvalidated = true;
    this.allowOverflow = initialAllowOverflow;
  }

  public void setAllowOverflow(final boolean flag) {
    if (flag != this.allowOverflow) {
      this.allowOverflow = flag;
      if (flag) {
        // Set to true only if allowOverflow was
        // previously false.
        this.firstAllowOverflowWord = true;
      }
    }
  }

  public boolean isAllowOverflow() {
    return this.allowOverflow;
  }

  /**
   * This method should only be invoked when the line has no items yet.
   */
  public void changeLimits(final int x, final int desiredMaxWidth) {
    this.x = x;
    this.desiredMaxWidth = desiredMaxWidth;
  }

  public int getBaselineOffset() {
    return this.baseLineOffset;
  }

  @Override
  protected void invalidateLayoutLocal() {
    // Workaround for fact that RBlockViewport does not
    // get validated or invalidated.
    this.layoutUpTreeCanBeInvalidated = true;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * net.sourceforge.xamj.domimpl.markup.Renderable#paint(java.awt.Graphics)
   */

  public void paint(final Graphics g) {
    // Paint according to render state of the start of line first.
    final RenderState rs = this.modelNode.getRenderState();

    if ((rs != null) && (rs.getVisibility() != RenderState.VISIBILITY_VISIBLE)) {
      // Just don't paint it.
      return;
    }

    if (rs != null) {
      final Color textColor = rs.getColor();
      g.setColor(textColor);
      final Font font = rs.getFont();
      g.setFont(font);
    }
    // Note that partial paints of the line can only be done
    // if all RStyleChanger's are applied first.
    final Iterator<Renderable> i = this.renderables.iterator();
    while (i.hasNext()) {
      final Renderable r = i.next();
      if (r instanceof RElement) {
        // RElements should be translated.
        final RElement relement = (RElement) r;
        if (!relement.isDelegated()) {
        final Graphics newG = g.create();
        newG.translate(relement.getVisualX(), relement.getVisualY());
        try {
          relement.paint(newG);
        } finally {
          newG.dispose();
        }
        }
      } else if (r instanceof BoundableRenderable) {
        final BoundableRenderable br = (BoundableRenderable) r;
        if (!br.isDelegated()) {
          br.paintTranslated(g);
        }
      } else {
        r.paint(g);
      }
    }
  }

  @Override
  public boolean extractSelectionText(final StringBuffer buffer, final boolean inSelection, final RenderableSpot startPoint,
      final RenderableSpot endPoint) {
    final boolean result = super.extractSelectionText(buffer, inSelection, startPoint, endPoint);
    if (result) {
      final LineBreak br = this.lineBreak;
      if (br != null) {
        buffer.append(System.getProperty("line.separator"));
      } else {
        final ArrayList<Renderable> renderables = this.renderables;
        final int size = renderables.size();
        if ((size > 0) && !(renderables.get(size - 1) instanceof RBlank)) {
          buffer.append(" ");
        }
      }
    }
    return result;
  }

  public final void addStyleChanger(final @NonNull RStyleChanger sc) {
    this.renderables.add(sc);
  }

  public final void simplyAdd(final @NonNull Renderable r) {
    this.renderables.add(r);
  }

  /**
   * This method adds and positions a renderable in the line, if possible. Note
   * that RLine does not set sizes, but only origins.
   *
   * @throws OverflowException
   *           Thrown if the renderable overflows the line. All overflowing
   *           renderables are added to the exception.
   */
  public final void add(final Renderable renderable) throws OverflowException {
    if (renderable instanceof RWord) {
      this.addWord((RWord) renderable);
    } else if (renderable instanceof RBlank) {
      this.addBlank((RBlank) renderable);
    } else if (renderable instanceof RElement) {
      this.addElement((RElement) renderable);
    } else if (renderable instanceof RSpacing) {
      this.addSpacing((RSpacing) renderable);
    } else if (renderable instanceof RStyleChanger) {
      this.addStyleChanger((RStyleChanger) renderable);
    } else if (renderable instanceof RFloatInfo) {
      this.simplyAdd(renderable);
    } else {
      throw new IllegalArgumentException("Can't add " + renderable);
    }
  }

  public final void addWord(final RWord rword) throws OverflowException {
    // Check if it fits horzizontally
    int offset = this.xoffset;
    final int wiwidth = rword.width;
    final boolean allowOverflow = this.allowOverflow;
    final boolean firstAllowOverflowWord = this.firstAllowOverflowWord;
    if (allowOverflow && firstAllowOverflowWord) {
      this.firstAllowOverflowWord = false;
    }
    if ((!allowOverflow || firstAllowOverflowWord) && (offset != 0) && ((offset + wiwidth) > this.desiredMaxWidth)) {
      final ArrayList<Renderable> renderables = this.renderables;
      ArrayList<Renderable> overflow = null;
      boolean cancel = false;
      // Check if other words need to be overflown (for example,
      // a word just before a markup tag adjacent to the word
      // we're trying to add). An RBlank between words prevents
      // a word from being overflown to the next line (and this
      // is the usefulness of RBlank.)
      int newOffset = offset;
      int newWidth = offset;
      for (int i = renderables.size(); --i >= 0;) {
        final Renderable renderable = renderables.get(i);
        if ((renderable instanceof RWord) || !(renderable instanceof BoundableRenderable)) {
          if (overflow == null) {
            overflow = new ArrayList<>();
          }
          if ((renderable != rword) && (renderable instanceof RWord) && (((RWord) renderable).getX() == 0)) {
            // Can't overflow words starting at offset zero.
            // Note that all or none should be overflown.
            cancel = true;
            // No need to set offset - set later.
            break;
          }
          overflow.add(0, renderable);
          renderables.remove(i);
        } else {
          if (renderable instanceof RBlank) {
            final RBlank rblank = (RBlank) renderable;
            newWidth = rblank.getX();
            newOffset = newWidth + rblank.getWidth();
          } else {
            final BoundableRenderable br = (BoundableRenderable) renderable;
            newWidth = newOffset = br.getX() + br.getWidth();
          }
          break;
        }
      }
      if (cancel) {
        // Oops. Need to undo overflow.
        if (overflow != null) {
          final Iterator<Renderable> i = overflow.iterator();
          while (i.hasNext()) {
            renderables.add(i.next());
          }
        }
      } else {
        this.xoffset = newOffset;
        this.width = newWidth;
        if (overflow == null) {
          throw new OverflowException(Collections.singleton((Renderable) rword));
        } else {
          overflow.add(rword);
          throw new OverflowException(overflow);
        }
      }
    }

    // Add it

    int extraHeight = 0;
    final int maxDescent = this.height - this.baseLineOffset;
    if (rword.descent > maxDescent) {
      extraHeight += (rword.descent - maxDescent);
    }
    final int maxAscentPlusLeading = this.baseLineOffset;
    if (rword.ascentPlusLeading > maxAscentPlusLeading) {
      extraHeight += (rword.ascentPlusLeading - maxAscentPlusLeading);
    }
    if (extraHeight > 0) {
      final int newHeight = this.height + extraHeight;
      this.adjustHeight(newHeight, newHeight, VerticalAlign.BOTTOM);
    }
    this.renderables.add(rword);
    rword.setParent(this);
    final int x = offset;
    offset += wiwidth;
    this.width = this.xoffset = offset;
    rword.setOrigin(x, this.baseLineOffset - rword.ascentPlusLeading);
  }

  public final void addBlank(final RBlank rblank) {
    // NOTE: Blanks may be added without concern for wrapping (?)
    final int x = this.xoffset;
    final int width = rblank.width;
    rblank.setOrigin(x, this.baseLineOffset - rblank.ascentPlusLeading);
    this.renderables.add(rblank);
    rblank.setParent(this);
    // Only move xoffset, but not width
    this.xoffset = x + width;
  }

  public final void addSpacing(final RSpacing rblank) {
    // NOTE: Spacing may be added without concern for wrapping (?)
    final int x = this.xoffset;
    final int width = rblank.width;
    rblank.setOrigin(x, (this.height - rblank.height) / 2);
    this.renderables.add(rblank);
    rblank.setParent(this);
    this.width = this.xoffset = x + width;
  }

  /**
   *
   * @param relement
   * @param x
   * @param elementHeight
   *          The required new line height.
   * @param valign
   */
  private final void setElementY(final RElement relement, final int elementHeight, final @Nullable VerticalAlign valign) {
    // At this point height should be more than what's needed.
    int yoffset;
    if (valign != null) {
      switch (valign) {
      case BOTTOM:
        yoffset = this.height - elementHeight;
        break;
      case MIDDLE:
        yoffset = (this.height - elementHeight) / 2;
        break;
      case BASELINE:
        yoffset = this.baseLineOffset - elementHeight;
        break;
      case TOP:
        yoffset = 0;
        break;
      default:
        yoffset = this.baseLineOffset - elementHeight;
      }
    } else {
      yoffset = this.baseLineOffset - elementHeight;
    }
    // RLine only sets origins, not sizes.
    // relement.setBounds(x, yoffset, width, height);
    relement.setY(yoffset);
  }

  // Check if it fits horizontally
  final boolean checkFit(final RElement relement) {
    final int origXOffset = this.xoffset;
    final int desiredMaxWidth = this.desiredMaxWidth;
    final int pw = relement.getWidth();
    final boolean allowOverflow = this.allowOverflow;
    final boolean firstAllowOverflowWord = this.firstAllowOverflowWord;
    if (allowOverflow && firstAllowOverflowWord) {
      this.firstAllowOverflowWord = false;
    }
    final boolean overflows = (!allowOverflow || firstAllowOverflowWord) && (origXOffset != 0) && ((origXOffset + pw) > desiredMaxWidth);
    return !overflows;
  }

  private final void addElement(final RElement relement) throws OverflowException {
    if (!checkFit(relement)) {
      throw new OverflowException(Collections.singleton((Renderable) relement));
    }

    // Note: Renderable for widget doesn't paint the widget, but
    // it's needed for height readjustment.
    final int boundsh = this.height;
    final int origXOffset = this.xoffset;
    final int pw = relement.getWidth();
    final int ph = relement.getHeight();
    int requiredHeight;

    final @Nullable VerticalAlign valign = relement.getVAlign();
    if (valign != null) {
      switch (valign) {
      case BASELINE:
        requiredHeight = ph + (boundsh - this.baseLineOffset);
        break;
      case MIDDLE:
        // TODO: This code probably only works with the older ABS-MIDDLE type of alignment.
        requiredHeight = Math.max(ph, (ph / 2) + (boundsh - this.baseLineOffset));
        break;
      default:
        requiredHeight = ph;
        break;
      }
    } else {
      requiredHeight = ph;
    }

    if (requiredHeight > boundsh) {
      // Height adjustment depends on bounds being already set.
      this.adjustHeight(requiredHeight, ph, valign);
    }
    this.renderables.add(relement);
    relement.setParent(this);
    relement.setX(origXOffset);
    this.setElementY(relement, ph, valign);
    final int newX = origXOffset + pw;
    this.width = this.xoffset = newX;
  }

   /**
   * Positions line elements vertically.
   */
  /*
  final void positionVertically() {
    final ArrayList<Renderable> renderables = this.renderables;

    // System.out.println("pos vertically: " + this + " : " + renderables.size());
    // Find word maximum metrics.
    int maxDescent = 0;
    int maxAscentPlusLeading = 0;
    int maxWordHeight = 0;
    for (final Iterator<Renderable> i = renderables.iterator(); i.hasNext();) {
      final Renderable r = i.next();
      if (r instanceof RWord) {
        final RWord rword = (RWord) r;
        final int descent = rword.descent;
        if (descent > maxDescent) {
          maxDescent = descent;
        }
        final int ascentPlusLeading = rword.ascentPlusLeading;
        if (ascentPlusLeading > maxAscentPlusLeading) {
          maxAscentPlusLeading = ascentPlusLeading;
        }
        if (rword.height > maxWordHeight) {
          maxWordHeight = rword.height;
        }
      }
    }

    // Determine proper baseline
    final int lineHeight = this.height;
    int baseLine = lineHeight - maxDescent;
    for (final Iterator<Renderable> i = renderables.iterator(); i.hasNext();) {
      final Renderable r = i.next();
      if (r instanceof RElement) {
        final RElement relement = (RElement) r;
        // System.out.println("Placing: " + r + "\n  with: " + relement.getVAlign());
        @Nullable VerticalAlign vAlign = relement.getVAlign();
        if (vAlign != null) {
          switch (vAlign) {
          case BOTTOM:
            // This case was implemented by HRJ, but not tested
            relement.setY(lineHeight - relement.getHeight());
            break;
          case MIDDLE:
            int midWord = baseLine + maxDescent - maxWordHeight / 2;
            final int halfElementHeight = relement.getHeight() / 2;
            if (midWord + halfElementHeight > lineHeight) {
              // Change baseLine
              midWord = lineHeight - halfElementHeight;
              baseLine = midWord + maxWordHeight / 2 - maxDescent;
            } else if (midWord - halfElementHeight < 0) {
              midWord = halfElementHeight;
              baseLine = midWord + maxWordHeight / 2 - maxDescent;
            } else {
              relement.setY(midWord - halfElementHeight);
            }
            break;
          default:
            // TODO
            System.out.println("Not implemented yet");
          }
        } else {
          // NOP
        }
      }
    }
  }
  */

  /**
   * Rearrange line elements based on a new line height and alignment provided.
   * All line elements are expected to have bounds preset.
   *
   * @param newHeight
   * @param alignmentY
   */
  private void adjustHeight(final int newHeight, final int elementHeight, final @Nullable VerticalAlign valign) {
    // Set new line height
    // int oldHeight = this.height;
    this.height = newHeight;
    final ArrayList<Renderable> renderables = this.renderables;
    // Find max baseline
    final FontMetrics firstFm = this.modelNode.getRenderState().getFontMetrics();
    int maxDescent = firstFm.getDescent();
    int maxAscentPlusLeading = firstFm.getAscent() + firstFm.getLeading();
    for (final Renderable renderable : renderables) {
      final Object r = renderable;
      if (r instanceof RStyleChanger) {
        final RStyleChanger rstyleChanger = (RStyleChanger) r;
        final FontMetrics fm = rstyleChanger.getModelNode().getRenderState().getFontMetrics();
        final int descent = fm.getDescent();
        if (descent > maxDescent) {
          maxDescent = descent;
        }
        final int ascentPlusLeading = fm.getAscent() + fm.getLeading();
        if (ascentPlusLeading > maxAscentPlusLeading) {
          maxAscentPlusLeading = ascentPlusLeading;
        }
      }
    }
    final int textHeight = maxDescent + maxAscentPlusLeading;

    // TODO: Need to take into account previous RElement's and
    // their alignments?

    int baseline;
    if (valign != null) {
      switch (valign) {
      case BOTTOM:
        baseline = newHeight - maxDescent;
        break;
      case MIDDLE:
        baseline = ((newHeight + textHeight) / 2) - maxDescent;
        break;
      case BASELINE:
        baseline = elementHeight;
        break;
      case TOP:
        baseline = maxAscentPlusLeading;
        break;
      default:
        baseline = elementHeight;
        break;
      }
    } else {
      baseline = elementHeight;
    }
    this.baseLineOffset = baseline;

    // Change bounds of renderables accordingly
    for (final Renderable renderable : renderables) {
      final Object r = renderable;
      if (r instanceof RWord) {
        final RWord rword = (RWord) r;
        rword.setY(baseline - rword.ascentPlusLeading);
      } else if (r instanceof RBlank) {
        final RBlank rblank = (RBlank) r;
        rblank.setY(baseline - rblank.ascentPlusLeading);
      } else if (r instanceof RElement) {
        final RElement relement = (RElement) r;
        // int w = relement.getWidth();
        this.setElementY(relement, relement.getHeight(), relement.getVAlign());
      } else {
        // RSpacing and RStyleChanger don't matter?
      }
    }
    // TODO: Could throw OverflowException when we add floating widgets
  }

  public boolean onMouseClick(final java.awt.event.MouseEvent event, final int x, final int y) {
    final Renderable[] rarray = this.renderables.toArray(Renderable.EMPTY_ARRAY);
    final BoundableRenderable r = MarkupUtilities.findRenderable(rarray, x, y, false);
    if (r != null) {
      final Rectangle rbounds = r.getVisualBounds();
      return r.onMouseClick(event, x - rbounds.x, y - rbounds.y);
    } else {
      return true;
    }
  }

  public boolean onDoubleClick(final java.awt.event.MouseEvent event, final int x, final int y) {
    final Renderable[] rarray = this.renderables.toArray(Renderable.EMPTY_ARRAY);
    final BoundableRenderable r = MarkupUtilities.findRenderable(rarray, x, y, false);
    if (r != null) {
      final Rectangle rbounds = r.getVisualBounds();
      return r.onDoubleClick(event, x - rbounds.x, y - rbounds.y);
    } else {
      return true;
    }
  }

  private BoundableRenderable mousePressTarget;

  /*
  public boolean onMousePressed(final java.awt.event.MouseEvent event, final int x, final int y) {
    final Renderable[] rarray = this.renderables.toArray(Renderable.EMPTY_ARRAY);
    final BoundableRenderable r = MarkupUtilities.findRenderable(rarray, x, y, false);
    if (r != null) {
      this.mousePressTarget = r;
      final Rectangle rbounds = r.getBounds();
      return r.onMousePressed(event, x - rbounds.x, y - rbounds.y);
    } else {
      return true;
    }
  }*/

  public RenderableSpot getLowestRenderableSpot(final int x, final int y) {
    final Renderable[] rarray = this.renderables.toArray(Renderable.EMPTY_ARRAY);
    final BoundableRenderable br = MarkupUtilities.findRenderable(rarray, x, y, false);
    if (br != null) {
      final Rectangle rbounds = br.getVisualBounds();
      return br.getLowestRenderableSpot(x - rbounds.x, y - rbounds.y);
    } else {
      return new RenderableSpot(this, x, y);
    }
  }

  public boolean onMouseReleased(final java.awt.event.MouseEvent event, final int x, final int y) {
    final Renderable[] rarray = this.renderables.toArray(Renderable.EMPTY_ARRAY);
    final BoundableRenderable r = MarkupUtilities.findRenderable(rarray, x, y, false);
    if (r != null) {
      final Rectangle rbounds = r.getVisualBounds();
      final BoundableRenderable oldArmedRenderable = this.mousePressTarget;
      if ((oldArmedRenderable != null) && (r != oldArmedRenderable)) {
        oldArmedRenderable.onMouseDisarmed(event);
        this.mousePressTarget = null;
      }
      return r.onMouseReleased(event, x - rbounds.x, y - rbounds.y);
    } else {
      final BoundableRenderable oldArmedRenderable = this.mousePressTarget;
      if (oldArmedRenderable != null) {
        oldArmedRenderable.onMouseDisarmed(event);
        this.mousePressTarget = null;
      }
      return true;
    }
  }

  public boolean onMouseDisarmed(final java.awt.event.MouseEvent event) {
    final BoundableRenderable target = this.mousePressTarget;
    if (target != null) {
      this.mousePressTarget = null;
      return target.onMouseDisarmed(event);
    } else {
      return true;
    }
  }

  @Override
  public Color getBlockBackgroundColor() {
    return this.container.getPaintedBackgroundColor();
  }

  // public final void adjustHorizontalBounds(int newX, int newMaxWidth) throws
  // OverflowException {
  // this.x = newX;
  // this.desiredMaxWidth = newMaxWidth;
  // int topX = newX + newMaxWidth;
  // ArrayList renderables = this.renderables;
  // int size = renderables.size();
  // ArrayList overflown = null;
  // Rectangle lastInLine = null;
  // for(int i = 0; i < size; i++) {
  // Object r = renderables.get(i);
  // if(overflown == null) {
  // if(r instanceof BoundableRenderable) {
  // BoundableRenderable br = (BoundableRenderable) r;
  // Rectangle brb = br.getBounds();
  // int x2 = brb.x + brb.width;
  // if(x2 > topX) {
  // overflown = new ArrayList(1);
  // }
  // else {
  // lastInLine = brb;
  // }
  // }
  // }
  // /* must not be else here */
  // if(overflown != null) {
  // //TODO: This could break a word across markup boundary.
  // overflown.add(r);
  // renderables.remove(i--);
  // size--;
  // }
  // }
  // if(overflown != null) {
  // if(lastInLine != null) {
  // this.width = this.xoffset = lastInLine.x + lastInLine.width;
  // }
  // throw new OverflowException(overflown);
  // }
  // }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.renderer.RCollection#getRenderables()
   */
  public Iterator<@NonNull Renderable> getRenderables(final boolean topFirst) {
    // TODO: Returning Renderables in order always, assuming that they don't overlap.
    //       Need to check the assumption
    return this.renderables.iterator();
    /*
    if (topFirst) {
      return CollectionUtilities.reverseIterator(this.renderables);
    } else {
      return this.renderables.iterator();
    }*/
  }

  public boolean isContainedByNode() {
    return false;
  }

  private LineBreak lineBreak;

  public LineBreak getLineBreak() {
    return lineBreak;
  }

  public void setLineBreak(final LineBreak lineBreak) {
    this.lineBreak = lineBreak;
  }

  public boolean isEmpty() {
    return this.xoffset == 0;
  }

  @Override
  public Rectangle getClipBounds() {
    // throw new NotImplementedYetException("This method is not expected to be called for RLine");
    return null;
  }

  @Override
  public String toString() {
    return "RLine belonging to: " + getParent();
  }

}
