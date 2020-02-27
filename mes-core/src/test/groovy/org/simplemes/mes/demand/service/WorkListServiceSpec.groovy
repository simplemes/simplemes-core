package org.simplemes.mes.demand.service

import ch.qos.logback.classic.Level
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.demand.FindWorkRequest
import org.simplemes.mes.demand.FindWorkResponse
import org.simplemes.mes.demand.LSNTrackingOption
import org.simplemes.mes.demand.WorkStateTrait
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.test.MESUnitTestUtils
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
class WorkListServiceSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [ActionLog, ProductionLog, Order, Product]

  WorkListService service

  def setup() {
    setCurrentUser()
    service = new WorkListService()
  }

  /**
   * Utility method to set the dates queued/started to a known value.
   * Processes all LSNs in the order for all possible routing steps.
   * This is used to make sure the sorting returns consistent values.
   * @param order The order to set the dates on.
   */
  protected void spreadDates(Order order) {
    spreadDates([order])
  }
  /**
   * Utility method to set the dates queued/started to a known value for a list of orders/LSNs.
   * Processes all LSNs in the orders for all possible routing steps.
   * @param orders The orders to set the dates on.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  protected void spreadDates(List<Order> orders) {
    def timeStamp = System.currentTimeMillis()
    for (order in orders) {
      timeStamp = setDates(order, timeStamp)
      for (orderOperState in order.operationStates) {
        timeStamp = setDates(orderOperState, timeStamp)
      }
      for (lsn in order.lsns) {
        timeStamp = setDates(lsn, timeStamp)
        for (lsnOperState in lsn.operationStates) {
          timeStamp = setDates(lsnOperState, timeStamp)
        }
      }
      order.save()
    }
  }

  /**
   * Sets the date queued/started (if already set) to the given timestamp and then increments the timestamp.
   * @param workState The state object to update.
   * @param timeStamp The time to set the dates to.
   * @return The updated timestamp.
   */
  protected long setDates(WorkStateTrait workState, long timeStamp) {
    if (workState.dateFirstQueued) {
      workState.dateFirstQueued = new Date(timeStamp)
      timeStamp += 1001
    }
    if (workState.dateFirstStarted) {
      workState.dateFirstStarted = new Date(timeStamp)
      timeStamp += 1001
    }
    return timeStamp
  }

  /**
   * Utility method to move the queued qty to in work and sets the dates queued/started to a known value.
   * Processes all LSNs in the order for all possible routing steps.
   * This is faster than performing the start.
   * @param order The order to move the qty and set the dates.
   */
  protected void moveQtyToInWork(Order order) {
    moveQtyToInWork([order])
  }

  /**
   * Utility method to move the queued qty to in work and sets the dates queued/started to a known value.
   * Processes all LSNs in the order for all possible routing steps.
   * This is faster than performing the start.
   * @param orders The list of orders to move the qty and set the dates.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  protected void moveQtyToInWork(List<Order> orders) {
    def timeStamp = System.currentTimeMillis()
    for (order in orders) {
      timeStamp = moveQtyToInWork(order, timeStamp)
      for (orderOperState in order.operationStates) {
        timeStamp = moveQtyToInWork(orderOperState, timeStamp)
      }
      for (lsn in order.lsns) {
        timeStamp = moveQtyToInWork(lsn, timeStamp)
        for (lsnOperState in lsn.operationStates) {
          timeStamp = moveQtyToInWork(lsnOperState, timeStamp)
        }
      }
      order.save()
    }
  }

  /**
   * Moves the qty to in work and adjust the dates.
   * @param workState The state object to update.
   * @param timeStamp The time to set the dates to.
   * @return The updated timestamp.
   */
  protected long moveQtyToInWork(WorkStateTrait workState, long timeStamp) {
    if (workState.qtyInQueue) {
      workState.qtyInWork = workState.qtyInQueue
      workState.qtyInQueue = 0.0
      workState.dateFirstStarted = new Date(timeStamp)
      timeStamp += 1001
    }
    return timeStamp
  }

  @Rollback
  def "test INFO (performance) logging"() {
    given: 'a released order'
    MESUnitTestUtils.releaseOrders(nOrders: 1)

    and: 'a mock logging is setup'
    def mockAppender = MockAppender.mock(WorkListService, Level.INFO)

    when: 'a work list is generated'
    service.findWork(new FindWorkRequest())

    then: 'an INFO message is logged'
    // 'findWork queries: time: {} - {}/{}/{}/{} (ms) found: {} for request: {}'
    mockAppender.assertMessageIsValid(['queries', 'found', 'request'])
  }

  @Rollback
  def "test inQueue for a single order with no routing"() {
    given: 'a single released order'
    Order order = MESUnitTestUtils.releaseOrder()
    spreadDates(order)

    when: 'a work list is generated'
    FindWorkResponse response = service.findWork(new FindWorkRequest())

    then: 'the right list is returned'
    response.totalAvailable == 1
    response.list.size() == 1

    and: 'the details have the right values'
    def responseDetail = response.list[0]
    responseDetail.order == order.order
    responseDetail.qtyInQueue == order.qtyInQueue
    responseDetail.qtyInWork == order.qtyInWork
    responseDetail.inQueue
    !responseDetail.inWork
  }

  @Rollback
  def "test inQueue for orders with no routing"() {
    given: 'multiple released orders'
    def orders = MESUnitTestUtils.releaseOrders(nOrders: 4)
    spreadDates(orders)

    when: 'a work list is generated'
    FindWorkResponse response = service.findWork(new FindWorkRequest())

    then: 'the right list is returned'
    response.totalAvailable == 4
    response.list.size() == 4

    and: 'the result is sorted correctly'
    def list = response.list
    list[0].order == orders[0].order
  }

  def "test max/offset inQueue orders with no routing"() {
    given: 'multiple released orders'
    Order.withTransaction {
      def orders = MESUnitTestUtils.releaseOrders(nOrders: 10)
      spreadDates(orders)
    }

    when: 'a work list is generated'
    FindWorkResponse response = null
    Order.withTransaction {
      response = service.findWork(new FindWorkRequest(max: max, from: from))
    }

    then: 'the right list is returned'
    response.totalAvailable == 10
    response.list.size() == size

    and: 'the result is sorted correctly'
    response.list[0].order == first

    where:
    max | from | size | first
    5   | 0    | 5    | 'M1000'
    5   | 1    | 5    | 'M1005'
  }

  @Rollback
  def "test inQueue for orders with routing"() {
    given: 'multiple released orders'
    def orders = MESUnitTestUtils.releaseOrders(nOrders: 3, operations: [3])
    spreadDates(orders)

    when: 'a work list is generated'
    FindWorkResponse response = service.findWork(new FindWorkRequest())

    then: 'the right list is returned'
    response.totalAvailable == 3
    response.list.size() == 3

    and: 'the result is sorted correctly'
    def list = response.list
    list[0].order == orders[0].order
    list[0].operationSequence == 3
  }

  def "test max/offset inQueue orders with routing"() {
    given: 'multiple released orders'
    Order.withTransaction {
      def orders = MESUnitTestUtils.releaseOrders(nOrders: 10, operations: [3])
      spreadDates(orders)
    }

    when: 'a work list is generated'
    FindWorkResponse response = null
    Order.withTransaction {
      response = service.findWork(new FindWorkRequest(max: max, from: from))
    }

    then: 'the right list is returned'
    response.totalAvailable == 10
    response.list.size() == size

    and: 'the result is sorted correctly'
    response.list[0].order == first

    where:
    max | from | size | first
    5   | 0    | 5    | 'M1000'
    5   | 1    | 5    | 'M1005'
  }

  @Rollback
  def "test inQueue for LSNs with no routing"() {
    given: 'a single released order with multiple LSNs'
    Order order = MESUnitTestUtils.releaseOrder(qty: 5, lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    LSN firstLSN = order.lsns[0]
    spreadDates(order)

    when: 'a work list is generated'
    FindWorkResponse response = service.findWork(new FindWorkRequest())

    then: 'the right list is returned'
    response.totalAvailable == 5
    response.list.size() == 5

    and: 'the details have the right values'
    def responseDetail = response.list[0]
    responseDetail.order == order.order
    responseDetail.lsn == firstLSN.lsn
    responseDetail.qtyInQueue == firstLSN.qtyInQueue
    responseDetail.qtyInWork == firstLSN.qtyInWork
    responseDetail.inQueue
    !responseDetail.inWork
  }

  def "test max/offset inQueue LSNs with no routing"() {
    given: 'a single released order with multiple LSNs'
    Order.withTransaction {
      MESUnitTestUtils.resetCodeSequences()
      Order order = MESUnitTestUtils.releaseOrder(qty: 10, lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
      spreadDates(order)
    }

    when: 'a work list is generated'
    FindWorkResponse response = null
    Order.withTransaction {
      response = service.findWork(new FindWorkRequest(max: max, from: from))
    }

    then: 'the right list is returned'
    response.totalAvailable == 10
    response.list.size() == size

    and: 'the result is sorted correctly'
    response.list[0].lsn == first

    where:
    max | from | size | first
    5   | 0    | 5    | 'SN1000'
    5   | 1    | 5    | 'SN1005'
  }

  def "test inQueue for LSNs with routing"() {
    given: 'multiple released LSNs'
    Order.withTransaction {
      MESUnitTestUtils.resetCodeSequences()
      def order = MESUnitTestUtils.releaseOrder(operations: [3], qty: 5, lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
      spreadDates(order)
    }

    when: 'a work list is generated'
    FindWorkResponse response = null
    Order.withTransaction {
      response = service.findWork(new FindWorkRequest())
    }

    then: 'the right list is returned'
    response.totalAvailable == 5
    response.list.size() == 5

    and: 'the result is sorted correctly'
    def list = response.list
    list[0].lsn == "SN1000"
    list[0].operationSequence == 3
  }

  // TODO: Restore @Rollback
  def "test max/offset inQueue LSNs with routing"() {
    given: 'multiple released LSNs'
    Order.withTransaction {
      MESUnitTestUtils.resetCodeSequences()
      def order = MESUnitTestUtils.releaseOrder(operations: [3], qty: 10, lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
      spreadDates(order)
    }

    when: 'a work list is generated'
    FindWorkResponse response = null
    Order.withTransaction {
      response = service.findWork(new FindWorkRequest(max: max, from: from))
    }

    then: 'the right list is returned'
    response.totalAvailable == 10
    response.list.size() == size

    and: 'the result is sorted correctly'
    response.list[0].lsn == first

    where:
    max | from | size | first
    5   | 0    | 5    | 'SN1000'
    5   | 1    | 5    | 'SN1005'
  }

  def "test max/offset inQueue all 4 sources"() {
    given: 'multiple released LSNs'
    Order.withTransaction {
      def order = MESUnitTestUtils.releaseOrder(id: "1WLS", operations: [3], qty: 10, lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
      spreadDates(order)
      // and a single released order with multiple LSNs and no routing
      order = MESUnitTestUtils.releaseOrder(id: "2WLS", qty: 10, lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
      spreadDates(order)
      // and multiple released orders with no routing
      def orders = MESUnitTestUtils.releaseOrders(nOrders: 10, id: "3WLS")
      spreadDates(orders)

      // and multiple released orders with a routing
      orders = MESUnitTestUtils.releaseOrders(nOrders: 10, id: "4WLS", operations: [3])
      spreadDates(orders)
    }

    when: 'a work list is generated'
    FindWorkResponse response = null
    Order.withTransaction {
      response = service.findWork(new FindWorkRequest(max: max, from: from))
    }

    then: 'the right list is returned'
    response.totalAvailable == 40
    response.list.size() == size

    and: 'the result is sorted correctly'
    response.list[0].toString().contains(first)

    where:
    max | from | size | first
    5   | 0    | 20   | 'M1000-3WLS'
    5   | 1    | 20   | 'M1005-3WLS'
  }

  def "test max/offset with inQueue and inWork at all 4 sources"() {
    given: 'multiple released LSNs'
    Order.withTransaction {
      def order = MESUnitTestUtils.releaseOrder(id: "1WLS", operations: [3], qty: 10, lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
      spreadDates(order)

      // and a single released order with multiple LSNs and no routing
      order = MESUnitTestUtils.releaseOrder(id: "2WLS", qty: 10, lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
      moveQtyToInWork(order)

      // and multiple released orders with no routing
      def orders = MESUnitTestUtils.releaseOrders(nOrders: 10, id: "3WLS")
      spreadDates(orders)

      // and multiple released orders with a routing
      orders = MESUnitTestUtils.releaseOrders(nOrders: 10, id: "4WLS", operations: [3])
      moveQtyToInWork(orders)
    }

    when: 'a work list is generated'
    FindWorkResponse response = null
    Order.withTransaction {
      response = service.findWork(new FindWorkRequest(max: max, from: from))
    }

    then: 'the right list is returned'
    response.totalAvailable == 40
    response.list.size() == size

    and: 'the result is sorted correctly'
    response.list[0].toString().contains(first)

    where:
    max | from | size | first
    5   | 0    | 20   | 'M1000-3WLS'
    5   | 1    | 20   | 'M1005-3WLS'
  }

  @Rollback
  def "test upper limit on max rows "() {
    given: 'multiple released orders'
    MESUnitTestUtils.resetCodeSequences()
    assert Order.list().size() == 0
    MESUnitTestUtils.releaseOrders(nOrders: 105)

    and: 'an upper limit on rows returned'
    Holders.configuration.maxRowLimit = 100

    when: 'a work list is generated'
    FindWorkResponse response = service.findWork(new FindWorkRequest(max: 110))

    then: 'the right list is returned'
    response.totalAvailable == 105
    response.list.size() == 100
  }

  @Rollback
  def "test inWork for a single order with no routing"() {
    given: 'a single released order'
    Order order = MESUnitTestUtils.releaseOrder()
    and: 'some qty has been started'
    order.startQty(1.0)

    when: 'a work list is generated'
    FindWorkResponse response = service.findWork(new FindWorkRequest())

    then: 'the right list is returned'
    response.totalAvailable == 1
    response.list.size() == 1

    and: 'the details have the right values'
    def responseDetail = response.list[0]
    responseDetail.order == order.order
    responseDetail.qtyInQueue == order.qtyInQueue
    responseDetail.qtyInWork == order.qtyInWork
    !responseDetail.inQueue
    responseDetail.inWork
  }

  @Rollback
  def "test inWork for orders with no routing"() {
    given: 'a single released order with some qty in work'
    def orders = MESUnitTestUtils.releaseOrders(nOrders: 4)
    moveQtyToInWork(orders)
    spreadDates(orders)

    when: 'a work list is generated'
    FindWorkResponse response = service.findWork(new FindWorkRequest())

    then: 'the right list is returned'
    response.totalAvailable == 4
    response.list.size() == 4

    and: 'the details have the right values'
    def list = response.list
    list[0].order == "M1000"
    !list[0].inQueue
    list[0].inWork
  }

  @Rollback
  def "test inWork for orders with routing"() {
    given: 'multiple released orders'
    def orders = MESUnitTestUtils.releaseOrders(nOrders: 3, operations: [3])
    moveQtyToInWork(orders)
    spreadDates(orders)

    when: 'a work list is generated'
    FindWorkResponse response = service.findWork(new FindWorkRequest())

    then: 'the right list is returned'
    response.totalAvailable == 3
    response.list.size() == 3

    and: 'the result is sorted correctly'
    def list = response.list
    list[0].order == "M1000"
    list[0].operationSequence == 3
  }

  @Rollback
  def "test inWork for LSNs with no routing"() {
    given: 'a single released order with multiple LSNs'
    Order order = MESUnitTestUtils.releaseOrder(qty: 5, lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    LSN firstLSN = order.lsns[0]
    moveQtyToInWork(order)
    spreadDates(order)

    when: 'a work list is generated'
    FindWorkResponse response = service.findWork(new FindWorkRequest())

    then: 'the right list is returned'
    response.totalAvailable == 5
    response.list.size() == 5

    and: 'the details have the right values'
    def responseDetail = response.list[0]
    responseDetail.order == order.order
    responseDetail.lsn == firstLSN.lsn
    responseDetail.qtyInQueue == firstLSN.qtyInQueue
    responseDetail.qtyInWork == firstLSN.qtyInWork
    !responseDetail.inQueue
    responseDetail.inWork
  }

  @Rollback
  def "test inWork for LSNs with routing"() {
    given: 'multiple released LSNs'
    MESUnitTestUtils.resetCodeSequences()
    def order = MESUnitTestUtils.releaseOrder(operations: [3], qty: 5, lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    moveQtyToInWork(order)
    spreadDates(order)

    when: 'a work list is generated'
    FindWorkResponse response = service.findWork(new FindWorkRequest())

    then: 'the right list is returned'
    response.totalAvailable == 5
    response.list.size() == 5

    and: 'the result is sorted correctly'
    def list = response.list
    list[0].lsn == "SN1000"
    list[0].operationSequence == 3
  }

  @Rollback
  def "test findInQueue option is false with both types available"() {
    given: 'multiple released orders that are in queue'
    def ordersInQueue = MESUnitTestUtils.releaseOrders(nOrders: 4, id: "WLS-2")
    spreadDates(ordersInQueue)

    and: 'multiple released orders that are in work'
    def orders = MESUnitTestUtils.releaseOrders(nOrders: 3)
    moveQtyToInWork(orders)
    spreadDates(orders)

    when: 'a work list is generated'
    FindWorkResponse response = service.findWork(new FindWorkRequest(findInQueue: false, max: 2))

    then: 'the right list is returned'
    response.totalAvailable == 3
    response.list.size() == 2

    and: 'the result is sorted correctly'
    def list = response.list
    list[0].order == "M1000"
    !list[0].inQueue
    list[0].inWork
  }

  @Rollback
  def "test findInWork option is false with both types available"() {
    given: 'multiple released orders that are in queue'
    def ordersInQueue = MESUnitTestUtils.releaseOrders(nOrders: 4)
    spreadDates(ordersInQueue)

    and: 'multiple released orders that are in work'
    def orders = MESUnitTestUtils.releaseOrders(nOrders: 3, id: "WLS-2")
    moveQtyToInWork(orders)

    when: 'a work list is generated'
    FindWorkResponse response = service.findWork(new FindWorkRequest(findInWork: false, max: 2))

    then: 'the right list is returned'
    response.totalAvailable == 4
    response.list.size() == 2

    and: 'the result is sorted correctly'
    def list = response.list
    list[0].order == "M1000"
    list[0].inQueue
    !list[0].inWork
  }
}
