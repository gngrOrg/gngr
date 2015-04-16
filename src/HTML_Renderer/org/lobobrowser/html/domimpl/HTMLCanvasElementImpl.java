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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import org.lobobrowser.html.js.NotGetterSetter;
import org.lobobrowser.js.HideFromJS;
import org.lobobrowser.util.gui.ColorFactory;
import org.mozilla.javascript.typedarrays.NativeUint8ClampedArray;
import org.w3c.dom.html.HTMLElement;

public class HTMLCanvasElementImpl extends HTMLAbstractUIElement implements HTMLElement {

  public HTMLCanvasElementImpl() {
    super("CANVAS");
  }

  public int getHeight() {
    return computedHeight;
  }

  private int computedWidth = 0;
  private int computedHeight = 0;

  public void setHeight(final double height) {
    computedHeight = ((int) height);
    this.setAttribute("height", "" + computedHeight);
    refreshImageDimension();
  }

  public int getWidth() {
    return computedWidth;
  }

  public void setWidth(final double width) {
    computedWidth = ((int) width);
    this.setAttribute("width", "" + computedWidth);
    refreshImageDimension();
  }

  private BufferedImage image = null;
  private int offsetX = 0;
  private int offsetY = 0;

  @HideFromJS
  public void paintComponent(final Graphics g) {
    if (image != null) {
      drawGrid(g);
      g.drawImage(image, offsetX, offsetY, null);
    }
  }

  @HideFromJS
  public void setBounds(final int x, final int y, final int width, final int height) {
    offsetX = x;
    offsetY = y;

    computedWidth = width;
    computedHeight = height;
    refreshImageDimension();
  }

  private void refreshImageDimension() {
    if (image == null) {
      createNewImage(computedWidth, computedHeight);
    } else if (image.getWidth(null) != computedWidth || image.getHeight(null) != computedHeight) {
      createNewImage(computedWidth, computedHeight);
    }
  }

  private void createNewImage(final int width, final int height) {
    image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    canvasContext.invalidate();
  }

  private void repaint() {
    getUINode().repaint(HTMLCanvasElementImpl.this);
  }

  private static final Color gridColor = new Color(30, 30, 30, 30);
  private static final int GRID_SIZE = 10;

  private void drawGrid(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g;
    final int height = image.getHeight(null);
    final int width = image.getWidth(null);

    g2.setColor(gridColor);

    for (int i = 0; i < height; i += GRID_SIZE) {
      g2.drawLine(0, i, width, i);
    }

    for (int i = 0; i < width; i += GRID_SIZE) {
      g2.drawLine(i, 0, i, height);
    }
  }

  final public class CanvasContext {
    public void fillRect(final int x, final int y, final int width, final int height) {
      final Graphics2D g2 = getGraphics();
      g2.setPaint(paintFill);
      g2.fillRect(x, y, width, height);
      repaint();
    }

    public void clearRect(final int x, final int y, final int width, final int height) {
      final Graphics2D g2 = getGraphics();
      g2.clearRect(x, y, width, height);
      repaint();
    }

    public void scale(final double x, final double y) {
      final Graphics2D g2 = getGraphics();
      g2.scale(x, y);
    }

    public void rotate(final double angle) {
      final Graphics2D g2 = getGraphics();
      g2.rotate(angle);
    }

    public void translate(final double x, final double y) {
      final Graphics2D g2 = getGraphics();
      g2.translate(x, y);
    }

    public void transform(final double a, final double b, final double c, final double d, final double e, final double f) {
      final Graphics2D g2 = getGraphics();
      final AffineTransform tx = new AffineTransform();
      tx.scale(a, d);
      tx.shear(-b, -c);
      tx.translate(e, f);
      g2.transform(tx);
    }

    public void setTransform(final double a, final double b, final double c, final double d, final double e, final double f) {
      final Graphics2D g2 = getGraphics();
      final AffineTransform tx = new AffineTransform();
      tx.scale(a, d);
      tx.shear(-b, -c);
      tx.translate(e, f);
      g2.setTransform(tx);
    }

