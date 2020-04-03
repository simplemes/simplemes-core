package org.simplemes.mes.demand

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.demand.domain.Order

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class CompleteUndoActionSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [SERVER]

  @SuppressWarnings("GroovyAssignabilityCheck")
  @Rollback
  def "verify that the copy constructor for StartResponse works"() {
    given: 'an order'
    def order = new Order(order: 'M1001', qtyToBuild: 1.2).save()

    when: 'the copy constructor is called'
    def completeUndoAction = new CompleteUndoAction(new CompleteResponse(order: order, qty: 1.2, operationSequence: 247))
    //println "startUndoAction = $startUndoAction"

    then: 'the values for undo action are correct'
    completeUndoAction.URI == '/work/reverseComplete'
    completeUndoAction.infoMsg == GlobalUtils.lookup('reversedComplete.message', order.order, 1.2)

    and: 'the JSON is correct'
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(startUndoAction.JSON)}"
    def completeRequest = Holders.objectMapper.readValue(completeUndoAction.JSON, CompleteRequest) as CompleteRequest
    completeRequest.order == order
    completeRequest.qty == 1.2
    completeRequest.operationSequence == 247

    and: 'the success events are correct'
    completeUndoAction.successEvents.size() == 1
    completeUndoAction.successEvents[0].type == 'ORDER_LSN_STATUS_CHANGED'
    completeUndoAction.successEvents[0].source == 'CompleteUndoAction'
    completeUndoAction.successEvents[0].list[0].order == order.order
  }
}
