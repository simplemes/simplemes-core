package org.simplemes.eframe.application.controller

import groovy.util.logging.Slf4j
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.views.ModelAndView
import org.simplemes.eframe.controller.BaseController
import org.simplemes.eframe.controller.StandardModelAndView

import javax.annotation.Nullable
import java.security.Principal

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The main index page (home).
 */
@Slf4j
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/")
@SuppressWarnings(["GroovyUnusedAssignment", "unused"])
class HomeController extends BaseController {


  /**
   * Displays the index page.  Requires a view  '{domain}/index' for the given domain.
   * @param principal The user logged in.
   * @return The model/view to display.
   */
  @Produces(MediaType.TEXT_HTML)
  @Get("/")
  ModelAndView index(@Nullable Principal principal) {
    def modelAndView = new StandardModelAndView("home/index", principal, this)
    log.debug('index(): {}', modelAndView)
    return modelAndView
  }

}
