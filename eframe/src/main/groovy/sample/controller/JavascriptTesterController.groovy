package sample.controller


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
 * A simple controller to support javascript library testing.
 * Simply serves a page with the standard libraries loaded.
 */
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/javascriptTester")
class JavascriptTesterController {

  /**
   * Displays the index page.
   * @param principal The user logged in.
   * @return The model/view to display.
   */
  @Produces(MediaType.TEXT_HTML)
  @Get("/")
  StandardModelAndView index(@Nullable Principal principal) {
    def modelAndView = new StandardModelAndView("sample/javascript/index", principal, this)

    // No model is used here.
    return modelAndView
  }


}
