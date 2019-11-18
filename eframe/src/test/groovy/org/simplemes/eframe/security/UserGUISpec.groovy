package org.simplemes.eframe.security


import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.security.page.UserCreatePage
import org.simplemes.eframe.security.page.UserEditPage
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.CRUDGUITester
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests the basic User Definition pages in a GUI/GEB test.
 */
@IgnoreIf({ !sys['geb.env'] })
class UserGUISpec extends BaseGUISpecification {

  static dirtyDomains = [User]

  def "verify that the standard GUI definition pages work"() {
    expect: 'the constraints are enforced'
    CRUDGUITester.test {
      tester this
      domain User
      recordParams userName: 'ABC', title: 'abc', password: 'XYZ'
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
    //passwordMatchFail=Password match fail on {0}
    messages.text() == lookup('passwordMatchFail', null, lookup('password.label'))

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
    //passwordMatchFail=Password match fail on {0}
    messages.text() == lookup('passwordMatchFail', null, lookup('password.label'))
  }

}
