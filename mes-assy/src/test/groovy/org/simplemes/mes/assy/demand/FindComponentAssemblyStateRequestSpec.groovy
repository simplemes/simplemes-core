package org.simplemes.mes.assy.demand

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class FindComponentAssemblyStateRequestSpec extends BaseSpecification {

  def "verify that the order constructor works"() {
    given: 'an order'
    def order = new Order(order: 'ABC')

    when: 'the constructor is used'
    def request = new FindComponentAssemblyStateRequest(order)

    then: 'the order is used'
    request.demand == order
  }

  def "verify that the LSN constructor works"() {
    given: 'an LSN'
    def lsn = new LSN(lsn: 'ABC')

    when: 'the constructor is used'
    def request = new FindComponentAssemblyStateRequest(lsn)

    then: 'the lsn is used'
    request.demand == lsn
  }

}
