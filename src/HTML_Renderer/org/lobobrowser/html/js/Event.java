/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The XAMJ Project

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
package org.lobobrowser.html.js;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import org.lobobrowser.js.AbstractScriptableDelegate;
import org.lobobrowser.js.HideFromJS;
import org.w3c.dom.Node;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLElement;

public class Event extends AbstractScriptableDelegate implements org.w3c.dom.events.Event {
  private boolean cancelBubble;
  private HTMLElement fromElement, toElement;
  private int leafX, leafY;
  private boolean returnValue;
  private Node srcElement;
  private String type;
  private final java.awt.event.InputEvent inputEvent;
  private boolean propagationStopped = false;
  private final boolean bubbles;

  public Event(final String type, final Node srcElement, final boolean bubbles) {
    this.type = type;
    this.srcElement = srcElement;
    this.inputEvent = null;
    this.bubbles = bubbles;
  }

  public Event(final String type, final Node srcElement, final java.awt.event.InputEvent mouseEvent, final int leafX, final int leafY) {
    this.type = type;
    this.srcElement = srcElement;
    this.leafX = leafX;
    this.leafY = leafY;
    this.inputEvent = mouseEvent;
    this.bubbles = true;
  }

  public Event(final String type, final Node srcElement, final java.awt.event.KeyEvent keyEvent) {
    this.type = type;
    this.srcElement = srcElement;
    this.inputEvent = keyEvent;
    this.bubbles = true;
  }

  public Event(final String type, final Node srcElement) {
    this.type = type;
    this.srcElement = srcElement;
    this.inputEvent = null;
    this.bubbles = true;
  }

  public boolean getAltKey() {
    final InputEvent ie = this.inputEvent;
    return ie == null ? false : ie.isAltDown();
  }

  public boolean getShiftKey() {
    final InputEvent ie = this.inputEvent;
    return ie == null ? false : ie.isShiftDown();
  }

  public boolean getCtrlKey() {
    final InputEvent ie = this.inputEvent;
    return ie == null ? false : ie.isControlDown();
  }

  public int getButton() {
    final InputEvent ie = this.inputEvent;
    if (ie instanceof MouseEvent) {
      // return ((MouseEvent) ie).getButton();
      // range of button is 0 to N in DOM spec, but 1 to N in AWT
      return ((MouseEvent) ie).getButton() - 1;
    } else {
      return 0;
    }
  }

  public boolean isCancelBubble() {
    return cancelBubble;
  }

  public void setCancelBubble(final boolean cancelBubble) {
    System.out.println("Event.setCancelBubble()");
    this.cancelBubble = cancelBubble;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public int getClientX() {
    final InputEvent ie = this.inputEvent;
    if (ie instanceof MouseEvent) {
      return ((MouseEvent) ie).getX();
    } else {
      return 0;
    }
  }

  public int getClientY() {
    final InputEvent ie = this.inputEvent;
    if (ie instanceof MouseEvent) {
      return ((MouseEvent) ie).getY();
    } else {
      return 0;
    }
  }

  public int getKeyCode() {
    final InputEvent ie = this.inputEvent;
    if (ie instanceof KeyEvent) {
      return ((KeyEvent) ie).getKeyCode();
    } else {
      return 0;
    }
  }

  // public int getOffsetX() {
  // // Despite advertising that it returns an element-relative offset,
  // // IE doesn't do this.
  // //TODO: Must be relative to top viewport.
  // return this.getClientX() - 2;
  // }
  //
  // public int getOffsetY() {
  // // Despite advertising that it returns an element-relative offset,
  // // IE doesn't do this.
  // //TODO: Must be relative to top viewport.
  // return this.getClientY() - 2;
  // }

  public boolean isReturnValue() {
    return returnValue;
  }

  public void setReturnValue(final boolean returnValue) {
    this.returnValue = returnValue;
  }

  public Node getSrcElement() {
    return srcElement;
  }

  public void setSrcElement(final HTMLElement srcElement) {
    this.srcElement = srcElement;
  }

  public HTMLElement getFromElement() {
    return fromElement;
  }

  public void setFromElement(final HTMLElement fromElement) {
    this.fromElement = fromElement;
  }

  public HTMLElement getToElement() {
    return toElement;
  }

  public void setToElement(final HTMLElement toElement) {
    this.toElement = toElement;
  }

  public int getLeafX() {
    return leafX;
  }

  public void setLeafX(final int leafX) {
    this.leafX = leafX;
  }

  public int getLeafY() {
    return leafY;
  }

  public void setLeafY(final int leafY) {
    this.leafY = leafY;
  }

  @Override
  public EventTarget getTarget() {
    System.out.println("TODO: Event.getTarget()");
    // TODO: Target and source may not be always same. Need to add a constructor param for target.
    return (EventTarget) srcElement;
  }

  @Override
  public EventTarget getCurrentTarget() {
    System.out.println("TODO: Event.getCurrentTarget()");
    return null;
  }

  private short currentPhase = 0;

  @Override
  public short getEventPhase() {
    System.out.println("Event.getEventPhase() : " + currentPhase);
    return currentPhase;
  }

  @HideFromJS
  public void setPhase(final short newPhase) {
    currentPhase = newPhase;
  }

  @Override
  public boolean getBubbles() {
    return this.bubbles;
  }

  @Override
  public boolean getCancelable() {
    System.out.println("TODO: Event.getCancelable()");
    return false;
  }

  @Override
  public long getTimeStamp() {
    System.out.println("Event.getTimeStamp()");
    return 0;
  }

  @Override
  public void stopPropagation() {
    propagationStopped = true;
    System.out.println("Event.stopPropagation()");
  }

  // TODO: Hide from JS
  public boolean isPropagationStopped() {
    return propagationStopped;
  }

  @Override
  public void preventDefault() {
    System.out.println("TODO: Event.preventDefault()");
  }

  @Override
  public void initEvent(final String eventTypeArg, final boolean canBubbleArg, final boolean cancelableArg) {
    System.out.println("TODO: Event.initEvent()");
  }

  @Override
  public String toString() {
    return "Event [phase=" + currentPhase + ", type=" + type + ", leafX=" + leafX + ", leafY=" + leafY + ", srcElement=" + srcElement + "]";
  }

}
