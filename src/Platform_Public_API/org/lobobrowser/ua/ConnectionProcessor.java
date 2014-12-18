/*
Copyright 1994-2006 The Lobo Project. All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer. Redistributions in binary form must
reproduce the above copyright notice, this list of conditions and the following
disclaimer in the documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE LOBO PROJECT ``AS IS'' AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
EVENT SHALL THE FREEBSD PROJECT OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.lobobrowser.ua;

import java.net.URLConnection;

/**
 * A connection pre- and post-processor.
 *
 * @see NavigatorExtensionContext#addConnectionProcessor(ConnectionProcessor)
 */
public interface ConnectionProcessor {
  public static final ConnectionProcessor[] EMPTY_ARRAY = new ConnectionProcessor[0];

  /**
   * This method is called after the request method and standard headers have
   * been set, and before a connection has been established or content has been
   * posted. Changing request properties (headers) is permitted but any other
   * changes could affect the operation of the platform.
   *
   * @param connection
   *          A URL connection.
   * @return It should return the <code>connection</code> object passed as a
   *         parameter. A different connection object can be returned if that's
   *         necessary (e.g. wrapping the original connection in order to
   *         process its POST stream).
   */
  public URLConnection processPreConnection(URLConnection connection);

  /**
   * This method is called after a connection has been established. At this
   * point there should be a response code and response headers, but the input
   * stream has not been read yet.
   * <p>
   * Note: Reading from the input stream of the connection must not be done,
   * unless a replacement stream is provided in the connection that is returned
   * by the method.
   *
   * @param connection
   *          A URL connection.
   * @return It should return the <code>connection</code> object passed as a
   *         parameter. A different connection object can be returned if that's
   *         necessary (e.g. wrapping the original connection in order to
   *         process its input stream).
   */
  public URLConnection processPostConnection(URLConnection connection);
}
