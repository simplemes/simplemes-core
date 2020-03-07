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

  static domainClass = Order

  /**
   * Defines the entry(s) in the main Task Menu.
   */
  def taskMenuItems = [new TaskMenuItem(folder: 'sample:9500', name: 'order', uri: '/order',
                                        displayOrder: 9710, clientRootActivity: true)]

  @Get("/orderWorkList")
  @Produces(MediaType.TEXT_HTML)
  StandardModelAndView orderWorkList(@Nullable Principal principal) {
    return new StandardModelAndView("sample/order/orderWorkList", principal, this)
  }

  /**
   * Returns a list (JSON formatted) that contains a POGO list of values.
   * @param request The request.
   * @param principal The user logged in.
   * @return The data for the list.
   */
  @Get("/findWork")
  HttpResponse findWork(HttpRequest request, @Nullable Principal principal) {
    def params = ControllerUtils.instance.convertToMap(request.parameters)
    def (from, max) = ControllerUtils.instance.calculateFromAndSizeForList(params)
    def (sortField, sortDir) = ControllerUtils.instance.calculateSortingForList(params)

    // Build some dummy data
    def response = new FindWorkResponse()
    response.totalAvailable = 1
    response.list = [new FindWorkResponseDetail(qtyInQueue: 237.1, order: 'M1003', qtyInWork: 437.2)]


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