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
 * Created on Nov 19, 2005
 */
package org.lobobrowser.html.gui;

import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.concurrent.Future;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.domimpl.DocumentNotificationListener;
import org.lobobrowser.html.domimpl.ElementImpl;
import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.domimpl.NodeImpl;
import org.lobobrowser.html.parser.DocumentBuilderImpl;
import org.lobobrowser.html.parser.InputSourceImpl;
import org.lobobrowser.html.renderer.BoundableRenderable;
import org.lobobrowser.html.renderer.FrameContext;
import org.lobobrowser.html.renderer.NodeRenderer;
import org.lobobrowser.html.renderer.RenderableSpot;
import org.lobobrowser.html.style.RenderState;
import org.lobobrowser.main.DefferedLayoutSupport;
import org.lobobrowser.ua.UserAgentContext;
import org.lobobrowser.util.EventDispatch2;
import org.lobobrowser.util.gui.WrapperLayout;
import org.w3c.dom.Document;
import org.w3c.dom.Text;
import org.w3c.dom.html.HTMLFrameSetElement;

/**
 * The <code>HtmlPanel</code> class is a Swing component that can render a HTML
 * DOM. It uses either {@link HtmlBlockPanel} or {@link FrameSetPanel}
 * internally, depending on whether the document is determined to be a FRAMESET
 * or not.
 * <p>
 * Invoke method {@link #setDocument(Document, HtmlRendererContext)} in order to
 * schedule a document for rendering.
 */
public class HtmlPanel extends JComponent implements FrameContext, DefferedLayoutSupport {
  private static final long serialVersionUID = -8342517547909796721L;
  private final EventDispatch2 selectionDispatch = new SelectionDispatch();
  private final javax.swing.Timer notificationTimer;
  private final DocumentNotificationListener notificationListener;
  private final Runnable notificationImmediateAction;
  private static final int NOTIF_TIMER_DELAY = 150;

  private volatile boolean isFrameSet = false;
  private volatile NodeRenderer nodeRenderer = null;
  private volatile NodeImpl rootNode;
  private volatile int preferredWidth = -1;
  private volatile int defaultOverflowX = RenderState.OVERFLOW_AUTO;
  private volatile int defaultOverflowY = RenderState.OVERFLOW_AUTO;

  protected volatile HtmlBlockPanel htmlBlockPanel;
  protected volatile FrameSetPanel frameSetPanel;

  /**
   * Constructs an <code>HtmlPanel</code>.
   */
  public HtmlPanel() {
    super();
    this.setLayout(WrapperLayout.getInstance());
    this.setOpaque(false);
    this.notificationTimer = new javax.swing.Timer(NOTIF_TIMER_DELAY, new NotificationTimerAction());
    this.notificationTimer.setRepeats(false);
    this.notificationListener = new LocalDocumentNotificationListener();
    this.notificationImmediateAction = new Runnable() {
      public void run() {
        processNotifications();
      }
    };
  }

  /**
   * Sets a preferred width that serves as a hint in calculating the preferred
   * size of the <code>HtmlPanel</code>. Note that the preferred size can only
   * be calculated when a document is available, and it will vary during
   * incremental rendering.
   * <p>
   * This method currently does not have any effect when the document is a
   * FRAMESET.
   * <p>
   * Note also that setting the preferred width (to a value other than
   * <code>-1</code>) will negatively impact performance.
   *
   * @param width
   *          The preferred width, or <code>-1</code> to unset.
   */
  public void setPreferredWidth(final int width) {
    this.preferredWidth = width;
    final HtmlBlockPanel htmlBlock = this.htmlBlockPanel;
    if (htmlBlock != null) {
      htmlBlock.setPreferredWidth(width);
    }
  }

  /**
   * If the current document is not a FRAMESET, this method scrolls the body
   * area to the given location.
   * <p>
   * This method should be called from the GUI thread.
   *
   * @param bounds
   *          The bounds in the scrollable block area that should become
   *          visible.
   * @param xIfNeeded
   *          If this parameter is true, scrolling will only occur if the
   *          requested bounds are not currently visible horizontally.
   * @param yIfNeeded
   *          If this parameter is true, scrolling will only occur if the
   *          requested bounds are not currently visible vertically.
   */
  public void scrollTo(final Rectangle bounds, final boolean xIfNeeded, final boolean yIfNeeded) {
    final HtmlBlockPanel htmlBlock = this.htmlBlockPanel;
    if (htmlBlock != null) {
      htmlBlock.scrollTo(bounds, xIfNeeded, yIfNeeded);
    }
  }

