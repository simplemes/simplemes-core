/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security

import edu.umd.cs.findbugs.annotations.Nullable
import io.micronaut.http.HttpRequest
import io.micronaut.security.authentication.AuthenticationFailed
import io.micronaut.security.authentication.AuthenticationFailureReason
import io.micronaut.security.authentication.AuthenticationProvider
import io.micronaut.security.authentication.AuthenticationRequest
import io.micronaut.security.authentication.AuthenticationResponse
import io.micronaut.security.authentication.UserDetails
import io.reactivex.Flowable
import org.reactivestreams.Publisher
import org.simplemes.eframe.security.domain.User

import javax.inject.Singleton

/**
 * Defines the basic DB-based user authentication.
 */
@Singleton
class DBAuthenticationProvider implements AuthenticationProvider {
  /**
   * Authenticates a user with the given request. If a successful authentication is
   * returned, the object must be an instance of {@link UserDetails}.
   *
   * Publishers <b>MUST emit cold observables</b>! This method will be called for
   * all authenticators for each authentication request and it is assumed no work
   * will be done until the publisher is subscribed to.
   *
   * @param httpRequest The http request
   * @param authenticationRequest The credentials to authenticate
   * @return A publisher that emits 0 or 1 responses
   */
  @Override
  Publisher<AuthenticationResponse> authenticate(@Nullable HttpRequest<?> httpRequest, AuthenticationRequest<?, ?> authenticationRequest) {
    def user = null
    def roles = null
    User.withTransaction {
      user = User.findByUserNameAndEnabled(authenticationRequest.getIdentity() as String, true)
      roles = user?.userRoles*.authority
    }

    if (user?.passwordMatches(authenticationRequest.getSecret() as String)) {
      if (user.accountExpired) {
        return Flowable.just(new AuthenticationFailed(AuthenticationFailureReason.ACCOUNT_EXPIRED))
      }
      if (user.accountLocked) {
        return Flowable.just(new AuthenticationFailed(AuthenticationFailureReason.ACCOUNT_LOCKED))
      }
      if (user.passwordExpired) {
        return Flowable.just(new AuthenticationFailed(AuthenticationFailureReason.PASSWORD_EXPIRED))
      }
      UserDetails userDetails = new UserDetails((String) authenticationRequest.getIdentity(), roles)
      return Flowable.just(userDetails)
    }
    return Flowable.just(new AuthenticationFailed())
  }
}