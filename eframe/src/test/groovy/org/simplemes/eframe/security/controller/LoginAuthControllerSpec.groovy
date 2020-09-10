/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security.controller

import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpStatus
import io.micronaut.security.authentication.UserDetails
import io.micronaut.security.errors.OauthErrorResponseException
import io.micronaut.security.token.event.RefreshTokenGeneratedEvent
import io.micronaut.security.token.jwt.validator.JwtTokenValidator
import io.micronaut.security.token.validator.RefreshTokenValidator
import io.reactivex.Single
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseSpecification

/**
 * Tests.
 */
class LoginAuthControllerSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [SERVER]

  LoginAuthController controller

  def setup() {
    controller = Holders.getBean(LoginAuthController)
  }

  def "verify that accessToken works for the basic refresh case"() {
    given: 'a refresh token'
    //def key = controller.refreshTokenGenerator.createKey(new UserDetails('joe',[]))
    def uuid = UUID.randomUUID().toString()
    def userDetails = new UserDetails('joe', [])
    def token = controller.refreshTokenGenerator.generate(userDetails, uuid).get()
    controller.refreshTokenPersistence.persistToken(new RefreshTokenGeneratedEvent(userDetails, uuid))

    when: 'the refresh is sent'
    def res = controller.accessToken(mockRequest(), token).toFuture().get()

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
    auth.name == 'joe'

    def refreshCookie = cookies.find { it.startsWith('JWT_REFRESH_TOKEN=') }
    def refreshTokens = refreshCookie.tokenize("=;")
    def refreshValidator = Holders.getBean(RefreshTokenValidator)
    refreshValidator.validate(refreshTokens[1]).get() == uuid

    // TODO: Test that it forces a new UUID - Single Use token.
  }

  def "verify that accessToken returns no token if there is no refresh token input"() {
    when: 'the refresh is sent'
    controller.accessToken(mockRequest(), null).toFuture().get()

    then: 'the exception is thrown'
    thrown(OauthErrorResponseException)
  }

  def "verify that accessToken fails with invalid token"() {
    given: 'a refresh token that is not registered'
    def uuid = UUID.randomUUID().toString()
    def userDetails = new UserDetails('joe', [])
    def token = controller.refreshTokenGenerator.generate(userDetails, uuid).get()

    when: 'the refresh is sent'
    controller.accessToken(mockRequest(), token).toFuture().get()

    then: 'the exception is thrown'
    def ex = thrown(Exception)
    ex.cause instanceof OauthErrorResponseException
  }

  // revoked refresh token
}
