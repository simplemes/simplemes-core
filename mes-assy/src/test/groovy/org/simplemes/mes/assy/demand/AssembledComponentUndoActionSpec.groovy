package org.simplemes.mes.assy.demand


import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.assy.test.AssyUnitTestUtils

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 *
 */
@SuppressWarnings("GroovyAssignabilityCheck")
class AssembledComponentUndoActionSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  @Rollback
  def "verify that copy constructor works with a valid component"() {
    given: 'a released order with a components assembled'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU'])
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10])

    when: 'the undo action is created'
    def comp = order.assembledComponents[0]
    def action = new AssembledComponentUndoAction(comp, order)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(action.JSON)}"

    then: 'the undo action has the right data'
    action.infoMsg == GlobalUtils.lookup('reversedAssemble.message', comp.component, order?.order)
    action.uri.contains('/orderAssy/removeComponent')
    def undoRequest = Holders.objectMapper.readValue(action.json, RemoveOrderAssembledComponentRequest)
    undoRequest.sequence == comp.sequence
    undoRequest.order == order

    and: 'the success event is correct'
    action.successEvents.size() == 1
    def event = action.successEvents[0]
    event.type == AssembledComponentAction.TYPE_ORDER_COMPONENT_STATUS_CHANGED
    event.source.contains(AssembledComponentUndoAction.simpleName)
    event.order == order.order
    event.component == comp.component.product
    event.bomSequence == comp.bomSequence
  }
}
