/*    GNU LESSER GENERAL PUBLIC LICENSE
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

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.logging.Level;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.html.BrowserFrame;
import org.lobobrowser.html.HtmlObject;
import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.domimpl.DocumentFragmentImpl;
import org.lobobrowser.html.domimpl.HTMLBaseInputElement;
import org.lobobrowser.html.domimpl.HTMLCanvasElementImpl;
import org.lobobrowser.html.domimpl.HTMLElementImpl;
import org.lobobrowser.html.domimpl.HTMLIFrameElementImpl;
import org.lobobrowser.html.domimpl.HTMLImageElementImpl;
import org.lobobrowser.html.domimpl.HTMLTableElementImpl;
import org.lobobrowser.html.domimpl.ModelNode;
import org.lobobrowser.html.domimpl.NodeImpl;
import org.lobobrowser.html.domimpl.UINode;
import org.lobobrowser.html.style.HtmlInsets;
import org.lobobrowser.html.style.JStyleProperties;
import org.lobobrowser.html.style.RenderState;
import org.lobobrowser.ua.UserAgentContext;
import org.lobobrowser.util.ArrayUtilities;
import org.lobobrowser.util.CollectionUtilities;
import org.w3c.dom.Node;
import org.w3c.dom.html.HTMLDocument;
import org.w3c.dom.html.HTMLHtmlElement;

/**
 * A substantial portion of the HTML rendering logic of the package can be found
 * in this class. This class is in charge of laying out the DOM subtree of a
 * node, usually on behalf of an RBlock. It creates a renderer subtree
 * consisting of RLine's or RBlock's. RLine's in turn contain RWord's and so on.
 * This class also happens to be used as an RBlock scrollable viewport.
 *
 * @author J. H. S.
 */
public class RBlockViewport extends BaseRCollection {
  // GENERAL NOTES
  // An RBlockViewport basically consists of two collections:
  // seqRenderables and positionedRenderables. The seqRenderables
  // collection is a sequential list of RLine's and RBlock's
  // that is amenable to a binary search by Y position. The
  // positionedRenderables collection is a z-index ordered
  // collection meant for blocks with position=absolute and such.
  //
  // HOW FLOATS WORK
  // Float boxes are scheduled to be added on the next available line.
  // Line layout is bounded by the current floatBounds.
  // When a float is placed with placeFloat(), an absolutely positioned
  // box is added. Whether the float height expands the RBlockViewport
  // height is determined by isFloatLimit().
  //
  // FloatingBounds are inherited by sub-boxes, but the bounds are
  // shifted.
  //
  // The RBlockViewport also publishes a collection of "exporatable
  // floating bounds." These are float boxes that go beyond the bounds
  // of the RBlockViewport, so ancestor blocks can obtain them to adjust
  // their own bounds.

  public static final @NonNull Insets ZERO_INSETS = new Insets(0, 0, 0, 0);

  // private final ArrayList awtComponents = new ArrayList();
  private final int listNesting;
  private final UserAgentContext userAgentContext;
  private final HtmlRendererContext rendererContext;
  private final FrameContext frameContext;

  private SortedSet<PositionedRenderable> positionedRenderables;
  private ArrayList<@NonNull BoundableRenderable> seqRenderables = null;
  private ArrayList<ExportableFloat> exportableFloats = null;
  // private Collection exportedRenderables;
  private RLine currentLine;
  private int maxX;
  private int maxY;
  // private int availHeight;
  private int desiredWidth; // includes insets
  public int getDesiredHeight() {
	return desiredHeight;
}

  private int desiredHeight; // includes insets
  private int availContentHeight; // does not include insets
  private int availContentWidth; // does not include insets
  private int yLimit;
  private int positionedOrdinal;
  private int currentCollapsibleMargin;
  private Insets paddingInsets;
  private boolean overrideNoWrap;
  private FloatingBounds floatBounds = null;
  private boolean sizeOnly;
  private BoundableRenderable lastSeqBlock;

  int scrollX = 0, scrollY = 0;

  private boolean firstElementProcessed = false;
  private boolean lastElementBeingProcessed = false;

  private static final Map<String, MarkupLayout> elementLayout = new HashMap<>(70);
  private static final MarkupLayout commonLayout = new CommonLayout();

  static {
    final Map<String, MarkupLayout> el = elementLayout;
    el.put("BR", new BrLayout());
    el.put("NOSCRIPT", new NoScriptLayout());
    final NopLayout nop = new NopLayout();
    el.put("SCRIPT", nop);
    el.put("HEAD", nop);
    el.put("TITLE", nop);
    el.put("META", nop);
    el.put("STYLE", nop);
    el.put("LINK", nop);
    el.put("IMG", new ImgLayout());
    el.put("INPUT", new InputLayout2());
    el.put("TEXTAREA", new TextAreaLayout2());
    el.put("SELECT", new SelectLayout());
    el.put("HR", new HrLayout());
    final ObjectLayout ol = new ObjectLayout(false, true);
    el.put("OBJECT", new ObjectLayout(true, true));
    el.put("APPLET", ol);
    el.put("EMBED", ol);
    el.put("IFRAME", new IFrameLayout());

    el.put("CANVAS", new CanvasLayout());
  }

  /**
   * Constructs an HtmlBlockLayout.
   *
   * @param container
   *          This is usually going to be an RBlock.
   * @param listNesting
   *          The nesting level for lists. This is zero except inside a list.
   * @param pcontext
   *          The HTMLParserContext instance.
   * @param frameContext
   *          This is usually going to be HtmlBlock, an object where text
   *          selections are contained.
   * @param parent
   *          This is usually going to be the parent of <code>container</code>.
   */
  public RBlockViewport(final ModelNode modelNode, final RenderableContainer container, final int listNesting,
      final UserAgentContext pcontext,
      final HtmlRendererContext rcontext, final FrameContext frameContext, final RCollection parent) {
    super(container, modelNode);
    this.parent = parent;
    this.userAgentContext = pcontext;
    this.rendererContext = rcontext;
    this.frameContext = frameContext;
    this.listNesting = listNesting;
    // Layout here can always be "invalidated"
    this.layoutUpTreeCanBeInvalidated = true;
  }

  @Override
  public void invalidateLayoutLocal() {
    // Workaround for fact that RBlockViewport does not
    // get validated or invalidated.
    this.layoutUpTreeCanBeInvalidated = true;
  }

  public int getAvailContentWidth() {
    return this.availContentWidth;
  }

//	Might still be useful
//  private int initCollapsibleMargin() {
//    final Object parent = this.parent;
//    if (!(parent instanceof RBlock)) {
//      return 0;
//    }
//    final RBlock parentBlock = (RBlock) parent;
//    return parentBlock.getCollapsibleMarginTop();
//  }

  /**
   * Builds the layout/renderer tree from scratch. Note: Returned dimension
   * needs to be actual size needed for rendered content, not the available
   * container size. This is relied upon by table layout.
   *
   * @param yLimit
   *          If other than -1, <code>layout</code> will throw
   *          <code>SizeExceededException</code> in the event that the layout
   *          goes beyond this y-coordinate point.
   */
  public void layout(final int desiredWidth, final int desiredHeight, final Insets paddingInsets, final int yLimit,
      final FloatingBounds floatBounds, final boolean sizeOnly) {
    this.cachedVisualHeight = null;
    this.cachedVisualWidth = null;

    // final RenderableContainer container = this.container;
    this.paddingInsets = paddingInsets;
    this.yLimit = yLimit;
    this.desiredHeight = desiredHeight;
    this.desiredWidth = desiredWidth;
    this.floatBounds = floatBounds;
    this.isFloatLimit = null;
    this.pendingFloats = null;
    this.sizeOnly = sizeOnly;
    this.lastSeqBlock = null;
    // this.currentCollapsibleMargin = this.initCollapsibleMargin();
    this.currentCollapsibleMargin = 0;

    // maxX and maxY should not be reset by layoutPass.
    this.maxX = paddingInsets.left;
    this.maxY = paddingInsets.top;

    int availw = desiredWidth - paddingInsets.left - paddingInsets.right;
    if (availw < 0) {
      availw = 0;
    }
    int availh = desiredHeight - paddingInsets.top - paddingInsets.bottom;
    if (availh == 0) {
      availh = 0;
    }
    this.availContentHeight = availh;
    this.availContentWidth = availw;

    // New floating algorithm.
    this.layoutPass((NodeImpl) this.modelNode);

    // Compute maxY according to last block.
    int maxY = this.maxY;
    int maxYWholeBlock = maxY;
    final BoundableRenderable lastSeqBlock = this.lastSeqBlock;
    if (lastSeqBlock != null) {
      final int effBlockHeight = this.getEffectiveBlockHeight(lastSeqBlock);
      if ((lastSeqBlock.getY() + effBlockHeight) > maxY) {
        this.maxY = maxY = lastSeqBlock.getY() + effBlockHeight;
        maxYWholeBlock = lastSeqBlock.getY() + lastSeqBlock.getHeight();
      }
    }

    // See if line should increase maxY. Empty
    // lines shouldn't, except in cases where
    // there was a BR.
    final RLine lastLine = this.currentLine;
    final Rectangle lastBounds = lastLine.getBounds();
    if ((lastBounds.height > 0) || (lastBounds.y > maxYWholeBlock)) {
      final int lastTopX = lastBounds.x + lastBounds.width;
      if (lastTopX > this.maxX) {
        this.maxX = lastTopX;
      }
      final int lastTopY = lastBounds.y + lastBounds.height;
      if (lastTopY > maxY) {
        this.maxY = maxY = lastTopY;
      }
    }

    // Check positioned renderables for maxX and maxY
    final SortedSet<PositionedRenderable> posRenderables = this.positionedRenderables;
    if (posRenderables != null) {
      final boolean isFloatLimit = this.isFloatLimit();
      final Iterator<PositionedRenderable> i = posRenderables.iterator();
      while (i.hasNext()) {
        final PositionedRenderable pr = i.next();
        final BoundableRenderable br = pr.renderable;
        if ((br.getX() + br.getWidth()) > this.maxX) {
          this.maxX = br.getX() + br.getWidth();
        }
        if (isFloatLimit || !pr.isFloat) {
          if ((br.getY() + br.getHeight()) > maxY) {
            this.maxY = maxY = br.getY() + br.getHeight();
          }
        }
      }
    }

    this.width = paddingInsets.right + this.maxX;
    this.height = paddingInsets.bottom + maxY;
  }

  private void layoutPass(final NodeImpl rootNode) {
    final RenderableContainer container = this.container;
    container.clearDelayedPairs();
    this.positionedOrdinal = 0;

    // Remove sequential renderables...
    this.seqRenderables = null;

    // Remove other renderables...
    this.positionedRenderables = null;

    // Remove exporatable floats...
    this.exportableFloats = null;

    this.cachedVisualHeight = null;
    this.cachedVisualWidth = null;
    // Call addLine after setting margins
    this.currentLine = this.addLine(rootNode, null, this.paddingInsets.top);

    // Start laying out...
    // The parent is expected to have set the RenderState already.
    this.layoutChildren(rootNode);

    // This adds last-line floats.
    this.lineDone(this.currentLine);
  }

  /**
   * Applies any horizonal aLignment. It may adjust height if necessary.
   *
   * @param canvasWidth
   *          The new width of the viewport. It could be different to the
   *          previously calculated width.
   * @param paddingInsets
   */
  public void alignX(final int alignXPercent, final int canvasWidth, final Insets paddingInsets) {
    final int prevMaxY = this.maxY;
    // Horizontal alignment
    if (alignXPercent > 0) {
      final ArrayList<@NonNull BoundableRenderable> renderables = this.seqRenderables;
      if (renderables != null) {
        final Insets insets = this.paddingInsets;
        // final FloatingBounds floatBounds = this.floatBounds;
        final int numRenderables = renderables.size();
//        final int yoffset = 0;  This may get adjusted due to blocks and floats.
        for (int i = 0; i < numRenderables; i++) {
          final Object r = renderables.get(i);
          if (r instanceof BoundableRenderable) {
            final BoundableRenderable seqRenderable = (BoundableRenderable) r;
            final int y = seqRenderable.getY();
//            int newY;
//            if (yoffset > 0) {
//              newY = y + yoffset;
//              seqRenderable.setY(newY);
//              if ((newY + seqRenderable.getHeight()) > this.maxY) {
//                this.maxY = newY + seqRenderable.getHeight();
//              }
//            } else {
//              newY = y;
//            }
            final boolean isVisibleBlock = (seqRenderable instanceof RBlock) && ((RBlock) seqRenderable).isOverflowVisibleX();
            final int leftOffset = isVisibleBlock ? insets.left : this.fetchLeftOffset(y);
            final int rightOffset = isVisibleBlock ? insets.right : this.fetchRightOffset(y);
            final int actualAvailWidth = canvasWidth - leftOffset - rightOffset;
            final int difference = actualAvailWidth - seqRenderable.getWidth();
            if (difference > 0) {
              // The difference check means that only
              // blocks with a declared width would get adjusted?
              /*
              if (floatBounds != null && isVisibleBlock) {
                RBlock block = (RBlock) seqRenderable;
                // Block needs to layed out again. Contents need
                // to shift because of float.
                final int expectedWidth = availContentWidth;
                final int blockShiftRight = insets.right;
                final int newX = leftOffset;
                FloatingBoundsSource floatBoundsSource = new ParentFloatingBoundsSource(blockShiftRight, expectedWidth, newX, newY, floatBounds);
                block.layout(actualAvailWidth, this.availContentHeight, true, false, floatBoundsSource, true);
              }*/
              final int shift = (difference * alignXPercent) / 100;
              if (!isVisibleBlock) {
                final int newX = leftOffset + shift;
                seqRenderable.setX(newX);
              }
            }
          }
        }
      }
    }
    if (prevMaxY != this.maxY) {
      this.height += (this.maxY - prevMaxY);
    }
  }

