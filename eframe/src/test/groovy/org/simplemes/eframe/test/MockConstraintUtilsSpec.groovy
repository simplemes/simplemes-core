package org.simplemes.eframe.test

import org.simplemes.eframe.domain.ConstraintUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class MockConstraintUtilsSpec extends BaseSpecification {

  def "verify that gerPersistentFields returns the fields from the fieldOrder list"() {
    given: 'a mocked domainUtils'
    new MockConstraintUtils(this, 43).install()

    expect:
    ConstraintUtils.instance.getPropertyMaxSize(null) == 43
  }
}
