/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The Lobo Project

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Contact info: lobochief@users.sourceforge.net
 */
package org.lobobrowser.html.js;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.domimpl.NodeImpl;
import org.lobobrowser.js.JavaScript;
import org.lobobrowser.ua.UserAgentContext;
import org.lobobrowser.ua.UserAgentContext.Request;
import org.lobobrowser.ua.UserAgentContext.RequestKind;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;

public class Executor {
  private static final Logger logger = Logger.getLogger(Executor.class.getName());
  private static final PolicySecurityController policySecurityController = new PolicySecurityController();

  /**
   * This method should be invoked instead of <code>Context.enter</code>.
   *
   * @param codeSource
   * @param ucontext
   */
  public static Context createContext(final java.net.URL codeSource, final UserAgentContext ucontext, final ContextFactory factory) {
    final Context prev = Context.getCurrentContext();

    // final Context ctx = Context.enter();
    final Context ctx = factory.enterContext();

    if (!ctx.isSealed()) {
      ctx.setOptimizationLevel(ucontext.getScriptingOptimizationLevel());
      if (prev == null) {
        // If there was a previous context, this one must be nested.
        // We still need to create a context because of exit() but
        // we cannot set a new security controller.

        // ctx.setSecurityController(new SecurityControllerImpl(codeSource, ucontext.getSecurityPolicy()));
        ctx.setSecurityController(policySecurityController);
      }
      // Sealing is recommended for untrusted scripts
      ctx.seal(null);
    }
    return ctx;
  }

  public static boolean executeFunction(final NodeImpl element, final Function f, final Object event, final ContextFactory contextFactory) {
    return Executor.executeFunction(element, element, f, event, contextFactory);
  }

  private static boolean executeFunction(final NodeImpl element, final Object thisObject, final Function f, final Object event,
      final ContextFactory contextFactory) {
    final Document doc = element.getOwnerDocument();
    if (doc == null) {
      throw new IllegalStateException("Element does not belong to a document.");
    }

    final UserAgentContext uaContext = element.getUserAgentContext();
    if (uaContext.isRequestPermitted(new Request(element.getDocumentURL(), RequestKind.JavaScript))) {
      final Context ctx = createContext(element.getDocumentURL(), element.getUserAgentContext(), contextFactory);
      // ctx.setGenerateObserverCount(true);
      try {
        final Scriptable scope = ((HTMLDocumentImpl) doc).getWindow().getWindowScope();
        if (scope == null) {
          throw new IllegalStateException("Scriptable (scope) instance is null");
        }
        final JavaScript js = JavaScript.getInstance();
        final Scriptable thisScope = (Scriptable) js.getJavascriptObject(thisObject, scope);
        try {
          // final Scriptable eventScriptable = (Scriptable) js.getJavascriptObject(event, thisScope);
          final Object eventScriptable = js.getJavascriptObject(event, thisScope);
          scope.put("event", thisScope, eventScriptable);
          // ScriptableObject.defineProperty(thisScope, "event",
          // eventScriptable,
          // ScriptableObject.READONLY);
          final Object result = f.call(ctx, thisScope, thisScope, new Object[] { eventScriptable });
          if (!(result instanceof Boolean)) {
            return true;
          }
          return ((Boolean) result).booleanValue();
        } catch (final Exception thrown) {
          logJSException(thrown);
          return true;
        }
      } finally {
        Context.exit();
      }
    } else {
      // TODO: Should this be true? I am copying the return from the exception clause above.
      System.out.println("Rejected request to execute script");
      return true;
    }

  }

  public static void logJSException(final Throwable err) {
    logger.log(Level.WARNING, "Unable to evaluate Javascript code", err);
    if (err instanceof RhinoException) {
      final RhinoException rhinoException = (RhinoException) err;
      logger.log(Level.WARNING, "JS Error: " + rhinoException.details() + "\nJS Stack:\n" + rhinoException.getScriptStackTrace());
    }
  }

  public static boolean executeFunction(final Scriptable thisScope, final Function f, final java.net.URL codeSource,
      final UserAgentContext ucontext, final ContextFactory contextFactory) {
    final Context ctx = createContext(codeSource, ucontext, contextFactory);
    try {
      try {
        final Object result = f.call(ctx, thisScope, thisScope, new Object[0]);
        if (!(result instanceof Boolean)) {
          return true;
        }
        return ((Boolean) result).booleanValue();
      } catch (final Exception err) {
        logJSException(err);
        return true;
      }
    } finally {
      Context.exit();
    }
  }

}
