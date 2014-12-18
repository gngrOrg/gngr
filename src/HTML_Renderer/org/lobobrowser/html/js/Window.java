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
/*
 * Created on Nov 12, 2005
 */
package org.lobobrowser.html.js;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Timer;

import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.domimpl.CommentImpl;
import org.lobobrowser.html.domimpl.HTMLDivElementImpl;
import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.domimpl.HTMLElementImpl;
import org.lobobrowser.html.domimpl.HTMLIFrameElementImpl;
import org.lobobrowser.html.domimpl.HTMLImageElementImpl;
import org.lobobrowser.html.domimpl.HTMLOptionElementImpl;
import org.lobobrowser.html.domimpl.HTMLScriptElementImpl;
import org.lobobrowser.html.domimpl.HTMLSelectElementImpl;
import org.lobobrowser.js.AbstractScriptableDelegate;
import org.lobobrowser.js.JavaClassWrapper;
import org.lobobrowser.js.JavaClassWrapperFactory;
import org.lobobrowser.js.JavaInstantiator;
import org.lobobrowser.js.JavaObjectWrapper;
import org.lobobrowser.js.JavaScript;
import org.lobobrowser.ua.UserAgentContext;
import org.lobobrowser.ua.UserAgentContext.Request;
import org.lobobrowser.ua.UserAgentContext.RequestKind;
import org.lobobrowser.util.ID;
import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.css.CSS2Properties;
import org.w3c.dom.events.EventException;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLCollection;
import org.w3c.dom.html.HTMLElement;
import org.w3c.dom.views.AbstractView;
import org.w3c.dom.views.DocumentView;

public class Window extends AbstractScriptableDelegate implements AbstractView, EventTarget {
  private static final Logger logger = Logger.getLogger(Window.class.getName());
  private static final Map<HtmlRendererContext, WeakReference<Window>> CONTEXT_WINDOWS = new WeakHashMap<>();
  // private static final JavaClassWrapper IMAGE_WRAPPER =
  // JavaClassWrapperFactory.getInstance().getClassWrapper(Image.class);
  private static final JavaClassWrapper XMLHTTPREQUEST_WRAPPER = JavaClassWrapperFactory.getInstance()
      .getClassWrapper(XMLHttpRequest.class);

  // Timer ids should begin counting from 1 or more.
  // jQuery's ajax polling handler relies on a non-zero value (uses it as a boolean condition)
  // Chromium 37 starts counting from 1 while Firefox 32 starts counting from 2 (from developer consoles and plugins installed)
  private static int timerIdCounter = 1;

  private final HtmlRendererContext rcontext;
  private final UserAgentContext uaContext;

  private Navigator navigator;
  private Screen screen;
  private Location location;
  private Map<Integer, TaskWrapper> taskMap;
  private volatile HTMLDocumentImpl document;

  public Window(final HtmlRendererContext rcontext, final UserAgentContext uaContext) {
    // TODO: Probably need to create a new Window instance
    // for every document. Sharing of Window state between
    // different documents is not correct.
    this.rcontext = rcontext;
    this.uaContext = uaContext;
  }

  private static int generateTimerID() {
    synchronized (logger) {
      return timerIdCounter++;
    }
  }

  public HtmlRendererContext getHtmlRendererContext() {
    return this.rcontext;
  }

  public UserAgentContext getUserAgentContext() {
    return this.uaContext;
  }

  private void clearState() {
    synchronized (this) {
      // windowClosing = true;
      document.stopEverything();
      jsScheduler.stopAndWindUp();
      jsScheduler = new JSScheduler(this);
      eventTargetManager.reset();
      this.onWindowLoadHandler = null;

      this.forgetAllTasks();

      // Commenting out call to getWindowScope() since that creates a new scope which is wasteful
      // if we are going to destroy it anyway.
      // final Scriptable s = this.getWindowScope();
      final Scriptable s = this.windowScope;
      if (s != null) {
        final Object[] ids = s.getIds();
        for (final Object id : ids) {
          if (id instanceof String) {
            s.delete((String) id);
          } else if (id instanceof Integer) {
            s.delete(((Integer) id).intValue());
          }
        }
      }

      System.out.println("Clearing document scope");
      document.setUserData(Executor.SCOPE_KEY, null, null);

      // This will ensure that a fresh scope will be created by getWindowScope() on the next call
      this.windowScope = null;
      jobFinishedHandler = null;
    }
  }

  public void setDocument(final HTMLDocumentImpl document) {
    synchronized (this) {

      final Document prevDocument = this.document;
      if (prevDocument != document) {
        final Function onunload = this.onunload;
        if (onunload != null) {
          final HTMLDocumentImpl oldDoc = (HTMLDocumentImpl) prevDocument;
          Executor.executeFunction(this.getWindowScope(), onunload, oldDoc.getDocumentURL(), this.uaContext, windowFactory);
          this.onunload = null;
        }

        // TODO: Should clearing of the state be done when window "unloads"?
        if (prevDocument != null) {
          // Only clearing when the previous document was not null
          // because state might have been set on the window before
          // the very first document is added.
          this.clearState();
        }
        // this.forgetAllTasks();
        this.initWindowScope(document);

        // Set up Javascript scope
        System.out.println("Setting up scope: " + getWindowScope());
        document.setUserData(Executor.SCOPE_KEY, getWindowScope(), null);
        jsScheduler.start();

        this.document = document;
        // eventTargetManager.setNode(document);
      }
    }
  }

  public DocumentView getDocument() {
    return this.document;
  }

  public Document getDocumentNode() {
    return this.document;
  }

