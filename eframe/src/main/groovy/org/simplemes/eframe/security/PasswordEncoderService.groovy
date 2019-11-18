package org.simplemes.eframe.security

import io.micronaut.security.authentication.providers.PasswordEncoder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

import javax.inject.Singleton

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Encodes (encrypts) a password.  This is suitable for storage in a database since it is a one-way
 * encryption.
 */
@Singleton
class PasswordEncoderService implements PasswordEncoder {
  org.springframework.security.crypto.password.PasswordEncoder delegate = new BCryptPasswordEncoder()

  String encode(String rawPassword) {
    return delegate.encode(rawPassword)
  }

  @Override
  boolean matches(String rawPassword, String encodedPassword) {
    return delegate.matches(rawPassword, encodedPassword)
  }
}