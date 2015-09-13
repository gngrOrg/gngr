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
 * Created on Apr 16, 2005
 */
package org.lobobrowser.html.renderer;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

//import java.util.logging.*;

/**
 * @author J. H. S.
 */
class MarkupUtilities {
  // private static final Logger logger =
  // Logger.getLogger(MarkupUtilities.class);

  // public static final int MODE_ABOVE_OR_AT = 0;
  // public static final int MODE_BELOW_OR_AT = 1;
  // public static final int MODE_LEFT_OR_AT = 0;
  // public static final int MODE_RIGHT_OR_AT = 1;

  /**
   *
   */
  private MarkupUtilities() {
    super();
  }

  public static BoundableRenderable findRenderable(final Renderable[] renderables, final Point point, final boolean vertical) {
    return findRenderable(renderables, point, 0, renderables.length, vertical);
  }

  public static BoundableRenderable findRenderable(final Renderable[] renderables, final int x, final int y, final boolean vertical) {
    return findRenderable(renderables, x, y, 0, renderables.length, vertical);
  }

  private static BoundableRenderable findRenderable(final Renderable[] renderables, final Point point, final int firstIndex,
      final int length, final boolean vertical) {
    return findRenderable(renderables, point.x, point.y, firstIndex, length, vertical);
  }

  private static BoundableRenderable findRenderable(final Renderable[] renderables, final int x, final int y, final int firstIndex,
      final int length, final boolean vertical) {
    for (int i = firstIndex + length - 1; i >= firstIndex; i--) {
      if (renderables[i] instanceof BoundableRenderable) {
        final BoundableRenderable br2 = (BoundableRenderable) renderables[i];
        if ((!br2.isDelegated()) && br2.contains(x, y)) {
          return br2;
        }
      }
    }
    return null;
  }

  // Linear scan version
  public static List<BoundableRenderable> findRenderables(final Renderable[] renderables, final int x, final int y, final boolean vertical) {
    List<BoundableRenderable> found = null;
    for (int i = 0; i < renderables.length; i++) {
      if (renderables[i] instanceof BoundableRenderable) {
        final BoundableRenderable br = (BoundableRenderable) renderables[i];
        if ((!br.isDelegated()) && br.contains(x, y)) {
          if (found == null) {
            found = new ArrayList<>();
          }
          found.add(br);
        }
      }
    }
    return found;
  }

  /*
  private static BoundableRenderable findRenderable(final Renderable[] renderables, final int x, final int y, final int firstIndex,
      final int length, final boolean vertical) {
    if (length == 0) {
      return null;
    }
    if (length == 1) {
      final Renderable r = renderables[firstIndex];
      if (!(r instanceof BoundableRenderable)) {
        return null;
      }
      final BoundableRenderable br = (BoundableRenderable) r;
      // return br.contains(x, y) ? br : null;
      if (br.contains(x, y)) {
        return br;
      } else {
        for (int i = firstIndex; i>= 0; i--) {
          if (renderables[i] instanceof BoundableRenderable) {
            final BoundableRenderable br2 = (BoundableRenderable) renderables[i];
            if (br2.contains(x, y)) {
              return br2;
            }
          }
        }
        return null;
      }
    } else {
      final int middleIndex = firstIndex + (length / 2);
      final Renderable r = renderables[middleIndex];
      Rectangle rbounds;
      if (r instanceof BoundableRenderable) {
        rbounds = ((BoundableRenderable) r).getVisualBounds();
      } else {
        final BoundableRenderable rleft = findRenderable(renderables, x, y, firstIndex, middleIndex + 1 - firstIndex, vertical);
        if (rleft != null) {
          return rleft;
        }
        return findRenderable(renderables, x, y, middleIndex + 1, length - ((middleIndex - firstIndex) + 1), vertical);
      }
      if (rbounds.contains(x, y)) {
        return (BoundableRenderable) r;
      }
      if (vertical) {
        if (y < rbounds.y) {
          return findRenderable(renderables, x, y, firstIndex, middleIndex - firstIndex, vertical);
        } else {
          return findRenderable(renderables, x, y, middleIndex + 1, length - ((middleIndex - firstIndex) + 1), vertical);
        }
      } else {
        if (x < rbounds.x) {
          return findRenderable(renderables, x, y, firstIndex, middleIndex - firstIndex, vertical);
        } else {
          return findRenderable(renderables, x, y, middleIndex + 1, length - ((middleIndex - firstIndex) + 1), vertical);
        }
      }
    }
  }*/

  public static Range findRenderables(final Renderable[] renderables, final Rectangle clipArea, final boolean vertical) {
    return findRenderables(renderables, clipArea, 0, renderables.length, vertical);
  }

  private static Range findRenderables(final Renderable[] renderables, final Rectangle clipArea, final int firstIndex, final int length,
      final boolean vertical) {
    if (length == 0) {
      return new Range(0, 0);
    }
    int offset1 = findFirstIndex(renderables, clipArea, firstIndex, length, vertical);
    int offset2 = findLastIndex(renderables, clipArea, firstIndex, length, vertical);
    if ((offset1 == -1) && (offset2 == -1)) {
      // if(logger.isLoggable(Level.INFO))logger.info("findRenderables(): Range not found for clipArea="
      // + clipArea + ",length=" + length);
      // for(int i = firstIndex; i < length; i++) {
      // logger.info("findRenderables(): renderable.bounds=" +
      // renderables[i].getBounds());
      // }
      return new Range(0, 0);
    }
    if (offset1 == -1) {
      offset1 = firstIndex;
    }
    if (offset2 == -1) {
      offset2 = (firstIndex + length) - 1;
    }
    return new Range(offset1, (offset2 - offset1) + 1);
  }