  private abstract static class JSTask implements Comparable<JSTask> {
    protected final int priority;
    protected final long creationTime;
    protected final String description;
    private final AccessControlContext context;

    // TODO: Add a context parameter that will be combined with current context, to help with creation of timer tasks
    // public JSTask(final int priority, final Runnable runnable) {
    public JSTask(final int priority, final String description) {
      this.priority = priority;
      this.description = description;
      this.context = AccessController.getContext();
      this.creationTime = System.nanoTime();
    }

    // TODO: Add a way to stop a task. It should return false if the task can't be stopped in which case a thread kill will be performed by the task scheduler.

    // TODO: Sorting by priority
    public int compareTo(final JSTask o) {
      final long diffCreation = (o.creationTime - creationTime);
      if (diffCreation < 0) {
        return 1;
      } else if (diffCreation == 0) {
        return 0;
      } else {
        return -1;
      }
    }

    public abstract void run();

  }

  public final static class JSRunnableTask extends JSTask {
    private final Runnable runnable;

    public JSRunnableTask(final int priority, final Runnable runnable) {
      this(priority, "", runnable);
    }

    public JSRunnableTask(final int priority, final String description, final Runnable runnable) {
      super(priority, description);
      this.runnable = runnable;
    }

    @Override
    public String toString() {
      // return "JSRunnableTask [priority=" + priority + ", runnable=" + runnable + ", creationTime=" + creationTime + "]";
      return "JSRunnableTask [priority=" + priority + ", description=" + description + ", creationTime=" + creationTime + "]";
    }

    @Override
    public void run() {
      runnable.run();
    }

  }

  public final static class JSSupplierTask<T> extends JSTask {
    private final Supplier<T> supplier;
    private final Consumer<T> consumer;

    public JSSupplierTask(final int priority, final Supplier<T> supplier, final Consumer<T> consumer) {
      super(priority, "supplier description TODO");
      this.supplier = supplier;
      this.consumer = consumer;
    }

    @Override
    public void run() {
      final T result = supplier.get();
      consumer.accept(result);
    }
  }

  private static final class JSScheduler extends Thread {
    private static final class ScheduledTask implements Comparable<ScheduledTask> {
      final int id;
      final JSTask task;

      public ScheduledTask(final int id, final JSTask task) {
        this.id = id;
        this.task = task;
      }

      public int compareTo(final ScheduledTask other) {
        return task.compareTo(other.task);
      }

      @Override
      public boolean equals(final Object o) {
        if (o instanceof Integer) {
          final Integer oId = (Integer) o;
          return oId == id;
        }
        return false;
      }

      @Override
      public String toString() {
        return "Scheduled Task (" + id + ", " + task + ")";
      }
    }

    private final PriorityBlockingQueue<ScheduledTask> jsQueue = new PriorityBlockingQueue<>();

    private volatile boolean windowClosing = false;

    // TODO: This is not water tight for one reason, Windows are reused for different documents.
    // If they are always freshly created, the taskIdCounter will be more reliable.
    private volatile AtomicInteger taskIdCounter = new AtomicInteger(0);

    // TODO: Remove, added just for debugging
    private final Window window;

    public JSScheduler(final Window window) {
      super("JS Scheduler");
      this.window = window;
    }

    @Override
    public void run() {
      System.out.println("\n\nIn " + window.document.getBaseURI() + " Running loop");
      while (!windowClosing) {
        try {
          ScheduledTask scheduledTask;
          // TODO: uncomment if synchronization is necessary with the add methods
          // synchronized (this) {
          scheduledTask = jsQueue.take();
          // }
          final PrivilegedAction<Object> action = new PrivilegedAction<Object>() {
            public Object run() {
              // System.out.println("In " + window.document.getBaseURI() + "\n  Running task: " + scheduledTask);
              scheduledTask.task.run();
              // System.out.println("Done task: " + scheduledTask);
              // System.out.println("  Remaining tasks: " + jsQueue.size());
              return null;
            }
          };
          AccessController.doPrivileged(action, scheduledTask.task.context);
        } catch (final InterruptedException e) {
          final int queueSize = jsQueue.size();
          if (queueSize > 0) {
            System.err.println("JS Scheduler was interrupted. Tasks remaining: " + jsQueue.size());
          }
        } catch (final Exception e) {
          e.printStackTrace();
        }
      }
      System.out.println("Exiting loop\n\n");
    }

    public void stopAndWindUp() {
      System.out.println("Going to stop JS scheduler");
      windowClosing = true;

      // TODO: Check if interrupt is needed if stop is anyway being called.
      this.interrupt();
      try {
        this.join(10);
      } catch (final InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      this.stop();
      System.out.println("Finished interrupting");
    }

    public void addJSTask(final JSTask task) {
      // synchronized (this) {
      jsQueue.add(new ScheduledTask(0, task));
      // }
    }

    public int addUniqueJSTask(final int oldId, final JSTask task) {
      // synchronized (this) {
      if (oldId != -1) {
        if (jsQueue.contains(oldId)) {
          return oldId;
        }
        /*
        for (ScheduledTask t : jsQueue) {
          if (t.id == oldId) {
            // Task found
            return oldId;
          }
        }*/
      }
      final int newId = taskIdCounter.addAndGet(1);
      jsQueue.add(new ScheduledTask(newId, task));
      return newId;
      // }
    }
  }

  private volatile JSScheduler jsScheduler = new JSScheduler(this);

  @HideFromJS
  public void addJSTask(final JSTask task) {
    final URL url = document.getDocumentURL();
    if (uaContext.isRequestPermitted(new Request(url, RequestKind.JavaScript))) {
      // System.out.println("Adding task: " + task);
      synchronized (this) {
        jsScheduler.addJSTask(task);
      }
    }
  }