  /**
   * Applies vertical alignment.
   *
   * @param canvasHeight
   * @param paddingInsets
   */
  public void alignY(final int alignYPercent, final int canvasHeight, final Insets paddingInsets) {
    final int prevMaxY = this.maxY;
    if (alignYPercent > 0) {
      final int availContentHeight = canvasHeight - paddingInsets.top - paddingInsets.bottom;
      final int usedHeight = this.maxY - paddingInsets.top;
      final int difference = availContentHeight - usedHeight;
      if (difference > 0) {
        final int shift = (difference * alignYPercent) / 100;
        final ArrayList<BoundableRenderable> rlist = this.seqRenderables;
        if (rlist != null) {
          // Try sequential renderables first.
          final Iterator<BoundableRenderable> renderables = rlist.iterator();
          while (renderables.hasNext()) {
            final Object r = renderables.next();
            if (r instanceof BoundableRenderable) {
              final BoundableRenderable line = (BoundableRenderable) r;
              final int newY = line.getY() + shift;
              line.setY(newY);
              if ((newY + line.getHeight()) > this.maxY) {
                this.maxY = newY + line.getHeight();
              }
            }
          }
        }

        // Now other renderables, but only those that can be
        // vertically aligned
        final Set<PositionedRenderable> others = this.positionedRenderables;
        if (others != null) {
          final Iterator<PositionedRenderable> i2 = others.iterator();
          while (i2.hasNext()) {
            final PositionedRenderable pr = i2.next();
            if (pr.verticalAlignable) {
              final BoundableRenderable br = pr.renderable;
              final int newY = br.getY() + shift;
              br.setY(newY);
              if ((newY + br.getHeight()) > this.maxY) {
                this.maxY = newY + br.getHeight();
              }
            }
          }
        }
      }
    }
    if (prevMaxY != this.maxY) {
      this.height += (this.maxY - prevMaxY);
    }
  }

  // /**
  // *
  // * @param block A block needing readjustment due to horizontal alignment.
  // * @return
  // */
  // private int readjustBlock(RBlock block, final int newX, final int newY,
  // final FloatingBounds floatBounds) {
  // final int rightInsets = this.paddingInsets.right;
  // final int expectedWidth = this.desiredWidth - rightInsets - newX;
  // final int blockShiftRight = rightInsets;
  // final int prevHeight = block.height;
  // FloatingBoundsSource floatBoundsSource = new FloatingBoundsSource() {
  // public FloatingBounds getChildBlockFloatingBounds(int apparentBlockWidth) {
  // int actualRightShift = blockShiftRight + (expectedWidth -
  // apparentBlockWidth);
  // return new ShiftedFloatingBounds(floatBounds, -newX, -actualRightShift,
  // -newY);
  // }
  // };
  // block.adjust(expectedWidth, this.availContentHeight, true, false,
  // floatBoundsSource, true);
  // return block.height - prevHeight;
  // }
  //
  private RLine addLine(final ModelNode startNode, final RLine prevLine, final int newLineY) {
    // lineDone must be called before we try to
    // get float bounds.
    this.lineDone(prevLine);
    this.checkY(newLineY);
    final int leftOffset = this.fetchLeftOffset(newLineY);
    int newX = leftOffset;
    int newMaxWidth = this.desiredWidth - this.fetchRightOffset(newLineY) - leftOffset;
    RLine rline;
    boolean initialAllowOverflow;
    if (prevLine == null) {
      // Note: Assumes that prevLine == null means it's the first line.
      final RenderState rs = this.modelNode.getRenderState();
      initialAllowOverflow = rs == null ? false : rs.getWhiteSpace() == RenderState.WS_NOWRAP;
      // Text indentation only applies to the first line in the block.
      final int textIndent = rs == null ? 0 : rs.getTextIndent(this.availContentWidth);
      if (textIndent != 0) {
        newX += textIndent;
        // Line width also changes!
        newMaxWidth += (leftOffset - newX);
      }
    } else {
      final int prevLineHeight = prevLine.getHeight();
      if (prevLineHeight > 0) {
        this.currentCollapsibleMargin = 0;
      }
      initialAllowOverflow = prevLine.isAllowOverflow();
      if ((prevLine.x + prevLine.width) > this.maxX) {
        this.maxX = prevLine.x + prevLine.width;
      }
    }
    rline = new RLine(startNode, this.container, newX, newLineY, newMaxWidth, 0, initialAllowOverflow);
    rline.setParent(this);
    ArrayList<@NonNull BoundableRenderable> sr = this.seqRenderables;
    if (sr == null) {
      sr = new ArrayList<>(1);
      this.seqRenderables = sr;
    }
    sr.add(rline);
    this.currentLine = rline;
    return rline;
  }

  private void layoutMarkup(final NodeImpl node) {
    // This is the "inline" layout of an element.
    // The difference with layoutChildren is that this
    // method checks for padding and margin insets.
    final RenderState rs = node.getRenderState();
    final HtmlInsets mi = rs.getMarginInsets();
    final Insets marginInsets = mi == null ? null : mi.getSimpleAWTInsets(this.availContentWidth, this.availContentHeight);
    final HtmlInsets pi = rs.getPaddingInsets();
    final Insets paddingInsets = pi == null ? null : pi.getSimpleAWTInsets(this.availContentWidth, this.availContentHeight);

    int leftSpacing = 0;
    int rightSpacing = 0;
    if (marginInsets != null) {
      leftSpacing += marginInsets.left;
      rightSpacing += marginInsets.right;
    }
    if (paddingInsets != null) {
      leftSpacing += paddingInsets.left;
      rightSpacing += paddingInsets.right;
    }
    if (leftSpacing > 0) {
      final RLine line = this.currentLine;
      line.addSpacing(new RSpacing(node, this.container, leftSpacing, line.height));
    }
    this.layoutChildren(node);
    if (rightSpacing > 0) {
      final RLine line = this.currentLine;
      line.addSpacing(new RSpacing(node, this.container, rightSpacing, line.height));
    }
  }

  private static boolean isLastElement(final int indx, final NodeImpl[] childrenArray) {
    for (int i = indx + 1; i < childrenArray.length; i++) {
      if (childrenArray[i].getNodeType() == Node.ELEMENT_NODE) {
        return false;
      }
    }
    return true;
  }

  private void layoutChildren(final NodeImpl node) {
    firstElementProcessed = false;
    lastElementBeingProcessed = false;

    final NodeImpl[] childrenArray = getAllNodeChildren(node);
    if (childrenArray != null) {
      final int length = childrenArray.length;
      for (int i = 0; i < length; i++) {
        final NodeImpl child = childrenArray[i];
        final short nodeType = child.getNodeType();
        if (nodeType == Node.TEXT_NODE) {
          this.layoutText(child);
        } else if (nodeType == Node.ELEMENT_NODE) {
          // Note that scanning for node bounds (anchor location)
          // depends on there being a style changer for inline elements.
          this.currentLine.addStyleChanger(new RStyleChanger(child));
          final String nodeName = child.getNodeName().toUpperCase();
          MarkupLayout ml = elementLayout.get(nodeName);
          if (ml == null) {
            ml = commonLayout;
          }
          if (isLastElement(i, childrenArray) ) {
            lastElementBeingProcessed = true;
          }
          ml.layoutMarkup(this, (HTMLElementImpl) child);
          this.currentLine.addStyleChanger(new RStyleChanger(node));
          firstElementProcessed = true;
        } else if ((nodeType == Node.COMMENT_NODE) || (nodeType == Node.PROCESSING_INSTRUCTION_NODE)) {
          // ignore
        } else if (nodeType == Node.DOCUMENT_FRAGMENT_NODE) {
          final DocumentFragmentImpl fragment = (DocumentFragmentImpl) child;
          for (final NodeImpl fragChild : fragment.getChildrenArray()) {
            layoutChildren(fragChild);
          }
        } else {

          /* TODO: This case is encountered in some web-platform-tests,
           * for example: /dom/ranges/Range-deleteContents.html
           */
          // throw new IllegalStateException("Unknown node: " + child);
          System.err.println("Unknown node: " + child);

        }
      }
    }
  }

  private NodeImpl[] getAllNodeChildren(final NodeImpl node) {
    final NodeImpl[] childrenArray = node.getChildrenArray();
    NodeImpl beforeNode = null;
    NodeImpl afterNode = null;
    if (node instanceof HTMLElementImpl) {
      HTMLElementImpl htmlElementImpl = (HTMLElementImpl) node;
      beforeNode = htmlElementImpl.getBeforeNode();
      afterNode = htmlElementImpl.getAfterNode();
    }
    if (beforeNode == null && afterNode == null) {
      return childrenArray;
    } else {
      final int totalNodes = (childrenArray == null ? 0 : childrenArray.length) +
          (beforeNode == null ? 0 : 1) +
          (afterNode == null ? 0 : 1);
      if (totalNodes == 0) {
        return null;
      } else {
        NodeImpl[] result = new NodeImpl[totalNodes];
        int count = 0;
        if (beforeNode != null) {
          result[count++] = beforeNode;
        }
        if (childrenArray != null) {
          System.arraycopy(childrenArray, 0, result, count, childrenArray.length);
          count += childrenArray.length;
        }
        if (afterNode != null) {
          result[count++] = afterNode;
        }
        return result;
      }
    }
  }

  private final void positionRBlock(final HTMLElementImpl markupElement, final RBlock renderable) {
    {
      final RenderState rs = renderable.modelNode.getRenderState();
      final int clear = rs.getClear();
      if (clear != LineBreak.NONE) {
        addLineBreak(renderable.modelNode, clear);
      }
    }
    if (!this.addElsewhereIfPositioned(renderable, markupElement, false, true, false)) {
      final int availContentHeight = this.availContentHeight;
      final RLine line = this.currentLine;
      // Inform line done before layout so floats are considered.
      this.lineDone(line);
      final Insets paddingInsets = this.paddingInsets;
      final int newLineY = line == null ? paddingInsets.top : line.y + line.height;
      // int leftOffset = this.fetchLeftOffset(newLineY);
      // int rightOffset = this.fetchRightOffset(newLineY);
      // Float offsets are ignored with block.
      final int availContentWidth = this.availContentWidth;
      final int expectedWidth = availContentWidth;
      final int blockShiftRight = paddingInsets.right;
      final int newX = paddingInsets.left;
      final int newY = newLineY;
      final FloatingBounds floatBounds = this.floatBounds;
      final FloatingBoundsSource floatBoundsSource = floatBounds == null ? null : new ParentFloatingBoundsSource(blockShiftRight,
          expectedWidth,
          newX, newY, floatBounds);

      if (isFirstBlock()) {
        this.currentCollapsibleMargin = 0;
      }

      final boolean isFirstCollapsibleBlock = isFirstCollapsibleBlock(renderable);
      final boolean isLastCollapsibleBlock = isLastCollapsibleBlock(renderable);
      renderable.setCollapseTop(isFirstCollapsibleBlock);
      renderable.setCollapseBottom(isLastCollapsibleBlock);

      /*
      if (isFirstCollapsibleBlock) {
        System.out.println("First block: " + renderable);
      }
      if (isLastCollapsibleBlock) {
        System.out.println("Last block: " + renderable);
      }
      */

      renderable.layout(availContentWidth, availContentHeight, true, false, floatBoundsSource, this.sizeOnly);

      if (isFirstCollapsibleBlock) {
        final RBlock pBlock = (RBlock) this.parent;
        pBlock.absorbMarginTopChild(renderable.getMarginTopOriginal());
      }
      if (isLastCollapsibleBlock) {
        final RBlock pBlock = (RBlock) this.parent;
        pBlock.absorbMarginBottomChild(renderable.getMarginBottomOriginal());
      }


      // if pos:relative then send it to parent for drawing along with other positioned elements.
      this.addAsSeqBlock(renderable, false, false, false, false, false);
      // Calculate new floating bounds after block has been put in place.
      final FloatingInfo floatingInfo = renderable.getExportableFloatingInfo();
      if (floatingInfo != null) {
        this.importFloatingInfo(floatingInfo, renderable);
      }
      // Now add line, after float is set.
      this.addLineAfterBlock(renderable, false);

      bubbleUpIfRelative(markupElement, renderable);
    }
  }

  /* This is used to bubble up relative elements (on the z-axis) */
  private boolean bubbleUpIfRelative(final HTMLElementImpl markupElement, final @NonNull RElement renderable) {
    final int position = getPosition(markupElement);
    final boolean isRelative = position == RenderState.POSITION_RELATIVE;
    if (isRelative) {
      final RenderableContainer con = getPositionedAncestor(container);
      final DelayedPair dp = new DelayedPair(container, con, renderable, null, null, null, null, null, null, null, 0, 0, position);
      container.addDelayedPair(dp);
      if (renderable instanceof RUIControl) {
        this.container.addComponent(((RUIControl) renderable).widget.getComponent());
      }
      return true;
    }

    return false;
  }

  private final void positionRElement(final HTMLElementImpl markupElement, final @NonNull RElement renderable, final boolean usesAlignAttribute,
      final boolean obeysFloats,
      final boolean alignCenterAttribute) {
    if (!this.addElsewhereIfPositioned(renderable, markupElement, usesAlignAttribute, true, true)) {
      int availContentWidth = this.availContentWidth;
      final int availContentHeight = this.availContentHeight;
      final RLine line = this.currentLine;
      // Inform line done before layout so floats are considered.
      this.lineDone(line);
      if (obeysFloats) {
        final int newLineY = line == null ? this.paddingInsets.top : line.y + line.height;
        final int leftOffset = this.fetchLeftOffset(newLineY);
        final int rightOffset = this.fetchRightOffset(newLineY);
        availContentWidth = this.desiredWidth - leftOffset - rightOffset;
      }
      renderable.layout(availContentWidth, availContentHeight, this.sizeOnly);
      boolean centerBlock = false;
      if (alignCenterAttribute) {
        final String align = markupElement.getAttribute("align");
        centerBlock = (align != null) && align.equalsIgnoreCase("center");
      }
      this.addAsSeqBlock(renderable, obeysFloats, false, true, centerBlock, false);
      bubbleUpIfRelative(markupElement, renderable);
    }
  }

