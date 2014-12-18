package org.lobobrowser.html.gui;

import org.lobobrowser.html.domimpl.NodeImpl;

class DocumentNotification {
  public static final int LOOK = 0;
  public static final int POSITION = 1;
  public static final int SIZE = 2;
  public static final int GENERIC = 3;

  public final int type;
  public final NodeImpl node;

  public DocumentNotification(final int type, final NodeImpl node) {
    this.type = type;
    this.node = node;
  }

  @Override
  public String toString() {
    return "DocumentNotification[type=" + this.type + ",node=" + this.node + "]";
  }
}
