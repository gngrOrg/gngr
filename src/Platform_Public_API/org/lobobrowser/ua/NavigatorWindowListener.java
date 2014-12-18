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

import java.util.EventListener;

/**
 * A listener of navigator window events. Listener methods are invoked by the
 * user agent in the GUI dispatch thread.
 */
public interface NavigatorWindowListener extends EventListener {
  /**
   * Invoked after a connection request to a URL succeeds and the document
   * starts loading. There is no guarantee that the document will render. For
   * example, a download may be started instead.
   * <p>
   */
  public void documentAccessed(NavigatorWindowEvent event);

  /**
   * Invoked after a clientlet has set the document content. This could be when
   * the document has rendered or is being rendered. This method is also called
   * when a page error occurs.
   * <p>
   * This method is meant to be used by the primary extension in order to update
   * the address bar.
   */
  public void documentRendering(NavigatorWindowEvent event);

  /**
   * Invoked as a document loads in order to inform the listener of progress.
   * <p>
   * This method is meant to be used by the primary extension in order to update
   * its progress bar. It is invoked outside the GUI dispatch thread.
   */
  public void progressUpdated(NavigatorProgressEvent event);

  /**
   * Invoked when the status message is updated.
   */
  public void statusUpdated(NavigatorWindowEvent event);

  /**
   * Invoked when the default status message is updated.
   */
  public void defaultStatusUpdated(NavigatorWindowEvent event);
}
