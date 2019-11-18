package sample.service

import sample.ProductionInterface
import sample.StartRequest
import sample.StartResponse

import javax.inject.Singleton

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A sample extension that handles Start actions.
 */
@Singleton
class StartExtension implements ProductionInterface {

  /**
   * Called when an order is started (sample code).
   * @param startRequest The order start request.
   * @param startResponse The response from the core start logic.
   */
  @Override
  void started(StartRequest startRequest, StartResponse startResponse, boolean fail) {
    //println "StartExtensionstarted() called: startRequest = $startRequest, startResponse = $startResponse"
    if (fail) {
      throw new IllegalArgumentException('Fail requested for test')
    }
  }
}
