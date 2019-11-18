package org.simplemes.eframe.test

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the methods need by a mock object that handles its own cleanup.  
 */
interface AutoCleanupMockInterface {

  /**
   * Performs the cleanup action.
   * @param testSpec The test that requests the cleanup.
   */
  void doCleanup(BaseSpecification testSpec)

}