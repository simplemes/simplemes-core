/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security

import edu.umd.cs.findbugs.annotations.Nullable
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import io.micronaut.core.util.StringUtils
import io.micronaut.http.HttpRequest
import io.micronaut.http.cookie.Cookie
import io.micronaut.security.config.RedirectConfiguration
import io.micronaut.security.errors.PriorToLoginPersistence
import io.micronaut.security.token.jwt.cookie.JwtCookieConfiguration
import io.micronaut.security.token.jwt.cookie.JwtCookieLoginHandler
import io.micronaut.security.token.jwt.generator.AccessRefreshTokenGenerator
import io.micronaut.security.token.jwt.generator.AccessTokenConfiguration
import io.micronaut.security.token.jwt.render.AccessRefreshToken
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.application.issues.WorkArounds

import javax.inject.Singleton

/**
 *
 */

@Requires(property = "micronaut.security.authentication", value = "cookie")
@Singleton
@Replaces(JwtCookieLoginHandler)
class WorkAround333CookieLoginHandler extends JwtCookieLoginHandler {

  /**
   * The default max age for the workaround JWT Refresh Cookie (seconds).  Set in EFrameConfiguration.jwtRefreshMaxAge.
   * This should be set in the micronaut-security configuration when issue 333 is fixed.
   */
  public static Long DEFAULT_MAX_AGE = 2592000 // 30 days.

  /**
   * @param redirectConfiguration Redirect configuration
   * @param jwtCookieConfiguration JWT Cookie Configuration
   * @param accessTokenConfiguration JWT Generator Configuration
   * @param accessRefreshTokenGenerator Access Refresh Token Generator
   * @param priorToLoginPersistence Prior To Login Persistence Mechanism
   */
  WorkAround333CookieLoginHandler(RedirectConfiguration redirectConfiguration, JwtCookieConfiguration jwtCookieConfiguration, AccessTokenConfiguration accessTokenConfiguration, AccessRefreshTokenGenerator accessRefreshTokenGenerator, @Nullable PriorToLoginPersistence priorToLoginPersistence) {
    super(redirectConfiguration, jwtCookieConfiguration, accessTokenConfiguration, accessRefreshTokenGenerator, priorToLoginPersistence)
  }

  /**
   * Return the cookies for the given parameters.
   *
   * @param accessRefreshToken The access refresh token
   * @param request The current request
   * @return A list of cookies
   */
  @Override
  protected List<Cookie> getCookies(AccessRefreshToken accessRefreshToken, HttpRequest<?> request) {
    def cookies = super.getCookies(accessRefreshToken, request)
    def res = []
    if (WorkArounds.workAround333) {
      for (cookie in cookies) {
        if (cookie.name == 'JWT_REFRESH_TOKEN') {
          String refreshToken = accessRefreshToken.getRefreshToken()
          if (StringUtils.isNotEmpty(refreshToken)) {
            Cookie refreshCookie = Cookie.of("JWT_REFRESH_TOKEN", refreshToken)
            refreshCookie.configure(jwtCookieConfiguration, request.isSecure())
            refreshCookie.maxAge(Holders.configuration.security.jwtRefreshMaxAge)
            res << refreshCookie
          }
        } else {
          res << cookie
        }
      }
      return res
    }

    return cookies
  }
}