  // TODO: Try to refactor this so that all tasks are checked here rather than in caller
  // TODO: Some tasks are added unchecked for various reasons that need to be reviewed:
  //       1. Timer task. The logic is that a script that was permitted to create the timer already has the permission to execute it.
  //          But it would be better if their permission is checked again to account for subsequent changes through RequestManager,
  //          or if RequestManager assures that page is reloaded for *any* permission change.
  //       2. Event listeners. Logic is similar to Timer task
  //       3. Script elements. They are doing the checks themselves, but it would better to move the check here.
  //       4. XHR handler. Logic similar to timer task.
  @HideFromJS
  public void addJSTaskUnchecked(final JSTask task) {
    // System.out.println("Adding task: " + task);
    synchronized (this) {
      jsScheduler.addJSTask(task);
    }
  }

  @HideFromJS
  public int addJSUniqueTask(final int oldId, final JSTask task) {
    System.out.println("Adding unique task: " + task);

    synchronized (this) {
      return jsScheduler.addUniqueJSTask(oldId, task);
    }
  }

  private void putAndStartTask(final Integer timeoutID, final Timer timer, final Object retained) {
    TaskWrapper oldTaskWrapper = null;
    synchronized (this) {
      Map<Integer, TaskWrapper> taskMap = this.taskMap;
      if (taskMap == null) {
        taskMap = new HashMap<>(4);
        this.taskMap = taskMap;
      } else {
        oldTaskWrapper = taskMap.get(timeoutID);
      }
      taskMap.put(timeoutID, new TaskWrapper(timer, retained));
    }
    // Do this outside synchronized block, just in case.
    if (oldTaskWrapper != null) {
      oldTaskWrapper.timer.stop();
    }
    timer.start();
  }

  private void forgetTask(final Integer timeoutID, final boolean cancel) {
    TaskWrapper oldTimer = null;
    synchronized (this) {
      final Map<Integer, TaskWrapper> taskMap = this.taskMap;
      if (taskMap != null) {
        oldTimer = taskMap.remove(timeoutID);
      }
    }
    if ((oldTimer != null) && cancel) {
      oldTimer.timer.stop();
    }
  }

  private void forgetAllTasks() {
    TaskWrapper[] oldTaskWrappers = null;
    synchronized (this) {
      final Map<Integer, TaskWrapper> taskMap = this.taskMap;
      if (taskMap != null) {
        oldTaskWrappers = taskMap.values().toArray(new TaskWrapper[0]);
        this.taskMap = null;
      }
    }
    if (oldTaskWrappers != null) {
      for (final TaskWrapper taskWrapper : oldTaskWrappers) {
        taskWrapper.timer.stop();
      }
    }
  }

  // private Timer getTask(Long timeoutID) {
  // synchronized(this) {
  // Map taskMap = this.taskMap;
  // if(taskMap != null) {
  // return (Timer) taskMap.get(timeoutID);
  // }
  // }
  // return null;
  // }

  /**
   * @param aFunction
   *          Javascript function to invoke on each loop.
   * @param aTimeInMs
   *          Time in millisecund between each loop. TODO: Can this be converted
   *          to long type?
   * @return Return the timer ID to use as reference
   * @see <a
   *      href="http://developer.mozilla.org/en/docs/DOM:window.setInterval">Window.setInterval
   *      interface definition</a>
   * @todo Make proper and refactore with
   *       {@link Window#setTimeout(Function, double)}.
   */
  public int setInterval(final Function aFunction, final double aTimeInMs) {
    if ((aTimeInMs > Integer.MAX_VALUE) || (aTimeInMs < 0)) {
      throw new IllegalArgumentException("Timeout value " + aTimeInMs + " is not supported.");
    }
    final int timeID = generateTimerID();
    System.out.println("Created interval timer: " + timeID);
    final Integer timeIDInt = new Integer(timeID);
    final ActionListener task = new FunctionTimerTask(this, timeIDInt, aFunction, false);
    int t = (int) aTimeInMs;
    if (t < 1) {
      t = 1;
    }
    final Timer timer = new Timer(t, task);
    timer.setRepeats(true); // The only difference with setTimeout
    this.putAndStartTask(timeIDInt, timer, aFunction);
    return timeID;
  }

  /**
   * @param aExpression
   *          Javascript expression to invoke on each loop.
   * @param aTimeInMs
   *          Time in millisecund between each loop.
   * @return Return the timer ID to use as reference
   * @see <a
   *      href="http://developer.mozilla.org/en/docs/DOM:window.setInterval">Window.setInterval
   *      interface definition</a>
   * @todo Make proper and refactore with
   *       {@link Window#setTimeout(String, double)}.
   */
  public int setInterval(final String aExpression, final double aTimeInMs) {
    if ((aTimeInMs > Integer.MAX_VALUE) || (aTimeInMs < 0)) {
      throw new IllegalArgumentException("Timeout value " + aTimeInMs + " is not supported.");
    }
    final int timeID = generateTimerID();
    final Integer timeIDInt = new Integer(timeID);
    final ActionListener task = new ExpressionTimerTask(this, timeIDInt, aExpression, false);
    int t = (int) aTimeInMs;
    if (t < 1) {
      t = 1;
    }
    final Timer timer = new Timer(t, task);
    timer.setRepeats(false); // The only difference with setTimeout
    this.putAndStartTask(timeIDInt, timer, null);
    return timeID;
  }

