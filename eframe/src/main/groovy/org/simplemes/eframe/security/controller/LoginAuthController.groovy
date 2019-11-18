package org.simplemes.eframe.security.controller


import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.views.View

import javax.annotation.Nullable

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Handles the login authentication view pages.
 */
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/login")
class LoginAuthController {

  @Get("/auth{?target}")
  @View("home/auth")
  Map<String, Object> auth(@Nullable String target) {
    //println "auth() target = $target"
    return [target: target]
  }

  @Get("/authFailed")
  @View("home/auth")
  Map<String, Object> authFailed() {
    return Collections.singletonMap("errors", true)
  }


}