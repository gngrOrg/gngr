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
package org.lobobrowser.html.renderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.ImageObserver;

import javax.swing.SwingUtilities;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.lobobrowser.html.domimpl.HTMLBaseInputElement;
import org.lobobrowser.html.domimpl.HTMLElementImpl;
import org.lobobrowser.html.domimpl.ImageEvent;
import org.lobobrowser.html.domimpl.ImageListener;
import org.lobobrowser.html.style.HtmlValues;
import org.lobobrowser.ua.ImageResponse;
import org.lobobrowser.ua.ImageResponse.State;
import org.lobobrowser.util.gui.WrapperLayout;

import cz.vutbr.web.css.CSSProperty.VerticalAlign;

class InputImageControl extends BaseInputControl implements ImageListener {
  private static final long serialVersionUID = -2242175570423778798L;
  // private JButton button;
  private boolean mouseBeingPressed;

  public InputImageControl(final HTMLBaseInputElement modelNode) {
    super(modelNode);
    this.setLayout(WrapperLayout.getInstance());
    // JButton button = new LocalButton();
    // this.button = button;
    // button.setMargin(RBlockViewport.ZERO_INSETS);
    // button.setBorder(null);
    // this.add(button);
    modelNode.addImageListener(this);
    this.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(final MouseEvent e) {
        mouseBeingPressed = true;
        repaint();
      }

      // public void mouseExited(MouseEvent e) {
      // mouseBeingPressed = false;
      // repaint();
      // }

      @Override
      public void mouseReleased(final MouseEvent e) {
        mouseBeingPressed = false;
        repaint();
        HtmlController.getInstance().onPressed(modelNode, e, e.getX(), e.getY());
      }
    });
  }

  private Dimension preferredSize;
  private int declaredWidth;
  private int declaredHeight;
  private ImageResponse imageResponse;

  @Override
  public void reset(final int availWidth, final int availHeight) {
    super.reset(availWidth, availHeight);
    final HTMLElementImpl element = this.controlElement;
    final int dw = HtmlValues.getOldSyntaxPixelSize(element.getAttribute("width"), availWidth, -1);
    final int dh = HtmlValues.getOldSyntaxPixelSize(element.getAttribute("height"), availHeight, -1);
    this.declaredWidth = dw;
    this.declaredHeight = dh;
    this.preferredSize = this.createPreferredSize(dw, dh);
  }

  @Override
  public VerticalAlign getVAlign() {
    final HTMLElementImpl element = this.controlElement;
    return element.getRenderState().getVerticalAlign();
  }

  @Override
  public void paintComponent(final Graphics g) {
    super.paintComponent(g);
    final Dimension size = this.getSize();
    final Insets insets = this.getInsets();
    synchronized (this) {
    }
    final ImageResponse imageResponse = this.imageResponse;
    if (imageResponse.state == State.loaded) {
      g.drawImage(imageResponse.img, insets.left, insets.top, size.width - insets.left - insets.right, size.height - insets.top - insets.bottom, this);
    } else {
      // TODO: alt
    }
    if (this.mouseBeingPressed) {
      final Color over = new Color(255, 100, 100, 64);
      final Color oldColor = g.getColor();
      try {
        g.setColor(over);
        g.fillRect(0, 0, size.width, size.height);
      } finally {
        g.setColor(oldColor);
      }
    }
  }

  @Override
  public Dimension getPreferredSize() {
    final Dimension ps = this.preferredSize;
    return ps == null ? new Dimension(0, 0) : ps;
  }

  public Dimension createPreferredSize(int dw, int dh) {
    final ImageResponse imgResponse = this.imageResponse;
    if (!imgResponse.isDecoded()) {
      return new Dimension(dw == -1 ? 0 : dw, dh == -1 ? 0 : dh);
    }

    assert (imgResponse.img != null);
    final @NonNull Image img = imgResponse.img;

    if (dw == -1) {
      dw = HtmlValues.scaleToDevicePixels(img.getWidth(this));
    }
    if (dh == -1) {
      dh = HtmlValues.scaleToDevicePixels(img.getHeight(this));
    }
    return new Dimension(dw, dh);
  }

  private final boolean checkPreferredSizeChange() {
    final Dimension newPs = this.createPreferredSize(this.declaredWidth, this.declaredHeight);
    final Dimension ps = this.preferredSize;
    if (ps == null) {
      return true;
    }
    if ((ps.width != newPs.width) || (ps.height != newPs.height)) {
      this.preferredSize = newPs;
      return true;
    } else {
      return false;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.awt.Component#imageUpdate(java.awt.Image, int, int, int, int,
   * int)
   */
  @Override
  public boolean imageUpdate(final Image img, final int infoflags, final int x, final int y, final int w, final int h) {
    if (((infoflags & ImageObserver.ALLBITS) != 0) || ((infoflags & ImageObserver.FRAMEBITS) != 0)) {
      SwingUtilities.invokeLater(() -> {
        if (!checkPreferredSizeChange()) {
          repaint();
        } else {
          ruicontrol.preferredSizeInvalidated();
        }
      });
    }
    return true;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.awt.Component#imageUpdate(java.awt.Image, int, int, int, int,
   * int)
   */
  public void imageUpdate(final Image img, final int w, final int h) {
    SwingUtilities.invokeLater(() -> {
      if (!checkPreferredSizeChange()) {
        repaint();
      } else {
        ruicontrol.preferredSizeInvalidated();
      }
    });
  }

  @Override
  public boolean paintSelection(final Graphics g, final boolean inSelection, final RenderableSpot startPoint, final RenderableSpot endPoint) {
    return inSelection;
  }

  public void imageLoaded(final ImageEvent event) {
    // Implementation of ImageListener. Invoked in a request thread most likely.
    final ImageResponse imageResponseLocal = event.imageResponse;
    // ImageIcon imageIcon = new ImageIcon(image);
    // this.button.setIcon(imageIcon);
    this.imageResponse = imageResponseLocal;
    if (imageResponseLocal.isDecoded()) {
      assert (imageResponseLocal.img != null);
      @Nullable Image image = imageResponseLocal.img;
      final int width = image.getWidth(this);
      final int height = image.getHeight(this);
      this.imageUpdate(image, width, height);
    }
  }

  public void imageAborted() {
    // do nothing
  }

  public void resetInput() {
    // NOP
  }

  @Override
  public boolean isReadyToPaint() {
    return imageResponse.isReadyToPaint();
  }

  // private static class LocalButton extends JButton {
  // public void revalidate() {
  // // ignore
  // }
  //
  // public void repaint() {
  // // ignore
  // }
  // }
}
