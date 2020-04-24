package org.simplemes.mes.assy.demand

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.demand.domain.Order

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ComponentRemoveUndoRequestSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  @Rollback
  def "verify that the Map constructor works - Order"() {
    given: 'an order'
    def order = new Order(order: 'ABC').save()

    when: 'the constructor is used'
    def request = new ComponentRemoveUndoRequest([order: order.order, sequence: 237])

    then: 'the order is used'
    request.order == order
    request.sequence == 237
  }

  @Rollback
  def "verify that the Map constructor gracefully handles missing order"() {
    when: 'the constructor is used'
    def request = new ComponentRemoveUndoRequest([:])

    then: 'the request is created as empty'
    !request.order
  }


}
