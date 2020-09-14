/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security.domain

import org.simplemes.eframe.misc.FieldSizes
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester

/**
 * Tests.
 */
class RefreshTokenSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [RefreshToken]

  def "verify that user domain enforces constraints"() {
    expect: 'the constraints are enforced'
    DomainTester.test {
      domain RefreshToken
      requiredValues userName: 'ADAM', refreshToken: 'dddd', expirationDate: new Date()
      maxSize 'userName', FieldSizes.MAX_CODE_LENGTH
      notNullCheck 'userName'
      notNullCheck 'refreshToken'
      notNullCheck 'expirationDate'
    }
  }
}
