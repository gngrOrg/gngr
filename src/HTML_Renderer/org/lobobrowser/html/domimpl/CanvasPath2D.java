/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2015 Uproot Labs India Pvt Ltd

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

 */

package org.lobobrowser.html.domimpl;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;

public class CanvasPath2D {

  private boolean needNewSubpath = true;
  Path2D path2D = new Path2D.Double();

  public void moveTo(final double x, final double y) {
    moveToWithTransform(x, y, null);
  }

  void moveToWithTransform(final double x, final double y, final AffineTransform aft) {
    if (aft == null) {
      path2D.moveTo(x, y);
    } else {
      final Point2D p1 = aft.transform(new Point2D.Double(x, y), null);
      path2D.moveTo(p1.getX(), p1.getY());
    }
    currPoint = new Point2D.Double(x, y);
    needNewSubpath = false;
  }

  public void closePath() {
    if (!needNewSubpath) {
      path2D.closePath();
    }
    needNewSubpath = currPoint == null;
  }

  public void lineTo(final double x, final double y) {
    lineToWithTransform(x, y, null);
  }

  void lineToWithTransform(final double x, final double y, final AffineTransform aft) {
    if (needNewSubpath) {
      ensureSubpathWithTransform(x, y, aft);
    } else {
      if (aft == null) {
        path2D.lineTo(x, y);
      } else {
        final Point2D p1 = aft.transform(new Point2D.Double(x, y), null);
        path2D.lineTo(p1.getX(), p1.getY());
      }
    }
    currPoint = new Point2D.Double(x, y);
    needNewSubpath = false;
  }

  public void quadraticCurveTo(final double x1, final double y1, final double x2, final double y2) {
    quadraticCurveToWithTransform(x1, y1, x2, y2, null);
  }

