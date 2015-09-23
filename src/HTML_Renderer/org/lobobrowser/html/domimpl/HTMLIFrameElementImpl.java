package org.lobobrowser.html.domimpl;

import java.net.URL;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.html.BrowserFrame;
import org.lobobrowser.html.js.Event;
import org.lobobrowser.html.js.Executor;
import org.lobobrowser.html.js.Window;
import org.lobobrowser.html.js.Window.JSRunnableTask;
import org.lobobrowser.html.style.IFrameRenderState;
import org.lobobrowser.html.style.RenderState;
import org.lobobrowser.js.HideFromJS;
import org.lobobrowser.ua.UserAgentContext.Request;
import org.lobobrowser.ua.UserAgentContext.RequestKind;
import org.mozilla.javascript.Function;
import org.w3c.dom.Document;
import org.w3c.dom.html.HTMLIFrameElement;

public class HTMLIFrameElementImpl extends HTMLAbstractUIElement implements HTMLIFrameElement, FrameNode {
  private volatile BrowserFrame browserFrame;

  public HTMLIFrameElementImpl(final String name) {
    super(name);
  }

  @HideFromJS
  public void setBrowserFrame(final BrowserFrame frame) {
    this.browserFrame = frame;
    createJob();
  }

  private boolean jobCreated = false;

  private void createJob() {
    synchronized (this) {
      final String src = this.getAttribute("src");
      if (src != null) {
        if (!jobCreated) {
          ((HTMLDocumentImpl) document).addJob(() -> loadURLIntoFrame(src), false);
          jobCreated = true;
        } else {
          ((HTMLDocumentImpl) document).addJob(() -> loadURLIntoFrame(src), false, 0);
        }
      } else {
        markJobDone(0, isAttachedToDocument());
      }
    }
  }

  private void markJobDone(final int jobs, final boolean loaded) {
    synchronized (this) {
      ((HTMLDocumentImpl) document).markJobsFinished(jobs, false);
      jobCreated = false;

      if (loaded) {
        if (onload != null) {
          // TODO: onload event object?
          final Window window = ((HTMLDocumentImpl) document).getWindow();
          window.addJSTask(new JSRunnableTask(0, "IFrame onload handler", () -> {
            Executor.executeFunction(HTMLIFrameElementImpl.this, onload, new Event("load", HTMLIFrameElementImpl.this), window.getContextFactory());
          }));
        }

        dispatchEvent(new Event("load", this));
      }
    }
  }

  public BrowserFrame getBrowserFrame() {
    return this.browserFrame;
  }

  public String getAlign() {
    return this.getAttribute("align");
  }

  public Document getContentDocument() {
    // TODO: Domain-based security
    final BrowserFrame frame = this.browserFrame;
    if (frame == null) {
      // Not loaded yet
      return null;
    }

    {
      // TODO: Remove this very ugly hack.
      // This is required because the content document is sometimes not ready, even though the browser frame is.
      // The browser frame is created by the layout thread, but the iframe is loaded in the window's JS Scheduler thread.
      // See GH #140
      int count = 10;
      while (count > 0 && frame.getContentDocument() == null) {
        try {
          Thread.sleep(100);
        } catch (final InterruptedException e) {
          throw new RuntimeException("Error while waiting for iframe document");
        }
        count--;
      }
    }

    return frame.getContentDocument();
  }

  public void setContentDocument(final Document d) {
    final BrowserFrame frame = this.browserFrame;
    if (frame == null) {
      // TODO: This needs to be handled.
      return;
    }
    frame.setContentDocument(d);
  }

  public Window getContentWindow() {
    final BrowserFrame frame = this.browserFrame;
    if (frame == null) {
      // Not loaded yet
      return null;
    }
    return Window.getWindow(frame.getHtmlRendererContext());
  }

  public String getFrameBorder() {
    return this.getAttribute("frameborder");
  }

