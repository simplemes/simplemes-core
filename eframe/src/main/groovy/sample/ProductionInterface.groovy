package sample

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A sample interface implemented by production extensions. Used to test calling extensions directly.
 */
interface ProductionInterface {

  /**
   * Called when an order is started (sample code).
   * @param startRequest The order start request.
   * @param startResponse The response from the core start logic.
   */
  void started(StartRequest startRequest, StartResponse startResponse, boolean fail)
}