  void quadraticCurveToWithTransform(final double x1, final double y1, final double x2, final double y2, final AffineTransform aft) {
    if (aft == null) {
      path2D.quadTo(x1, y1, x2, y2);
    } else {
      final Point2D p1 = aft.transform(new Point2D.Double(x1, y1), null);
      final Point2D p2 = aft.transform(new Point2D.Double(x2, y2), null);
      path2D.quadTo(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }
    currPoint = new Point2D.Double(x2, y2);
    needNewSubpath = false;
  }

  public void bezierCurveTo(final double x1, final double y1, final double x2, final double y2, final double x3, final double y3) {
    bezierCurveToWithTransform(x1, y1, x2, y2, x3, y3, null);
  }

  void bezierCurveToWithTransform(final double x1, final double y1, final double x2, final double y2, final double x3, final double y3,
      final AffineTransform aft) {
    if (aft == null) {
      path2D.quadTo(x1, y1, x2, y2);
    } else {
      final Point2D p1 = aft.transform(new Point2D.Double(x1, y1), null);
      final Point2D p2 = aft.transform(new Point2D.Double(x2, y2), null);
      final Point2D p3 = aft.transform(new Point2D.Double(x3, y3), null);
      path2D.curveTo(p1.getX(), p1.getY(), p2.getX(), p2.getY(), p3.getX(), p3.getY());
    }
    currPoint = new Point2D.Double(x3, y3);
    needNewSubpath = false;
  }

  private static double tweakStart(final double start, double value, final double end) {
    while (value < start) {
      value += (TWO_PI);
    }
    while (value > end) {
      value -= (TWO_PI);
    }
    return value;
  }

  private static double tweakEnd(final double start, double value, final double end) {
    while (value <= start) {
      value += (TWO_PI);
    }
    while (value > end) {
      value -= (TWO_PI);
    }
    return value;
  }

  private final static double TWO_PI = 2 * Math.PI;

  public void arc(final double x, final double y, final double radius, final double startAngle, final double endAngle) {
    arc(x, y, radius, startAngle, endAngle, false);
  }

  public void arc(final double x, final double y, final double radius, final double startAngle, final double endAngle,
      final boolean antiClockwise) {
    arcWithTransform(x, y, radius, startAngle, endAngle, antiClockwise, null);
  }

  void arcWithTransform(final double x, final double y, final double radius, final double startAngle, final double endAngle,
      final boolean antiClockwise, final AffineTransform aft) {
    ellipseWithTransform(x, y, radius, radius, 0, startAngle, endAngle, antiClockwise, aft);
  }

  private void appendWithTransform(final Shape shape, final AffineTransform aft, final boolean connect) {
    if (aft != null) {
      final PathIterator pi = shape.getPathIterator(aft);
      path2D.append(pi, connect);
    } else {
      path2D.append(shape, connect);
    }
  }

  private void setCurrPoint(final AffineTransform aft) {
    if (aft != null) {
      try {
        currPoint = aft.createInverse().transform(path2D.getCurrentPoint(), null);
      } catch (final NoninvertibleTransformException e) {
        throw new IllegalArgumentException(e);
      }
    } else {
      currPoint = path2D.getCurrentPoint();
    }
  }

  public void arcTo(final double x1, final double y1, final double x2, final double y2, final double radius) {
    arcToWithTransform(x1, y1, x2, y2, radius, null);
  }

  void arcToWithTransform(final double x1, final double y1, final double x2, final double y2, final double radius, final AffineTransform aft) {
    final Point2D p0 = ensureSubpathWithTransform(x1, y1, aft);
    final Point2D p1 = new Point2D.Double(x1, y1);
    final Point2D p2 = new Point2D.Double(x2, y2);
    final Line2D l1 = new Line2D.Double(p0, p2);
    if (p0.equals(p1) || p1.equals(p2)) {
      lineToWithTransform(x1, y1, aft);
    } else if (l1.contains(p1)) {
      lineToWithTransform(x1, y1, aft);
    } else {
      final Arc2D.Double arcTo = new Arc2D.Double();
      arcTo.setArcByTangent(p0, p1, p2, radius);
      appendWithTransform(arcTo, aft, true);
    }
  }

  public void ellipse(final double x, final double y, final double radiusX, final double radiusY, final double rotation,
      final double startAngle, final double endAngle) {
    ellipse(x, y, radiusX, radiusY, rotation, startAngle, endAngle, false);
  }

  public void ellipse(final double x, final double y, final double radiusX, final double radiusY, final double rotation,
      final double startAngle, final double endAngle, final boolean antiClockwise) {
    ellipseWithTransform(x, y, radiusX, radiusY, rotation, startAngle, endAngle, antiClockwise, null);
  }

  void ellipseWithTransform(final double x, final double y, final double radiusX, final double radiusY, final double rotation,
      final double startAngle, final double endAngle, final boolean antiClockwise, final AffineTransform aft) {
    final double start;
    final double end;
    final double extent;
    final double diffAngle = antiClockwise ? (startAngle - endAngle) : (endAngle - startAngle);

    if (diffAngle >= TWO_PI) {
      start = 0;
      end = TWO_PI;
      extent = TWO_PI;
    } else {
      start = tweakStart(0, -startAngle % TWO_PI, TWO_PI);
      end = tweakEnd(start, -endAngle % TWO_PI, TWO_PI + start);
      extent = antiClockwise ? (end - start) : -(TWO_PI + (start - end));
    }

    final Arc2D.Double ellipse = new Arc2D.Double(x - radiusX, y - radiusY, 2 * radiusX, 2 * radiusY, Math.toDegrees(start),
        Math.toDegrees(extent), Arc2D.OPEN);
    AffineTransform rotatedT;
    if (aft != null) {
      rotatedT = new AffineTransform(aft);
      rotatedT.rotate(rotation, x, y);
    } else {
      rotatedT = AffineTransform.getRotateInstance(rotation, x, y);
    }
    appendWithTransform(ellipse, rotatedT, true);
    setCurrPoint(aft);
    needNewSubpath = false;
  }

  public void rect(final double x, final double y, final double width, final double height) {
    rectWithTransform(x, y, width, height, null);
  }

  void rectWithTransform(final double x, final double y, final double width, final double height, final AffineTransform aft) {
    // Note: We can't use Rectangle2D because it doesn't support negative width, height, nor can we adjust x, y for negative
    // widths / heights because the clockwise / anti-clockwise nature of the path isn't preserved
    moveToWithTransform(x,y,aft);
    lineToWithTransform(x+width, y, aft);
    lineToWithTransform(x+width, y+height, aft);
    lineToWithTransform(x, y+height, aft);
    closePath();
    moveToWithTransform(x, y, aft);
  }

  private Point2D currPoint = null;

  private Point2D ensureSubpathWithTransform(final double x, final double y, final AffineTransform aft) {
    if (needNewSubpath) {
      moveToWithTransform(x, y, aft);
      return new Point2D.Double(x, y);
    } else {
      return currPoint;
    }
  }

}