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
/*
 * Created on Feb 4, 2006
 */
package org.lobobrowser.primary.clientlets.html;

import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.io.File;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.html.BrowserFrame;
import org.lobobrowser.html.FormInput;
import org.lobobrowser.html.HtmlObject;
import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.domimpl.FrameNode;
import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.domimpl.HTMLImageElementImpl;
import org.lobobrowser.html.domimpl.HTMLLinkElementImpl;
import org.lobobrowser.html.gui.HtmlPanel;
import org.lobobrowser.request.DomainValidation;
import org.lobobrowser.request.SilentUserAgentContextImpl;
import org.lobobrowser.ua.NavigationEntry;
import org.lobobrowser.ua.NavigatorFrame;
import org.lobobrowser.ua.Parameter;
import org.lobobrowser.ua.ParameterInfo;
import org.lobobrowser.ua.RequestType;
import org.lobobrowser.ua.TargetType;
import org.lobobrowser.ua.UserAgentContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.html.HTMLCollection;
import org.w3c.dom.html.HTMLElement;
import org.w3c.dom.html.HTMLLinkElement;

public class HtmlRendererContextImpl implements HtmlRendererContext {
  private static final Logger logger = Logger.getLogger(HtmlRendererContextImpl.class.getName());
  private static final Map<NavigatorFrame, WeakReference<HtmlRendererContextImpl>> weakAssociation = new WeakHashMap<>();

  private final NavigatorFrame clientletFrame;
  private final HtmlPanel htmlPanel;

  private HtmlRendererContextImpl(final NavigatorFrame clientletFrame) {
    this.clientletFrame = clientletFrame;
    this.htmlPanel = new HtmlPanel();
  }

  // public static void clearFrameAssociations() {
  // synchronized(weakAssociation) {
  // weakAssociation.clear();
  // }
  // }
  //
  public static HtmlRendererContextImpl getHtmlRendererContext(final NavigatorFrame frame) {
    synchronized (weakAssociation) {
      final WeakReference<HtmlRendererContextImpl> existingWR = weakAssociation.get(frame);
      HtmlRendererContextImpl hrc;
      if (existingWR != null) {
        hrc = existingWR.get();
        if (hrc != null) {
          return hrc;
        }
      }
      hrc = new HtmlRendererContextImpl(frame);
      weakAssociation.put(frame, new WeakReference<>(hrc));
      return hrc;
    }
  }

  public Document getContentDocument() {
    final Object rootNode = this.htmlPanel.getRootNode();
    if (rootNode instanceof Document) {
      return (Document) rootNode;
    }
    return null;
  }

  public void setContentDocument(final Document d) {
    this.htmlPanel.setDocument(d, this);
  }

  public HtmlPanel getHtmlPanel() {
    return this.htmlPanel;
  }

  public void linkClicked(final HTMLElement linkNode, final @NonNull URL url, final String target) {
    this.navigateImpl(url, target, RequestType.CLICK, linkNode);
  }

  public void navigate(final @NonNull URL href, final String target) {
    this.navigateImpl(href, target, RequestType.PROGRAMMATIC, null);
  }

  private void navigateImpl(final @NonNull URL href, final String target, final RequestType requestType, final Object linkObject) {
    if (logger.isLoggable(Level.INFO)) {
      logger.info("navigateImpl(): href=" + href + ",target=" + target);
    }
    // First check if target is a frame identifier.
    TargetType targetType;
    if (target != null) {
      final HtmlRendererContext topCtx = this.getTop();
      final HTMLCollection frames = topCtx.getFrames();
      if (frames != null) {
        final org.w3c.dom.Node frame = frames.namedItem(target);
        if (frame instanceof FrameNode) {
          final BrowserFrame bframe = ((FrameNode) frame).getBrowserFrame();
          if (bframe == null) {
            throw new IllegalStateException("Frame node without a BrowserFrame instance: " + frame);
          }
          if (bframe.getHtmlRendererContext() != this) {
            bframe.loadURL(href);
            return;
          }
        }
      }
      // Now try special target types.
      targetType = HtmlRendererContextImpl.getTargetType(target);
    } else {
      targetType = TargetType.SELF;
    }
    if (requestType == RequestType.CLICK) {
      this.clientletFrame.linkClicked(href, targetType, linkObject);
    } else {
      this.clientletFrame.navigate(href, "GET", null, targetType, requestType);
    }
  }

  private static TargetType getTargetType(final String target) {
    if ("_blank".equalsIgnoreCase(target)) {
      return TargetType.BLANK;
    } else if ("_parent".equalsIgnoreCase(target)) {
      return TargetType.PARENT;
    } else if ("_top".equalsIgnoreCase(target)) {
      return TargetType.TOP;
    } else {
      return TargetType.SELF;
    }
  }