  /**
   * Scrolls the body area to the node given, if it is part of the current
   * document.
   * <p>
   * This method should be called from the GUI thread.
   *
   * @param node
   *          A DOM node.
   */
  public void scrollTo(final org.w3c.dom.Node node) {
    final HtmlBlockPanel htmlBlock = this.htmlBlockPanel;
    if (htmlBlock != null) {
      htmlBlock.scrollTo(node);
    }
  }

  /**
   * Gets the root <code>Renderable</code> of the HTML block. It returns
   * <code>null</code> for FRAMESETs.
   */
  public BoundableRenderable getBlockRenderable() {
    final HtmlBlockPanel htmlBlock = this.htmlBlockPanel;
    return htmlBlock == null ? null : htmlBlock.getRootRenderable();
  }

  /**
   * Gets an instance of {@link FrameSetPanel} in case the currently rendered
   * page is a FRAMESET.
   * <p>
   * Note: This method should be invoked in the GUI thread.
   *
   * @return A <code>FrameSetPanel</code> instance or <code>null</code> if the
   *         document currently rendered is not a FRAMESET.
   */
  public FrameSetPanel getFrameSetPanel() {
    final int componentCount = this.getComponentCount();
    if (componentCount == 0) {
      return null;
    }
    final Object c = this.getComponent(0);
    if (c instanceof FrameSetPanel) {
      return (FrameSetPanel) c;
    }
    return null;
  }

  private void setUpAsBlock(final UserAgentContext ucontext, final HtmlRendererContext rcontext) {
    final HtmlBlockPanel shp = this.createHtmlBlockPanel(ucontext, rcontext);
    shp.setPreferredWidth(this.preferredWidth);
    shp.setDefaultOverflowX(this.defaultOverflowX);
    shp.setDefaultOverflowY(this.defaultOverflowY);
    this.htmlBlockPanel = shp;
    this.frameSetPanel = null;
    this.removeAll();
    this.add(shp);
    this.nodeRenderer = shp;
  }

  private void setUpFrameSet(final NodeImpl fsrn) {
    this.isFrameSet = true;
    this.htmlBlockPanel = null;
    final FrameSetPanel fsp = this.createFrameSetPanel();
    this.frameSetPanel = fsp;
    this.nodeRenderer = fsp;
    this.removeAll();
    this.add(fsp);
    fsp.setRootNode(fsrn);
  }

  /**
   * Method invoked internally to create a {@link HtmlBlockPanel}. It is made
   * available so it can be overridden.
   */
  protected HtmlBlockPanel createHtmlBlockPanel(final UserAgentContext ucontext, final HtmlRendererContext rcontext) {
    return new HtmlBlockPanel(java.awt.Color.WHITE, true, ucontext, rcontext, this);
  }

  /**
   * Method invoked internally to create a {@link FrameSetPanel}. It is made
   * available so it can be overridden.
   */
  protected FrameSetPanel createFrameSetPanel() {
    return new FrameSetPanel();
  }

  /**
   * Scrolls the document such that x and y coordinates are placed in the
   * upper-left corner of the panel.
   * <p>
   * This method may be called outside of the GUI Thread.
   *
   * @param x
   *          The x coordinate.
   * @param y
   *          The y coordinate.
   */
  public void scroll(final int x, final int y) {
    if (SwingUtilities.isEventDispatchThread()) {
      this.scrollImpl(x, y);
    } else {
      SwingUtilities.invokeLater(() -> scrollImpl(x, y));
    }
  }

  public void scrollBy(final int x, final int y) {
    if (SwingUtilities.isEventDispatchThread()) {
      this.scrollByImpl(x, y);
    } else {
      SwingUtilities.invokeLater(() -> scrollByImpl(x, y));
    }
  }

  private void scrollImpl(final int x, final int y) {
    this.scrollTo(new Rectangle(x, y, 16, 16), false, false);
  }

  private void scrollByImpl(final int xOffset, final int yOffset) {
    final HtmlBlockPanel bp = this.htmlBlockPanel;
    if (bp != null) {
      bp.scrollBy(xOffset, yOffset);
    }
  }

