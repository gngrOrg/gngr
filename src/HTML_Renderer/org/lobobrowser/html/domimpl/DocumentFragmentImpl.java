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
 * Created on Oct 9, 2005
 */
package org.lobobrowser.html.domimpl;

import org.w3c.dom.DOMException;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;

/* TODO: extends ElementImpl as a hack, to get the ParentNode functionality.
         Better solution would be to extend from NodeImpl and create a ParentNode interface with default methods.
         Issue #88 */
public class DocumentFragmentImpl extends ElementImpl implements DocumentFragment {
  public DocumentFragmentImpl() {
    super("#document-fragment");
  }

  @Override
  public String getLocalName() {
    return null;
  }

  @Override
  public String getNodeName() {
    return "#document-fragment";
  }

  @Override
  public String getNodeValue() throws DOMException {
    return null;
  }

  @Override
  public void setNodeValue(final String nodeValue) throws DOMException {
  }

  @Override
  public short getNodeType() {
    return org.w3c.dom.Node.DOCUMENT_FRAGMENT_NODE;
  }

  @Override
  protected Node createSimilarNode() {
    return new DocumentFragmentImpl();
  }
}
