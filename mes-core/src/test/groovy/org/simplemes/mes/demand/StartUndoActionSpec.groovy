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
class StartUndoActionSpec extends BaseSpecification {

  static specNeeds = [SERVER, JSON]

  @SuppressWarnings("GroovyAssignabilityCheck")
  @Rollback
  def "verify that the copy constructor for StartResponse works"() {
    given: 'an order'
    def order = new Order(order: 'M1001', qtyToBuild: 1.2).save()

    when: 'the copy constructor is called'
    def startUndoAction = new StartUndoAction(new StartResponse(order: order, qty: 1.2, operationSequence: 247))
    //println "startUndoAction = $startUndoAction"

    then: 'the values for undo action are correct'
    startUndoAction.URI == '/work/reverseStart'
    startUndoAction.infoMsg == GlobalUtils.lookup('reversedStart.message', order.order, 1.2)

    and: 'the JSON is correct'
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(startUndoAction.JSON)}"
    def startRequest = Holders.objectMapper.readValue(startUndoAction.JSON, StartRequest) as StartRequest
    startRequest.order == order
    startRequest.qty == 1.2
    startRequest.operationSequence == 247

    and: 'the success events are correct'
    startUndoAction.successEvents.size() == 1
    startUndoAction.successEvents[0].type == 'ORDER_LSN_STATUS_CHANGED'
    startUndoAction.successEvents[0].source == 'StartUndoAction'
    startUndoAction.successEvents[0].list[0].order == order.order
  }
}
