package org.simplemes.mes.assy.demand

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.mes.assy.demand.domain.OrderAssembledComponent
import org.simplemes.mes.demand.domain.Order

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class RemoveOrderAssembledComponentRequestSpec extends BaseSpecification {

  def "verify that the copy constructor works"() {
    given: 'an OrderAssembledComponent record'
    def order = new Order(order: 'M1001')
    def comp = new OrderAssembledComponent(sequence: 247)

    when: 'the constructor is called'
    def request = new RemoveOrderAssembledComponentRequest(comp, order)

    then: 'the values are correct'
    request.order == order
    request.sequence == 247
  }
}