  /**
   * @param aTimerID
   *          Timer ID to stop.
   * @see <a
   *      href="http://developer.mozilla.org/en/docs/DOM:window.clearInterval">Window.clearInterval
   *      interface Definition</a>
   */
  public void clearInterval(final int aTimerID) {
    final Integer key = new Integer(aTimerID);
    this.forgetTask(key, true);
  }

  public void clearInterval(final Object unused) {
    // Happens when jQuery calls this with a null parameter;
    // TODO: Check if there are other cases
    if (unused instanceof Integer) {
      final Integer id = (Integer) unused;
      clearInterval((int) id);
      return;
    }
    System.out.println("Clear interval : ignoring " + unused);
    // TODO: Should this be throwing an exception?
    // throw new UnsupportedOperationException();
  }

  public void alert(final String message) {
    if (this.rcontext != null) {
      this.rcontext.alert(message);
    }
  }

  public void back() {
    final HtmlRendererContext rcontext = this.rcontext;
    if (rcontext != null) {
      rcontext.back();
    }
  }

  public void blur() {
    final HtmlRendererContext rcontext = this.rcontext;
    if (rcontext != null) {
      rcontext.blur();
    }
  }

  public void clearTimeout(final Object someObj) {
    if (someObj instanceof Integer) {
      final Integer id = (Integer) someObj;
      clearTimeout(id.intValue());
    } else {
      System.out.println("Window.clearTimeout() : Ignoring: " + someObj);
    }
  }

  private void clearTimeout(final int timeoutID) {
    System.out.println("Clearing timeout: " + timeoutID);
    final Integer key = new Integer(timeoutID);
    this.forgetTask(key, true);
  }

  public void close() {
    final HtmlRendererContext rcontext = this.rcontext;
    if (rcontext != null) {
      rcontext.close();
    }
  }

  public boolean confirm(final String message) {
    final HtmlRendererContext rcontext = this.rcontext;
    if (rcontext != null) {
      return rcontext.confirm(message);
    } else {
      return false;
    }
  }

  // TODO: Hide from JS
  // Making public for link element
  public void evalInScope(final String javascript) {
    addJSTask(new JSRunnableTask(0, new Runnable() {
      public void run() {
        try {
          final String scriptURI = "window.eval";
          final Context ctx = Executor.createContext(document.getDocumentURL(), Window.this.uaContext, windowFactory);
          ctx.evaluateString(getWindowScope(), javascript, scriptURI, 1, null);
        } finally {
          Context.exit();
        }
      }
    }));
  }

  /*
  private Object evalInScope(final String javascript) {
    final Context ctx = Executor.createContext(document.getDocumentURL(), this.uaContext);
    try {
      final String scriptURI = "window.eval";
      return ctx.evaluateString(getWindowScope(), javascript, scriptURI, 1, null);
    } finally {
      Context.exit();
    }
  }

  /* Removing because this eval method interferes with the default eval() method.
   * The context of the JS eval() call is not preserved by this method.
  public Object eval(final String javascript) {
    final HTMLDocumentImpl document = (HTMLDocumentImpl) this.document;
    if (document == null) {
      throw new IllegalStateException("Cannot evaluate if document is not set.");
    }
    final Context ctx = Executor.createContext(document.getDocumentURL(), this.uaContext);
    try {
      final Scriptable scope = this.getWindowScope();
      if (scope == null) {
        throw new IllegalStateException("Scriptable (scope) instance was expected to be keyed as UserData to document using "
            + Executor.SCOPE_KEY);
      }
      final String scriptURI = "window.eval";
      if (logger.isLoggable(Level.INFO)) {
        logger.info("eval(): javascript follows...\r\n" + javascript);
      }
      return ctx.evaluateString(scope, javascript, scriptURI, 1, null);
    } finally {
      Context.exit();
    }
  }
   */

  public void focus() {
    final HtmlRendererContext rcontext = this.rcontext;
    if (rcontext != null) {
      rcontext.focus();
    }
  }

  static class MyContextFactory extends ContextFactory {
    static final private ClassShutter myClassShutter = new ClassShutter() {
      public boolean visibleToScripts(final String fullClassName) {
        // System.out.println("class shutter Checking: " + fullClassName);
        if (fullClassName.startsWith("java")) {
          final boolean isException = (fullClassName.startsWith("java.lang") && fullClassName.endsWith("Exception"));
          if (fullClassName.equals("java.lang.Object") || isException) {
            return true;
          }
          System.out.println("Warning: Something tried to access java classes from javascript.");
          Thread.dumpStack();
          return false;
        }

        // TODO: Change the default to false
        return true;
      }
    };

    // Override {@link #makeContext()}
    @Override
    protected Context makeContext()
    {
      final Context cx = super.makeContext();
      cx.setClassShutter(myClassShutter);
      // cx.setOptimizationLevel(9);
      cx.setOptimizationLevel(-1);
      // System.out.println("Opt level: " + cx.getOptimizationLevel());

      // Make Rhino runtime to call observeInstructionCount
      // each 100000 bytecode instructions
      // cx.setInstructionObserverThreshold(1000000);

      // cx.setMaximumInterpreterStackDepth(100);
      // cx.seal(null);

      //cx.setDebugger(myDebugger, null);
      return cx;
    }

    @Override
    protected void observeInstructionCount(final Context cx, final int instructionCount) {
      System.out.println("Context: " + cx + "  Instruction count: " + instructionCount);
    }

    /*
    final private Debugger myDebugger = new Debugger() {

      @Override
      public void handleCompilationDone(Context cx, DebuggableScript fnOrScript, String source) {
        // TODO Auto-generated method stub

      }

      @Override
      public DebugFrame getFrame(Context cx, DebuggableScript fnOrScript) {
        // TODO Auto-generated method stub
        return null;
      }
    };*/

  }

