package org.simplemes.mes.assy.demand

import groovy.transform.ToString
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.web.undo.UndoActionInterface
import org.simplemes.mes.assy.demand.domain.OrderAssembledComponent
import org.simplemes.mes.demand.domain.Order

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the action needed to undo a single start action.
 */
@ToString(includePackage = false, includeNames = true)
class AssembledComponentUndoAction implements UndoActionInterface {
  /**
   * The URI to submit the JSON content to in order to undo a previous action.
   */
  String uri = '/orderAssy/removeComponent'

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
   * A copy constructor to undo the addition of a single component to an order/LSN.
   * @param orderAssembledComponent The assembled component added to the order to be undone.
   * @param order the Order this component was assembled into.  This is required here since the orderAssembledComponent.orderId is not
   *        populated until the transaction commit.
   */
  AssembledComponentUndoAction(OrderAssembledComponent orderAssembledComponent, Order order) {
    //reversedAssembly.message=Removed component {0} from {1}.
    infoMsg = GlobalUtils.lookup('reversedAssemble.message', orderAssembledComponent.component, order?.order)

    def request = new RemoveOrderAssembledComponentRequest(orderAssembledComponent, order)
    json = Holders.objectMapper.writeValueAsString(request)
    successEvents << [type       : AssembledComponentAction.TYPE_ORDER_COMPONENT_STATUS_CHANGED,
                      source     : AssembledComponentUndoAction.simpleName,
                      order      : order?.order,
                      component  : orderAssembledComponent.component.product,
                      bomSequence: orderAssembledComponent.bomSequence]
  }

}
