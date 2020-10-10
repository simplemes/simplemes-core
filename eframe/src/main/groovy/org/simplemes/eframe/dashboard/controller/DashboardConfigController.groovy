/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.controller

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import org.simplemes.eframe.controller.BaseCrudRestController
import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.eframe.dashboard.domain.DashboardConfig

import javax.annotation.Nullable
import java.security.Principal

/**
 * Provides definition-time access to a dashboard configuration.  Mainly used for CRUD/REST access.  No GUI access.
 */
@Slf4j
@Secured(['ADMIN', 'DESIGNER'])
@Controller('/dashboardConfig')
class DashboardConfigController extends BaseCrudRestController {

  /**
   * Specify the domain for this controller.
   */
  @SuppressWarnings('unused')
  static domainClass = DashboardConfig


  /**
   * Displays the editor dialog page.
   * @param principal The user logged in.
   * @return The model/view to display.
   */
  @Produces(MediaType.TEXT_HTML)
  @Get("/editor")
  StandardModelAndView editor(HttpRequest request, @Nullable Principal principal) {
    def modelAndView = new StandardModelAndView("dashboard/editor", principal, this)
    log.trace('index(): {}', modelAndView)
    return modelAndView
  }

  /**
   * Contains the details dialog for a single dashboard.
   */
  def detailsDialog() {
  }

  /**
   * Contains the panel details dialog for a panel
   */
  def panelDetailsDialog() {
  }

  /**
   * Contains the button details dialog for a button.
   */
  def buttonDetailsDialog() {
  }

}
