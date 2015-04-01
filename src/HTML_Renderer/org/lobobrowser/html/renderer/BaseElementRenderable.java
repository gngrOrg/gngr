/*  GNU LESSER GENERAL PUBLIC LICENSE
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.image.ImageObserver;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.Function;
import java.util.logging.Level;

import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.domimpl.HTMLElementImpl;
import org.lobobrowser.html.domimpl.ModelNode;
import org.lobobrowser.html.style.BackgroundInfo;
import org.lobobrowser.html.style.BorderInfo;
import org.lobobrowser.html.style.HtmlInsets;
import org.lobobrowser.html.style.HtmlValues;
import org.lobobrowser.html.style.JStyleProperties;
import org.lobobrowser.html.style.RenderState;
import org.lobobrowser.ua.NetworkRequest;
import org.lobobrowser.ua.UserAgentContext;
import org.lobobrowser.ua.UserAgentContext.Request;
import org.lobobrowser.ua.UserAgentContext.RequestKind;
import org.lobobrowser.util.SecurityUtil;
import org.lobobrowser.util.Strings;
import org.lobobrowser.util.gui.GUITasks;
import org.w3c.dom.css.CSS2Properties;

abstract class BaseElementRenderable extends BaseRCollection implements RElement, RenderableContainer, java.awt.image.ImageObserver {
  protected static final Integer INVALID_SIZE = new Integer(Integer.MIN_VALUE);

  /**
   * A collection of all GUI components added by descendents.
   */
  private Collection<Component> guiComponents = null;

  /**
   * A list of absolute positioned or float parent-child pairs.
   */
  protected Collection<DelayedPair> delayedPairs = null;

  // protected boolean renderStyleCanBeInvalidated = true;

  /**
   * Background color which may be different to that from RenderState in the
   * case of a Document node.
   */
  protected Color backgroundColor;
  protected volatile Image backgroundImage;
  protected int zIndex;
  protected Color borderTopColor;
  protected Color borderLeftColor;
  protected Color borderBottomColor;
  protected Color borderRightColor;
  protected Insets borderInsets;
  protected Insets marginInsets;
  protected Insets paddingInsets;
  protected BorderInfo borderInfo;
  protected java.net.URL lastBackgroundImageUri;
  protected Insets defaultMarginInsets = null;
  protected Insets defaultPaddingInsets = null;
  protected int overflowX;
  protected int overflowY;

  protected final UserAgentContext userAgentContext;

  public BaseElementRenderable(final RenderableContainer container, final ModelNode modelNode, final UserAgentContext ucontext) {
    super(container, modelNode);
    this.userAgentContext = ucontext;
  }

  public void setDefaultPaddingInsets(final Insets insets) {
    this.defaultPaddingInsets = insets;
  }

  public void setDefaultMarginInsets(final Insets insets) {
    this.defaultMarginInsets = insets;
  }

  public float getAlignmentX() {
    return 0.0f;
  }

  public float getAlignmentY() {
    return 0.0f;
  }

  protected boolean layoutDeepCanBeInvalidated = false;

  /**
   * Invalidates this Renderable and all descendents. This is only used in
   * special cases, such as when a new style sheet is added.
   */
  @Override
  public final void invalidateLayoutDeep() {
    if (this.layoutDeepCanBeInvalidated) {
      this.layoutDeepCanBeInvalidated = false;
      this.invalidateLayoutLocal();
      final Iterator<? extends Renderable> i = this.getRenderables();
      if (i != null) {
        while (i.hasNext()) {
          final Object r = i.next();
          if (r instanceof RCollection) {
            ((RCollection) r).invalidateLayoutDeep();
          }
        }
      }
    }
  }

  @Override
  protected void invalidateLayoutLocal() {
    final RenderState rs = this.modelNode.getRenderState();
    if (rs != null) {
      rs.invalidate();
    }
    this.overflowX = RenderState.OVERFLOW_NONE;
    this.overflowY = RenderState.OVERFLOW_NONE;
    this.declaredWidth = INVALID_SIZE;
    this.declaredHeight = INVALID_SIZE;
    this.lastAvailHeightForDeclared = -1;
    this.lastAvailWidthForDeclared = -1;
  }

  private Integer declaredWidth = INVALID_SIZE;
  private Integer declaredHeight = INVALID_SIZE;
  private int lastAvailWidthForDeclared = -1;
  private int lastAvailHeightForDeclared = -1;

  protected Integer getDeclaredWidth(final RenderState renderState, final int actualAvailWidth) {
    Integer dw = this.declaredWidth;
    if ((dw == INVALID_SIZE) || (actualAvailWidth != this.lastAvailWidthForDeclared)) {
      this.lastAvailWidthForDeclared = actualAvailWidth;
      final int dwInt = this.getDeclaredWidthImpl(renderState, actualAvailWidth);
      dw = dwInt == -1 ? null : new Integer(dwInt);
      this.declaredWidth = dw;
    }
    return dw;
  }

  protected Integer getDeclaredHelper(final RenderState renderState, final int baseValue,
      final Function<CSS2Properties, String> propertyGetter) {
    final Object rootNode = this.modelNode;
    if (rootNode instanceof HTMLElementImpl) {
      final HTMLElementImpl element = (HTMLElementImpl) rootNode;
      final CSS2Properties props = element.getCurrentStyle();
      if (props == null) {
        return null;
      }
      final String valueText = propertyGetter.apply(props);
      if ((valueText == null) || "".equals(valueText)) {
        return null;
      }
      return new Integer(HtmlValues.getPixelSize(valueText, renderState, -1, baseValue));
    } else {
      return null;
    }
  }

  protected Integer getDeclaredMaxWidth(final RenderState renderState, final int actualAvailWidth) {
    return getDeclaredHelper(renderState, actualAvailWidth, props -> props.getMaxWidth());
  }

  protected Integer getDeclaredMinWidth(final RenderState renderState, final int actualAvailWidth) {
    return getDeclaredHelper(renderState, actualAvailWidth, props -> props.getMinWidth());
  }

  protected Integer getDeclaredMaxHeight(final RenderState renderState, final int actualAvailHeight) {
    return getDeclaredHelper(renderState, actualAvailHeight, props -> props.getMaxHeight());
  }

  protected Integer getDeclaredMinHeight(final RenderState renderState, final int actualAvailHeight) {
    return getDeclaredHelper(renderState, actualAvailHeight, props -> props.getMinHeight());
  }

  public final boolean hasDeclaredWidth() {
    final Integer dw = this.declaredWidth;
    if (dw == INVALID_SIZE) {
      final Object rootNode = this.modelNode;
      if (rootNode instanceof HTMLElementImpl) {
        final HTMLElementImpl element = (HTMLElementImpl) rootNode;
        final CSS2Properties props = element.getCurrentStyle();
        if (props == null) {
          return false;
        }
        return !Strings.isBlank(props.getWidth()) || !Strings.isBlank(props.getMaxWidth());
      }
      return false;
    }
    return dw != null;
  }

  private final int getDeclaredWidthImpl(final RenderState renderState, final int availWidth) {
    final Object rootNode = this.modelNode;
    if (rootNode instanceof HTMLElementImpl) {
      final HTMLElementImpl element = (HTMLElementImpl) rootNode;
      final CSS2Properties props = element.getCurrentStyle();
      if (props == null) {
        return -1;
      }
      final String widthText = props.getWidth();
      if ((widthText == null) || "".equals(widthText)) {
        return -1;
      }
      return HtmlValues.getPixelSize(widthText, renderState, -1, availWidth);
    } else {
      return -1;
    }
  }

  protected Integer getDeclaredHeight(final RenderState renderState, final int actualAvailHeight) {
    Integer dh = this.declaredHeight;
    if ((dh == INVALID_SIZE) || (actualAvailHeight != this.lastAvailHeightForDeclared)) {
      this.lastAvailHeightForDeclared = actualAvailHeight;
      final int dhInt = this.getDeclaredHeightImpl(renderState, actualAvailHeight);
      dh = dhInt == -1 ? null : new Integer(dhInt);
      this.declaredHeight = dh;
    }
    return dh;
  }

  protected int getDeclaredHeightImpl(final RenderState renderState, final int availHeight) {
    final Object rootNode = this.modelNode;
    if (rootNode instanceof HTMLElementImpl) {
      final HTMLElementImpl element = (HTMLElementImpl) rootNode;
      final CSS2Properties props = element.getCurrentStyle();
      if (props == null) {
        return -1;
      }
      final String heightText = props.getHeight();
      if ((heightText == null) || "".equals(heightText)) {
        return -1;
      }
      return HtmlValues.getPixelSize(heightText, renderState, -1, availHeight);
    } else {
      return -1;
    }
  }

  /**
   * All overriders should call super implementation.
   */
  public void paint(final Graphics g) {
  }

  /**
   * Lays out children, and deals with "valid" state. Override doLayout method
   * instead of this one.
   */
  public final void layout(final int availWidth, final int availHeight, final boolean sizeOnly) {
    // Must call doLayout regardless of validity state.
    try {
      this.doLayout(availWidth, availHeight, sizeOnly);
    } finally {
      this.layoutUpTreeCanBeInvalidated = true;
      this.layoutDeepCanBeInvalidated = true;
    }
  }

  /**
   * Lays out children, and deals with "valid" state. Override doLayout method
   * instead of this one.
   */
  public final void layout(final int availWidth, final int availHeight, final boolean expandWidth, final boolean sizeOnly) {
    // Must call doLayout regardless of validity state.
    try {
      this.doLayout(availWidth, availHeight, expandWidth, sizeOnly);
    } finally {
      this.layoutUpTreeCanBeInvalidated = true;
      this.layoutDeepCanBeInvalidated = true;
    }
  }

  protected abstract void doLayout(int availWidth, int availHeight, boolean sizeOnly);

  protected abstract void doLayout(int availWidth, int availHeight, boolean expand, boolean sizeOnly);

  protected final void sendGUIComponentsToParent() {
    // Ensures that parent has all the components
    // below this renderer node. (Parent expected to have removed them).
    final Collection<Component> gc = this.guiComponents;
    if (gc != null) {
      final RenderableContainer rc = this.container;
      for (final Component c : gc) {
        rc.addComponent(c);
      }
    }
  }

  protected final void clearGUIComponents() {
    final Collection<Component> gc = this.guiComponents;
    if (gc != null) {
      gc.clear();
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.renderer.RenderableContainer#add(java.awt.Component)
   */
  public Component addComponent(final Component component) {
    // TODO: This gets called too many times!

    // Expected to be called in GUI thread.
    // Adds only in local collection.
    // Does not remove from parent.
    // Sending components to parent is done by sendGUIComponentsToParent().
    Collection<Component> gc = this.guiComponents;
    if (gc == null) {
      gc = new HashSet<>(1);
      this.guiComponents = gc;
    }
    gc.add(component);
    return component;
  }

  public void updateAllWidgetBounds() {
    this.container.updateAllWidgetBounds();
  }

  /**
   * Updates widget bounds below this node only. Should not be called during
   * general rendering.
   */
  public void updateWidgetBounds() {
    final java.awt.Point guiPoint = this.getGUIPoint(0, 0);
    this.updateWidgetBounds(guiPoint.x, guiPoint.y);
  }

  public Rectangle getBoundsRelativeToBlock() {
    RCollection parent = this;
    int x = 0, y = 0;
    while (parent != null) {
      x += parent.getX();
      y += parent.getY();
      parent = parent.getParent();
      if (parent instanceof RElement) {
        break;
      }
    }
    return new Rectangle(x, y, this.getWidth(), this.getHeight());
  }

  protected void clearStyle(final boolean isRootBlock) {
    this.borderInfo = null;
    this.borderInsets = null;
    this.borderTopColor = null;
    this.borderLeftColor = null;
    this.borderBottomColor = null;
    this.borderRightColor = null;
    this.zIndex = 0;
    this.backgroundColor = null;
    this.backgroundImage = null;
    this.lastBackgroundImageUri = null;
    this.overflowX = RenderState.OVERFLOW_VISIBLE;
    this.overflowY = RenderState.OVERFLOW_VISIBLE;

    if (isRootBlock) {
      // The margin of the root block behaves like extra padding.
      final Insets insets1 = this.defaultMarginInsets;
      final Insets insets2 = this.defaultPaddingInsets;
      Insets finalInsets = insets1 == null ? null : new Insets(insets1.top, insets1.left, insets1.bottom, insets1.right);
      if (insets2 != null) {
        if (finalInsets == null) {
          finalInsets = new Insets(insets2.top, insets2.left, insets2.bottom, insets2.right);
        } else {
          finalInsets.top += insets2.top;
          finalInsets.bottom += insets2.bottom;
          finalInsets.left += insets2.left;
          finalInsets.right += insets2.right;
        }
      }
      this.marginInsets = null;
      this.paddingInsets = finalInsets;
    } else {
      this.marginInsets = this.defaultMarginInsets;
      this.paddingInsets = this.defaultPaddingInsets;
    }
  }

  protected void applyStyle(final int availWidth, final int availHeight) {
    // TODO: Can be optimized if there's no style?
    // TODO: There's part of this that doesn't depend on availWidth
    // and availHeight, so it doesn't need to be redone on
    // every resize.
    // Note: Overridden by tables and lists.
    final Object rootNode = this.modelNode;
    HTMLElementImpl rootElement;
    boolean isRootBlock;
    if (rootNode instanceof HTMLDocumentImpl) {
      isRootBlock = true;
      final HTMLDocumentImpl doc = (HTMLDocumentImpl) rootNode;
      // Need to get BODY tag, for bgcolor, etc.
      rootElement = (HTMLElementImpl) doc.getBody();
    } else {
      isRootBlock = false;
      rootElement = (HTMLElementImpl) rootNode;
    }
    if (rootElement == null) {
      this.clearStyle(isRootBlock);
      return;
    }
    final RenderState rs = rootElement.getRenderState();
    if (rs == null) {
      throw new IllegalStateException("Element without render state: " + rootElement + "; parent=" + rootElement.getParentNode());
    }
    final BackgroundInfo binfo = rs.getBackgroundInfo();
    this.backgroundColor = binfo == null ? null : binfo.backgroundColor;
    final java.net.URL backgroundImageUri = binfo == null ? null : binfo.backgroundImage;
    if (backgroundImageUri == null) {
      this.backgroundImage = null;
      this.lastBackgroundImageUri = null;
    } else if (!backgroundImageUri.equals(this.lastBackgroundImageUri)) {
      this.lastBackgroundImageUri = backgroundImageUri;
      this.loadBackgroundImage(backgroundImageUri);
    }
    final JStyleProperties props = rootElement.getCurrentStyle();
    if (props == null) {
      this.clearStyle(isRootBlock);
    } else {
      final BorderInfo borderInfo = rs.getBorderInfo();
      this.borderInfo = borderInfo;
      final HtmlInsets binsets = borderInfo == null ? null : borderInfo.insets;
      final HtmlInsets minsets = rs.getMarginInsets();
      final HtmlInsets pinsets = rs.getPaddingInsets();
      final Insets defaultMarginInsets = this.defaultMarginInsets;
      int dmleft = 0, dmright = 0, dmtop = 0, dmbottom = 0;
      if (defaultMarginInsets != null) {
        dmleft = defaultMarginInsets.left;
        dmright = defaultMarginInsets.right;
        dmtop = defaultMarginInsets.top;
        dmbottom = defaultMarginInsets.bottom;
      }
      final Insets defaultPaddingInsets = this.defaultPaddingInsets;
      int dpleft = 0, dpright = 0, dptop = 0, dpbottom = 0;
      if (defaultPaddingInsets != null) {
        dpleft = defaultPaddingInsets.left;
        dpright = defaultPaddingInsets.right;
        dptop = defaultPaddingInsets.top;
        dpbottom = defaultPaddingInsets.bottom;
      }
      Insets borderInsets = binsets == null ? null : binsets.getAWTInsets(0, 0, 0, 0, availWidth, availHeight, 0, 0);
      if (borderInsets == null) {
        borderInsets = RBlockViewport.ZERO_INSETS;
      }
      Insets paddingInsets = pinsets == null ? defaultPaddingInsets : pinsets.getAWTInsets(dptop, dpleft, dpbottom, dpright, availWidth,
          availHeight, 0, 0);
      if (paddingInsets == null) {
        paddingInsets = RBlockViewport.ZERO_INSETS;
      }
      Insets tentativeMarginInsets = minsets == null ? defaultMarginInsets : minsets.getAWTInsets(dmtop, dmleft, dmbottom, dmright,
          availWidth, availHeight, 0, 0);
      if (tentativeMarginInsets == null) {
        tentativeMarginInsets = RBlockViewport.ZERO_INSETS;
      }
      // TODO: Get rid of autoMarginX and autoMarginY. They will be always zero
      final int autoMarginX = 0, autoMarginY = 0;
      this.borderInsets = borderInsets;
      if (isRootBlock) {
        // In the root block, the margin behaves like an extra padding.
        Insets regularMarginInsets = ((autoMarginX == 0) && (autoMarginY == 0)) ? tentativeMarginInsets
            : (minsets == null ? defaultMarginInsets : minsets.getAWTInsets(dmtop, dmleft, dmbottom, dmright, availWidth, availHeight,
                autoMarginX, autoMarginY));
        if (regularMarginInsets == null) {
          regularMarginInsets = RBlockViewport.ZERO_INSETS;
        }
        this.marginInsets = null;
        this.paddingInsets = new Insets(paddingInsets.top + regularMarginInsets.top, paddingInsets.left + regularMarginInsets.left,
            paddingInsets.bottom + regularMarginInsets.bottom, paddingInsets.right + regularMarginInsets.right);
      } else {
        this.paddingInsets = paddingInsets;
        this.marginInsets = ((autoMarginX == 0) && (autoMarginY == 0)) ? tentativeMarginInsets : (minsets == null ? defaultMarginInsets
            : minsets.getAWTInsets(dmtop, dmleft, dmbottom, dmright, availWidth, availHeight, autoMarginX, autoMarginY));
      }
      if (borderInfo != null) {
        this.borderTopColor = borderInfo.topColor;
        this.borderLeftColor = borderInfo.leftColor;
        this.borderBottomColor = borderInfo.bottomColor;
        this.borderRightColor = borderInfo.rightColor;
      } else {
        this.borderTopColor = null;
        this.borderLeftColor = null;
        this.borderBottomColor = null;
        this.borderRightColor = null;
      }
      final String zIndex = props.getZIndex();
      if (zIndex != null) {
        try {
          this.zIndex = Integer.parseInt(zIndex);
        } catch (final NumberFormatException err) {
          logger.log(Level.WARNING, "Unable to parse z-index [" + zIndex + "] in element " + this.modelNode + ".", err);
          this.zIndex = 0;
        }
      } else {
        this.zIndex = 0;
      }
      this.overflowX = rs.getOverflowX();
      this.overflowY = rs.getOverflowY();
    }

    // Check if background image needs to be loaded
  }

  // TODO: Move to RBlock and make private
  protected Dimension applyAutoStyles(final int availWidth, final int availHeight) {
    final Object rootNode = this.modelNode;
    HTMLElementImpl rootElement;
    if (rootNode instanceof HTMLDocumentImpl) {
      final HTMLDocumentImpl doc = (HTMLDocumentImpl) rootNode;
      // Need to get BODY tag, for bgcolor, etc.
      rootElement = (HTMLElementImpl) doc.getBody();
    } else {
      rootElement = (HTMLElementImpl) rootNode;
    }
    if (rootElement == null) {
      return null;
    }
    final RenderState rs = rootElement.getRenderState();
    if (rs == null) {
      throw new IllegalStateException("Element without render state: " + rootElement + "; parent=" + rootElement.getParentNode());
    }
    final Dimension changes = new Dimension();
    final HtmlInsets minsets = rs.getMarginInsets();
    if (minsets != null) {
      if (availWidth > 1) {
        // TODO: Consider the case when only one is auto
        final int autoMarginX = availWidth / 2;
        if (minsets.leftType == HtmlInsets.TYPE_AUTO) {
          this.marginInsets.left = autoMarginX;
          changes.width += autoMarginX;
        }
        if (minsets.rightType == HtmlInsets.TYPE_AUTO) {
          this.marginInsets.right = autoMarginX;
          changes.width += autoMarginX;
        }
      }
      /* auto for margin-top and margin-bottom is supposed to compute to zero, except when parent is a flex box */
      /*
      if (availHeight > 1) {
        // TODO: Consider the case when only one is auto
        final int autoMarginY = availHeight / 2;
        if (minsets.topType == HtmlInsets.TYPE_AUTO) {
          this.marginInsets.top = autoMarginY;
          changes.height += autoMarginY;
        }
        if (minsets.bottomType == HtmlInsets.TYPE_AUTO) {
          this.marginInsets.bottom = autoMarginY;
          changes.height += autoMarginY;
        }
      }*/
    }
    return changes;
  }

  protected void loadBackgroundImage(final java.net.URL imageURL) {
    final UserAgentContext ctx = this.userAgentContext;
    if (ctx != null) {
      final NetworkRequest request = ctx.createHttpRequest();
      request.addNetworkRequestListener(event -> {
        final int readyState = request.getReadyState();
        if (readyState == NetworkRequest.STATE_COMPLETE) {
          final int status = request.getStatus();
          if ((status == 200) || (status == 0)) {
            final Image img = request.getResponseImage();
            if (img != null) {
              BaseElementRenderable.this.backgroundImage = img;
              // Cause observer to be called
          final int w = img.getWidth(BaseElementRenderable.this);
          final int h = img.getHeight(BaseElementRenderable.this);
          // Maybe image already done...
          if ((w != -1) && (h != -1)) {
            BaseElementRenderable.this.repaint();
          }
        }
      }
    }
  })  ;

      SecurityUtil.doPrivileged(() -> {
        // Code might have restrictions on accessing items from elsewhere.
          try {
            request.open("GET", imageURL);
            request.send(null, new Request(imageURL, RequestKind.Image));
          } catch (final java.io.IOException thrown) {
            logger.log(Level.WARNING, "loadBackgroundImage()", thrown);
          }
          return null;
        });
    }
  }

  @Override
  public int getZIndex() {
    return this.zIndex;
  }

  private Color getBorderTopColor() {
    final Color c = this.borderTopColor;
    return c == null ? Color.black : c;
  }

  private Color getBorderLeftColor() {
    final Color c = this.borderLeftColor;
    return c == null ? Color.black : c;
  }

  private Color getBorderBottomColor() {
    final Color c = this.borderBottomColor;
    return c == null ? Color.black : c;
  }

  private Color getBorderRightColor() {
    final Color c = this.borderRightColor;
    return c == null ? Color.black : c;
  }

  protected void prePaint(final java.awt.Graphics g) {
    final int startWidth = this.width;
    final int startHeight = this.height;
    int totalWidth = startWidth;
    int totalHeight = startHeight;
    int startX = 0;
    int startY = 0;
    final ModelNode node = this.modelNode;
    final RenderState rs = node.getRenderState();
    final Insets marginInsets = this.marginInsets;
    if (marginInsets != null) {
      totalWidth -= (marginInsets.left + marginInsets.right);
      totalHeight -= (marginInsets.top + marginInsets.bottom);
      startX += marginInsets.left;
      startY += marginInsets.top;
    }

    final Insets borderInsets = this.borderInsets;

    prePaintBackground(g, totalWidth, totalHeight, startX, startY, node, rs, borderInsets);

    prePaintBorder(g, totalWidth, totalHeight, startX, startY, borderInsets);
  }

  private void prePaintBackground(final java.awt.Graphics g, int totalWidth, int totalHeight, int startX, int startY, final ModelNode node,
      final RenderState rs, final Insets borderInsets) {
    // TODO: Check if we can use TexturePaint to draw repeated background images
    // See example: http://www.informit.com/articles/article.aspx?p=26349&seqNum=4

    // Using clientG (clipped below) beyond this point.
    final Graphics clientG = g.create(startX, startY, totalWidth, totalHeight);
    try {
      Rectangle bkgBounds = null;
      if (node != null) {
        final int btop = borderInsets == null ? 0 : borderInsets.top;
        final int bleft = borderInsets == null ? 0 : borderInsets.left;

        final Color bkg = this.backgroundColor;
        if ((bkg != null) && (bkg.getAlpha() > 0)) {
          clientG.setColor(bkg);
          clientG.fillRect(0, 0, totalWidth, totalHeight);
        }
        final BackgroundInfo binfo = rs == null ? null : rs.getBackgroundInfo();
        final Image image = this.backgroundImage;
        if (image != null) {
          bkgBounds = clientG.getClipBounds();

          final int w = image.getWidth(this);
          final int h = image.getHeight(this);
          if ((w != -1) && (h != -1)) {
            final int imageY = getImageY(totalHeight, binfo, h);
            final int imageX = getImageX(totalWidth, binfo, w);

            // TODO: optimise this. Although it works fine, it makes an extra `draw` call when imageX or imageY are negative
            final int baseX = (bleft % w) + ((bkgBounds.x / w) * w) - (w - imageX);
            final int baseY = (btop % h) + ((bkgBounds.y / h) * h) - (h - imageY);

            switch (binfo == null ? BackgroundInfo.BR_REPEAT : binfo.backgroundRepeat) {
            case BackgroundInfo.BR_NO_REPEAT: {
              clientG.drawImage(image, bleft + imageX, btop + imageY, w, h, this);
              break;
            }
            case BackgroundInfo.BR_REPEAT_X: {
              // Modulate starting x.
              final int topX = bkgBounds.x + bkgBounds.width;
              for (int x = baseX; x < topX; x += w) {
                clientG.drawImage(image, x, btop + imageY, w, h, this);
              }
              break;
            }
            case BackgroundInfo.BR_REPEAT_Y: {
              // Modulate starting y.
              final int topY = bkgBounds.y + bkgBounds.height;
              for (int y = baseY; y < topY; y += h) {
                clientG.drawImage(image, bleft + imageX, y, w, h, this);
              }
              break;
            }
            default: {
              // Modulate starting x and y.
              final int topX = bkgBounds.x + bkgBounds.width;
              final int topY = bkgBounds.y + bkgBounds.height;
              // Replacing this:
              for (int x = baseX; x < topX; x += w) {
                for (int y = baseY; y < topY; y += h) {
                  clientG.drawImage(image, x, y, w, h, this);
                }
              }
              break;
            }
            }
          }
        }
      }
    } finally {
      clientG.dispose();
    }
  }

  private void prePaintBorder(final java.awt.Graphics g, int totalWidth, int totalHeight, int startX, int startY, final Insets borderInsets) {
    if (borderInsets != null) {
      final int btop = borderInsets.top;
      final int bleft = borderInsets.left;
      final int bright = borderInsets.right;
      final int bbottom = borderInsets.bottom;

      final int newTotalWidth = totalWidth - (bleft + bright);
      final int newTotalHeight = totalHeight - (btop + bbottom);
      final int newStartX = startX + bleft;
      final int newStartY = startY + btop;
      final Rectangle clientRegion = new Rectangle(newStartX, newStartY, newTotalWidth, newTotalHeight);

      // Paint borders if the clip bounds are not contained
      // by the content area.
      final Rectangle clipBounds = g.getClipBounds();
      if (!clientRegion.contains(clipBounds)) {
        final BorderInfo borderInfo = this.borderInfo;
       
        if (btop > 0) {
          g.setColor(this.getBorderTopColor());
          final Color borderTopDarkColor = g.getColor().darker();
          final Color borderTopLightColor = g.getColor().brighter();
          final int borderStyle = borderInfo == null ? HtmlValues.BORDER_STYLE_SOLID : borderInfo.topStyle;

          if (borderStyle == HtmlValues.BORDER_STYLE_DOTTED) {
            final int topMid = startY + (btop / 2);
            GUITasks.drawDotted(g, startX, topMid, startX + totalWidth, topMid, btop);
          } else {
            int btopBy3 = btop / 3;
            int btopBy2 = btop / 2;
            if (borderStyle == HtmlValues.BORDER_STYLE_INSET) {
              g.setColor(borderTopDarkColor);
            } else if (borderStyle == HtmlValues.BORDER_STYLE_OUTSET) {
              g.setColor(borderTopLightColor);
            } else if (borderStyle == HtmlValues.BORDER_STYLE_GROOVE) {
              g.setColor(borderTopDarkColor);
            } else if (borderStyle == HtmlValues.BORDER_STYLE_RIDGE) {
              g.setColor(borderTopLightColor);
            }
            for (int i = 0; i < btop; i++) {
              final int leftOffset = (i * bleft) / btop;
              final int rightOffset = (i * bright) / btop;
              if (borderStyle == HtmlValues.BORDER_STYLE_DASHED) {
                GUITasks.drawDashed(g, startX + leftOffset, startY + i, (startX + totalWidth) - rightOffset - 1, startY + i, 10 + btop, 6);
              } else {
                if (borderStyle == HtmlValues.BORDER_STYLE_GROOVE) {
                  if (i == btopBy2) {
                    g.setColor(borderTopLightColor);
                  }
                } else if (borderStyle == HtmlValues.BORDER_STYLE_RIDGE) {
                  if (i == btopBy2) {
                    g.setColor(borderTopDarkColor);
                  }
                }
                g.drawLine(startX + leftOffset, startY + i, (startX + totalWidth) - rightOffset - 1, startY + i);
                if (borderStyle == HtmlValues.BORDER_STYLE_DOUBLE) {
                  if (i == btopBy3) {
                    i += btopBy3;
                  }
                }
              }
            }
          }
        }
        if (bright > 0) {
          final int borderStyle = borderInfo == null ? HtmlValues.BORDER_STYLE_SOLID : borderInfo.rightStyle;
          g.setColor(this.getBorderRightColor());
          final Color borderRightLightColor = g.getColor().brighter();
          final Color borderRightDarkColor = g.getColor().darker();
          final int lastX = (startX + totalWidth) - 1;

          if (borderStyle == HtmlValues.BORDER_STYLE_DOTTED) {
            final int rightMid = lastX - (bright / 2);
            GUITasks.drawDotted(g, rightMid, startY, rightMid, startY + totalHeight, bright);
          } else {
            int brightBy3 = bright / 3;
            int brightBy2 = bright / 2;
            if (borderStyle == HtmlValues.BORDER_STYLE_INSET) {
              g.setColor(borderRightLightColor);
            } else if (borderStyle == HtmlValues.BORDER_STYLE_OUTSET) {
              g.setColor(borderRightDarkColor);
            } else if (borderStyle == HtmlValues.BORDER_STYLE_GROOVE) {
              g.setColor(borderRightLightColor);
            } else if (borderStyle == HtmlValues.BORDER_STYLE_RIDGE) {
              g.setColor(borderRightDarkColor);
            }
            for (int i = 0; i < bright; i++) {
              final int topOffset = (i * btop) / bright;
              final int bottomOffset = (i * bbottom) / bright;
              if (borderStyle == HtmlValues.BORDER_STYLE_DASHED) {
                GUITasks.drawDashed(g, lastX - i, startY + topOffset, lastX - i, (startY + totalHeight) - bottomOffset - 1, 10 + bright, 6);
              } else {
                if (borderStyle == HtmlValues.BORDER_STYLE_GROOVE) {
                  if (i == brightBy2) {
                    g.setColor(borderRightDarkColor);
                  }
                } else if (borderStyle == HtmlValues.BORDER_STYLE_RIDGE) {
                  if (i == brightBy2) {
                    g.setColor(borderRightLightColor);
                  }
                }
                g.drawLine(lastX - i, startY + topOffset, lastX - i, (startY + totalHeight) - bottomOffset - 1);
                if (borderStyle == HtmlValues.BORDER_STYLE_DOUBLE) {
                  if (i == brightBy3) {
                    i += brightBy3;
                  }
                }
              }
            }
          }
        }
        if (bbottom > 0) {
          final int borderStyle = borderInfo == null ? HtmlValues.BORDER_STYLE_SOLID : borderInfo.bottomStyle;
          g.setColor(this.getBorderBottomColor());
          final Color borderBottomDarkColor = g.getColor().darker();
          final Color borderBottomLightColor = g.getColor().brighter();

          final int lastY = (startY + totalHeight) - 1;

          if (borderStyle == HtmlValues.BORDER_STYLE_DOTTED) {
            final int bottomMid = lastY - (bbottom / 2);
            GUITasks.drawDotted(g, startX, bottomMid, startX + totalWidth, bottomMid, bbottom);
          } else {
            int bbottomBy3 = bbottom / 3;
            int bbottomBy2 = bbottom / 2;
            if (borderStyle == HtmlValues.BORDER_STYLE_INSET) {
              g.setColor(borderBottomLightColor);
            } else if (borderStyle == HtmlValues.BORDER_STYLE_OUTSET) {
              g.setColor(borderBottomDarkColor);
            } else if (borderStyle == HtmlValues.BORDER_STYLE_GROOVE) {
              g.setColor(borderBottomLightColor);
            } else if (borderStyle == HtmlValues.BORDER_STYLE_RIDGE) {
              g.setColor(borderBottomDarkColor);
            }
            for (int i = 0; i < bbottom; i++) {
              final int leftOffset = (i * bleft) / bbottom;
              final int rightOffset = (i * bright) / bbottom;
              if (borderStyle == HtmlValues.BORDER_STYLE_DASHED) {
                GUITasks.drawDashed(g, startX + leftOffset, lastY - i, (startX + totalWidth) - rightOffset - 1, lastY - i, 10 + bbottom, 6);
              } else {
                if (borderStyle == HtmlValues.BORDER_STYLE_GROOVE) {
                  if (i == bbottomBy2) {
                    g.setColor(borderBottomDarkColor);
                  }
                } else if (borderStyle == HtmlValues.BORDER_STYLE_RIDGE) {
                  if (i == bbottomBy2) {
                    g.setColor(borderBottomLightColor);
                  }
                }
                g.drawLine(startX + leftOffset, lastY - i, (startX + totalWidth) - rightOffset - 1, lastY - i);
                if (borderStyle == HtmlValues.BORDER_STYLE_DOUBLE) {
                  if (i == bbottomBy3) {
                    i += bbottomBy3;
                  }
                }
              }
            }
          }
        }
        if (bleft > 0) {
          final int borderStyle = borderInfo == null ? HtmlValues.BORDER_STYLE_SOLID : borderInfo.leftStyle;
          g.setColor(this.getBorderLeftColor());
          final Color borderLeftDarkColor = g.getColor().darker();
          final Color borderLeftLightColor = g.getColor().brighter();

          if (borderStyle == HtmlValues.BORDER_STYLE_DOTTED) {
            final int leftMid = startX + (bleft / 2);
            GUITasks.drawDotted(g, leftMid, startY, leftMid, startY + totalHeight, bleft);
          } else {
            int bleftBy3 = bleft / 3;
            int bleftBy2 = bleft / 2;
            if (borderStyle == HtmlValues.BORDER_STYLE_INSET) {
              g.setColor(borderLeftDarkColor);
            } else if (borderStyle == HtmlValues.BORDER_STYLE_OUTSET) {
              g.setColor(borderLeftLightColor);
            } else if (borderStyle == HtmlValues.BORDER_STYLE_GROOVE) {
              g.setColor(borderLeftDarkColor);
            } else if (borderStyle == HtmlValues.BORDER_STYLE_RIDGE) {
              g.setColor(borderLeftLightColor);
            }
            for (int i = 0; i < bleft; i++) {
              final int topOffset = (i * btop) / bleft;
              final int bottomOffset = (i * bbottom) / bleft;
              if (borderStyle == HtmlValues.BORDER_STYLE_DASHED) {
                GUITasks
                    .drawDashed(g, startX + i, startY + topOffset, startX + i, (startY + totalHeight) - bottomOffset - 1, 10 + bleft, 6);
              } else {
                if (borderStyle == HtmlValues.BORDER_STYLE_GROOVE) {
                  if (i == bleftBy2) {
                    g.setColor(borderLeftLightColor);
                  }
                } else if (borderStyle == HtmlValues.BORDER_STYLE_RIDGE) {
                  if (i == bleftBy2) {
                    g.setColor(borderLeftDarkColor);
                  }
                }
                g.drawLine(startX + i, startY + topOffset, startX + i, (startY + totalHeight) - bottomOffset - 1);
                if (borderStyle == HtmlValues.BORDER_STYLE_DOUBLE) {
                  if (i == bleftBy3) {
                    i += bleftBy3;
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private static int getImageY(final int totalHeight, final BackgroundInfo binfo, final int h) {
    if (binfo == null) {
      return 0;
    } else {
      if (binfo.backgroundYPositionAbsolute) {
        return binfo.backgroundYPosition;
      } else {
        return (binfo.backgroundYPosition * (totalHeight - h)) / 100;
      }
    }
  }

  private static int getImageX(final int totalWidth, final BackgroundInfo binfo, final int w) {
    if (binfo == null) {
      return 0;
    } else {
      if (binfo.backgroundXPositionAbsolute) {
        return binfo.backgroundXPosition;
      } else {
        return (binfo.backgroundXPosition * (totalWidth - w)) / 100;
      }
    }
  }

  public boolean imageUpdate(final Image img, final int infoflags, final int x, final int y, final int w, final int h) {
    // This is so that a loading image doesn't cause
    // too many repaint events.
    if (((infoflags & ImageObserver.ALLBITS) != 0) || ((infoflags & ImageObserver.FRAMEBITS) != 0)) {
      this.repaint();
    }
    return true;
  }

  protected static final int SCROLL_BAR_THICKNESS = 16;

  public Insets getBorderInsets() {
    return this.borderInsets == null ? RBlockViewport.ZERO_INSETS : this.borderInsets;
  }

  /**
   * Gets insets of content area. It includes margin, borders, padding and
   * scrollbars.
   */
  public Insets getInsets(final boolean hscroll, final boolean vscroll) {
    return getInsets(hscroll, vscroll, true, true, true);
  }

  /**
   * Gets insets of content area. It includes margin, borders, and scrollbars
   * but excludes padding.
   */
  public Insets getInsetsMarginBorder(final boolean hscroll, final boolean vscroll) {
    return getInsets(hscroll, vscroll, true, true, false);
  }

  // TODO: This method maybe inlined for performance
  private Insets getInsets(final boolean hscroll, final boolean vscroll,
      final boolean includeMI, final boolean includeBI, final boolean includePI) {
    final Insets mi = this.marginInsets;
    final Insets bi = this.borderInsets;
    final Insets pi = this.paddingInsets;
    int top = 0;
    int bottom = 0;
    int left = 0;
    int right = 0;
    if (includeMI && (mi != null)) {
      top += mi.top;
      left += mi.left;
      bottom += mi.bottom;
      right += mi.right;
    }
    if (includeBI && (bi != null)) {
      top += bi.top;
      left += bi.left;
      bottom += bi.bottom;
      right += bi.right;
    }
    if (includePI && (pi != null)) {
      top += pi.top;
      left += pi.left;
      bottom += pi.bottom;
      right += pi.right;
    }
    if (hscroll) {
      bottom += SCROLL_BAR_THICKNESS;
    }
    if (vscroll) {
      right += SCROLL_BAR_THICKNESS;
    }
    return new Insets(top, left, bottom, right);
  }

  protected final void sendDelayedPairsToParent() {
    // Ensures that parent has all the components
    // below this renderer node. (Parent expected to have removed them).
    final Collection<DelayedPair> gc = this.delayedPairs;
    if (gc != null) {
      final RenderableContainer rc = this.container;
      final Iterator<DelayedPair> i = gc.iterator();
      while (i.hasNext()) {
        final DelayedPair pair = i.next();
        if (pair.containingBlock != this) {
          rc.addDelayedPair(pair);
        }
      }
    }
  }

  public final void clearDelayedPairs() {
    final Collection<DelayedPair> gc = this.delayedPairs;
    if (gc != null) {
      gc.clear();
    }
  }

  public final Collection<DelayedPair> getDelayedPairs() {
    return this.delayedPairs;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.renderer.RenderableContainer#add(java.awt.Component)
   */
  public void addDelayedPair(final DelayedPair pair) {
    // Expected to be called in GUI thread.
    // Adds only in local collection.
    // Does not remove from parent.
    // Sending components to parent is done
    // by sendDelayedPairsToParent().
    Collection<DelayedPair> gc = this.delayedPairs;
    if (gc == null) {
      // Sequence is important.
      // TODO: But possibly added multiple
      // times in table layout?
      gc = new java.util.LinkedList<>();
      this.delayedPairs = gc;
    }
    gc.add(pair);
  }

  public RenderableContainer getParentContainer() {
    return this.container;
  }

  public boolean isContainedByNode() {
    return true;
  }

  public int getCollapsibleMarginBottom() {
    int cm;
    final Insets paddingInsets = this.paddingInsets;
    if ((paddingInsets != null) && (paddingInsets.bottom > 0)) {
      cm = 0;
    } else {
      final Insets borderInsets = this.borderInsets;
      if ((borderInsets != null) && (borderInsets.bottom > 0)) {
        cm = 0;
      } else {
        cm = this.getMarginBottom();
      }
    }
    if (this.isMarginBoundary()) {
      final RenderState rs = this.modelNode.getRenderState();
      if (rs != null) {
        final FontMetrics fm = rs.getFontMetrics();
        final int fontHeight = fm.getHeight();
        if (fontHeight > cm) {
          cm = fontHeight;
        }
      }
    }
    return cm;
  }

  protected boolean isMarginBoundary() {
    return ((this.overflowY != RenderState.OVERFLOW_VISIBLE) && (this.overflowX != RenderState.OVERFLOW_NONE))
        || (this.modelNode instanceof HTMLDocumentImpl);
  }

  public int getCollapsibleMarginTop() {
    int cm;
    final Insets paddingInsets = this.paddingInsets;
    if ((paddingInsets != null) && (paddingInsets.top > 0)) {
      cm = 0;
    } else {
      final Insets borderInsets = this.borderInsets;
      if ((borderInsets != null) && (borderInsets.top > 0)) {
        cm = 0;
      } else {
        cm = this.getMarginTop();
      }
    }
    if (this.isMarginBoundary()) {
      final RenderState rs = this.modelNode.getRenderState();
      if (rs != null) {
        final FontMetrics fm = rs.getFontMetrics();
        final int fontHeight = fm.getHeight();
        if (fontHeight > cm) {
          cm = fontHeight;
        }
      }
    }
    return cm;
  }

  public int getMarginBottom() {
    final Insets marginInsets = this.marginInsets;
    return marginInsets == null ? 0 : marginInsets.bottom;
  }

  public int getMarginLeft() {
    final Insets marginInsets = this.marginInsets;
    return marginInsets == null ? 0 : marginInsets.left;
  }

  public int getMarginRight() {
    final Insets marginInsets = this.marginInsets;
    return marginInsets == null ? 0 : marginInsets.right;
  }

  public int getMarginTop() {
    final Insets marginInsets = this.marginInsets;
    return marginInsets == null ? 0 : marginInsets.top;
  }

}