  private final void layoutRBlock(final HTMLElementImpl markupElement) {
    final UINode uiNode = markupElement.getUINode();
    RBlock renderable = null;
    if (uiNode instanceof RBlock) {
      renderable = (RBlock) markupElement.getUINode();
    }
    if (renderable == null) {
      renderable = new RBlock(markupElement, this.listNesting, this.userAgentContext, this.rendererContext, this.frameContext,
          this.container);
      markupElement.setUINode(renderable);
    }
    renderable.setOriginalParent(this);
    this.positionRBlock(markupElement, renderable);
  }

  private final void layoutRTable(final HTMLElementImpl markupElement) {
    RElement renderable = (RElement) markupElement.getUINode();
    if (renderable == null) {
      renderable = new RTable(markupElement, this.userAgentContext, this.rendererContext, this.frameContext, container);
      markupElement.setUINode(renderable);
    }
    renderable.setOriginalParent(this);
    this.positionRElement(markupElement, renderable, markupElement instanceof HTMLTableElementImpl, true, true);
  }

  private final void layoutListItem(final HTMLElementImpl markupElement) {
    RListItem renderable = (RListItem) markupElement.getUINode();
    if (renderable == null) {
      renderable = new RListItem(markupElement, this.listNesting, this.userAgentContext, this.rendererContext, this.frameContext,
          this.container, null);
      markupElement.setUINode(renderable);
    }
    renderable.setOriginalParent(this);
    this.positionRBlock(markupElement, renderable);
  }

  private final void layoutList(final HTMLElementImpl markupElement) {
    RList renderable = (RList) markupElement.getUINode();
    if (renderable == null) {
      renderable = new RList(markupElement, this.listNesting, this.userAgentContext, this.rendererContext, this.frameContext,
          this.container, null);
      markupElement.setUINode(renderable);
    }
    renderable.setOriginalParent(this);
    this.positionRBlock(markupElement, renderable);
  }

  // private void addParagraphBreak(ModelNode startNode) {
  // // This needs to get replaced with paragraph collapsing
  // this.addLineBreak(startNode, LineBreak.NONE);
  // this.addLineBreak(startNode, LineBreak.NONE);
  // }

  private void addLineBreak(final ModelNode startNode, final int breakType) {
    RLine line = this.currentLine;
    if (line == null) {
      final Insets insets = this.paddingInsets;
      this.addLine(startNode, null, insets.top);
      line = this.currentLine;
    }
    if (line.getHeight() == 0) {
      final RenderState rs = startNode.getRenderState();
      final int fontHeight = rs.getFontMetrics().getHeight();
      line.setHeight(fontHeight);
    }
    line.setLineBreak(new LineBreak(breakType));
    int newLineY;
    final FloatingBounds fb = this.floatBounds;
    if ((breakType == LineBreak.NONE) || (fb == null)) {
      newLineY = line.y + line.height;
    } else {
      final int prevY = line.y + line.height;
      switch (breakType) {
      case LineBreak.LEFT:
        newLineY = fb.getLeftClearY(prevY);
        break;
      case LineBreak.RIGHT:
        newLineY = fb.getRightClearY(prevY);
        break;
      default:
        newLineY = fb.getClearY(prevY);
        break;
      }
    }
    this.currentLine = this.addLine(startNode, line, newLineY);
  }

  private boolean addElsewhereIfFloat(final RElement renderable, final HTMLElementImpl element,
      final boolean usesAlignAttribute,
      final JStyleProperties style, final boolean layout) {
    // "static" handled here
    String align = null;
    if (style != null) {
      align = style.getFloat();
      if ((align != null) && (align.length() == 0)) {
        align = null;
      }
    }
    if ((align == null) && usesAlignAttribute) {
      align = element.getAttribute("align");
    }
    if (align != null) {
      if ("left".equalsIgnoreCase(align)) {
        this.layoutFloat(renderable, layout, true);
        return true;
      } else if ("right".equalsIgnoreCase(align)) {
        this.layoutFloat(renderable, layout, false);
        return true;
      } else {
        // fall through
      }
    }
    return false;
  }

  // final RBlockViewport getParentViewport(ExportedRenderable er) {
  // if(er.alignment == 0) {
  // return this.getParentViewport();
  // }
  // else {
  // return this.getParentViewportForAlign();
  // }
  // }
  //
  // final boolean isImportable(ExportedRenderable er) {
  // if(er.alignment == 0) {
  // return this.positionsAbsolutes();
  // }
  // else {
  // return this.getParentViewportForAlign() == null;
  // }
  // }

  final RBlockViewport getParentViewport() {
    // Use originalParent, which for one, is not going to be null during layout.
    RCollection parent = this.getOriginalOrCurrentParent();
    while ((parent != null) && !(parent instanceof RBlockViewport)) {
      parent = parent.getOriginalOrCurrentParent();
    }
    return (RBlockViewport) parent;
  }

  // final RBlockViewport getParentViewportForAlign() {
  // // Use originalParent, which for one, is not going to be null during
  // layout.
  // Object parent = this.getOriginalOrCurrentParent();
  // if(parent instanceof RBlock) {
  // RBlock block = (RBlock) parent;
  // if(!block.couldBeScrollable()) {
  // parent = ((BaseElementRenderable) parent).getOriginalOrCurrentParent();
  // if(parent instanceof RBlockViewport) {
  // return (RBlockViewport) parent;
  // }
  // }
  // }
  // return null;
  // }
  //
  private static int getPosition(final HTMLElementImpl element) {
    final RenderState rs = element.getRenderState();
    return rs.getPosition();
  }

