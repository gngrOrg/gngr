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
 * Exception thrown by clientlets.
 */
public class ClientletException extends Exception {
  private static final long serialVersionUID = -3172234813987721169L;
  private final String sourceCode;

  /**
   * Constructs a ClientletException.
   *
   * @param message
   *          The exception message.
   */
  public ClientletException(final String message) {
    super(message);
    this.sourceCode = null;
  }

  /**
   * Constructs a ClientletException.
   *
   * @param message
   *          The exception message.
   * @param sourceCode
   *          If the source code of the document generating the error is known,
   *          it should be passed in this parameter.
   */
  public ClientletException(final String message, final String sourceCode) {
    super(message);
    this.sourceCode = sourceCode;
  }

  /**
   * Constructs a ClientletException.
   *
   * @param message
   *          The exception message.
   * @param rootCause
   *          The root cause exception.
   */
  public ClientletException(final String message, final Throwable rootCause) {
    super(message, rootCause);
    this.sourceCode = null;
  }

  /**
   * Constructs a ClientletException.
   *
   * @param rootCause
   *          The root cause exception.
   */
  public ClientletException(final Throwable rootCause) {
    super(rootCause);
    this.sourceCode = null;
  }

  public String getSourceCode() {
    return sourceCode;
  }
}
