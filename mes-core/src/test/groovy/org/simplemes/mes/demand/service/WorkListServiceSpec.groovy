package org.simplemes.mes.demand.service

import ch.qos.logback.classic.Level
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.demand.FindWorkRequest
import org.simplemes.mes.demand.FindWorkResponse
import org.simplemes.mes.demand.LSNTrackingOption
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
    Order order = MESUnitTestUtils.releaseOrder(spreadQueuedDates: true)

    when: 'a work list is generated'
    FindWorkResponse response = service.findWork(new FindWorkRequest())

    then: 'the right list is returned'
    response.totalAvailable == 1
    response.list.size() == 1

    and: 'the details have the right values'
    def responseDetail = response.list[0]
    responseDetail.order == order.order
    responseDetail.orderID == order.uuid
    responseDetail.id == order.uuid
    responseDetail.qtyInQueue == order.qtyInQueue
    responseDetail.qtyInWork == order.qtyInWork
    responseDetail.inQueue
    !responseDetail.inWork
  }

  @Rollback
  def "test inQueue for orders with no routing"() {
    given: 'multiple released orders'
    def orders = MESUnitTestUtils.releaseOrders(nOrders: 4, spreadQueuedDates: true)

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
    MESUnitTestUtils.releaseOrders(nOrders: 10, spreadQueuedDates: true)

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
    def orders = MESUnitTestUtils.releaseOrders(nOrders: 3, operations: [3], spreadQueuedDates: true)

    when: 'a work list is generated'
    FindWorkResponse response = service.findWork(new FindWorkRequest())

    then: 'the right list is returned'
    response.totalAvailable == 3
    response.list.size() == 3

    and: 'the result is sorted correctly'
    def list = response.list
    list[0].order == orders[0].order
    list[0].orderID == orders[0].uuid
    list[0].id == orders[0].operationStates[0].uuid
    list[0].operationSequence == 3
  }

  def "test max/offset inQueue orders with routing"() {
    given: 'multiple released orders'
    MESUnitTestUtils.releaseOrders(nOrders: 10, operations: [3], spreadQueuedDates: true)

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
    Order order = MESUnitTestUtils.releaseOrder(qty: 5, lsnTrackingOption: LSNTrackingOption.LSN_ONLY, spreadQueuedDates: true)
    LSN firstLSN = order.lsns[0]

    when: 'a work list is generated'
    FindWorkResponse response = service.findWork(new FindWorkRequest())

    then: 'the right list is returned'
    response.totalAvailable == 5
    response.list.size() == 5

    and: 'the details have the right values'
    def responseDetail = response.list[0]
    responseDetail.order == order.order
    responseDetail.orderID == order.uuid
    responseDetail.lsn == firstLSN.lsn
    responseDetail.lsnID == firstLSN.uuid
    responseDetail.id == firstLSN.uuid
    responseDetail.qtyInQueue == firstLSN.qtyInQueue
    responseDetail.qtyInWork == firstLSN.qtyInWork
    responseDetail.inQueue
    !responseDetail.inWork
  }

  def "test max/offset inQueue LSNs with no routing"() {
    given: 'a single released order with multiple LSNs'
    Order.withTransaction {
      MESUnitTestUtils.resetCodeSequences()
      MESUnitTestUtils.releaseOrder(qty: 10, lsnTrackingOption: LSNTrackingOption.LSN_ONLY, spreadQueuedDates: true)
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
      MESUnitTestUtils.releaseOrder(operations: [3], qty: 5,
                                    lsnTrackingOption: LSNTrackingOption.LSN_ONLY, spreadQueuedDates: true)
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

  def "test max/offset inQueue LSNs with routing"() {
    given: 'multiple released LSNs'
    Order.withTransaction {
      MESUnitTestUtils.resetCodeSequences()
      MESUnitTestUtils.releaseOrder(operations: [3], qty: 10,
                                    lsnTrackingOption: LSNTrackingOption.LSN_ONLY, spreadQueuedDates: true)
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
    MESUnitTestUtils.releaseOrder(id: "1WLS", operations: [3], qty: 10,
                                  lsnTrackingOption: LSNTrackingOption.LSN_ONLY, spreadQueuedDates: true)
    // and a single released order with multiple LSNs and no routing
    MESUnitTestUtils.releaseOrder(id: "2WLS", qty: 10,
                                  lsnTrackingOption: LSNTrackingOption.LSN_ONLY, spreadQueuedDates: true)
    // and multiple released orders with no routing
    MESUnitTestUtils.releaseOrders(nOrders: 10, id: "3WLS", spreadQueuedDates: true)
    // and multiple released orders with a routing
    MESUnitTestUtils.releaseOrders(nOrders: 10, id: "4WLS", operations: [3], spreadQueuedDates: true)

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
    MESUnitTestUtils.releaseOrder(id: "1WLS", operations: [3], qty: 10,
                                  lsnTrackingOption: LSNTrackingOption.LSN_ONLY, spreadQueuedDates: true)

    // and a single released order with multiple LSNs and no routing
    MESUnitTestUtils.releaseOrder(id: "2WLS", qty: 10, qtyInWork: 10.0,
                                  lsnTrackingOption: LSNTrackingOption.LSN_ONLY, spreadQueuedDates: true)

    // and multiple released orders with no routing
    MESUnitTestUtils.releaseOrders(nOrders: 10, id: "3WLS", spreadQueuedDates: true)

    // and multiple released orders with a routing
    MESUnitTestUtils.releaseOrders(nOrders: 10, id: "4WLS", operations: [3], qtyInWork: 1.0)

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
    MESUnitTestUtils.releaseOrders(nOrders: 4, qtyInWork: 1.0, spreadQueuedDates: true)

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
    MESUnitTestUtils.releaseOrders(nOrders: 3, operations: [3], qtyInWork: 1.0, spreadQueuedDates: true)

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
    Order order = MESUnitTestUtils.releaseOrder(qty: 5, lsnTrackingOption: LSNTrackingOption.LSN_ONLY,
                                                qtyInWork: 1.0, spreadQueuedDates: true)
    LSN firstLSN = order.lsns[0]

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
    def order = MESUnitTestUtils.releaseOrder(operations: [3], qty: 5, lsnTrackingOption: LSNTrackingOption.LSN_ONLY,
                                              qtyInWork: 1.0, spreadQueuedDates: true)

    when: 'a work list is generated'
    FindWorkResponse response = service.findWork(new FindWorkRequest())

    then: 'the right list is returned'
    response.totalAvailable == 5
    response.list.size() == 5

    and: 'the result is sorted correctly'
    def responseDetail = response.list[0]
    responseDetail.lsn == "SN1000"
    responseDetail.operationSequence == 3
    responseDetail.order == order.order
    responseDetail.orderID == order.uuid
    responseDetail.lsn == order.lsns[0].lsn
    responseDetail.lsnID == order.lsns[0].uuid
    responseDetail.id == order.lsns[0].operationStates[0].uuid
  }

  @Rollback
  def "test findInQueue option is false with both types available"() {
    given: 'multiple released orders that are in queue'
    MESUnitTestUtils.releaseOrders(nOrders: 4, id: "WLS-2", spreadQueuedDates: true)

    and: 'multiple released orders that are in work'
    MESUnitTestUtils.releaseOrders(nOrders: 3, qtyInWork: 1.0, spreadQueuedDates: true)

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
    MESUnitTestUtils.releaseOrders(nOrders: 4, spreadQueuedDates: true)

    and: 'multiple released orders that are in work'
    MESUnitTestUtils.releaseOrders(nOrders: 3, id: "WLS-2", qtyInWork: 1.0, spreadQueuedDates: true)

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

  @Rollback
  def "verify that filter is applied to results - order with no routing"() {
    given: 'multiple released orders'
    def list = MESUnitTestUtils.releaseOrders(nOrders: 15)
    def order = list.find { it.order == 'M1010' }

    when: 'a work list is generated'
    FindWorkResponse response = service.findWork(new FindWorkRequest(filter: 'M101'))

    then: 'the right list is returned'
    response.totalAvailable == 5
    response.list.size() == 5

    and: 'the result is sorted correctly'
    def responseDetail = response.list[0]
    println "response = ${response.list*.order}"
    println "responseDetail = $responseDetail"
    responseDetail.order == order.order
    responseDetail.orderID == order.uuid
    responseDetail.id == order.uuid
    responseDetail.qtyInQueue == order.qtyInQueue
    responseDetail.qtyInWork == order.qtyInWork
    responseDetail.inQueue
    !responseDetail.inWork
  }

  @Rollback
  def "verify filter is applied - orders with routing"() {
    given: 'multiple released orders'
    def orders = MESUnitTestUtils.releaseOrders(nOrders: 5, operations: [3], orderSequenceStart: 1008)
    def order = orders.find { it.order == 'M1010' }

    when: 'a work list is generated'
    FindWorkResponse response = service.findWork(new FindWorkRequest(filter: 'M101'))

    then: 'the right list is returned'
    response.totalAvailable == 3
    response.list.size() == 3

    and: 'the result is sorted correctly'
    def responseDetail = response.list[0]
    responseDetail.order == order.order
    responseDetail.operationSequence == 3
    responseDetail.orderID == order.uuid
    responseDetail.id == order.operationStates[0].uuid
    responseDetail.qtyInQueue == order.operationStates[0].qtyInQueue
    responseDetail.qtyInWork == order.operationStates[0].qtyInWork
    responseDetail.inQueue
    !responseDetail.inWork
  }

  @Rollback
  def "verify that filter is applied to results for LSN - no routing"() {
    MESUnitTestUtils.resetCodeSequences()
    def order = MESUnitTestUtils.releaseOrder(qty: 5, lsns: ['SNA2', 'SNA3', 'SNB2', 'SNB1', 'SNA1',],
                                              lsnTrackingOption: LSNTrackingOption.LSN_ONLY,
                                              qtyInWork: 1.0, spreadQueuedDates: true)
    LSN firstLSN = order.lsns.find { it.lsn == 'SNA1' }

    when: 'a work list is generated'
    FindWorkResponse response = service.findWork(new FindWorkRequest(filter: 'SNA'))

    then: 'the right list is returned'
    response.totalAvailable == 3
    response.list.size() == 3

    and: 'the result is sorted correctly'
    def responseDetail = response.list[0]
    responseDetail.order == order.order
    responseDetail.orderID == order.uuid
    responseDetail.lsn == firstLSN.lsn
    responseDetail.lsnID == firstLSN.uuid
    responseDetail.id == firstLSN.uuid
    responseDetail.qtyInQueue == 0.0
    responseDetail.qtyInWork == 1.0
    !responseDetail.inQueue
    responseDetail.inWork
  }

  @Rollback
  def "verify that filter is applied to inQueue LSNs - with routing"() {
    given: 'a single released order with multiple LSNs'
    def order = MESUnitTestUtils.releaseOrder(operations: [3], qty: 5, lsns: ['SNA2', 'SNA3', 'SNB2', 'SNB1', 'SNA1'],
                                              lsnTrackingOption: LSNTrackingOption.LSN_ONLY,
                                              qtyInWork: 1.0, spreadQueuedDates: true)
    LSN firstLSN = order.lsns.find { it.lsn == 'SNA1' }

    when: 'a work list is generated'
    FindWorkResponse response = service.findWork(new FindWorkRequest(filter: 'SNA'))

    then: 'the right list is returned'
    response.totalAvailable == 3
    response.list.size() == 3

    and: 'the details have the right values'
    def responseDetail = response.list[0]
    responseDetail.order == order.order
    responseDetail.orderID == order.uuid
    responseDetail.lsn == firstLSN.lsn
    responseDetail.lsnID == firstLSN.uuid
    responseDetail.id == firstLSN.operationStates[0].uuid
    responseDetail.qtyInQueue == firstLSN.operationStates[0].qtyInQueue
    responseDetail.qtyInWork == firstLSN.operationStates[0].qtyInWork
    responseDetail.inQueue
    !responseDetail.inWork
  }

}
