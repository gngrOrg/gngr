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

import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class CanvasPath2D {

  private boolean needNewSubpath = true;
  Path2D path2D = new Path2D.Double();

  public void moveTo(final double x, final double y) {
    path2D.moveTo(x, y);
    needNewSubpath = false;
  }

  public void closePath() {
    path2D.closePath();
    needNewSubpath = true;
  }

  public void lineTo(final double x, final double y) {
    if (needNewSubpath) {
      ensureSubpath(x, y);
    } else {
      path2D.lineTo(x, y);
    }
    needNewSubpath = false;
  }

  public void quadraticCurveTo(final double x1, final double y1, final double x2, final double y2) {
    path2D.quadTo(x1, y1, x2, y2);
    needNewSubpath = false;
  }

  public void bezierCurveTo(final double x1, final double y1, final double x2, final double y2, final double x3, final double y3) {
    path2D.curveTo(x1, y1, x2, y2, x3, y3);
    needNewSubpath = false;
  }

  private double tweakStart(final double start, double value, final double end) {
    while (value < start) {
      value += (TWO_PI);
    }
    while (value > end) {
      value -= (TWO_PI);
    }
    return value;
  }

  private double tweakEnd(final double start, double value, final double end) {
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
    if (needNewSubpath)
    {
      final Arc2D.Double arc = new Arc2D.Double();
      arc.setArcByCenter(x, y, radius, Math.toDegrees(start), Math.toDegrees(extent), Arc2D.OPEN);
      path2D.append(arc, false);
      needNewSubpath = false;
    }
    else {
      final Arc2D.Double arc = new Arc2D.Double();
      arc.setArcByCenter(x, y, radius, Math.toDegrees(start), Math.toDegrees(extent), Arc2D.OPEN);
      path2D.append(arc, true);
    }
  }

  public void arcTo(final double x1, final double y1, final double x2, final double y2, final double radius) {
    final Point2D p0 = ensureSubpath(x1, y1);
    final Point2D p1 = new Point2D.Double(x1, y1);
    final Point2D p2 = new Point2D.Double(x2, y2);
    final Line2D l1 = new Line2D.Double(p0, p2);
    if (p0.equals(p1) || p1.equals(p2)) {
      path2D.lineTo(x1, y1);
    } else if (l1.contains(p1)) {
      path2D.lineTo(x1, y1);
    } else {
      final Arc2D.Double arc = new Arc2D.Double();
      arc.setArcByTangent(new Point2D.Double(p0.getX(), p0.getY()), new Point2D.Double(x1, y1), new Point2D.Double(x2, y2), radius);
      path2D.append(arc, true);
    }
  }

  public void ellipse(final double x, final double y, final double radiusX, final double radiusY, final double rotation,
      final double startAngle, final double endAngle) {
    ellipse(x, y, radiusX, radiusY, rotation, startAngle, endAngle, false);
  }

  public void ellipse(final double x, final double y, final double radiusX, final double radiusY, final double rotation,
      final double startAngle, final double endAngle, final boolean antiClockwise) {
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

    if (needNewSubpath) {
      final Arc2D.Double ellipse = new Arc2D.Double(x - radiusX, y - radiusY, 2 * radiusX, 2 * radiusY, Math.toDegrees(start),
          Math.toDegrees(extent), Arc2D.OPEN);
      final AffineTransform transform = new AffineTransform();
      transform.rotate(rotation, x, y);
      final PathIterator pi = ellipse.getPathIterator(transform);
      path2D.append(pi, false);
      needNewSubpath = false;
    } else {
      final Arc2D.Double ellipse = new Arc2D.Double(x - radiusX, y - radiusY, 2 * radiusX, 2 * radiusY, Math.toDegrees(start),
          Math.toDegrees(extent), Arc2D.OPEN);
      final AffineTransform transform = new AffineTransform();
      transform.rotate(rotation, x, y);
      final PathIterator pi = ellipse.getPathIterator(transform);
      path2D.append(pi, true);
    }
  }

  public void rect(final double x, final double y, final double width, final double height) {
    path2D.append(new Rectangle2D.Double(x, y, width, height), false);
  }

  private Point2D ensureSubpath(final double x, final double y) {
    if (needNewSubpath) {
      moveTo(x, y);
      needNewSubpath = false;
      return new Point2D.Double(x, y);
    } else {
      return path2D.getCurrentPoint();
    }
  }
}