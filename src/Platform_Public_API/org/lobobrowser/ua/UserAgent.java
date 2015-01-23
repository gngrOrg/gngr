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

/**
 * Represents the user agent, browser or clientlet engine.
 *
 * @author J. H. S.
 */
public interface UserAgent {
  /**
   * Gets the user agent name.
   */
  public String getName();

  /**
   * Gets the user agent version.
   */
  public String getVersion();

  // /**
  // * Gets the earliest XAMJ version this user agent
  // * implements as a subset. (Supersets and other
  // * non-subsets of XAMJ versions are not allowed.)
  // */
  // public String getXamjVersion();

  /**
   * Gets the user agent name followed by the version.
   */
  public String getNameAndVersion();

  /**
   * Optional URL with information about the user agent.
   */
  public String getInfoUrl();

  /**
   * Gets the value the User-Agent header should be set to.
   */
  public String getUserAgentString();

  // (unavalilable for now)
  // /**
  // * Gets an ID that is nearly guaranteed to be globally unique
  // * for a browser session and a protocol-host pair. The session ID
  // * should be practically impossible to guess, even if the algorithm
  // * to produce it is well known. The session ID does not persist
  // * if the browser exits.
  // */
  // public String getSessionID(java.net.URL url);
}
