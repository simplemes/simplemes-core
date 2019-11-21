package org.simplemes.mes.demand.service

import grails.gorm.transactions.Rollback
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.mes.demand.CompleteRequest
import org.simplemes.mes.demand.StartRequest
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.test.MESUnitTestUtils
import org.simplemes.mes.tracking.domain.ActionLog
import org.simplemes.mes.tracking.service.ProductionLogService

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests the routing related scenarios for the Work Service actions.
 */
class WorkServiceRoutingSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = HIBERNATE

  /**
   * The work service being tested.
   */
  WorkService service

  def setup() {
    setCurrentUser()
    service = new WorkService()
    service.resolveService = new ResolveService()
    service.productionLogService = new ProductionLogService()
  }

  @Rollback
  def "test start with product routing"() {
    given: "an order is released with a product routing"
    def order = MESUnitTestUtils.releaseOrder(qty: 2.0, operations: [1, 2])

    when: 'the first step is started'
    def request = new StartRequest(order: order, qty: 1.2)
    service.start(request)

    then: 'the quantity is started'
    order.operationStates[0].qtyInQueue == 0.8
    order.operationStates[0].qtyInWork == 1.2
  }

  @Rollback
  def "test complete with product routing"() {
    given: "an order is released with a product routing"
    def order = MESUnitTestUtils.releaseOrder(qty: 2.0, operations: [1, 2])

    and: 'the first step is started'
    service.start(new StartRequest(order: order, qty: 1.2))

    when: 'the first step is completed'
    def completeRequest = new CompleteRequest(order: order)
    def completeResponses = service.complete(completeRequest)

    then: 'the quantity is complete'
    completeResponses[0].order.order == order.order
    order.operationStates[0].qtyInQueue == 0.8
    order.operationStates[0].qtyInWork == 0.0
    order.operationStates[0].qtyDone == 1.2

    and: 'the qty is moved to the next operation'
    order.operationStates[1].qtyInQueue == 1.2

    and: 'the action is logged'
    def l = ActionLog.findAllByAction(WorkService.ACTION_COMPLETE)
    assert l.size() == 1
    assert l[0].order == order
    assert l[0].qty == 1.2
  }

  @Rollback
  def "test complete with qty too big on product routing"() {
    given: "an order is released with a product routing"
    def order = MESUnitTestUtils.releaseOrder(qty: 2.0, operations: [1, 2])

    and: 'the first step is started'
    service.start(new StartRequest(order: order, qty: 1.2))

    when: 'the first step is completed with qty too large'
    service.complete(new CompleteRequest(order: order, qty: 1.3))

    then:
    def e = thrown(BusinessException)
    // error.3008.message=Quantity to complete ({0}) must be less than or equal to the quantity in work ({1})
    assert UnitTestUtils.allParamsHaveValues(e)
    assert e.code == 3008
    assert e.toString().contains('1.2')
    assert e.toString().contains('1.3')
  }


  @Rollback
  def "test start and complete to end of routing"() {
    given: "an order is released with a product routing"
    def order = MESUnitTestUtils.releaseOrder(qty: 1.0, operations: [1, 2])

    when: 'the first step is started and completed'
    service.start(new StartRequest(order: order, operationSequence: 1))
    service.complete(new CompleteRequest(order: order, operationSequence: 1))

    and: 'the last step is started and completed'
    service.start(new StartRequest(order: order, operationSequence: 2))
    service.complete(new CompleteRequest(order: order, operationSequence: 2))

    then: 'the quantity is done'
    def orderEnd = Order.findByOrder(order.order)
    orderEnd.overallStatus.done
    orderEnd.qtyDone == 1.0
    orderEnd.qtyInWork == 0.0
  }

  @Rollback
  def "test partial completes off end of routing"() {
    given: "an order is released with a product routing"
    def order = MESUnitTestUtils.releaseOrder(qty: 2.0, operations: [1, 2])

    when: 'the first step is partially started and completed'
    service.start(new StartRequest(order: order, operationSequence: 1, qty: 0.2))
    service.complete(new CompleteRequest(order: order, operationSequence: 1, qty: 0.2))

    and: 'the last step is partially started and completed'
    service.start(new StartRequest(order: order, operationSequence: 2, qty: 0.1))
    service.complete(new CompleteRequest(order: order, operationSequence: 2, qty: 0.1))
    //println "res = $res"

    then: 'the quantity is partially done and status is still not done'
    def orderEnd = Order.findByOrder(order.order)
    !orderEnd.overallStatus.done
    orderEnd.qtyDone == 0.1
  }
}
