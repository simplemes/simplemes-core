package org.simplemes.eframe.preference.event

import grails.gorm.transactions.Rollback
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
class SplitterResizedSpec extends BaseSpecification {

  static specNeeds = [HIBERNATE, JSON]

  @Rollback
  def "verify that dialog resize event creates the preference correctly"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    when: 'the event is handled'
    new SplitterResized().handleEvent([pageURI: '/app/testPage',
                                       event  : 'SplitterResized',
                                       element: '_dOPERATOR',
                                       resizer: 'resizerA',
                                       size   : '23.4'])

    then: 'the settings are saved'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.TEST_USER
      element '_dOPERATOR'
    }
    preference.resizerA.size == 23.4
  }

  @Rollback
  def "verify that two resize events saves the latest size correctly"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    when: 'the two events are handled'
    new SplitterResized().handleEvent([pageURI: '/app/testPage',
                                       event  : 'SplitterResized',
                                       element: '_dOPERATOR',
                                       resizer: 'resizerA',
                                       size   : '23.4'])
    new SplitterResized().handleEvent([pageURI: '/app/testPage',
                                       event  : 'SplitterResized',
                                       element: '_dOPERATOR',
                                       resizer: 'resizerA',
                                       size   : '33.4'])

    then: 'the settings are saved'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.TEST_USER
      element '_dOPERATOR'
    }
    preference.resizerA.size == 33.4
  }

  @Rollback
  def "verify that dialog resize event handles URI with arguments"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    when: 'the event is handled'
    new SplitterResized().handleEvent([pageURI: '/app/testPage/show?test=12&dummy=xyz',
                                       event  : 'SplitterResized',
                                       element: '_dOPERATOR',
                                       resizer: 'resizerA',
                                       size   : 13.4])

    then: 'the settings are saved'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/testPage/show?test=13&dummy=xyz'
      user SecurityUtils.TEST_USER
      element '_dOPERATOR'
    }
    preference.resizerA.size == 13.4
  }

  @Rollback
  def "verify that splitter resize gracefully handles bad size"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    when: 'the event is handled'
    new SplitterResized().handleEvent([pageURI: '/app/testPage/show?test=12&dummy=xyz',
                                       event  : 'SplitterResized',
                                       element: '_dOPERATOR',
                                       resizer: 'resizerA',
                                       size   : 'XYZ'])

    then: 'the settings are not saved'
    UserPreference.list().size() == 0
  }

  @Rollback
  def "verify that a splitter resize event with no current user logged does not save any values"() {
    given: 'no current user'
    SecurityUtils.simulateNoUserInUnitTest = true

    when: 'the event is handled'
    new SplitterResized().handleEvent([pageURI: '/app/testPage/show?test=12&dummy=xyz',
                                       event  : 'SplitterResized',
                                       element: '_dOPERATOR',
                                       size   : '12.3'])

    then: 'no settings are saved'
    UserPreference.list().size() == 0

    cleanup:
    SecurityUtils.simulateNoUserInUnitTest = false
  }

}
