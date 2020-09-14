/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security

import io.micronaut.security.authentication.UserDetails

/**
 * Defines the replacement refresh token and user details for a replacement request.
 */
class ReplacementTokenResponse {

  /**
   * The token.
   */
  String refreshToken

  /**
   * The user details.
   */
  UserDetails userDetails
}
