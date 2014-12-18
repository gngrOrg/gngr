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

import org.lobobrowser.clientlet.ClientletSelector;

/**
 * This interface must be implemented by a platform extension or plugin.
 */
public interface NavigatorExtension {
  /**
   * Invoked when the platform first loads the extension. At this point the
   * extension can register {@link ClientletSelector}s (extra content handlers)
   * by invoking
   * {@link NavigatorExtensionContext#addClientletSelector(ClientletSelector)}.
   *
   * @param pcontext
   *          The extension context. It provides extensions with access to
   *          browser functionality.
   */
  public void init(NavigatorExtensionContext pcontext);

  /**
   * Invoked right before the platform opens a new window. At this point the
   * extension can add custom widgets to the platform window.
   * <p>
   * Note that this method may not be invoked if the window does not require any
   * toolbars, status bars, address bars or menus.
   *
   * @param wcontext
   */
  public void windowOpening(NavigatorWindow wcontext);

  /**
   * Invoked when a window is about to close. At this point the extension can
   * perform cleanup operations that are window specific.
   *
   * @param wcontext
   */
  public void windowClosing(NavigatorWindow wcontext);

  /**
   * Invoked when the platform needs to unload the extension. This method should
   * release any resources used by the extension.
   */
  public void destroy();
}
