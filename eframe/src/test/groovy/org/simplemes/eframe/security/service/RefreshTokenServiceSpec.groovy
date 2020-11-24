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
import org.simplemes.eframe.application.EFrameConfiguration
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.security.domain.RefreshToken
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback

import java.time.Duration

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
   * @param useAttemptCount The vallue for the attempt use field. Default: 0.
   * @return The encoded token string.
   */
  String createToken(String userName, Integer useAttemptCount = 0) {
    String uuidString = UUID.randomUUID().toString()
    def userDetails = new UserDetails(userName, [])
    def token = refreshTokenGenerator.generate(userDetails, uuidString).get()
    service.persistToken(new RefreshTokenGeneratedEvent(userDetails, uuidString))

    if (useAttemptCount) {
      RefreshToken.withTransaction {
        def tokenRecord = RefreshToken.findByRefreshToken(uuidString)
        tokenRecord.useAttemptCount = useAttemptCount
        tokenRecord.save()
      }
    }

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
    def maxAge = service.refreshTokenCookieConfiguration.getCookieMaxAge().orElseGet(() -> Duration.ofDays(30)) as Duration
    UnitTestUtils.compareDates(refreshToken.expirationDate, new Date(new Date().time + maxAge.toMillis()))
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
    given: 'a mock appender to reduce console clutter'
    MockAppender.mock(RefreshTokenService, Level.ERROR)

    when: 'the token is replaced'
    def replacementToken = service.replaceRefreshToken('gibberish', mockRequest(), false)

    then: 'no token is generated'
    !replacementToken
  }

  @Rollback
  def "verify that replaceRefreshToken works for first use that needs replacement"() {
    given: 'a refresh token is saved'
    def token = createToken('admin', new EFrameConfiguration().security.jwtRefreshUseMax - 1)

    when: 'the token is replaced'
    def replacementTokenResponse = service.replaceRefreshToken(token, mockRequest(), false)
    def replacementToken = replacementTokenResponse.refreshToken

    then: 'the token is new'
    replacementToken
    replacementToken != token

    and: 'the original token is no longer valid'
    def originalRefreshToken = RefreshToken.findByRefreshToken(getUUID(token))
    !originalRefreshToken.enabled
    originalRefreshToken.useAttemptCount == new EFrameConfiguration().security.jwtRefreshUseMax

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
  def "verify that replaceRefreshToken with forceReplace works even if some attempts are left"() {
    given: 'a refresh token is saved'
    def token = createToken('admin')

    when: 'the token is replaced'
    def replacementTokenResponse = service.replaceRefreshToken(token, mockRequest(), true)
    def replacementToken = replacementTokenResponse.refreshToken

    then: 'the token is new'
    replacementToken
    replacementToken != token
  }

  @Rollback
  def "verify that replaceRefreshToken works multiple times including the final use"() {
    given: 'a refresh token is saved with 3 re-uses left on it'
    def token = createToken('admin', new EFrameConfiguration().security.jwtRefreshUseMax - 3)

    when: 'the token is replaced'
    def replacementTokenResponse = null
    for (i in 1..3) {
      replacementTokenResponse = service.replaceRefreshToken(token, mockRequest(), false)
    }
    def replacementToken = replacementTokenResponse.refreshToken

    then: 'the token is new'
    replacementToken
    replacementToken != token

    and: 'the original token is no longer valid'
    def originalRefreshToken = RefreshToken.findByRefreshToken(getUUID(token))
    !originalRefreshToken.enabled
    originalRefreshToken.useAttemptCount == new EFrameConfiguration().security.jwtRefreshUseMax

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

    and: 'there is just one other token in the DB'
    RefreshToken.list().size() == 2
  }

  @Rollback
  def "verify that replaceRefreshToken re-uses original refresh token when below the jwtRefreshUseMax limit"() {
    given: 'a refresh token is saved'
    def originalUseAttemptCount = new EFrameConfiguration().security.jwtRefreshUseMax - 10
    def token = createToken('admin', originalUseAttemptCount)

    when: 'the refresh token is not replaced'
    def replacementTokenResponse = service.replaceRefreshToken(token, mockRequest(), false)

    then: 'the JWT is defined to be replaced'
    replacementTokenResponse.userDetails

    and: 'the refresh token is not replaced'
    !replacementTokenResponse.refreshToken

    and: 'no new tokens are generated'
    RefreshToken.list().size() == 1
  }

  @Rollback
  def "verify that replaceRefreshToken detects disabled token"() {
    given: 'a refresh token is saved as disabled'
    def token = createToken('admin')
    service.revokeAllUserTokens('admin')

    and: 'a mock appender to reduce console clutter'
    MockAppender.mock(RefreshTokenService, Level.ERROR)

    when: 'the token is replaced'
    def replacementToken = service.replaceRefreshToken(token, mockRequest(), false)

    then: 'no token is created'
    !replacementToken
  }

  @Rollback
  def "verify that replaceRefreshToken detects attempt use token too many times and disables all users tokens"() {
    given: 'multiple refresh tokens for user are saved'
    def address = new InetSocketAddress(837)
    def attemptCount = new EFrameConfiguration().security.jwtRefreshUseMax + 2
    def token1 = createToken('admin', attemptCount)
    createToken('admin')
    createToken('admin')

    and: 'a mock appender for Error level only'
    def mockAppender = MockAppender.mock(RefreshTokenService, Level.ERROR)

    when: 'the token is refreshed and replaced again'
    def replacementToken = service.replaceRefreshToken(token1, mockRequest(remoteAddress: address, uri: '/thePath'), false)

    then: 'the second attempt failed'
    !replacementToken

    and: 'all other tokens are revoked'
    def list = RefreshToken.findAllByUserName('admin')
    list.size() >= 3
    for (token in list) {
      assert !token.enabled
    }

    and: 'the log message is written'
    mockAppender.assertMessageIsValid(['ERROR', 'all', 'tokens', 'revoked', "${attemptCount}",
                                       'admin', address?.address?.toString(), '/thePath'])
  }

  def "verify that replaceRefreshToken allows re-use with IP mis-match"() {
    given: 'a refresh token for user is saved'
    def token1 = createToken('admin')

    and: 'the request source is set correctly'
    RefreshToken.withTransaction {
      def record = RefreshToken.findByRefreshToken(getUUID(token1))
      record.requestSource = originalAddress
      record.save()
    }

    when: 'the token is refreshed and replaced from a different address'
    def address = new InetSocketAddress(InetAddress.localHost, 437)
    def replacementToken = service.replaceRefreshToken(token1, mockRequest(remoteAddress: address, uri: '/thePath'), false)

    then: 'the attempt worked'
    replacementToken

    where:
    originalAddress | _
    '9.9.9.9'       | _
    null            | _
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
    def replacementToken = service.replaceRefreshToken(token, mockRequest(), false)

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
    def replacementToken = service.replaceRefreshToken(token, mockRequest(), false)

    then: 'the refresh fails'
    !replacementToken
  }

  @Rollback
  def "verify that replaceRefreshToken does not work for deleted user"() {
    given: 'a refresh token is saved'
    def token = createToken('ABC')

    when: 'the token is replaced'
    def replacementToken = service.replaceRefreshToken(token, mockRequest(), false)

    then: 'the refresh fails'
    !replacementToken
  }

  @Rollback
  def "verify that replaceRefreshToken cleans up older tokens for the user"() {
    given: 'multiple refresh tokens for user are saved'
    def currentToken = createToken('admin', new EFrameConfiguration().security.jwtRefreshUseMax - 1)
    def expiredTokenStillEnabled = createToken('admin')
    def expiredTokenUsedOnce = createToken('admin')
    def expiredTokenUsedOnceOld = createToken('admin')
    def expiredTokenUsedMultipleTimes = createToken('admin')
    def maxAge = service.refreshTokenCookieConfiguration.getCookieMaxAge().orElseGet(() -> Duration.ofDays(30)) as Duration


    and: 'one token is expired but still enabled'
    def record
    record = RefreshToken.findByRefreshToken(getUUID(expiredTokenStillEnabled))
    record.expirationDate = new Date(System.currentTimeMillis() - 100 * DateUtils.MILLIS_PER_DAY - maxAge.toMillis())
    record.save()

    and: 'one token is expired and used once - recently'
    record = RefreshToken.findByRefreshToken(getUUID(expiredTokenUsedOnce))
    record.expirationDate = new Date(System.currentTimeMillis() - 3600000L)
    record.enabled = false
    record.useAttemptCount = 1
    record.save()

    and: 'one token is expired and used once - old'
    record = RefreshToken.findByRefreshToken(getUUID(expiredTokenUsedOnceOld))
    record.expirationDate = new Date(System.currentTimeMillis() - 100 * DateUtils.MILLIS_PER_DAY - maxAge.toMillis())
    record.enabled = false
    record.useAttemptCount = 1
    record.save()

    and: 'one token is expired and used multiple times'
    record = RefreshToken.findByRefreshToken(getUUID(expiredTokenUsedMultipleTimes))
    record.expirationDate = new Date(System.currentTimeMillis() - 100 * DateUtils.MILLIS_PER_DAY - maxAge.toMillis())
    record.enabled = false
    record.useAttemptCount = 137
    record.save()

    when: 'the token is refreshed and replaced again'
    def replacementTokenResponse = service.replaceRefreshToken(currentToken, mockRequest(), false)
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

  def "verify that getRequestSource builds a source with the correct values"() {
    when: 'the token is refreshed and replaced from a different address'
    def address = new InetSocketAddress(originalAddress, 437)
    def source = service.getRequestSource(mockRequest(remoteAddress: address, headers: headers))
    println "source = $source"

    then: 'source contains the expected values'
    UnitTestUtils.assertContainsAllIgnoreCase(source, results)

    where:
    originalAddress | headers                    | results
    '9.9.9.9'       | [:]                        | ['9.9.9.9']
    '9.9.9.9'       | ['X-Request-ID': 'xyzzy']  | ['9.9.9.9', 'xyzzy']
    '9.9.9.9'       | ['X-Forwarded-For': 'pdq'] | ['9.9.9.9', 'pdq']


  }

}
