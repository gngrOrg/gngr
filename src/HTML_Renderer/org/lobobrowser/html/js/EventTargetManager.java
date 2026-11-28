package org.lobobrowser.html.js;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.lobobrowser.html.domimpl.ModelNode;
import org.lobobrowser.html.domimpl.NodeImpl;
import org.lobobrowser.html.js.Window.JSRunnableTask;
import org.lobobrowser.ua.UserAgentContext;
import org.lobobrowser.ua.UserAgentContext.Request;
import org.lobobrowser.ua.UserAgentContext.RequestKind;
import org.mozilla.javascript.Function;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.events.EventException;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

public final class EventTargetManager {

  private final class JSEventTask extends JSRunnableTask {
    private final Event evt;
    private final List<NodeImpl> propagationPath;
    private final Document expectedDoc;
    private final Runnable defaultAction;

    private JSEventTask(final int priority, final String description, final List<NodeImpl> propagationPath, final Event evt,
        final Document expectedDoc, final Runnable defaultAction) {
      super(priority, description, () -> { });
      this.evt = evt;
      this.propagationPath = propagationPath;
      this.expectedDoc = expectedDoc;
      this.defaultAction = defaultAction;
    }

    @Override
    public boolean shouldExecute() {
      if (defaultAction != null) {
        return true;
      }
      final String type = evt.getType();
      return hasAnyEventListeners(propagationPath, type)
          || hasAnyJSFunctions(propagationPath, type)
          || hasAnyInlineHandlers(propagationPath, type);
    }

    @Override
    public void run() {
      if ((expectedDoc != null) && (window.getDocumentNode() != expectedDoc)) {
        return;
      }
      final String type = evt.getType();
      try {
        for (int i = 0; (i < propagationPath.size()) && !evt.isPropagationStopped(); i++) {
          if ((expectedDoc != null) && (window.getDocumentNode() != expectedDoc)) {
            return;
          }
          final NodeImpl currNode = propagationPath.get(i);
          evt.setPhase(i == 0 ? org.w3c.dom.events.Event.AT_TARGET : org.w3c.dom.events.Event.BUBBLING_PHASE);
          evt.setCurrentTarget(currNode instanceof EventTarget ? (EventTarget) currNode : null);
          if (!evt.isImmediatePropagationStopped() && currNode.hasInlineEventHandler(type)) {
            final Function inlineHandler = currNode.getInlineEventHandler(type);
            if (inlineHandler != null) {
              if (!Executor.executeFunction(currNode, inlineHandler, evt, window.getContextFactory())) {
                evt.preventDefault();
              }
            }
          }
          if (!evt.isImmediatePropagationStopped()) {
            dispatchEventToHandlers(currNode, evt);
          }
          if (!evt.isImmediatePropagationStopped()) {
            dispatchEventToJSHandlers(currNode, evt);
          }
        }
      } finally {
        evt.setCurrentTarget(null);
      }
      if ((defaultAction != null) && !evt.getDefaultPrevented()
          && ((expectedDoc == null) || (window.getDocumentNode() == expectedDoc))) {
        defaultAction.run();
      }
    }
  }

  private final Map<NodeImpl, Map<String, List<EventListener>>> nodeOnEventListeners = new IdentityHashMap<>();
  private final Window window;

  public EventTargetManager(final Window window) {
    this.window = window;
  }

  public synchronized void addEventListener(final NodeImpl node, final String type, final EventListener listener, final boolean useCapture) {
    final List<EventListener> handlerList = getListenerList(type, node, true);
    handlerList.add(listener);
  }

