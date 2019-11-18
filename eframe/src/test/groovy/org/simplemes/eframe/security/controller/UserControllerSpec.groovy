package org.simplemes.eframe.security.controller

import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.ControllerTester
import org.simplemes.eframe.test.UnitTestUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class UserControllerSpec extends BaseSpecification {

  def "verify that the controller passes the standard controller test - security, etc"() {
    expect: 'the controller passes'
    ControllerTester.test {
      controller UserController
      taskMenu name: 'user', uri: '/user', clientRootActivity: true
    }
  }

  def "verify that the bindEvent handles password change checks - passes"() {
    def user = new User()

    when: 'the bind event method is called'
    new UserController().bindEvent(user, [_pwNew: 'abc', _pwConfirm: 'abc'])

    then: 'the password is set'
    user.password == 'abc'
  }

  def "verify that the bindEvent handles password change checks - fails check"() {
    def user = new User()

    when: 'the bind event method is called'
    new UserController().bindEvent(user, [_pwNew: 'abc', _pwConfirm: 'wrong'])

    then: 'the validation error is set'
    def errors = GlobalUtils.lookupValidationErrors(user)
    UnitTestUtils.assertContainsAllIgnoreCase(errors.password, ['match'])

    and: 'neither password is in the validation message'
    !errors.password.contains('abc')
    !errors.password.contains('wrong')
  }
}
