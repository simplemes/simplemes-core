package org.simplemes.eframe.preference.event

import ch.qos.logback.classic.Level
import grails.gorm.transactions.Rollback
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.UnitTestUtils

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class TreeStateChangedSpec extends BaseSpecification {
  static specNeeds = [HIBERNATE, JSON]

  @Rollback
  def "verify that handleEvent saves the state in the preferences"() {
    given: 'a session and state change request parameters'
    def params = [expandedKeys: 'A,B', pageURI: '/app/testPage', event: 'TreeStateChanged', element: '_tree']

    and: 'a simulated current user is set'
    setCurrentUser()

    when: 'the event is handled'
    new TreeStateChanged().handleEvent(params)

    then: 'the state is saved in the preferences'
    def preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.TEST_USER
      element '_tree'
    }
    preference[TreeStateChanged.KEY].expandedKeys == 'A,B'
  }

  @Rollback
  def "verify that handleEvent works with multiple state changes"() {
    given: 'a session and an existing state in the preferences'
    def params = [expandedKeys: 'A,B', pageURI: '/app/testPage', event: 'TreeStateChanged', element: '_tree']
    new TreeStateChanged().handleEvent(params)

    and: 'a simulated current user is set'
    setCurrentUser()

    when: 'the state change event for other nodes is handled'
    params.expandedKeys = 'D,A'
    new TreeStateChanged().handleEvent(params)

    then: 'the state is saved in the preferences'
    def preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.TEST_USER
      element '_tree'
    }
    preference[TreeStateChanged.KEY].expandedKeys == 'D,A'
  }

  @Rollback
  def "verify that handleEvent clears the preference when the full tree is collapsed"() {
    given: 'a session and an existing state in the preferences'
    def params = [expandedKeys: 'A,B', pageURI: '/app/testPage', event: 'TreeStateChanged', element: '_tree']
    new TreeStateChanged().handleEvent(params)

    and: 'a simulated current user is set'
    setCurrentUser()

    when: 'the state change event for a collapsed tree is handled'
    params.expandedKeys = ''
    new TreeStateChanged().handleEvent(params)

    then: 'the state is saved in the preferences'
    def preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.TEST_USER
      element '_tree'
    }
    !preference[TreeStateChanged.KEY].expandedKeys
  }

  @Rollback
  def "verify that handleEvent gracefully handles no user in session"() {
    given: 'no user is in the simulated session'
    SecurityUtils.simulateNoUserInUnitTest = true

    when: 'a tree state change is handled'
    new TreeStateChanged().handleEvent([pageURI: '/app/testPage'])

    then: 'no preference is saved'
    UserPreference.count() == 0

    cleanup:
    SecurityUtils.simulateNoUserInUnitTest = false
  }

  @Rollback
  def "verify that logging works for the handleEvent method"() {
    given: 'a mock appender for Trace level only'
    def mockAppender = MockAppender.mock(TreeStateChanged, Level.TRACE)

    and: 'a simulated current user is set'
    setCurrentUser()

    when: 'the event is handled'
    def params = [expandedKeys: 'A,B', pageURI: '/app/testPage', event: 'TreeStateChanged', element: '_tree']
    new TreeStateChanged().handleEvent(params)

    then: 'the log message is written'
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, ['TRACE', '_tree', 'A,B'])
  }

}
