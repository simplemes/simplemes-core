/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.system.controller


import groovy.util.logging.Slf4j
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.system.DemoDataLoaderInterface

import javax.annotation.Nullable
import java.security.Principal

/**
 * This controller provides a way to create demo data.  Will call all beans that implement the
 * {@link org.simplemes.eframe.system.DemoDataLoaderInterface}.
 */
@Slf4j
@Secured("ADMIN")
@Controller("/demoData")
class DemoDataController {

  /**
   * Displays the index page.  Requires a view  '{domain}/index' for the given domain.
   * @param principal The user logged in.
   * @return The model/view to display.
   */
  @Produces(MediaType.TEXT_HTML)
  @Get("/")
  StandardModelAndView index(@Nullable Principal principal) {
    def modelAndView = new StandardModelAndView("demo/index", principal, this)
    Map model = (Map) modelAndView.model.get()

    // Load the data.
    def res = []
    User.withTransaction {
      def beans = Holders.applicationContext.getBeansOfType(DemoDataLoaderInterface)
      for (bean in beans) {
        log.debug("index(): Loading data from bean: {}", bean)
        def loaded = bean.loadDemoData()
        res.addAll(loaded)
      }
    }
    log.debug("index(): Loaded: {}", res)
    model.list = res

    log.trace('index(): {}', modelAndView)
    return modelAndView
  }


}
