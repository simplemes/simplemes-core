/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security.service

import ch.qos.logback.classic.Level
import io.micronaut.security.authentication.UserDetails
import io.micronaut.security.errors.OauthErrorResponseException
import io.micronaut.security.token.event.RefreshTokenGeneratedEvent
import io.micronaut.security.token.generator.RefreshTokenGenerator
import io.micronaut.security.token.validator.RefreshTokenValidator
import io.reactivex.Single
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.security.domain.RefreshToken
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.annotation.Rollback

/**
 * Tests.
 */
class RefreshTokenServiceSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [SERVER]

  RefreshTokenService service
  RefreshTokenGenerator refreshTokenGenerator
  RefreshTokenValidator refreshTokenValidator

  def setup() {
    service = Holders.getBean(RefreshTokenService)
    refreshTokenGenerator = Holders.getBean(RefreshTokenGenerator)
    refreshTokenValidator = Holders.getBean(RefreshTokenValidator)
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
    service.persistToken(new RefreshTokenGeneratedEvent(userDetails, uuidString))

    return token
  }

  /**
   * Returns the internal UUID for the given refresh token.
   * @param token
   * @return
   */
  String getUUID(String token) {
    return refreshTokenValidator.validate(token).get()
  }


  def "verify that persistToken saves the record"() {
    given: 'a refresh token'
    def userDetails = new UserDetails('joe', [])
    def uuid = UUID.randomUUID().toString()
    refreshTokenGenerator.generate(userDetails, uuid).get()

    when: 'the token is persisted'
    service.persistToken(new RefreshTokenGeneratedEvent(userDetails, uuid))

    then: 'the token is in the DB'
    RefreshToken.list().size() == 1
    def refreshToken = RefreshToken.findByRefreshToken(uuid)
    refreshToken.refreshToken == uuid
    refreshToken.userName == 'joe'
    refreshToken.enabled
    refreshToken.expirationDate.time > (System.currentTimeMillis() + (Holders.configuration.security.jwtRefreshMaxAge * 1000) - 1000)
  }

  def "verify that getUserDetails always fails"() {
    // Prevents std /oauth/access_refresh endpoint from refreshing using the token.
    given: 'a refresh token is saved'
    def token = createToken('admin')

    when: 'the token is persisted'
    Single.fromPublisher(service.getUserDetails(token))
      .map(userDetailsArg -> userDetailsArg).toFlowable().singleElement().blockingGet()

    then: 'an exception is expected'
    def ex = thrown(Exception)
    ex instanceof OauthErrorResponseException
  }

  def "verify that replaceRefreshToken fails when original token does not exist"() {
    when: 'the token is replaced'
    def replacementToken = service.replaceRefreshToken('gibberish', mockRequest())

    then: 'no token is generated'
    !replacementToken
  }

  @Rollback
  def "verify that replaceRefreshToken retrieves the record"() {
    given: 'a refresh token is saved'
    def token = createToken('admin')

    when: 'the token is replaced'
    def replacementTokenResponse = service.replaceRefreshToken(token, mockRequest())
    def replacementToken = replacementTokenResponse.refreshToken

    then: 'the token is new'
    replacementToken != token

    and: 'the original token is no longer valid'
    def originalRefreshToken = RefreshToken.findByRefreshToken(getUUID(token))
    !originalRefreshToken.enabled
    originalRefreshToken.useAttemptCount == 1

    and: 'the new token is valid'
    def newRefreshToken = RefreshToken.findByRefreshToken(getUUID(replacementToken))
    newRefreshToken.enabled
    newRefreshToken.useAttemptCount == 0
    newRefreshToken.userName == 'admin'

    and: 'the expiration date is used from the original'
    newRefreshToken.expirationDate == originalRefreshToken.expirationDate

    and: 'the user details are correct'
    replacementTokenResponse.userDetails.username == 'admin'
    replacementTokenResponse.userDetails.roles.contains('ADMIN')
    replacementTokenResponse.userDetails.roles.contains('MANAGER')
  }

  @Rollback
  def "verify that replaceRefreshToken detects disabled token"() {
    given: 'a refresh token is saved as disabled'
    def token = createToken('admin')
    service.revokeAllUserTokens('admin')

    when: 'the token is replaced'
    def replacementToken = service.replaceRefreshToken(token, mockRequest())

    then: 'no token is created'
    !replacementToken
  }

  @Rollback
  def "verify that replaceRefreshToken detects attempt use token twice and disables all users tokens"() {
    given: 'multiple refresh tokens for user are saved'
    def address = new InetSocketAddress(837)
    def token1 = createToken('admin')
    createToken('admin')
    createToken('admin')

    and: 'a mock appender for Error level only'
    def mockAppender = MockAppender.mock(RefreshTokenService, Level.ERROR)

    and: 'the token is already used'
    service.replaceRefreshToken(token1, mockRequest(remoteAddress: address))

    and: 'the attempt count is set'
    def record = RefreshToken.findByRefreshToken(getUUID(token1))
    record.useAttemptCount = 236
    record.save()

    when: 'the token is refreshed and replaced again'
    def replacementToken = service.replaceRefreshToken(token1, mockRequest(remoteAddress: address))

    then: 'the second attempt failed'
    !replacementToken

    and: 'all other tokens are revoked'
    def list = RefreshToken.findAllByUserName('admin')
    list.size() >= 3
    for (token in list) {
      assert !token.enabled
    }

    and: 'the log message is written'
    mockAppender.assertMessageIsValid(['ERROR', 'all', 'tokens', 'revoked', '237', 'admin', address.toString()])
  }

  @Rollback
  def "verify that replaceRefreshToken does not use expired token"() {
    given: 'a refresh token is saved'
    def token = createToken('admin')

    and: 'the date is set to expired'
    def record = RefreshToken.findByRefreshToken(getUUID(token))
    record.expirationDate = new Date(System.currentTimeMillis() - 20000)
    record.save()

    when: 'the token is replaced'
    def replacementToken = service.replaceRefreshToken(token, mockRequest())

    then: 'the refresh fails'
    !replacementToken
  }

  @Rollback
  def "verify that replaceRefreshToken does not work for disabled user"() {
    given: 'a disabled user'
    new User(userName: 'ABC', password: 'XYZ', enabled: false).save()

    and: 'a refresh token is saved'
    def token = createToken('ABC')

    when: 'the token is replaced'
    def replacementToken = service.replaceRefreshToken(token, mockRequest())

    then: 'the refresh fails'
    !replacementToken
  }

  @Rollback
  def "verify that replaceRefreshToken does not work for deleted user"() {
    given: 'a refresh token is saved'
    def token = createToken('ABC')

    when: 'the token is replaced'
    def replacementToken = service.replaceRefreshToken(token, mockRequest())

    then: 'the refresh fails'
    !replacementToken
  }

  @Rollback
  def "verify that replaceRefreshToken cleans up older tokens for the user"() {
    given: 'multiple refresh tokens for user are saved'
    def currentToken = createToken('admin')
    def expiredTokenStillEnabled = createToken('admin')
    def expiredTokenUsedOnce = createToken('admin')
    def expiredTokenUsedOnceOld = createToken('admin')
    def expiredTokenUsedMultipleTimes = createToken('admin')

    and: 'one token is expired but still enabled'
    def record
    record = RefreshToken.findByRefreshToken(getUUID(expiredTokenStillEnabled))
    record.expirationDate = new Date(System.currentTimeMillis() - 100 * DateUtils.MILLIS_PER_DAY - Holders.configuration.security.jwtRefreshMaxAge * 1000)
    record.save()

    and: 'one token is expired and used once - recently'
    record = RefreshToken.findByRefreshToken(getUUID(expiredTokenUsedOnce))
    record.expirationDate = new Date(System.currentTimeMillis() - 3600000L)
    record.enabled = false
    record.useAttemptCount = 1
    record.save()

    and: 'one token is expired and used once - old'
    record = RefreshToken.findByRefreshToken(getUUID(expiredTokenUsedOnceOld))
    record.expirationDate = new Date(System.currentTimeMillis() - 100 * DateUtils.MILLIS_PER_DAY - Holders.configuration.security.jwtRefreshMaxAge * 1000)
    record.enabled = false
    record.useAttemptCount = 1
    record.save()

    and: 'one token is expired and used multiple times'
    record = RefreshToken.findByRefreshToken(getUUID(expiredTokenUsedMultipleTimes))
    record.expirationDate = new Date(System.currentTimeMillis() - 100 * DateUtils.MILLIS_PER_DAY - Holders.configuration.security.jwtRefreshMaxAge * 1000)
    record.enabled = false
    record.useAttemptCount = 137
    record.save()

    when: 'the token is refreshed and replaced again'
    def replacementTokenResponse = service.replaceRefreshToken(currentToken, mockRequest())
    def replacementToken = replacementTokenResponse.refreshToken

    then: 'the right tokens are delete as old and uninteresting'
    RefreshToken.findAllByUserName('admin').size() == 4
    !RefreshToken.findByRefreshToken(getUUID(expiredTokenStillEnabled))
    !RefreshToken.findByRefreshToken(getUUID(expiredTokenUsedOnceOld))

    and: 'the current or tokens with security are still in the DB'
    RefreshToken.findByRefreshToken(getUUID(replacementToken))
    RefreshToken.findByRefreshToken(getUUID(currentToken))
    RefreshToken.findByRefreshToken(getUUID(expiredTokenUsedOnce))
    RefreshToken.findByRefreshToken(getUUID(expiredTokenUsedMultipleTimes))
  }

}