  // TODO: make private
  public MyContextFactory windowFactory = new MyContextFactory();

  // final private org.mozilla.javascript.tools.debugger.Main debuggerMain = new org.mozilla.javascript.tools.debugger.Main("Debugger");

  private void initWindowScope(final Document doc) {
    /*
    ContextFactory.getGlobal().addListener(new org.mozilla.javascript.ContextFactory.Listener() {

      @Override
      public void contextCreated(Context cx) {
        // cx.setGenerateObserverCount(true);
        cx.setInstructionObserverThreshold(10);
      }

      @Override
      public void contextReleased(Context cx) {
        // TODO Auto-generated method stub

      }});*/

    // Initialize GlobalFactory with custom factory
    /*
    if (!ContextFactory.hasExplicitGlobal()) {
      ContextFactory.initGlobal(new MyFactory());
    }*/

    // TODO: Attaching to the context helps remove the getParentScope() infinite loop
    // But, need to check if this is a race condition in our own code.
    // Also, debugger needs lots of extra permissions!
    // debuggerMain.attachTo(ContextFactory.getGlobal());
    // debuggerMain.attachTo(windowFactory);
    // debuggerMain.setVisible(true);

    // Special Javascript class: XMLHttpRequest
    final Scriptable ws = this.getWindowScope();
    final JavaInstantiator xi = new JavaInstantiator() {
      public Object newInstance() {
        final Document d = doc;
        if (d == null) {
          throw new IllegalStateException("Cannot perform operation when document is unset.");
        }
        HTMLDocumentImpl hd;
        try {
          hd = (HTMLDocumentImpl) d;
        } catch (final ClassCastException err) {
          throw new IllegalStateException("Cannot perform operation with documents of type " + d.getClass().getName() + ".");
        }
        return new XMLHttpRequest(uaContext, hd.getDocumentURL(), ws, Window.this);
      }
    };
    final Function xmlHttpRequestC = JavaObjectWrapper.getConstructor("XMLHttpRequest", XMLHTTPREQUEST_WRAPPER, ws, xi);
    ScriptableObject.defineProperty(ws, "XMLHttpRequest", xmlHttpRequestC, ScriptableObject.READONLY);

    /*
    {
      // Define the Window object
      final JavaClassWrapper windowWrapper = JavaClassWrapperFactory.getInstance().getClassWrapper(Window.class);
      final JavaInstantiator wi = () -> { throw new UnsupportedOperationException(); };
      ScriptableObject.defineProperty(ws, "Window", JavaObjectWrapper.getConstructor("Window", windowWrapper, ws, wi),
          ScriptableObject.READONLY);
    }*/

    // ScriptableObject.defineClass(ws, org.mozilla.javascript.ast.Comment.class);
    defineElementClass(ws, doc, "Comment", "comment", CommentImpl.class);

    // HTML element classes
    defineElementClass(ws, doc, "Image", "img", HTMLImageElementImpl.class);
    defineElementClass(ws, doc, "Script", "script", HTMLScriptElementImpl.class);
    defineElementClass(ws, doc, "IFrame", "iframe", HTMLIFrameElementImpl.class);
    defineElementClass(ws, doc, "Option", "option", HTMLOptionElementImpl.class);
    defineElementClass(ws, doc, "Select", "select", HTMLSelectElementImpl.class);

    // TODO: Add all similar elements
    defineElementClass(ws, doc, "HTMLDivElement", "div", HTMLDivElementImpl.class);
  }

  private Scriptable windowScope;

  private Scriptable getWindowScope() {
    synchronized (this) {
      Scriptable windowScope = this.windowScope;
      if (windowScope != null) {
        return windowScope;
      }
      // Context.enter() OK in this particular case.
      // final Context ctx = Context.enter();
      final Context ctx = windowFactory.enterContext();
      try {
        // Window scope needs to be top-most scope.
        windowScope = (Scriptable) JavaScript.getInstance().getJavascriptObject(this, null);
        windowScope = ctx.initStandardObjects((ScriptableObject) windowScope);
        final Object consoleJSObj = JavaScript.getInstance().getJavascriptObject(new Console(), windowScope);
        ScriptableObject.putProperty(windowScope, "console", consoleJSObj);
        this.windowScope = windowScope;
        return windowScope;
      } finally {
        Context.exit();
      }
    }
  }

  static public class Console {
    public void log(final Object obj) {
      System.out.println("> " + obj);
    }

  }

  private final static void defineElementClass(final Scriptable scope, final Document document, final String jsClassName,
      final String elementName,
      final Class<?> javaClass) {
    final JavaInstantiator ji = new JavaInstantiator() {
      public Object newInstance() {
        final Document d = document;
        if (d == null) {
          throw new IllegalStateException("Document not set in current context.");
        }
        return d.createElement(elementName);
      }
    };
    final JavaClassWrapper classWrapper = JavaClassWrapperFactory.getInstance().getClassWrapper(javaClass);
    final Function constructorFunction = JavaObjectWrapper.getConstructor(jsClassName, classWrapper, scope, ji);
    ScriptableObject.defineProperty(scope, jsClassName, constructorFunction, ScriptableObject.READONLY);
  }

  public static Window getWindow(final HtmlRendererContext rcontext) {
    if (rcontext == null) {
      return null;
    }
    synchronized (CONTEXT_WINDOWS) {
      final WeakReference<Window> wref = CONTEXT_WINDOWS.get(rcontext);
      if (wref != null) {
        final Window window = wref.get();
        if (window != null) {
          return window;
        }
      }
      final Window window = new Window(rcontext, rcontext.getUserAgentContext());
      CONTEXT_WINDOWS.put(rcontext, new WeakReference<>(window));
      return window;
    }
  }

