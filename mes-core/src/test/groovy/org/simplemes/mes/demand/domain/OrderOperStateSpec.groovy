package org.simplemes.mes.demand.domain

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.mes.product.domain.MasterOperation
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.test.MESUnitTestUtils
import org.simplemes.mes.tracking.domain.ActionLog

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class OrderOperStateSpec extends BaseSpecification {

  /**
   * The domain class records to clean up after each run.
   */
  @SuppressWarnings("GroovyUnusedDeclaration")
  static dirtyDomains = [ActionLog, Order, Product]

  def "verify that workStateTrait method saveChanges forces update of record for operation start"() {
    given: 'a simulated user'
    setCurrentUser()

    and: 'an order that has qty in queue'
    Order order = null
    Order.withTransaction {
      order = MESUnitTestUtils.releaseOrder(qty: 1, operations: [1])
    }

    when: 'the order is started and saved'
    Order.withTransaction {
      def order2 = Order.findByOrder(order.order)
      order2.operationStates[0].startQty(1.0)
      assert order2.operationStates[0].qtyInWork == 1.0
    }

    then: 'the order is updated in the db'
    Order.withTransaction {
      def order3 = Order.findByOrder(order.order)
      assert order3.operationStates[0].qtyInWork == 1.0
      true
    }
  }

  def "test copy constructor from a RoutingOperation"() {
    given: 'a routing operation object to copy from'
    def routingOperation = new MasterOperation(sequence: 100, title: 'XYZ')

    when: 'the constructor is used'
    def orderOperState = new OrderOperState(routingOperation)

    then: 'the values are copied'
    routingOperation.sequence == orderOperState.sequence
  }

  def "test determineNextWorkable()"() {
    given: 'an OrderOperState with an order'
    def order = Mock(Order)
    def orderOperState = new OrderOperState(qtyInQueue: 0.0, sequence: 100)
    orderOperState.order = order

    and: 'a dummy operation state'
    def dummyOrderOperState = new OrderOperState(sequence: 101)

    when: 'the next workable is called'
    def nextWorkable = orderOperState.determineNextWorkable()

    then: 'the return value is passed back to the caller'
    nextWorkable == dummyOrderOperState

    and: 'the task is delegated to the Order'
    1 * order.determineNextWorkable(orderOperState) >> dummyOrderOperState
  }

  def "test determineNextWorkable() with null order"() {
    given: 'an OrderOperState with a null Order'
    def orderOperState = new OrderOperState(qtyInQueue: 0.0, sequence: 100)

    when: 'the next workable is called'
    def nextWorkable = orderOperState.determineNextWorkable()

    then: 'no next workable is found'
    nextWorkable == null
  }

  def "test toString() with null Order"() {
    given: 'an LSNOperState with a null Order'
    def orderOperState = new OrderOperState(sequence: 100)

    expect: 'the sequence is used'
    orderOperState.toString().contains('100')
  }

  def "test toString() with an Order"() {
    given: 'an LSNOperState with an LSN'
    def orderOperState = new OrderOperState(sequence: 127)
    orderOperState.order = new Order(order: 'M4512')

    expect: 'the sequence and Order are used'
    orderOperState.toString().contains('M4512')
    orderOperState.toString().contains('127')
  }


}
