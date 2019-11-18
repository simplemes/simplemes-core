package org.simplemes.eframe.preference.event
/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This defines the interface used by the User preferences handlers to respond to user GUI state events.
 *
 */
interface GUIEventInterface {
  /**
   * Handles a GUI event.
   * @param request The HTTP request, with parameters.
   */
  void handleEvent(Map params)

}
