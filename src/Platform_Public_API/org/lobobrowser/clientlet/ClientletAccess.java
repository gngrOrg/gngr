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

/**
 * Provides utility methods to access the current clientlet context.
 */
public class ClientletAccess {
  private static final ThreadLocal<ClientletContext> currentClientletContext = new ThreadLocal<>();

  private ClientletAccess() {
  }

  /**
   * Gets the {@link ClientletContext} of the current thread, if any.
   */
  public static ClientletContext getCurrentClientletContext() {
    final ClientletContext ctx = currentClientletContext.get();
    if (ctx != null) {
      return ctx;
    } else {
      final ThreadGroup td = Thread.currentThread().getThreadGroup();
      if (td instanceof ClientletThreadGroup) {
        return ((ClientletThreadGroup) td).getClientletContext();
      } else {
        return null;
      }
    }
  }

  /**
   * This method should be invoked by the clientlet platform to publish the
   * {@link ClientletContext} of the current thread.
   *
   * @param context
   *          A {@link ClientletContext} instance.
   */
  public static void setCurrentClientletContext(final ClientletContext context) {
    currentClientletContext.set(context);
  }
}
