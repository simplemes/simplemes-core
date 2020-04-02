package org.simplemes.mes.demand.service

import groovy.util.logging.Slf4j
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.mes.demand.CompleteRequest
import org.simplemes.mes.demand.CompleteResponse
import org.simplemes.mes.demand.ResolveQuantityPreference
import org.simplemes.mes.demand.ResolveWorkableRequest
import org.simplemes.mes.demand.StartRequest
import org.simplemes.mes.demand.StartResponse
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.tracking.ProductionLogRequest
import org.simplemes.mes.tracking.domain.ActionLog
import org.simplemes.mes.tracking.service.ProductionLogService

import javax.inject.Inject
import javax.transaction.Transactional

/**
 * Defines Services for the starting and completing work on Orders and LSNs.
 * <p/>
 * This service is tightly coupled with the Order object.  Most of the real business logic used to move production
 * around the shop floor is contained here.
 * <p/>
 * This Service is part of the <b>Stable API</b>.
 */
@Slf4j
@Transactional
class WorkService {

  @Inject
  ResolveService resolveService

  @Inject
  ProductionLogService productionLogService

  /**
   * The ActionLog {@link org.simplemes.mes.tracking.domain.ActionLog} entry used for start work requests.
   */
  public static final String ACTION_START = "START"

  /**
   * The ActionLog {@link org.simplemes.mes.tracking.domain.ActionLog} entry used for reverse start work requests.
   */
  public static final String ACTION_REVERSE_START = "REVERSE_START"

  /**
   * The {@link org.simplemes.mes.tracking.domain.ActionLog} entry used for complete work requests.
   */
  public static final String ACTION_COMPLETE = "COMPLETE"

  /**
   * The ActionLog {@link org.simplemes.mes.tracking.domain.ActionLog} entry used for reverse complete work requests.
   */
  public static final String ACTION_REVERSE_COMPLETE = "REVERSE_COMPLETE"

  /**
   * Begin work on an Order/LSN.  The quantity processed is optional.
   * Below is a typical usage:
   * <pre>
   *   WorkService workService   // Allow automatic injection of WorkService
   *   def request = new StartRequest()
   *   request.qty = 3.0
   *   request.order = Order.findByOrder('M001')
   *   workService.start(request)
   * </pre>
   *
   * @param request The start request data itself.  This request is refined by the method
   * {@link org.simplemes.mes.demand.service.ResolveService#resolveProductionRequest}.
   * @return The order/LSN(s) that were started.
   */
  List<StartResponse> start(StartRequest request) {
    def res = []
    resolveService.resolveProductionRequest(request, ResolveQuantityPreference.QUEUE)
    def resolveRequest = new ResolveWorkableRequest(order: request?.order,
                                                    lsn: request?.lsn,
                                                    qty: request?.qty,
                                                    operationSequence: request?.operationSequence)
    def workables = resolveService.resolveWorkable(resolveRequest)
    for (w in workables) {
      // Make sure the order for the workable can be worked.
      Order order = resolveService.determineOrderForWorkable(w)
      def lsn = resolveService.determineLSNForWorkable(w)
      if (order) {
        if (!order.overallStatus.isWorkable()) {
          // error.3001.message=Order {0} cannot be worked.  It has a status of {1}
          throw new BusinessException(3001, [order.order, order.overallStatus.toStringLocalized()])
        }
      }
      BigDecimal qty = request?.qty
      if (qty == null) {
        qty = w.qtyInQueue
      }
      qty = w.startQty(qty, request.dateTime)
      // now, log the action of the order
      ActionLog l = new ActionLog()
      l.action = ACTION_START
      l.qty = qty
      l.lsn = lsn
      l.order = order
      l.workCenter = request.workCenter
      l.save()

      res << new StartResponse(order: order, lsn: lsn, qty: qty)
    }

    log.debug('start() res = {}', res)
    return res
  }

  /**
   * Reverses the start of work on an Order/LSN.  The quantity processed is optional.
   * This moves the in work qty back to in queue.  No undo actions are supported for this.
   *
   * @param request The start request data itself.  This request is refined by the method
   * {@link org.simplemes.mes.demand.service.ResolveService#resolveProductionRequest}.
   * @return The order/LSN(s) that were reversed.
   */

  List<StartResponse> reverseStart(StartRequest request) {
    def res = []
    def now = new Date()
    resolveService.resolveProductionRequest(request, ResolveQuantityPreference.WORK)
    def resolveRequest = new ResolveWorkableRequest(order: request?.order,
                                                    lsn: request?.lsn,
                                                    qty: request?.qty,
                                                    operationSequence: request?.operationSequence)
    def workables = resolveService.resolveWorkable(resolveRequest)
    for (w in workables) {
      // Make sure the order for the workable can be worked.
      Order order = resolveService.determineOrderForWorkable(w)
      //order.lastUpdated = new Date()+1
      def lsn = resolveService.determineLSNForWorkable(w)
      BigDecimal qty = request?.qty
      if (qty == null) {
        qty = w.qtyInWork
      }
      def dateQtyStarted = w.dateQtyStarted
      qty = w.reverseStartQty(qty)

      // now, log the action of the order
      ActionLog l = new ActionLog()
      l.action = ACTION_REVERSE_START
      l.qty = qty
      l.lsn = lsn
      l.order = order
      l.workCenter = request.workCenter
      l.dateTime = request.dateTime ?: now
      l.save()

      // Log the production totals for this action
      def plRequest = new ProductionLogRequest(action: ACTION_REVERSE_START)
      plRequest.dateTime = request.dateTime ?: now
      plRequest.startDateTime = dateQtyStarted
      plRequest.order = order
      plRequest.lsn = lsn
      plRequest.workCenter = request.workCenter
      plRequest.qty = qty
      plRequest.qtyStarted = qty
      productionLogService.log(plRequest)

      res << new StartResponse(order: order, lsn: lsn, qty: qty, allowUndo: false)
    }

    return res
  }

