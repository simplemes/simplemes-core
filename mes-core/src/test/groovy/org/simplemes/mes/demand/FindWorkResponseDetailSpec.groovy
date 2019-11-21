package org.simplemes.mes.demand

import grails.gorm.transactions.Rollback
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.LSNOperState
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.domain.OrderOperState
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.tracking.domain.ActionLog
import org.simplemes.mes.tracking.domain.ProductionLog

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class FindWorkResponseDetailSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [ActionLog, ProductionLog, Order, Product]

  @Rollback
  def "test order copy constructor"() {
    given: 'an order'
    def order = new Order(order: 'ABC', qtyInQueue: 23.2, qtyInWork: 14.2, id: 247)

    when: 'the detail is created from the order'
    def detail = new FindWorkResponseDetail(order)

    then: 'the qty/order is copied'
    detail.order == order.order
    detail.orderID == order.id
    detail.qtyInQueue == order.qtyInQueue
    detail.qtyInWork == order.qtyInWork
  }

  @Rollback
  def "test LSN copy constructor"() {
    given: 'an LSN'
    def order = new Order(order: 'ABC', qtyInQueue: 23.2, qtyInWork: 14.2, id: 137)
    def lsn = new LSN(lsn: 'SN237', order: order, qtyInQueue: 33.2, qtyInWork: 74.2, id: 237)

    when: 'the detail is created from the order'
    def detail = new FindWorkResponseDetail(lsn)

    then: 'the qty is copied'
    detail.lsn == lsn.lsn
    detail.lsnID == lsn.id
    detail.order == lsn.order.order
    detail.orderID == lsn.order.id
    detail.qtyInQueue == lsn.qtyInQueue
    detail.qtyInWork == lsn.qtyInWork
  }

  @Rollback
  def "test OrderOperState copy constructor"() {
    given: 'an order with a step state'
    def order = new Order(order: 'ABC', qtyInQueue: 23.2, qtyInWork: 14.2, id: 337)
    def orderOperState = new OrderOperState(sequence: 3, qtyInQueue: 27.2, qtyInWork: 38.4, order: order)
    order.operationStates = [orderOperState]

    when: 'the detail is created from the order'
    def detail = new FindWorkResponseDetail(orderOperState)

    then: 'the qty/operation is copied'
    detail.order == order.order
    detail.orderID == order.id
    detail.qtyInQueue == orderOperState.qtyInQueue
    detail.qtyInWork == orderOperState.qtyInWork
    detail.operationSequence == orderOperState.sequence
  }

  @Rollback
  def "test LSNOperState copy constructor"() {
    given: 'an LSN/Order with a step state'
    def order = new Order(order: 'ABC', qtyInQueue: 23.2, qtyInWork: 14.2, id: 537)
    def lsn = new LSN(lsn: 'SN001', order: order, id: 437)
    def lsnOperState = new LSNOperState(sequence: 3, qtyInQueue: 27.2, qtyInWork: 38.4, lsn: lsn)
    lsn.operationStates = [lsnOperState]

    when: 'the detail is created from the lsn oper state'
    def detail = new FindWorkResponseDetail(lsnOperState)

    then: 'the qty/operation is copied'
    detail.lsn == lsn.lsn
    detail.lsnID == lsn.id
    detail.order == lsn.order.order
    detail.orderID == lsn.order.id
    detail.qtyInQueue == lsnOperState.qtyInQueue
    detail.qtyInWork == lsnOperState.qtyInWork
    detail.operationSequence == lsnOperState.sequence
  }

  @Rollback
  def "test common constructor status tests"() {
    given: 'a detail record'
    def detail = new FindWorkResponseDetail(qtyInQueue: qtyInQueue, qtyInWork: qtyInWork)

    when: 'the status tests are made'
    detail.init()

    then: 'the status flags are set correctly'
    detail.inQueue == inQueue
    detail.inWork == inWork

    where: 'various combinations of queue and in work are used'
    qtyInQueue | qtyInWork | inQueue | inWork
    13.2       | 14.2      | true    | true
    23.2       | 0.0       | true    | false
    0.0        | 34.2      | false   | true
    0.0        | 0.0       | false   | false
  }
}
