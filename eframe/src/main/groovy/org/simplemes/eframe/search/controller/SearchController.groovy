/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search.controller

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import org.simplemes.eframe.controller.BaseController
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.search.SearchStatus
import org.simplemes.eframe.search.service.SearchService
import org.simplemes.eframe.web.task.TaskMenuItem

import javax.annotation.Nullable
import javax.inject.Inject
import java.security.Principal

/**
 * Global System control settings.  These control functions/behaviors in the system.
 * This controller provides access to the settings.
 */
@Slf4j
@Secured(SecurityRule.IS_AUTHENTICATED)
@Controller("/search")
class SearchController extends BaseController {

  @Inject
  SearchService searchService

  /**
   * Defines the standard end-user task entry points that this controller handles.
   */
  @SuppressWarnings("GroovyUnusedDeclaration")
  def taskMenuItems = [new TaskMenuItem(folder: 'search:6000', name: 'globalSearch', uri: '/search', displayOrder: 6010),
                       new TaskMenuItem(folder: 'admin:7000', name: 'searchAdmin', uri: '/search/admin', displayOrder: 7030)]

  /**
   * Displays the search index page.
   * @param principal The user logged in.
   * @return The model/view to display.
   */
  @Produces(MediaType.TEXT_HTML)
  @Get("/")
  StandardModelAndView index(HttpRequest request, @Nullable Principal principal) {
    def modelAndView = new StandardModelAndView("search/index", principal, this)
    def params = ControllerUtils.instance.convertToMap(request.parameters)

    def res = [totalHits: 237]
    if (params.query) {
      res = searchService.globalSearch((String) params.query, params)
      //println "res = $res"
    }
    modelAndView.model.get().searchResult = res
    modelAndView.model.get().searchStatus = searchService.status

    log.trace('index(): {}', modelAndView)
    return modelAndView
  }

  /**
   * Displays the search admin page.
   * @param principal The user logged in.
   * @return The model/view to display.
   */
  @Secured(["ADMIN"])
  @Produces(MediaType.TEXT_HTML)
  @Get("/admin")
  StandardModelAndView admin(@Nullable Principal principal) {
    def modelAndView = new StandardModelAndView("search/admin", principal, this)
    modelAndView.model.get().searchStatus = searchService.status

    log.trace('admin(): {}', modelAndView)
    return modelAndView
  }

  /**
   * Returns the current search engine status as JSON/XML.
   * @return The status as JSON.
   */
  @Secured(["ADMIN"])
  @Get("/status")
  @SuppressWarnings('unused')
  SearchStatus status(@Nullable Principal principal) {
    return searchService.status
  }

  /**
   * Starts the bulk index request.  This queues up the index requests needed.
   * The index delete is run synchronously, then the index request are queued after that.
   *
   * @param deleteAllIndices If true, then this triggers a delete of all indices before the request is started.
   */
  @Secured(["ADMIN"])
  @Get("/startBulkIndexRequest")
  @SuppressWarnings('unused')
  def startBulkIndexRequest(HttpRequest request, @Nullable Principal principal) {
    def params = ControllerUtils.instance.convertToMap(request.parameters)
    def deleteFlag = ArgumentUtils.convertToBoolean(params.deleteAllIndices)
    searchService.startBulkIndexRequest(deleteFlag)
    return [status: 'ok']
  }

  /**
   * Clears the current statistics.
   */
  @Secured(["ADMIN"])
  @Get("/clearStatistics")
  @SuppressWarnings('unused')
  def clearStatistics(@Nullable Principal principal) {
    searchService.clearStatistics()
    return [status: 'ok']
  }
}
