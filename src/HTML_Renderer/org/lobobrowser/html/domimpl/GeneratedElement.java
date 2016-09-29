package org.lobobrowser.html.domimpl;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.html.style.ComputedJStyleProperties;
import org.lobobrowser.html.style.JStyleProperties;
import org.lobobrowser.js.HideFromJS;

import cz.vutbr.web.css.NodeData;

// TODO: Extend a common interface or a minimal class instead of HTMLElementImpl
public class GeneratedElement extends HTMLElementImpl {

  private NodeData nodeData;
  private JStyleProperties currentStyle;
  private final String unescapedText;

  public GeneratedElement(HTMLElementImpl parent, NodeData nodeData) {
    super("");
    setParentImpl(parent);
    this.nodeData = nodeData;

    final String text = nodeData.getAsString("content", true);

    // The string returned by node data is single quoted automatically by JStyleParser
    // And quotes inside the string are escaped.
    final String unquotedText = text.substring(1, text.length() - 1);
    this.unescapedText = unquotedText.replace("\\", "");
  }

  @HideFromJS
  public @NonNull JStyleProperties getCurrentStyle() {
    synchronized (this) {
      if (currentStyle != null) {
        return currentStyle;
      }
      currentStyle = new ComputedJStyleProperties(this, nodeData, true);
      return currentStyle;
    }
  }

  @Override
  public NodeImpl[] getChildrenArray() {
    return new NodeImpl[] {new TextImpl(unescapedText)};
  }
}
