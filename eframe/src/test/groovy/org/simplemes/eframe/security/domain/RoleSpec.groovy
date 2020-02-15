/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security.domain


import org.simplemes.eframe.misc.FieldSizes
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.annotation.Rollback

/**
 * Tests.
 */
class RoleSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [SERVER]

  def "verify that Role domain enforces constraints"() {
    expect: 'the constraints are enforced'
    DomainTester.test {
      domain Role
      requiredValues authority: 'DUMMY', title: 'Admin'
      maxSize 'authority', FieldSizes.MAX_CODE_LENGTH
      maxSize 'title', FieldSizes.MAX_TITLE_LENGTH
      notNullCheck 'authority'
      notNullCheck 'title'
      fieldOrderCheck false
    }
  }

  @Rollback
  def "verify that the load initial data  works"() {
    given: 'all user records are deleted'
    deleteAllRecords(User, false)
    deleteAllRecords(Role, false)

    when: ''
    assert Role.list().size() == 0
    Role.initialDataLoad()

    then: 'the expected records are loaded'
    Role.findByAuthority('ADMIN')
    Role.findByAuthority('CUSTOMIZER')
    Role.findByAuthority('MANAGER')
    Role.findByAuthority('DESIGNER')
  }

}