  public void submitForm(final String method, final @NonNull URL url, final String target, final String enctype, final FormInput[] formInputs) {
    final TargetType targetType = HtmlRendererContextImpl.getTargetType(target);
    final ParameterInfo pinfo = new LocalParameterInfo(enctype, formInputs);
    this.clientletFrame.navigate(url, method, pinfo, targetType, RequestType.FORM);
  }

  public BrowserFrame createBrowserFrame() {
    final NavigatorFrame newFrame = this.clientletFrame.createFrame();
    return new BrowserFrameImpl(newFrame, this);
  }

  public void alert(final String message) {
    this.clientletFrame.alert(message);
  }

  public void blur() {
    this.clientletFrame.windowToBack();
  }

  public void close() {
    this.clientletFrame.closeWindow();
  }

  public boolean confirm(final String message) {
    return this.clientletFrame.confirm(message);
  }

  public void focus() {
    this.clientletFrame.windowToFront();
  }

  public HtmlRendererContext open(final String url, final String windowName, final String windowFeatures, final boolean replace) {
    try {
      final URL urlObj = DomainValidation.guessURL(url);
      return this.open(urlObj, windowName, windowFeatures, replace);
    } catch (final Exception err) {
      logger.log(Level.WARNING, "open(): Unable to open URL [" + url + "].", err);
      return null;
    }
  }

  public HtmlRendererContext open(final @NonNull URL urlObj, final String windowName, final String windowFeatures, final boolean replace) {
    final Properties windowProperties = windowFeatures == null ? null : org.lobobrowser.gui.NavigatorWindowImpl
        .getPropertiesFromWindowFeatures(windowFeatures);
    try {
      final NavigatorFrame newFrame = this.clientletFrame.open(urlObj, "GET", null, windowName, windowProperties);
      if (newFrame == null) {
        return null;
      }
      return HtmlRendererContextImpl.getHtmlRendererContext(newFrame);
    } catch (final Exception err) {
      logger.log(Level.WARNING, "open(): Unable to open URL [" + urlObj + "].", err);
      return null;
    }
  }

  public String prompt(final String message, final String inputDefault) {
    return this.clientletFrame.prompt(message, inputDefault);
  }

  public void scroll(final int x, final int y) {
    this.htmlPanel.scroll(x, y);
  }

  public void scrollBy(final int xOffset, final int yOffset) {
    this.htmlPanel.scrollBy(xOffset, yOffset);
  }

  public boolean isClosed() {
    return this.clientletFrame.isWindowClosed();
  }

  public String getDefaultStatus() {
    return this.clientletFrame.getDefaultStatus();
  }

  public void setDefaultStatus(final String value) {
    this.clientletFrame.setDefaultStatus(value);
  }

  public HTMLCollection getFrames() {
    final Object rootNode = this.htmlPanel.getRootNode();
    if (rootNode instanceof HTMLDocumentImpl) {
      return ((HTMLDocumentImpl) rootNode).getFrames();
    } else {
      return null;
    }
  }

  public int getLength() {
    final HTMLCollection frames = this.getFrames();
    return frames == null ? 0 : frames.getLength();
  }

  public String getName() {
    return this.clientletFrame.getWindowId();
  }

  // private static final String HTML_RENDERER_ITEM = "lobo.html.renderer";

  public HtmlRendererContext getParent() {
    final NavigatorFrame parentFrame = this.clientletFrame.getParentFrame();
    return parentFrame == null ? null : HtmlRendererContextImpl.getHtmlRendererContext(parentFrame);
  }

  public HtmlRendererContext getOpener() {
    final HtmlRendererContext opener = this.assignedOpener;
    if (opener != null) {
      return opener;
    }
    final NavigatorFrame openerFrame = this.clientletFrame.getOpenerFrame();
    return openerFrame == null ? null : HtmlRendererContextImpl.getHtmlRendererContext(openerFrame);
  }

  private volatile HtmlRendererContext assignedOpener;

  public void setOpener(final HtmlRendererContext opener) {
    this.assignedOpener = opener;
  }

  public String getStatus() {
    return this.clientletFrame.getStatus();
  }

  public void setStatus(final String message) {
    this.clientletFrame.setStatus(message);
  }

  public void reload() {
    this.clientletFrame.reload();
  }

  public HtmlRendererContext getTop() {
    final NavigatorFrame parentFrame = this.clientletFrame.getTopFrame();
    return parentFrame == null ? null : HtmlRendererContextImpl.getHtmlRendererContext(parentFrame);
  }

