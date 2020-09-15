/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security

import io.micronaut.context.annotation.Replaces
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.cookie.Cookie
import io.micronaut.security.config.RedirectConfiguration
import io.micronaut.security.token.jwt.cookie.JwtCookieClearerLogoutHandler
import io.micronaut.security.token.jwt.cookie.JwtCookieConfiguration
import org.simplemes.eframe.security.service.RefreshTokenService

import javax.inject.Singleton

/**
 * A logout handler that clears the JWT and Access cookies upon logout.
 */
@Singleton
@Replaces(JwtCookieClearerLogoutHandler)
class CookieClearingLogoutHandler extends JwtCookieClearerLogoutHandler {
  /**
   * @param jwtCookieConfiguration JWT Cookie Configuration
   * @param redirectConfiguration Redirect configuration
   */
  CookieClearingLogoutHandler(JwtCookieConfiguration jwtCookieConfiguration, RedirectConfiguration redirectConfiguration) {
    super(jwtCookieConfiguration, redirectConfiguration)
  }

  @Override
  MutableHttpResponse<?> logout(HttpRequest<?> request) {
    def res = super.logout(request)
    Cookie cookie = Cookie.of(RefreshTokenService.JWT_REFRESH_TOKEN, "")
    cookie.maxAge(0).path("/")
    res.cookie(cookie)
    return res
  }
}
