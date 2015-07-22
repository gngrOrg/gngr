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

import java.awt.Component;

/**
 * Abstract implementation of {@link ComponentContent}. It is recommended that
 * <code>ComponentContent</code> implementations extend this class for forward
 * compatibility.
 */
public abstract class AbstractComponentContent implements ComponentContent {
  public AbstractComponentContent() {
  }

  public boolean canCopy() {
    return false;
  }

  public boolean copy() {
    return false;
  }

  public abstract Component getComponent();

  public abstract String getSourceCode();

  public abstract String getTitle();

  public String getDescription() {
    return "";
  }

  public void addNotify() {
  }

  public void removeNotify() {
  }

  public Object getContentObject() {
    return null;
  }

  public String getMimeType() {
    return null;
  }

  public void setProperty(final String name, final Object value) {
    // NOP
  }

  // Backward compatibility note: Additional methods should provide an empty
  // body.
}
