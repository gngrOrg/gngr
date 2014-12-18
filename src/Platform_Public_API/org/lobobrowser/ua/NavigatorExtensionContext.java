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
 * This interface gives extensions access to the platform.
 *
 * @see NavigatorExtension#init(NavigatorExtensionContext)
 */
public interface NavigatorExtensionContext {
  /**
   * Adds a clientlet selector. This is how platform extensions register
   * additional content handlers.
   */
  public void addClientletSelector(ClientletSelector selector);

  public void removeClientletSelector(ClientletSelector selector);

  /**
   * Adds an object that can view connections made by the browser and
   * potentially modify their headers and other data.
   *
   * @param processor
   *          An connection processor.
   */
  public void addConnectionProcessor(ConnectionProcessor processor);

  public void removeConnectionProcessor(ConnectionProcessor processor);

  /**
   * Adds a listener of navigator events.
   */
  public void addNavigatorErrorListener(NavigatorErrorListener listener);

  /**
   * Removes a listener of navigation events.
   */
  public void removeNavigatorErrorListener(NavigatorErrorListener listener);

  /**
   * Adds a global listener of navigation events.
   *
   * @param listener
   *          The listener.
   */
  public void addNavigationListener(NavigationListener listener);

  public void removeNavigationListener(NavigationListener listener);

  /**
   * Gets the {@link UserAgent} instance associated with this context.
   */
  public UserAgent getUserAgent();

  /**
   * Registers a URL stream handler factory which may be used to implement
   * custom protocols. Note that Java platform protocols (HTTP, HTTPS, etc.) or
   * protocols defined by extensions with higher priority cannot be overridden.
   * The factory must return <code>null</code> if it does not know how to handle
   * a particular protocol.
   *
   * @param factory
   *          An implementation of <code>java.net.URLStreamHandlerFactory</code>
   *          .
   */
  public void addURLStreamHandlerFactory(java.net.URLStreamHandlerFactory factory);
}
