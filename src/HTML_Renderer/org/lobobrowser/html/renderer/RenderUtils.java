package org.lobobrowser.html.renderer;

import java.util.Iterator;

import org.w3c.dom.html.HTMLHtmlElement;

public final class RenderUtils {

  public static Renderable findHtmlRenderable(RCollection root) {
    final Iterator<? extends Renderable> rs = root.getRenderables();
    if (rs != null) {
      while (rs.hasNext()) {
        final Renderable r = rs.next();
        if (r.getModelNode() instanceof HTMLHtmlElement) {
          return r;
        }
      }
    }

    return null;
  }

}
