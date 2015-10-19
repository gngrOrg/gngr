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
package org.lobobrowser.html.domimpl;

import java.util.ArrayList;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.html.FormInput;
import org.lobobrowser.html.js.Event;
import org.lobobrowser.html.js.NotGetterSetter;
import org.lobobrowser.ua.ImageResponse;
import org.lobobrowser.ua.ImageResponse.State;
import org.mozilla.javascript.Function;
import org.w3c.dom.Node;
import org.w3c.dom.html.HTMLFormElement;

public abstract class HTMLBaseInputElement extends HTMLAbstractUIElement {
  public HTMLBaseInputElement(final String name) {
    super(name);
  }

  protected InputContext inputContext;
  protected String deferredValue;
  protected Boolean deferredChecked;
  protected Boolean deferredReadonly;
  protected Boolean deferredDisabled;

  public void setInputContext(final @NonNull InputContext ic) {
    String dv = null;
    Boolean defDisabled = null;
    Boolean defReadonly = null;
    Boolean defChecked = null;
    synchronized (this) {
      this.inputContext = ic;
      dv = this.deferredValue;
      defDisabled = this.deferredDisabled;
      defReadonly = this.deferredReadonly;
      defChecked = this.deferredChecked;
    }
    if (dv != null) {
      ic.setValue(dv);
    }
    if (defDisabled != null) {
      ic.setDisabled(defDisabled.booleanValue());
    }
    if (defReadonly != null) {
      ic.setDisabled(defReadonly.booleanValue());
    }
    if (defChecked != null) {
      ic.setDisabled(defChecked.booleanValue());
    }
  }

  public String getDefaultValue() {
    return this.getAttribute("defaultValue");
  }

  public void setDefaultValue(final String defaultValue) {
    this.setAttribute("defaultValue", defaultValue);
  }

  public HTMLFormElement getForm() {
    Node parent = this.getParentNode();
    while ((parent != null) && !(parent instanceof HTMLFormElement)) {
      parent = parent.getParentNode();
    }
    return (HTMLFormElement) parent;
  }

  public void submitForm(final FormInput[] extraFormInputs) {
    final HTMLFormElementImpl form = (HTMLFormElementImpl) this.getForm();
    if (form != null) {
      form.submit(extraFormInputs);
    }
  }

  public void resetForm() {
    final HTMLFormElement form = this.getForm();
    if (form != null) {
      form.reset();
    }
  }

  public String getAccept() {
    return this.getAttribute("accept");
  }

  public void setAccept(final String accept) {
    this.setAttribute("accept", accept);
  }

  public String getAccessKey() {
    return this.getAttribute("accessKey");
  }

  public void setAccessKey(final String accessKey) {
    this.setAttribute("accessKey", accessKey);
  }

  public String getAlign() {
    return this.getAttribute("align");
  }

  public void setAlign(final String align) {
    this.setAttribute("align", align);
  }

  public String getAlt() {
    return this.getAttribute("alit");
  }

  public void setAlt(final String alt) {
    this.setAttribute("alt", alt);
  }

  public String getName() {
    // TODO: Should this return value of "id"?
    return this.getAttribute("name");
  }

  public void setName(final String name) {
    this.setAttribute("name", name);
  }

  public boolean getDisabled() {
    final InputContext ic = this.inputContext;
    if (ic == null) {
      final Boolean db = this.deferredDisabled;
      return db == null ? false : db.booleanValue();
    } else {
      return ic.getDisabled();
    }
  }

  public void setDisabled(final boolean disabled) {
    final InputContext ic = this.inputContext;
    if (ic != null) {
      ic.setDisabled(disabled);
    } else {
      this.deferredDisabled = Boolean.valueOf(disabled);
    }
  }

  public boolean getReadOnly() {
    final InputContext ic = this.inputContext;
    if (ic == null) {
      final Boolean db = this.deferredReadonly;
      return db == null ? false : db.booleanValue();
    } else {
      return ic.getReadOnly();
    }
  }

  public void setReadOnly(final boolean readOnly) {
    final InputContext ic = this.inputContext;
    if (ic != null) {
      ic.setReadOnly(readOnly);
    } else {
      this.deferredReadonly = Boolean.valueOf(readOnly);
    }
  }

  public boolean getChecked() {
    final InputContext ic = this.inputContext;
    if (ic == null) {
      final Boolean db = this.deferredChecked;
      return db == null ? false : db.booleanValue();
    } else {
      return ic.getChecked();
    }
  }

  public void setChecked(final boolean value) {
    final InputContext ic = this.inputContext;
    if (ic != null) {
      ic.setChecked(value);
    } else {
      this.deferredChecked = Boolean.valueOf(value);
    }
  }

  public int getTabIndex() {
    final InputContext ic = this.inputContext;
    return ic == null ? 0 : ic.getTabIndex();
  }

  public void setTabIndex(final int tabIndex) {
    final InputContext ic = this.inputContext;
    if (ic != null) {
      ic.setTabIndex(tabIndex);
    }
  }

  public String getValue() {
    final InputContext ic = this.inputContext;
    if (ic != null) {
      // Note: Per HTML Spec, setValue does not set attribute.
      return ic.getValue();
    } else {
      final String dv = this.deferredValue;
      if (dv != null) {
        return dv;
      } else {
        final String val = this.getAttribute("value");
        return val == null ? "" : val;
      }
    }
  }

