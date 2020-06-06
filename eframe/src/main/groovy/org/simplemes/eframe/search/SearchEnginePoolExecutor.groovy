/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import groovy.util.logging.Slf4j
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.misc.LogUtils

import java.util.concurrent.BlockingQueue
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * The thread pool executor that provides background processing of search engine requests.
 * This works in conjunction with the bound SearchEngineRequestQueue to process most index requests
 * from object updates.  The behavior and limits such as pool sizes are controlled by configuration values.
 */
@Slf4j
class SearchEnginePoolExecutor extends ThreadPoolExecutor implements RejectedExecutionHandler {

  /**
   * The thread factory.
   */
  private ThreadFactory threadFactory

  @SuppressWarnings("ThisReferenceEscapesConstructor")
  SearchEnginePoolExecutor(int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit timeUnits, BlockingQueue<Runnable> queue) {
    super(corePoolSize, maxPoolSize, keepAliveTime, timeUnits, queue)
    setRejectedExecutionHandler(this)
  }

  @Override
  void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
    try {
      executor.queue.put(r)
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt()
    }
  }

  /**
   * This afterExecute hook mainly detects exceptions and logs them.
   * @param r the runnable that has completed
   * @param t the exception that caused termination, or null if
   * execution completed normally
   */
  @Override
  protected void afterExecute(Runnable r, Throwable t) {
    SearchHelper.instance.finishedRequest()
    super.afterExecute(r, t)
    if (t == null && r.hasProperty('outcome')) {
      t = r.outcome
    }
    if (t) {
      SearchHelper.instance.requestFailed()
      LogUtils.logStackTrace(log, t, r)
      log.error('Exception in background task: {}', t.toString())
    }
  }

  /**
   * A thread-safe way to increment the request count.
   */

  /**
   * Returns the thread factory used to create new threads.
   *
   * @return the current thread factory
   * @see #setThreadFactory(ThreadFactory)
   */
  @Override
  ThreadFactory getThreadFactory() {
    threadFactory = threadFactory ?: new SearchThreadFactory()
    return threadFactory
  }

  /**
   * Determines if the pool is idle and has no requests in queue.
   * @return true if no threads are active and no requests are queued.
   */
  boolean isIdle() {
    return (pool.activeCount == 0) && (queue.size() == 0)
  }

  /**
   * The current pool in use.  Like a singleton, that is created when the pool is started.
   */
  static SearchEnginePoolExecutor pool

  /**
   * Adds a request to the queue for processing.
   * @param request The request.
   */
  static Future<?> addRequest(SearchEngineRequestInterface request) {
    log.trace('addRequest: adding {}', request)
    pool.submit(request)
  }

  /**
   * Creates and starts the execution pool.
   */
  static void startPool() {
    pool?.shutdown()  // Make sure any old queue is shutdown
    pool = new SearchEnginePoolExecutor(determineThreadInitSize(), determineThreadMaxSize(),
                                        10, TimeUnit.SECONDS, new SearchEngineRequestQueue())
  }

  /**
   * Shuts down the pool. <b>Use only in tests.</b>
   */
  static void shutdownPool() {
    pool?.shutdown()
    pool = null
  }

  /**
   * Waits for the queue to be empty and the threads to be all idle.
   * <b>Use only in tests.</b>  Will poll and check every 50ms for idle status.
   */
  static waitForIdle() {
    if (!pool) {
      return
    }
    while (!pool.idle) {
      sleep(50)
    }
  }

  /**
   * Determines the initial size of the thread pool (coreSize).
   * Uses the configuration entry <code>org.simplemes.eframe.search.threadPool.coreSize</code>.
   * @return The core size.  Default: 4
   */
  static Integer determineThreadInitSize() {
    Integer coreSize = 4
    def configValue = Holders.configuration.search.threadInitSize
    if (configValue instanceof Integer) {
      coreSize = configValue
    }
    return coreSize
  }

  /**
   * Determines the initial size of the thread pool (coreSize).
   * Uses the configuration entry <code>org.simplemes.eframe.search.threadPool.coreSize</code>.
   * @return The core size.  Default: 10
   */
  static Integer determineThreadMaxSize() {
    Integer coreSize = 10
    def configValue = Holders.configuration.search.threadMaxSize
    if (configValue instanceof Integer) {
      coreSize = configValue
    }
    return coreSize
  }

  /**
   * The thread factory for the pool.  The thread names starts with 'search'.
   */
  @SuppressWarnings(["UnnecessaryGetter", "FieldName"])
  static class SearchThreadFactory implements ThreadFactory {
    // This mirrors the DefaultThreadFactory in the JDK, but a distinct thread name was desired.
    private static final AtomicInteger poolNumber = new AtomicInteger(1)
    private final ThreadGroup group
    private final AtomicInteger threadNumber = new AtomicInteger(1)
    private final String namePrefix

    SearchThreadFactory() {
      SecurityManager s = System.securityManager
      group = (s != null) ? s.threadGroup : Thread.currentThread().threadGroup
      namePrefix = "search-" + poolNumber.getAndIncrement() + "-"
    }

    Thread newThread(Runnable r) {
      Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0)
      if (t.isDaemon()) {
        t.setDaemon(false)
      }
      if (t.getPriority() != Thread.NORM_PRIORITY) {
        t.setPriority(Thread.NORM_PRIORITY)
      }
      return t
    }
  }
}

