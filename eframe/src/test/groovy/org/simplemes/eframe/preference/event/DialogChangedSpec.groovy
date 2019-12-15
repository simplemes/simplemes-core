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
class DialogChangedSpec extends BaseSpecification {

  static specNeeds = [SERVER, JSON]

  //TODO: Find alternative to @Rollback
  def "verify that dialog resize event creates the preference correctly"() {
    given: 'the parameters for the GUI event handler'
    def params = [pageURI: '/app/testPage',
                  event  : DialogChanged.EVENT,
                  element: '_dash_page',
                  width  : '23.4',
                  height : '25.6',
                  left   : '26.7',
                  top    : '27.8']

    and: 'a simulated current user is set'
    setCurrentUser()

    when: 'the event is handled'
    new DialogChanged().handleEvent(params)

    then: 'the settings are saved'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.TEST_USER
      element '_dash_page'
    }
    preference[DialogChanged.KEY].width == 23.4
    preference[DialogChanged.KEY].height == 25.6
    preference[DialogChanged.KEY].left == 26.7
    preference[DialogChanged.KEY].top == 27.8
  }

  //TODO: Find alternative to @Rollback
  def "verify that dialog can be resized twice"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    when: 'the event is handled once'
    new DialogChanged().handleEvent([pageURI: '/app/testPage',
                                     event  : DialogChanged.EVENT,
                                     element: '_dash_page',
                                     width  : '23.4', height: '25.6',
                                     left   : '26.7', top: '27.8'])

    and: 'the event is handled again'
    new DialogChanged().handleEvent([pageURI: '/app/testPage',
                                     event  : DialogChanged.EVENT,
                                     element: '_dash_page',
                                     width  : '33.4', height: '35.6',
                                     left   : '36.7', top: '37.8'])

    then: 'the settings are saved'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.TEST_USER
      element '_dash_page'
    }
    preference[DialogChanged.KEY].width == 33.4
    preference[DialogChanged.KEY].height == 35.6
    preference[DialogChanged.KEY].left == 36.7
    preference[DialogChanged.KEY].top == 37.8
  }

  //TODO: Find alternative to @Rollback
  def "verify that handler can gracefully deal with bad values"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    when: 'the event is handled once'
    new DialogChanged().handleEvent([pageURI: '/app/testPage',
                                     event  : DialogChanged.EVENT,
                                     element: '_dash_page',
                                     width  : '23.4', height: '25.6',
                                     left   : 26.7, top: 27.8])

    and: 'the event is handled again'
    new DialogChanged().handleEvent([pageURI: '/app/testPage',
                                     event  : DialogChanged.EVENT,
                                     element: '_dash_page',
                                     width  : 'XYZ', height: '35.6',
                                     left   : '36.7', top: '37.8'])

    then: 'the settings are saved'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.TEST_USER
      element '_dash_page'
    }
    preference[DialogChanged.KEY].width == 23.4
    preference[DialogChanged.KEY].height == 25.6
    preference[DialogChanged.KEY].left == 26.7
    preference[DialogChanged.KEY].top == 27.8
  }

  //TODO: Find alternative to @Rollback
  def "verify that a dialog changed event with no current user logged does not save any values"() {
    given: 'no current user'
    SecurityUtils.simulateNoUserInUnitTest = true

    when: 'the event is handled'
    new DialogChanged().handleEvent([pageURI: '/app/testPage',
                                     event  : DialogChanged.EVENT,
                                     element: '_dash_page',
                                     width  : '23.4', height: '25.6',
                                     left   : 26.7, top: 27.8])

    then: 'no settings are saved'
    UserPreference.list().size() == 0

    cleanup:
    SecurityUtils.simulateNoUserInUnitTest = false
  }

}
