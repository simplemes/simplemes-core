/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security.controller

import io.micronaut.core.io.Writable
import io.micronaut.http.HttpMethod
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.http.simple.SimpleHttpRequest
import io.micronaut.security.annotation.Secured
import io.micronaut.security.errors.CookiePriorToLoginPersistence
import io.micronaut.security.rules.SecurityRule
import io.micronaut.views.ViewsRenderer
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.controller.StandardModelAndView

import javax.annotation.Nullable
import java.security.Principal

/**
 * Handles the login authentication view pages.
 */
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/login")
class LoginAuthController {

/*
  @Get("/auth{?target}")
  @View("home/auth")
  Map<String, Object> auth(@Nullable String target) {
    //println "auth() target = $target"
    return [target: target]
  }
*/

  @Produces(MediaType.TEXT_HTML)
  @Get("/auth")
  HttpResponse index(HttpRequest request, @Nullable Principal principal) {
    def modelAndView = new StandardModelAndView('home/auth', principal, this)
    def params = ControllerUtils.instance.convertToMap(request.parameters)
    def renderer = Holders.applicationContext.getBean(ViewsRenderer)
    Writable writable = renderer.render(modelAndView.view.get(), modelAndView.model.get())

    def response = HttpResponse.status(HttpStatus.OK).body(writable)
    def handler = Holders.getBean(CookiePriorToLoginPersistence)
    if (handler) {
      // Make a dummy request to simulate the normal authorization failure.
      // This allows us to create the cookie for normal redirection after login success.
      def uri = params.uri
      if (!uri) {
        def originalURICookie = request.cookies.get('ORIGINAL_URI')
        uri = originalURICookie?.value ?: "/"
      }
      def request2 = new SimpleHttpRequest(HttpMethod.GET, (String) uri, null)
      handler.onUnauthorized(request2, response)
    }

    return response
  }

}