  private List<EventListener> getListenerList(final String type, final NodeImpl node, final boolean createIfNotExist) {
    final Map<String, List<EventListener>> onEventListeners = getEventListeners(node, createIfNotExist);

    if (onEventListeners != null) {
      if (onEventListeners.containsKey(type)) {
        return onEventListeners.get(type);
      } else if (createIfNotExist) {
        final List<EventListener> handlerList = new ArrayList<>();
        onEventListeners.put(type, handlerList);
        return handlerList;
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  private Map<String, List<EventListener>> getEventListeners(final NodeImpl node, final boolean createIfNotExist) {
    if (nodeOnEventListeners.containsKey(node)) {
      return nodeOnEventListeners.get(node);
    } else {
      if (createIfNotExist) {
        final Map<String, List<EventListener>> onEventListeners = new HashMap<>();
        nodeOnEventListeners.put(node, onEventListeners);
        return onEventListeners;
      } else {
        return null;
      }
    }
  }

  public synchronized void removeEventListener(final NodeImpl node, final String type, final EventListener listener,
      final boolean useCapture) {
    final Map<String, List<EventListener>> onEventListeners = getEventListeners(node, false);
    if (onEventListeners != null) {
      if (onEventListeners.containsKey(type)) {
        onEventListeners.get(type).remove(listener);
      }
    }

  }

  private List<Function> getFunctionList(final String type, final NodeImpl node, final boolean createIfNotExist) {
    final Map<String, List<Function>> onEventListeners = getEventFunctions(node, createIfNotExist);

    if (onEventListeners != null) {
      if (onEventListeners.containsKey(type)) {
        return onEventListeners.get(type);
      } else if (createIfNotExist) {
        final List<Function> handlerList = new ArrayList<>();
        onEventListeners.put(type, handlerList);
        return handlerList;
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  private Map<String, List<Function>> getEventFunctions(final NodeImpl node, final boolean createIfNotExist) {
    if (nodeOnEventFunctions.containsKey(node)) {
      return nodeOnEventFunctions.get(node);
    } else {
      if (createIfNotExist) {
        final Map<String, List<Function>> onEventListeners = new HashMap<>();
        nodeOnEventFunctions.put(node, onEventListeners);
        return onEventListeners;
      } else {
        return null;
      }
    }
  }

  private static boolean isScriptingPermitted(final NodeImpl node) {
    final UserAgentContext uac = node.getUserAgentContext();
    return (uac != null) && uac.isScriptingEnabled()
        && uac.isRequestPermitted(new Request(node.getDocumentURL(), RequestKind.JavaScript));
  }

  private synchronized boolean hasAnyEventListeners(final List<NodeImpl> path, final String type) {
    for (final NodeImpl currNode : path) {
      final List<EventListener> nodeHandlers = getListenerList(type, currNode, false);
      if ((nodeHandlers != null) && !nodeHandlers.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private synchronized boolean hasAnyJSFunctions(final List<NodeImpl> path, final String type) {
    for (final NodeImpl currNode : path) {
      final List<Function> nodeFunctions = getFunctionList(type, currNode, false);
      if ((nodeFunctions != null) && !nodeFunctions.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasAnyInlineHandlers(final List<NodeImpl> path, final String type) {
    for (final NodeImpl currNode : path) {
      if (currNode.hasInlineEventHandler(type)) {
        return true;
      }
    }
    return false;
  }

  private synchronized List<EventListener> snapshotEventListeners(final NodeImpl node, final String type) {
    final List<EventListener> handlers = getListenerList(type, node, false);
    return (handlers == null || handlers.isEmpty()) ? null : new ArrayList<>(handlers);
  }

  private synchronized boolean isListenerRegistered(final NodeImpl node, final String type, final EventListener listener) {
    final List<EventListener> handlers = getListenerList(type, node, false);
    return (handlers != null) && handlers.contains(listener);
  }

  private synchronized List<Function> snapshotJSFunctions(final NodeImpl node, final String type) {
    final List<Function> handlers = getFunctionList(type, node, false);
    return (handlers == null || handlers.isEmpty()) ? null : new ArrayList<>(handlers);
  }

  private synchronized boolean isFunctionRegistered(final NodeImpl node, final String type, final Function function) {
    final List<Function> handlers = getFunctionList(type, node, false);
    return (handlers != null) && handlers.contains(function);
  }

  public boolean dispatchEvent(final NodeImpl node, final Event evt) throws EventException {
    return dispatchEvent(node, evt, null, null);
  }

  public boolean dispatchEvent(final NodeImpl node, final Event evt, final ModelNode stopNode, final Runnable defaultAction)
      throws EventException {
    // TODO: get Window into the propagation path
    final List<NodeImpl> propagationPath = getPropagationPath(node, evt, stopNode);
    final String type = evt.getType();
    final boolean hasJavaListeners = hasAnyEventListeners(propagationPath, type);
    final boolean jsPermitted = isScriptingPermitted(node);
    final boolean hasJSHandlers = jsPermitted
        && (hasAnyJSFunctions(propagationPath, type) || hasAnyInlineHandlers(propagationPath, type));

    if (!hasJavaListeners && !hasJSHandlers) {
      if (defaultAction != null) {
        defaultAction.run();
      }
      return false;
    }

    // TODO: Capture phase, and distinction between target phase and bubbling phase
    evt.setPhase(org.w3c.dom.events.Event.AT_TARGET);
    final Document expectedDoc = node.getOwnerDocument();
    // TODO: The JS Task should be added with the correct base URL
    window.addJSTaskUnchecked(new JSEventTask(0, "Event dispatch for " + evt, propagationPath, evt, expectedDoc, defaultAction));
    return false;
  }

  private static List<NodeImpl> getPropagationPath(NodeImpl node, final Event evt, final ModelNode stopNode) {
    final List<NodeImpl> nodes = new LinkedList<>();
    while ((node != null) && (node != stopNode)) {
      if ((node instanceof Element) || (node instanceof Document)) { //  TODO || node instanceof Window) {
        nodes.add(node);

        if (!evt.getBubbles()) {
          break;
        }
      }
      node = (NodeImpl) node.getParentNode();
    }

    // TODO
    // nodes.add(window);

    return nodes;
  }

  private void dispatchEventToHandlers(final NodeImpl node, final Event event) {
    final String type = event.getType();
    final List<EventListener> handlersCopy = snapshotEventListeners(node, type);
    if (handlersCopy != null) {
      // Snapshot under lock and re-check membership to avoid ConcurrentModificationException during dispatch
      for (final EventListener h : handlersCopy) {
        if (event.isImmediatePropagationStopped()) {
          break;
        }

        if (isListenerRegistered(node, type, h)) {
          h.handleEvent(event);
        }
      }
    }
  }

  private void dispatchEventToJSHandlers(final NodeImpl node, final Event event) {
    final String type = event.getType();
    final List<Function> handlersCopy = snapshotJSFunctions(node, type);
    if (handlersCopy != null) {
      // Snapshot under lock and re-check membership to avoid ConcurrentModificationException during dispatch
      for (final Function h : handlersCopy) {
        if (event.isImmediatePropagationStopped()) {
          break;
        }

        if (isFunctionRegistered(node, type, h)) {
          Executor.executeFunction(node, h, event, window.getContextFactory());
        }
      }
    }
  }

  // private final Map<String, List<Function>> onEventHandlers = new HashMap<>();
  private final Map<NodeImpl, Map<String, List<Function>>> nodeOnEventFunctions = new IdentityHashMap<>();

  public void addEventListener(final NodeImpl node, final String type, final Function listener) {
    addEventListener(node, type, listener, false);
  }

  public synchronized void addEventListener(final NodeImpl node, final String type, final Function listener, final boolean useCapture) {
    final List<Function> handlerList = getFunctionList(type, node, true);
    handlerList.add(listener);
  }

  public synchronized void removeEventListener(final NodeImpl node, final String type, final Function listener, final boolean useCapture) {
    final Map<String, List<Function>> onEventListeners = getEventFunctions(node, false);
    if (onEventListeners != null) {
      if (onEventListeners.containsKey(type)) {
        onEventListeners.get(type).remove(listener);
      }
    }
  }

  public synchronized void reset() {
    nodeOnEventFunctions.clear();
    nodeOnEventListeners.clear();
  }

}
