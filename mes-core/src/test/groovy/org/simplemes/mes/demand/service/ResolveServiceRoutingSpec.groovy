package org.simplemes.mes.demand.service

import grails.gorm.transactions.Rollback
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.mes.demand.ResolveQuantityPreference
import org.simplemes.mes.demand.ResolveWorkableRequest
import org.simplemes.mes.demand.StartRequest
import org.simplemes.mes.demand.WorkableInterface
import org.simplemes.mes.demand.domain.LSNSequence
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
 * Tests the routing related scenarios for the Resolve Service actions.
 */
class ResolveServiceRoutingSpec extends BaseSpecification {
  @SuppressWarnings("unused")
  static dirtyDomains = [ActionLog, ProductionLog, Order, Product, LSNSequence]

  def setup() {
    setCurrentUser()
  }

  @Rollback
  def "test resolve when using order-based processing and a routing"() {
    given: "an order is released with a product routing"
    def order = MESUnitTestUtils.releaseOrder(qty: 5.0, operations: [1])

    when: 'the current workable is resolved'
    ResolveWorkableRequest req = new ResolveWorkableRequest(order: order)
    WorkableInterface w = new ResolveService().resolveWorkable(req)[0]

    then: 'the qty is in queue at the first operation'
    w != null
    w.qtyInQueue == 5.0
    w == order.operationStates[0]
  }

  @Rollback
  def "test resolve production request with no operation sequence"() {
    given: "an order is released with a product routing"
    def order = MESUnitTestUtils.releaseOrder(qty: 5.0, operations: [1])

    when: 'the production request is resolved to find the operation'
    def req = new StartRequest(barcode: order.order)
    new ResolveService().resolveProductionRequest(req)

    then: 'the request has the correct sequence filled in'
    req.order == order
    req.operationSequence == 1
  }

  @Rollback
  def "test resolve with order and no operation for orders with a routing and caller wants just qty in queue"() {
    given: "an order is released with a product routing"
    def order = MESUnitTestUtils.releaseOrder(qty: 5.0, operations: [1, 2, 3])

    and: "the qty is in queue at second operation and in work before then"
    order.operationStates[0].qtyInQueue = 0.0
    order.operationStates[0].qtyInWork = 1.0
    order.operationStates[1].qtyInQueue = 1.0
    order.save()

    when: 'the production request is resolved to find the operation with a qty in queue'
    def req = new StartRequest(barcode: order.order)
    new ResolveService().resolveProductionRequest(req, ResolveQuantityPreference.QUEUE)

    then: 'the request is updated with the correct sequence for the qty in queue'
    req.order == order
    req.operationSequence == 2
  }

  @Rollback
  def "test resolve with order and no operation for orders with a routing and caller wants just qty in work"() {
    given: "an order is released with a product routing"
    def order = MESUnitTestUtils.releaseOrder(qty: 5.0, operations: [1, 2, 3])

    and: "the qty is in work at second operation and in queue before there"
    order.operationStates[0].qtyInQueue = 1.0
    order.operationStates[1].qtyInWork = 1.0
    order.save()

    when: 'the production request is resolved to find the operation with a qty in work'
    def req = new StartRequest(barcode: order.order)
    new ResolveService().resolveProductionRequest(req, ResolveQuantityPreference.WORK)

    then: 'the request is updated with the correct sequence for the qty in work'
    req.order == order
    req.operationSequence == 2
  }

  @Rollback
  def "test resolve request when given an operation sequence"() {
    given: "an order is released with a product routing"
    def order = MESUnitTestUtils.releaseOrder(qty: 5.0, operations: [1, 2, 3])

    and: "the qty is in queue at two operations"
    order.operationStates[0].qtyInQueue = 4.0
    order.operationStates[1].qtyInQueue = 1.0
    order.save()

    when: 'the production request uses the second operation sequence'
    def req = new StartRequest(order: order, operationSequence: 2, qty: 0.2)
    new ResolveService().resolveProductionRequest(req)

    then: 'the workable returned is the one for the second oper'
    req.operationSequence == 2
  }

  @Rollback
  def "test resolve workable when given an operation sequence"() {
    given: "an order is released with a product routing"
    def order = MESUnitTestUtils.releaseOrder(qty: 5.0, operations: [1, 2, 3])

    and: "the qty is in queue at two operations"
    order.operationStates[0].qtyInQueue = 4.0
    order.operationStates[1].qtyInQueue = 1.0
    order.save()

    when: 'the production request uses the second operation sequence'
    ResolveWorkableRequest req = new ResolveWorkableRequest(order: order, operationSequence: 2)
    def w = new ResolveService().resolveWorkable(req)[0]

    then: 'the workable returned is the one for the second oper'
    w.sequence == 2
  }

}
