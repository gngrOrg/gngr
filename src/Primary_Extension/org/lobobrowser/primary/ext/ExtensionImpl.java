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
package org.lobobrowser.primary.ext;

import java.awt.Component;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lobobrowser.clientlet.ClientletException;
import org.lobobrowser.clientlet.ClientletResponse;
import org.lobobrowser.clientlet.ComponentContent;
import org.lobobrowser.clientlet.JavaVersionException;
import org.lobobrowser.clientlet.NavigatorVersionException;
import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.gui.HtmlPanel;
import org.lobobrowser.js.JavaScript;
import org.lobobrowser.main.PlatformInit;
import org.lobobrowser.primary.clientlets.PrimaryClientletSelector;
import org.lobobrowser.primary.clientlets.html.HtmlContent;
import org.lobobrowser.primary.clientlets.html.HtmlRendererContextImpl;
import org.lobobrowser.request.UserAgentImpl;
import org.lobobrowser.ua.NavigationEntry;
import org.lobobrowser.ua.NavigatorErrorListener;
import org.lobobrowser.ua.NavigatorExceptionEvent;
import org.lobobrowser.ua.NavigatorExtension;
import org.lobobrowser.ua.NavigatorExtensionContext;
import org.lobobrowser.ua.NavigatorFrame;
import org.lobobrowser.ua.NavigatorWindow;
import org.lobobrowser.util.Html;
import org.lobobrowser.util.Strings;
import org.w3c.dom.html.HTMLDocument;

public class ExtensionImpl implements NavigatorExtension {
  private static final Logger logger = Logger.getLogger(ExtensionImpl.class.getName());

  public void destroy() {
  }

  public void init(final NavigatorExtensionContext pcontext) {
    pcontext.addURLStreamHandlerFactory(new PrimaryStreamHandlerFactory());
    pcontext.addClientletSelector(new PrimaryClientletSelector());
    pcontext.addNavigatorErrorListener(new NavigatorErrorListener() {
      public void errorOcurred(final NavigatorExceptionEvent event) {
        showError(event.getNavigatorFrame(), event.getResponse(), event.getException());
      }
    });
    JavaScript.init();
  }

  public void windowClosing(final NavigatorWindow wcontext) {
    NavigationHistory.getInstance().save();
  }

  public void windowOpening(final NavigatorWindow wcontext) {
    final ComponentSource cs = new ComponentSource(wcontext);
    final Component[] abc = cs.getAddressBarComponents();
    for (final Component c : abc) {
      wcontext.addAddressBarComponent(c);
    }
    final Component[] sbc = cs.getStatusBarComponents();
    for (final Component c : sbc) {
      wcontext.addStatusBarComponent(c);
    }
    wcontext.addMenu("lobo.file", cs.getFileMenu());
    wcontext.addMenu("lobo.edit", cs.getEditMenu());
    wcontext.addMenu("lobo.view", cs.getViewMenu());
    wcontext.addMenu("lobo.navigation", cs.getNavigationMenu());
    wcontext.addMenu("lobo.bookmarks", cs.getBookmarksMenu());
    wcontext.addMenu("lobo.directory", cs.getDirectoryMenu());
    wcontext.addMenu("lobo.page.services", cs.getPageServicesMenu());
    wcontext.addMenu("lobo.tools", cs.getToolsMenu());
    // wcontext.addMenu("lobo.extensions", cs.getExtensionsMenu());
    wcontext.addMenu("lobo.help", cs.getHelpMenu());
    wcontext.addNavigatorWindowListener(cs);
    final NavigationEntry firstEntry = wcontext.getCurrentNavigationEntry();
    cs.setNavigationEntry(firstEntry);
  }

  public static void showError(final NavigatorFrame frame, final ClientletResponse response, final Throwable exception) {
    if (logger.isLoggable(Level.WARNING)) {
      logger.log(Level.WARNING,
          "showError(): An error occurred trying to process document " + (response == null ? "[null]" : response.getResponseURL()),
          exception);
    }
    final ComponentContent errorComponent = getErrorComponent(frame, response, exception);
    frame.replaceContent(response, errorComponent);
    // TODO: Get window or something, and ensure current URL is shown.
  }

  private static ComponentContent getErrorComponent(final NavigatorFrame frame, final ClientletResponse response, final Throwable exception) {
    final HtmlPanel panel = new HtmlPanel();
    final HtmlRendererContext rcontext = HtmlRendererContextImpl.getHtmlRendererContext(frame);
    panel.setHtml(getErrorHtml(response, exception), "about:error", rcontext);
    String sourceCode = "[NOT AVAILABLE]";
    if (exception instanceof ClientletException) {
      final ClientletException ce = (ClientletException) exception;
      final String sc = ce.getSourceCode();
      if (sc != null) {
        sourceCode = sc;
      }
    }
    return new HtmlContent((HTMLDocument) panel.getRootNode(), panel, sourceCode);
  }

  private static Throwable getRootCause(Throwable t) {
    while (t.getCause() != null) {
      t = t.getCause();
    }
    return t;
  }

