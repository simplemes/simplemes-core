/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.authentication.AuthenticationUserDetailsAdapter
import io.micronaut.security.filters.AuthenticationFetcher
import io.micronaut.security.token.config.TokenConfiguration
import io.micronaut.security.token.jwt.cookie.JwtCookieConfiguration
import io.micronaut.security.token.jwt.encryption.EncryptionConfiguration
import io.micronaut.security.token.jwt.signature.SignatureConfiguration
import io.micronaut.security.token.jwt.validator.GenericJwtClaimsValidator
import io.micronaut.security.token.jwt.validator.JwtTokenValidator
import io.micronaut.security.token.jwt.validator.JwtValidator
import io.reactivex.Flowable
import org.reactivestreams.Publisher
import org.simplemes.eframe.security.service.RefreshTokenService

import javax.inject.Singleton

/**
 * Authentication fetcher that will trigger a JWT refresh if possible on all requests.
 * Works with {@link AutoRefreshTokenFilter} to update the JWT cookies on browser for this response.
 */
@Slf4j
@Singleton
class AutoRefreshAuthenticationFetcher implements AuthenticationFetcher {

  /**
   * The name of the attribute in the request to hold the refresh response in case of Auto refresh.
   */
  static final String AUTO_REFRESH_ATTRIBUTE = '_JWT_REFRESH_PASSED'

  protected final JwtCookieConfiguration jwtCookieConfiguration
  protected final TokenConfiguration tokenConfiguration
  protected final JwtTokenValidator jwtTokenValidator
  protected final JwtValidator jwtValidator
  protected final RefreshTokenService refreshTokenService

  /**
   */
  AutoRefreshAuthenticationFetcher(JwtCookieConfiguration jwtCookieConfiguration,
                                   TokenConfiguration tokenConfiguration,
                                   JwtTokenValidator jwtTokenValidator,
                                   RefreshTokenService refreshTokenService,
                                   Collection<SignatureConfiguration> signatureConfigurations,
                                   Collection<EncryptionConfiguration> encryptionConfigurations,
                                   Collection<GenericJwtClaimsValidator> genericJwtClaimsValidators) {
    this.jwtCookieConfiguration = jwtCookieConfiguration
    this.jwtTokenValidator = jwtTokenValidator
    this.refreshTokenService = refreshTokenService
    this.tokenConfiguration = tokenConfiguration

    // This validator creation is cloned from JwtTokenValidator to avoid the publisher mess in that API.
    jwtValidator = JwtValidator.builder()
      .withSignatures(signatureConfigurations)
      .withEncryptions(encryptionConfigurations)
      .withClaimValidators(genericJwtClaimsValidators)
      .build()
  }


  @Override
  Publisher<Authentication> fetchAuthentication(HttpRequest<?> request) {
    if (request.path.startsWith('/assets/') || request.path == '/logout') {
      // No need to authenticate assets.
      return Flowable.empty()
    }
    // If no valid JWT token, then generate one if REFRESH_TOKEN is good.
    //   Generate new token response and store in request for later use by the AutoRefreshFilter to add it to the response.
    def jwt = request.cookies.get(jwtCookieConfiguration.cookieName)

    if (jwt) {
      def auth0 = jwtValidator.validate(jwt.value)
      if (!auth0) {
        // The JWT is no longer valid.
        jwt = null
      }

    }

    if (jwt == null) {
      def jwtRefreshToken = request.cookies.get(RefreshTokenService.JWT_REFRESH_TOKEN)
      def refreshTokenValue = jwtRefreshToken?.value
      // Work around for test mode issue with cookies.  Not needed for production.
      refreshTokenValue = refreshTokenValue ?: getCookie(RefreshTokenService.JWT_REFRESH_TOKEN, request)

      if (refreshTokenValue) {
        log.debug('fetchAuthentication(): refreshTokenValue {}', refreshTokenValue)
        def replacementTokenResponse = refreshTokenService.replaceRefreshToken(refreshTokenValue, request)
        if (replacementTokenResponse) {
          request.setAttribute(AUTO_REFRESH_ATTRIBUTE, replacementTokenResponse)
          log.debug('fetchAuthentication(): Forced Auth on {} set request attribute {} to {}', request, AUTO_REFRESH_ATTRIBUTE, replacementTokenResponse)
          // Now, since we just generated a valid JWT, we will consider this request authenticated.
          def authentication = new AuthenticationUserDetailsAdapter(replacementTokenResponse.userDetails,
                                                                    tokenConfiguration.getRolesName(),
                                                                    tokenConfiguration.getNameKey())
          return Flowable.just(authentication)
        }
      }
    }
    return Flowable.empty()
  }

  /**
   * Gets the given cookie.  Works around an issue with request.cookies.get(name) that seems to fail with
   * 2+ cookies in test mode.  Not sure if this is a problem in production.
   * @param name The cookie name.
   * @param request The HTTP request.
   */
  private getCookie(String name, request) {
    def cookieHeaders = request.headers.getAll(HttpHeaders.COOKIE)
    def prefix = "$name="
    def cookie = cookieHeaders?.find { it.startsWith(prefix) } as String
    if (cookie) {
      return cookie[prefix.size()..-1]
    }
    return null

  }
}