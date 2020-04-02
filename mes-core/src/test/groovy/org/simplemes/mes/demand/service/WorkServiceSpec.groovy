package org.simplemes.mes.demand.service

import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.demand.CompleteRequest
import org.simplemes.mes.demand.LSNTrackingOption
import org.simplemes.mes.demand.OrderCreatedStatus
import org.simplemes.mes.demand.OrderHoldStatus
import org.simplemes.mes.demand.StartRequest
import org.simplemes.mes.demand.StartUndoAction
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.floor.domain.WorkCenter
import org.simplemes.mes.test.MESUnitTestUtils
import org.simplemes.mes.tracking.domain.ActionLog
import org.simplemes.mes.tracking.domain.ProductionLog
import org.simplemes.mes.tracking.service.ProductionLogService

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests of routing-related actions for WorkService.
 */
class WorkServiceSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

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
  def "verify that order start with order and no routing works"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(qty: 100)

    when: 'the start request is made'
    def startResponses = service.start(new StartRequest(order: order, qty: 10.2))

    then: 'the response is correct'
    startResponses.size() == 1
    startResponses[0].order.order == order.order
    startResponses[0].qty == 10.2

    and: 'the order is started'
    def order2 = Order.findByOrder(order.order)
    order2 == order
    order2.qtyInWork == 10.2
    order2.qtyInQueue == 89.8

    and: 'the action is logged'
    def l = ActionLog.list().findAll { it.action == WorkService.ACTION_START }
    l.size() == 1
    l[0].order == order
    l[0].qty == 10.2

    and: 'the undo actions are valid'
    startResponses[0].undoActions.size() == 1
    def undoAction = startResponses[0].undoActions[0]
    undoAction instanceof StartUndoAction
    UnitTestUtils.assertContainsAllIgnoreCase(undoAction.JSON, [order.order, '10.2'])
    UnitTestUtils.assertContainsAllIgnoreCase(undoAction.infoMsg, [order.order, '10.2'])
  }

  @Rollback
  def "verify that order start fails with order in non-workable status"() {
    given: 'an un-released order'
    Order order = new Order(order: 'M002', qtyInQueue: 1.0, overallStatus: OrderCreatedStatus.instance).save()

    when: 'the start request is made'
    service.start(new StartRequest(order: order))

    then: 'an exception is triggered'
    def ex = thrown(BusinessException)
    // error.3001.message=Order {0} cannot be processed.  It has a status of {1}.
    UnitTestUtils.allParamsHaveValues(ex)
    UnitTestUtils.assertContainsAllIgnoreCase(ex.toString(), ['M002', 'Created'])
    ex.code == 3001
  }

  @Rollback
  def "verify that order start fails with bad qty"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(qty: 100)

    when: 'the start request is made'
    service.start(new StartRequest(order: order, qty: -1))

    then: 'an exception is triggered'
    def ex = thrown(BusinessException)
    // error.3002.message=Quantity to start ({0}) must be greater than 0
    UnitTestUtils.allParamsHaveValues(ex)
    UnitTestUtils.assertContainsAllIgnoreCase(ex.toString(), ['-1', 'quantity'])
    ex.code == 3002
  }

  @Rollback
  def "verify that order start fails with qty to large"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(qty: 100)

    when: 'the start request is made'
    service.start(new StartRequest(order: order, qty: 147))

    then: 'an exception is triggered'
    def ex = thrown(BusinessException)
    // error.3003.message=Quantity to start ({0}) must be less than or equal to the quantity in queue ({1})
    UnitTestUtils.allParamsHaveValues(ex)
    UnitTestUtils.assertContainsAllIgnoreCase(ex.toString(), ['147', 'quantity'])
    ex.code == 3003
  }

  @Rollback
  def "verify that order start fails missing LSN or Order"() {
    when: 'the start request is made'
    service.start(new StartRequest(qty: 147))

    then: 'an exception is triggered'
    def ex = thrown(BusinessException)
    UnitTestUtils.allParamsHaveValues(ex)
    UnitTestUtils.assertContainsAllIgnoreCase(ex.toString(), ['LSN', 'order'])
  }

  @Rollback
  def "verify that order complete fails missing LSN or Order"() {
    when: 'the start request is made'
    service.complete(new CompleteRequest(qty: 147))

    then: 'an exception is triggered'
    def ex = thrown(BusinessException)
    UnitTestUtils.allParamsHaveValues(ex)
    UnitTestUtils.assertContainsAllIgnoreCase(ex.toString(), ['LSN', 'order'])
  }

  @Rollback
  def "verify that order complete works with no routing and specific dates"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(qty: 100)

    and: 'the times the order was started and reversed - 10 seconds elapsed'
    def completeTime = new Date()
    def startTime = new Date(completeTime.time - 10000)

    when: 'the qty is started first'
    service.start(new StartRequest(order: order, qty: 10.2, dateTime: startTime))

    and: 'the qty is completed'
    def completeResponses = service.complete(new CompleteRequest(order: order, qty: 10.2, dateTime: completeTime))

    then: 'the response is correct'
    completeResponses.size() == 1
    completeResponses[0].order.order == order.order
    completeResponses[0].qty == 10.2
    !completeResponses[0].done

    and: 'the order is updated in the database'
    def order2 = Order.findByOrder(order.order)
    order2 == order
    order2.qtyInWork == 0
    order2.qtyDone == 10.2
    order2.qtyInQueue == 89.8

    and: 'the action is logged'
    def al = ActionLog.list().find { it.action == WorkService.ACTION_COMPLETE }
    al.order == order
    al.qty == 10.2

    and: 'the production log is written correctly'
    def pl = ProductionLog.list().find { it.action == WorkService.ACTION_COMPLETE }
    pl.order == order.order
    pl.qty == 10.2
    pl.elapsedTime == 10000
    UnitTestUtils.compareDates(pl.dateTime, completeTime)
    UnitTestUtils.compareDates(pl.startDateTime, startTime)
  }

  @Rollback
  def "verify that complete works with product, LSN, work center and no dates"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(qty: 1, lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    def lsn = order.lsns[0]

    and: 'a work center'
    def workCenter = new WorkCenter(workCenter: 'WC1').save()

    and: 'the order is started'
    service.start(new StartRequest(order: order, lsn: lsn, qty: 1, workCenter: workCenter))

    when: 'the qty is completed with optional values'
    service.complete(new CompleteRequest(order: order, lsn: lsn, qty: 1, workCenter: workCenter))

    then: 'the action is logged correctly'
    def al = ActionLog.list().find { it.action == WorkService.ACTION_COMPLETE }
    al.order == order
    al.lsn == lsn
    al.qty == 1
    al.workCenter == workCenter
    al.product == order.product

    and: 'the production log is written correctly'
    def pl = ProductionLog.list().find { WorkService.ACTION_COMPLETE }
    pl.order == order.order
    pl.qty == 1
    pl.elapsedTime < 1000
    pl.lsn == lsn.lsn
    pl.workCenter == workCenter.workCenter
    pl.product == order.product.product
    UnitTestUtils.dateIsCloseToNow(pl.dateTime)
    UnitTestUtils.dateIsCloseToNow(pl.startDateTime)
  }

  @Rollback
  def "verify that order complete off the end of the routing works"() {
    given: 'a released order with a routing'
    def order = MESUnitTestUtils.releaseOrder(id: 'WSS', qty: 100, operations: [1])

    when: 'the qty is started first'
    service.start(new StartRequest(order: order, qty: 20.2, operationSequence: 1))

    and: 'the qty is completed'
    def completeResponses = service.complete(new CompleteRequest(order: order, qty: 20.2))

    then: 'the response is correct'
    completeResponses[0].order.order == order.order

    and: 'the quantity is correct at the first step'
    completeResponses[0].order.operationStates[0].qtyInWork == 0.0
    completeResponses[0].order.operationStates[0].qtyDone == 20.2

    and: 'the quantity is marked as done at the order level'
    completeResponses[0].order.qtyDone == 20.2
  }

  @Rollback
  def "verify that order complete fails with bad qty"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(qty: 100)

    when: 'the qty is started first'
    service.start(new StartRequest(order: order, qty: 10.2))

    and: 'the qty is completed'
    service.complete(new CompleteRequest(order: order, qty: -2))

    then: 'an exception is triggered'
    def ex = thrown(BusinessException)
    // error.3007.message=Quantity to complete ({0}) must be greater than 0
    UnitTestUtils.allParamsHaveValues(ex)
    UnitTestUtils.assertContainsAllIgnoreCase(ex.toString(), ['-2', 'quantity'])
    ex.code == 3007
  }

  @Rollback
  def "verify that order complete fails with too large qty for complete"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(qty: 100)

    when: 'the qty is started first'
    service.start(new StartRequest(order: order, qty: 10.2))

    and: 'the qty is completed'
    service.complete(new CompleteRequest(order: order, qty: 11.2))

    then: 'an exception is triggered'
    def ex = thrown(BusinessException)
    // error.3008.message=Quantity to complete ({0}) must be less than or equal to the quantity in work ({1})
    UnitTestUtils.allParamsHaveValues(ex)
    UnitTestUtils.assertContainsAllIgnoreCase(ex.toString(), ['10.2', '11.2', 'quantity'])
    ex.code == 3008
  }

  @Rollback
  def "verify that order complete fails with order in non-workable status"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder()

    when: 'the start request is made'
    service.start(new StartRequest(order: order))

    and: 'the order status is changed to non-workable'
    order.overallStatus = OrderHoldStatus.instance

    and: 'the complete is attempted'
    service.complete(new CompleteRequest(order: order))

    then: 'an exception is triggered'
    def ex = thrown(BusinessException)
    // error.3001.message=Order {0} cannot be processed.  It has a status of {1}.
    UnitTestUtils.allParamsHaveValues(ex)
    UnitTestUtils.assertContainsAllIgnoreCase(ex.toString(), [order.order, order.overallStatus.toStringLocalized()])
    ex.code == 3001
  }

  @Rollback
  def "verify that order reverseStart works with order, dates and no routing"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(qty: 100)

    and: 'the times the order was started and reversed - 10 seconds elapsed'
    def reverseStartTime = new Date()
    def startTime = new Date(reverseStartTime.time - 10000)

    and: 'the order is started'
    service.start(new StartRequest(order: order, qty: 10.2, dateTime: startTime))

    when: 'the reverse start request is made'
    def reverseStartResponses = service.reverseStart(new StartRequest(order: order, qty: 10.2, dateTime: reverseStartTime))

    then: 'the response is correct'
    reverseStartResponses[0].order.order == order.order
    reverseStartResponses[0].order.qtyInWork == 0.0
    reverseStartResponses[0].order.qtyInQueue == 100.0

    and: 'the order is no longer in work'
    def order2 = Order.findByOrder(order.order)
    order2 == order
    order2.qtyInWork == 0.0
    order2.qtyInQueue == 100.0

    and: 'the action is logged'
    def al = ActionLog.list().find { it.action == WorkService.ACTION_REVERSE_START }
    al.order == order
    al.qty == 10.2
    al.dateTime == reverseStartTime

    and: 'the production log is written correctly'
    def pl = ProductionLog.list().find { it.action == WorkService.ACTION_REVERSE_START }
    pl.order == order.order
    pl.qty == 10.2
    pl.qtyStarted == 10.2
    pl.qtyCompleted == 0.0
    UnitTestUtils.dateIsCloseToNow(pl.dateTime)
    UnitTestUtils.compareDates(pl.startDateTime, startTime)
    pl.elapsedTime == 10000

    and: 'the response has no undo actions'
    reverseStartResponses[0].undoActions.size() == 0
  }

  @Rollback
  def "verify that reverseStart works with product, LSN and work center"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(qty: 1, lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    def lsn = order.lsns[0]

    and: 'a work center'
    def workCenter = new WorkCenter(workCenter: 'WC1').save()

    and: 'the order is started'
    service.start(new StartRequest(order: order, lsn: lsn, qty: 1, workCenter: workCenter))

    when: 'the reverse start request is made'
    service.reverseStart(new StartRequest(order: order, lsn: lsn, qty: 1, workCenter: workCenter))

    then: 'the action is logged'
    def al = ActionLog.list().find { it.action == WorkService.ACTION_REVERSE_START }
    al.order == order
    al.lsn == lsn
    al.qty == 1
    al.workCenter == workCenter
    al.product == order.product

    and: 'the production log is written correctly'
    def pl = ProductionLog.list().find { it.action == WorkService.ACTION_REVERSE_START }
    pl.order == order.order
    pl.qty == 1
    pl.elapsedTime < 1000
    pl.lsn == lsn.lsn
    pl.workCenter == workCenter.workCenter
    pl.product == order.product.product
    UnitTestUtils.dateIsCloseToNow(pl.dateTime)
    UnitTestUtils.dateIsCloseToNow(pl.startDateTime)
  }

  @Rollback
  def "verify that order reverseStart works with no qty on order and no routing"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(qty: 100)

    and: 'the order is started'
    service.start(new StartRequest(order: order, qty: 10.2))

    when: 'the reverse start request is made'
    def reverseStartResponses = service.reverseStart(new StartRequest(order: order))

    then: 'the response is correct'
    reverseStartResponses[0].order.order == order.order
    reverseStartResponses[0].order.qtyInWork == 0.0
    reverseStartResponses[0].order.qtyInQueue == 100.0

    and: 'the order is no longer in work'
    def order2 = Order.findByOrder(order.order)
    order2 == order
    order2.qtyInWork == 0.0
    order2.qtyInQueue == 100.0

    and: 'the action is logged'
    def l = ActionLog.list().findAll { it.action == WorkService.ACTION_REVERSE_START }
    l.size() == 1
    l[0].order == order
    l[0].qty == 10.2
  }

  @Rollback
  def "verify that order reverseStart fails with bad qty"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(qty: 100)

    when: 'the qty is started first'
    service.start(new StartRequest(order: order, qty: 10.2))

    and: 'the wrong qty is reversed'
    service.reverseStart(new StartRequest(order: order, qty: 11.2))

    then: 'an exception is triggered'
    def ex = thrown(BusinessException)
    // error.3008.message=Quantity to process ({0}) must be less than or equal to the quantity in work ({1})
    UnitTestUtils.allParamsHaveValues(ex)
    UnitTestUtils.assertContainsAllIgnoreCase(ex.toString(), ['10.2', '11.2', 'quantity'])
    ex.code == 3008
  }

  @Rollback
  def "verify that order reverseComplete works with order, dates and no routing"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(qty: 100, qtyDone: 1.2)

    and: 'the times the order was started and reversed - 10 seconds elapsed'
    def reverseCompleteTime = new Date(UnitTestUtils.SAMPLE_TIME_MS)

    when: 'the reverse start request is made'
    def reverseCompleteResponses = service.reverseComplete(new CompleteRequest(order: order, qty: 1.2, dateTime: reverseCompleteTime))

    then: 'the response is correct'
    reverseCompleteResponses[0].order.order == order.order
    reverseCompleteResponses[0].order.qtyDone == 0.0
    reverseCompleteResponses[0].order.qtyInWork == 0.0
    reverseCompleteResponses[0].order.qtyInQueue == 100.0

    and: 'the order is no longer in work'
    def order2 = Order.findByOrder(order.order)
    order2 == order
    order2.qtyInWork == 0.0
    order2.qtyDone == 0.0
    order2.qtyInQueue == 100.0

    and: 'the action is logged'
    def al = ActionLog.list().find { it.action == WorkService.ACTION_REVERSE_COMPLETE }
    al.order == order
    al.qty == 1.2
    al.dateTime == reverseCompleteTime

    and: 'the production log is written correctly'
    def pl = ProductionLog.list().find { it.action == WorkService.ACTION_REVERSE_COMPLETE }
    pl.order == order.order
    pl.qty == -1.2
    pl.qtyStarted == -1.2
    pl.qtyCompleted == 0.0
    UnitTestUtils.compareDates(pl.startDateTime, reverseCompleteTime)
    UnitTestUtils.compareDates(pl.dateTime, reverseCompleteTime)
    pl.elapsedTime == 0

    and: 'the response has no undo actions'
    reverseCompleteResponses[0].undoActions.size() == 0
  }

  @Rollback
  def "verify that lsn reverseComplete works with dates and no routing"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(qty: 1, qtyDone: 1, lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    def lsn = order.lsns[0]

    and: 'the times the order was started and reversed - 10 seconds elapsed'
    def reverseCompleteTime = new Date(UnitTestUtils.SAMPLE_TIME_MS)

    when: 'the reverse start request is made'
    def reverseCompleteResponses = service.reverseComplete(new CompleteRequest(lsn: lsn, qty: 1.0, dateTime: reverseCompleteTime))

    then: 'the response is correct'
    reverseCompleteResponses[0].lsn.lsn == lsn.lsn
    reverseCompleteResponses[0].lsn.qtyDone == 0.0
    reverseCompleteResponses[0].lsn.qtyInWork == 0.0
    reverseCompleteResponses[0].lsn.qtyInQueue == 1.0

    and: 'the lsn is no longer in work'
    def lsn2 = LSN.findByUuid(lsn.uuid)
    lsn2.qtyInWork == 0.0
    lsn2.qtyDone == 0.0
    lsn2.qtyInQueue == 1.0

    and: 'the action is logged'
    def al = ActionLog.list().find { it.action == WorkService.ACTION_REVERSE_COMPLETE }
    al.lsn == lsn
    al.order == order
    al.qty == 1.0
    al.dateTime == reverseCompleteTime

    and: 'the production log is written correctly'
    def pl = ProductionLog.list().find { it.action == WorkService.ACTION_REVERSE_COMPLETE }
    pl.order == order.order
    pl.lsn == lsn.lsn
    pl.qty == -1.0
    pl.qtyStarted == -1.0
    pl.qtyCompleted == 0.0
    UnitTestUtils.compareDates(pl.startDateTime, reverseCompleteTime)
    UnitTestUtils.compareDates(pl.dateTime, reverseCompleteTime)
    pl.elapsedTime == 0

    and: 'the response has no undo actions'
    reverseCompleteResponses[0].undoActions.size() == 0
  }

  @Rollback
  def "verify that order partial reverseComplete works with order, dates and no routing"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(qty: 100, qtyDone: 2.2)

    and: 'the times the order was started and reversed - 10 seconds elapsed'
    def reverseCompleteTime = new Date(UnitTestUtils.SAMPLE_TIME_MS)

    when: 'the reverse start request is made'
    def reverseCompleteResponses = service.reverseComplete(new CompleteRequest(order: order, qty: 1.2, dateTime: reverseCompleteTime))

    then: 'the response is correct'
    reverseCompleteResponses[0].order.order == order.order
    reverseCompleteResponses[0].order.qtyDone == 1.0
    reverseCompleteResponses[0].order.qtyInWork == 0.0
    reverseCompleteResponses[0].order.qtyInQueue == 99.0

    and: 'the order is no longer in work'
    def order2 = Order.findByOrder(order.order)
    order2 == order
    order2.qtyInWork == 0.0
    order2.qtyDone == 1.0
    order2.qtyInQueue == 99.0
  }

  // testStartLSN
  // testStartLSNRouting
  // testCompleteLSN
  // testCompleteLSNRouting
}
