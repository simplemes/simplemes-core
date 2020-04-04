/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security.controller

import org.simplemes.eframe.exception.ValidationException
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.ControllerTester
import org.simplemes.eframe.test.UnitTestUtils

/**
 * Tests.
 */
class UserControllerSpec extends BaseSpecification {

  def "verify that the controller passes the standard controller test - security, etc"() {
    expect: 'the controller passes'
    ControllerTester.test {
      controller UserController
      taskMenu name: 'user', uri: '/user', clientRootActivity: true, folder: 'admin:7000', displayOrder: 7050
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
    def user = new User(userName: 'ABC')

    when: 'the bind event method is called'
    new UserController().bindEvent(user, [_pwNew: 'abc', _pwConfirm: 'wrong'])

    then: 'the right exception is thrown'
    def ex = thrown(ValidationException)
    //error.207.message=Password match fail on {1}
    UnitTestUtils.assertContainsError(ex.errors, 207, 'password', ['ABC'])
  }
}