  public String getHeight() {
    return this.getAttribute("height");
  }

  public String getLongDesc() {
    return this.getAttribute("longdesc");
  }

  public String getMarginHeight() {
    return this.getAttribute("marginheight");
  }

  public String getMarginWidth() {
    return this.getAttribute("marginwidth");
  }

  public String getName() {
    return this.getAttribute("name");
  }

  public String getScrolling() {
    return this.getAttribute("scrolling");
  }

  public String getSrc() {
    return this.getAttribute("src");
  }

  public String getWidth() {
    return this.getAttribute("width");
  }

  public void setAlign(final String align) {
    this.setAttribute("align", align);
  }

  public void setFrameBorder(final String frameBorder) {
    this.setAttribute("frameborder", frameBorder);
  }

  public void setHeight(final String height) {
    this.setAttribute("height", height);
  }

  public void setLongDesc(final String longDesc) {
    this.setAttribute("longdesc", longDesc);
  }

  public void setMarginHeight(final String marginHeight) {
    this.setAttribute("marginHeight", marginHeight);
  }

  public void setMarginWidth(final String marginWidth) {
    this.setAttribute("marginWidth", marginWidth);
  }

  public void setName(final String name) {
    this.setAttribute("name", name);
  }

  public void setScrolling(final String scrolling) {
    this.setAttribute("scrolling", scrolling);
  }

  public void setSrc(final String src) {
    this.setAttribute("src", src);
  }

  public void setWidth(final String width) {
    this.setAttribute("width", width);
  }

  @Override
  protected void handleAttributeChanged(String name, String oldValue, String newValue) {
    super.handleAttributeChanged(name, oldValue, newValue);
    if ("src".equals(name)) {
      createJob();
    }
  }

  @Override
  protected void handleDocumentAttachmentChanged() {
    super.handleDocumentAttachmentChanged();
    if (isAttachedToDocument()) {
      if (hasAttribute("onload")) {
        setOnload(getEventFunction(null, "onload"));
      }
    }
  }

  private Function onload;

  public Function getOnload() {
    return this.getEventFunction(this.onload, "onload");
  }

  public void setOnload(final Function onload) {
    this.onload = onload;
  }

  private void loadURLIntoFrame(final String value) {
    final BrowserFrame frame = this.browserFrame;
    if (frame != null) {
      try {
        final URL fullURL = value == null ? null : this.getFullURL(value);
        if (fullURL != null) {
          if (getUserAgentContext().isRequestPermitted(new Request(fullURL, RequestKind.Frame))) {
            frame.getHtmlRendererContext().setJobFinishedHandler(new Runnable() {
              public void run() {
                System.out.println("Iframes window's job over!");
                markJobDone(1, true);
              }
            });
            // frame.loadURL(fullURL);
            // ^^ Using window.open is better because it fires the various events correctly.
            getContentWindow().open(fullURL.toExternalForm(), "iframe", "", true);
          } else {
            System.out.println("Request not permitted: " + fullURL);
            markJobDone(1, false);
          }
        } else {
          this.warn("Can't load URL: " + value);
          // TODO: Plug: marking as load=true because we are not handling javascript URIs currently.
          //       javascript URI is being used in some of the web-platform-tests.
          markJobDone(1, true);
        }
      } catch (final java.net.MalformedURLException mfu) {
        this.warn("loadURLIntoFrame(): Unable to navigate to src.", mfu);
        markJobDone(1, false);
      } finally {
        /* TODO: Implement an onload handler
        // Copied from image element
        final Function onload = this.getOnload();
        System.out.println("onload: " + onload);
        if (onload != null) {
          // TODO: onload event object?
          Executor.executeFunction(HTMLIFrameElementImpl.this, onload, null);
        }*/
      }
    }
  }

  @Override
  protected @NonNull RenderState createRenderState(final RenderState prevRenderState) {
    return new IFrameRenderState(prevRenderState, this);
  }
}
