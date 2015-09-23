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
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Stack;

import javax.imageio.ImageIO;

import org.lobobrowser.html.js.NotGetterSetter;
import org.lobobrowser.js.HideFromJS;
import org.lobobrowser.main.PlatformInit;
import org.lobobrowser.util.gui.ColorFactory;
import org.mozilla.javascript.typedarrays.NativeUint8ClampedArray;
import org.w3c.dom.html.HTMLElement;

public class HTMLCanvasElementImpl extends HTMLAbstractUIElement implements HTMLElement {

  public HTMLCanvasElementImpl() {
    super("CANVAS");

    // The default width and height are defined by the spec to 300 x 150
    setBounds(0, 0, 300, 150);
  }

  public String toDataURL() {
    return toDataURL("image/png", 1);
  }

  public String toDataURL(final String type, final double encoderOptions) {
    String format = "png";
    if ("image/png".equals(type)) {
      format = "png";
    } else if ("image/gif".equals(type)) {
      format = "gif";
    } else if ("image/jpeg".equals(type)) {
      format = "jpg";
    }

    if (computedWidth == 0 || computedHeight == 0) {
      return "data:,";
    }

    try {
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      ImageIO.write(image, format, outputStream);
      final String outputStr = Base64.getEncoder().encodeToString(outputStream.toByteArray());
      return "data:" + type + ";base64," + outputStr;
    } catch (final IOException e) {
      e.printStackTrace();
      throw new RuntimeException("Unexpected exception while encoding canvas to data-url");
    }
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

      // Draw a grid if debugging
      if (PlatformInit.getInstance().debugOn) {
        final Graphics newG = g.create(offsetX, offsetY, computedWidth, computedHeight);
        try {
          drawGrid(newG);
        } finally {
          newG.dispose();
        }
      }

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
    if (width != 0 && height != 0) {
      image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      canvasContext.invalidate();
    } else {
      // TODO: Need to handle the case when width or height is zero. Buffered image doesn't accept zero width / height.
    }
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
      g2.setPaint(currDrawingState.paintFill);
      g2.fillRect(x, y, width, height);
      repaint();
    }

    public void clearRect(final int x, final int y, final int width, final int height) {
      final Graphics2D g2 = getGraphics();
      g2.clearRect(x, y, width, height);
      repaint();
    }

    private AffineTransform getCurrentTransformMatrix() {
      final Graphics2D g2 = getGraphics();
      return g2.getTransform();
    }

    private Shape getCurrClip() {
      final Graphics2D g2 = getGraphics();
      return g2.getClip();
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
      final AffineTransform tx = new AffineTransform(a, b, c, d, e, f);
      g2.transform(tx);
    }

    public void setTransform(final double a, final double b, final double c, final double d, final double e, final double f) {
      final Graphics2D g2 = getGraphics();
      final AffineTransform tx = new AffineTransform(a, b, c, d, e, f);
      g2.setTransform(tx);
    }

    public void resetTransform() {
      final Graphics2D g2 = getGraphics();
      g2.setTransform(new AffineTransform());
    }

    private CanvasPath2D cpath2D = new CanvasPath2D();

    public void beginPath() {
      cpath2D = new CanvasPath2D();
    }

    public void closePath() {
      cpath2D.closePath();
    }

    public void moveTo(final double x, final double y) {
      cpath2D.moveToWithTransform(x, y, getCurrentTransformMatrix());
    }

    public void lineTo(final int x, final int y) {
      cpath2D.lineToWithTransform(x, y, getCurrentTransformMatrix());
    }

    public void quadraticCurveTo(final double x1, final double y1, final double x2, final double y2) {
      cpath2D.quadraticCurveToWithTransform(x1, y1, x2, y2, getCurrentTransformMatrix());
    }

    public void bezierCurveTo(final double x1, final double y1, final double x2, final double y2, final double x3, final double y3) {
      cpath2D.bezierCurveToWithTransform(x1, y1, x2, y2, x3, y3, getCurrentTransformMatrix());
    }

    public void arc(final int x, final int y, final int radius, final double startAngle, final double endAngle) {
      arc(x, y, radius, startAngle, endAngle, false);
    }

    public void arc(final int x, final int y, final int radius, final double startAngle, final double endAngle, final boolean antiClockwise) {
      cpath2D.arcWithTransform(x, y, radius, startAngle, endAngle, antiClockwise, getCurrentTransformMatrix());
    }

    public void arcTo(final double x1, final double y1, final double x2, final double y2, final double radius) {
      cpath2D.arcToWithTransform(x1, y1, x2, y2, radius, getCurrentTransformMatrix());
    }

