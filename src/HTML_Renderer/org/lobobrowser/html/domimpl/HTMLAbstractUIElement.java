package org.lobobrowser.html.domimpl;

import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import org.lobobrowser.html.js.Executor;
import org.lobobrowser.html.js.Window;
import org.lobobrowser.js.HideFromJS;
import org.lobobrowser.js.JavaScript;
import org.lobobrowser.ua.UserAgentContext;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;

/**
 * Implements common functionality of most elements.
 */
public class HTMLAbstractUIElement extends HTMLElementImpl {
  private volatile Function onfocus, onblur, onclick, ondblclick, onmousedown, onmouseup, onmouseover, onmousemove, onmouseout, onkeypress,
      onkeydown, onkeyup, oncontextmenu;

  public HTMLAbstractUIElement(final String name) {
    super(name);
  }

  public Function getOnblur() {
    return this.getEventFunction(onblur, "onblur");
  }

  public void setOnblur(final Function onblur) {
    this.onblur = onblur;
  }

  public Function getOnclick() {
    return this.getEventFunction(onclick, "onclick");
  }

  public void setOnclick(final Function onclick) {
    this.onclick = onclick;
  }

  public Function getOndblclick() {
    return this.getEventFunction(ondblclick, "ondblclick");
  }

  public void setOndblclick(final Function ondblclick) {
    this.ondblclick = ondblclick;
  }

  public Function getOnfocus() {
    return this.getEventFunction(onfocus, "onfocus");
  }

  public void setOnfocus(final Function onfocus) {
    this.onfocus = onfocus;
  }

  public Function getOnkeydown() {
    return this.getEventFunction(onkeydown, "onkeydown");
  }

  public void setOnkeydown(final Function onkeydown) {
    this.onkeydown = onkeydown;
  }

  public Function getOnkeypress() {
    return this.getEventFunction(onkeypress, "onkeypress");
  }

  public void setOnkeypress(final Function onkeypress) {
    this.onkeypress = onkeypress;
  }

  public Function getOnkeyup() {
    return this.getEventFunction(onkeyup, "onkeyup");
  }

  public void setOnkeyup(final Function onkeyup) {
    this.onkeyup = onkeyup;
  }

  public Function getOnmousedown() {
    return this.getEventFunction(onmousedown, "onmousedown");
  }

  public void setOnmousedown(final Function onmousedown) {
    this.onmousedown = onmousedown;
  }

  public Function getOnmousemove() {
    return this.getEventFunction(onmousemove, "onmousemove");
  }

  public void setOnmousemove(final Function onmousemove) {
    this.onmousemove = onmousemove;
  }

  public Function getOnmouseout() {
    return this.getEventFunction(onmouseout, "onmouseout");
  }

  public void setOnmouseout(final Function onmouseout) {
    this.onmouseout = onmouseout;
  }

  public Function getOnmouseover() {
    return this.getEventFunction(onmouseover, "onmouseover");
  }

  public void setOnmouseover(final Function onmouseover) {
    this.onmouseover = onmouseover;
  }

  public Function getOnmouseup() {
    return this.getEventFunction(onmouseup, "onmouseup");
  }

  public void setOnmouseup(final Function onmouseup) {
    this.onmouseup = onmouseup;
  }

  public Function getOncontextmenu() {
    return this.getEventFunction(oncontextmenu, "oncontextmenu");
  }

  public void setOncontextmenu(final Function oncontextmenu) {
    this.oncontextmenu = oncontextmenu;
  }

  public void focus() {
    final UINode node = this.getUINode();
    if (node != null) {
      node.focus();
    }
  }

  public void blur() {
    final UINode node = this.getUINode();
    if (node != null) {
      node.blur();
    }
  }

  private Map<String, Function> functionByAttribute = null;

  /**
   * Checks whether an inline handler is set via JS property or non-empty HTML attribute
   * without entering Rhino or compiling the attribute code. Safe to call on the EDT.
   */
  @HideFromJS
  protected boolean hasEventFunction(final Function varValue, final String attributeName) {
    if (varValue != null) {
      return true;
    }
    final UserAgentContext uac = this.getUserAgentContext();
    if (uac == null || !uac.isScriptingEnabled()) {
      return false;
    }
    final String normalAttributeName = normalizeAttributeName(attributeName);
    synchronized (this) {
      final Map<String, Function> fba = this.functionByAttribute;
      if (fba != null && fba.containsKey(normalAttributeName)) {
        return fba.get(normalAttributeName) != null;
      }
    }
    final String attributeValue = this.getAttribute(attributeName);
    return (attributeValue != null) && (attributeValue.length() != 0);
  }

