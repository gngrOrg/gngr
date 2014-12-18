package org.lobobrowser.html.style;

public class BaseFontRenderState extends RenderStateDelegator {
  private final int fontBase;

  public BaseFontRenderState(final RenderState prevRenderState, final int fontBase) {
    super(prevRenderState);
    this.fontBase = fontBase;
  }

  @Override
  public int getFontBase() {
    return this.fontBase;
  }
}
