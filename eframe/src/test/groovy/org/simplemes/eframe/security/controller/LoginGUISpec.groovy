/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security.controller


import org.simplemes.eframe.test.BaseGUISpecification
import spock.lang.IgnoreIf

/**
 * Tests.
 */
@IgnoreIf({ !sys['geb.env'] })
class LoginGUISpec extends BaseGUISpecification {

  def "verify that accessing a GUI before logging called redirects to the login GUI"() {
    given: 'a specific logging state'
    logout()

    when: 'the GUI is displayed'
    go '/logging'
    currentUrl.endsWith('/login/auth')

    and: 'the user logs in'
    loggedInUser = 'admin'
    $('input', id: 'username') << 'admin'
    $('input', id: 'password') << 'admin'
    clickButton('login')

    then: 'we are redirected to the original page'
    currentUrl.endsWith('/logging')
  }

  def "verify that accessing a CRUD GUI before logging called redirects to the login GUI"() {
    // CRUD GUIs use different re-direct mechanism
    given: 'a specific logging state'
    logout()

    when: 'the GUI is displayed'
    go '/user'
    currentUrl.endsWith('/login/auth')

    and: 'the user logs in'
    loggedInUser = 'admin'
    $('input', id: 'username') << 'admin'
    $('input', id: 'password') << 'admin'
    clickButton('login')

    then: 'we are redirected to the original page'
    currentUrl.endsWith('/user')
  }

  def "verify that login failures are displayed and redirect works after correction"() {
    given: 'a specific logging state'
    logout()

    when: 'the GUI is displayed'
    go '/user'
    currentUrl.endsWith('/login/auth')

    and: 'the user logs in with wrong data'
    loggedInUser = 'admin'
    $('input', id: 'username') << 'admin'
    $('input', id: 'password') << 'adminX'
    clickButton('login')

    then: 'the error is displayed'
    $('#errors').text() == lookup('login.failed.message')

    when: 'the user logs in correctly'
    loggedInUser = 'admin'
    $('input', id: 'username') << 'admin'
    $('input', id: 'password') << 'admin'
    clickButton('login')

    then: 'we are redirected to the original page'
    currentUrl.endsWith('/user')
  }

}
