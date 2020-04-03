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
 * Defines the action needed to undo a single complete action.
 */
@SuppressWarnings(["FieldName", "GetterMethodCouldBeProperty"])
@ToString(includePackage = false, includeNames = true)
class CompleteUndoAction implements UndoActionInterface {
  /**
   * The URI to submit the JSON content to in order to undo a previous action.
   */
  protected String URI = '/work/reverseComplete'

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
   * @param completeResponse The complete response this undo action was generated from.
   */
  CompleteUndoAction(CompleteResponse completeResponse) {
    infoMsg = GlobalUtils.lookup('reversedComplete.message', completeResponse.order?.order, completeResponse.qty)

    def completeRequest = new CompleteRequest(order: completeResponse.order, qty: completeResponse.qty,
                                              operationSequence: completeResponse.operationSequence)
    //println "startRequest = $startRequest"
    JSON = Holders.objectMapper.writeValueAsString(completeRequest)
    def list = [[order: completeResponse.order?.order]]
    successEvents << [type: 'ORDER_LSN_STATUS_CHANGED', list: list, source: 'CompleteUndoAction']
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
