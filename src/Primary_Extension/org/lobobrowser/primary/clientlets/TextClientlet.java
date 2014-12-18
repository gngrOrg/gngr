/*
    GNU GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The Lobo Project

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public
    License as published by the Free Software Foundation; either
    verion 2 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Contact info: lobochief@users.sourceforge.net
 */
/*
 * Created on Jul 10, 2005
 */
package org.lobobrowser.primary.clientlets;

import java.io.IOException;
import java.io.InputStream;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.lobobrowser.clientlet.Clientlet;
import org.lobobrowser.clientlet.ClientletContext;
import org.lobobrowser.clientlet.ClientletException;
import org.lobobrowser.util.io.IORoutines;

public final class TextClientlet implements Clientlet {
  public TextClientlet() {
  }

  public void process(final ClientletContext context) throws ClientletException {
    try {
      final InputStream in = context.getResponse().getInputStream();
      try {
        final String text = IORoutines.loadAsText(in, "ISO-8859-1");
        final JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        final JScrollPane pane = new JScrollPane(textArea);
        context.setResultingContent(pane, context.getResponse().getResponseURL());
      } finally {
        in.close();
      }
    } catch (final IOException ioe) {
      throw new ClientletException(ioe);
    }
  }
}