  protected java.io.File getFileValue() {
    final InputContext ic = this.inputContext;
    if (ic != null) {
      return ic.getFileValue();
    } else {
      return null;
    }
  }

  public void setValue(final String value) {
    InputContext ic = null;
    synchronized (this) {
      ic = this.inputContext;
      if (ic == null) {
        this.deferredValue = value;
      }
    }
    if (ic != null) {
      ic.setValue(value);
    }
  }

  @Override
  public void blur() {
    final InputContext ic = this.inputContext;
    if (ic != null) {
      ic.blur();
    }
  }

  @Override
  public void focus() {
    final InputContext ic = this.inputContext;
    if (ic != null) {
      ic.focus();
    }
  }

  public void select() {
    final InputContext ic = this.inputContext;
    if (ic != null) {
      ic.select();
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.html.domimpl.HTMLElementImpl#assignAttributeField(java.lang.
   * String, java.lang.String)
   */
  /*
  @Override
  protected void assignAttributeField(final String normalName, final String value) {
    if ("value".equals(normalName)) {
      this.setValue(value);
    } else if ("checked".equals(normalName)) {
      this.setChecked(value != null);
    } else if ("disabled".equals(normalName)) {
      this.setDisabled(value != null);
    } else if ("readonly".equals(normalName)) {
      this.setReadOnly(value != null);
    } else if ("src".equals(normalName)) {
      // TODO: Should check whether "type" == "image"
      this.loadImage(value);
    } else {
      super.assignAttributeField(normalName, value);
    }
  }*/

  @Override
  protected void handleAttributeChanged(String name, String oldValue, String newValue) {
    super.handleAttributeChanged(name, oldValue, newValue);
    if ("value".equals(name)) {
      this.setValue(newValue);
    } else if ("checked".equals(name)) {
      this.setChecked(newValue != null);
    } else if ("disabled".equals(name)) {
      this.setDisabled(newValue != null);
    } else if ("readonly".equals(name)) {
      this.setReadOnly(newValue != null);
    } else if ("src".equals(name)) {
      // TODO: Should check whether "type" == "image"
      this.loadImage(newValue);
    }
  }

  private Function onload;

  public Function getOnload() {
    return this.getEventFunction(this.onload, "onload");
  }

  public void setOnload(final Function onload) {
    this.onload = onload;
  }

  private ImageResponse imageResponse = null;
  private String imageSrc;

  private void loadImage(final String src) {
    final HTMLDocumentImpl document = (HTMLDocumentImpl) this.document;
    if (document != null) {
      synchronized (this.imageListeners) {
        this.imageSrc = src;
        this.imageResponse = null;
      }
      if (src != null) {
        document.loadImage(src, new LocalImageListener(src));
      }
    }
  }

  private final ArrayList<ImageListener> imageListeners = new ArrayList<>(1);

  /**
   * Adds a listener of image loading events. The listener gets called right
   * away if there's already an image.
   *
   * @param listener
   */
  public void addImageListener(final ImageListener listener) {
    final ArrayList<ImageListener> l = this.imageListeners;
    ImageResponse currentImageResponse;
    synchronized (l) {
      currentImageResponse = this.imageResponse;
      l.add(listener);
    }
    if (currentImageResponse.state != State.loading) {
      // Call listener right away if there's already an
      // image; holding no locks.
      listener.imageLoaded(new ImageEvent(this, currentImageResponse));
      // Should not call onload handler here. That's taken
      // care of otherwise.
    }
  }

  public void removeImageListener(final ImageListener listener) {
    final ArrayList<ImageListener> l = this.imageListeners;
    synchronized (l) {
      l.remove(l);
    }
  }

  void resetInput() {
    final InputContext ic = this.inputContext;
    if (ic != null) {
      ic.resetInput();
    }
  }

  private void dispatchEvent(final String expectedImgSrc, final ImageEvent event) {
    final ArrayList<ImageListener> l = this.imageListeners;
    ImageListener[] listenerArray;
    synchronized (l) {
      if (!expectedImgSrc.equals(this.imageSrc)) {
        return;
      }
      this.imageResponse = event.imageResponse;
      // Get array of listeners while holding lock.
      listenerArray = l.toArray(ImageListener.EMPTY_ARRAY);
    }
    final int llength = listenerArray.length;
    for (int i = 0; i < llength; i++) {
      // Inform listener, holding no lock.
      listenerArray[i].imageLoaded(event);
    }

    // TODO: With this change, setOnLoad method should add a listener with dispatch mechanism. Best implemented in a parent class.
    dispatchEvent(new Event("load", this));

    /*
    final Function onload = this.getOnload();
    if (onload != null) {
      // TODO: onload event object?
      Executor.executeFunction(HTMLBaseInputElement.this, onload, null);
    }*/
  }

  private class LocalImageListener implements ImageListener {
    private final String expectedImgSrc;

    public LocalImageListener(final String imgSrc) {
      this.expectedImgSrc = imgSrc;
    }

    public void imageLoaded(final ImageEvent event) {
      dispatchEvent(this.expectedImgSrc, event);
    }

    public void imageAborted() {
      // Do nothing
    }
  }

  @NotGetterSetter
  public void setCustomValidity(final String message) {
    // TODO Implement
    System.out.println("TODO: HTMLBaseInputElement.setCustomValidity() " + message);
  }
}
