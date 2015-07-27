/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2015 Uproot Labs India Pvt Ltd

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

 */
package org.lobobrowser.html.domimpl;

import org.lobobrowser.util.NotImplementedYetException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.ranges.Range;
import org.w3c.dom.ranges.RangeException;

public class RangeImpl implements Range {
  private Node startContainer = null;
  private Node endContainer = null;
  private int startOffset = 0;
  private int endOffset = 0;

  public RangeImpl(final Document doc) {
    this.startContainer = doc;
    this.endContainer = doc;
  }

  @Override
  public Node getStartContainer() throws DOMException {
    return startContainer;
  }

  @Override
  public int getStartOffset() throws DOMException {
    return startOffset;
  }

  @Override
  public Node getEndContainer() throws DOMException {
    return endContainer;
  }

  @Override
  public int getEndOffset() throws DOMException {
    return endOffset;
  }

  @Override
  public boolean getCollapsed() throws DOMException {
    return startContainer == endContainer && startOffset == endOffset;
  }

  @Override
  public Node getCommonAncestorContainer() throws DOMException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setStart(Node refNode, int offset) throws RangeException, DOMException {
    startContainer = refNode;
    startOffset = offset;
  }

  @Override
  public void setEnd(Node refNode, int offset) throws RangeException, DOMException {
    endContainer = refNode;
    endOffset = offset;
  }

  @Override
  public void setStartBefore(Node refNode) throws RangeException, DOMException {
    // TODO Auto-generated method stub
    throw new NotImplementedYetException();
  }

  @Override
  public void setStartAfter(Node refNode) throws RangeException, DOMException {
    // TODO Auto-generated method stub
    throw new NotImplementedYetException();
  }

  @Override
  public void setEndBefore(Node refNode) throws RangeException, DOMException {
    // TODO Auto-generated method stub
    throw new NotImplementedYetException();
  }

  @Override
  public void setEndAfter(Node refNode) throws RangeException, DOMException {
    // TODO Auto-generated method stub
    throw new NotImplementedYetException();
  }

  @Override
  public void collapse(boolean toStart) throws DOMException {
    // TODO Auto-generated method stub
    throw new NotImplementedYetException();
  }

  @Override
  public void selectNode(Node refNode) throws RangeException, DOMException {
    // TODO Auto-generated method stub
    throw new NotImplementedYetException();
  }

  @Override
  public void selectNodeContents(Node refNode) throws RangeException, DOMException {
    // TODO Auto-generated method stub
    throw new NotImplementedYetException();
  }

  @Override
  public short compareBoundaryPoints(short how, Range sourceRange) throws DOMException {
    // TODO Auto-generated method stub
    throw new NotImplementedYetException();
    // return 0;
  }

  @Override
  public void deleteContents() throws DOMException {
    // TODO Auto-generated method stub
    throw new NotImplementedYetException();
  }

  @Override
  public DocumentFragment extractContents() throws DOMException {
    // TODO Auto-generated method stub
    throw new NotImplementedYetException();
  }

  @Override
  public DocumentFragment cloneContents() throws DOMException {
    // TODO Auto-generated method stub
    throw new NotImplementedYetException();
  }

  @Override
  public void insertNode(Node newNode) throws DOMException, RangeException {
    // TODO Auto-generated method stub
    throw new NotImplementedYetException();
  }

  @Override
  public void surroundContents(Node newParent) throws DOMException, RangeException {
    // TODO Auto-generated method stub
    throw new NotImplementedYetException();
  }

  @Override
  public Range cloneRange() throws DOMException {
    // TODO Auto-generated method stub
    throw new NotImplementedYetException();
  }

  @Override
  public void detach() throws DOMException {
    // TODO Auto-generated method stub
  }

}
