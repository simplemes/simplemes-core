package org.simplemes.eframe.preference.event

import ch.qos.logback.classic.Level
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
class ListSortedSpec extends BaseSpecification {

  static specNeeds = [SERVER, JSON]

  //TODO: Find alternative to @Rollback
  def "verify that handleEvent works for basic case"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    when: 'the list sorted event is handled'
    def params = [pageURI: '/app/testPage', event: 'ListSorted', 'sort': 'title', 'order': 'desc', element: 'OrderList']
    new ListSorted().handleEvent(params)

    then: 'the settings are saved'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.TEST_USER
      element 'OrderList'
    }
    //preference['title'].sortLevel == 1
    preference['title'].sortAscending == false
  }

  //TODO: Find alternative to @Rollback
  def "verify that sort order is changed"() {
    given: 'the original settings is descending sort order'
    def params = [pageURI: '/app/testPage', event: 'ListSorted', 'sort': 'order',
                  'order': 'desc', element: 'OrderList',]
    new ListSorted().handleEvent(params)

    and: 'a simulated current user is set'
    setCurrentUser()

    when: 'the event to sort on another column is handled'
    def map = [pageURI: '/app/testPage', event: 'ListSorted', element: 'OrderList']
    map.'sort' = 'name'
    map.'order' = 'asc'
    new ListSorted().handleEvent(map)

    then: 'the settings are saved'
    PreferenceHolder preference2 = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.TEST_USER
      element 'OrderList'
    }
    preference2['name'].sortLevel == 1
    preference2['name'].sortAscending == true

    and: 'no other column is sorted'
    preference2['order'] == null
  }

  //TODO: Find alternative to @Rollback
  def "verify that handleEvent will reset to default settings"() {
    given: 'an existing sort order on one column'
    def params = [pageURI: '/app/testPage', event: 'ListSorted', 'sort': 'order',
                  'order': 'desc', element: 'OrderList']
    new ListSorted().handleEvent(params)

    and: 'a simulated current user is set'
    setCurrentUser()

    when: 'the list is sorted on the default column'
    def map = [pageURI: '/app/testPage', event: 'ListSorted', element: 'OrderList']
    map.defaultSortField = 'name'
    map.'sort' = 'name'
    map.'order' = 'asc'
    new ListSorted().handleEvent(map)

    then: 'the preferences are cleared'
    PreferenceHolder preference2 = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.TEST_USER
      element 'OrderList'
    }
    preference2.settings.size() == 0
  }

  //TODO: Find alternative to @Rollback
  def "verify that handleEvent strips the record ID from the Show page URL"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    when: 'the event is handled'
    def params = [pageURI: '/app/parent/show/11', event: 'ListSorted', 'sort': 'order',
                  'order': 'desc', element: 'OrderList']
    new ListSorted().handleEvent(params)

    then: 'the preference is saved under a URL without the record ID'
    PreferenceHolder preference2 = PreferenceHolder.find {
      page '/app/parent/show'
      user SecurityUtils.TEST_USER
      element 'OrderList'
    }
    preference2.settings.size() == 1
  }

  //TODO: Find alternative to @Rollback
  def "verify that handleEvent strips params from page URL"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    when: 'the event is handled'
    def params = [pageURI: '/app/parent/show?test=null', event: 'ListSorted', 'sort': 'order',
                  'order': 'desc', element: 'OrderList']
    new ListSorted().handleEvent(params)

    then: 'the preference is saved under a URL without the record ID'
    PreferenceHolder preference2 = PreferenceHolder.find {
      page '/app/parent/show'
      user SecurityUtils.TEST_USER
      element 'OrderList'
    }
    preference2.settings.size() == 1
  }

  //TODO: Find alternative to @Rollback
  def "verify that handleEvent does nothing if there is no user for the current session"() {
    given: 'no user for the session/request'
    SecurityUtils.simulateNoUserInUnitTest = true

    when: 'a sort event is handled'
    new ListSorted().handleEvent([pageURI: '/app/testPage'])

    then: 'no preference is created'
    UserPreference.count() == 0

    cleanup:
    SecurityUtils.simulateNoUserInUnitTest = false
  }

  //TODO: Find alternative to @Rollback
  def "verify that handleEvent does nothing if there is no URI in the event"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    when: 'a sort event is handled with no URI'
    new ListSorted().handleEvent([:])

    then: 'no preference is created'
    UserPreference.count() == 0
  }

  //TODO: Find alternative to @Rollback
  def "verify that logging works for the handleEvent method"() {
    given: 'a mock appender for Trace level only'
    def mockAppender = MockAppender.mock(ListSorted, Level.TRACE)

    and: 'a simulated current user is set'
    setCurrentUser()

    when: 'the event is handled'
    def params = [pageURI: '/app/testPage', event: 'ListSorted', 'sort': 'title', 'order': 'desc', element: 'OrderList']
    new ListSorted().handleEvent(params)

    then: 'the log message is written'
    mockAppender.messages.size() == 1
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, ['TRACE', '/app/testPage', 'title', 'sortAscending:false', 'OrderList'])
  }
}
