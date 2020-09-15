/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security.controller

import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpStatus
import io.micronaut.security.authentication.UserDetails
import io.micronaut.security.errors.OauthErrorResponseException
import io.micronaut.security.token.event.RefreshTokenGeneratedEvent
import io.micronaut.security.token.generator.RefreshTokenGenerator
import io.micronaut.security.token.jwt.validator.JwtTokenValidator
import io.micronaut.security.token.validator.RefreshTokenValidator
import io.reactivex.Single
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.security.service.RefreshTokenService
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.annotation.Rollback

/**
 * Tests.
 */
class LoginAuthControllerSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [SERVER]

  LoginAuthController controller
  RefreshTokenGenerator refreshTokenGenerator

  def setup() {
    controller = Holders.getBean(LoginAuthController)
    refreshTokenGenerator = Holders.getBean(RefreshTokenGenerator)
  }

  /**
   * Creates a token and persists it.
   * @param userName The user to create the token for.
   * @param uuidString The UUID used in the token.  Default: Random UUID.
   * @return The encoded token string.
   */
  String createToken(String userName, String uuidString = UUID.randomUUID().toString()) {
    def userDetails = new UserDetails(userName, [])
    def token = refreshTokenGenerator.generate(userDetails, uuidString).get()
    controller.refreshTokenPersistence.persistToken(new RefreshTokenGeneratedEvent(userDetails, uuidString))

    return token
  }


  def "verify that accessToken works for the basic refresh case"() {
    given: 'a refresh token'
    def refreshValidator = Holders.getBean(RefreshTokenValidator)
    def token = createToken('admin')
    def uuid = refreshValidator.validate(token).get()

    when: 'the refresh is sent'
    def res = controller.accessToken(mockRequest(), token)

    then: 'the response is a simple page for an iframe'
    res.status == HttpStatus.OK

    and: 'the body is valid HTML'
    res.body.get().contains("<html>")

    and: 'the cookies are correct'
    def cookies = res.headers.getAll(HttpHeaders.SET_COOKIE)

    def jwtCookie = cookies.find { it.startsWith('JWT=') }
    def jwtTokens = jwtCookie.tokenize("=;")
    def accessValidator = Holders.getBean(JwtTokenValidator)
    def auth = Single.fromPublisher(accessValidator.validateToken(jwtTokens[1], null))
      .map(auth -> auth).toFlowable().singleElement().blockingGet()
    auth.name == 'admin'

    def refreshCookie = cookies.find { it.startsWith(RefreshTokenService.JWT_REFRESH_TOKEN) }
    def refreshTokens = refreshCookie.tokenize("=;")

    and: 'the new refresh token is different from the original'
    refreshValidator.validate(refreshTokens[1]).get() != uuid
  }

  def "verify that accessToken returns no token if there is no refresh token input"() {
    when: 'the refresh is sent'
    controller.accessToken(mockRequest(), null)

    then: 'the exception is thrown'
    thrown(OauthErrorResponseException)
  }

  def "verify that accessToken fails with invalid token"() {
    given: 'a refresh token that is not registered'
    def uuid = UUID.randomUUID().toString()
    def userDetails = new UserDetails('joe', [])
    def token = refreshTokenGenerator.generate(userDetails, uuid).get()

    when: 'the refresh is sent'
    controller.accessToken(mockRequest(), token)

    then: 'the exception is thrown'
    thrown(OauthErrorResponseException)
  }

  @Rollback
  def "verify that accessToken detects disabled user account"() {
    given: 'a disabled user with a refresh token'
    new User(userName: 'ABC', password: 'ABC', enabled: false).save()
    def token = createToken('ABC')

    when: 'the refresh is sent'
    controller.accessToken(mockRequest(), token)

    then: 'the exception is thrown'
    thrown(OauthErrorResponseException)
  }

  @Rollback
  def "verify that accessToken detects expired user account"() {
    given: 'a disabled user with a refresh token'
    new User(userName: 'ABC', password: 'ABC', accountExpired: true).save()
    def token = createToken('ABC')

    when: 'the refresh is sent'
    controller.accessToken(mockRequest(), token)

    then: 'the exception is thrown'
    thrown(OauthErrorResponseException)
  }

  @Rollback
  def "verify that accessToken detects locked user account"() {
    given: 'a disabled user with a refresh token'
    new User(userName: 'ABC', password: 'ABC', accountLocked: true).save()
    def token = createToken('ABC')

    when: 'the refresh is sent'
    controller.accessToken(mockRequest(), token)

    then: 'the exception is thrown'
    thrown(OauthErrorResponseException)
  }

}
