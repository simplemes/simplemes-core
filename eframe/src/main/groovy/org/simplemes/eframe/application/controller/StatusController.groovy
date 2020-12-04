/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.application.controller

import groovy.util.logging.Slf4j
import io.micrometer.core.instrument.MeterRegistry
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.views.ModelAndView
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.application.ModuleUtils
import org.simplemes.eframe.controller.BaseController
import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.eframe.web.task.TaskMenuItem

import javax.annotation.Nullable
import java.security.Principal

/**
 * Status controller.
 */
@Slf4j
@Secured(SecurityRule.IS_AUTHENTICATED)
@Controller("/status")
@SuppressWarnings(["GroovyUnusedAssignment", "unused"])
class StatusController extends BaseController {

  /**
   * Defines the entry(s) in the main Task Menu.
   */
  @SuppressWarnings("unused")
  def taskMenuItems = [new TaskMenuItem(folder: 'admin:7000', name: 'systemStatus', uri: '/status',
                                        displayOrder: 7010, clientRootActivity: true)]


  /**
   * Displays the index page.  
   * @param principal The user logged in.
   * @return The model/view to display.
   */
  @Produces(MediaType.TEXT_HTML)
  @Get("/")
  ModelAndView status(@Nullable Principal principal) {
    def modelAndView = new StandardModelAndView("status/status", principal, this)
    Map model = (Map) modelAndView.model.get()
    model.modules = ModuleUtils.instance.getModules()

    // See if the metrics are available
    MeterRegistry registry = Holders.getBean(MeterRegistry)
    model.checkRequests = (registry != null)

    log.debug('status(): {}', modelAndView)
    return modelAndView
  }

}
