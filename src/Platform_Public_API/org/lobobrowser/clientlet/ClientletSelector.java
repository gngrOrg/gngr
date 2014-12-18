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
package org.lobobrowser.clientlet;

import org.lobobrowser.ua.NavigatorExtensionContext;

/**
 * Interface implemented in order to handle web responses, typically by checking
 * the content types.
 *
 * @see NavigatorExtensionContext#addClientletSelector(ClientletSelector)
 */
public interface ClientletSelector {
  /**
   * This method is invoked after a URL response has been received by the user
   * agent. It obtains a <code>Clientlet</code> instance that can handle the
   * given request and response. It <i>must</i> return <code>null</code> if it
   * does not know how to handle the response. Generally a selector should only
   * attempt to handle a specific response MIME type. If the MIME type is
   * missing or is generic (e.g. application/octet-stream), then the selector
   * should check the file extension. It is recommended that implementors use
   * the convenience method, {@link ClientletResponse#matches(String, String[])}
   * , to determine if they should handle a response.
   * <p>
   * The <code>select</code> method is invoked on all extensions that have
   * registered one or more clientlet selectors. Extensions are invoked in
   * descending order of priority. If a extension returns a non-null clientlet,
   * the rest of the extensions are not invoked.
   *
   * @return A new <code>Clientlet</code> instance, or <code>null</code> if the
   *         clientlet selector does not know how to handle the response.
   * @see ClientletResponse#matches(String, String[])
   */
  public Clientlet select(ClientletRequest request, ClientletResponse response);

  /**
   * This method is meant for the primary extension to handle content that was
   * not handled by any other extension. Invocation of this method proceeds in
   * <i>ascending</i> order of extension priority. Implementors will generally
   * have this method return <code>null</code> unless they would like to allow
   * extensions with lower priority to override the selection.
   */
  public Clientlet lastResortSelect(ClientletRequest request, ClientletResponse response);
}
