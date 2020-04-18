package org.simplemes.mes.assy.demand

import groovy.transform.ToString
import org.simplemes.mes.assy.demand.domain.OrderAssembledComponent
import org.simplemes.mes.demand.domain.Order

/*
 * Copyright Michael Houston 2016. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This class is the request to undo a single component removal action for the order.
 * This request marks the record as assembled..
 *
 */
@ToString(includeNames = true, includePackage = false)
class ComponentRemoveUndoRequest {

  /**
   * The Sequence of the OrderAssembledComponent to mark as assembled.
   */
  Order order

  /**
   * The Sequence of the OrderAssembledComponent to mark as assembled.
   */
  Integer sequence

  /**
   * A copy constructor to undo the removal of a single component on an order/LSN.
   * @param orderAssembledComponent The assembled component removed from the order to be undone.
   * @param order the Order this component was assembled into.  This is required here since the orderAssembledComponent.orderId is not
   *        populated until the transaction commit.
   */
  ComponentRemoveUndoRequest(OrderAssembledComponent orderAssembledComponent, Order order) {
    sequence = orderAssembledComponent.sequence
    this.order = order
  }

  /**
   * The no argument constructor.
   */
  ComponentRemoveUndoRequest() {

  }


}
