/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security

import org.simplemes.eframe.security.domain.Role
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.security.page.UserCreatePage
import org.simplemes.eframe.security.page.UserEditPage
import org.simplemes.eframe.security.page.UserShowPage
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.CRUDGUITester
import org.simplemes.eframe.test.UnitTestUtils
import spock.lang.IgnoreIf

/**
 * Tests the basic User Definition pages in a GUI/GEB test.
 */
@IgnoreIf({ !sys['geb.env'] })
class UserGUISpec extends BaseGUISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [User]

  def "verify that the standard GUI definition pages work"() {
    given: 'some roles loaded by the data loader'
    waitForInitialDataLoad()
    def roles = Role.list()

    expect: 'the constraints are enforced'
    CRUDGUITester.test {
      tester this
      domain User
      recordParams userName: 'ABC', displayName: 'abc', password: 'XYZ', userRoles: "${roles[1].uuid}"
      minimalParams userName: 'XYZ', password: 'none'
      // Need to set the password and confirm field to change it.  We do not verify it is actually saved here.
      createClosure {
        getInputField("_pwNew").value('XYZ')
        getInputField("_pwConfirm").value('XYZ')
      }
      editClosure {
        getInputField("_pwNew").value('XYZ')
        getInputField("_pwConfirm").value('XYZ')
      }
    }
  }

  def "verify that create will fail if the passwords do not match"() {
    when: 'the create page is displayed'
    login()
    to UserCreatePage

    and: 'fields are filled in'
    userName.input.value('ABC')
    _pwNew.input.value('ABC1')
    _pwConfirm.input.value('ABC2')

    and: 'the save is attempted'
    createButton.click()
    waitFor {
      // Some sort of error message
      messages.text()
    }

    then: 'the message is correct'
    //error.207.message=Password match fail on {1}
    UnitTestUtils.assertContainsAllIgnoreCase(messages.text(), ['password', 'match', 'ABC'])
  }

  def "verify that edit will fail if the passwords do not match"() {
    given: 'a record to edit'
    def user = null
    User.withTransaction {
      user = new User(userName: 'ABC', password: 'xyz').save()
    }

    when: 'the create page is displayed'
    login()
    to UserEditPage, user

    and: 'fields are filled in'
    _pwNew.input.value('ABC1')
    _pwConfirm.input.value('ABC2')

    and: 'the save is attempted'
    updateButton.click()
    waitFor {
      // Some sort of error message
      messages.text()
    }

    then: 'the message is correct'
    //error.207.message=Password match fail on {1}
    UnitTestUtils.assertContainsAllIgnoreCase(messages.text(), ['password', 'match', 'ABC'])
  }

  def "verify that edit can update the password"() {
    given: 'a record to edit'
    def user = null
    User.withTransaction {
      user = new User(userName: 'ABC', password: 'xyz').save()
    }

    when: 'the create page is displayed'
    login()
    to UserEditPage, user

    and: 'fields are filled in'
    _pwNew.input.value('ABC1')
    _pwConfirm.input.value('ABC1')

    and: 'the record saved'
    updateButton.click()
    at UserShowPage

    then: 'the database is correct'
    waitForRecordChange(user)
    def user2 = User.findByUserName('ABC')
    user2.passwordMatches('ABC1')
  }

  def "verify that the show page works"() {
    given: 'a record to edit'
    def user = null
    User.withTransaction {
      user = new User(userName: 'ABC', password: 'xyz').save()
    }

    when: 'the show page is displayed'
    login()
    to UserShowPage, user

    then: 'fields are shown'
    userName.value == user.userName
  }

}
