package org.simplemes.eframe.preference.service


import org.simplemes.eframe.preference.ColumnPreference
import org.simplemes.eframe.preference.DialogPreference
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import sample.pogo.SamplePOGO

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class UserPreferenceServiceSpec extends BaseSpecification {

  static specNeeds = [SERVER, JSON]


  /**
   * Utility method to read the UserPreference record (for the page '/app/testPage' and TEST_USER), grab the
   * preferences for the given element and then return the first setting found.  Also forces the JSON
   * to be re-parsed from the raw text field.
   * @param element The element.
   * @return The first setting.  Can be null.
   */
  Object firstSetting(String element) {
    def userPreference = UserPreference.findByPageAndUserName('/app/testPage', SecurityUtils.TEST_USER)
    userPreference.textHasBeenParsed = false
    def simple = userPreference.preferences.find { it.element == element }
    return simple?.settings[0]
  }


  //TODO: Find alternative to @Rollback
  def "test basic saveSimplePreference with String value"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    when: 'a simple string preference is saved'
    new UserPreferenceService().saveSimplePreference('/app/testPage', 'workCenterDefault', 'WC237')

    then: 'the preference is saved'
    firstSetting('workCenterDefault').value == 'WC237'
  }

  //TODO: Find alternative to @Rollback
  def "test basic saveSimplePreference with POGO value"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    when: 'a simple POGO preference is saved'
    def pogo = new SamplePOGO(name: 'PG247', count: 237)
    new UserPreferenceService().saveSimplePreference('/app/testPage', 'pogoPref', pogo)

    then: 'the preference is saved'
    firstSetting('pogoPref') == pogo
  }

  //TODO: Find alternative to @Rollback
  def "test saveSimplePreference when merging with an existing preference on page"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    when: 'a simple POGO preference is saved'
    def pogo = new SamplePOGO(name: 'PG247', count: 237)
    new UserPreferenceService().saveSimplePreference('/app/testPage', 'pogoPref', pogo)

    and: 'a simple string preference is added to the page preferences'
    new UserPreferenceService().saveSimplePreference('/app/testPage', 'workCenterDefault', 'WC237')

    then: 'both were saved correctly'
    firstSetting('workCenterDefault').value == 'WC237'
    firstSetting('pogoPref') == pogo
  }

  //TODO: Find alternative to @Rollback
  def "test saveSimplePreference with a new empty value"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    when: 'an empty preference is saved'
    new UserPreferenceService().saveSimplePreference('/app/testPage', 'workCenterDefault', '')

    then: 'the preference was saved'
    firstSetting('workCenterDefault').value == ''
  }

  //TODO: Find alternative to @Rollback
  def "test saveSimplePreference update"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    when: 'a string preference is saved'
    new UserPreferenceService().saveSimplePreference('/app/testPage', 'workCenterDefault', 'orig')

    and: 'a different string preference is saved'
    new UserPreferenceService().saveSimplePreference('/app/testPage', 'workCenterDefault', 'new')

    then: 'the preference was saved'
    firstSetting('workCenterDefault').value == 'new'
  }

  //TODO: Find alternative to @Rollback
  def "test saveSimplePreference update with empty string"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    when: 'a string preference is saved'
    new UserPreferenceService().saveSimplePreference('/app/testPage', 'workCenterDefault', 'orig')

    and: 'an empty string preference is saved'
    new UserPreferenceService().saveSimplePreference('/app/testPage', 'workCenterDefault', '')

    then: 'the preference was saved'
    firstSetting('workCenterDefault').value == ''
  }

  //TODO: Find alternative to @Rollback
  def "test simplePreference round trip with a string"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    when: 'a string preference is saved'
    new UserPreferenceService().saveSimplePreference('/app/testPage', 'workCenterDefault', 'orig',)

    then: 'the preference is retrieved'
    new UserPreferenceService().findSimplePreference('/app/testPage', 'workCenterDefault') == 'orig'
  }

  //TODO: Find alternative to @Rollback
  def "test simplePreference round trip with a POGO"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    when: 'a POGO preference is saved'
    def pogo = new SamplePOGO(name: 'PG247', count: 237)
    new UserPreferenceService().saveSimplePreference('/app/testPage', 'pogo', pogo)

    then: 'the preference is retrieved'
    def foundPOGO = new UserPreferenceService().findSimplePreference('/app/testPage', 'pogo')

    and: 'the value is the same'
    foundPOGO == pogo
  }

  //TODO: Find alternative to @Rollback
  def "test findSimplePreference with no preference"() {
    expect: 'no preference is found '
    new UserPreferenceService().findSimplePreference('/app/testPage', 'workCenterDefault') == null
  }

  //TODO: Find alternative to @Rollback
  def "verify that guiStateChanged gracefully handles missing event"() {
    when: 'the guiStateChanged is called without an event'
    new UserPreferenceService().guiStateChanged([:])

    then: 'the correct exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['params.event', 'allowed'])
  }

  //TODO: Find alternative to @Rollback
  def "verify that guiStateChanged gracefully handles bad event"() {
    when: 'the guiStateChanged is called with bad event'
    new UserPreferenceService().guiStateChanged([event: 'ColumnResizedX'])

    then: 'the correct exception is thrown'
    def ex = thrown(Exception)
    ex instanceof ClassNotFoundException
    UnitTestUtils.assertExceptionIsValid(ex, ['ColumnResizedX'])
  }

  //TODO: Find alternative to @Rollback
  def "verify that guiStateChanged supports first column resize"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    when: 'the column resize is triggered'
    def params = [pageURI: '/app/testPage', event: 'ColumnResized', column: 'order', newSize: '125', element: 'OrderList']
    new UserPreferenceService().guiStateChanged(params)

    then: 'the preference is updated'
    def preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.TEST_USER
      element 'OrderList'
    }
    preference['order'].width == 125
  }

  //TODO: Find alternative to @Rollback
  def "verify that findPreferences can find existing preferences"() {
    given: 'a few preferences'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/test'
      user SecurityUtils.TEST_USER
      element '_dialogX'
    }
    def dialogPreference = new DialogPreference(width: 23.4, height: 24.5, left: 25.6, top: 26.7)
    preference.setPreference(dialogPreference).save()

    and: 'a dialog state for another dialog'
    preference = PreferenceHolder.find {
      page '/app/test'
      user SecurityUtils.TEST_USER
      element '_dialogY'
    }
    dialogPreference = new DialogPreference(width: 99, height: 98, left: 97, top: 96)
    preference.setPreference(dialogPreference).save()

    and: 'a column size'
    preference = PreferenceHolder.find {
      page '/app/test'
      user SecurityUtils.TEST_USER
      element 'OrderList'
    }
    def columnPreference = new ColumnPreference(column: 'order', width: 399)
    preference.setPreference(columnPreference).save()

    and: 'a simulated current user is set'
    setCurrentUser()

    when: 'the findPreferences is called'
    def preferences = new UserPreferenceService().findPreferences('/app/test', '_dialogX')

    then: 'the right one is found'
    preferences.keySet().size() == 1
    preferences['_dialogX'] instanceof DialogPreference
    preferences['_dialogX'].width == 23.4
    preferences['_dialogX'].height == 24.5
    preferences['_dialogX'].left == 25.6
    preferences['_dialogX'].top == 26.7
  }

  //TODO: Find alternative to @Rollback
  def "verify that findPreferences can find existing preferences with a type passed in"() {
    given: 'a few preferences'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/test'
      user SecurityUtils.TEST_USER
      element '_dialogX'
    }
    def dialogPreference = new DialogPreference(width: 23.4, height: 24.5, left: 25.6, top: 26.7)
    preference.setPreference(dialogPreference).save()

    and: 'a dialog state for another dialog'
    preference = PreferenceHolder.find {
      page '/app/test'
      user SecurityUtils.TEST_USER
      element '_dialogY'
    }
    dialogPreference = new DialogPreference(width: 99, height: 98, left: 97, top: 96)
    preference.setPreference(dialogPreference).save()

    and: 'a column size'
    preference = PreferenceHolder.find {
      page '/app/test'
      user SecurityUtils.TEST_USER
      element 'OrderList'
    }
    def columnPreference = new ColumnPreference(column: 'order', width: 399)
    preference.setPreference(columnPreference).save()

    and: 'a simulated current user is set'
    setCurrentUser()

    when: 'the findPreferences is called'
    def preferences = new UserPreferenceService().findPreferences('/app/test', null, 'DialogPreference')

    then: 'the right one is found'
    preferences.keySet().size() == 2
    preferences['_dialogX'] instanceof DialogPreference
    preferences['_dialogX'].width == 23.4
    preferences['_dialogY'] instanceof DialogPreference
    preferences['_dialogY'].width == 99

    and: 'no other types are returned'
    !preferences['list1']
  }

  //TODO: Find alternative to @Rollback
  def "verify that findPreferences handles empty scenario"() {
    when: 'the findPreferences is called'
    def preferences = new UserPreferenceService().findPreferences('/app/test', null, 'DialogPreference')

    then: 'none are found'
    preferences.keySet().size() == 0
  }

}
