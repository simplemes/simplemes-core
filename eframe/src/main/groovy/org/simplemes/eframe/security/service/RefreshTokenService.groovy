/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security.service

import groovy.util.logging.Slf4j
import io.micronaut.core.async.publisher.Publishers
import io.micronaut.http.HttpRequest
import io.micronaut.security.authentication.UserDetails
import io.micronaut.security.errors.IssuingAnAccessTokenErrorCode
import io.micronaut.security.errors.OauthErrorResponseException
import io.micronaut.security.token.event.RefreshTokenGeneratedEvent
import io.micronaut.security.token.generator.RefreshTokenGenerator
import io.micronaut.security.token.refresh.RefreshTokenPersistence
import io.micronaut.security.token.validator.RefreshTokenValidator
import io.reactivex.Flowable
import org.reactivestreams.Publisher
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.security.ReplacementTokenResponse
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.security.domain.RefreshToken
import org.simplemes.eframe.security.domain.User

import javax.inject.Singleton
import javax.transaction.Transactional

/**
 * Provides services for the JWT refresh tokens.  Implements the refresh token persistence and realted methods.
 */
@Slf4j
@Singleton
class RefreshTokenService implements RefreshTokenPersistence {

  /**
   * Tokens that expired more than this many days ago are eligible for cleanup.  Tokens used more than once
   * are not cleaned up this way.
   */
  public static final Long CLEANUP_AGE_DAYS = 1

  /**
   * The name of the cookie for the refresh token.
   */
  public static final String JWT_REFRESH_TOKEN = "JWT_REFRESH_TOKEN"

  /**
   * The name of the cookie for the silent refresh data.
   */
  public static final String JWT_SILENT_REFRESH = "JWT_SILENT_REFRESH"

  protected final RefreshTokenGenerator refreshTokenGenerator
  protected final RefreshTokenValidator refreshTokenValidator

  /**
   * @param refreshTokenGenerator Refresh Token Generator
   */
  RefreshTokenService(RefreshTokenGenerator refreshTokenGenerator, RefreshTokenValidator refreshTokenValidator) {
    this.refreshTokenGenerator = refreshTokenGenerator
    this.refreshTokenValidator = refreshTokenValidator
  }


  /**
   * Persists the refresh token.
   * @param event
   */
  @Override
  @Transactional
  void persistToken(RefreshTokenGeneratedEvent event) {
    def refreshToken = new RefreshToken()
    refreshToken.refreshToken = event.getRefreshToken()
    refreshToken.userName = event.getUserDetails().username
    refreshToken.enabled = true
    refreshToken.expirationDate = new Date(System.currentTimeMillis() + Holders.configuration.security.jwtRefreshMaxAge * 1000)

    refreshToken.save()
  }

  /**
   * Gets the refresh token's user details.
   * <p>
   *   <b>Note:</b> This intentionally never finds the refresh details to prevent the standard OAuth logic
   *         from working.
   * @param refreshTokenString
   * @return
   */
  @Override
  @Transactional
  Publisher<UserDetails> getUserDetails(String refreshTokenString) {
    Flowable.error(new OauthErrorResponseException(IssuingAnAccessTokenErrorCode.INVALID_GRANT, "refresh token not found", null))
  }

