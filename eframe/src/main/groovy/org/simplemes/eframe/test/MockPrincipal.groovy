package org.simplemes.eframe.test


import org.simplemes.eframe.security.SecurityUtils

import java.security.Principal

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A mock of the user Principal for use in unit tests only.  Provides the specified (optional) roles for the user.
 */
class MockPrincipal implements Principal {

  /**
   * The Mock user name.
   */
  String userName

  /**
   * The attributes (simulates the attributes in AuthenticationJWTClaimsSetAdapter).
   */
  Map attributes = [:]

  /**
   * Builds a mock principal for the user 'TEST' ( {@link SecurityUtils#TEST_USER} ).  No roles assigned.
   */
  MockPrincipal() {
    this.userName = SecurityUtils.TEST_USER
  }

  /**
   * Builds a mock principal for the given user (required).  No roles assigned.
   * @param userName The user name.
   */
  MockPrincipal(String userName) {
    this.userName = userName
  }

  /**
   * Builds a mock principal for the given user (required).
   * @param userName The user name.
   * @param roles The roles the mock user has.
   */
  @SuppressWarnings("unused")
  MockPrincipal(String userName, List<String> roles) {
    this.userName = userName
    this.attributes.roles = roles
  }

  /**
   * Builds a mock principal for the given user (required).
   * @param userName The user name.
   * @param role The role the mock user has.
   */
  MockPrincipal(String userName, String role) {
    this.userName = userName
    this.attributes.roles = [role]
  }

  /**
   * Returns the name of this principal.
   *
   * @return the name of this principal.
   */
  @Override
  String getName() {
    return userName
  }

  /**
   * The mock attributes.
   * @return
   */
  Map getAttributes() {
    return attributes
  }
}
