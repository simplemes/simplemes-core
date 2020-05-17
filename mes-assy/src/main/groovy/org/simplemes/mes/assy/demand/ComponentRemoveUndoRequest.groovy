package org.simplemes.mes.assy.demand

import groovy.transform.ToString
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.json.JSONByKey
import org.simplemes.eframe.misc.ArgumentUtils
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
  @JSONByKey
  Order order

  /**
   * The Sequence of the OrderAssembledComponent to mark as assembled.
   */
  Integer sequence

  /**
   * A copy constructor to undo the removal of a single component on an order/LSN.
   * @param orderAssembledComponent The assembled component removed from the order to be undone.
   * @param order the Order this component was assembled into.
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

  /**
   * Main constructor for use with a controller's HTTP parameters.  Validates the inputs, but does not fail if no order is
   * given.
   * @param params The HTTP parameters (order and sequence as string)
   */
  ComponentRemoveUndoRequest(Map params) {
    if (params.order) {
      order = Order.findByOrder(params.order as String)
      if (!order) {
        //error.110.message=Could not find {0} {1}
        throw new BusinessException(110, [GlobalUtils.lookup('order.label'), params.order])
      }
    }

    sequence = ArgumentUtils.convertToInteger(params.sequence)

  }

}