  public Window open(final String relativeUrl, final String windowName, final String windowFeatures, final boolean replace) {
    final HtmlRendererContext rcontext = this.rcontext;
    if (rcontext != null) {
      java.net.URL url;
      final Object document = this.document;
      if (document instanceof HTMLDocumentImpl) {
        url = ((HTMLDocumentImpl) document).getFullURL(relativeUrl);
      } else {
        try {
          url = new java.net.URL(relativeUrl);
        } catch (final java.net.MalformedURLException mfu) {
          throw new IllegalArgumentException("Malformed URI: " + relativeUrl);
        }
      }
      final HtmlRendererContext newContext = rcontext.open(url, windowName, windowFeatures, replace);
      return getWindow(newContext);
    } else {
      return null;
    }
  }

  public Window open(final String url) {
    return this.open(url, "window:" + String.valueOf(ID.generateLong()));
  }

  public Window open(final String url, final String windowName) {
    return this.open(url, windowName, "", false);
  }

  public Window open(final String url, final String windowName, final String windowFeatures) {
    return this.open(url, windowName, windowFeatures, false);
  }

  public String prompt(final String message) {
    return this.prompt(message, "");
  }

  public String prompt(final String message, final int inputDefault) {
    return this.prompt(message, String.valueOf(inputDefault));
  }

  public String prompt(final String message, final String inputDefault) {
    final HtmlRendererContext rcontext = this.rcontext;
    if (rcontext != null) {
      return rcontext.prompt(message, inputDefault);
    } else {
      return null;
    }
  }

  public void scrollTo(final int x, final int y) {
    final HtmlRendererContext rcontext = this.rcontext;
    if (rcontext != null) {
      rcontext.scroll(x, y);
    }
  }

  public void scrollBy(final int x, final int y) {
    final HtmlRendererContext rcontext = this.rcontext;
    if (rcontext != null) {
      rcontext.scrollBy(x, y);
    }
  }

  public void resizeTo(final int width, final int height) {
    final HtmlRendererContext rcontext = this.rcontext;
    if (rcontext != null) {
      rcontext.resizeTo(width, height);
    }
  }

  public void resizeBy(final int byWidth, final int byHeight) {
    final HtmlRendererContext rcontext = this.rcontext;
    if (rcontext != null) {
      rcontext.resizeBy(byWidth, byHeight);
    }
  }

  @NotGetterSetter
  public int setTimeout(final String expr, final double millis) {
    if ((millis > Integer.MAX_VALUE) || (millis < 0)) {
      throw new IllegalArgumentException("Timeout value " + millis + " is not supported.");
    }
    final int timeID = generateTimerID();
    final Integer timeIDInt = new Integer(timeID);
    final ActionListener task = new ExpressionTimerTask(this, timeIDInt, expr, true);
    int t = (int) millis;
    if (t < 1) {
      t = 1;
    }
    final Timer timer = new Timer(t, task);
    timer.setRepeats(false);
    this.putAndStartTask(timeIDInt, timer, null);
    return timeID;
  }

  @NotGetterSetter
  public int setTimeout(final Function function, final double millis) {
    if ((millis > Integer.MAX_VALUE) || (millis < 0)) {
      throw new IllegalArgumentException("Timeout value " + millis + " is not supported.");
    }
    final int timeID = generateTimerID();
    System.out.println("Creating timer with id: " + timeID + " in " + document.getBaseURI());
    final Integer timeIDInt = new Integer(timeID);
    final ActionListener task = new FunctionTimerTask(this, timeIDInt, function, true);
    int t = (int) millis;
    if (t < 1) {
      t = 1;
    }
    final Timer timer = new Timer(t, task);
    timer.setRepeats(false);
    this.putAndStartTask(timeIDInt, timer, function);
    return timeID;
  }

  @NotGetterSetter
  public int setTimeout(final Function function) {
    return setTimeout(function, 0);
  }

  @NotGetterSetter
  public int setTimeout(final String expr) {
    return setTimeout(expr, 0);
  }

  public boolean isClosed() {
    final HtmlRendererContext rcontext = this.rcontext;
    if (rcontext != null) {
      return rcontext.isClosed();
    } else {
      return false;
    }
  }

  public String getDefaultStatus() {
    final HtmlRendererContext rcontext = this.rcontext;
    if (rcontext != null) {
      return rcontext.getDefaultStatus();
    } else {
      return null;
    }
  }

  public HTMLCollection getFrames() {
    final Document doc = this.document;
    if (doc instanceof HTMLDocumentImpl) {
      return ((HTMLDocumentImpl) doc).getFrames();
    }
    return null;
  }

  private int length;
  private boolean lengthSet = false;

  /**
   * Gets the number of frames.
   */
  public int getLength() {
    if (this.lengthSet) {
      return this.length;
    } else {
      final HTMLCollection frames = this.getFrames();
      return frames == null ? 0 : frames.getLength();
    }
  }

  public void setLength(final int length) {
    this.lengthSet = true;
    this.length = length;
  }

  public String getName() {
    final HtmlRendererContext rcontext = this.rcontext;
    if (rcontext != null) {
      return rcontext.getName();
    } else {
      return null;
    }
  }

  public void setName(final String newName) {
    // TODO
    System.out.println("TODO: window.setName");
  }

