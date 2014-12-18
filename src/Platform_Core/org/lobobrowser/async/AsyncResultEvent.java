/*
    GNU GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The Lobo Project

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public
    License as published by the Free Software Foundation; either
    verion 2 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Contact info: lobochief@users.sourceforge.net
 */
/*
 * Created on Mar 31, 2005
 */
package org.lobobrowser.async;

import java.util.EventObject;

/**
 * An asynchronous result event. This is a generic class with a
 * <code>TResult</code> type parameter, the type of the result.
 *
 * @see org.lobobrowser.async.AsyncResultListener
 * @author J. H. S.
 */
final class AsyncResultEvent<TResult> extends EventObject {
  private static final long serialVersionUID = -4837654436436995017L;
  private final TResult result;

  /**
   * Instance constructor.
   *
   * @param source
   *          The event source.
   * @param result
   *          The asynchronous result.
   */
  public AsyncResultEvent(final Object source, final TResult result) {
    super(source);
    this.result = result;
  }

  /**
   * Gets the asynchronous result. This may be an exception instance.
   */
  public TResult getResult() {
    return this.result;
  }
}
