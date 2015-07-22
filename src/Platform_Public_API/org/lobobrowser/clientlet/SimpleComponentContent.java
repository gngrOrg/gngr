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

public abstract class SimpleComponentContent extends AbstractComponentContent {
  private final Component component;
  private final String title;
  private final String sourceCode;

  public SimpleComponentContent(final Component component, final String title, final String sourceCode) {
    this.component = component;
    this.title = title;
    this.sourceCode = sourceCode;
  }

  public SimpleComponentContent(final Component component) {
    this.component = component;
    this.title = component.toString();
    this.sourceCode = null;
  }

  @Override
  public boolean canCopy() {
    return false;
  }

  @Override
  public boolean copy() {
    return false;
  }

  @Override
  public Component getComponent() {
    return this.component;
  }

  @Override
  public String getSourceCode() {
    return this.sourceCode;
  }

  @Override
  public String getTitle() {
    return this.title;
  }

}
