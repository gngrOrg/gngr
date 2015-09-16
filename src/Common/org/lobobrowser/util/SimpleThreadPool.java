package org.lobobrowser.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A thread pool that allows cancelling all running tasks without shutting down
 * the thread pool.
 */
public class SimpleThreadPool {
  private static final Logger logger = Logger.getLogger(SimpleThreadPool.class.getName());
  private final LinkedList<SimpleThreadPoolTask> taskList = new LinkedList<>();
  private final Set<SimpleThreadPoolTask> runningSet = new HashSet<>();
  private final int minThreads;
  private final int maxThreads;
  private final String name;
  private final int idleAliveMillis;
  private final Object taskMonitor = new Object();
  private final ThreadGroup threadGroup;

  private int numThreads = 0;
  private int numIdleThreads = 0;
  private int threadNumber = 0;

  public SimpleThreadPool(final String name, final int minShrinkToThreads, final int maxThreads, final int idleAliveMillis) {
    this.minThreads = minShrinkToThreads;
    this.maxThreads = maxThreads;
    this.idleAliveMillis = idleAliveMillis;
    this.name = name;
    // Thread group needed so item requests
    // don't get assigned sub-thread groups.
    // TODO: Thread group needs to be thought through. It's retained in
    // memory, and we need to return the right one in the GUI thread as well.
    this.threadGroup = null; // new ThreadGroup(name);
  }

  public void schedule(final SimpleThreadPoolTask task) {
    if (task == null) {
      throw new IllegalArgumentException("null task");
    }
    final Object monitor = this.taskMonitor;
    synchronized (monitor) {
      if (this.numIdleThreads == 0) {
        this.addThreadImpl();
      }
      this.taskList.add(task);
      monitor.notify();
    }
  }

  public void cancel(final SimpleThreadPoolTask task) {
    synchronized (this.taskMonitor) {
      this.taskList.remove(task);
    }
    task.cancel();
  }

  private void addThreadImpl() {
    if (this.numThreads < this.maxThreads) {
      final Thread t = new Thread(this.threadGroup, new ThreadRunnable(), this.name + this.threadNumber++);
      t.setDaemon(true);
      t.start();
      this.numThreads++;
    }
  }

  /**
   * Cancels all waiting tasks and any currently running task.
   */
  public void cancelAll() {
    synchronized (this.taskMonitor) {
      this.taskList.clear();
      final Iterator<SimpleThreadPoolTask> i = this.runningSet.iterator();
      while (i.hasNext()) {
        i.next().cancel();
      }
    }
  }

  private class ThreadRunnable implements Runnable {
    public void run() {
      final Object monitor = taskMonitor;
      final LinkedList<SimpleThreadPoolTask> tl = taskList;
      final Set<SimpleThreadPoolTask> rs = runningSet;
      final int iam = idleAliveMillis;
      SimpleThreadPoolTask task = null;
      for (;;) {
        try {
          synchronized (monitor) {
            if (task != null) {
              rs.remove(task);
            }
            numIdleThreads++;
            try {
              long waitBase = System.currentTimeMillis();
              INNER: while (tl.isEmpty()) {
                final long maxWait = iam - (System.currentTimeMillis() - waitBase);
                if (maxWait <= 0) {
                  if (numThreads > minThreads) {
                    // Should be only way to exit thread.
                    numThreads--;
                    return;
                  } else {
                    waitBase = System.currentTimeMillis();
                    continue INNER;
                  }
                }
                monitor.wait(maxWait);
              }
            } finally {
              numIdleThreads--;
            }
            task = taskList.removeFirst();
            rs.add(task);
          }
          final Thread currentThread = Thread.currentThread();
          final String baseName = currentThread.getName();
          try {
            try {
              currentThread.setName(baseName + ":" + task.toString());
            } catch (final Exception thrown) {
              logger.log(Level.WARNING, "run(): Unable to set task name.", thrown);
            }
            try {
              task.run();
            } catch (final Exception thrown) {
              logger.log(Level.SEVERE, "run(): Error in task: " + task + ".", thrown);
            }
          } finally {
            currentThread.setName(baseName);
          }
        } catch (final Exception thrown) {
          logger.log(Level.SEVERE, "run(): Error in thread pool: " + SimpleThreadPool.this.name + ".", thrown);
        }
      }
    }
  }
}
