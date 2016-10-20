package org.lobobrowser.html.renderer;

import java.awt.Graphics;
import java.awt.event.MouseEvent;

import org.lobobrowser.html.domimpl.ModelNode;
import org.lobobrowser.ua.UserAgentContext;

abstract class BaseBlockyRenderable extends BaseElementRenderable {

  public BaseBlockyRenderable(RenderableContainer container, ModelNode modelNode, UserAgentContext ucontext) {
    super(container, modelNode, ucontext);
  }

  public abstract void layout(int availWidth, int availHeight, boolean b, boolean c, FloatingBoundsSource source, boolean sizeOnly);

}