  public Window getParent() {
    final HtmlRendererContext rcontext = this.rcontext;
    if (rcontext != null) {
      final HtmlRendererContext rcontextParent = rcontext.getParent();
      if (rcontextParent == null) {
        return this;
      } else {
        return Window.getWindow(rcontextParent);
      }
    } else {
      return null;
    }
  }

  public Window getOpener() {
    final HtmlRendererContext rcontext = this.rcontext;
    if (rcontext != null) {
      return Window.getWindow(rcontext.getOpener());
    } else {
      return null;
    }
  }

  public void setOpener(final Window opener) {
    final HtmlRendererContext rcontext = this.rcontext;
    if (rcontext != null) {
      if (opener == null) {
        rcontext.setOpener(null);
      } else {
        rcontext.setOpener(opener.rcontext);
      }
    }
  }

  public Window getSelf() {
    return this;
  }

  public String getStatus() {
    final HtmlRendererContext rcontext = this.rcontext;
    if (rcontext != null) {
      return rcontext.getStatus();
    } else {
      return null;
    }
  }

  public void setStatus(final String message) {
    final HtmlRendererContext rcontext = this.rcontext;
    if (rcontext != null) {
      rcontext.setStatus(message);
    }
  }

  public Window getTop() {
    final HtmlRendererContext rcontext = this.rcontext;
    if (rcontext != null) {
      return Window.getWindow(rcontext.getTop());
    } else {
      return null;
    }
  }

  public Window getWindow() {
    return this;
  }

  public Navigator getNavigator() {
    synchronized (this) {
      Navigator nav = this.navigator;
      if (nav == null) {
        nav = new Navigator(this.uaContext);
        this.navigator = nav;
      }
      return nav;
    }
  }

  public Screen getScreen() {
    synchronized (this) {
      Screen nav = this.screen;
      if (nav == null) {
        nav = new Screen();
        this.screen = nav;
      }
      return nav;
    }
  }

  public Location getLocation() {
    synchronized (this) {
      Location location = this.location;
      if (location == null) {
        location = new Location(this);
        this.location = location;
      }
      return location;
    }
  }

  public void setLocation(final String location) {
    this.getLocation().setHref(location);
  }

  private History history;

  public History getHistory() {
    synchronized (this) {
      History history = this.history;
      if (history == null) {
        history = new History(this);
        this.history = history;
      }
      return history;
    }
  }

  public CSS2Properties getComputedStyle(final HTMLElement element, final String pseudoElement) {
    if (element instanceof HTMLElementImpl) {
      return ((HTMLElementImpl) element).getComputedStyle(pseudoElement);
    } else {
      throw new java.lang.IllegalArgumentException("Element implementation unknown: " + element);
    }
  }

  public Function getOnload() {
    final Document doc = this.document;
    if (doc instanceof HTMLDocumentImpl) {
      return ((HTMLDocumentImpl) doc).getOnloadHandler();
    } else {
      return null;
    }
  }

  public void setOnload(final Function onload) {
    // Note that body.onload overrides
    // window.onload.
    /*
    final Document doc = this.document;
    if (doc instanceof HTMLDocumentImpl) {
      ((HTMLDocumentImpl) doc).setWindowOnloadHandler(onload);
    }*/
    onWindowLoadHandler = onload;
  }

  private Function onunload;

  public Function getOnunload() {
    return onunload;
  }

  public void setOnunload(final Function onunload) {
    this.onunload = onunload;
  }

  public org.w3c.dom.Node namedItem(final String name) {
    // Bug 1928758: Element IDs are named objects in context.
    final HTMLDocumentImpl doc = this.document;
    if (doc == null) {
      return null;
    }
    final org.w3c.dom.Node node = doc.getElementById(name);
    if (node != null) {
      return node;
    }
    return null;
  }

  public void forceGC() {
    System.gc();
  }

  private static abstract class WeakWindowTask implements ActionListener {
    private final WeakReference<Window> windowRef;

    public WeakWindowTask(final Window window) {
      this.windowRef = new WeakReference<>(window);
    }

    protected Window getWindow() {
      final WeakReference<Window> ref = this.windowRef;
      return ref == null ? null : ref.get();
    }
  }

  private static class FunctionTimerTask extends WeakWindowTask {
    // Implemented as a static WeakWindowTask to allow the Window
    // to get garbage collected, especially in infinite loop
    // scenarios.
    private final Integer timeIDInt;
    private final WeakReference<Function> functionRef;
    private final boolean removeTask;

    public FunctionTimerTask(final Window window, final Integer timeIDInt, final Function function, final boolean removeTask) {
      super(window);
      this.timeIDInt = timeIDInt;
      this.functionRef = new WeakReference<>(function);
      this.removeTask = removeTask;
    }

    public void actionPerformed(final ActionEvent e) {
      System.out.println("Timer ID fired: " + timeIDInt + ", oneshot: " + removeTask);
      // This executes in the GUI thread and that's good.
      try {
        final Window window = this.getWindow();
        if (window == null) {
          if (logger.isLoggable(Level.INFO)) {
            logger.info("actionPerformed(): Window is no longer available.");
          }
          return;
        }
        if (this.removeTask) {
          window.forgetTask(this.timeIDInt, false);
        }
        final HTMLDocumentImpl doc = (HTMLDocumentImpl) window.getDocument();
        if (doc == null) {
          throw new IllegalStateException("Cannot perform operation when document is unset.");
        }
        final Function function = this.functionRef.get();
        if (function == null) {
          throw new IllegalStateException("Cannot perform operation. Function is no longer available.");
        }
        window.addJSTaskUnchecked(new JSRunnableTask(0, "timer task for id: " + timeIDInt + ", oneshot: " + removeTask, () -> {
          Executor.executeFunction(window.getWindowScope(), function, doc.getDocumentURL(), window.getUserAgentContext(),
              window.windowFactory);
        }));
        // Executor.executeFunction(window.getWindowScope(), function, doc.getDocumentURL(), window.getUserAgentContext(), window.windowFactory);
      } catch (final Throwable err) {
        logger.log(Level.WARNING, "actionPerformed()", err);
      }
    }
  }