  static String getErrorHtml(final ClientletResponse response, final Throwable exception) {
    final java.net.URL url = response == null ? null : response.getResponseURL();
    final String method = response == null ? null : response.getLastRequestMethod();
    final Writer swriter = new StringWriter();
    final PrintWriter writer = new PrintWriter(swriter);
    writer.println("<html><body>");
    writer.println("<dl style='background-color: #FFE0E0; border: 1px solid red; padding: 1em;'>");
    writer.println("  <big>An <strong>error</strong> occurred trying to process a request.</big>");
    writer.println("  <br>");
    if (url != null) {
      writer.println("  <h3>URL:</h3>");
      writer.println("  <dd>" + getErrorUrlText(url, method) + "</dd>");
    }
    writer.println("  <h3>Exception:</h3>");
    writer.println("  <p>" + exception.getClass().getName() + "</p>");
    writer.println("  <h3>Meaning</h3>");
    writer.println("  <div>" + getExceptionMeaning(url, exception) + "</div>");
    writer.println("  <h3>Message:</h3>");
    writer.println("  <div>" + Html.textToHTML(exception.getMessage()) + "</div>");
    writer.println("</dl>");
    writer.println("<p></p>");

    if (PlatformInit.getInstance().debugOn) {
      writer.println("<table border='1' width='100%' style='background-color: #E0E0FF; bolder: solid red 2px;'>");
      writer.println("  <tr><th style='padding:.2em'>Details</th></tr>");
      writer.println("  <tr><td style='padding:.2em'>");

      final StringWriter sw = new StringWriter();
      final PrintWriter pw = new PrintWriter(sw);
      exception.printStackTrace(pw);
      pw.flush();

      writer.println(Html.textToHTML(sw.toString()));

      if (exception.getCause() != null) {
        final Throwable rootCause = getRootCause(exception);
        final StringWriter sw2 = new StringWriter();
        final PrintWriter pw2 = new PrintWriter(sw2);
        rootCause.printStackTrace(pw2);
        pw2.flush();
        writer.println("<p><strong>Root Cause</strong></p>");
        writer.println(Html.textToHTML(sw2.toString()));
      }

      writer.println("  </td></tr>");
      writer.println("</table>");
    }

    writer.println("</body><html>");
    writer.flush();
    return swriter.toString();
  }

  private static String getErrorUrlText(final java.net.URL url, final String method) {
    final StringBuffer buf = new StringBuffer();
    final boolean isGet = "GET".equals(method);
    if (isGet) {
      buf.append("<a href=\"" + url.toExternalForm() + "\">");
    }
    buf.append(Strings.truncate(url.toExternalForm(), 80));
    if (isGet) {
      buf.append("</a>");
    }
    return buf.toString();
  }

  private static String getExceptionMeaning(final java.net.URL url, final Throwable exception) {
    if (exception instanceof org.lobobrowser.clientlet.JavaVersionException) {
      final JavaVersionException jve = (JavaVersionException) exception;
      return "This exception is thrown when the content expects the user's Java Virtual Machine "
          + "to be more up to date than it currently is. In this case the content is " + "expecting version " + jve.getExpectingVersion()
          + " whereas the version running " + "the browser is " + System.getProperty("java.version") + ".";
    } else if (exception instanceof NavigatorVersionException) {
      final NavigatorVersionException nve = (NavigatorVersionException) exception;
      return "This exception is thrown when the content expects the browser version "
          + "to be more up to date than it currently is. In this case the content is " + "expecting version " + nve.getExpectingVersion()
          + " whereas the user agent " + "version is " + UserAgentImpl.getInstance().getVersion() + ".";
    } else {
      Throwable cause = exception;
      if (exception instanceof ClientletException) {
        cause = ((ClientletException) exception).getCause();
        if (cause == null) {
          // oops
          cause = exception;
        }
      } else if (exception instanceof java.lang.reflect.InvocationTargetException) {
        cause = ((java.lang.reflect.InvocationTargetException) exception).getCause();
      }
      if (cause instanceof java.net.MalformedURLException) {
        return "A URL or URI was not formatted correctly.";
      } else if (cause instanceof javax.net.ssl.SSLHandshakeException) {
        return "<p>This is most likely caused due to a JVM with crippled cipher suites.</p>" +
               "<p>We are actively working on this. Please see https://github.com/UprootLabs/gngr/wiki/SSL-Handshake-Failures</p>";
      } else if (cause instanceof java.net.UnknownHostException) {
        return "The host named '" + ((java.net.UnknownHostException) cause).getMessage()
            + "' could not be found by the Domain Name Service (DNS).";
      } else if (cause instanceof java.lang.SecurityException) {
        return "An attempted security violation has been detected and stopped.";
      } else if (cause instanceof java.net.ProtocolException) {
        return "Indicates there is an error in the underlying communications protocol.";
      } else if (cause instanceof java.net.SocketTimeoutException) {
        return "It means the server accepted the connection, but failed to respond after a long period of time. This is usually indicative of a server problem.";
      } else if (cause instanceof java.net.ConnectException) {
        return "It means a connection to the server could not be obtained. Typically, the server has refused the connection, i.e. it's not accepting connections on a given port.";
      } else if (cause instanceof java.net.SocketException) {
        return "Indicates there was an error in the underlying protocol, e.g. TCP/IP.";
      } else if (cause instanceof java.io.IOException) {
        return "Indicates an Input/Output error has occurred. This is typically due "
            + "to a network connection that cannot be establised or one that has failed, "
            + "but it can also mean that a file could not be accessed or found.";
      } else if ((cause instanceof java.lang.NullPointerException) || (cause instanceof java.lang.ClassCastException)) {
        return "This is a common Java exception that generally occurs due to a programming error. "
            + "The stack trace will show if the error is in browser code, an extension or the document itself.";
      } else if (cause instanceof ClientletException) {
        return "A " + ClientletException.class.getSimpleName()
            + " is thrown by extensions or documents typically to indicate an unexpected state has been encountered.";
      } else {
        return "Unknown.";
      }
    }
  }
}
