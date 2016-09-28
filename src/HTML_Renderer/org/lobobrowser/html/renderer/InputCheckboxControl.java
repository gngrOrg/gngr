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

import javax.swing.JCheckBox;

import org.lobobrowser.html.domimpl.HTMLBaseInputElement;
import org.lobobrowser.util.gui.WrapperLayout;

class InputCheckboxControl extends BaseInputControl {
  private static final long serialVersionUID = -7156618963339104117L;
  private final JCheckBox widget;

  public InputCheckboxControl(final HTMLBaseInputElement modelNode) {
    super(modelNode);
    this.setLayout(WrapperLayout.getInstance());
    final JCheckBox checkBox = new JCheckBox();
    checkBox.setOpaque(false);
    this.widget = checkBox;

    // Note: Value attribute cannot be set in reset() method.
    // Otherwise, layout revalidation causes typed values to
    // be lost (including revalidation due to hover.)
    checkBox.setSelected(this.controlElement.getAttributeAsBoolean("checked"));

    this.add(checkBox);
  }

  @Override
  public void reset(final int availWidth, final int availHeight) {
    super.reset(availWidth, availHeight);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#click()
   */
  @Override
  public void click() {
    this.widget.doClick();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#getChecked()
   */
  @Override
  public boolean getChecked() {
    return this.widget.isSelected();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#setChecked(boolean)
   */
  @Override
  public void setChecked(final boolean checked) {
    this.widget.setSelected(checked);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#setDisabled(boolean)
   */
  @Override
  public void setDisabled(final boolean disabled) {
    super.setDisabled(disabled);
    this.widget.setEnabled(!disabled);
  }

  public void resetInput() {
    this.widget.setSelected(this.controlElement.getAttributeAsBoolean("checked"));
  }

  @Override
  public String getValue() {
    return this.controlElement.getAttribute("value");
  }
}
