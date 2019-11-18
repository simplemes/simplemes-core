package org.simplemes.eframe.web.undo

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines a request that can be un-done using one or more undoActions.
 */
interface UndoableInterface {

  /**
   * The undo action(s) needed to undo the previous user action.
   */
  List<UndoActionInterface> getUndoActions()

}
