package org.simplemes.eframe.web.undo

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines a single undo action to that will undo a previous user action.
 */
@SuppressWarnings("GroovyUnusedDeclaration")
interface UndoActionInterface {

  /**
   * The URI to submit the JSON content to in order to undo a previous action.
   */
  String getUri()

  /**
   * The JSON content to submit in order to undo a previous action.
   */
  String getJson()

  /**
   * Returns an optional  message to be displayed when the user triggers this undo action.
   * @return The info message.  A null value if not needed.
   */
  String getInfoMsg()

  /**
   * Returns optional dashboard events to trigger after the undo action completes successfully.
   * @return The list of events. Null if none are needed.
   */
  List<Map> getSuccessEvents()


}
