/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

import javax.inject.Singleton

/**
 * Encodes (encrypts) a password.  This is suitable for storage in a database since it is a one-way
 * encryption.
 */
@Singleton
class PasswordEncoderService {
  org.springframework.security.crypto.password.PasswordEncoder delegate = new BCryptPasswordEncoder()

  String encode(String rawPassword) {
    return delegate.encode(rawPassword)
  }

  boolean matches(String rawPassword, String encodedPassword) {
    return delegate.matches(rawPassword, encodedPassword)
  }
}