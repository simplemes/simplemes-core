package sample

import groovy.transform.ToString
import org.simplemes.eframe.web.undo.UndoActionInterface
import org.simplemes.eframe.web.undo.UndoableInterface

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The start result response POGO (sample).
 */
@ToString(includeNames = true, includePackage = false)
class StartResponse implements UndoableInterface {
  String order
  BigDecimal qty
  Date dateStarted = new Date()
  String userName
  Integer counter

  /**
   * The undo action(s) needed to undo the previous user action.
   */
  @Override
  List<UndoActionInterface> getUndoActions() {
    return [new StartUndoAction(this)]
  }
}