  /**
   * Clears the current document if any. If called outside the GUI thread, the
   * operation will be scheduled to be performed in the GUI thread.
   */
  public void clearDocument() {
    if (SwingUtilities.isEventDispatchThread()) {
      this.clearDocumentImpl();
    } else {
      SwingUtilities.invokeLater(() -> HtmlPanel.this.clearDocumentImpl());
    }
  }

  private void clearDocumentImpl() {
    final HTMLDocumentImpl prevDocument = (HTMLDocumentImpl) this.rootNode;
    if (prevDocument != null) {
      prevDocument.removeDocumentNotificationListener(this.notificationListener);
    }
    final NodeRenderer nr = this.nodeRenderer;
    if (nr != null) {
      nr.setRootNode(null);
    }
    this.rootNode = null;
    this.htmlBlockPanel = null;
    this.nodeRenderer = null;
    this.isFrameSet = false;
    this.removeAll();
    this.revalidate();
    this.repaint();
  }

  /**
   * Sets an HTML DOM node and invalidates the component so it is rendered as
   * soon as possible in the GUI thread.
   * <p>
   * If this method is called from a thread that is not the GUI dispatch thread,
   * the document is scheduled to be set later. Note that
   * {@link #setPreferredWidth(int) preferred size} calculations should be done
   * in the GUI dispatch thread for this reason.
   *
   * @param node
   *          This should normally be a Document instance obtained with
   *          {@link org.lobobrowser.html.parser.DocumentBuilderImpl}.
   *          <p>
   * @param rcontext
   *          A renderer context.
   * @see org.lobobrowser.html.parser.DocumentBuilderImpl#parse(org.xml.sax.InputSource)
   * @see org.lobobrowser.html.test.SimpleHtmlRendererContext
   */
  public void setDocument(final Document node, final HtmlRendererContext rcontext) {
    setCursor(Cursor.getDefaultCursor());

    if (SwingUtilities.isEventDispatchThread()) {
      this.setDocumentImpl(node, rcontext);
    } else {
      SwingUtilities.invokeLater(() -> HtmlPanel.this.setDocumentImpl(node, rcontext));
    }
  }

  @Override
  public void setCursor(final Cursor cursor) {
    if (cursor != getCursor()) {
      super.setCursor(cursor);
    }
  }

  /**
   * Scrolls to the element identified by the given ID in the current document.
   * <p>
   * If this method is invoked outside the GUI thread, the operation is
   * scheduled to be performed as soon as possible in the GUI thread.
   *
   * @param nameOrId
   *          The name or ID of the element in the document.
   */
  public void scrollToElement(final String nameOrId) {
    if (SwingUtilities.isEventDispatchThread()) {
      this.scrollToElementImpl(nameOrId);
    } else {
      SwingUtilities.invokeLater(() -> scrollToElementImpl(nameOrId));
    }
  }

  private void scrollToElementImpl(final String nameOrId) {
    final NodeImpl node = this.rootNode;
    if (node instanceof HTMLDocumentImpl) {
      final HTMLDocumentImpl doc = (HTMLDocumentImpl) node;
      final org.w3c.dom.Element element = doc.getElementById(nameOrId);
      if (element != null) {
        this.scrollTo(element);
      }
    }
  }

  private void setDocumentImpl(final Document node, final HtmlRendererContext rcontext) {
    // Expected to be called in the GUI thread.
    /*
    if (!(node instanceof HTMLDocumentImpl)) {
      throw new IllegalArgumentException("Only nodes of type HTMLDocumentImpl are currently supported. Use DocumentBuilderImpl.");
    }
    */

    if (this.rootNode instanceof HTMLDocumentImpl) {
      final HTMLDocumentImpl prevDocument = (HTMLDocumentImpl) this.rootNode;
      prevDocument.removeDocumentNotificationListener(this.notificationListener);
    }
    if (node instanceof HTMLDocumentImpl) {
    final HTMLDocumentImpl nodeImpl = (HTMLDocumentImpl) node;
    nodeImpl.addDocumentNotificationListener(this.notificationListener);
    }

    if (node instanceof NodeImpl) {
    final NodeImpl nodeImpl = (NodeImpl) node;
    this.rootNode = nodeImpl;
    final NodeImpl fsrn = this.getFrameSetRootNode(nodeImpl);
    final boolean newIfs = fsrn != null;
    if ((newIfs != this.isFrameSet) || (this.getComponentCount() == 0)) {
      this.isFrameSet = newIfs;
      if (newIfs) {
        this.setUpFrameSet(fsrn);
      } else {
        this.setUpAsBlock(rcontext.getUserAgentContext(), rcontext);
      }
    }
    final NodeRenderer nr = this.nodeRenderer;
    if (nr != null) {
      // These subcomponents should take care
      // of revalidation.
      if (newIfs) {
        nr.setRootNode(fsrn);
      } else {
        nr.setRootNode(nodeImpl);
      }
    } else {
      this.invalidate();
      this.validate();
      this.repaint();
    }
    }
  }

