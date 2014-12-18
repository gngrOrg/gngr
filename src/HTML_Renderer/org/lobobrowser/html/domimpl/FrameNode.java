package org.lobobrowser.html.domimpl;

import org.lobobrowser.html.BrowserFrame;

/**
 * Tag interface for frame nodes.
 */
public interface FrameNode {
  public BrowserFrame getBrowserFrame();

  public void setBrowserFrame(BrowserFrame frame);
}
