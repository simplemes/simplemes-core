package org.simplemes.eframe.preference.domain

import com.fasterxml.jackson.databind.ObjectMapper
import grails.gorm.transactions.Rollback
import org.simplemes.eframe.misc.FieldSizes
import org.simplemes.eframe.preference.ColumnPreference
import org.simplemes.eframe.preference.Preference
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.MockBean

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *
 */
class UserPreferenceSpec extends BaseSpecification {

  static specNeeds = [JSON, HIBERNATE]
  //static dirtyDomains = [UserPreference]

  def setup() {
    new MockBean(this, ObjectMapper, new ObjectMapper()).install()  // Auto cleaned up
  }

  def "verify that domain enforces constraints"() {
    expect: 'the constraints are enforced'
    DomainTester.test {
      domain UserPreference
      requiredValues userName: 'ADMIN', page: '/allFieldsDomains/index'
      maxSize 'userName', FieldSizes.MAX_CODE_LENGTH
      maxSize 'page', FieldSizes.MAX_PATH_LENGTH
      notNullCheck 'userName'
      notNullCheck 'page'
      fieldOrderCheck false
    }
  }

  @Rollback
  def "verify that JSON conversions work for the persisted JSON format"() {
    given: 'a test preference'
    // Create a preference value
    def columnPref = new ColumnPreference(column: 'order', width: 105)
    def preference = new Preference(element: 'OrderList', settings: [columnPref])
    def userPreference = new UserPreference(page: '/app/test', userName: 'JOE')
    userPreference.preferences << preference

    when: 'the record is saved'
    userPreference.save(flush: true)

    and: 'the text is re-parsed'
    // Force the re-parsing of the value from the DB to simulate a fresh record.  afterLoad() does not seem to work in tests.
    userPreference.textHasBeenParsed = false
    // Reload the record
    userPreference = UserPreference.findByUserNameAndPage('JOE', '/app/test')

    then: 'the user preference is correct with the right JSON'
    userPreference.preferencesText
    userPreference.preferences[0].element == 'OrderList'

    and: 'the class is correct'
    def pref = (Preference) userPreference.preferences[0]
    pref.class == Preference

    and: 'the settings are restored'
    pref.element == 'OrderList'
    pref.settings.size() == 1
    pref.settings[0].column == 'order'
    pref.settings[0].width == 105
  }

  @Rollback
  def "verify that an empty preference can be saved"() {
    given: 'an empty preference'
    def userPreference = new UserPreference(page: '/app/test', userName: 'JOE')

    when: 'the record is saved'
    userPreference.save(flush: true)

    and: 'the Map is cleared so we can test loading due to issues with afterLoad in test mode'
    userPreference.preferences = []
    userPreference = UserPreference.findByUserNameAndPage('JOE', '/app/test')

    then: 'the preferences an JSON are empty'
    userPreference.preferencesText == null
    userPreference.preferences == []
  }

  @Rollback
  def "verify that preferences can be cleared"() {
    given: 'a record with preference values'
    def columnPref1 = new ColumnPreference(column: 'order', width: 105)
    def columnPref2 = new ColumnPreference(column: 'product', width: 106)
    def columnPref3 = new ColumnPreference(column: 'qty', width: 107)
    def preference = new Preference(element: 'OrderList', settings: [columnPref1, columnPref2, columnPref3])
    def userPreference = new UserPreference(page: '/app/test', userName: 'JOE')
    userPreference.preferences << preference
    userPreference.save(flush: true)

    when: 'the record is re-read'
    userPreference = UserPreference.findByUserNameAndPage('JOE', '/app/test')

    and: 'the preferences are cleared and saved'
    userPreference.preferences = []
    userPreference.save(flush: true)

    then: 'the re-read values are correct'
    def userPreference2 = UserPreference.findByUserNameAndPage('JOE', '/app/test')
    userPreference2.preferencesText == null
    userPreference2.preferences == []
  }

  @Rollback
  def "verify that preferences can be changed in memory and saved"() {
    given: 'a preference with multiple values'
    def columnPref1 = new ColumnPreference(column: 'order', width: 105)
    def columnPref2 = new ColumnPreference(column: 'product', width: 106)
    def columnPref3 = new ColumnPreference(column: 'qty', width: 107)
    def preference = new Preference(element: 'OrderList', settings: [columnPref1, columnPref2, columnPref3])
    def userPreference = new UserPreference(page: '/app/test', userName: 'JOE')
    userPreference.preferences[0] = preference
    userPreference.save(flush: true)

    when: 'the preference is changed and re-saved'
    columnPref3.width = 108
    userPreference.save(flush: true)

    and: 'a read is simulated'
    // Force the re-parsing of the value from the DB to simulate a fresh record.  afterLoad() does not seem to work in tests.
    userPreference.textHasBeenParsed = false
    userPreference = UserPreference.findByUserNameAndPage('JOE', '/app/test')

    then: 'the updated value is found'
    userPreference.preferencesText
    userPreference.preferences[0].element == 'OrderList'

    def pref = (Preference) userPreference.preferences[0]
    pref.class == Preference

    pref.element == 'OrderList'
    pref.settings.size() == 3
    pref.settings[0].column == 'order'
    pref.settings[0].width == 105
    pref.settings[1].column == 'product'
    pref.settings[1].width == 106
    pref.settings[2].column == 'qty'
    pref.settings[2].width == 108
  }

  @Rollback
  def "verify that large preferences can be saved quickly enough"() {
    given: 'a very large preference with more than 4K XML'
    int maxColumns = 40
    def list = []
    for (int column = 0; column < maxColumns; column++) {
      def c = sprintf("%03d", column)
      def columnPref = new ColumnPreference(column: "order${c}", width: 100 + column)
      list << columnPref
    }
    def preference = new Preference(element: 'OrderList', settings: list)
    def userPreference = new UserPreference(page: '/app/test', userName: 'JOE')
    userPreference.preferences[0] = preference
    userPreference.save(flush: true)
    assert userPreference.preferencesText.length() > 4096

    when: 'the value is re-parsed during a simulated read'
    // Force the re-parsing of the value from the DB to simulate a fresh record.  afterLoad() does not seem to work in tests.
    userPreference.textHasBeenParsed = false
    userPreference = UserPreference.findByUserNameAndPage('JOE', '/app/test')

    then: 'the values are parsed quickly enough'
    userPreference.preferencesText
    long start = System.currentTimeMillis()
    userPreference.preferences[0].element == 'OrderList'
    long end = System.currentTimeMillis()
    (end - start) < 150        // Basic sanity check on the performance.
    //println "elapsed = ${end-start}"

    and: 'the values are correct'
    def pref = (Preference) userPreference.preferences[0]
    pref.class == Preference

    pref.element == 'OrderList'
    pref.settings.size() == maxColumns
    for (int column = 0; column < maxColumns; column++) {
      pref.settings[column].width == 100 + column
    }
  }


}
