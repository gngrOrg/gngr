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
 * Created on Mar 4, 2006
 */
package org.lobobrowser.async;

import java.util.Collection;
import java.util.LinkedList;

import org.lobobrowser.util.ArrayUtilities;

/**
 * Internal class.
 *
 * @author J. H. S.
 */
final class AsyncResultWrapper<TResult> implements AsyncResult<TResult>, AsyncResultListener<TResult> {
  private AsyncResult<TResult> ar;
  private final Collection<AsyncResultListener<TResult>> listeners = new LinkedList<>();

  public AsyncResultWrapper(final AsyncResult<TResult> ar) {
    super();
    this.ar = ar;
  }

  /**
   * @param ar
   *          The ar to set.
   */
  public void setAsyncResult(final AsyncResult<TResult> ar) {
    final AsyncResult<TResult> oldResult = this.ar;
    if (oldResult != null) {
      oldResult.removeResultListener(this);
    }
    if (ar != null) {
      ar.addResultListener(this);
    }
    this.ar = ar;
  }

  public AsyncResult<TResult> getAsyncResult() {
    return this.ar;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.clientlet.AsyncResult#addResultListener(org.xamjwg.clientlet
   * .AsyncResultListener)
   */
  public void addResultListener(final AsyncResultListener<TResult> listener) {
    synchronized (this) {
      this.listeners.add(listener);
    }
    final AsyncResult<TResult> ar = this.ar;
    if (ar != null) {
      ar.signal();
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.clientlet.AsyncResult#removeResultListener(org.xamjwg.clientlet
   * .AsyncResultListener)
   */
  public void removeResultListener(final AsyncResultListener<TResult> listener) {
    synchronized (this) {
      this.listeners.remove(listener);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.clientlet.AsyncResultListener#exceptionReceived(org.xamjwg.clientlet.AsyncResultEvent)
   */
  public void exceptionReceived(final AsyncResultEvent<Throwable> event) {
    ArrayUtilities.forEachSynched(this.listeners, this, (l) -> l.exceptionReceived(event));
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.xamjwg.clientlet.AsyncResultListener#resultReceived(org.xamjwg.clientlet.AsyncResultEvent)
   */
  public void resultReceived(final AsyncResultEvent<TResult> event) {
    ArrayUtilities.forEachSynched(this.listeners, this, (l) -> l.resultReceived(event));
  }

  /*
   * (non-Javadoc)
   *
   * @see org.xamjwg.clientlet.AsyncResult#signal()
   */
  public void signal() {
    final AsyncResult<TResult> ar = this.ar;
    if (ar != null) {
      ar.signal();
    }
  }
}