  /**
   * Checks for position and float attributes.
   *
   * @param container
   * @param containerSize
   * @param insets
   * @param renderable
   * @param element
   * @param usesAlignAttribute
   * @return True if it was added elsewhere.
   */
  private boolean addElsewhereIfPositioned(final @NonNull RElement renderable, final HTMLElementImpl element, final boolean usesAlignAttribute,
      final boolean layoutIfPositioned, final boolean obeysFloats) {
    // At this point block already has bounds.
    final JStyleProperties style = element.getCurrentStyle();
    final int position = getPosition(element);
    final boolean absolute = position == RenderState.POSITION_ABSOLUTE;
    final boolean fixed = position == RenderState.POSITION_FIXED;
    if (absolute || fixed) {
      if (layoutIfPositioned) {
        // Presumes the method will return true.
        if (renderable instanceof RBlock) {
          final RBlock block = (RBlock) renderable;
          block.layout(this.availContentWidth, this.availContentHeight, false, false, null, this.sizeOnly);
        } else {
          renderable.layout(this.availContentWidth, this.availContentHeight, this.sizeOnly);
        }
      }

      final RenderState rs = element.getRenderState();
      final String leftText = style.getLeft();
      final String rightText = style.getRight();
      final String bottomText = style.getBottom();
      final String topText = style.getTop();
      final String widthText = style.getWidth();
      final String heightText = style.getHeight();

      // Schedule as delayed pair. Will be positioned after everything else.
      this.scheduleAbsDelayedPair(renderable, leftText, rightText, topText, bottomText, widthText, heightText, rs, currentLine.getX(), currentLine.getY() + currentLine.getHeight(), absolute);
      // Does not affect bounds of this viewport yet.
      return true;
    } else {
      if (this.addElsewhereIfFloat(renderable, element, usesAlignAttribute, style, layoutIfPositioned)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks property 'float' and in some cases attribute 'align'.
   */
  private void addRenderableToLineCheckStyle(final @NonNull RElement renderable, final HTMLElementImpl element, final boolean usesAlignAttribute) {
    if (this.addElsewhereIfPositioned(renderable, element, usesAlignAttribute, true, true)) {
      return;
    }
    renderable.layout(this.availContentWidth, this.availContentHeight, this.sizeOnly);
    this.addRenderableToLine(renderable);
    bubbleUpIfRelative(element, renderable);
  }

  private void addRenderableToLine(final Renderable renderable) {
    // this.skipLineBreakBefore = false;
    final RLine line = this.currentLine;
    final int liney = line.y;
    final boolean emptyLine = line.isEmpty();
    final FloatingBounds floatBounds = this.floatBounds;
    int cleary;
    if (floatBounds != null) {
      cleary = floatBounds.getFirstClearY(liney);
    } else {
      cleary = liney + line.height;
    }
    try {
      line.add(renderable);
      // Check if the line goes into the float.
      if ((floatBounds != null) && (cleary > liney)) {
        final int rightOffset = this.fetchRightOffset(liney);
        final int topLineX = this.desiredWidth - rightOffset;
        if ((line.getX() + line.getWidth()) > topLineX) {
          // Shift line down to clear area
          line.setY(cleary);
        }
      }
    } catch (final OverflowException oe) {
      final int nextY = emptyLine ? cleary : liney + line.height;
      this.addLine(renderable.getModelNode(), line, nextY);
      final Collection<Renderable> renderables = oe.getRenderables();
      final Iterator<Renderable> i = renderables.iterator();
      while (i.hasNext()) {
        final Renderable r = i.next();
        this.addRenderableToLine(r);
      }
    }
    if (renderable instanceof RUIControl) {
      this.container.addComponent(((RUIControl) renderable).widget.getComponent());
    }
  }

  private void addWordToLine(final RWord renderable) {
    // this.skipLineBreakBefore = false;
    final RLine line = this.currentLine;
    final int liney = line.y;
    final boolean emptyLine = line.isEmpty();
    final FloatingBounds floatBounds = this.floatBounds;
    int cleary;
    if (floatBounds != null) {
      cleary = floatBounds.getFirstClearY(liney);
    } else {
      cleary = liney + line.height;
    }
    try {
      line.addWord(renderable);
      // Check if the line goes into the float.
      if (!line.isAllowOverflow() && (floatBounds != null) && (cleary > liney)) {
        final int rightOffset = this.fetchRightOffset(liney);
        final int topLineX = this.desiredWidth - rightOffset;
        if ((line.getX() + line.getWidth()) > topLineX) {
          // Shift line down to clear area
          line.setY(cleary);
        }
      }
    } catch (final OverflowException oe) {
      final int nextY = emptyLine ? cleary : liney + line.height;
      this.addLine(renderable.getModelNode(), line, nextY);
      final Collection<Renderable> renderables = oe.getRenderables();
      final Iterator<Renderable> i = renderables.iterator();
      while (i.hasNext()) {
        final Renderable r = i.next();
        this.addRenderableToLine(r);
      }
    }
  }

  /*
  private void addAsSeqBlockCheckStyle(final @NonNull RElement block, final HTMLElementImpl element, final boolean usesAlignAttribute) {
    if (this.addElsewhereIfPositioned(block, element, usesAlignAttribute, false, true)) {
      return;
    }
    this.addAsSeqBlock(block);
  }*/

  private void addAsSeqBlock(final RElement block) {
    this.addAsSeqBlock(block, true, true, true, false, false);
  }

  private int getNewBlockY(final BoundableRenderable newBlock, final int expectedY) {
    // Assumes the previous block is not a line with height > 0.
    if (!(newBlock instanceof RElement)) {
      return expectedY;
    }
    final RElement block = (RElement) newBlock;
    final int ccm = this.currentCollapsibleMargin;
    final int topMargin = block.getMarginTop();
    return expectedY - Math.min(topMargin, ccm);
  }

  private int getEffectiveBlockHeight(final BoundableRenderable block) {
    // Assumes block is the last one in the sequence.
    if (!(block instanceof RElement)) {
      return block.getHeight();
    }
    final RCollection parent = this.getParent();
    if (!(parent instanceof RElement)) {
      return block.getHeight();
    }
    final int blockMarginBottom = ((RElement) block).getMarginBottom();
    final int parentMarginBottom = ((RElement) parent).getCollapsibleMarginBottom();
    return block.getHeight() - Math.min(blockMarginBottom, parentMarginBottom);
  }

  private void addAsSeqBlock(final BoundableRenderable block, final boolean obeysFloats, final boolean informLineDone,
      final boolean addLine, final boolean centerBlock, final boolean isRelative) {
    final Insets insets = this.paddingInsets;
    final int insetsl = insets.left;
    ArrayList<@NonNull BoundableRenderable> sr = this.seqRenderables;
    if (sr == null) {
      sr = new ArrayList<>(1);
      this.seqRenderables = sr;
    }
    final RLine prevLine = this.currentLine;
    boolean initialAllowOverflow;
    if (prevLine != null) {
      initialAllowOverflow = prevLine.isAllowOverflow();
      if (informLineDone) {
        this.lineDone(prevLine);
      }
      if ((prevLine.x + prevLine.width) > this.maxX) {
        this.maxX = prevLine.x + prevLine.width;
      }
      // Check height only with floats.
    } else {
      initialAllowOverflow = false;
    }
    final int prevLineHeight = prevLine == null ? 0 : prevLine.height;
    int newLineY = prevLine == null ? insets.top : prevLine.y + prevLineHeight;
    int blockX;
    int blockY = prevLineHeight == 0 ? this.getNewBlockY(block, newLineY) : newLineY;
    final int blockWidth = block.getWidth();
    if (obeysFloats) {
      // TODO: execution of fetchLeftOffset done twice with positionRElement.
      final FloatingBounds floatBounds = this.floatBounds;
      int actualAvailWidth;
      if (floatBounds != null) {
        final int blockOffset = this.fetchLeftOffset(newLineY);
        blockX = blockOffset;
        final int rightOffset = this.fetchRightOffset(newLineY);
        actualAvailWidth = this.desiredWidth - rightOffset - blockOffset;
        if (blockWidth > actualAvailWidth) {
          blockY = floatBounds.getClearY(newLineY);
        }
      } else {
        actualAvailWidth = this.availContentWidth;
        blockX = insetsl;
      }
      if (centerBlock) {
        final int roomX = actualAvailWidth - blockWidth;
        if (roomX > 0) {
          blockX += (roomX / 2);
        }
      }
    } else {
      // Block does not obey alignment margins
      blockX = insetsl;
    }
    block.setOrigin(blockX, blockY);
    if (!isRelative) {
      sr.add(block);
      block.setParent(this);
    }
    if ((blockX + blockWidth) > this.maxX) {
      this.maxX = blockX + blockWidth;
    }
    this.lastSeqBlock = block;
    this.currentCollapsibleMargin = block instanceof RElement ? ((RElement) block).getMarginBottom() : 0;
    if (addLine) {
      newLineY = blockY + block.getHeight();
      this.checkY(newLineY);
      final int leftOffset = this.fetchLeftOffset(newLineY);
      final int newX = leftOffset;
      final int newMaxWidth = this.desiredWidth - this.fetchRightOffset(newLineY) - leftOffset;
      final ModelNode lineNode = block.getModelNode().getParentModelNode();
      final RLine newLine = new RLine(lineNode, this.container, newX, newLineY, newMaxWidth, 0, initialAllowOverflow);
      newLine.setParent(this);
      sr.add(newLine);
      this.currentLine = newLine;
    }
    if (!isRelative) {
      if (block instanceof RUIControl) {
        this.container.addComponent(((RUIControl) block).widget.getComponent());
      }
    }
  }

  private static boolean isCollapsibleBlock(final RBlock block, final Function<HtmlInsets, Boolean> insetChecker) {
    final ModelNode mn = block.getModelNode();
    final RenderState rs = mn.getRenderState();
    final boolean isDisplayBlock = rs.getDisplay() == RenderState.DISPLAY_BLOCK;
    final boolean isPosStaticOrRelative = rs.getPosition() == RenderState.POSITION_STATIC || rs.getPosition() == RenderState.POSITION_RELATIVE;
    final HtmlInsets borderInsets = rs.getBorderInfo().insets;
    final HtmlInsets paddingInsets = rs.getPaddingInsets();
    final boolean isZeroBorderAndPadding = insetChecker.apply(borderInsets) && insetChecker.apply(paddingInsets);
    return (!(mn instanceof HTMLHtmlElement)) && isDisplayBlock && isPosStaticOrRelative && isZeroBorderAndPadding;
  }

  private static boolean checkTopInset(final HtmlInsets insets) {
    return insets == null || insets.top == 0;
  }

  private static boolean checkBottomInset(final HtmlInsets insets) {
    return insets == null || insets.bottom == 0;
  }

  private static boolean isCollapsibleParentBlock(final RBlock block, final Function<HtmlInsets, Boolean> insetChecker) {
    final ModelNode mn = block.getModelNode();
    final RenderState rs = mn.getRenderState();
    return isCollapsibleBlock(block, insetChecker) && isOverflowVisibleOrNone(rs);
  }

  private static boolean isOverflowVisibleOrNone(final RenderState rs) {
    final int overflowX = rs.getOverflowX();
    final int overflowY = rs.getOverflowY();
    final boolean xOverflowFine = (overflowX == RenderState.OVERFLOW_VISIBLE) || (overflowX == RenderState.OVERFLOW_NONE);
    final boolean yOverflowFine = (overflowY == RenderState.OVERFLOW_VISIBLE) || (overflowY == RenderState.OVERFLOW_NONE);
    final boolean overflowFine = xOverflowFine && yOverflowFine;
    return overflowFine;
  }

  private boolean isFirstCollapsibleBlock(final RBlock child) {
    return isFirstBlock() && isCollapsibleBlock(child, RBlockViewport::checkTopInset)
        && isCollapsibleParentBlock((RBlock) this.parent, RBlockViewport::checkTopInset);
  }

  private boolean isLastCollapsibleBlock(final RBlock child) {
    return lastElementBeingProcessed && isCollapsibleBlock(child, RBlockViewport::checkBottomInset)
        && isCollapsibleParentBlock((RBlock) this.parent, RBlockViewport::checkBottomInset);
  }

  private boolean isFirstBlock() {
    final ArrayList<@NonNull BoundableRenderable> sr = this.seqRenderables;
    return (!firstElementProcessed) && (sr == null || ((sr.size() == 1) && (sr.get(0) instanceof RLine) && ((RLine)sr.get(0)).isEmpty()));
  }

  private void addLineAfterBlock(final RBlock block, final boolean informLineDone) {
    ArrayList<@NonNull BoundableRenderable> sr = this.seqRenderables;
    if (sr == null) {
      sr = new ArrayList<>(1);
      this.seqRenderables = sr;
    }
    final RLine prevLine = this.currentLine;
    boolean initialAllowOverflow;
    if (prevLine != null) {
      initialAllowOverflow = prevLine.isAllowOverflow();
      if (informLineDone) {
        this.lineDone(prevLine);
      }
      if ((prevLine.x + prevLine.width) > this.maxX) {
        this.maxX = prevLine.x + prevLine.width;
      }
      // Check height only with floats.
    } else {
      initialAllowOverflow = false;
    }
    final ModelNode lineNode = block.getModelNode().getParentModelNode();
    final int newLineY = block.getY() + block.getHeight();
    this.checkY(newLineY);
    final int leftOffset = this.fetchLeftOffset(newLineY);
    final int newX = leftOffset;
    final int newMaxWidth = this.desiredWidth - this.fetchRightOffset(newLineY) - leftOffset;
    final RLine newLine = new RLine(lineNode, this.container, newX, newLineY, newMaxWidth, 0, initialAllowOverflow);
    newLine.setParent(this);
    sr.add(newLine);
    this.currentLine = newLine;
  }

  private void layoutText(final NodeImpl textNode) {
    final String text = textNode.getNodeValue();
    final int length = text.length();
    final RenderState renderState = textNode.getRenderState();
    final FontMetrics fm = renderState.getFontMetrics();
    final int descent = fm.getDescent();
    final int ascentPlusLeading = fm.getAscent() + fm.getLeading();
    final int wordHeight = fm.getHeight();
    final int blankWidth = fm.charWidth(' ');
    final int whiteSpace = this.overrideNoWrap ? RenderState.WS_NOWRAP : renderState.getWhiteSpace();
    final int textTransform = renderState.getTextTransform();
    if (whiteSpace != RenderState.WS_PRE) {
      final boolean prevAllowOverflow = this.currentLine.isAllowOverflow();
      final boolean allowOverflow = whiteSpace == RenderState.WS_NOWRAP;
      this.currentLine.setAllowOverflow(allowOverflow);
      try {
        final StringBuffer word = new StringBuffer(12);
        for (int i = 0; i < length; i++) {
          char ch = text.charAt(i);
          if (Character.isWhitespace(ch)) {
            final int wlen = word.length();
            if (wlen > 0) {
              final RWord rword = new RWord(textNode, word.toString(), container, fm, descent, ascentPlusLeading, wordHeight, textTransform);
              this.addWordToLine(rword);
              word.delete(0, wlen);
            }
            final RLine line = this.currentLine;
            if (line.width > 0) {
              final RBlank rblank = new RBlank(textNode, fm, container, ascentPlusLeading, blankWidth, wordHeight);
              line.addBlank(rblank);
            }
            for (i++; i < length; i++) {
              ch = text.charAt(i);
              if (!Character.isWhitespace(ch)) {
                word.append(ch);
                break;
              }
            }
          } else {
            word.append(ch);
          }
        }
        if (word.length() > 0) {
          final RWord rword = new RWord(textNode, word.toString(), container, fm, descent, ascentPlusLeading, wordHeight, textTransform);
          this.addWordToLine(rword);
        }
      } finally {
        this.currentLine.setAllowOverflow(prevAllowOverflow);
      }
    } else {
      boolean lastCharSlashR = false;
      final StringBuffer line = new StringBuffer();
      for (int i = 0; i < length; i++) {
        final char ch = text.charAt(i);
        switch (ch) {
        case '\r':
          lastCharSlashR = true;
          break;
        case '\n':
          final RWord rword = new RWord(textNode, line.toString(), container, fm, descent, ascentPlusLeading, wordHeight, textTransform);
          this.addWordToLine(rword);
          line.delete(0, line.length());
          final RLine prevLine = this.currentLine;
          prevLine.setLineBreak(new LineBreak(LineBreak.NONE));
          this.addLine(textNode, prevLine, prevLine.y + prevLine.height);
          break;
        default:
          if (lastCharSlashR) {
            line.append('\r');
            lastCharSlashR = false;
          }
          if (ch == '\t') {
            /* Tabs are not recognized as advancing width in FontMetrics. There are two approaches possible:
               1. Convert to spaces. Simple, but when copying selection spaces are copied.
               2. Define a new class call RTab, that manages tabs.
               3. Modify the width calculation logic of RWord to account for tab character.
            */

            // TODO: The number of spaces is hard-coded right now. But when CSS `tab-size` property is supported, it could be made variable.
            final int NUM_SPACES = 8;

            // Solution 1.
            // line.append("        ");

            // Solution 2.
            addWordToLine(new RTab(textNode, container, fm, descent, ascentPlusLeading, wordHeight, NUM_SPACES));
          } else {
            line.append(ch);
          }
          break;
        }
      }
      if (line.length() > 0) {
        final RWord rword = new RWord(textNode, line.toString(), container, fm, descent, ascentPlusLeading, wordHeight, textTransform);
        this.addWordToLine(rword);
      }
    }
  }

  /**
   *
   * @param others
   *          An ordered collection.
   * @param seqRenderables
   * @param destination
   */
  /*
  private static void populateZIndexGroupsIterator(final Collection<PositionedRenderable> others,
      final Collection<? extends Renderable> seqRenderables,
      final ArrayList<Renderable> destination) {
    populateZIndexGroups(others, seqRenderables == null ? null : seqRenderables.iterator(), destination);
  }*/

  /**
   *
   * @param others
   *          An ordered collection.
   * @param seqRenderablesIterator
   * @param destination
   */
  private static void populateZIndexGroups(final Collection<PositionedRenderable> others,
      final Iterator<? extends Renderable> seqRenderablesIterator,
      final ArrayList<Renderable> destination) {
    // First, others with z-index < 0
    final Iterator<PositionedRenderable> i1 = others.iterator();
    Renderable pending = null;
    while (i1.hasNext()) {
      final PositionedRenderable pr = i1.next();
      final BoundableRenderable r = pr.renderable;
      if (r.getZIndex() >= 0) {
        pending = pr;
        break;
      }
      destination.add(pr);
    }

    // Second, sequential renderables
    final Iterator<? extends Renderable> i2 = seqRenderablesIterator;
    if (i2 != null) {
      while (i2.hasNext()) {
        destination.add(i2.next());
      }
    }

    // Third, other renderables with z-index >= 0.
    if (pending != null) {
      destination.add(pending);
      while (i1.hasNext()) {
        final PositionedRenderable pr = i1.next();
        destination.add(pr);
      }
    }
  }

  private static void populateZIndexGroupsTopFirst(final List<PositionedRenderable> others,
      final Iterator<? extends Renderable> seqRenderablesIterator,
      final ArrayList<Renderable> destination) {
    // First, others with z-index >= 0
    final Iterator<PositionedRenderable> i1 = CollectionUtilities.reverseIterator(others);
    Renderable pending = null;
    while (i1.hasNext()) {
      final PositionedRenderable pr = i1.next();
      final BoundableRenderable r = pr.renderable;
      if (r.getZIndex() < 0) {
        pending = pr;
        break;
      }
      destination.add(pr);
    }

    // Second, sequential renderables
    final Iterator<? extends Renderable> i2 = seqRenderablesIterator;
    if (i2 != null) {
      while (i2.hasNext()) {
        destination.add(i2.next());
      }
    }

    // Third, other renderables with z-index >= 0.
    if (pending != null) {
      destination.add(pending);
      while (i1.hasNext()) {
        final PositionedRenderable pr = i1.next();
        destination.add(pr);
      }
    }
  }

  public Iterator<@NonNull ? extends Renderable> getRenderables(final boolean topFirst) {
    final SortedSet<PositionedRenderable> others = this.positionedRenderables;
    final ArrayList<@NonNull ? extends Renderable> sr = this.seqRenderables;
    if ((others == null) || (others.size() == 0)) {
      return sr == null ? null : sr.iterator();
    } else {
      final ArrayList<@NonNull Renderable> allRenderables = new ArrayList<>();
      Iterator<? extends Renderable> srIterator = sr == null ? null : sr.iterator();
      if (topFirst) {
        populateZIndexGroupsTopFirst(new ArrayList<>(others), srIterator, allRenderables);
      } else {
        populateZIndexGroups(others, srIterator, allRenderables);
      }
      return allRenderables.iterator();
    }
  }

  private Iterator<Renderable> getRenderables(final Rectangle clipBounds) {
    final ArrayList<BoundableRenderable> sr = this.seqRenderables;
    Iterator<Renderable> baseIterator = null;
    if (sr != null) {
      final Renderable[] array = sr.toArray(Renderable.EMPTY_ARRAY);
      final Range range = MarkupUtilities.findRenderables(array, clipBounds, true);
      baseIterator = ArrayUtilities.iterator(array, range.offset, range.length);
    }

    final SortedSet<PositionedRenderable> others = this.positionedRenderables;
    if ((others == null) || (others.size() == 0)) {
      return baseIterator;
    } else {
      final ArrayList<PositionedRenderable> matches = new ArrayList<>();
      // ArrayList "matches" keeps the order from "others".
      final Iterator<PositionedRenderable> i = others.iterator();
      while (i.hasNext()) {
        final PositionedRenderable pr = i.next();
        // if (pr.isFixed() || clipBounds.intersects(pr.renderable.getVisualBounds())) {
        if (pr.isFixed() || clipBounds.intersects(pr.getVisualBounds())) {
          matches.add(pr);
        }
        // matches.add(pr);
      }
      if (matches.size() == 0) {
        return baseIterator;
      } else {
        final ArrayList<Renderable> destination = new ArrayList<>();
        populateZIndexGroups(matches, baseIterator, destination);
        return destination.iterator();
      }
    }
  }

  /*
  @Override
  public BoundableRenderable getRenderable(final int x, final int y) {
    // TODO: Optimize. Find only the first renderable instead of all of them
    final Iterator<? extends Renderable> i = this.getRenderables(x, y);
    return i == null ? null : (i.hasNext() ? (BoundableRenderable) i.next() : null);
  }
  */

  public BoundableRenderable getRenderable(final java.awt.Point point) {
    return this.getRenderable(point.x, point.y);
  }

  public Iterator<? extends Renderable> getRenderables(final java.awt.Point point) {
    return this.getRenderables(point.x, point.y);
  }

  public Iterator<? extends Renderable> getRenderables(final int pointx, final int pointy) {
    Collection<BoundableRenderable> result = null;
    final SortedSet<PositionedRenderable> others = this.positionedRenderables;
    final int size = others == null ? 0 : others.size();
    final PositionedRenderable[] otherArray = size == 0 ? null : others.toArray(PositionedRenderable.EMPTY_ARRAY);
    // Try to find in other renderables with z-index >= 0 first.
    int index = 0;
    if (otherArray != null) {
      // Must go in reverse order
      for (index = size; --index >= 0;) {
        final PositionedRenderable pr = otherArray[index];
        final BoundableRenderable br = pr.renderable;
        if (br.getZIndex() < 0) {
          break;
        }
        if (br.contains(pointx, pointy)) {
          if (result == null) {
            result = new LinkedList<>();
          }
          result.add(br);
        }
      }
    }

    // Now do a "binary" search on sequential renderables.
    /*
    final ArrayList<BoundableRenderable> sr = this.seqRenderables;
    if (sr != null) {
      final Renderable[] array = sr.toArray(Renderable.EMPTY_ARRAY);
      final BoundableRenderable found = MarkupUtilities.findRenderable(array, pointx, pointy, true);
      if (found != null) {
        if (result == null) {
          result = new LinkedList<>();
        }
        result.add(found);
      }
    }*/

    /* Get all sequential renderables that contain the point */
    final ArrayList<BoundableRenderable> sr = this.seqRenderables;
    if (sr != null) {
      final Renderable[] array = sr.toArray(Renderable.EMPTY_ARRAY);
      final List<BoundableRenderable> found = MarkupUtilities.findRenderables(array, pointx, pointy, true);
      if (found != null) {
        if (result == null) {
          result = new LinkedList<>();
        }
        result.addAll(found);
      }
    }

    // Finally, try to find it in renderables with z-index < 0.
    if (otherArray != null) {
      // Must go in reverse order
      for (; index >= 0; index--) {
        final PositionedRenderable pr = otherArray[index];
        final BoundableRenderable br = pr.renderable;
        if (br.contains(pointx, pointy)) {
          if (result == null) {
            result = new LinkedList<>();
          }
          result.add(br);
        }
      }
    }
    return result == null ? null : result.iterator();
  }

  private RElement setupNewUIControl(final RenderableContainer container, final HTMLElementImpl element, final UIControl control) {
    final RElement renderable = new RUIControl(element, control, container, this.frameContext, this.userAgentContext);
    element.setUINode(renderable);
    return renderable;
  }

  private final void addAlignableAsBlock(final HTMLElementImpl markupElement, final RElement renderable) {
    // TODO: Get rid of this method?
    // At this point block already has bounds.
    boolean regularAdd = false;
    final String align = markupElement.getAttribute("align");
    if (align != null) {
      if ("left".equalsIgnoreCase(align)) {
        this.layoutFloat(renderable, false, true);
      } else if ("right".equalsIgnoreCase(align)) {
        this.layoutFloat(renderable, false, false);
      } else {
        regularAdd = true;
      }
    } else {
      regularAdd = true;
    }
    if (regularAdd) {
      this.addAsSeqBlock(renderable);
    }
  }

  private final void layoutHr(final HTMLElementImpl markupElement) {
    RElement renderable = (RElement) markupElement.getUINode();
    if (renderable == null) {
      renderable = this.setupNewUIControl(container, markupElement, new HrControl(markupElement));
    }
    renderable.layout(this.availContentWidth, this.availContentHeight, this.sizeOnly);
    this.addAlignableAsBlock(markupElement, renderable);
  }

  private final static BaseInputControl createInputControl(final HTMLBaseInputElement markupElement) {
    String type = markupElement.getAttribute("type");
    if (type == null) {
      return new InputTextControl(markupElement);
    }
    type = type.toLowerCase();
    if ("text".equals(type) || "url".equals(type) || "number".equals(type) || "search".equals(type) || (type.length() == 0)) {
      return new InputTextControl(markupElement);
    } else if ("hidden".equals(type)) {
      return null;
    } else if ("submit".equals(type)) {
      return new InputButtonControl(markupElement);
    } else if ("password".equals(type)) {
      return new InputPasswordControl(markupElement);
    } else if ("radio".equals(type)) {
      return new InputRadioControl(markupElement);
    } else if ("checkbox".equals(type)) {
      return new InputCheckboxControl(markupElement);
    } else if ("image".equals(type)) {
      return new InputImageControl(markupElement);
    } else if ("reset".equals(type)) {
      return new InputButtonControl(markupElement);
    } else if ("button".equals(type)) {
      return new InputButtonControl(markupElement);
    } else if ("file".equals(type)) {
      return new InputFileControl(markupElement);
    } else {
      return null;
    }
  }

  /**
   * Gets offset from the left due to floats. It includes padding.
   */
  private final int fetchLeftOffset(final int newLineY) {
    final Insets paddingInsets = this.paddingInsets;
    final FloatingBounds floatBounds = this.floatBounds;
    if (floatBounds == null) {
      return paddingInsets.left;
    }
    final int left = floatBounds.getLeft(newLineY);
    return Math.max(left, paddingInsets.left);
  }

  /**
   * Gets offset from the right due to floats. It includes padding.
   */
  private final int fetchRightOffset(final int newLineY) {
    final Insets paddingInsets = this.paddingInsets;
    final FloatingBounds floatBounds = this.floatBounds;
    if (floatBounds == null) {
      return paddingInsets.right;
    }
    final int right = floatBounds.getRight(newLineY);
    return Math.max(right, paddingInsets.right);
  }

  private static final SizeExceededException SEE = new SizeExceededException();

  private final void checkY(final int y) {
    if ((this.yLimit != -1) && (y > this.yLimit)) {
      throw SEE;
    }
  }

  private final void layoutFloat(final RElement renderable, final boolean layout, final boolean leftFloat) {
    renderable.setOriginalParent(this);
    if (layout) {
      final int availWidth = this.availContentWidth;
      final int availHeight = this.availContentHeight;
      if (renderable instanceof RBlock) {
        final RBlock block = (RBlock) renderable;
        // Float boxes don't inherit float bounds?
        block.layout(availWidth, availHeight, false, false, null, this.sizeOnly);
      } else {
        renderable.layout(availWidth, availHeight, this.sizeOnly);
      }
    }
    final RFloatInfo floatInfo = new RFloatInfo(renderable.getModelNode(), renderable, leftFloat);

    // TODO: WHy is this required? Could RFloatInfo be removed completely?
    this.currentLine.simplyAdd(floatInfo);

    this.scheduleFloat(floatInfo);
  }

  /**
   * @param absolute    if true, then position is absolute, else fixed
   */
  private void scheduleAbsDelayedPair(final @NonNull BoundableRenderable renderable,
      final String leftText, final String rightText, final String topText, final String bottomText,
      final String widthText, final String heightText,
      final RenderState rs, final int currX, final int currY, final boolean absolute) {
    // It gets reimported in the local
    // viewport if it turns out it can't be exported up.
    final RenderableContainer containingBlock = absolute ? getPositionedAncestor(this.container) : getRootContainer(container);
    final int pos = absolute ? RenderState.POSITION_ABSOLUTE : RenderState.POSITION_FIXED;
    final DelayedPair pair = new DelayedPair(this.container, containingBlock, renderable, leftText, rightText, topText, bottomText, widthText, heightText, rs, currX, currY, pos);
    this.container.addDelayedPair(pair);
  }

  private static RenderableContainer getRootContainer(final RenderableContainer container) {
    RenderableContainer c = container.getParentContainer();
    RenderableContainer prevC = container;
    for (;;) {
      final RenderableContainer newContainer = c.getParentContainer();
      if (newContainer == null) {
        break;
      }
      prevC = c;
      c = newContainer;
    }
    return prevC;
  }

  /** Gets an ancestor which is "positioned" (that is whose position is not static).
   *  Stops searching when HTML element is encountered.
   */
  private static RenderableContainer getPositionedAncestor(RenderableContainer containingBlock) {
    for (;;) {
      if (containingBlock instanceof Renderable) {
        final ModelNode node = ((Renderable) containingBlock).getModelNode();
        if (node instanceof HTMLElementImpl) {
          final HTMLElementImpl element = (HTMLElementImpl) node;
          final int position = getPosition(element);
          // if (position != RenderState.POSITION_STATIC || (element instanceof HTMLHtmlElement)) {
          if (position != RenderState.POSITION_STATIC) {
            break;
          }
          final RenderableContainer newContainer = containingBlock.getParentContainer();
          if (newContainer == null) {
            break;
          }
          containingBlock = newContainer;
        } else {
          break;
        }
      } else {
        break;
      }
    }
    return containingBlock;
  }

  void importDelayedPair(final DelayedPair pair) {
    // if (!pair.isAdded()) {
    // pair.markAdded();
    final BoundableRenderable r = pair.positionPairChild();
    // final BoundableRenderable r = pair.child;
    this.addPositionedRenderable(r, false, false, pair.isFixed);
    // Size of block does not change - it's  set in stone?
    // }
  }

  private final void addPositionedRenderable(final @NonNull BoundableRenderable renderable, final boolean verticalAlignable, final boolean isFloat, final boolean isFixed) {
    addPositionedRenderable(renderable, verticalAlignable, isFloat, isFixed, false);
  }

  private final void addPositionedRenderable(final @NonNull BoundableRenderable renderable, final boolean verticalAlignable, final boolean isFloat, final boolean isFixed, final boolean isDelegated) {
    // Expected to be called only in GUI thread.
    final PositionedRenderable pr = new PositionedRenderable(renderable, verticalAlignable, this.positionedOrdinal++, isFloat, isFixed, isDelegated);
    addPosRenderable(pr);
    renderable.setParent(this);
    if (renderable instanceof RUIControl) {
      this.container.addComponent(((RUIControl) renderable).widget.getComponent());
    }
  }

  private void addPosRenderable(final PositionedRenderable pr) {
    // System.out.println("Adding : " + pr);
    // System.out.println("  to: " + this);
    SortedSet<PositionedRenderable> others = this.positionedRenderables;
    if (others == null) {
      others = new TreeSet<>(new ZIndexComparator());
      this.positionedRenderables = others;
    }
    others.add(pr);
    // System.out.println("  total: " + others.size());
  }

  public int getFirstLineHeight() {
    final ArrayList<BoundableRenderable> renderables = this.seqRenderables;
    if (renderables != null) {
      final int size = renderables.size();
      if (size == 0) {
        return 0;
      }
      for (int i = 0; i < size; i++) {
        final BoundableRenderable br = renderables.get(0);
        final int height = br.getHeight();
        if (height != 0) {
          return height;
        }
      }
    }
    // Not found!!
    return 1;
  }

  public int getFirstBaselineOffset() {
    final ArrayList<BoundableRenderable> renderables = this.seqRenderables;
    if (renderables != null) {
      final Iterator<BoundableRenderable> i = renderables.iterator();
      while (i.hasNext()) {
        final Object r = i.next();
        if (r instanceof RLine) {
          final int blo = ((RLine) r).getBaselineOffset();
          if (blo != 0) {
            return blo;
          }
        } else if (r instanceof RBlock) {
          final RBlock block = (RBlock) r;
          if (block.getHeight() > 0) {
            final Insets insets = block.getInsetsMarginBorder(false, false);
            final Insets paddingInsets = this.paddingInsets;
            return block.getFirstBaselineOffset() + insets.top + (paddingInsets == null ? 0 : paddingInsets.top);
          }
        }
      }
    }
    return 0;
  }

  // ----------------------------------------------------------------

  public RenderableSpot getLowestRenderableSpot(final int x, final int y) {
    final BoundableRenderable br = this.getRenderable(new Point(x, y));
    if (br != null) {
      return br.getLowestRenderableSpot(x - br.getX(), y - br.getY());
    } else {
      return new RenderableSpot(this, x, y);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.html.renderer.BoundableRenderable#onMouseClick(java.awt.event
   * .MouseEvent, int, int)
   */
  /*
  public boolean onMouseClick(final MouseEvent event, final int x, final int y) {
    final Iterator<? extends Renderable> i = this.getRenderables(new Point(x, y));
    if (i != null) {
      while (i.hasNext()) {
        final BoundableRenderable br = (BoundableRenderable) i.next();
        if (br != null) {
          // final Rectangle bounds = br.getVisualBounds();
          // if (!br.onMouseClick(event, x - bounds.x, y - bounds.y)) {
          final Point or = br.getOriginRelativeTo(this);
          if (!br.onMouseClick(event, x - or.x, y - or.y)) {
            return false;
          }
        }
      }
    }
    return true;
  }*/

  public boolean onDoubleClick(final MouseEvent event, final int x, final int y) {
    final Iterator<? extends Renderable> i = this.getRenderables(new Point(x, y));
    if (i != null) {
      while (i.hasNext()) {
        final BoundableRenderable br = (BoundableRenderable) i.next();
        if (br != null) {
          // final Rectangle bounds = br.getVisualBounds();
          // if (!br.onDoubleClick(event, x - bounds.x, y - bounds.y)) {
          final Point or = br.getOriginRelativeTo(this);
          if (!br.onMouseClick(event, x - or.x, y - or.y)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.html.renderer.BoundableRenderable#onMouseDisarmed(java.awt.event
   * .MouseEvent)
   */
  public boolean onMouseDisarmed(final MouseEvent event) {
    final BoundableRenderable br = this.armedRenderable;
    if (br != null) {
      try {
        return br.onMouseDisarmed(event);
      } finally {
        this.armedRenderable = null;
      }
    } else {
      return true;
    }
  }

  private BoundableRenderable armedRenderable;

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.html.renderer.BoundableRenderable#onMousePressed(java.awt.event
   * .MouseEvent, int, int)
   */
  /*
  public boolean onMousePressed(final MouseEvent event, final int x, final int y) {
    final Iterator<? extends Renderable> i = this.getRenderables(new Point(x, y));
    if (i != null) {
      while (i.hasNext()) {
        final BoundableRenderable br = (BoundableRenderable) i.next();
        if (br != null) {
          // final Rectangle bounds = br.getVisualBounds();
          // if (!br.onMousePressed(event, x - bounds.x, y - bounds.y)) {
          final Point or = br.getOriginRelativeTo(this);
          if (!br.onMousePressed(event, x - or.x, y - or.y)) {
            this.armedRenderable = br;
            return false;
          }
        }
      }
    }
    return true;
  }
  */

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.html.renderer.BoundableRenderable#onMouseReleased(java.awt.event
   * .MouseEvent, int, int)
   */
  public boolean onMouseReleased(final MouseEvent event, final int x, final int y) {
    final Iterator<? extends Renderable> i = this.getRenderables(new Point(x, y));
    if (i != null) {
      while (i.hasNext()) {
        final BoundableRenderable br = (BoundableRenderable) i.next();
        if (br != null) {
          // final Rectangle bounds = br.getVisualBounds();
          // if (!br.onMouseReleased(event, x - bounds.x, y - bounds.y)) {
          final Point or = br.getOriginRelativeTo(this);
          if (!br.onMouseReleased(event, x - or.x, y - or.y)) {
            final BoundableRenderable oldArmedRenderable = this.armedRenderable;
            if ((oldArmedRenderable != null) && (br != oldArmedRenderable)) {
              oldArmedRenderable.onMouseDisarmed(event);
              this.armedRenderable = null;
            }
            return false;
          }
        }
      }
    }
    final BoundableRenderable oldArmedRenderable = this.armedRenderable;
    if (oldArmedRenderable != null) {
      oldArmedRenderable.onMouseDisarmed(event);
      this.armedRenderable = null;
    }
    return true;
  }

  public void paint(final Graphics gIn) {
    paint(gIn, gIn);
  }

  public void paint(final Graphics gIn, Graphics gInUnClipped) {
    final boolean translationRequired = (x | y) != 0;
    final Graphics g = translationRequired ? gIn.create() : gIn;
    if (translationRequired) {
      g.translate(x, y);
    }
    final Graphics gUnClipped = translationRequired ? gInUnClipped.create() : gInUnClipped;
    if (translationRequired) {
      gUnClipped.translate(x, y);
    }
    try {
      final Rectangle clipBounds = gUnClipped.getClipBounds();
      final Iterator<Renderable> i = this.getRenderables(clipBounds);
      if (i != null) {
        while (i.hasNext()) {
          final Renderable robj = i.next();
          // The expected behavior in HTML is for boxes
          // not to be clipped unless overflow=hidden.
          if (robj instanceof BoundableRenderable) {
            final BoundableRenderable renderable = (BoundableRenderable) robj;
            // if (!((renderable instanceof RBlock) && renderable.getModelNode().getRenderState().getPosition() == RenderState.POSITION_RELATIVE)) {
            if (!renderable.isDelegated()) {
              renderable.paintTranslated(g);
            }
          } else {
            // PositionedRenderable, etc because they don't inherit from BoundableRenderable
            final boolean isReplacedElement = robj.isReplacedElement();
            final Graphics selectedG = isReplacedElement ? (robj.isFixed() ? gIn: g) : (robj.isFixed() ? gInUnClipped : gUnClipped);

            if (getModelNode() instanceof HTMLDocument) {
              Renderable htmlRenderable = RenderUtils.findHtmlRenderable(this);
              if (htmlRenderable instanceof PositionedRenderable) {
                final PositionedRenderable htmlPR = (PositionedRenderable) htmlRenderable;
                htmlRenderable = htmlPR.renderable;
              }
              // TODO: Handle other renderable types such as RTable
              if (htmlRenderable instanceof RBlock) {
                final Rectangle htmlBounds = ((RBlock) htmlRenderable).getClipBoundsWithoutInsets();
                if (htmlBounds != null) {
                  final Graphics clippedG = selectedG.create(0, 0, htmlBounds.width, htmlBounds.height);
                  try {
                    robj.paint(clippedG);
                  } finally {
                    clippedG.dispose();
                  }
                } else {
                  robj.paint(selectedG);
                }
              } else {
                robj.paint(selectedG);
              }
            } else {
              robj.paint(selectedG);
            }
          }
        }
      }
    } finally {
      if (translationRequired) {
        g.dispose();
        gUnClipped.dispose();
      }
    }
  }

  // ----------------------------------------------------------------

  private static class NopLayout implements MarkupLayout {
    public void layoutMarkup(final RBlockViewport bodyLayout, final HTMLElementImpl markupElement) {
    }
  }

  private static class NoScriptLayout implements MarkupLayout {
    public void layoutMarkup(final RBlockViewport bodyLayout, final HTMLElementImpl markupElement) {
      final UserAgentContext ucontext = bodyLayout.userAgentContext;
      if (!ucontext.isScriptingEnabled()) {
        bodyLayout.layoutMarkup(markupElement);
      } else {
        // NOP
      }
    }
  }

  private static class BrLayout implements MarkupLayout {
    /*
     * (non-Javadoc)
     *
     * @see
     * org.xamjwg.html.renderer.MarkupLayout#layoutMarkup(java.awt.Container,
     * java.awt.Insets, org.xamjwg.html.domimpl.HTMLElementImpl)
     */
    public void layoutMarkup(final RBlockViewport bodyLayout, final HTMLElementImpl markupElement) {
      final String clear = markupElement.getAttribute("clear");
      bodyLayout.addLineBreak(markupElement, LineBreak.getBreakType(clear));
    }
  }

  private static class HrLayout implements MarkupLayout {
    /*
     * (non-Javadoc)
     *
     * @see
     * org.xamjwg.html.renderer.MarkupLayout#layoutMarkup(java.awt.Container,
     * java.awt.Insets, org.xamjwg.html.domimpl.HTMLElementImpl)
     */
    public void layoutMarkup(final RBlockViewport bodyLayout, final HTMLElementImpl markupElement) {
      bodyLayout.layoutHr(markupElement);
    }
  }

  private static class ObjectLayout extends CommonWidgetLayout {
    private final boolean tryToRenderContent;

    /**
     * @param tryToRenderContent
     *          If the object is unknown, content is rendered as HTML.
     * @param usesAlignAttribute
     */
    public ObjectLayout(final boolean tryToRenderContent, final boolean usesAlignAttribute) {
      super(ADD_INLINE, usesAlignAttribute);
      this.tryToRenderContent = tryToRenderContent;
    }

    /**
     * Must use this ThreadLocal because an ObjectLayout instance is shared
     * across renderers.
     */
    private final ThreadLocal<HtmlObject> htmlObject = new ThreadLocal<>();

    @Override
    public void layoutMarkup(final RBlockViewport bodyLayout, final HTMLElementImpl markupElement) {
      final HtmlObject ho = bodyLayout.rendererContext.getHtmlObject(markupElement);
      if ((ho == null) && this.tryToRenderContent) {
        // Don't know what to do with it - render contents.
        bodyLayout.layoutMarkup(markupElement);
      } else if (ho != null) {
        this.htmlObject.set(ho);
        super.layoutMarkup(bodyLayout, markupElement);
      }
    }

    @Override
    protected RElement createRenderable(final RBlockViewport bodyLayout, final HTMLElementImpl markupElement) {
      final HtmlObject ho = this.htmlObject.get();
      final UIControl uiControl = new UIControlWrapper(ho);
      final RUIControl ruiControl = new RUIControl(markupElement, uiControl, bodyLayout.container, bodyLayout.frameContext,
          bodyLayout.userAgentContext);
      return ruiControl;
    }
  }

  private static class ImgLayout extends CommonWidgetLayout {
    public ImgLayout() {
      super(ADD_INLINE, true);
    }

    @Override
    protected RElement createRenderable(final RBlockViewport bodyLayout, final HTMLElementImpl markupElement) {
      final ImgControl control = new ImgControl((HTMLImageElementImpl) markupElement);
      return new RImgControl(markupElement, control, bodyLayout.container, bodyLayout.frameContext, bodyLayout.userAgentContext);
    }
  }

  private static class CanvasLayout extends CommonWidgetLayout {
    public CanvasLayout() {
      super(ADD_INLINE, false);
    }

    @Override
    protected RElement createRenderable(final RBlockViewport bodyLayout, final HTMLElementImpl markupElement) {
      final HTMLCanvasElementImpl canvasImpl = (HTMLCanvasElementImpl) markupElement;
      return new RUIControl(markupElement, new CanvasControl(canvasImpl), bodyLayout.container, bodyLayout.frameContext, bodyLayout.userAgentContext);
    }

    static class CanvasControl extends BaseControl {
      private static final long serialVersionUID = -3487994653091311061L;
      private final HTMLCanvasElementImpl canvasNode;

      public CanvasControl(HTMLCanvasElementImpl canvasNode) {
        super(canvasNode);
        this.canvasNode = canvasNode;
      }

      public void paintComponent(final Graphics g) {
        canvasNode.paintComponent(g);
      }

      @Override
      public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        final Insets insets = ruicontrol.getInsetsMarginPadding();
        canvasNode.setBounds(insets.left, insets.top, width - (insets.left + insets.right), height - (insets.top + insets.bottom));
      }

    }
  }

  private static class InputLayout2 extends CommonWidgetLayout {
    public InputLayout2() {
      super(ADD_INLINE, true);
    }

    @Override
    protected RElement createRenderable(final RBlockViewport bodyLayout, final HTMLElementImpl markupElement) {
      final HTMLBaseInputElement bie = (HTMLBaseInputElement) markupElement;
      final BaseInputControl uiControl = createInputControl(bie);
      if (uiControl == null) {
        return null;
      }
      bie.setInputContext(uiControl);
      return new RUIControl(markupElement, uiControl, bodyLayout.container, bodyLayout.frameContext, bodyLayout.userAgentContext);
    }
  }

  private static class SelectLayout extends CommonWidgetLayout {
    public SelectLayout() {
      super(ADD_INLINE, true);
    }

    @Override
    protected RElement createRenderable(final RBlockViewport bodyLayout, final HTMLElementImpl markupElement) {
      final HTMLBaseInputElement bie = (HTMLBaseInputElement) markupElement;
      final BaseInputControl uiControl = new InputSelectControl(bie);
      bie.setInputContext(uiControl);
      return new RUIControl(markupElement, uiControl, bodyLayout.container, bodyLayout.frameContext, bodyLayout.userAgentContext);
    }
  }

  private static class TextAreaLayout2 extends CommonWidgetLayout {
    public TextAreaLayout2() {
      super(ADD_INLINE, true);
    }

    @Override
    protected RElement createRenderable(final RBlockViewport bodyLayout, final HTMLElementImpl markupElement) {
      final HTMLBaseInputElement bie = (HTMLBaseInputElement) markupElement;
      final BaseInputControl control = new InputTextAreaControl(bie);
      bie.setInputContext(control);
      return new RUIControl(markupElement, control, bodyLayout.container, bodyLayout.frameContext, bodyLayout.userAgentContext);
    }
  }

  private static class IFrameLayout extends CommonWidgetLayout {
    public IFrameLayout() {
      super(ADD_INLINE, true);
    }

    @Override
    protected RElement createRenderable(final RBlockViewport bodyLayout, final HTMLElementImpl markupElement) {
      final BrowserFrame frame = bodyLayout.rendererContext.createBrowserFrame();
      ((HTMLIFrameElementImpl) markupElement).setBrowserFrame(frame);
      final UIControl control = new BrowserFrameUIControl(markupElement, frame);
      return new RUIControl(markupElement, control, bodyLayout.container, bodyLayout.frameContext, bodyLayout.userAgentContext);
    }
  }

  // ------------------------------------------------------------------------

  /**
   * This is layout common to elements that render themselves, except RBlock,
   * RTable and RList.
   */
  private static abstract class CommonWidgetLayout implements MarkupLayout {
    protected static final int ADD_INLINE = 0;
    protected static final int ADD_AS_BLOCK = 1;
    protected static final int ADD_INLINE_BLOCK = 2;
    private final int method;
    private final boolean useAlignAttribute;

    public CommonWidgetLayout(final int method, final boolean usesAlignAttribute) {
      this.method = method;
      this.useAlignAttribute = usesAlignAttribute;
    }

    public void layoutMarkup(final RBlockViewport bodyLayout, final HTMLElementImpl markupElement) {
      final JStyleProperties style = markupElement.getCurrentStyle();
      int currMethod = this.method;
      {
        final String display = style.getDisplay();
        if (display != null) {
          if ("none".equalsIgnoreCase(display)) {
            // For hidden iframes: GH-140
            // return;
            if (!"iframe".equalsIgnoreCase(markupElement.getNodeName())) {
              return;
            }
          } else if ("block".equalsIgnoreCase(display)) {
            currMethod = ADD_AS_BLOCK;
          } else if ("inline".equalsIgnoreCase(display)) {
            currMethod = ADD_INLINE;
          } else if ("display-inline".equalsIgnoreCase(display)) {
            currMethod = ADD_INLINE_BLOCK;
          }
        }
      }
      final UINode node = markupElement.getUINode();
      RElement renderable = null;
      if (node == null) {
        renderable = this.createRenderable(bodyLayout, markupElement);
        if (renderable == null) {
          if (logger.isLoggable(Level.INFO)) {
            logger.info("layoutMarkup(): Don't know how to render " + markupElement + ".");
          }
          return;
        }
        markupElement.setUINode(renderable);
      } else {
        renderable = (RElement) node;
      }
      renderable.setOriginalParent(bodyLayout);
      switch (currMethod) {
      case ADD_INLINE:
      case ADD_INLINE_BLOCK:
        bodyLayout.addRenderableToLineCheckStyle(renderable, markupElement, this.useAlignAttribute);
        break;
      case ADD_AS_BLOCK:
        bodyLayout.positionRElement(markupElement, renderable, this.useAlignAttribute, true, false);
        break;
      }
    }

    protected abstract RElement createRenderable(RBlockViewport bodyLayout, HTMLElementImpl markupElement);
  }

  private static final class CommonLayout implements MarkupLayout {
    public CommonLayout() {
    }

    public void layoutMarkup(final RBlockViewport bodyLayout, final HTMLElementImpl markupElement) {
      final RenderState rs = markupElement.getRenderState();
      int display = rs.getDisplay();
      if (display == RenderState.DISPLAY_INLINE || display == RenderState.DISPLAY_INLINE_BLOCK || display == RenderState.DISPLAY_INLINE_TABLE) {
        // Inline elements with absolute or fixed positions need to be treated as blocks.
        // TODO: ^^Verify; is that an internal hack or a spec requirement?
        final int position = rs.getPosition();
        if ((position == RenderState.POSITION_ABSOLUTE) || (position == RenderState.POSITION_FIXED)) {
          display = RenderState.DISPLAY_BLOCK;
        } else {
          final int boxFloat = rs.getFloat();
          if (boxFloat != RenderState.FLOAT_NONE) {
            display = RenderState.DISPLAY_BLOCK;
          }
        }
      }
      switch (display) {
      case RenderState.DISPLAY_TABLE_COLUMN:
      case RenderState.DISPLAY_TABLE_COLUMN_GROUP:
      case RenderState.DISPLAY_NONE:
        // skip it completely.
        final UINode node = markupElement.getUINode();
        if (node instanceof BaseBoundableRenderable) {
          // This is necessary so that if the element is made
          // visible again, it can be invalidated.
          ((BaseBoundableRenderable) node).markLayoutValid();
        }
        break;
      case RenderState.DISPLAY_BLOCK:
        //TODO refer issue #87
        final String tagName = markupElement.getTagName();
        if ("UL".equalsIgnoreCase(tagName) || "OL".equalsIgnoreCase(tagName)) {
          bodyLayout.layoutList(markupElement);
        } else {
          bodyLayout.layoutRBlock(markupElement);
        }
        break;
      case RenderState.DISPLAY_LIST_ITEM:
        bodyLayout.layoutListItem(markupElement);
        break;
      case RenderState.DISPLAY_TABLE:
        bodyLayout.layoutRTable(markupElement);
        break;
      case RenderState.DISPLAY_INLINE_TABLE:
        bodyLayout.layoutRInlineBlock(markupElement);
        break;
      case RenderState.DISPLAY_INLINE_BLOCK:
        bodyLayout.layoutRInlineBlock(markupElement);
        break;
      default:
        // Assume INLINE
        bodyLayout.layoutMarkup(markupElement);
        break;
      }
    }
  }

  public boolean isContainedByNode() {
    return false;
  }

  private void layoutRInlineBlock(final HTMLElementImpl markupElement) {
    final UINode uINode = markupElement.getUINode();
    RInlineBlock inlineBlock = null;
    if (uINode instanceof RInlineBlock) {
      inlineBlock = (RInlineBlock) uINode;
    } else {
      final RInlineBlock newInlineBlock = new RInlineBlock(container, markupElement, userAgentContext, rendererContext, frameContext);
      markupElement.setUINode(newInlineBlock);
      inlineBlock = newInlineBlock;
    }
    inlineBlock.doLayout(availContentWidth, availContentHeight, sizeOnly);
    addRenderableToLine(inlineBlock);
    inlineBlock.setOriginalParent(inlineBlock.getParent());
    bubbleUpIfRelative(markupElement, inlineBlock);
  }

  @Override
  public String toString() {
    return "RBlockViewport[node=" + this.modelNode + "]";
  }

  // /**
  // * Performs layout adjustment step.
  // * @param desiredWidth The desired viewport width, including padding.
  // * @param desiredHeight The desired viewport height, including padding.
  // * @param paddingInsets The padding insets.
  // * @param floatBounds The starting float bounds, including floats
  // * in ancestors.
  // */
  // public void adjust(int desiredWidth, int desiredHeight, Insets
  // paddingInsets, FloatingBounds floatBounds) {
  // // Initializations
  // this.paddingInsets = paddingInsets;
  // this.desiredHeight = desiredHeight;
  // this.desiredWidth = desiredWidth;
  // this.floatBounds = floatBounds;
  //
  // int availw = desiredWidth - paddingInsets.left - paddingInsets.right;
  // if(availw < 0) {
  // availw = 0;
  // }
  // int availh = desiredHeight - paddingInsets.top - paddingInsets.bottom;
  // if(availh < 0) {
  // availh = 0;
  // }
  // this.availContentWidth = availw;
  // this.availContentHeight = availh;
  //
  // // maxX and maxY should not be reset by layoutPass.
  // this.maxX = paddingInsets.left;
  // this.maxY = paddingInsets.top;
  //
  // // Keep copy of old sequential renderables,
  // // and clear the list.
  // ArrayList oldSeqRenderables = this.seqRenderables;
  // this.seqRenderables = null;
  //
  // // Clear current line
  // this.currentLine = null;
  //
  // // Reprocess all sequential renderables
  // if(oldSeqRenderables != null) {
  // Iterator i = oldSeqRenderables.iterator();
  // while(i.hasNext()) {
  // Renderable r = (Renderable) i.next();
  // this.reprocessSeqRenderable(r);
  // }
  // }
  //
  // RLine lastLine = this.currentLine;
  //
  // // This adds any pending floats
  // this.lineDone(this.currentLine);
  //
  // // Calculate maxX and maxY.
  // if(lastLine != null) {
  // Rectangle lastBounds = lastLine.getBounds();
  // int lastTopX = lastBounds.x + lastBounds.width;
  // if(lastTopX > this.maxX) {
  // this.maxX = lastTopX;
  // }
  // int lastTopY = lastBounds.y + lastBounds.height;
  // int maxY = this.maxY;
  // if(lastTopY > maxY) {
  // this.maxY = maxY = lastTopY;
  // }
  // }
  //
  // // Check positioned renderables for maxX and maxY
  // SortedSet posRenderables = this.positionedRenderables;
  // if(posRenderables != null) {
  // Iterator i = posRenderables.iterator();
  // while(i.hasNext()) {
  // PositionedRenderable pr = (PositionedRenderable) i.next();
  // BoundableRenderable br = pr.renderable;
  // if(br.getX() + br.getWidth() > this.maxX) {
  // this.maxX = br.getX() + br.getWidth();
  // }
  // if(br.getY() + br.getHeight() > this.maxY) {
  // this.maxY = br.getY() + br.getHeight();
  // }
  // }
  // }
  //
  // this.width = paddingInsets.right + this.maxX;
  // this.height = paddingInsets.bottom + maxY;
  // }

  // private void reprocessSeqRenderable(Renderable r) {
  // if(r instanceof RLine) {
  // this.reprocessLine((RLine) r);
  // }
  // else if(r instanceof RElement) {
  // this.reprocessElement((RElement) r);
  // }
  // else if(r instanceof RRelative) {
  // this.reprocessRelative((RRelative) r);
  // }
  // else {
  // throw new IllegalStateException("Unexpected Renderable: " + r);
  // }
  // }
  //
  // private void reprocessLine(RLine line) {
  // Iterator renderables = line.getRenderables();
  // if(renderables != null) {
  // while(renderables.hasNext()) {
  // Renderable r = (Renderable) renderables.next();
  // if(this.currentLine == null) {
  // // Must add at this point in case there was a float.
  // this.currentLine = this.addLine(r.getModelNode(), null,
  // this.paddingInsets.top);
  // }
  // if(r instanceof RWord) {
  // RWord word = (RWord) r;
  // this.addWordToLine(word);
  // }
  // else if (r instanceof RFloatInfo) {
  // RFloatInfo oldr = (RFloatInfo) r;
  // // Switch to a float info with registerElement=true.
  // this.scheduleFloat(new RFloatInfo(oldr.getModelNode(),
  // oldr.getRenderable(), oldr.isLeftFloat()));
  // }
  // else if (r instanceof RStyleChanger) {
  // RStyleChanger sc = (RStyleChanger) r;
  // RenderState rs = sc.getModelNode().getRenderState();
  // int whiteSpace = rs == null ? RenderState.WS_NORMAL : rs.getWhiteSpace();
  // boolean isAO = this.currentLine.isAllowOverflow();
  // if(!isAO && whiteSpace == RenderState.WS_NOWRAP) {
  // this.currentLine.setAllowOverflow(true);
  // }
  // else if(isAO && whiteSpace != RenderState.WS_NOWRAP) {
  // this.currentLine.setAllowOverflow(false);
  // }
  // this.addRenderableToLine(r);
  // }
  // else {
  // this.addRenderableToLine(r);
  // }
  // }
  // }
  // LineBreak br = line.getLineBreak();
  // if(br != null) {
  // this.addLineBreak(br.getModelNode(), br.getBreakType());
  // }
  // }
  //
  // private void reprocessElement(RElement element) {
  // RLine line = this.currentLine;
  // this.lineDone(line);
  // boolean isRBlock = element instanceof RBlock;
  // boolean obeysFloats = !isRBlock || !((RBlock) element).isOverflowVisibleY()
  // || !((RBlock) element).isOverflowVisibleY();
  // if(obeysFloats) {
  // if(isRBlock) {
  // RBlock block = (RBlock) element;
  // int newLineY = line == null ? this.paddingInsets.top : line.y +
  // line.height;
  // int leftOffset = this.fetchLeftOffset(newLineY);
  // int rightOffset = this.fetchRightOffset(newLineY);
  // int availContentWidth = this.desiredWidth - leftOffset - rightOffset;
  // block.adjust(availContentWidth, this.availContentHeight, true, false, null,
  // true);
  // // Because a block that obeys margins is also a float limit,
  // // we don't expect exported float bounds.
  // }
  // else if(element instanceof RTable) {
  // RTable table = (RTable) element;
  // int newLineY = line == null ? this.paddingInsets.top : line.y +
  // line.height;
  // int leftOffset = this.fetchLeftOffset(newLineY);
  // int rightOffset = this.fetchRightOffset(newLineY);
  // int availContentWidth = this.desiredWidth - leftOffset - rightOffset;
  // table.adjust(availContentWidth, this.availContentHeight);
  // }
  // }
  // else {
  // RBlock block = (RBlock) element;
  // final FloatingBounds currentFloatBounds = this.floatBounds;
  // FloatingBoundsSource blockFloatBoundsSource = null;
  // if(currentFloatBounds != null) {
  // Insets paddingInsets = this.paddingInsets;
  // final int blockShiftX = paddingInsets.left;
  // final int blockShiftRight = paddingInsets.right;
  // final int blockShiftY = line == null ? paddingInsets.top : line.y +
  // line.height;
  // final int expectedBlockWidth = this.availContentWidth;
  // blockFloatBoundsSource = new FloatingBoundsSource() {
  // public FloatingBounds getChildBlockFloatingBounds(int apparentBlockWidth) {
  // int actualRightShift = blockShiftRight + (expectedBlockWidth -
  // apparentBlockWidth);
  // return new ShiftedFloatingBounds(currentFloatBounds, -blockShiftX,
  // -actualRightShift, -blockShiftY);
  // }
  // };
  // }
  // block.adjust(this.availContentWidth, this.availContentHeight, true, false,
  // blockFloatBoundsSource, true);
  // FloatingBounds blockBounds = block.getExportableFloatingBounds();
  // if(blockBounds != null) {
  // FloatingBounds prevBounds = this.floatBounds;
  // FloatingBounds newBounds;
  // if(prevBounds == null) {
  // newBounds = blockBounds;
  // }
  // else {
  // newBounds = new CombinedFloatingBounds(prevBounds, blockBounds);
  // }
  // if(newBounds.getMaxY() > this.maxY && this.isFloatLimit()) {
  // this.maxY = newBounds.getMaxY();
  // }
  // }
  // }
  // this.addAsSeqBlock(element, obeysFloats, false);
  // }

  // private void reprocessRelative(RRelative relative) {
  // RLine line = this.currentLine;
  // this.lineDone(line);
  // boolean obeysFloats = false;
  // RElement element = relative.getElement();
  // if(element instanceof RBlock) {
  // obeysFloats = false;
  // RBlock block = (RBlock) element;
  // final FloatingBounds currentFloatBounds = this.floatBounds;
  // FloatingBoundsSource blockFloatBoundsSource = null;
  // if(currentFloatBounds != null) {
  // Insets paddingInsets = this.paddingInsets;
  // final int blockShiftX = paddingInsets.left + relative.getXOffset();
  // final int blockShiftRight = paddingInsets.right - relative.getXOffset();
  // final int blockShiftY = (line == null ? paddingInsets.top : line.y +
  // line.height) + relative.getYOffset();
  // final int expectedBlockWidth = this.availContentWidth;
  // blockFloatBoundsSource = new FloatingBoundsSource() {
  // public FloatingBounds getChildBlockFloatingBounds(int apparentBlockWidth) {
  // int actualRightShift = blockShiftRight + (expectedBlockWidth -
  // apparentBlockWidth);
  // return new ShiftedFloatingBounds(currentFloatBounds, -blockShiftX,
  // -actualRightShift, -blockShiftY);
  // }
  // };
  // }
  // block.adjust(this.availContentWidth, this.availContentHeight, true, false,
  // blockFloatBoundsSource, true);
  // relative.assignDimension();
  // FloatingBounds blockBounds = relative.getExportableFloatingBounds();
  // if(blockBounds != null) {
  // FloatingBounds prevBounds = this.floatBounds;
  // FloatingBounds newBounds;
  // if(prevBounds == null) {
  // newBounds = blockBounds;
  // }
  // else {
  // newBounds = new CombinedFloatingBounds(prevBounds, blockBounds);
  // }
  // if(newBounds.getMaxY() > this.maxY && this.isFloatLimit()) {
  // this.maxY = newBounds.getMaxY();
  // }
  // }
  // }
  // else {
  // obeysFloats = true;
  // }
  // this.addAsSeqBlock(relative, obeysFloats, false);
  // }

  private void scheduleFloat(final RFloatInfo floatInfo) {
    final RLine line = this.currentLine;
    if (line == null) {
      final int y = this.paddingInsets.top;
      this.placeFloat(floatInfo.getRenderable(), y, floatInfo.isLeftFloat());
    } else if (line.getWidth() == 0) {
      final int y = line.getY();
      this.placeFloat(floatInfo.getRenderable(), y, floatInfo.isLeftFloat());
      final int leftOffset = this.fetchLeftOffset(y);
      final int rightOffset = this.fetchRightOffset(y);
      line.changeLimits(leftOffset, this.desiredWidth - leftOffset - rightOffset);
    } else {
      // These pending floats are positioned when
      // lineDone() is called.
      Collection<RFloatInfo> c = this.pendingFloats;
      if (c == null) {
        c = new LinkedList<>();
        this.pendingFloats = c;
      }
      c.add(floatInfo);
    }
  }

  private Collection<RFloatInfo> pendingFloats = null;

  private void lineDone(final RLine line) {
    final Collection<RFloatInfo> pfs = this.pendingFloats;
    if (pfs != null) {
      this.pendingFloats = null;
      final Iterator<RFloatInfo> i = pfs.iterator();
      int yAfterLine = 0;
      boolean yComputed = false;
      while (i.hasNext()) {
        final RFloatInfo pf = i.next();
        if (!yComputed) {
          yAfterLine = line == null ? this.paddingInsets.top : (line.checkFit(pf.getRenderable()) ? line.y : line.y + line.height);
          yComputed = true;
        }
        this.placeFloat(pf.getRenderable(), yAfterLine, pf.isLeftFloat());
      }
    }
  }

  private void addExportableFloat(final RElement element, final boolean leftFloat, final int origX, final int origY, final boolean pendingPlacement) {
    ArrayList<ExportableFloat> ep = this.exportableFloats;
    if (ep == null) {
      ep = new ArrayList<>(1);
      this.exportableFloats = ep;
    }
    ExportableFloat ef = new ExportableFloat(element, leftFloat, origX, origY);
    ef.pendingPlacement = pendingPlacement;
    ep.add(ef);
  }

  private void addFloat(final RElement renderable, final int newX, final int newY) {
    renderable.setOrigin(newX, newY);
    // TODO: Enabling this causes problems in some cases. See GH #153
    // if (!bubbleUpIfRelative((HTMLElementImpl) renderable.getModelNode(), renderable)) {
      this.addPositionedRenderable(renderable, true, true, false);
    // } else {
      // this.addPositionedRenderable(renderable, true, true, false, true);
    // }
  }

  /**
   *
   * @param element
   * @param y
   *          The desired top position of the float element.
   * @param floatType
   *          -1 (left) or +1 (right)
   */
  private void placeFloat(final RElement element, final int y, final boolean leftFloat) {
    final Insets insets = this.paddingInsets;
    int boxY = y;
    int boxWidth = element.getWidth();
    int boxHeight = element.getHeight();
    final int desiredWidth = this.desiredWidth;
    int boxX;
    for (;;) {
      final int leftOffset = this.fetchLeftOffset(boxY);
      final int rightOffset = this.fetchRightOffset(boxY);
      boxX = leftFloat ? leftOffset : desiredWidth - rightOffset - boxWidth;
      if ((leftOffset == insets.left) && (rightOffset == insets.right)) {
        // Probably typical scenario. If it's overflowing to the left,
        // we need to correct.
        if (!leftFloat && (boxX < leftOffset)) {
          boxX = leftOffset;
        }
        break;
      }
      if ((desiredWidth <= 0) || (boxWidth <= (desiredWidth - rightOffset - leftOffset))) {
        // Size is fine.
        break;
      }
      // At this point the float doesn't fit at the current Y position.
      if (element instanceof RBlock) {
        // Try shrinking it.
        final RBlock relement = (RBlock) element;
        if (!relement.hasDeclaredWidth()) {
          final int availableBoxWidth = desiredWidth - rightOffset - leftOffset;
          relement.layout(availableBoxWidth, this.availContentHeight, this.sizeOnly);
          if (relement.getWidth() < boxWidth) {
            if (relement.getWidth() > (desiredWidth - rightOffset - leftOffset)) {
              // Didn't work out. Put it back the way it was.
              relement.layout(this.availContentWidth, this.availContentHeight, this.sizeOnly);
            } else {
              // Retry
              boxWidth = relement.getWidth();
              boxHeight = relement.getHeight();
              continue;
            }
          }
        }
      }
      final FloatingBounds fb = this.floatBounds;
      final int newY = fb == null ? boxY + boxHeight : fb.getFirstClearY(boxY);
      if (newY == boxY) {
        // Possible if prior box has height zero?
        break;
      }
      boxY = newY;
    }
    // Position element
    // element.setOrigin(boxX, boxY);
    // Update float bounds accordingly
    final int offsetFromBorder = leftFloat ? boxX + boxWidth : desiredWidth - boxX;
    this.floatBounds = new FloatingViewportBounds(this.floatBounds, leftFloat, boxY, offsetFromBorder, boxHeight);
    // Add element to collection
    final boolean isFloatLimit = this.isFloatLimit();

    boolean placementPending = true;
    if (getPosition((HTMLElementImpl)modelNode) != RenderState.POSITION_STATIC) {
      addFloat(element, boxX, boxY);
      placementPending = false;
    }

    if (isFloatLimit) {
      // System.out.println("Created float as renderable in " + this);
      // System.out.println("  r: " + element);
      if (placementPending) {
        addFloat(element, boxX, boxY);
      }
    } else {
      this.addExportableFloat(element, leftFloat, boxX, boxY, placementPending);
    }
    // Adjust maxX based on float.
    if ((boxX + boxWidth) > this.maxX) {
      this.maxX = boxX + boxWidth;
    }
    // Adjust maxY based on float, but only if this viewport is the float limit.
    if (isFloatLimit) {
      if ((boxY + boxHeight) > this.maxY) {
        this.maxY = boxY + boxHeight;
      }
    }
  }

  private Boolean isFloatLimit = null;

  private boolean isFloatLimit() {
    Boolean fl = this.isFloatLimit;
    if (fl == null) {
      fl = this.isFloatLimitImpl();
      this.isFloatLimit = fl;
    }
    return fl.booleanValue();
  }

  private Boolean isFloatLimitImpl() {
    final Object parent = this.getOriginalOrCurrentParent();
    if (!(parent instanceof RBlock)) {
      return Boolean.TRUE;
    }
    final RBlock blockParent = (RBlock) parent;
    final Object grandParent = blockParent.getOriginalOrCurrentParent();
    if (!(grandParent instanceof RBlockViewport)) {
      // Could be contained in a table, or it could
      // be a list item, for example.
      return Boolean.TRUE;
    }
    final ModelNode node = this.modelNode;
    if (!(node instanceof HTMLElementImpl)) {
      // Can only be a document here.
      return Boolean.TRUE;
    }
    final HTMLElementImpl element = (HTMLElementImpl) node;
    final int position = getPosition(element);
    if ((position == RenderState.POSITION_ABSOLUTE) || (position == RenderState.POSITION_FIXED)) {
      return Boolean.TRUE;
    }
    final RenderState rs = element.getRenderState();
    final int floatValue = rs.getFloat();
    if (floatValue != RenderState.FLOAT_NONE) {
      return Boolean.TRUE;
    }
    return !isOverflowVisibleOrNone(rs);
  }

  // /**
  // * Gets FloatingBounds from this viewport that should
  // * be considered by an ancestor block.
  // */
  // public FloatingBounds getExportableFloatingBounds() {
  // FloatingBounds floatBounds = this.floatBounds;
  // if(floatBounds == null) {
  // return null;
  // }
  // if(this.isFloatLimit()) {
  // return null;
  // }
  // int maxY = floatBounds.getMaxY();
  // if(maxY > this.height) {
  // return floatBounds;
  // }
  // return null;
  // }

  public FloatingInfo getExportableFloatingInfo() {
    final ArrayList<ExportableFloat> ef = this.exportableFloats;
    if (ef == null) {
      return null;
    }
    final ExportableFloat[] floats = ef.toArray(ExportableFloat.EMPTY_ARRAY);
    return new FloatingInfo(0, 0, floats);
  }

  private void importFloatingInfo(final FloatingInfo floatingInfo, final BoundableRenderable block) {
    final int shiftX = floatingInfo.shiftX + block.getX();
    final int shiftY = floatingInfo.shiftY + block.getY();
    final ExportableFloat[] floats = floatingInfo.floats;
    final int length = floats.length;
    for (int i = 0; i < length; i++) {
      final ExportableFloat ef = floats[i];
      this.importFloat(ef, shiftX, shiftY);
    }
  }

  private void importFloat(final ExportableFloat ef, final int shiftX, final int shiftY) {
    final RElement renderable = ef.element;
    final int newX = ef.origX + shiftX;
    final int newY = ef.origY + shiftY;
    // final int newX = ef.origX;
    // final int newY = ef.origY;
    // renderable.setOrigin(ef.origX + ef.visualX, ef.origY + ef.visualY);
    final FloatingBounds prevBounds = this.floatBounds;
    int offsetFromBorder;
    final boolean leftFloat = ef.leftFloat;
    if (leftFloat) {
      offsetFromBorder = newX + renderable.getWidth();
    } else {
      offsetFromBorder = this.desiredWidth - newX;
    }
    this.floatBounds = new FloatingViewportBounds(prevBounds, leftFloat, newY, offsetFromBorder, renderable.getHeight());

    if (ef.pendingPlacement && getPosition((HTMLElementImpl)modelNode) != RenderState.POSITION_STATIC) {
      // System.out.println("Adding float as renderable to " + this);
      addFloat(renderable, newX, newY);
      ef.pendingPlacement = false;
    }

    if (this.isFloatLimit()) {
      // this.addPositionedRenderable(renderable, true, true, false);
      if (ef.pendingPlacement) {
        // System.out.println("  r: " + renderable);
        addFloat(renderable, newX, newY);
        ef.pendingPlacement = false;
      }
    } else {
      this.addExportableFloat(renderable, leftFloat, newX, newY, ef.pendingPlacement);
    }
  }

  public void positionDelayed() {
    final Collection<DelayedPair> delayedPairs = container.getDelayedPairs();
    if ((delayedPairs != null) && (delayedPairs.size() > 0)) {
      // Add positioned renderables that belong here
      final Iterator<DelayedPair> i = delayedPairs.iterator();
      while (i.hasNext()) {
        final DelayedPair pair = i.next();
        if (pair.containingBlock == container) {
          this.importDelayedPair(pair);
        }
      }
    }
  }

  private Integer cachedVisualHeight = null;
  private Integer cachedVisualWidth = null;

  @Override
  public int getVisualHeight() {
    if (cachedVisualHeight != null) {
      return cachedVisualHeight;
    }
    double maxY = getHeight();
    final Iterator<? extends Renderable> renderables = getRenderables();
    if (renderables != null) {
      while (renderables.hasNext()) {
        final Renderable r = renderables.next();
        if (r instanceof RenderableContainer) {
          final RenderableContainer rc = (RenderableContainer) r;
          // double rcMaxY = rc.getVisualBounds().getMaxY();
          // final Insets rcInsets = rc.getInsetsMarginBorder(false, false);
          // double rcMaxY = rc.getY() + rc.getVisualHeight() + rcInsets.top + rcInsets.bottom;
          // double rcMaxY = rc.getVisualBounds().getMaxY() + rcInsets.top + rcInsets.bottom;
          double rcMaxY = rc.getVisualBounds().getMaxY(); //  + rcInsets.bottom;
          if (rcMaxY > maxY) {
            maxY = rcMaxY;
          }
        } else if (r instanceof BoundableRenderable) {
          final BoundableRenderable br = (BoundableRenderable) r;
          double brMaxY = br.getVisualBounds().getMaxY();
          if (brMaxY > maxY) {
            maxY = brMaxY;
          }
        } else if (r instanceof PositionedRenderable) {
          final PositionedRenderable rc = (PositionedRenderable) r;
          double rcMaxY = rc.renderable.getVisualBounds().getMaxY();
          if (rcMaxY > maxY) {
            maxY = rcMaxY;
          }
        } else {
          System.err.println("Unhandled renderable: " + r);
        }
      }
    }
    cachedVisualHeight = (int) maxY;
    return cachedVisualHeight;
  }

  @Override
  public int getVisualWidth() {
    if (cachedVisualWidth != null) {
      return cachedVisualWidth;
    }
    double maxX = getWidth();
    final Iterator<? extends Renderable> renderables = getRenderables();
    if (renderables != null) {
      while (renderables.hasNext()) {
        final Renderable r = renderables.next();
        if (r instanceof RenderableContainer) {
          final RenderableContainer rc = (RenderableContainer) r;
          final Insets rcInsets = rc.getInsetsMarginBorder(false, false);
          double rcMaxX = rc.getX() + rc.getVisualWidth() + rcInsets.left + rcInsets.right;
          if (rcMaxX > maxX) {
            maxX = rcMaxX;
          }
        } else if (r instanceof BoundableRenderable) {
          final BoundableRenderable br = (BoundableRenderable) r;
          double brMaxX = br.getVisualBounds().getMaxX();
          if (brMaxX > maxX) {
            maxX = brMaxX;
          }
        } else if (r instanceof PositionedRenderable) {
          final PositionedRenderable rc = (PositionedRenderable) r;
          double rcMaxX = rc.renderable.getVisualBounds().getMaxX();
          if (rcMaxX > maxX) {
            maxX = rcMaxX;
          }
        } else {
          System.err.println("Unhandled renderable: " + r);
          Thread.dumpStack();
        }
      }
    }
    cachedVisualWidth = (int) maxX;
    return cachedVisualWidth;
  }

  @Override
  public Rectangle getClipBounds() {
    return ((RBlock)container).getClipBounds();
    // return new Rectangle(0, 0, width, height);
  }
}