  /**
   * Complete work on an Order/LSN.  The quantity processed is optional.
   * Below is a typical usage:
   * <pre>
   *   WorkService workService   // Allow automatic injection of WorkService
   *   void complete() {*     def request = new CompleteRequest()
   *     request.qty = 3.0
   *     request.order = Order.findByOrder('M001')
   *     Order results = workService.complete(request)
   *} </pre>
   *
   * @param request The complete request data itself.  This request is refined by the method
   * {@link org.simplemes.mes.demand.service.ResolveService#resolveProductionRequest}.
   * @return The order/LSN(s) that were completed.
   */
  @SuppressWarnings("AbcMetric")
  List<CompleteResponse> complete(CompleteRequest request) {
    def res = []
    def now = new Date()
    resolveService.resolveProductionRequest(request, ResolveQuantityPreference.WORK)
    def resolveRequest = new ResolveWorkableRequest(order: request?.order,
                                                    lsn: request?.lsn,
                                                    qty: request?.qty,
                                                    operationSequence: request?.operationSequence)
    def workables = resolveService.resolveWorkable(resolveRequest)
    for (w in workables) {
      // Make sure the order for the workable can be worked.
      Order order = resolveService.determineOrderForWorkable(w)
      if (order) {
        if (!order.overallStatus.isWorkable()) {
          // error.3001.message=Order {0} cannot be worked.  It has a status of {1}
          throw new BusinessException(3001, [order.order, order.overallStatus.toStringLocalized()])
        }
      }
      BigDecimal qty = request?.qty
      if (qty == null) {
        qty = w.qtyInWork
        request.qty = qty
      }
      def dateQtyStarted = w.dateQtyStarted
      qty = w.completeQty(qty)
      def nextWorkable = w.determineNextWorkable()
      if (nextWorkable) {
        nextWorkable.queueQty(qty)
      } else {
        // No more stuff to work, so see if we should mark the order as done.
        order?.checkForOrderDone(request)
      }
      // now, log the action of the order
      ActionLog l = new ActionLog()
      l.action = ACTION_COMPLETE
      l.qty = qty
      def lsn = resolveService.determineLSNForWorkable(w)
      l.lsn = lsn
      l.order = resolveService.determineOrderForWorkable(w)
      l.workCenter = request.workCenter
      l.dateTime = request.dateTime ?: now
      l.save()

      // Log the production totals for this action
      def plRequest = new ProductionLogRequest(action: ACTION_COMPLETE)
      plRequest.dateTime = request.dateTime ?: now
      plRequest.startDateTime = dateQtyStarted
      plRequest.order = order
      plRequest.lsn = lsn
      plRequest.workCenter = request.workCenter
      plRequest.qty = qty
      plRequest.qtyStarted = qty
      plRequest.qtyCompleted = qty
      productionLogService.log(plRequest)

      res << new CompleteResponse(order: order, lsn: lsn, qty: qty, done: order.overallStatus.done)
    }

    return res
  }

  /**
   * Reverses the complete of work on an Order/LSN.  The quantity processed is optional.
   * This moves the done qty back to in queue.  No undo actions are supported for this.
   *
   * @param request The complete request data itself.  This request is refined by the method
   * {@link org.simplemes.mes.demand.service.ResolveService#resolveProductionRequest}.
   * @return The order/LSN(s) that were reversed.
   */

  List<CompleteResponse> reverseComplete(CompleteRequest request) {
    def res = []
    def now = new Date()
    resolveService.resolveProductionRequest(request, ResolveQuantityPreference.DONE)
    def resolveRequest = new ResolveWorkableRequest(order: request?.order,
                                                    lsn: request?.lsn,
                                                    qty: request?.qty,
                                                    operationSequence: request?.operationSequence)
    def workables = resolveService.resolveWorkable(resolveRequest)
    for (w in workables) {
      // Make sure the order for the workable can be worked.
      Order order = resolveService.determineOrderForWorkable(w)
      //order.lastUpdated = new Date()+1
      def lsn = resolveService.determineLSNForWorkable(w)
      BigDecimal qty = request?.qty
      if (qty == null) {
        qty = w.qtyInWork
      }
      def dateQtyStarted = w.dateQtyStarted
      qty = w.reverseCompleteQty(qty)

      // now, log the action of the order
      ActionLog l = new ActionLog()
      l.action = ACTION_REVERSE_COMPLETE
      l.qty = qty
      l.lsn = lsn
      l.order = order
      l.workCenter = request.workCenter
      l.dateTime = request.dateTime ?: now
      l.save()

      // Log the production totals for this action
      def plRequest = new ProductionLogRequest(action: ACTION_REVERSE_COMPLETE)
      plRequest.dateTime = request.dateTime ?: now
      plRequest.startDateTime = dateQtyStarted
      plRequest.order = order
      plRequest.lsn = lsn
      plRequest.workCenter = request.workCenter
      plRequest.qty = -qty
      plRequest.qtyStarted = -qty
      productionLogService.log(plRequest)

      res << new StartResponse(order: order, lsn: lsn, qty: qty, allowUndo: false)
    }

    return res
  }

}


