package org.lobobrowser.html.domimpl;

import org.w3c.dom.html.HTMLBRElement;

public class HTMLBRElementImpl extends HTMLElementImpl implements HTMLBRElement {
  public HTMLBRElementImpl(final String name) {
    super(name);
  }

  public String getClear() {
    return this.getAttribute("clear");
  }

  public void setClear(final String clear) {
    this.setAttribute("clear", clear);
  }

  @Override
  protected void appendInnerTextImpl(final StringBuffer buffer) {
    buffer.append("\r\n");
    super.appendInnerTextImpl(buffer);
  }
}
