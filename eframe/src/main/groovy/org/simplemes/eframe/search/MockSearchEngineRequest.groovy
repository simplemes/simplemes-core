/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import groovy.transform.ToString

/**
 * A mock request used for testing.  Can trigger an exception or record that it was successfully run() properly.
 * <b>Use only in tests.</b>
 */
@ToString(includePackage = false, includeNames = true)
class MockSearchEngineRequest implements SearchEngineRequestInterface {

  /**
   * Set to true if the run() method was called.
   */
  boolean runWasCalled = false

  /**
   * The exception message. Optional.
   */
  String exceptionMsg

  /**
   * The number of milliseconds to sleep on each run.
   */
  int sleep = 0

  /**
   * This executes the indexObject action on the external search server.
   */
  @Override
  void run() {
    runWasCalled = true
    if (sleep) {
      Thread.sleep(sleep)
    }
    if (exceptionMsg) {
      throw new IllegalArgumentException(exceptionMsg)
    }
  }
}