  private static class ExpressionTimerTask extends WeakWindowTask {
    // Implemented as a static WeakWindowTask to allow the Window
    // to get garbage collected, especially in infinite loop
    // scenarios.
    private final Integer timeIDInt;
    private final String expression;
    private final boolean removeTask;

    public ExpressionTimerTask(final Window window, final Integer timeIDInt, final String expression, final boolean removeTask) {
      super(window);
      this.timeIDInt = timeIDInt;
      this.expression = expression;
      this.removeTask = removeTask;
    }

    public void actionPerformed(final ActionEvent e) {
      // This executes in the GUI thread and that's good.
      try {
        final Window window = this.getWindow();
        if (window == null) {
          if (logger.isLoggable(Level.INFO)) {
            logger.info("actionPerformed(): Window is no longer available.");
          }
          return;
        }
        if (this.removeTask) {
          window.forgetTask(this.timeIDInt, false);
        }
        final HTMLDocumentImpl doc = (HTMLDocumentImpl) window.getDocument();
        if (doc == null) {
          throw new IllegalStateException("Cannot perform operation when document is unset.");
        }
        window.addJSTaskUnchecked(new JSRunnableTask(0, "timer task for id: " + timeIDInt, () -> {
          window.evalInScope(this.expression);
        }));
        // window.evalInScope(this.expression);
      } catch (final Throwable err) {
        logger.log(Level.WARNING, "actionPerformed()", err);
      }
    }
  }

  private static class TaskWrapper {
    public final Timer timer;
    private final Object retained;

    public TaskWrapper(final Timer timer, final Object retained) {
      super();
      this.timer = timer;
      this.retained = retained;
    }
  }

  public void addEventListener(final String type, final Function listener) {
    addEventListener(type, listener, false);
  }

  private final EventTargetManager eventTargetManager = new EventTargetManager(this);

  public EventTargetManager getEventTargetManager() {
    return eventTargetManager;
  }

  public void addEventListener(final String type, final Function listener, final boolean useCapture) {
    if (useCapture) {
      throw new UnsupportedOperationException();
    }
    /*
    // TODO: Should this delegate completely to document
    if ("load".equals(type)) {
      document.addLoadHandler(listener);
    } else {
      document.addEventListener(type, listener);
    }*/
    System.out.println("window Added listener for: " + type);
    eventTargetManager.addEventListener(document, type, listener);
  }

  public void removeEventListener(final String type, final Function listener, final boolean useCapture) {
    // TODO: Should this delegate completely to document
    if ("load".equals(type)) {
      document.removeLoadHandler(listener);
    }
    eventTargetManager.removeEventListener(document, type, listener, useCapture);
  }

  public boolean dispatchEvent(final Event evt) throws EventException {
    // TODO
    System.out.println("TODO: window dispatch event");
    eventTargetManager.dispatchEvent(document, evt);
    return false;
  }

  // TODO: Hide from JS
  public void domContentLoaded(final Event domContentLoadedEvent) {
    eventTargetManager.dispatchEvent(document, domContentLoadedEvent);
  }

  private Function onWindowLoadHandler;

  // private Function windowLoadListeners;

  // TODO: Hide from JS
  // TODO: Move job scheduling logic into Window class
  // private AtomicBoolean jobsOver = new AtomicBoolean(false);
  public void jobsFinished() {
    final Event windowLoadEvent = new Event("load", document);
    eventTargetManager.dispatchEvent(document, windowLoadEvent);

    final Function handler = this.onWindowLoadHandler;
    if (handler != null) {
      addJSTask(new JSRunnableTask(0, new Runnable() {
        public void run() {
          Executor.executeFunction(document, handler, windowLoadEvent, windowFactory);
        }
      }));
      // Executor.executeFunction(document, handler, windowLoadEvent);
    }

    if (jobFinishedHandler != null) {
      jobFinishedHandler.run();
    }
    // jobsOver.set(true);
  }

  private volatile Runnable jobFinishedHandler = null;

  // TODO: ensure not accessible from JS
  public void setJobFinishedHandler(final Runnable handler) {
    jobFinishedHandler = handler;
  }

  /*
  @PropertyName("Element")
  public Class<Element> getElement() {
    return Element.class;
  }*/

  /* changed from above For prototype.js */
  private Object element = Element.class;

  @PropertyName("Element")
  public Object getElement() {
    return element;
  }

  @PropertyName("Element")
  public void setElement(final Object o) {
    System.out.println("Setting element to: " + o);
    element = o;
  }

  @PropertyName("Node")
  public Class<Node> getNode() {

    return Node.class;
  }

  public void addEventListener(final String type, final EventListener listener) {
    addEventListener(type, listener, false);
  }

  public void addEventListener(final String type, final EventListener listener, final boolean useCapture) {
    if (useCapture) {
      throw new UnsupportedOperationException();
    }
    // TODO Auto-generated method stub
    // throw new UnsupportedOperationException();
    eventTargetManager.addEventListener(document, type, listener, useCapture);
  }

  public void removeEventListener(final String type, final EventListener listener, final boolean useCapture) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean dispatchEvent(final org.w3c.dom.events.Event evt) throws EventException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

}