package sample.controller

import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import org.simplemes.eframe.controller.StandardModelAndView

import javax.annotation.Nullable
import java.security.Principal

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A simple controller to support UI view testing.
 * Simply serves a page with the standard libraries loaded.
 */
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/pageTester")
class pageTesterController {

  /**
   * Displays the given page (by default the index.hbs).
   * @param request The request.
   * @param principal The user logged in.
   * @return The model/view to display.
   */
  @Produces(MediaType.TEXT_HTML)
  @Get("/")
  StandardModelAndView index(HttpRequest request, @Nullable Principal principal) {
    def view = "sample/pageTester/index"
    def page = request.parameters.get('page')
    if (page) {
      view = "sample/pageTester/$page"
    }
    //println "view = $view"
    def modelAndView = new StandardModelAndView(view, principal, this)

    // No model is used here.
    return modelAndView
  }


}
