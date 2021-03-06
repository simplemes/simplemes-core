package org.simplemes.mes.demand.domain

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.product.domain.ProductOperation

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class OrderOperStateSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [OrderOperState, Order]

  def "test constraints"() {
    given: 'an Order'
    def order = new Order(order: 'M1001')

    expect: 'the constraints are enforced'
    DomainTester.test {
      domain OrderOperState
      requiredValues order: order, sequence: 437
      fieldOrderCheck false
    }
  }

  @Rollback
  def "verify that the copy constructor works"() {
    given: 'a routing operation'
    def op1 = new ProductOperation(sequence: 100, title: 'XYZ')

    when: 'the copy constructor is used'
    def opState = new OrderOperState(op1)

    then: 'the fields are copied'
    opState.sequence == op1.sequence
    opState.dateFirstQueued == null
    opState.dateFirstStarted == null
  }

  def "verify that determineNextWorkable is delegated to Order"() {
    given: 'a mocked Order'
    def order = Mock(Order)
    def opState = new OrderOperState(order: order, sequence: 237)

    when: 'the method is called'
    def res = opState.determineNextWorkable()

    then: 'the Order method is used'
    1 * order.determineNextWorkable(opState) >> order

    and: 'it returned the value from the order method'
    order == res
  }

  @Rollback
  def "verify that saveChanges saves this oper state"() {
    given: 'an Order'
    def order = new Order(order: 'M1001')

    when: 'the method is called'
    def opState = new OrderOperState(order: order, sequence: 237)
    assert opState.uuid == null
    opState.saveChanges()

    then: 'the record was saved'
    OrderOperState.findByUuid(opState.uuid)
  }

  @Rollback
  def "verify that toString works"() {
    given: 'an order'
    def order = new Order(order: 'M1001')

    when: 'the method is called'
    def opState = new OrderOperState(order: order, sequence: 237)

    then: 'the string is valid'
    opState.toString().contains('M1001')
    opState.toString().contains('237')
  }


}
