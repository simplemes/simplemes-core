/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.controller


import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import org.simplemes.eframe.controller.BaseCrudRestController
import org.simplemes.eframe.i18n.GlobalUtils
import sample.domain.SampleParent

/**
 * Test controller for extension mechanism.
 */
@Secured("MANAGER")
@Controller("/sample")
class SampleController extends BaseCrudRestController {

  static domainClass = SampleParent

  @Secured(SecurityRule.IS_ANONYMOUS)
  @Get("/locale")
  String locale() {
    //println "in locale()"
    return GlobalUtils.lookup('home.label')
  }

  @Secured(SecurityRule.IS_ANONYMOUS)
  @Post("/throwsException")
  String throwsException() {
    throw new IllegalArgumentException('a bad argument')
  }

  @Secured(SecurityRule.IS_ANONYMOUS)
  @Post("/throwsInfiniteException")
  String throwsInfiniteException() {
    throw new IllegalArgumentException('an infinite exception loop')
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
    return "sample/sampleParent/$methodName"
  }

}