    private Path2D currPath = new Path2D.Float();

    public void beginPath() {
      currPath = new Path2D.Float();
    }

    public void closePath() {
      currPath.closePath();
    }

    public void moveTo(final int x, final int y) {
      currPath.moveTo(x, y);
    }

    public void lineTo(final int x, final int y) {
      currPath.lineTo(x, y);
    }

    public void quadraticCurveTo(final double x1, final double y1, final double x2, final double y2) {
      currPath.quadTo(x1, y1, x2, y2);
    }

    public void bezierCurveTo(final double x1, final double y1, final double x2, final double y2, final double x3, final double y3) {
      currPath.curveTo(x1, y1, x2, y2, x3, y3);
    }

    public void arc(final int x, final int y, final int radius, final double startAngle, final double endAngle, final boolean antiClockwise) {
      final double start = antiClockwise ? startAngle : endAngle;
      final double extent = antiClockwise ? endAngle - startAngle : Math.abs(startAngle - endAngle);
      final Arc2D.Double arc = new Arc2D.Double(x - radius, y - radius, radius * 2, radius * 2, Math.toDegrees(start),
          Math.toDegrees(extent), Arc2D.OPEN);
      currPath.append(arc, false);
    }

    public void rect(final double x, final double y, final double width, final double height) {
      currPath.append(new Rectangle2D.Double(x, y, width, height), false);
    }

    public void strokeRect(final double x, final double y, final double w, final double h) {
      final Graphics2D g2 = getGraphics();
      g2.setPaint(paintStroke);
      g2.draw(new Rectangle2D.Double(x, y, w, h));
    }

    public void stroke() {
      final Graphics2D g2 = getGraphics();
      g2.setPaint(paintStroke);
      g2.draw(currPath);
      repaint();
    }

    public void fill() {
      final Graphics2D g2 = getGraphics();
      g2.setPaint(paintFill);
      g2.fill(currPath);
      repaint();
    }

    private Paint paintFill;
    private Paint paintStroke;

    public void setFillStyle(final String style) {
      this.paintFill = parseStyle(style);
    }

    public void setStrokeStyle(final String style) {
      this.paintStroke = parseStyle(style);
    }

