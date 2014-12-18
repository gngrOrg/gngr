package org.lobobrowser.html.renderer;

import java.util.Comparator;

class ZIndexComparator implements Comparator<PositionedRenderable> {
  // Note: It is assumed that objects don't change their
  // z-indexes or ordinals after entering the sorted set.
  // They may do so after the sorted set is no longer valid.
  public int compare(final PositionedRenderable element1, final PositionedRenderable element2) {
    final int zIndex1 = element1.renderable.getZIndex();
    final int zIndex2 = element2.renderable.getZIndex();
    final int diff = zIndex1 - zIndex2;
    if (diff != 0) {
      return diff;
    }
    return element1.ordinal - element2.ordinal;
  }
}
