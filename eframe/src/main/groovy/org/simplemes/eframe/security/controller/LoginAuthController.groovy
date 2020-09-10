/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security.controller

import edu.umd.cs.findbugs.annotations.NonNull
import io.micronaut.core.io.Writable
import io.micronaut.core.util.StringUtils
import io.micronaut.http.HttpMethod
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.CookieValue
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.http.simple.SimpleHttpRequest
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.UserDetails
import io.micronaut.security.errors.CookiePriorToLoginPersistence
import io.micronaut.security.errors.IssuingAnAccessTokenErrorCode
import io.micronaut.security.errors.OauthErrorResponseException
import io.micronaut.security.handlers.LoginHandler
import io.micronaut.security.rules.SecurityRule
import io.micronaut.security.token.generator.RefreshTokenGenerator
import io.micronaut.security.token.jwt.endpoints.TokenRefreshRequest
import io.micronaut.security.token.jwt.generator.AccessRefreshTokenGenerator
import io.micronaut.security.token.refresh.RefreshTokenPersistence
import io.micronaut.security.token.validator.RefreshTokenValidator
import io.micronaut.views.ViewsRenderer
import io.reactivex.Single
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


  protected final RefreshTokenPersistence refreshTokenPersistence
  protected final RefreshTokenValidator refreshTokenValidator
  protected final AccessRefreshTokenGenerator accessRefreshTokenGenerator
  protected final RefreshTokenGenerator refreshTokenGenerator
  protected final LoginHandler loginHandler


  /**
   * @param refreshTokenPersistence The persistence mechanism for the refresh token
   * @param refreshTokenValidator The refresh token validator
   */
  LoginAuthController(RefreshTokenPersistence refreshTokenPersistence,
                      RefreshTokenValidator refreshTokenValidator,
                      AccessRefreshTokenGenerator accessRefreshTokenGenerator,
                      RefreshTokenGenerator refreshTokenGenerator,
                      LoginHandler loginHandler) {
    this.refreshTokenPersistence = refreshTokenPersistence
    this.refreshTokenValidator = refreshTokenValidator
    this.accessRefreshTokenGenerator = accessRefreshTokenGenerator
    this.refreshTokenGenerator = refreshTokenGenerator
    this.loginHandler = loginHandler
  }

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
  HttpResponse auth(HttpRequest request, @Nullable Principal principal) {
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

  // This section is cloned somewhat from the io.micronaut.security.token.jwt.endpoints.OauthController


  @Produces(MediaType.TEXT_HTML)
  @Get("/access_token")
  Single<MutableHttpResponse<?>> accessToken(HttpRequest<?> request,
                                             @Nullable @CookieValue("JWT_REFRESH_TOKEN") String cookieRefreshToken) {
    String refreshToken = resolveRefreshToken(null, cookieRefreshToken)
    return createResponse(request, refreshToken)
  }


  private Single<MutableHttpResponse<?>> createResponse(HttpRequest<?> request, String refreshToken) {
    Optional<String> validRefreshToken = refreshTokenValidator.validate(refreshToken)
    if (!validRefreshToken.isPresent()) {
      throw new OauthErrorResponseException(IssuingAnAccessTokenErrorCode.INVALID_GRANT, "Refresh token is invalid", null)
    }
    return Single.fromPublisher(refreshTokenPersistence.getUserDetails(validRefreshToken.get()))
      .map(userDetails -> createSuccessResponse(userDetails, refreshToken, request))
  }

  /**
   * Creates a simple page for the response (suitable for iframe use).
   * Contains the cookies needed for access and refresh tokens.
   *
   * @param userDetails Authenticated user's representation.
   * @param refreshToken The refresh token
   * @param request The {@link HttpRequest} being executed
   * @return An HTTP Response. Eg. a redirect or an JWT token rendered to the response
   */
  MutableHttpResponse<?> createSuccessResponse(UserDetails userDetails, String refreshToken, HttpRequest<?> request) {
    def res = loginHandler.loginRefresh(userDetails, refreshToken, request)
    res.status(HttpStatus.OK)
    return res.body("<html><body>Ok</body></html>")
  }


  /**
   * Resolves the refresh token from the internal store.
   * @param tokenRefreshRequest
   * @param cookieRefreshToken
   * @return
   */
  @NonNull
  @SuppressWarnings('ChangeToOperator')
  private String resolveRefreshToken(TokenRefreshRequest tokenRefreshRequest, String cookieRefreshToken) {
    String refreshToken = null
    if (tokenRefreshRequest != null) {
      if (StringUtils.isEmpty(tokenRefreshRequest.getGrantType()) || StringUtils.isEmpty(tokenRefreshRequest.getRefreshToken())) {
        throw new OauthErrorResponseException(IssuingAnAccessTokenErrorCode.INVALID_REQUEST, "refresh_token and grant_type are required", null)
      }
      if (!tokenRefreshRequest.getGrantType().equals(TokenRefreshRequest.GRANT_TYPE_REFRESH_TOKEN)) {
        throw new OauthErrorResponseException(IssuingAnAccessTokenErrorCode.UNSUPPORTED_GRANT_TYPE, "grant_type must be refresh_token", null)
      }
      refreshToken = tokenRefreshRequest.getRefreshToken()
    } else if (cookieRefreshToken != null) {
      refreshToken = cookieRefreshToken
    }
    if (StringUtils.isEmpty(refreshToken)) {
      throw new OauthErrorResponseException(IssuingAnAccessTokenErrorCode.INVALID_REQUEST, "refresh_token is required", null)
    }
    return refreshToken
  }

}