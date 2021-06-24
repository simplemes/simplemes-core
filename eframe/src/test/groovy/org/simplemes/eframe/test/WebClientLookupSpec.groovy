/*
 * Copyright (c) Michael Houston 2021. All rights reserved.
 */

package org.simplemes.eframe.test

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.i18n.GlobalUtils

/**
 *
 */
class WebClientLookupSpec extends BaseSpecification {

  def "verify that the constructor will fail if in a production environment"() {
    given: 'force the production environment'
    Holders.simulateProductionEnvironment = true

    when: 'the constructor is called'
    new WebClientLookup('dummy').lookup('label.flexType')

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['production'])
  }

  def "verify that the constructor parses the file correctly"() {
    expect: 'the lookup works'
    WebClientLookup.lookup('label.title') == 'Title'
    WebClientLookup.lookup('label.category', Locale.GERMANY) == 'Kategorie'

    and: 'the fallback to english works.'
    WebClientLookup.lookup('label._notInOtherLanguages', Locale.GERMANY) == 'Not'

  }


  // Other cases that might need testing.
  // Lookup with locale with country.  Finds the the language base?
  /*
  key:
  key: abcd
  key: '
  key: "
  key: ''
  key: '"'
  key: '""""""""""'
  key: ""
  key: "'"
  key: " "
  key: '*label.title'

   */

  def "verify that the basic lookup works"() {
    given: 'force the locale'
    GlobalUtils.defaultLocale = locale

    expect: 'the lookup works'
    WebClientLookup.lookup('label.title') == value

    where:
    locale         | value
    Locale.US      | 'Title'
    Locale.GERMANY | 'Titel'
  }

  def "verify that the addLocaleFolder works"() {
    when: 'the locale is added'
    WebClientLookup.addLocaleFolder("src/client/sample/src/locales")

    then: 'the lookup finds the value from the added folder'
    WebClientLookup.lookup('label.name', Locale.US) == 'Name'

    and: 'the default locale lookups still work'
    WebClientLookup.lookup('label.title', Locale.US) == 'Title'

  }
}
