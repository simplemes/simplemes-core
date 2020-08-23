/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.application.controller

import groovy.util.logging.Slf4j
import io.micronaut.core.io.Writable
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Error
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.views.ViewsRenderer
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.controller.BaseController
import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.eframe.i18n.GlobalUtils

/**
 * The global error handler.  Covers 404 errors and other common behaviors.
 */
@Slf4j
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/xyz")
class GlobalErrorController {

  /**
   * Standard page not found display.
   * @param request The HTTP request.
   * @return The error page contents.
   */
  @Error(status = HttpStatus.NOT_FOUND, global = true)
  HttpResponse notFound(HttpRequest request) {
    def accept = request.headers?.get(HttpHeaders.ACCEPT)
    if (accept?.contains(MediaType.TEXT_HTML)) {
      def modelAndView = new StandardModelAndView('home/error', null, this)
      modelAndView[StandardModelAndView.FLASH] = "Page '$request.uri' Not Found"
      modelAndView[StandardModelAndView.FLASH_DETAILS] = ''
      def renderer = Holders.applicationContext.getBean(ViewsRenderer)
      Writable writable = renderer.render(modelAndView.view.get(), modelAndView.model.get())

      return HttpResponse.status(HttpStatus.OK).contentType(MediaType.TEXT_HTML).body(writable)
    } else {
      return HttpResponse.status(HttpStatus.NOT_FOUND)
    }
  }

  /**
   * Handles the FORBIDDEN response from the security logic.  This indicates the user is not logged or
   * not allowed access.  This will redirect the GUI to a login or access denied page content.
   * @param request The request that was forbidden.
   * @return The redirect or a simple FORBIDDEN response.
   */
  @Error(status = HttpStatus.FORBIDDEN, global = true)
  HttpResponse forbidden(HttpRequest request) {
    def accept = request.headers?.get(HttpHeaders.ACCEPT)
    if (accept?.contains(MediaType.TEXT_HTML)) {
      return buildLoginRedirect(request)
    } else {
      return HttpResponse.status(HttpStatus.FORBIDDEN)
    }
  }

  /**
   * Builds a redirect (303) to the login page or the access denied response.
   * @param request The original request.
   * @return A Redirect to the login page or an access denied response.
   */
  static HttpResponse buildLoginRedirect(HttpRequest request) {
    def uri = URLEncoder.encode(request.uri.toString(), 'UTF-8')
    def response = HttpResponse.status(HttpStatus.SEE_OTHER).header(HttpHeaders.LOCATION, "/login/auth?uri=$uri")
    if (request.getUserPrincipal().isPresent()) {
      // Already logged in, so display the denied page instead.
      // roleMissing.message=Missing role(s) {0} for {1}
      def msg = GlobalUtils.lookup("roleMissing.message", '', request.uri.toString())
      response = BaseController.buildDeniedResponse(request, msg, request.getUserPrincipal().get())
    }

    return response
  }

}
