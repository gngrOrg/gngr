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

/**
 * Used by methods that need to return results asynchronously. Results are
 * received in the event dispatch thread. This is a generic class that takes a
 * type parameter <code>TResult</code>, the type of the expected result object.
 *
 * @author J. H. S.
 */
public interface AsyncResult<TResult> {
  /**
   * Registers a listener of asynchronous results.
   *
   * @param listener
   */
  public void addResultListener(AsyncResultListener<TResult> listener);

  /**
   * Removes a listener.
   */
  public void removeResultListener(AsyncResultListener<TResult> listener);

  /**
   * Forces listeners to be notified of a result if there is one.
   */
  public void signal();
}