  /**
   * Renders HTML given as a string.
   *
   * @param htmlSource
   *          The HTML source code.
   * @param uri
   *          A base URI used to resolve item URIs.
   * @param rcontext
   *          The {@link HtmlRendererContext} instance.
   * @see org.lobobrowser.html.test.SimpleHtmlRendererContext
   * @see #setDocument(Document, HtmlRendererContext)
   */
  public void setHtml(final String htmlSource, final String uri, final HtmlRendererContext rcontext) {
    try {
      final DocumentBuilderImpl builder = new DocumentBuilderImpl(rcontext.getUserAgentContext(), rcontext);
      try (
        final Reader reader = new StringReader(htmlSource)) {
        final InputSourceImpl is = new InputSourceImpl(reader, uri);
        final Document document = builder.parse(is);
        this.setDocument(document, rcontext);
      }
    } catch (final java.io.IOException ioe) {
      throw new IllegalStateException("Unexpected condition.", ioe);
    } catch (final org.xml.sax.SAXException se) {
      throw new IllegalStateException("Unexpected condition.", se);
    }
  }

  /**
   * Gets the HTML DOM node currently rendered if any.
   */
  public NodeImpl getRootNode() {
    return this.rootNode;
  }

  private boolean resetIfFrameSet() {
    final NodeImpl nodeImpl = this.rootNode;
    final NodeImpl fsrn = this.getFrameSetRootNode(nodeImpl);
    final boolean newIfs = fsrn != null;
    if ((newIfs != this.isFrameSet) || (this.getComponentCount() == 0)) {
      this.isFrameSet = newIfs;
      if (newIfs) {
        this.setUpFrameSet(fsrn);
        final NodeRenderer nr = this.nodeRenderer;
        nr.setRootNode(fsrn);
        // Set proper bounds and repaint.
        this.validate();
        this.repaint();
        return true;
      }
    }
    return false;
  }

  private NodeImpl getFrameSetRootNode(final NodeImpl node) {
    if (node instanceof Document) {
      final ElementImpl element = (ElementImpl) ((Document) node).getDocumentElement();
      if ((element != null) && "HTML".equalsIgnoreCase(element.getTagName())) {
        return this.getFrameSet(element);
      } else {
        return this.getFrameSet(node);
      }
    } else {
      return null;
    }
  }

  private NodeImpl getFrameSet(final NodeImpl node) {
    final NodeImpl[] children = node.getChildrenArray();
    if (children == null) {
      return null;
    }
    final int length = children.length;
    NodeImpl frameSet = null;
    for (int i = 0; i < length; i++) {
      final NodeImpl child = children[i];
      if (child instanceof Text) {
        // Ignore
      } else if (child instanceof ElementImpl) {
        final String tagName = child.getNodeName();
        if ("HEAD".equalsIgnoreCase(tagName) || "NOFRAMES".equalsIgnoreCase(tagName) || "TITLE".equalsIgnoreCase(tagName)
            || "META".equalsIgnoreCase(tagName) || "SCRIPT".equalsIgnoreCase(tagName) || "NOSCRIPT".equalsIgnoreCase(tagName)) {
          // ignore it
        } else if ("FRAMESET".equalsIgnoreCase(tagName)) {
          frameSet = child;
          break;
        } else {
          if (this.hasSomeHtml((ElementImpl) child)) {
            return null;
          }
        }
      }
    }
    return frameSet;
  }