    public void ellipse(final double x, final double y, final double radiusX, final double radiusY, final double rotation,
        final double startAngle, final double endAngle, final boolean antiClockwise) {
      cpath2D.ellipseWithTransform(x, y, radiusX, radiusY, rotation, startAngle, endAngle, antiClockwise, getCurrentTransformMatrix());
    }

    public void ellipse(final double x, final double y, final double radiusX, final double radiusY, final double rotation,
        final double startAngle, final double endAngle) {
      ellipse(x, y, radiusX, radiusY, rotation, startAngle, endAngle, false);
    }

    public void rect(final double x, final double y, final double width, final double height) {
      cpath2D.rectWithTransform(x, y, width, height, getCurrentTransformMatrix());
    }

    public void strokeRect(final double x, final double y, final double w, final double h) {
      final Graphics2D g2 = getGraphics();
      g2.setPaint(currDrawingState.paintStroke);
      g2.draw(new Rectangle2D.Double(x, y, w, h));
    }

    public void stroke() {
      final Graphics2D g2 = getGraphics();
      final AffineTransform currAFT = g2.getTransform();
      resetTransform();
      stroke(cpath2D);
      g2.setTransform(currAFT);
    }

    public void stroke(final CanvasPath2D cpath2D) {
      final Graphics2D g2 = getGraphics();
      g2.setPaint(currDrawingState.paintStroke);
      g2.draw(cpath2D.path2D);
      repaint();
    }

    public void fill() {
      final Graphics2D g2 = getGraphics();
      final AffineTransform currAFT = g2.getTransform();
      resetTransform();
      fill(cpath2D);
      g2.setTransform(currAFT);
    }

    public void fill(final CanvasPath2D cpath2D) {
      final Graphics2D g2 = getGraphics();
      g2.setPaint(currDrawingState.paintFill);
      g2.fill(cpath2D.path2D);
      repaint();
    }

    public void clip() {
      clip(cpath2D);
    }

    public void clip(final CanvasPath2D cpath2D) {
      final Graphics2D g2 = getGraphics();
      g2.clip(cpath2D.path2D);
    }

    public void resetClip() {
      final Graphics2D g2 = getGraphics();
      g2.setClip(null);
    }

    // TODO: Check if polymorphism can be handled in JavaObjectWrapper
    public void setFillStyle(final Object style) {
      if (style instanceof String) {
        currDrawingState.paintFill = parseColor((String) style);
      } else if (style instanceof CanvasGradient) {
        currDrawingState.paintFill = ((CanvasGradient) style).toPaint();
      } else {
        throw new UnsupportedOperationException("Fill style not recognized");
      }
    }

    private String toHex(final int r, final int g, final int b) {
      return "#" + toBrowserHexValue(r) + toBrowserHexValue(g) + toBrowserHexValue(b);
    }

    private String toBrowserHexValue(final int number) {
      final StringBuilder builder = new StringBuilder(Integer.toHexString(number & 0xff));
      while (builder.length() < 2) {
        builder.append("0");
      }
      return builder.toString().toLowerCase();
    }

    public Object getFillStyle() {
      return formatStyle(currDrawingState.paintFill);
    }

    private Object formatStyle(final Paint paint) {
      if (paint instanceof Color) {
        final Color color = (Color) paint;
        if (color.getAlpha() == 1) {
          return toHex(color.getRed(), color.getGreen(), color.getBlue());
        } else {
          System.out.println("Alpha: " + color.getAlpha());
          return "rgba(" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ", " + (color.getAlpha()/255.0) + ")";
        }
      }
      // TODO: Handle canvas pattern and canvas gradient
      return null;
    }

    // TODO: Check if polymorphism can be handled in JavaObjectWrapper
    public void setStrokeStyle(final Object style) {
      if (style instanceof String) {
        currDrawingState.paintStroke = parseColor((String) style);
      } else if (style instanceof CanvasGradient) {
        currDrawingState.paintStroke = ((CanvasGradient) style).toPaint();
      } else {
        throw new UnsupportedOperationException("Stroke style not recognized");
      }
    }

    public Object getStrokeStyle() {
      return formatStyle(currDrawingState.paintStroke);
    }

    private int rule = AlphaComposite.SRC_OVER;

    public void setGlobalAlpha(final double alpha) {
      final Graphics2D g2 = getGraphics();
      currDrawingState.globalAlpha = (float) alpha;
      final AlphaComposite a = AlphaComposite.getInstance(rule, currDrawingState.globalAlpha);
      g2.setComposite(a);
    }

    public float getGlobalAlpha() {
      return currDrawingState.globalAlpha;
    }