  public HtmlObject getHtmlObject(final HTMLElement element) {
    // TODO
    return null;
  }

  private UserAgentContext uaContext;

  public UserAgentContext getUserAgentContext() {
    if (this.uaContext == null) {
      synchronized (this) {
        if (this.uaContext == null) {
          this.uaContext = new SilentUserAgentContextImpl(this.clientletFrame);
        }
      }
    }
    return this.uaContext;
  }

  public boolean isVisitedLink(final HTMLLinkElement link) {
    // TODO
    return false;
  }

  private Runnable getMiddleClickTask(final Node element) {
    Node currElement = element;
    Runnable task = null;

    while ((task == null) && currElement != null) {
      if (currElement instanceof HTMLLinkElementImpl) {
        final HTMLLinkElementImpl link = (HTMLLinkElementImpl) currElement;
        task = (() -> {
          HtmlRendererContextImpl.this.open(link.getAbsoluteHref(), null, null, false);
        });

      } else if (currElement instanceof HTMLImageElementImpl) {

        final HTMLImageElementImpl img = (HTMLImageElementImpl) currElement;
        try {
          final URL srcUrl = img.getFullURL(img.getSrc());
          task = (() -> {
            HtmlRendererContextImpl.this.open(srcUrl, "new window", null, false);
          });
        } catch (final MalformedURLException e) {
          logger.log(Level.INFO, "Couldn't get Image URL", e);
        }
      }
      currElement = currElement.getParentNode();
    }
    return task;
  }

  public boolean onMiddleClick(final HTMLElement element, final MouseEvent event) {
    final Runnable task = getMiddleClickTask(element);
    if (task != null) {
      SwingUtilities.invokeLater(task);
      return false;
    }
    return true;
  }

  public boolean onContextMenu(final HTMLElement element, final MouseEvent event) {
    final JPopupMenu popupMenu = new JPopupMenu();
    if (popupMenu.isPopupTrigger(event)) {
      populatePopup(element, popupMenu);

      popupMenu.show(event.getComponent(), event.getX(), event.getY());
      return false;
    }
    return true;
  }

  private void populatePopup(final HTMLElement element, final JPopupMenu popupMenu) {
    final boolean componentEntriesAdded = populatePopupForElement(element, popupMenu);

    if (componentEntriesAdded) {
      popupMenu.addSeparator();
    }

    final boolean prevPresent = getPreviousURL().isPresent();
    final boolean nextPresent = getNextURL().isPresent();
    {
      final JMenuItem menuItem = new JMenuItem("Back");
      menuItem.addActionListener(e -> {
        HtmlRendererContextImpl.this.back();
      });
      menuItem.setEnabled(prevPresent);
      popupMenu.add(menuItem);
    }
    {
      final JMenuItem menuItem = new JMenuItem("Reload");
      menuItem.addActionListener(e -> {
        HtmlRendererContextImpl.this.reload();
      });
      popupMenu.add(menuItem);
    }
    {
      final JMenuItem menuItem = new JMenuItem("Forward");
      menuItem.addActionListener(e -> {
        HtmlRendererContextImpl.this.forward();
      });
      menuItem.setEnabled(nextPresent);
      popupMenu.add(menuItem);
    }
  }

