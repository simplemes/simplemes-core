package org.simplemes.mes.demand.controller

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import org.simplemes.eframe.controller.BaseCrudRestController
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.web.task.TaskMenuItem
import org.simplemes.mes.demand.OrderReleaseRequest
import org.simplemes.mes.demand.OrderUtils
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.service.OrderService

import javax.annotation.Nullable
import javax.inject.Inject
import java.security.Principal

//import org.simplemes.eframe.controller.ControllerErrorHandlerTrait

/**
 * Handles HTTP requests for the Order object.
 *
 * <h3>Logging</h3>
 * The logging for this class that can be enabled:
 * <ul>
 *   <li><b>debug</b> - Debugging information. Typically includes inputs and outputs. </li>
 * </ul>
 *
 */
@Slf4j
@Secured("SUPERVISOR")
@Controller("/order")
class OrderController extends BaseCrudRestController {
  /**
   * Injected OrderService.
   */
  @Inject
  OrderService orderService

  /**
   * Defines the entry(s) in the main Task Menu.
   */
  @SuppressWarnings("unused")
  def taskMenuItems = [new TaskMenuItem(folder: 'demand:500', name: 'order', uri: '/order', displayOrder: 510)]

  /**
   * API entry for release() action.  Supports JSON format using standard framework parsing.
   * Executes the {@link org.simplemes.mes.demand.service.OrderService#release(org.simplemes.mes.demand.OrderReleaseRequest)} method.
   */
  @Post(value = "/release")
  @SuppressWarnings("unused")
  HttpResponse release(HttpRequest request, @Nullable Principal principal) {
    def res = null
    Order.withTransaction {
      def orderReleaseRequest = (OrderReleaseRequest) parseBody(request, OrderReleaseRequest)
      if (!orderReleaseRequest) {
        res = buildErrorResponse("Empty body in request")
      } else {
        log.debug('release() request: {}', orderReleaseRequest)
        res = buildOkResponse(orderService.release(orderReleaseRequest))
      }
    }
    return res
  }

  /**
   * API entry for release() action.  Supports JSON format using standard framework parsing.
   * Executes the {@link org.simplemes.mes.demand.service.OrderService#release(org.simplemes.mes.demand.OrderReleaseRequest)} method.
   */
  @Produces(MediaType.TEXT_HTML)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Post("/releaseUI")
  HttpResponse releaseUI(HttpRequest request, @Body Object bodyParams, @Nullable Principal principal) {
    def uuid = UUID.fromString(bodyParams.uuid)
    def order = Order.findByUuid(uuid)
    if (!order) {
      return buildErrorResponse("Could not find order with uuid='$uuid'")
    }
    def orderReleaseRequest = new OrderReleaseRequest(order)
    log.debug('release() request: {}', orderReleaseRequest)
    def msg
    try {
      def res = orderService.release(orderReleaseRequest)
      msg = '?_info=' + URLEncoder.encode(GlobalUtils.lookup('released.message', null, order.order, res.qtyReleased), "UTF-8")
    } catch (BusinessException ex) {
      // Display any errors on the show page.
      msg = '?_error=' + URLEncoder.encode(ex.toString(), "UTF-8")
    }
    //released.message=Released {1} on order {0}
    return HttpResponse.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, "${rootPath}/show/${order.uuid}$msg")
  }

  /**
   * API entry for archiveOld() action.  Supports JSON in the content.
   * Calls the
   * {@link org.simplemes.mes.demand.service.OrderService#archiveOld(java.math.BigDecimal, java.lang.Integer, java.lang.Integer)}
   * method.
   * Archive old, done orders based on the date that they were completed.  There are options
   * for the age (days) and the max number of orders to archive in one batch (transaction).  There is also an option to limit the number
   * batches archived at a time.  This last option is used to limit the slowdown from archiving during 'catch-up' scenarios when
   * archiving has not been run recently.  Values that exceed the maximum will be ignored and the maximum will be used.
   * <p/>
   * This method is called automatically once per day using the job scheduler in production/dev environments.
   * <p/>
   * The JSON is in the format:
   * <pre>
   *{   "ageDays": 100,
   *   "maxOrdersPerTxn": 50,
   *   "maxTxns": 1
   *} </pre>
   * <p/>
   * Returns the archive file references in a JSON array:
   * <pre>
   *{   [
   *     "archives/M1001.arc"},
   *     "archives/M1002.arc"},
   *     "archives/M1003.arc"}*   ]
   *} </pre>
   *
   */
  @Post(value = "/archiveOld")
  @SuppressWarnings("unused")
  HttpResponse archiveOld(HttpRequest request, @Nullable Principal principal) {
    def res
    BigDecimal ageDays
    Integer maxOrdersPerTxn
    Integer maxTxns
    def map = (Map) parseBody(request, Map)
    if (!map) {
      res = buildErrorResponse("Empty body in request")
    } else {
      log.debug('archiveOld() request: {}', map)
      ageDays = ArgumentUtils.convertToBigDecimal(map.ageDays)
      maxOrdersPerTxn = ArgumentUtils.convertToInteger(map.maxOrdersPerTxn)
      maxTxns = ArgumentUtils.convertToInteger(map.maxTxns)
      res = buildOkResponse(orderService.archiveOld(ageDays, maxOrdersPerTxn, maxTxns))
    }

    return res
  }

  /**
   * API entry for determineQtyStates() action.  This is passed through to the OrderService.
   * Supports XML and JSON formats using standard framework parsing.
   * Executes the {@link org.simplemes.mes.demand.service.OrderService#determineQtyStates(org.simplemes.mes.demand.domain.Order)} method.
   * <p/>
   * This follows the normal REST API approach, using the order/LSN id/name as the parameter on the URL:
   * <code>/order/determineQtyStates/M1001</code>.  The parameter is evaluated with this precedence order:
   * <ul>
   *   <li>LSN - ID (numeric)</li>
   *   <li>Order - ID (numeric)</li>
   *   <li>LSN - name</li>
   *   <li>Order - name</li>
   * </ul>
   */
  @Get("/determineQtyStates/{id}")
  @SuppressWarnings(["GroovyAssignabilityCheck", "unused"])
  HttpResponse determineQtyStates(@PathVariable(name = 'id') String id, @Nullable Principal principal) {
    log.debug('determineQtyStates() id: {}', id)
    def object = OrderUtils.resolveUuidOrName(id)
    if (object) {
      def workables = orderService.determineQtyStates(object)
      def qtyInQueue = 0.0
      def qtyInWork = 0.0
      for (o in workables) {
        qtyInQueue += o.qtyInQueue
        qtyInWork += o.qtyInWork
      }
      def map = [qtyInQueue: qtyInQueue, qtyInWork: qtyInWork, object: object]
      return buildOkResponse(map)
    }
    return HttpResponse.status(HttpStatus.NOT_FOUND)
  }

}
