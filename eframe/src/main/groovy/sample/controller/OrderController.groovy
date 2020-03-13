/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.controller

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.controller.BaseCrudRestController
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.eframe.web.task.TaskMenuItem
import sample.domain.Order
import sample.pogo.FindWorkResponse
import sample.pogo.FindWorkResponseDetail

import javax.annotation.Nullable
import java.security.Principal

/**
 * Handles the controller actions for the Order objects.  Includes CRUD actions on the user.
 * <p>
 * Not shipped with the framework.
 */
@Slf4j
@Secured("MANAGER")
@Controller("/order")
@SuppressWarnings("unused")
class OrderController extends BaseCrudRestController {

  /**
   * The number of rows available for the dummy findWork() method.
   */
  public static final int WORK_RECORD_COUNT = 105

  static domainClass = Order

  /**
   * Defines the entry(s) in the main Task Menu.
   */
  def taskMenuItems = [new TaskMenuItem(folder: 'sample:9500', name: 'order', uri: '/order',
                                        displayOrder: 9710, clientRootActivity: true)]

  @Get("/orderWorkList")
  @Produces(MediaType.TEXT_HTML)
  StandardModelAndView orderWorkList(@Nullable Principal principal) {
    def res = new StandardModelAndView("sample/order/orderWorkList", principal, this)
    return res
  }

  /**
   * The dummy work list values.
   */
  static workList

  /**
   * Returns a list (JSON formatted) that contains a POGO list of values.  Dummy static list.
   * @param request The request.
   * @param principal The user logged in.
   * @return The data for the list.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  @Get("/findWork")
  HttpResponse findWork(HttpRequest request, @Nullable Principal principal) {
    def params = ControllerUtils.instance.convertToMap(request.parameters)
    def (from, max) = ControllerUtils.instance.calculateFromAndSizeForList(params)
    def (String sortField, String sortDir) = ControllerUtils.instance.calculateSortingForList(params)
    sortField = sortField ?: 'order'
    sortDir = sortDir ?: 'asc'

    // Build some dummy data
    if (!workList) {
      def products = ['BIKE-27', 'BIKE-24', 'BIKE-21']
      def workCenter = params.workCenter ?: ''
      workList = []
      def rng = new Random(1)
      for (i in (1..WORK_RECORD_COUNT)) {
        def qtyInQueue = 10.0 * rng.nextDouble() as BigDecimal
        def qtyInWork = 10.0 * rng.nextDouble() as BigDecimal
        def order = "M1${sprintf("%03d", i)}"
        def product = products[rng.nextInt(products.size())]
        workList << new FindWorkResponseDetail(qtyInQueue: qtyInQueue, order: order, qtyInWork: qtyInWork,
                                               product: product, workCenter: workCenter)
      }
    }

    // Now, sort the list as needed
    workList.sort { FindWorkResponseDetail a, FindWorkResponseDetail b ->
      if (sortDir == 'desc') {
        // Swap the elements to compare
        def x = b
        b = a
        a = x
      }
      def valueA = a[sortField]
      def valueB = b[sortField]
      if (valueA != null && valueB != null) {
        return valueA <=> valueB
      }
      return 0
    }

    def response = new FindWorkResponse()
    response.totalAvailable = workList.size()
    response.list = []

    // Find the right records
    int start = from * max
    for (int i = 0; i <= max; i++) {
      if (start + i < workList.size()) {
        response.list << (workList[start + i] as FindWorkResponseDetail)
      }
    }

    def json = Holders.objectMapper.writeValueAsString([data: response.list, total_count: response.totalAvailable,
                                                        pos : from * max, sort: sortField, sortDir: sortDir])
    return HttpResponse.status(HttpStatus.OK).body(json)
  }


  /**
   * Determines the view to display for the given method.  This can be overridden in your controller class to
   * use a different naming scheme.<p>
   * This sub-class points to a sample directory.
   * @param methodName The method that needs the view (e.g. 'index').
   * @return The resulting view path.
   */
  @Override
  String getView(String methodName) {
    return "sample/order/$methodName"
  }
}