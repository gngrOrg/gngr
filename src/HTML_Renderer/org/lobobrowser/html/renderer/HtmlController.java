package org.lobobrowser.html.renderer;

import java.awt.Cursor;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import org.lobobrowser.html.FormInput;
import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.domimpl.HTMLAbstractUIElement;
import org.lobobrowser.html.domimpl.HTMLButtonElementImpl;
import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.domimpl.HTMLInputElementImpl;
import org.lobobrowser.html.domimpl.HTMLLinkElementImpl;
import org.lobobrowser.html.domimpl.HTMLSelectElementImpl;
import org.lobobrowser.html.domimpl.ModelNode;
import org.lobobrowser.html.domimpl.NodeImpl;
import org.lobobrowser.html.js.Event;
import org.lobobrowser.html.style.RenderState;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class HtmlController {
  private static final Logger logger = Logger.getLogger(HtmlController.class.getName());
  private static final HtmlController instance = new HtmlController();

  public static HtmlController getInstance() {
    return instance;
  }

  public void onEnterPressed(final ModelNode node, final InputEvent event) {
    if (node instanceof HTMLInputElementImpl) {
      final HTMLInputElementImpl hie = (HTMLInputElementImpl) node;
      if (hie.isSubmittableWithEnterKey()) {
        hie.submitForm(null);
      }
    }
  }

  private static NodeImpl findEventTarget(final ModelNode node) {
    return findEventTarget(node, null);
  }

  private static NodeImpl findEventTarget(final ModelNode node, final ModelNode stopNode) {
    for (ModelNode curr = node; (curr != null) && (curr != stopNode); curr = curr.getParentModelNode()) {
      if ((curr instanceof NodeImpl) && ((curr instanceof Element) || (curr instanceof Document))) {
        return (NodeImpl) curr;
      }
    }
    return null;
  }

  private static void runOnEDT(final Runnable action) {
    if (SwingUtilities.isEventDispatchThread()) {
      action.run();
    } else {
      SwingUtilities.invokeLater(action);
    }
  }

  private static boolean isSameDocument(final NodeImpl target) {
    final Document ownerDoc = target.getOwnerDocument();
    if (ownerDoc instanceof HTMLDocumentImpl) {
      return ((HTMLDocumentImpl) ownerDoc).getWindow().getDocumentNode() == ownerDoc;
    }
    return true;
  }

  public void onMouseClick(final ModelNode node, final MouseEvent event, final int x, final int y) {
    if (node == null) {
      return;
    }
    if (logger.isLoggable(Level.INFO)) {
      logger.info("onMouseClick(): node=" + node + ",class=" + node.getClass().getName());
    }
    final NodeImpl target = findEventTarget(node);
    if (target == null) {
      return;
    }
    final Runnable defaultAction = () -> {
      for (ModelNode curr = target; curr != null; curr = curr.getParentModelNode()) {
        if (curr instanceof HTMLAbstractUIElement) {
          final HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) curr;
          final HtmlRendererContext rcontext = uiElement.getHtmlRendererContext();
          if ((rcontext != null) && !rcontext.onMouseClick(uiElement, event)) {
            return;
          }
        }
        if (curr instanceof HTMLLinkElementImpl) {
          if (((HTMLLinkElementImpl) curr).navigate()) {
            return;
          }
        } else if (curr instanceof HTMLButtonElementImpl) {
          final HTMLButtonElementImpl button = (HTMLButtonElementImpl) curr;
          final String rawType = button.getAttribute("type");
          final String type = rawType == null ? "submit" : rawType.trim().toLowerCase();
          if ("submit".equals(type)) {
            final String name = button.getName();
            final FormInput[] formInputs = name == null ? null : new FormInput[] { new FormInput(name, button.getValue()) };
            button.submitForm(formInputs);
            return;
          } else if ("reset".equals(type)) {
            runOnEDT(() -> {
              if (isSameDocument(target)) {
                button.resetForm();
              }
            });
            return;
          } else if ("button".equals(type)) {
            System.out.println("Button TODO;");
          }
        }
      }
    };
    target.dispatchEvent(new Event("click", target, event, x, y), defaultAction);
  }

  public void onMiddleClick(final ModelNode node, final MouseEvent event, final int x, final int y) {
    for (ModelNode curr = node; curr != null; curr = curr.getParentModelNode()) {
      if (curr instanceof HTMLAbstractUIElement) {
        final HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) curr;
        final HtmlRendererContext rcontext = uiElement.getHtmlRendererContext();
        if (rcontext != null) {
          // Needs to be done after Javascript, so the script
          // is able to prevent it.
          if (!rcontext.onMiddleClick(uiElement, event)) {
            return;
          }
        }
      }
    }
  }

  public void onContextMenu(final ModelNode node, final MouseEvent event, final int x, final int y) {
    if (node == null) {
      return;
    }
    if (logger.isLoggable(Level.INFO)) {
      logger.info("onContextMenu(): node=" + node + ",class=" + node.getClass().getName());
    }
    final NodeImpl target = findEventTarget(node);
    if (target == null) {
      return;
    }
    final Runnable defaultAction = () -> runOnEDT(() -> {
      if (!isSameDocument(target)) {
        return;
      }
      for (ModelNode curr = target; curr != null; curr = curr.getParentModelNode()) {
        if (curr instanceof HTMLAbstractUIElement) {
          final HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) curr;
          final HtmlRendererContext rcontext = uiElement.getHtmlRendererContext();
          if ((rcontext != null) && !rcontext.onContextMenu(uiElement, event)) {
            return;
          }
        }
      }
    });
    target.dispatchEvent(new Event("contextmenu", target, event, x, y), defaultAction);
  }

  public void onMouseOver(final BoundableRenderable renderable, final ModelNode nodeStart, final MouseEvent event, final int x,
      final int y, final ModelNode limit) {
    if (nodeStart == null) {
      return;
    }
    {
      ModelNode node = nodeStart;
      while (node != null) {
        if (node == limit) {
          break;
        }
        if (node instanceof HTMLAbstractUIElement) {
          final HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) node;
          uiElement.setMouseOver(true);
          final HtmlRendererContext rcontext = uiElement.getHtmlRendererContext();
          if (rcontext != null) {
            rcontext.onMouseOver(uiElement, event);
          }
        }
        node = node.getParentModelNode();
      }
    }

    setMouseOnMouseOver(renderable, nodeStart, limit);

    final NodeImpl target = findEventTarget(nodeStart, limit);
    if (target != null) {
      target.dispatchEvent(new Event("mouseover", target, event, x, y), limit, null);
    }
  }

  private static void setMouseOnMouseOver(final BoundableRenderable renderable, final ModelNode nodeStart, final ModelNode limit) {
    ModelNode node = nodeStart;
    while (node != null) {
      if (node == limit) {
        break;
      }
      if (node instanceof NodeImpl) {
        final NodeImpl uiElement = (NodeImpl) node;
        final HtmlRendererContext rcontext = uiElement.getHtmlRendererContext();
        final RenderState rs = uiElement.getRenderState();
        final Optional<Cursor> cursorOpt = rs.getCursor();
        if (rcontext != null) {
          if (cursorOpt.isPresent()) {
            rcontext.setCursor(cursorOpt);
            break;
          } else {
            if (node.getParentModelNode() == limit) {
              if ((renderable instanceof RWord) || (renderable instanceof RBlank)) {
                rcontext.setCursor(Optional.of(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR)));
              }
            }
          }
        }
      }
      node = node.getParentModelNode();
    }
  }

  public void onMouseOut(final ModelNode nodeStart, final MouseEvent event, final int x, final int y, final ModelNode limit) {
    if (nodeStart == null) {
      return;
    }
    {
      ModelNode node = nodeStart;
      while (node != null) {
        if (node == limit) {
          break;
        }
        if (node instanceof HTMLAbstractUIElement) {
          final HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) node;
          uiElement.setMouseOver(false);
          final HtmlRendererContext rcontext = uiElement.getHtmlRendererContext();
          if (rcontext != null) {
            rcontext.onMouseOut(uiElement, event);
          }

        }
        node = node.getParentModelNode();
      }
    }

    resetCursorOnMouseOut(nodeStart, limit);

    final NodeImpl target = findEventTarget(nodeStart, limit);
    if (target != null) {
      target.dispatchEvent(new Event("mouseout", target, event, x, y), limit, null);
    }
  }

  private static void resetCursorOnMouseOut(final ModelNode nodeStart, final ModelNode limit) {
    Optional<Cursor> foundCursorOpt = Optional.empty();
    ModelNode node = limit;
    while (node != null) {
      if (node instanceof NodeImpl) {
        final NodeImpl uiElement = (NodeImpl) node;

        final RenderState rs = uiElement.getRenderState();
        final Optional<Cursor> cursorOpt = rs.getCursor();
        foundCursorOpt = cursorOpt;
        if (cursorOpt.isPresent()) {
          break;
        }

      }
      node = node.getParentModelNode();
    }

    if (nodeStart instanceof NodeImpl) {
      final NodeImpl uiElement = (NodeImpl) nodeStart;
      final HtmlRendererContext rcontext = uiElement.getHtmlRendererContext();
      // rcontext.setCursor(Optional.empty());
      if (rcontext != null) {
        rcontext.setCursor(foundCursorOpt);
      }
    }
  }

  public void onDoubleClick(final ModelNode node, final MouseEvent event, final int x, final int y) {
    if (node == null) {
      return;
    }
    if (logger.isLoggable(Level.INFO)) {
      logger.info("onDoubleClick(): node=" + node + ",class=" + node.getClass().getName());
    }
    final NodeImpl target = findEventTarget(node);
    if (target == null) {
      return;
    }
    final Runnable defaultAction = () -> {
      for (ModelNode curr = target; curr != null; curr = curr.getParentModelNode()) {
        if (curr instanceof HTMLAbstractUIElement) {
          final HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) curr;
          final HtmlRendererContext rcontext = uiElement.getHtmlRendererContext();
          if ((rcontext != null) && !rcontext.onDoubleClick(uiElement, event)) {
            return;
          }
        }
      }
    };
    target.dispatchEvent(new Event("dblclick", target, event, x, y), defaultAction);
  }

  public void onMouseDisarmed(final ModelNode node, final MouseEvent event) {
    for (ModelNode curr = node; curr != null; curr = curr.getParentModelNode()) {
      if (curr instanceof HTMLLinkElementImpl) {
        ((HTMLLinkElementImpl) curr).getCurrentStyle().setOverlayColor(null);
        return;
      }
    }
  }

  public void onMouseDown(final ModelNode node, final MouseEvent event, final int x, final int y) {
    if (node == null) {
      return;
    }
    for (ModelNode curr = node; curr != null; curr = curr.getParentModelNode()) {
      if (curr instanceof HTMLLinkElementImpl) {
        ((HTMLLinkElementImpl) curr).getCurrentStyle().setOverlayColor("#9090FF80");
        break;
      }
    }
    final NodeImpl target = findEventTarget(node);
    if (target == null) {
      return;
    }
    target.dispatchEvent(new Event("mousedown", target, event, x, y), null);
  }

  public void onMouseUp(final ModelNode node, final MouseEvent event, final int x, final int y) {
    if (node == null) {
      return;
    }
    for (ModelNode curr = node; curr != null; curr = curr.getParentModelNode()) {
      if (curr instanceof HTMLLinkElementImpl) {
        ((HTMLLinkElementImpl) curr).getCurrentStyle().setOverlayColor(null);
        break;
      }
    }
    final NodeImpl target = findEventTarget(node);
    if (target == null) {
      return;
    }
    target.dispatchEvent(new Event("mouseup", target, event, x, y), null);
  }

  /**
   * @param node
   *          The node generating the event.
   * @param x
   *          For images only, x coordinate of mouse click.
   * @param y
   *          For images only, y coordinate of mouse click.
   */
  public void onPressed(final ModelNode node, final InputEvent event, final int x, final int y) {
    final NodeImpl target = findEventTarget(node);
    final Runnable defaultAction;
    if (node instanceof HTMLInputElementImpl) {
      final HTMLInputElementImpl hie = (HTMLInputElementImpl) node;
      if (hie.isSubmitInput()) {
        final String name = hie.getName();
        final FormInput[] formInputs = name == null ? null : new FormInput[] { new FormInput(name, hie.getValue()) };
        defaultAction = () -> hie.submitForm(formInputs);
      } else if (hie.isImageInput()) {
        final String name = hie.getName();
        final String prefix = name == null ? "" : name + ".";
        final FormInput[] extraFormInputs = new FormInput[] { new FormInput(prefix + "x", String.valueOf(x)),
            new FormInput(prefix + "y", String.valueOf(y)) };
        defaultAction = () -> hie.submitForm(extraFormInputs);
      } else if (hie.isResetInput()) {
        defaultAction = () -> runOnEDT(() -> {
          if ((target == null) || isSameDocument(target)) {
            hie.resetForm();
          }
        });
      } else {
        defaultAction = null;
      }
    } else {
      defaultAction = null;
    }

    if (target != null) {
      target.dispatchEvent(new Event("click", target, event, x, y), defaultAction);
    } else if (defaultAction != null) {
      defaultAction.run();
    }
  }

  public void onChange(final ModelNode node) {
    if (node instanceof HTMLSelectElementImpl) {
      final HTMLSelectElementImpl selectElement = (HTMLSelectElementImpl) node;
      selectElement.dispatchEvent(new Event("change", selectElement), null);
    }
  }

  public boolean onKeyUp(final ModelNode node, final KeyEvent ke) {
    boolean pass = true;
    if (node instanceof NodeImpl) {
      final NodeImpl uiElement = (NodeImpl) node;
      final Event jsEvent = new Event("keyup", uiElement, ke);
      pass = uiElement.dispatchEvent(jsEvent);
      System.out.println("Dispatch result: " + pass);
    }
    return pass;
  }
}
