package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.BaseMarkerSpecification

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class LanguageMarkerSpec extends BaseMarkerSpecification {

  def "verify that the language marker uses the current locale"() {
    given: 'a locale'
    GlobalUtils.defaultLocale = locale

    expect: 'the correct language is used'
    execute(source: '<@efLanguage/>') == results

    where:
    locale         | results
    Locale.GERMANY | "de_DE"
    Locale.US      | "en_US"
    Locale.ENGLISH | "en"
  }

}