  @Override
  @HideFromJS
  public boolean hasInlineEventHandler(final String eventType) {
    if (eventType == null) {
      return false;
    }
    switch (eventType.toLowerCase(Locale.ROOT)) {
    case "click":
      return this.hasEventFunction(this.onclick, "onclick");
    case "dblclick":
      return this.hasEventFunction(this.ondblclick, "ondblclick");
    case "mousedown":
      return this.hasEventFunction(this.onmousedown, "onmousedown");
    case "mouseup":
      return this.hasEventFunction(this.onmouseup, "onmouseup");
    case "mouseover":
      return this.hasEventFunction(this.onmouseover, "onmouseover");
    case "mousemove":
      return this.hasEventFunction(this.onmousemove, "onmousemove");
    case "mouseout":
      return this.hasEventFunction(this.onmouseout, "onmouseout");
    case "contextmenu":
      return this.hasEventFunction(this.oncontextmenu, "oncontextmenu");
    case "focus":
      return this.hasEventFunction(this.onfocus, "onfocus");
    case "blur":
      return this.hasEventFunction(this.onblur, "onblur");
    case "keypress":
      return this.hasEventFunction(this.onkeypress, "onkeypress");
    case "keydown":
      return this.hasEventFunction(this.onkeydown, "onkeydown");
    case "keyup":
      return this.hasEventFunction(this.onkeyup, "onkeyup");
    default:
      return super.hasInlineEventHandler(eventType);
    }
  }

  @Override
  @HideFromJS
  public Function getInlineEventHandler(final String eventType) {
    if (eventType == null) {
      return null;
    }
    switch (eventType.toLowerCase(Locale.ROOT)) {
    case "click":
      return this.getOnclick();
    case "dblclick":
      return this.getOndblclick();
    case "mousedown":
      return this.getOnmousedown();
    case "mouseup":
      return this.getOnmouseup();
    case "mouseover":
      return this.getOnmouseover();
    case "mousemove":
      return this.getOnmousemove();
    case "mouseout":
      return this.getOnmouseout();
    case "contextmenu":
      return this.getOncontextmenu();
    case "focus":
      return this.getOnfocus();
    case "blur":
      return this.getOnblur();
    case "keypress":
      return this.getOnkeypress();
    case "keydown":
      return this.getOnkeydown();
    case "keyup":
      return this.getOnkeyup();
    default:
      return super.getInlineEventHandler(eventType);
    }
  }

  protected Function getEventFunction(final Function varValue, final String attributeName) {
    if (varValue != null) {
      return varValue;
    }
    final String normalAttributeName = normalizeAttributeName(attributeName);
    synchronized (this) {
      Map<String, Function> fba = this.functionByAttribute;
      Function f = fba == null ? null : fba.get(normalAttributeName);
      if (f != null) {
        return f;
      }
      final UserAgentContext uac = this.getUserAgentContext();
      if (uac == null) {
        throw new IllegalStateException("No user agent context.");
      }
      if (uac.isScriptingEnabled()) {
        final String attributeValue = this.getAttribute(attributeName);
        if ((attributeValue != null) && (attributeValue.length() != 0)) {
          final String functionCode = "function " + normalAttributeName + "_" + System.identityHashCode(this) + "() { " + attributeValue
              + " }";
          final Document doc = this.document;
          if (doc == null) {
            throw new IllegalStateException("Element does not belong to a document.");
          }
          final Window window = ((HTMLDocumentImpl) doc).getWindow();
          final Context ctx = Executor.createContext(this.getDocumentURL(), uac, window.getContextFactory());
          try {
            final Scriptable scope = window.getWindowScope();
            if (scope == null) {
              throw new IllegalStateException("Scriptable (scope) instance was null");
            }
            final Scriptable thisScope = (Scriptable) JavaScript.getInstance().getJavascriptObject(this, scope);
            try {
              // TODO: Get right line number for script. //TODO: Optimize this
              // in case it's called multiple times? Is that done?
              final CodeSource cs = new CodeSource(this.getDocumentURL(), (Certificate[]) null);
              f = ctx.compileFunction(thisScope, functionCode, this.getTagName() + "[" + this.getId() + "]." + attributeName, 1, cs);
            } catch (final EcmaError ecmaError) {
              logger.log(Level.WARNING, "Javascript error at " + ecmaError.sourceName() + ":" + ecmaError.lineNumber() + ": "
                  + ecmaError.getMessage(), ecmaError);
              f = null;
            } catch (final Exception err) {
              logger.log(Level.WARNING, "Unable to evaluate Javascript code", err);
              f = null;
            }
          } finally {
            Context.exit();
          }
        }
        if (fba == null) {
          fba = new HashMap<>(1);
          this.functionByAttribute = fba;
        }
        fba.put(normalAttributeName, f);
      }
      return f;
    }
  }

  @Override
  protected void handleAttributeChanged(String name, String oldValue, String newValue) {
    super.handleAttributeChanged(name, oldValue, newValue);
    if (name.startsWith("on")) {
      synchronized (this) {
        final Map<String, Function> fba = this.functionByAttribute;
        if (fba != null) {
          fba.remove(name);
        }
      }
    }
  }
}
