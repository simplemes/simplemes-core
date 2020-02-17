/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.preference


import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.annotation.Rollback

/**
 * Tests.
 */
class PreferenceHolderSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  /**
   * Creates a test setting and saves it to the DB.
   * @param settings The list of settings to save for the user admin, in page /app/testPage, with element OrderList.
   * @param elementID The element (default: OrderList).
   * @return
   */
  PreferenceHolder buildSetting(List<PreferenceSettingInterface> settings, String elementID = 'OrderList') {
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element elementID
    }
    for (setting in settings) {
      preference.setPreference(setting).save()
    }

    // Force the re-parsing of the value from the DB to simulate a fresh record.  afterLoad() does not seem to work in tests.
    preference.userPreference.textHasBeenParsed = false

    return preference
  }

  @Rollback
  def "verify that DSL for save works with no settings"() {
    when: 'the preference is saved'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element 'OrderList'
    }
    preference.save()

    then: 'the value is in the database'
    def userPreference = UserPreference.findByUserNameAndPage(SecurityUtils.API_TEST_USER, '/app/testPage')
    userPreference.preferences.find { it.element == 'OrderList' } == null
  }

  @Rollback
  def "verify that set a preference setting works"() {
    when: 'the preference is saved'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element 'OrderList'
    }
    preference['order'] = new ColumnPreference(width: 437)
    preference.save()

    then: 'the value is in the database'
    def userPreference = UserPreference.findByUserNameAndPage(SecurityUtils.API_TEST_USER, '/app/testPage')
    def orderListPreference = userPreference.preferences.find { it.element == 'OrderList' }
    orderListPreference != null
    orderListPreference.settings[0].width == 437
  }

  @Rollback
  def "verify that get on the DSL field names finds the settings"() {
    when: 'the non-existent preference looked for'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element 'OrderList'
    }
    preference['order'] = new ColumnPreference(width: 437)
    preference.save()

    then: 'get on the DSL elements return nulls'
    preference['page'] == null
    preference['user'] == null
    preference['element'] == null
    preference['name'] == null
  }

  @Rollback
  def "verify that DSL works with the multiple named preferences"() {
    when: 'two existing preferences with different names exist'
    PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      name 'ABC'
      element 'OrderList'
    }.setPreference(new ColumnPreference(column: 'ColumnA', width: 23)).save()
    PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      name 'XYZ'
      element 'OrderList'
    }.setPreference(new ColumnPreference(column: 'ColumnX', width: 33)).save()

    then: 'the first named preference can be found'
    def preferenceABC = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      name 'ABC'
      element 'OrderList'
    }
    preferenceABC['ColumnA'].width == 23

    and: 'the other named preference can be found'
    def preferenceXYZ = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      name 'XYZ'
      element 'OrderList'
    }
    preferenceXYZ['ColumnX'].width == 33
  }

  @Rollback
  def "verify that DSL works with the optional name"() {
    when: 'two existing preferences with different names exist'
    PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element 'OrderList'
    }.setPreference(new ColumnPreference(column: 'ColumnA', width: 23)).save()
    PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      name 'XYZ'
      element 'OrderList'
    }.setPreference(new ColumnPreference(column: 'ColumnX', width: 33)).save()

    then: 'the first named preference can be found'
    def preferenceA = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element 'OrderList'
    }
    preferenceA['ColumnA'].width == 23

    and: 'the first named preference can be found'
    def preferenceX = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      name 'XYZ'
      element 'OrderList'
    }
    preferenceX['ColumnX'].width == 33
  }

  @Rollback
  def "verify that DSL works with a page with arguments"() {
    when: 'the preference is saved'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/testPage/show?test=12&dummy=xyz'
      user SecurityUtils.API_TEST_USER
      element 'OrderList'
    }
    preference['order'] = new ColumnPreference(width: 437)
    preference.save()

    then: 'the value is in the database'
    def userPreference = UserPreference.findByUserNameAndPage(SecurityUtils.API_TEST_USER, '/app/testPage/show')
    userPreference.preferences.find { it.element == 'OrderList' } != null
  }

  @Rollback
  def "verify that DSL for setting preference and save works"() {
    given: 'a preference setting to save'
    def columnPreference = new ColumnPreference(column: 'ABC', width: 237)

    when: 'the preference is saved'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element 'OrderList'
    }
    preference.setPreference(columnPreference).save()

    then: 'the value is in the database'
    def userPreference = UserPreference.findByUserNameAndPage(SecurityUtils.API_TEST_USER, '/app/testPage')
    def orderListPreference = userPreference.preferences.find { it.element == 'OrderList' }
    orderListPreference.settings.contains(columnPreference)
  }

  @Rollback
  def "verify that DSL for save and find round trip works"() {
    given: 'a preference setting that is saved'
    def columnPreference = new ColumnPreference(column: 'ABC', width: 237)

    and: 'the preference is saved'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element 'OrderList'
    }
    preference.setPreference(columnPreference).save()

    and: 'force the re-parsing of the value from the DB to simulate a fresh record.  afterLoad() does not seem to work in tests.'
    preference.userPreference.textHasBeenParsed = false

    when: 'the preference is loaded again'
    PreferenceHolder preference2 = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element 'OrderList'
    }

    then: 'the value stored is found'
    def columnPref = preference2['ABC']
    columnPref.width == 237
  }

  @Rollback
  def "verify that DSL can overwrite the values"() {
    given: 'a saved preference setting'
    def preference = buildSetting([new ColumnPreference(column: 'XYZ', width: 137)])

    when: 'the preference is loaded again'
    PreferenceHolder preference2 = PreferenceHolder.find {
      page preference._page
      user preference._user
      element preference._element
    }

    and: 'a new value is saved over it'
    def columnPreference = new ColumnPreference(column: 'XYZ', width: 237)
    preference2.setPreference(columnPreference).save()

    and: 'force the re-parsing of the value from the DB to simulate a fresh record.  afterLoad() does not seem to work in tests.'
    preference2.userPreference.textHasBeenParsed = false

    and: 'the value is re-read'
    PreferenceHolder preference3 = PreferenceHolder.find {
      page preference._page
      user preference._user
      element preference._element
    }

    then: 'the value stored is found'
    def columnPref = preference3['XYZ']
    columnPref.width == 237

    and: 'no other settings were created'
    preference3.userPreference.preferences[0].settings.size() == 1
  }

  @Rollback
  def "verify that DSL can store multiple settings before saving"() {
    when: 'several preference settings are saved'
    def preference = buildSetting([new ColumnPreference(column: 'ABC', width: 137),
                                   new ColumnPreference(column: 'DEF', width: 237),
                                   new ColumnPreference(column: 'XYZ', width: 337)])

    then: 'the values can be read'
    PreferenceHolder preferenceFound = PreferenceHolder.find {
      page preference._page
      user preference._user
      element preference._element
    }

    and: 'all values are available'
    preferenceFound.get('ABC').width == 137
    preferenceFound.get('DEF').width == 237
    preferenceFound.get('XYZ').width == 337
  }

  @Rollback
  def "verify that DSL can store multiple elements in one User Preference record"() {
    when: 'several preference settings are saved'
    def preference = buildSetting([new ColumnPreference(column: 'ABC', width: 137)], 'ListA')
    buildSetting([new ColumnPreference(column: 'ABC', width: 237)], 'ListB')
    buildSetting([new ColumnPreference(column: 'XYZ', width: 337)], 'ListB')

    then: 'the first element value can be read'
    PreferenceHolder preferenceFound1 = PreferenceHolder.find {
      page preference._page
      user preference._user
      element 'ListA'
    }
    preferenceFound1.get('ABC').width == 137

    and: 'the second element value can be read'
    PreferenceHolder preferenceFound2 = PreferenceHolder.find {
      page preference._page
      user preference._user
      element 'ListB'
    }
    preferenceFound2.get('ABC').width == 237
    preferenceFound2.get('XYZ').width == 337
  }

  @Rollback
  def "verify that DSL when find does not find a preference, no record is created"() {
    when: 'the preference is saved'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element 'OrderList'
    }

    then: 'the user preference is unsaved'
    preference.userPreference.uuid == null
    UserPreference.list().size() == 0
  }

  @Rollback
  def "verify that the holder can support access to multiple elements by changing the element"() {
    when: 'two existing preferences with different elements exist'
    PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element 'OrderListA'
    }.setPreference(new ColumnPreference(column: 'order', width: 23)).save()
    PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element 'OrderListB'
    }.setPreference(new ColumnPreference(column: 'order', width: 33)).save()

    then: 'the preference for the first element can be used'
    def preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element 'OrderListA'
    }
    preference['order'].width == 23

    when: 'the element is changed to the second element'
    preference.element = 'OrderListB'

    then: 'the preference for the other element is available'
    preference['order'].width == 33

    and: 'the list of element names can be found'
    preference.elementNames == ['OrderListA', 'OrderListB']
  }

  @Rollback
  def "verify that the holder can support access to multiple elements by changing the element to an element that does not exist"() {
    when: 'two existing preferences with different elements exist'
    PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element 'OrderListA'
    }.setPreference(new ColumnPreference(column: 'order', width: 23)).save()

    then: 'the preference for the first element can be used'
    def preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element 'OrderListA'
    }
    preference['order'].width == 23

    when: 'the element is changed to the second element'
    preference.element = 'OrderListB'

    and: 'the preference value is set'
    ColumnPreference columnPref = (ColumnPreference) preference['order'] ?: new ColumnPreference(column: 'order')
    columnPref.width = 33
    preference.setPreference(columnPref).save()

    then: 'the preferences can be found'
    def preference2 = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element 'OrderListB'
    }
    preference2['order'].width == 33
  }

  @Rollback
  def "verify that the holder can support access to multiple elements without an element on the initial query"() {
    when: 'two existing preferences with different elements exist'
    PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element 'OrderListA'
    }.setPreference(new ColumnPreference(column: 'order', width: 23)).save()
    PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element 'OrderListB'
    }.setPreference(new ColumnPreference(column: 'order', width: 33)).save()

    then: 'the preferences can be found'
    def preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
    }

    and: 'the list of element names can be found'
    preference.elementNames == ['OrderListA', 'OrderListB']

    and: 'a default element is used'
    preference['order'] != null
  }

  @Rollback
  def "verify that the holder can set values in multiple elements with a single find"() {
    when: 'two existing preferences with different elements exist'
    def preferenceHolder = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element 'OrderListA'
    }.setPreference(new ColumnPreference(column: 'order', width: 23))

    and: 'the element is changed'
    preferenceHolder._element = 'OrderListB'

    and: 'the value is set and saved'
    preferenceHolder.setPreference(new ColumnPreference(column: 'order', width: 43)).save()

    then: 'both  values can be used'
    def preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element 'OrderListA'
    }
    preference['order'].width == 23

    when: 'the element is changed to the second element'
    preference.element = 'OrderListB'

    then: 'the preference for the other element is available'
    preference['order'].width == 43

    and: 'the list of element names can be found'
    preference.elementNames == ['OrderListA', 'OrderListB']
  }

  @Rollback
  def "verify that the holder can set values in multiple elements with a single find without an initial element"() {
    when: 'a preference is started'
    def preferenceHolder = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
    }

    and: 'the first element is changed'
    preferenceHolder.element = 'OrderListA'
    preferenceHolder.setPreference(new ColumnPreference(column: 'order', width: 23))

    and: 'the next element preference is set '
    preferenceHolder.element = 'OrderListB'
    preferenceHolder.setPreference(new ColumnPreference(column: 'order', width: 43)).save()

    then: 'both  values can be used'
    def preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element 'OrderListA'
    }
    preference['order'].width == 23

    when: 'the element is changed to the second element'
    preference.element = 'OrderListB'

    then: 'the preference for the other element is available'
    preference['order'].width == 43

    and: 'the list of element names can be found'
    preference.elementNames == ['OrderListA', 'OrderListB']
  }

  @Rollback
  def "verify that the getSettings method works"() {
    when: 'the preference is saved'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element 'OrderList'
    }
    preference.setPreference(new ColumnPreference()).save()

    then: 'the geSettings result is correct'
    preference.settings.size() == 1
  }

  @Rollback
  def "verify that the setSettings method works"() {
    when: 'the preference is saved'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element 'OrderList'
    }
    preference.settings = [new ColumnPreference()]
    preference.save()

    then: 'the geSettings result is correct'
    preference.settings.size() == 1
  }

  @Rollback
  def "verify that set a preference setting works on an element with invalid JSON tag characters"() {
    given: 'an element name with '
    def badElement = 'OrderList/":some.jrxml'

    when: 'the preference is saved'
    PreferenceHolder preference = PreferenceHolder.find {
      page '/app/testPage'
      user SecurityUtils.API_TEST_USER
      element badElement
    }
    preference['order'] = new ColumnPreference(width: 437)
    preference.save()

    and: 'the text is forced to be re-parsed'
    preference.userPreference.textHasBeenParsed = false

    then: 'the value is in the database'
    def userPreference = UserPreference.findByUserNameAndPage(SecurityUtils.API_TEST_USER, '/app/testPage')
    def pref = userPreference.preferences.find { it.element == badElement }
    pref != null
    pref.settings[0].width == 437
  }


}