  private boolean hasSomeHtml(final ElementImpl element) {
    final String tagName = element.getTagName();
    if ("HEAD".equalsIgnoreCase(tagName) || "TITLE".equalsIgnoreCase(tagName) || "META".equalsIgnoreCase(tagName)) {
      return false;
    }
    final NodeImpl[] children = element.getChildrenArray();
    if (children != null) {
      final int length = children.length;
      for (int i = 0; i < length; i++) {
        final NodeImpl child = children[i];
        if (child instanceof Text) {
          final String textContent = ((Text) child).getTextContent();
          if ((textContent != null) && !"".equals(textContent.trim())) {
            return false;
          }
        } else if (child instanceof ElementImpl) {
          if (this.hasSomeHtml((ElementImpl) child)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  /**
   * Internal method used to expand the selection to the given point.
   * <p>
   * Note: This method should be invoked in the GUI thread.
   */
  public void expandSelection(final RenderableSpot rpoint) {
    final HtmlBlockPanel block = this.htmlBlockPanel;
    if (block != null) {
      block.setSelectionEnd(rpoint);
      block.repaint();
      this.selectionDispatch.fireEvent(new SelectionChangeEvent(this, block.isSelectionAvailable()));
    }
  }

  /**
   * Internal method used to reset the selection so that it is empty at the
   * given point. This is what is called when the user clicks on a point in the
   * document.
   * <p>
   * Note: This method should be invoked in the GUI thread.
   */
  public void resetSelection(final RenderableSpot rpoint) {
    final HtmlBlockPanel block = this.htmlBlockPanel;
    if (block != null) {
      block.setSelectionStart(rpoint);
      block.setSelectionEnd(rpoint);
      block.repaint();
    }
    this.selectionDispatch.fireEvent(new SelectionChangeEvent(this, false));
  }

  /**
   * Gets the selection text.
   * <p>
   * Note: This method should be invoked in the GUI thread.
   */
  public String getSelectionText() {
    final HtmlBlockPanel block = this.htmlBlockPanel;
    if (block == null) {
      return null;
    } else {
      return block.getSelectionText();
    }
  }

  /**
   * Gets a DOM node enclosing the selection. The node returned should be the
   * inner-most node that encloses both selection start and end points. Note
   * that the selection end point may be just outside of the selection.
   * <p>
   * Note: This method should be invoked in the GUI thread.
   *
   * @return A node enclosing the current selection, or <code>null</code> if
   *         there is no such node. It also returns <code>null</code> for
   *         FRAMESETs.
   */
  public org.w3c.dom.Node getSelectionNode() {
    final HtmlBlockPanel block = this.htmlBlockPanel;
    if (block == null) {
      return null;
    } else {
      return block.getSelectionNode();
    }
  }

  /**
   * Returns true only if the current block has a selection. This method has no
   * effect in FRAMESETs at the moment.
   */
  public boolean hasSelection() {
    final HtmlBlockPanel block = this.htmlBlockPanel;
    if (block == null) {
      return false;
    } else {
      return block.hasSelection();
    }
  }

  /**
   * Copies the current selection, if any, into the clipboard. This method has
   * no effect in FRAMESETs at the moment.
   */
  public boolean copy() {
    final HtmlBlockPanel block = this.htmlBlockPanel;
    if (block != null) {
      return block.copy();
    } else {
      return false;
    }
  }

  /**
   * Adds listener of selection changes. Note that it does not have any effect
   * on FRAMESETs.
   *
   * @param listener
   *          An instance of {@link SelectionChangeListener}.
   */
  public void addSelectionChangeListener(final SelectionChangeListener listener) {
    this.selectionDispatch.addListener(listener);
  }

  /**
   * Removes a listener of selection changes that was previously added.
   */
  public void removeSelectionChangeListener(final SelectionChangeListener listener) {
    this.selectionDispatch.removeListener(listener);
  }

  /**
   * Sets the default horizontal overflow.
   * <p>
   * This method has no effect on FRAMESETs.
   *
   * @param overflow
   *          See {@link org.lobobrowser.html.style.RenderState}.
   */
  public void setDefaultOverflowX(final int overflow) {
    this.defaultOverflowX = overflow;
    final HtmlBlockPanel block = this.htmlBlockPanel;
    if (block != null) {
      block.setDefaultOverflowX(overflow);
    }
  }

  /**
   * Sets the default vertical overflow.
   * <p>
   * This method has no effect on FRAMESETs.
   *
   * @param overflow
   *          See {@link org.lobobrowser.html.style.RenderState}.
   */
  public void setDefaultOverflowY(final int overflow) {
    this.defaultOverflowY = overflow;
    final HtmlBlockPanel block = this.htmlBlockPanel;
    if (block != null) {
      block.setDefaultOverflowY(overflow);
    }
  }

  private final ArrayList<DocumentNotification> notifications = new ArrayList<>(1);

  private void addNotification(final DocumentNotification notification) {
    // This can be called in a random thread.
    final ArrayList<DocumentNotification> notifs = this.notifications;
    synchronized (notifs) {
      notifs.add(notification);
    }
    if (SwingUtilities.isEventDispatchThread()) {
      // In this case we want the notification to be processed
      // immediately. However, we don't want potential recursions
      // to occur when a Javascript property is set in the GUI thread.
      // Additionally, many property values may be set in one
      // event block.
      SwingUtilities.invokeLater(this.notificationImmediateAction);
    } else {
      this.notificationTimer.restart();
    }
  }

  /**
   * Invalidates the layout of the given node and schedules it to be layed out
   * later. Multiple invalidations may be processed in a single document layout.
   */
  public void delayedRelayout(final NodeImpl node) {
    final ArrayList<DocumentNotification> notifs = this.notifications;
    synchronized (notifs) {
      notifs.add(new DocumentNotification(DocumentNotification.SIZE, node));
    }
    this.notificationTimer.restart();
  }

  private void processNotifications() {
    // This is called in the GUI thread.
    final ArrayList<DocumentNotification> notifs = this.notifications;
    DocumentNotification[] notifsArray;
    synchronized (notifs) {
      final int size = notifs.size();
      if (size == 0) {
        return;
      }
      notifsArray = new DocumentNotification[size];
      notifsArray = notifs.toArray(notifsArray);
      notifs.clear();
    }
    final int length = notifsArray.length;
    for (int i = 0; i < length; i++) {
      final DocumentNotification dn = notifsArray[i];
      if ((dn.node instanceof HTMLFrameSetElement) && (this.htmlBlockPanel != null)) {
        if (this.resetIfFrameSet()) {
          // Revalidation already taken care of.
          return;
        }
      }
    }
    final HtmlBlockPanel blockPanel = this.htmlBlockPanel;
    if (blockPanel != null) {
      blockPanel.processDocumentNotifications(notifsArray);
    }
    final FrameSetPanel frameSetPanel = this.frameSetPanel;
    if (frameSetPanel != null) {
      frameSetPanel.processDocumentNotifications(notifsArray);
    }
  }

  private class SelectionDispatch extends EventDispatch2 {
    /*
     * (non-Javadoc)
     *
     * @see
     * org.xamjwg.util.EventDispatch2#dispatchEvent(java.util.EventListener,
     * java.util.EventObject)
     */
    @Override
    protected void dispatchEvent(final EventListener listener, final EventObject event) {
      ((SelectionChangeListener) listener).selectionChanged((SelectionChangeEvent) event);
    }
  }

  private class LocalDocumentNotificationListener implements DocumentNotificationListener {
    public void allInvalidated() {
      HtmlPanel.this.addNotification(new DocumentNotification(DocumentNotification.GENERIC, null));
    }

    public void invalidated(final NodeImpl node) {
      HtmlPanel.this.addNotification(new DocumentNotification(DocumentNotification.GENERIC, node));
    }

    public void lookInvalidated(final NodeImpl node) {
      HtmlPanel.this.addNotification(new DocumentNotification(DocumentNotification.LOOK, node));
    }

    public void positionInvalidated(final NodeImpl node) {
      HtmlPanel.this.addNotification(new DocumentNotification(DocumentNotification.POSITION, node));
    }

    public void sizeInvalidated(final NodeImpl node) {
      HtmlPanel.this.addNotification(new DocumentNotification(DocumentNotification.SIZE, node));
    }

    public void externalScriptLoading(final NodeImpl node) {
      // Ignorable here.
    }

    public void nodeLoaded(final NodeImpl node) {
      HtmlPanel.this.addNotification(new DocumentNotification(DocumentNotification.GENERIC, node));
    }

    public void structureInvalidated(final NodeImpl node) {
      HtmlPanel.this.addNotification(new DocumentNotification(DocumentNotification.GENERIC, node));
    }
  }

  private class NotificationTimerAction implements java.awt.event.ActionListener {
    public void actionPerformed(final ActionEvent e) {
      HtmlPanel.this.processNotifications();
    }
  }

  @Override
  public Future<Boolean> layoutCompletion() {
    return htmlBlockPanel.layoutCompletion();
  }

  public boolean isReadyToPaint() {
    final HtmlBlockPanel htmlBlock = this.htmlBlockPanel;
    if (htmlBlock != null) {
      return  (notifications.size() == 0) && htmlBlock.isReadyToPaint();
    }
    return false;
  }

  public void disableRenderHints() {
    this.htmlBlockPanel.disableRenderHints();
  }
}