  /*
  private static int findFirstIndex(final Renderable[] renderables, final Rectangle clipArea, final int index, final int length,
      final boolean vertical) {
    Diagnostics.Assert(length > 0, "length=" + length);
    if (length == 1) {
      final Renderable r = renderables[index];
      Rectangle rbounds;
      if (r instanceof BoundableRenderable) {
        rbounds = ((BoundableRenderable) r).getVisualBounds();
      } else {
        return -1;
      }
      if (intersects(rbounds, clipArea, vertical)) {
        return index;
      } else {
        return -1;
      }
    } else {
      final int middleIndex = index + (length / 2);
      final Renderable r = renderables[middleIndex];
      Rectangle rbounds;
      if (r instanceof BoundableRenderable) {
        rbounds = ((BoundableRenderable) r).getVisualBounds();
      } else {
        final int leftIndex = findFirstIndex(renderables, clipArea, index, middleIndex - index, vertical);
        if (leftIndex != -1) {
          return leftIndex;
        }
        return findFirstIndex(renderables, clipArea, middleIndex + 1, length - ((middleIndex - index) + 1), vertical);
      }
      if (vertical) {
        if ((rbounds.y + rbounds.height) < clipArea.y) {
          final int newLen = length - ((middleIndex - index) + 1);
          return newLen == 0 ? -1 : findFirstIndex(renderables, clipArea, middleIndex + 1, newLen, vertical);
        } else {
          final int newLen = middleIndex - index;
          final int resultIdx = newLen == 0 ? -1 : findFirstIndex(renderables, clipArea, index, newLen, vertical);
          if (resultIdx == -1) {
            if (intersects(clipArea, rbounds, vertical)) {
              return middleIndex;
            }
          }
          return resultIdx;
        }
      } else {
        if ((rbounds.x + rbounds.width) < clipArea.x) {
          return findFirstIndex(renderables, clipArea, middleIndex + 1, length - (middleIndex - index), vertical);
        } else {
          final int resultIdx = findFirstIndex(renderables, clipArea, index, middleIndex - index, vertical);
          if (resultIdx == -1) {
            if (intersects(clipArea, rbounds, vertical)) {
              return middleIndex;
            }
          }
          return resultIdx;
        }
      }
    }
  } */

  private static int findFirstIndex(final Renderable[] renderables, final Rectangle clipArea, final int index, final int length,
      final boolean vertical) {
    for (int i = index; i < length; i++) {
      final Renderable ri = renderables[i];
      if (ri instanceof BoundableRenderable) {
        final BoundableRenderable br = (BoundableRenderable) ri;
        if (intersects(clipArea, br.getVisualBounds(), vertical)) {
          return i;
        }
      }
    }
    return -1;
  }

  private static int findLastIndex(final Renderable[] renderables, final Rectangle clipArea, final int index, final int length,
      final boolean vertical) {
    for (int i = index + length - 1; i >= index; i--) {
      final Renderable ri = renderables[i];
      if (ri instanceof BoundableRenderable) {
        final BoundableRenderable br = (BoundableRenderable) ri;
        if (intersects(clipArea, br.getVisualBounds(), vertical)) {
          return i;
        }
      }
    }
    return -1;
  }
  /*
  private static int findLastIndex(final Renderable[] renderables, final Rectangle clipArea, final int index, final int length,
      final boolean vertical) {
    Diagnostics.Assert(length > 0, "length<=0");
    if (length == 1) {
      final Renderable r = renderables[index];
      Rectangle rbounds;
      if (r instanceof BoundableRenderable) {
        rbounds = ((BoundableRenderable) r).getVisualBounds();
      } else {
        return -1;
      }
      if (intersects(clipArea, rbounds, vertical)) {
        return index;
      } else {
        return -1;
      }
    } else {
      final int middleIndex = index + (length / 2);
      final Renderable r = renderables[middleIndex];
      Rectangle rbounds;
      if (r instanceof BoundableRenderable) {
        rbounds = ((BoundableRenderable) r).getVisualBounds();
      } else {
        final int rightIndex = findLastIndex(renderables, clipArea, middleIndex + 1, length - ((middleIndex - index) + 1), vertical);
        if (rightIndex != -1) {
          return rightIndex;
        }
        return findLastIndex(renderables, clipArea, index, middleIndex - index, vertical);
      }
      if (vertical) {
        if (rbounds.y > (clipArea.y + clipArea.height)) {
          return findLastIndex(renderables, clipArea, index, middleIndex - index, vertical);
        } else {
          final int newLen = length - ((middleIndex - index) + 1);
          final int resultIdx = newLen == 0 ? -1 : findLastIndex(renderables, clipArea, middleIndex + 1, newLen, vertical);
          if (resultIdx == -1) {
            if (intersects(clipArea, rbounds, vertical)) {
              return middleIndex;
            }
          }
          return resultIdx;
        }
      } else {
        if (rbounds.x > (clipArea.x + clipArea.width)) {
          return findLastIndex(renderables, clipArea, index, middleIndex - index, vertical);
        } else {
          final int resultIdx = findLastIndex(renderables, clipArea, middleIndex + 1, length - ((middleIndex - index) + 1), vertical);
          if (resultIdx == -1) {
            if (intersects(clipArea, rbounds, vertical)) {
              return middleIndex;
            }
          }
          return resultIdx;
        }
      }
    }
  } */

  private static boolean intersects(final Rectangle rect1, final Rectangle rect2, final boolean vertical) {
    if (vertical) {
      return !((rect1.y > (rect2.y + rect2.height)) || (rect2.y > (rect1.y + rect1.height)));
    } else {
      return !((rect1.x > (rect2.x + rect2.width)) || (rect2.x > (rect1.x + rect1.width)));
    }
  }
}