    private class CanvasState implements Cloneable {
      private AffineTransform currTransformMatrix;
      private Shape currClippingRegion;
      private Paint paintFill = Color.BLACK;
      private Paint paintStroke = Color.BLACK;
      private float lineWidth = 1;
      private int lineCap = BasicStroke.CAP_BUTT;
      private int lineJoin = BasicStroke.JOIN_MITER;
      private float miterLimit = 10;
      private float[] lineDash = null;
      private float lineDashOffset = 0;
      private float globalAlpha = 1;
      private String globalCompositeOperation = "source-over";

      CanvasState() {
        currTransformMatrix = null;
        currClippingRegion = null;
      }

      public Object clone() throws CloneNotSupportedException {
        return super.clone();
      }
    }

    public void setGlobalCompositeOperation(final String composition) {
      final Graphics2D g2 = getGraphics();
      currDrawingState.globalCompositeOperation = composition;

      if ("source-atop".equals(currDrawingState.globalCompositeOperation)) {
        rule = AlphaComposite.SRC_ATOP;
      } else if ("source-in".equals(currDrawingState.globalCompositeOperation)) {
        rule = AlphaComposite.SRC_IN;
      } else if ("source-out".equals(currDrawingState.globalCompositeOperation)) {
        rule = AlphaComposite.SRC_OUT;
      } else if ("source-over".equals(currDrawingState.globalCompositeOperation)) {
        rule = AlphaComposite.SRC_OVER;
      } else if ("destination-atop".equals(currDrawingState.globalCompositeOperation)) {
        rule = AlphaComposite.DST_ATOP;
      } else if ("destination-in".equals(currDrawingState.globalCompositeOperation)) {
        rule = AlphaComposite.DST_IN;
      } else if ("destination-out".equals(currDrawingState.globalCompositeOperation)) {
        rule = AlphaComposite.DST_OUT;
      } else if ("destination-over".equals(currDrawingState.globalCompositeOperation)) {
        rule = AlphaComposite.DST_OVER;
      } else if ("xor".equals(currDrawingState.globalCompositeOperation)) {
        rule = AlphaComposite.XOR;
      } else if ("clear".equals(currDrawingState.globalCompositeOperation)) {
        rule = AlphaComposite.CLEAR;
      }

      final AlphaComposite a = AlphaComposite.getInstance(rule, currDrawingState.globalAlpha);
      g2.setComposite(a);
    }

    public String getGlobalCompositeOperation() {
      return currDrawingState.globalCompositeOperation;
    }

    public void setLineWidth(final double width) {
      currDrawingState.lineWidth = (float) width;
      setStroke();
    }

    public double getLineWidth() {
      return currDrawingState.lineWidth;
    }

    public void setLineCap(final String cap) {
      if ("butt".equals(cap)) {
        currDrawingState.lineCap = BasicStroke.CAP_BUTT;
      } else if ("round".equals(cap)) {
        currDrawingState.lineCap = BasicStroke.CAP_ROUND;
      } else if ("square".equals(cap)) {
        currDrawingState.lineCap = BasicStroke.CAP_SQUARE;
      }

      setStroke();
    }

    public String getLineCap() {
      if (currDrawingState.lineCap == BasicStroke.CAP_BUTT) {
        return "butt";
      } else if (currDrawingState.lineCap == BasicStroke.CAP_ROUND) {
        return "round";
      } else if (currDrawingState.lineCap == BasicStroke.CAP_SQUARE) {
        return "square";
      }
      return null;
    }

    public void setLineJoin(final String join) {
      if ("round".equals(join)) {
        currDrawingState.lineJoin = BasicStroke.JOIN_ROUND;
      } else if ("bevel".equals(join)) {
        currDrawingState.lineJoin = BasicStroke.JOIN_BEVEL;
      } else if ("miter".equals(join)) {
        currDrawingState.lineJoin = BasicStroke.JOIN_MITER;
      }
      setStroke();
    }

    public String getLineJoin() {
      if (currDrawingState.lineJoin == BasicStroke.JOIN_MITER) {
        return "miter";
      } else if (currDrawingState.lineCap == BasicStroke.JOIN_BEVEL) {
        return "bevel";
      } else if (currDrawingState.lineCap == BasicStroke.JOIN_ROUND) {
        return "round";
      }
      return null;
    }

    public void setMiterLimit(final double miterLimit) {
      currDrawingState.miterLimit = (float) miterLimit;
      setStroke();
    }

    public float getMiterLimit() {
      return currDrawingState.miterLimit;
    }

    @NotGetterSetter
    public void setLineDash(final double[] segments) {
      currDrawingState.lineDash = new float[segments.length];
      for (int i = 0; i < segments.length; i++) {
        currDrawingState.lineDash[i] = (float) segments[i];
      }
      setStroke();
    }