  /**
   * Replaces the given refresh token with a new single use token.
   * Will log an ERROR if a token is used 2+ times and revoke all of the user's current tokens.
   * @param currentEncodedToken The current token (encoded).
   * @param request The request.
   * @return The token and user details.  Null if token is not valid or has already been used.
   */
  @Transactional
  ReplacementTokenResponse replaceRefreshToken(String currentEncodedToken, HttpRequest<?> request) {
    def requestSource = request.remoteAddress?.toString() ?: 'no IP'
    def opt = refreshTokenValidator.validate(currentEncodedToken)
    if (!opt.present) {
      log.debug("replaceRefreshToken(): No valid refresh token for user '{}' from '{}'.",
                SecurityUtils.currentUserName, requestSource)
      return null
    }
    def currentToken = opt.get()
    def currentRefreshToken = RefreshToken.findByRefreshToken(currentToken)
    if (currentRefreshToken) {
      if (currentRefreshToken.enabled) {
        currentRefreshToken.useAttemptCount++
        currentRefreshToken.enabled = false
        currentRefreshToken.requestSource = requestSource
        currentRefreshToken.save()
        if (currentRefreshToken.expirationDate < new Date()) {
          log.debug("replaceRefreshToken(): Refresh token {} expired for user '{}' from '{}'.",
                    currentRefreshToken.uuid, currentRefreshToken.userName, requestSource)
          return null
        }

        // Clean up any old records for this user.
        cleanupOldUserTokens(currentRefreshToken.userName)

        def userDetails = getUserDetailsForUserName(currentRefreshToken.userName)
        if (!userDetails) {
          log.debug("replaceRefreshToken(): No user details for user '{}' from '{}'.", currentRefreshToken.userName, requestSource)
          return null
        }
        def newRefreshUUID = refreshTokenGenerator.createKey(userDetails)
        def newRefreshToken = refreshTokenGenerator.generate(userDetails, newRefreshUUID).get()

        // Now, create a new one
        def refreshToken = new RefreshToken()
        refreshToken.refreshToken = newRefreshUUID
        refreshToken.userName = currentRefreshToken.userName
        refreshToken.enabled = true
        refreshToken.expirationDate = currentRefreshToken.expirationDate
        refreshToken.requestSource = requestSource
        refreshToken.save()

        log.debug("replaceRefreshToken(): Issued new refresh token {} for user '{}' from '{}'.", newRefreshUUID, currentRefreshToken.userName, requestSource)
        return new ReplacementTokenResponse(refreshToken: newRefreshToken, userDetails: userDetails)
      } else {
        // Already used, so fail with an error logged
        revokeAllUserTokens(currentRefreshToken.userName)
        currentRefreshToken.useAttemptCount++
        log.error("replaceRefreshToken(): Attempt to use refresh token more than once for user '{}' from '{}'.  All refresh tokens revoked.  Attempt count {}.",
                  currentRefreshToken.userName, requestSource, currentRefreshToken.useAttemptCount)
        currentRefreshToken.save()

        return null
      }
    }
    log.debug("replaceRefreshToken(): No user refresh token {} for user '{}' from '{}'.", currentToken, SecurityUtils.currentUserName, requestSource)
    return null
  }

  /**
   * Revokes all user's tokens.
   * @param userName The user.
   */
  @Transactional
  protected void revokeAllUserTokens(String userName) {
    def list = RefreshToken.findAllByUserName(userName)
    for (token in list) {
      token.enabled = false
      token.save()
    }
  }

  /**
   * Deletes any old, normal tokens.  Preserves all active tokens or tokens that were attempted to use more than once.
   * @param userName The user.
   */
  @Transactional
  protected void cleanupOldUserTokens(String userName) {
    def list = RefreshToken.findAllByUserName(userName)
    for (token in list) {
      if (token.expirationDate.time < (System.currentTimeMillis() - CLEANUP_AGE_DAYS * DateUtils.MILLIS_PER_DAY) &&
        token.useAttemptCount < 2) {
        // Only delete really old expired records that are not likely to be due to security issue.
        token.delete()
      }
    }
  }


  /**
   * Gets the refresh token's user details and creates a replacement token.
   * @param refreshTokenString
   * @return
   */
  @Transactional
  Publisher<UserDetails> getUserDetailsAndNewToken(String refreshTokenString) {
    def refreshToken = RefreshToken.findByRefreshToken(refreshTokenString)
    UserDetails userDetails = null
    if (refreshToken) {
      userDetails = getUserDetailsForUserName(refreshToken.userName)
    }
    if (userDetails) {
      Publishers.just(userDetails)
    } else {
      Flowable.error(new OauthErrorResponseException(IssuingAnAccessTokenErrorCode.INVALID_GRANT, "refresh token not found", null))
    }
  }

  /**
   * Returns the user details for the given user name.
   * @param userName
   * @return The details.  Null if not found or not enabled.
   */
  protected UserDetails getUserDetailsForUserName(String userName) {
    def user = User.findByUserName(userName)
    if (user?.enabled) {
      if (user.accountExpired || user.accountLocked) {
        return null
      }
      def roles = user.userRoles*.authority
      return new UserDetails(userName, roles)
    }
    return null
  }

}
