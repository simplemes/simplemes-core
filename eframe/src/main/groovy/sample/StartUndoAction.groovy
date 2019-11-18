package sample

import groovy.transform.ToString
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.web.undo.UndoActionInterface

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the action needed to undo a single start action.
 */
@ToString(includePackage = false, includeNames = true)
class StartUndoAction implements UndoActionInterface {
  /**
   * The URI to submit the JSON content to in order to undo a previous action.
   */
  String uri = '/sample/dashboard/undoStart'

  /**
   * The JSON content to submit in order to undo a previous action.
   */
  String json

  /**
   * The message to be displayed when the user triggers this undo action.
   */
  String infoMsg

  /**
   * Optional dashboard events to trigger after the undo action completes successfully.
   */
  List<Map> successEvents = []

  /**
   * A copy constructor.
   * @param startResponse The start response this undo action was generated from.
   */
  StartUndoAction(StartResponse startResponse) {
    infoMsg = GlobalUtils.lookup('reversedStart.message', startResponse.order, startResponse.qty)

    def startRequest = new StartRequest(order: startResponse.order, qty: startResponse.qty)
    json = Holders.objectMapper.writeValueAsString(startRequest)
    def list = [[order: startResponse.order]]
    successEvents << [type: 'ORDER_STATUS_CHANGED', list: list, source: 'StartUndoAction']
  }

}