    @NotGetterSetter
    public double[] getLineDash() {
      final double[] lineDash1 = new double[currDrawingState.lineDash.length];
      for (int i = 0; i < currDrawingState.lineDash.length; i++) {
        lineDash1[i] = currDrawingState.lineDash[i];
      }
      return lineDash1;
    }

    public void setLineDashOffset(final double lineDashOffset) {
      currDrawingState.lineDashOffset = (float) lineDashOffset;
      setStroke();
    }

    public double getLineDashOffset() {
      return currDrawingState.lineDashOffset;
    }

    private void setStroke() {
      final Graphics2D g2 = getGraphics();
      g2.setStroke(new BasicStroke(currDrawingState.lineWidth, currDrawingState.lineCap, currDrawingState.lineJoin,
          currDrawingState.miterLimit, currDrawingState.lineDash, currDrawingState.lineDashOffset));
    }

    public ImageData createImageData(final int width, final int height) {
      final NativeUint8ClampedArray data = new NativeUint8ClampedArray(width * height * 4);
      return new ImageData(width, height, data);
    }

    public ImageData createImageData(final ImageData imgdata) {
      final int width = imgdata.getWidth();
      final int height = imgdata.getHeight();
      final NativeUint8ClampedArray data = new NativeUint8ClampedArray(width * height * 4);
      return new ImageData(width, height, data);
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
        for (int i = 0, j = 0; i < argbArray.length; i++, j += 4) {
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
        cachedGraphics.setBackground(new Color(0, 0, 0, 0));
        cachedGraphics.setPaint(Color.BLACK);
        cachedGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      }
      return cachedGraphics;
    }

    public CanvasGradient createLinearGradient(final float x0, final float y0, final float x1, final float y1) {
      final LinearCanvasGradient linearGradient = new LinearCanvasGradient(x0, y0, x1, y1);
      return linearGradient;
    }

    private final Stack<CanvasState> drawingStateStack = new Stack<>();

    private CanvasState currDrawingState = new CanvasState();

    public void save() {
      try {
        final CanvasState cloneDrawingState = (CanvasState) currDrawingState.clone();
        cloneDrawingState.currTransformMatrix = this.getCurrentTransformMatrix();
        cloneDrawingState.currClippingRegion = this.getCurrClip();
        drawingStateStack.push(cloneDrawingState);
      } catch (final CloneNotSupportedException e) {
        e.printStackTrace();
        throw new IllegalStateException(e);
      }
    }

    public void restore() {
      if (drawingStateStack.empty()) {
        // Do nothing
      } else {
        currDrawingState = drawingStateStack.pop();
        this.setGlobalAlpha(currDrawingState.globalAlpha);
        this.setGlobalCompositeOperation(currDrawingState.globalCompositeOperation);
        this.setStroke();
        getGraphics().setTransform(currDrawingState.currTransformMatrix);
        getGraphics().setClip(currDrawingState.currClippingRegion);
      }
    }

    public void fillText(final String s, final double x, final double y) {
      final char[] chars = s.toCharArray();
      final Graphics2D g2 = getGraphics();
      g2.setPaint(currDrawingState.paintFill);
      g2.drawChars(chars, 0, chars.length, (int) x, (int) y);
    }
  }

  public abstract class CanvasGradient {

    final protected ArrayList<Float> offsets = new ArrayList<>();
    final protected ArrayList<Color> colors = new ArrayList<>();

    public void addColorStop(final float offset, final String color) {
      this.offsets.add(offset);
      this.colors.add(parseColor(color));
    }

    public abstract Paint toPaint();
  }

  public class LinearCanvasGradient extends CanvasGradient {
    private final float x0;
    private final float y0;
    private final float x1;
    private final float y1;

    LinearCanvasGradient(final float x0, final float y0, final float x1, final float y1) {
      this.x0 = x0;
      this.y0 = y0;
      this.x1 = x1;
      this.y1 = y1;
    }

    public Paint toPaint() {
      if (colors.size() == 0) {
        return new Color(0, 0, 0, 0);
      } else if (colors.size() == 1) {
        return colors.get(0);
      } else {
        // TODO: See if this can be optimized
        final float[] offsetsArray = new float[offsets.size()];
        for (int i = 0; i < offsets.size(); i++) {
          offsetsArray[i] = offsets.get(i);
        }
        return new LinearGradientPaint(x0, y0, x1, y1, offsetsArray, colors.toArray(new Color[colors.size()]));
      }
    }
  }

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

  private static Color parseColor(final String color) {
    return ColorFactory.getInstance().getColor(color);
  }
}