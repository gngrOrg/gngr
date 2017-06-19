package org.lobobrowser.html.js;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.lobobrowser.html.domimpl.NodeImpl;
import org.lobobrowser.html.js.Window.JSRunnableTask;
import org.mozilla.javascript.Function;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.events.EventException;
import org.w3c.dom.events.EventListener;

public final class EventTargetManager {

  private final class JSEventTask extends JSRunnableTask {
    private final NodeImpl node;
    private final Event evt;
    private final List<NodeImpl> propagationPath;
    private List<EventListener> handlers;
    private List<Function> functions;

    private JSEventTask(final int priority, final String description, final List<NodeImpl> propagationPath, final NodeImpl node, final Event evt) {
      super(priority, description, () -> { });
      this.node = node;
      this.evt = evt;
      this.propagationPath = propagationPath;
    }

    public boolean shouldExecute() {
      handlers = getListenerList(evt.getType(), node, false);
      functions = getFunctionList(evt.getType(), node, false);
      return (handlers != null && handlers.size() > 0) || (functions != null && functions.size() > 0);
    }

    public void run() {
      for (int i = 0; (i < propagationPath.size()) && !evt.isPropagationStopped(); i++) {
        final NodeImpl currNode = propagationPath.get(i);
        dispatchEventToHandlers(currNode, evt, handlers);
        dispatchEventToJSHandlers(currNode, evt, functions);
        evt.setPhase(org.w3c.dom.events.Event.BUBBLING_PHASE);
      }
    }
  }

  private final Map<NodeImpl, Map<String, List<EventListener>>> nodeOnEventListeners = new IdentityHashMap<>();
  private final Window window;

  public EventTargetManager(final Window window) {
    this.window = window;
  }

  public void addEventListener(final NodeImpl node, final String type, final EventListener listener, final boolean useCapture) {
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

  public void removeEventListener(final NodeImpl node, final String type, final EventListener listener, final boolean useCapture) {
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

  public boolean dispatchEvent(final NodeImpl node, final Event evt) throws EventException {
    // dispatchEventToHandlers(node, evt, onEventListeners.get(evt.getType()));
    // dispatchEventToJSHandlers(node, evt, onEventHandlers.get(evt.getType()));

    // TODO: get Window into the propagation path
    final List<NodeImpl> propagationPath = getPropagationPath(node, evt);

    // TODO: Capture phase, and distinction between target phase and bubbling phase
    evt.setPhase(org.w3c.dom.events.Event.AT_TARGET);
    // TODO: The JS Task should be added with the correct base URL
    window.addJSTask(new JSEventTask(0, "Event dispatch for " + evt, propagationPath, node, evt));

    // dispatchEventToHandlers(node, evt);
    // dispatchEventToJSHandlers(node, evt);
    return false;
  }

  private static List<NodeImpl> getPropagationPath(NodeImpl node, final Event evt) {
    final List<NodeImpl> nodes = new LinkedList<>();
    while (node != null) {
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

  // private void dispatchEventToHandlers(final NodeImpl node, final Event event, final List<EventListener> handlers) {
  private void dispatchEventToHandlers(final NodeImpl node, final Event event, final List<EventListener> handlers) {
    if (handlers != null) {
      // We clone the collection and check if original collection still contains
      // the handler before dispatching
      // This is to avoid ConcurrentModificationException during dispatch
      final ArrayList<EventListener> handlersCopy = new ArrayList<>(handlers);
      for (final EventListener h : handlersCopy) {
        // TODO: Not sure if we should stop calling handlers after propagation is stopped
        // if (event.isPropagationStopped()) {
        // return;
        // }

        if (handlers.contains(h)) {
          // window.addJSTask(new JSRunnableTask(0, "Event dispatch for: " + event, new Runnable(){
          // public void run() {
          h.handleEvent(event);
          // }
          // }));
          // h.handleEvent(event);

          // Executor.executeFunction(node, h, event);
        }
      }
    }
  }

  // protected void dispatchEventToJSHandlers(final NodeImpl node, final Event event, final List<Function> handlers) {
  protected void dispatchEventToJSHandlers(final NodeImpl node, final Event event, final List<Function> handlers) {
    if (handlers != null) {
      // We clone the collection and check if original collection still contains
      // the handler before dispatching
      // This is to avoid ConcurrentModificationException during dispatch
      final ArrayList<Function> handlersCopy = new ArrayList<>(handlers);
      for (final Function h : handlersCopy) {
        // TODO: Not sure if we should stop calling handlers after propagation is stopped
        // if (event.isPropagationStopped()) {
        // return;
        // }

        if (handlers.contains(h)) {
          // window.addJSTask(new JSRunnableTask(0, "Event dispatch for " + event, new Runnable(){
          // public void run() {
          Executor.executeFunction(node, h, event, window.getContextFactory());
          // }
          // }));
          // Executor.executeFunction(node, h, event);
        }
      }
    }
  }

  // private final Map<String, List<Function>> onEventHandlers = new HashMap<>();
  private final Map<NodeImpl, Map<String, List<Function>>> nodeOnEventFunctions = new IdentityHashMap<>();

  public void addEventListener(final NodeImpl node, final String type, final Function listener) {
    addEventListener(node, type, listener, false);
  }

  public void addEventListener(final NodeImpl node, final String type, final Function listener, final boolean useCapture) {
    // TODO
    // System.out.println("node by name: " + node.getNodeName() + " adding Event listener of type: " + type);

    /*
    List<Function> handlerList = null;
    if (onEventHandlers.containsKey(type)) {
      handlerList = onEventHandlers.get(type);
    } else {
      handlerList = new ArrayList<>();
      onEventHandlers.put(type, handlerList);
    }*/
    // final Map<String, List<Function>> handlerList = getEventFunctions(node, true);
    final List<Function> handlerList = getFunctionList(type, node, true);
    handlerList.add(listener);
  }

  public void removeEventListener(final NodeImpl node, final String type, final Function listener, final boolean useCapture) {
    final Map<String, List<Function>> onEventListeners = getEventFunctions(node, false);
    if (onEventListeners != null) {
      if (onEventListeners.containsKey(type)) {
        onEventListeners.get(type).remove(listener);
      }
    }
  }

  public void reset() {
    nodeOnEventFunctions.clear();
    nodeOnEventListeners.clear();
  }

}
