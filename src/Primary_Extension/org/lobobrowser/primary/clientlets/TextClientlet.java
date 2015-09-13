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
import org.lobobrowser.clientlet.SimpleComponentContent;
import org.lobobrowser.html.js.Window;
import org.lobobrowser.primary.clientlets.html.HtmlRendererContextImpl;
import org.lobobrowser.util.io.IORoutines;

public final class TextClientlet implements Clientlet {
  public TextClientlet() {
  }

  public void process(final ClientletContext context) throws ClientletException {
    System.out.println("Processing text client");
    try (
      final InputStream in = context.getResponse().getInputStream()) {
      final String text = IORoutines.loadAsText(in, "ISO-8859-1");
      final JTextArea textArea = new JTextArea(text);
      textArea.setEditable(false);
      final JScrollPane pane = new JScrollPane(textArea);
      final HtmlRendererContextImpl rcontext = HtmlRendererContextImpl.getHtmlRendererContext(context.getNavigatorFrame());
      rcontext.getHtmlPanel().setDocument(new SimpleDocument(context.getResponse().getMimeType()), rcontext);
      // context.setResultingContent(pane, context.getResponse().getResponseURL());
      context.setResultingContent(new SimpleComponentContent(pane) {

        @Override
        public void navigatedNotify() {
          System.out.println("Navigated");
          rcontext.jobsFinished();
          Window.getWindow(rcontext).jobsFinished();
        }
      });
    } catch (final IOException ioe) {
      throw new ClientletException(ioe);
    }
  }
}
