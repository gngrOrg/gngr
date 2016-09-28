package org.lobobrowser.primary.ext;

import java.awt.Container;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.lobobrowser.gui.DefaultWindowFactory;
import org.lobobrowser.ua.UserAgentContext;
import org.lobobrowser.util.gui.WrapperLayout;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLDocument;

public class DomViewerWindow extends JFrame implements TreeSelectionListener {

  private static final long serialVersionUID = 5152758358370104207L;
  private final JTree domTree;
  private final JTextArea textArea;

  public DomViewerWindow() {
    super("Lobo DOM Viewer");
    final UserAgentContext uaContext = null; // TODO
    this.setIconImage(DefaultWindowFactory.getInstance().getDefaultImageIcon(uaContext).getImage());
    final Container contentPane = this.getContentPane();
    this.domTree = new JTree();
    this.domTree.setRootVisible(false);
    this.domTree.setShowsRootHandles(true);
    this.domTree.addTreeSelectionListener(this);
    final JTextArea textArea = createTextArea();
    this.textArea = textArea;
    final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(domTree), new JScrollPane(textArea));
    contentPane.setLayout(WrapperLayout.getInstance());
    contentPane.add(splitPane);
  }

  private static JTextArea createTextArea() {
    final JTextArea textArea = new JTextArea();
    textArea.setEditable(false);
    return textArea;
  }

  public void setDocument(final HTMLDocument document) {
    this.domTree.setModel(new DefaultTreeModel(new DomTreeNode(document)));
  }

  private class DomTreeNode extends DefaultMutableTreeNode {
    private static final long serialVersionUID = -6118030836309592257L;

    public DomTreeNode(final Node node) {
      super(node);
      final NodeList childNodes = node.getChildNodes();
      for (int i = 0; i < childNodes.getLength(); i++) {
        final Node child = childNodes.item(i);
        if (child.getNodeType() == Node.TEXT_NODE) {
          if (child.getNodeValue().trim().length() > 0) {
            this.add(new DomTreeNode(child));
          }
        } else {
          this.add(new DomTreeNode(child));
        }
      }
    }

    @Override
    public String toString() {
      return getNode().getNodeName();
    }

    public Node getNode() {
      return (Node) getUserObject();
    }

  }

  public void valueChanged(final TreeSelectionEvent treeselectionevent) {
    final TreePath path = treeselectionevent.getNewLeadSelectionPath();
    if (path != null) {
      final DomTreeNode domNode = (DomTreeNode) path.getLastPathComponent();
      final Node node = domNode.getNode();
      if ((node.getNodeType() == Node.TEXT_NODE) || (node.getNodeType() == Node.COMMENT_NODE)) {
        this.textArea.setText(node.getNodeValue());
      } else {
        this.textArea.setText("");
        this.appendNode(0, node);
      }
      this.textArea.setCaretPosition(0);
    }
  }

  private void appendNode(final int indent, final Node node) {
    if (node.getNodeType() == Node.TEXT_NODE) {
      this.textArea.append(node.getNodeValue());
    } else if (node.getNodeType() == Node.COMMENT_NODE) {
      this.textArea.append("\n/* " + node.getNodeValue() + " */");
    } else {
      this.textArea.setText(this.textArea.getText().trim());
      this.textArea.append("\n");
      addIndent(indent);
      this.textArea.append("<" + node.getNodeName());
      this.addAttributes(node);
      final NodeList childNodes = node.getChildNodes();
      if (childNodes.getLength() == 0) {
        this.textArea.append("/");
      }
      this.textArea.append(">");
      for (int i = 0; i < childNodes.getLength(); i++) {
        appendNode(indent + 1, childNodes.item(i));
      }
      if (childNodes.getLength() > 0) {
        this.textArea.append("\n");
        addIndent(indent);
        this.textArea.append("</" + node.getNodeName() + ">");
      }
    }
  }

  private void addAttributes(final Node node) {
    final NamedNodeMap attributes = node.getAttributes();
    for (int i = 0; i < attributes.getLength(); i++) {
      final Node attr = attributes.item(i);
      textArea.append(" ");
      textArea.append(attr.getNodeName());
      textArea.append("=\"");
      textArea.append(attr.getNodeValue());
      textArea.append("\"");
    }
  }

  private void addIndent(final int indent) {
    for (int i = 0; i < indent; i++) {
      this.textArea.append("   ");
    }
  }
}
