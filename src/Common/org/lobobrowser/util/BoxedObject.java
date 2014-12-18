package org.lobobrowser.util;

public class BoxedObject {
  private Object object;

  public BoxedObject() {
  }

  public BoxedObject(final Object object) {
    super();
    this.object = object;
  }

  public Object getObject() {
    return object;
  }

  public void setObject(final Object object) {
    this.object = object;
  }

}
