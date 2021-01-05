package org.simplemes.mes.demand.controller

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
import org.simplemes.eframe.controller.BaseController
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.eframe.web.ui.webix.ToolkitConstants
import org.simplemes.mes.demand.FindWorkRequest
import org.simplemes.mes.demand.service.WorkListService
import org.simplemes.mes.floor.domain.WorkCenter

import javax.annotation.Nullable
import javax.inject.Inject
import java.security.Principal

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The controller for the core MES selection activities.
 *
 *  <h3>Logging</h3>
 * The logging for this class that can be enabled:
 * <ul>
 *   <li><b>debug</b> - Debugging information. Typically includes inputs and outputs. </li>
 * </ul>
 */
@Slf4j
@Secured('OPERATOR')
@Controller("/workList")
class WorkListController extends BaseController {

  /**
   * The service used to satisfy the work list request.
   */
  @Inject
  WorkListService workListService

  /**
   * Displays the core workList activity page.
   * @param request The request.
   * @param principal The user logged in.
   * @return The model/view to display.
   */
  @Produces(MediaType.TEXT_HTML)
  @Get("/workListActivity")
  StandardModelAndView workListActivity(HttpRequest request, @Nullable Principal principal) {
    def view = "demand/workList/workList"
    def modelAndView = new StandardModelAndView(view, principal, this)

    // No model is used here.
    return modelAndView
  }


  /**
   * Provides a list of active/queued work.  This exposes the
   * {@link org.simplemes.mes.demand.service.WorkListService#findWork(org.simplemes.mes.demand.FindWorkRequest)} method.
   * The response is a JSON form used for data lists for the toolkit (e.g. data:...).
   * The input objects supported are JSON forms from that method.
   * <p>
   * <b>Note:</b> The max/offset values are typically overridden by the value(s) passed in as a HTTP URL parameter.
   *
   */
  @Get('/findWork')
  HttpResponse findWork(HttpRequest request, @Nullable Principal principal) {
    def params = ControllerUtils.instance.convertToMap(request.parameters)
    def (from, size) = ControllerUtils.instance.calculateFromAndSizeForList(params)
    def (String sortField, String sortDir) = ControllerUtils.instance.calculateSortingForList(params)
    sortField = sortField ?: 'order'
    sortDir = sortDir ?: 'asc'

    def findWorkResponse = null
    WorkCenter.withTransaction {
      def findWorkRequest = new FindWorkRequest()
      if (params.workCenter) {
        findWorkRequest.workCenter = WorkCenter.findByWorkCenter((String) params.workCenter)
      }
      findWorkRequest.max = size ?: findWorkRequest.max
      findWorkRequest.from = from ?: findWorkRequest.from
      if (params.findInWork) {
        findWorkRequest.findInWork = Boolean.valueOf((String) params.findInWork)
      }
      if (params.findInQueue) {
        findWorkRequest.findInQueue = Boolean.valueOf((String) params.findInQueue)
      }
      //println "workListService = $workListService"
      log.debug('findWork() findWorkRequest: {}', findWorkRequest)
      findWorkResponse = workListService.findWork(findWorkRequest)
      //DomainUtils.instance.resolveProxies(findWorkResponse)
    }

    def json = Holders.objectMapper.writeValueAsString([data: findWorkResponse.list, total_count: findWorkResponse.totalAvailable,
                                                        pos : from * size, sort: sortField, sortDir: sortDir])

    return HttpResponse.status(HttpStatus.OK).body(json)
  }

  /**
   * Provides a suggestion list of active/queued work.  This exposes the
   * {@link org.simplemes.mes.demand.service.WorkListService#findWork(org.simplemes.mes.demand.FindWorkRequest)} method.
   * The response is a JSON form used for suggest lists ([ id: ..., value: ""]
   * Filtering is supported ('filter[value]').
   * <p>
   * <b>Note:</b> Paging (size, from) is supported, but the default is (100 rows from the first page).  Sorting is not supported.
   *
   * @return The list of matching work.  LSN or order if the LSN is null.  Returns nothing if the filter is empty/null.
   *
   */
  @Get('/suggest')
  HttpResponse suggest(HttpRequest request, @Nullable Principal principal) {
    def params = ControllerUtils.instance.convertToMap(request.parameters)
    def (from, size) = ControllerUtils.instance.calculateFromAndSizeForList(params)

    def res = []
    def filter = params[ToolkitConstants.SUGGEST_FILTER_PARAMETER_NAME] as String
    if (filter) {
      WorkCenter.withTransaction {
        def findWorkResponse
        def findWorkRequest = new FindWorkRequest()
        if (params.workCenter) {
          findWorkRequest.workCenter = WorkCenter.findByWorkCenter((String) params.workCenter)
        }
        findWorkRequest.max = size ?: 100  // Need a few more rows than the normal  findWork().
        findWorkRequest.from = from ?: findWorkRequest.from
        findWorkRequest.filter = filter.toUpperCase()
        //println "workListService = $workListService"
        log.debug('suggest() findWorkRequest: {}', findWorkRequest)
        findWorkResponse = workListService.findWork(findWorkRequest)
        //DomainUtils.instance.resolveProxies(findWorkResponse)
        for (row in findWorkResponse.list) {
          res << [id: row.id, value: row.lsn ?: row.order]
        }
      }
    }

    def json = Holders.objectMapper.writeValueAsString(res)
    return HttpResponse.status(HttpStatus.OK).body(json)
  }
}
