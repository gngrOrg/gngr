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
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

import org.lobobrowser.js.HideFromJS;
import org.lobobrowser.util.gui.ColorFactory;
import org.w3c.dom.html.HTMLElement;

public class HTMLCanvasElementImpl extends HTMLAbstractUIElement implements HTMLElement {

  public HTMLCanvasElementImpl() {
    super("CANVAS");
  }

  public String getHeight() {
    final UINode r = this.uiNode;
    final int height = r == null ? 0 : r.getBounds().height;
    return String.valueOf(height);
  }

  public void setHeight(final String height) {
    this.setAttribute("height", height);
  }

  public String getWidth() {
    final UINode r = this.uiNode;
    final int width = r == null ? 0 : r.getBounds().width;
    return String.valueOf(width);
  }

  public void setWidth(final String width) {
    this.setAttribute("width", width);
  }

  private java.awt.Image image = null;
  private int offsetX = 0;
  private int offsetY = 0;

  @HideFromJS
  public void paintComponent(final Graphics g) {
    if (image != null) {
      g.drawImage(image, offsetX, offsetY, null);
    }
  }

  @HideFromJS
  public void setBounds(final int x, final int y, final int width, final int height) {
    offsetX = x;
    offsetY = y;

    if (image == null) {
      createNewImage(width, height);
    } else if (image.getWidth(null) != width || image.getHeight(null) != height) {
      createNewImage(width, height);
    }
  }

  private void createNewImage(final int width, final int height) {
    image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    canvasContext.invalidate();
    drawGrid();
  }

  private void repaint() {
    getUINode().repaint(HTMLCanvasElementImpl.this);
  }

  private static final Color gridColor = new Color(30, 30, 30, 30);
  private static final int GRID_SIZE = 10;

  private void drawGrid() {
    final Graphics2D g2 = (Graphics2D) image.getGraphics();
    final int height = image.getHeight(null);
    final int width = image.getWidth(null);

    g2.setColor(gridColor);

    for (int i = 0; i < height; i += GRID_SIZE) {
      g2.drawLine(0, i, width, i);
    }

    for (int i = 0; i < width; i += 10) {
      g2.drawLine(i, 0, i, height);
    }
  }

  final public class CanvasContext {
    public void fillRect(final int x, final int y, final int width, final int height) {
      final Graphics2D g2 = getGraphics();
      g2.fillRect(x, y, width, height);
      repaint();
    }

    public void clearRect(final int x, final int y, final int width, final int height) {
      final Graphics2D g2 = getGraphics();
      g2.clearRect(x, y, width, height);
      repaint();
    }

    private Path2D currPath = null;

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

    public void arc(final int x, final int y, final int radius, final double startAngle, final double endAngle, final boolean antiClockwise) {
      final double start = antiClockwise ? startAngle : endAngle;
      final double extent = antiClockwise ? endAngle - startAngle : Math.abs(startAngle - endAngle);
      final Arc2D.Double arc = new Arc2D.Double(x-radius, y-radius, radius*2, radius*2, Math.toDegrees(start), Math.toDegrees(extent), Arc2D.OPEN);
      currPath.append(arc, false);
    }

    public void stroke() {
      final Graphics2D g2 = getGraphics();
      g2.draw(currPath);
      repaint();
    }

    public void fill() {
      final Graphics2D g2 = getGraphics();
      g2.fill(currPath);
      repaint();
    }

    public void setFillStyle(final String style) {
      final Graphics2D g2 = getGraphics();
      g2.setPaint(parseStyle(style));
    }

    public void setStrokeStyle(final String style) {
      final Graphics2D g2 = getGraphics();
      g2.setColor(parseStyle(style));
    }

    public void setGlobalAlpha(final double alpha) {
      final Graphics2D g2 = getGraphics();
      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) alpha));
    }

    public void setLineWidth(final double width) {
      final Graphics2D g2 = getGraphics();
      g2.setStroke(new BasicStroke((float) width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
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

  final private CanvasContext canvasContext = new CanvasContext();

  public CanvasContext getContext(final String type) {
    return canvasContext;
  }

  private static Color parseStyle(final String style) {
    return ColorFactory.getInstance().getColor(style);
  }

}