    public void setGlobalAlpha(final double alpha) {
      final Graphics2D g2 = getGraphics();
      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) alpha));
    }

    private float lineWidth = 1;
    private int lineCap = BasicStroke.CAP_BUTT;
    private int lineJoin = BasicStroke.JOIN_MITER;
    private float miterLimit = 10;
    private float[] lineDash = null;
    private float lineDashOffset=0;
        
    public void setLineWidth(final double width) {
      lineWidth = (float) width;
      setStroke();
    }

    public void setLineCap(final String cap) {

      if ("butt".equals(cap)) {
        lineCap = BasicStroke.CAP_BUTT;
      } else if ("round".equals(cap)) {
        lineCap = BasicStroke.CAP_ROUND;
      } else if ("square".equals(cap)) {
        lineCap = BasicStroke.CAP_SQUARE;
      }

      setStroke();
    }

    public void setLineJoin(final String join) {

      if ("round".equals(join)) {
        lineJoin = BasicStroke.JOIN_ROUND;
      }

      else if ("bevel".equals(join)) {
        lineJoin = BasicStroke.JOIN_BEVEL;
      }

      else if ("miter".equals(join)) {
        lineJoin = BasicStroke.JOIN_MITER;
      }

      setStroke();
    }

    public void setMiterLimit(final double miterLimit) {
      this.miterLimit = (float) miterLimit;
      setStroke();
    }

    @NotGetterSetter
    public void setLineDash(final double[] segments) {
      lineDash = new float[segments.length];
      for (int i = 0; i < segments.length; i++) {
        lineDash[i] = (float) segments[i];
      }
      setStroke();
    }

    @NotGetterSetter
    public double[] getLineDash() {
      double[] lineDash1 = new double[lineDash.length];
      for (int i = 0; i < lineDash.length; i++) {
        lineDash1[i] = (float) lineDash[i];
      }
      return lineDash1;
    }

    public void setLineDashOffset(final double lineDashOffset) {
      this.lineDashOffset=(float) lineDashOffset;
      setStroke();
    }

    public double getLineDashOffset() {
      return this.lineDashOffset;
    }

    public void setStroke() {
      final Graphics2D g2 = getGraphics();
      g2.setStroke(new BasicStroke(lineWidth, lineCap, lineJoin, miterLimit, lineDash, lineDashOffset));
    }

    public ImageData getImageData(final int x, final int y, final int width, final int height) {
      final int[] argbArray = new int[width * height];
      image.getRGB(x, y, width, height, argbArray, 0, width);
      final NativeUint8ClampedArray clampedBuffer = new NativeUint8ClampedArray(width * height * 4);
      final byte[] clampedByteBuffer = clampedBuffer.getBuffer().getBuffer();
      for (int i = 0, j = 0; i < argbArray.length; i++, j += 4) {
        final int argb = argbArray[i];
        clampedByteBuffer[j    ] = (byte) ((argb >> 16) & 0xff);
        clampedByteBuffer[j + 1] = (byte) ((argb >>  8) & 0xff);
        clampedByteBuffer[j + 2] = (byte) ((argb      ) & 0xff);
        clampedByteBuffer[j + 3] = (byte) ((argb >> 24) & 0xff);
      }
      return new ImageData(width, height, clampedBuffer);
    }

    public void putImageData(final ImageData imgData, final int x, final int y) {
      putImageData(imgData, x, y, imgData.width, imgData.height);
    }

    public void putImageData(final ImageData imgData, final int x, final int y, final int width, final int height) {
      System.out.println("putImageData(imgData, x, y, width, height)" + java.util.Arrays.toString(new Object[] { x, y, width, height }));
      if (x >= 0 && y >= 0) {
        final byte[] dataBytes = imgData.getData().getBuffer().getBuffer();
        final int[] argbArray = new int[imgData.width * imgData.height];
        for (int i = 0, j = 0; i < argbArray.length; i++, j+=4) {
          argbArray[i] = packBytes2Int(
              dataBytes[j + 3], dataBytes[j    ],
              dataBytes[j + 1], dataBytes[j + 2]);
        }
        image.setRGB(x, y, Math.min(width, imgData.width), Math.min(height, imgData.height), argbArray, 0, imgData.width);
        repaint();
      }
    }

    private Graphics2D cachedGraphics = null;

    @HideFromJS
    public synchronized void invalidate() {
      cachedGraphics = null;
    }

    private synchronized Graphics2D getGraphics() {
      if (cachedGraphics == null) {
        cachedGraphics = (Graphics2D) image.getGraphics();
        cachedGraphics.setPaint(Color.BLACK);
        cachedGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      }
      return cachedGraphics;
    }

  };

  private static int packBytes2Int(final byte a, final byte b, final byte c, final byte d) {
    return (a << 24) | ((b & 0xff) << 16) | ((c & 0xff) << 8) | (d & 0xff);
  }

  public static final class ImageData {

    final private int width;
    final private int height;
    final private NativeUint8ClampedArray dataInternal;

    public ImageData(final int width, final int height, final NativeUint8ClampedArray data) {
      this.width = width;
      this.height = height;

      this.dataInternal = data;
    }

    public int getWidth() {
      return width;
    }

    public int getHeight() {
      return height;
    }

    public NativeUint8ClampedArray getData() {
      return dataInternal;
    }
  }

  final private CanvasContext canvasContext = new CanvasContext();

  public CanvasContext getContext(final String type) {
    return canvasContext;
  }

  private static Color parseStyle(final String style) {
    return ColorFactory.getInstance().getColor(style);
  }

}
