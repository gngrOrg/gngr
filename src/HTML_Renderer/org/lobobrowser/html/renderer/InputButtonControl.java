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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import org.lobobrowser.html.domimpl.HTMLBaseInputElement;
import org.lobobrowser.html.domimpl.HTMLInputElementImpl;
import org.lobobrowser.util.gui.WrapperLayout;

class InputButtonControl extends BaseInputControl {
  private static final long serialVersionUID = -8399402892016789567L;
  private final JButton widget;

  public InputButtonControl(final HTMLBaseInputElement modelNode) {
    super(modelNode);
    this.setLayout(WrapperLayout.getInstance());
    final JButton widget = new JButton();
    widget.setContentAreaFilled(false);
    this.widget = widget;
    this.add(widget);
    widget.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        HtmlController.getInstance().onPressed(InputButtonControl.this.controlElement, null, 0, 0);
      }
    });
  }

  @Override
  public void reset(final int availWidth, final int availHeight) {
    super.reset(availWidth, availHeight);
    final RUIControl ruiControl = this.ruicontrol;
    final JButton button = this.widget;
    button.setContentAreaFilled(!ruiControl.hasBackground());
    final java.awt.Color foregroundColor = ruiControl.getForegroundColor();
    if (foregroundColor != null) {
      button.setForeground(foregroundColor);
    }
    final HTMLInputElementImpl element = (HTMLInputElementImpl) this.controlElement;
    String text = element.getAttribute("value");
    if ((text == null) || (text.length() == 0)) {
      final String type = element.getType();
      if ("submit".equalsIgnoreCase(type)) {
        text = " ";
      } else if ("reset".equalsIgnoreCase(type)) {
        text = " ";
      } else {
        text = "";
      }
    }
    button.setText(text);
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
   * @see org.xamjwg.html.domimpl.InputContext#getValue()
   */
  @Override
  public String getValue() {
    return this.widget.getText();
  }

  @Override
  public void setDisabled(final boolean disabled) {
    super.setDisabled(disabled);
    this.widget.setEnabled(!disabled);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.html.domimpl.InputContext#setValue(java.lang.String)
   */
  @Override
  public void setValue(final String value) {
    this.widget.setText(value);
  }

  public void resetInput() {
    // nop
  }
}
