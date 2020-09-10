/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security

import io.micronaut.core.async.publisher.Publishers
import io.micronaut.security.authentication.UserDetails
import io.micronaut.security.errors.IssuingAnAccessTokenErrorCode
import io.micronaut.security.errors.OauthErrorResponseException
import io.micronaut.security.token.event.RefreshTokenGeneratedEvent
import io.micronaut.security.token.refresh.RefreshTokenPersistence
import io.reactivex.Flowable
import org.reactivestreams.Publisher

import javax.inject.Singleton


/**
 *
 */
@Singleton
class UserRefreshTokenPersistence implements RefreshTokenPersistence {


  Map<String, UserDetails> tokens = [:]

  @Override
  void persistToken(RefreshTokenGeneratedEvent event) {
    tokens.put(event.getRefreshToken(), event.getUserDetails())
  }

  @Override
  Publisher<UserDetails> getUserDetails(String refreshToken) {
    UserDetails userDetails = tokens.get(refreshToken)
    if (userDetails) {
      Publishers.just(userDetails)
    } else {
      Flowable.error(new OauthErrorResponseException(IssuingAnAccessTokenErrorCode.INVALID_GRANT, "refresh token not found", null))
    }
  }
}
