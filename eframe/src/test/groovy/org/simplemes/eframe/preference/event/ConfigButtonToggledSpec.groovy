package org.simplemes.eframe.preference.event


import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * Tests.
 */
class ConfigButtonToggledSpec extends BaseSpecification {

  static specNeeds = [SERVER, JSON]

  //TODO: Find alternative to @Rollback
  def "verify that button toggled to visible works"() {
    given: 'the parameters for the GUI event handler'
    def params = [pageURI: ConfigButtonToggled.PAGE, event: ConfigButtonToggled.EVENT,
                  visible: 'true', element: ConfigButtonToggled.ELEMENT]

    and: 'a simulated current user is set'
    setCurrentUser()

    when: 'the event is handled'
    new ConfigButtonToggled().handleEvent(params)

    then: 'the settings are saved'
    PreferenceHolder preference = PreferenceHolder.find {
      page ConfigButtonToggled.PAGE
      user SecurityUtils.TEST_USER
      element ConfigButtonToggled.ELEMENT
    }
    preference[ConfigButtonToggled.KEY].visible == true
  }

  //TODO: Find alternative to @Rollback
  def "verify that button toggled to not visible works"() {
    given: 'the parameters for the GUI event handler'
    def params = [pageURI: ConfigButtonToggled.PAGE, event: ConfigButtonToggled.EVENT,
                  visible: 'false', element: ConfigButtonToggled.ELEMENT]

    and: 'a simulated current user is set'
    setCurrentUser()

    when: 'the event is handled'
    new ConfigButtonToggled().handleEvent(params)

    then: 'the settings are saved'
    PreferenceHolder preference = PreferenceHolder.find {
      page ConfigButtonToggled.PAGE
      user SecurityUtils.TEST_USER
      element ConfigButtonToggled.ELEMENT
    }
    preference[ConfigButtonToggled.KEY].visible == false
  }

  //TODO: Find alternative to @Rollback
  def "verify that button toggled twice works"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    when: 'the event is triggered twice'
    def params1 = [pageURI: ConfigButtonToggled.PAGE, event: ConfigButtonToggled.EVENT,
                   visible: 'false', element: ConfigButtonToggled.ELEMENT]
    new ConfigButtonToggled().handleEvent(params1)
    def params2 = [pageURI: ConfigButtonToggled.PAGE, event: ConfigButtonToggled.EVENT,
                   visible: 'true', element: ConfigButtonToggled.ELEMENT]
    new ConfigButtonToggled().handleEvent(params2)

    then: 'the settings are saved correctly'
    PreferenceHolder preference = PreferenceHolder.find {
      page ConfigButtonToggled.PAGE
      user SecurityUtils.TEST_USER
      element ConfigButtonToggled.ELEMENT
    }
    preference[ConfigButtonToggled.KEY].visible == true
  }

  //TODO: Find alternative to @Rollback
  def "verify that a toggle config button event with no current user logged does not save any values"() {
    given: 'the parameters for the GUI event handler'
    def params = [pageURI: ConfigButtonToggled.PAGE, event: ConfigButtonToggled.EVENT,
                  visible: 'true', element: ConfigButtonToggled.ELEMENT]

    and: 'no current user'
    SecurityUtils.simulateNoUserInUnitTest = true

    when: 'the event is handled'
    new ConfigButtonToggled().handleEvent(params)

    then: 'no settings are saved'
    UserPreference.list().size() == 0

    cleanup:
    SecurityUtils.simulateNoUserInUnitTest = false
  }


}