  private boolean populatePopupForElement(final HTMLElement element, final JPopupMenu popupMenu) {
    boolean linkEntryAdded = false;
    boolean imageEntryAdded = false;
    Node currElement = element;
    while (currElement != null) {
      if ((!linkEntryAdded) && (currElement instanceof HTMLLinkElementImpl)) {
        if (imageEntryAdded) {
          popupMenu.addSeparator();
        }

        final HTMLLinkElementImpl link = (HTMLLinkElementImpl) currElement;
        final JMenuItem openLinkMenuItem = new JMenuItem("Open link in new window");
        openLinkMenuItem.addActionListener(e -> {
          HtmlRendererContextImpl.this.open(link.getAbsoluteHref(), null, null, false);
        });
        popupMenu.add(openLinkMenuItem);

        final JMenuItem copyLinkMenuItem = new JMenuItem("Copy link");
        copyLinkMenuItem.addActionListener(e -> {
          final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
          clipboard.setContents(new StringSelection(link.getAbsoluteHref()), null);
        });
        popupMenu.add(copyLinkMenuItem);

        linkEntryAdded = true;
      } else if ((!imageEntryAdded) && (currElement instanceof HTMLImageElementImpl)) {

        if (linkEntryAdded) {
          popupMenu.addSeparator();
        }

        final HTMLImageElementImpl img = (HTMLImageElementImpl) currElement;
        try {
          final URL srcUrl = img.getFullURL(img.getSrc());
          final JMenuItem openImageMenuItem = new JMenuItem("Open image in new window");
          openImageMenuItem.addActionListener(e -> {
            HtmlRendererContextImpl.this.open(srcUrl, null, null, false);
          });
          popupMenu.add(openImageMenuItem);

          final JMenuItem copyImageURLMenuItem = new JMenuItem("Copy Image URL");
          copyImageURLMenuItem.addActionListener(e -> {
            final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(srcUrl.toExternalForm()), null);
          });
          popupMenu.add(copyImageURLMenuItem);

          imageEntryAdded = true;
        } catch (final MalformedURLException e) {
          logger.log(Level.INFO, "Couldn't get Image URL", e);
        }
      }
      currElement = currElement.getParentNode();
    }
    return linkEntryAdded || imageEntryAdded;
  }

  public void onMouseOut(final HTMLElement element, final MouseEvent event) {
    if (element instanceof HTMLLinkElementImpl) {
      this.clientletFrame.setStatus(null);
    }
  }

  public boolean isImageLoadingEnabled() {
    return true;
  }

  public void onMouseOver(final HTMLElement element, final MouseEvent event) {
    if (element instanceof HTMLLinkElementImpl) {
      final HTMLLinkElementImpl linkElement = (HTMLLinkElementImpl) element;
      this.clientletFrame.setStatus(linkElement.getAbsoluteHref());
    }
  }

  public boolean onDoubleClick(final HTMLElement element, final MouseEvent event) {
    return true;
  }

  public boolean onMouseClick(final HTMLElement element, final MouseEvent event) {
    return true;
  }

  public void resizeBy(final int byWidth, final int byHeight) {
    this.clientletFrame.resizeWindowBy(byWidth, byHeight);
  }

  public void resizeTo(final int width, final int height) {
    this.clientletFrame.resizeWindowTo(width, height);
  }

  public void forward() {
    this.clientletFrame.forward();
  }

  public void back() {
    this.clientletFrame.back();
  }

  public String getCurrentURL() {
    final NavigationEntry entry = this.clientletFrame.getCurrentNavigationEntry();
    return entry == null ? null : entry.getUrl().toExternalForm();
  }

  public int getHistoryLength() {
    return this.clientletFrame.getHistoryLength();
  }

  public Optional<String> getNextURL() {
    final Optional<NavigationEntry> entry = this.clientletFrame.getNextNavigationEntry();
    return entry.map((e) -> e.getUrl().toExternalForm());
  }

  public Optional<String> getPreviousURL() {
    final Optional<NavigationEntry> entry = this.clientletFrame.getPreviousNavigationEntry();
    return entry.map((e) -> e.getUrl().toExternalForm());
  }

  public void goToHistoryURL(final String url) {
    this.clientletFrame.navigateInHistory(url);
  }

  public void moveInHistory(final int offset) {
    this.clientletFrame.moveInHistory(offset);
  }

  private static class LocalParameterInfo implements ParameterInfo {
    private final String encodingType;
    private final FormInput[] formInputs;

    /**
     * @param type
     * @param inputs
     */
    public LocalParameterInfo(final String type, final FormInput[] inputs) {
      super();
      encodingType = type;
      formInputs = inputs;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.xamjwg.clientlet.ParameterInfo#getEncoding()
     */
    public String getEncoding() {
      return this.encodingType;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.xamjwg.clientlet.ParameterInfo#getParameters()
     */
    public Parameter[] getParameters() {
      final FormInput[] formInputs = this.formInputs;
      final Parameter[] params = new Parameter[formInputs.length];
      for (int i = 0; i < params.length; i++) {
        final int index = i;
        params[i] = new Parameter() {
          public String getName() {
            return formInputs[index].getName();
          }

          public File getFileValue() {
            return formInputs[index].getFileValue();
          }

          public String getTextValue() {
            return formInputs[index].getTextValue();
          }

          public boolean isFile() {
            return formInputs[index].isFile();
          }

          public boolean isText() {
            return formInputs[index].isText();
          }
        };
      }
      return params;
    }
  }

  public void setCursor(final Optional<Cursor> cursorOpt) {
    final Cursor cursor = cursorOpt.orElse(Cursor.getDefaultCursor());
    htmlPanel.setCursor(cursor);
  }

  private Runnable jobFinishedHandler = null;

  public synchronized void setJobFinishedHandler(Runnable runnable) {
    jobFinishedHandler = runnable;
  }

  public synchronized void jobsFinished() {
    if (jobFinishedHandler != null) {
      jobFinishedHandler.run();
      jobFinishedHandler = null;
    }
  }
}
