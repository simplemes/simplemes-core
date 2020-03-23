package org.simplemes.mes.demand

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
@SuppressWarnings(["FieldName", "GetterMethodCouldBeProperty"])
@ToString(includePackage = false, includeNames = true)
class StartUndoAction implements UndoActionInterface {
  /**
   * The URI to submit the JSON content to in order to undo a previous action.
   */
  protected String URI = '/work/reverseStart'

  /**
   * The JSON content to submit in order to undo a previous action.
   */
  protected String JSON

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
    infoMsg = GlobalUtils.lookup('reversedStart.message', startResponse.order?.order, startResponse.qty)

    def startRequest = new StartRequest(order: startResponse.order, qty: startResponse.qty,
                                        operationSequence: startResponse.operationSequence)
    //println "startRequest = $startRequest"
    JSON = Holders.objectMapper.writeValueAsString(startRequest)
    def list = [[order: startResponse.order?.order]]
    successEvents << [type: 'ORDER_LSN_STATUS_CHANGED', list: list, source: 'StartUndoAction']
  }

  /**
   * The URI to submit the JSON content to in order to undo a previous action.
   */
  @Override
  String getUri() {
    return URI
  }

  /**
   * The JSON content to submit in order to undo a previous action.
   */
  @Override
  String getJson() {
    return JSON
  }
}
