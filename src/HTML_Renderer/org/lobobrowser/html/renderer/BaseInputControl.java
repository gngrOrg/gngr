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
 * Created on Jan 15, 2006
 */
package org.lobobrowser.html.renderer;

import java.awt.Graphics;
import java.io.File;

import org.lobobrowser.html.domimpl.HTMLBaseInputElement;
import org.lobobrowser.html.domimpl.InputContext;

import cz.vutbr.web.css.CSSProperty.VerticalAlign;

abstract class BaseInputControl extends BaseControl implements InputContext {
  private static final long serialVersionUID = -5300609640161763515L;
  protected String value;

  public BaseInputControl(final HTMLBaseInputElement modelNode) {
    super(modelNode);
    this.setOpaque(false);
  }

  @Override
  public void reset(final int availWidth, final int availHeight) {
    super.reset(availWidth, availHeight);
    final String sizeText = this.controlElement.getAttribute("size");
    if (sizeText != null) {
      try {
        this.size = Integer.parseInt(sizeText);
      } catch (final NumberFormatException nfe) {
        // ignore
      }
    }
  }

  @Override
  public VerticalAlign getVAlign() {
    return VerticalAlign.BOTTOM;
  }

  protected int size = -1;

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#blur()
   */
  public void blur() {
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#click()
   */
  public void click() {
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#focus()
   */
  public void focus() {
    this.requestFocus();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#getChecked()
   */
  public boolean getChecked() {
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#getDisabled()
   */
  public boolean getDisabled() {
    return !this.isEnabled();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#getMaxLength()
   */
  public int getMaxLength() {
    return 0;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#getReadOnly()
   */
  public boolean getReadOnly() {
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#getTabIndex()
   */
  public int getTabIndex() {
    return 0;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#getValue()
   */
  public String getValue() {
    return this.value;
  }

  /**
   * Returns <code>null</code>. It should be overridden by controls that support
   * multiple values.
   */
  public String[] getValues() {
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#select()
   */
  public void select() {
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#setChecked(boolean)
   */
  public void setChecked(final boolean checked) {
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#setDisabled(boolean)
   */
  public void setDisabled(final boolean disabled) {
    this.setEnabled(!disabled);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#setMaxLength(int)
   */
  public void setMaxLength(final int maxLength) {
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#setReadOnly(boolean)
   */
  public void setReadOnly(final boolean readOnly) {
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#setSize(int)
   */
  public void setControlSize(final int size) {
    this.size = size;
    this.invalidate();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#setTabIndex(int)
   */
  public void setTabIndex(final int tabIndex) {
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#setValue(java.lang.String)
   */
  public void setValue(final String value) {
    this.value = value;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#getTextSize()
   */
  public int getControlSize() {
    return this.size;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#getCols()
   */
  public int getCols() {
    return 0;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#getRows()
   */
  public int getRows() {
    return 0;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#setCols(int)
   */
  public void setCols(final int cols) {
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#setRows(int)
   */
  public void setRows(final int rows) {
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.renderer.UIControl#paintSelection(java.awt.Graphics,
   * boolean, org.xamjwg.html.renderer.RenderablePoint,
   * org.xamjwg.html.renderer.RenderablePoint)
   */
  public boolean paintSelection(final Graphics g, final boolean inSelection, final RenderableSpot startPoint, final RenderableSpot endPoint) {
    return inSelection;
  }

  public boolean getMultiple() {
    // For selects
    return false;
  }

  public int getSelectedIndex() {
    // For selects
    return -1;
  }

  public int getVisibleSize() {
    // For selects
    return 0;
  }

  public void setMultiple(final boolean value) {
    // For selects
  }

  public void setSelectedIndex(final int value) {
    // For selects
  }

  public void setVisibleSize(final int value) {
    // For selects
  }

  public File getFileValue() {
    // For file inputs
    return null;
  }
}
