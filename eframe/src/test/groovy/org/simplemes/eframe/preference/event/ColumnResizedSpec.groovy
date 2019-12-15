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
class ColumnResizedSpec extends BaseSpecification {

  static specNeeds = [SERVER, JSON]

  //TODO: Find alternative to @Rollback
  def "verify that column resize the first time works"() {
    given: 'the parameters for the GUI event handler'
    def params = [pageURI: '/app/testPage', event: 'ColumnResized', column: 'order', newSize: '109', element: 'OrderList']

    and: 'a simulated current user is set'
    setCurrentUser()

    when: 'the event is handled'
    new ColumnResized().handleEvent(params)

    then: 'the settings are saved'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.TEST_USER
      element 'OrderList'
    }
    preference['order'].width == 109
  }

  //TODO: Find alternative to @Rollback
  def "verify that column resize works after several resizes"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    when: 'the event is handled'
    def params1 = [pageURI: '/app/testPage', event: 'ColumnResized', column: 'order',
                   newSize: '109', element: 'OrderList']
    new ColumnResized().handleEvent(params1)

    and: 'resized again'
    def params2 = [pageURI: '/app/testPage', event: 'ColumnResized', column: 'order',
                   newSize: '137', element: 'OrderList']
    new ColumnResized().handleEvent(params2)

    then: 'the final settings are found'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.TEST_USER
      element 'OrderList'
    }
    preference['order'].width == 137
  }

  //TODO: Find alternative to @Rollback
  def "verify that column resize supports the URL format used for show pages"() {
    given: 'the parameters for the GUI event handler'
    def params = [pageURI: '/app/parent/show/11', event: 'ColumnResized', column: 'order', newSize: '109', element: 'OrderList']

    and: 'a simulated current user is set'
    setCurrentUser()

    when: 'the event is handled'
    new ColumnResized().handleEvent(params)

    then: 'the settings are saved'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/parent/show'
      user SecurityUtils.TEST_USER
      element 'OrderList'
    }
    preference['order'].width == 109
  }

  //TODO: Find alternative to @Rollback
  def "verify that column resize supports the URL format used for pages with arguments"() {
    given: 'the parameters for the GUI event handler'
    def params = [pageURI: '/app/parent/show?test=null', event: 'ColumnResized', column: 'order', newSize: '107', element: 'OrderList']

    and: 'a simulated current user is set'
    setCurrentUser()

    when: 'the event is handled'
    new ColumnResized().handleEvent(params)

    then: 'the settings are saved'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/parent/show'
      user SecurityUtils.TEST_USER
      element 'OrderList'
    }
    preference['order'].width == 107
  }

  //TODO: Find alternative to @Rollback
  def "verify that a column resize with no current user logged does not save any values"() {
    given: 'the parameters for the GUI event handler'
    def params = [pageURI: '/app/parent/show?test=null', event: 'ColumnResized', column: 'order', newSize: '107', element: 'OrderList']

    and: 'no current user'
    SecurityUtils.simulateNoUserInUnitTest = true

    when: 'the event is handled'
    new ColumnResized().handleEvent(params)

    then: 'no settings are saved'
    UserPreference.list().size() == 0

    cleanup:
    SecurityUtils.simulateNoUserInUnitTest = false
  }


}
