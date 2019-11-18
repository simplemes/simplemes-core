package org.simplemes.eframe.security

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

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/
/**
 * Defines the basic DB-based user authentication.
 */
@Singleton
class DBAuthenticationProvider implements AuthenticationProvider {
  /**
   * Authenticates a specific user.
   * @param authenticationRequest
   * @return
   */
  @Override
  Publisher<AuthenticationResponse> authenticate(AuthenticationRequest authenticationRequest) {
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