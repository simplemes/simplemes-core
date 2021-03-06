/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.i18n

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.UnitTestUtils

/**
 * Tests the GlobalUtils.
 */
@SuppressWarnings("SpellCheckingInspection")
class GlobalUtilsSpec extends BaseSpecification {


  static specNeeds = [SERVER]

  /**
   * Save the original setting.
   */
  boolean originalLocalizationTest = Holders.configuration.localizationTest


  def setup() {
    GlobalUtils.defaultLocale = Locale.US
  }

  void cleanup() {
    // Make sure later tests have the same default locale.
    GlobalUtils.defaultLocale = Locale.default
    Holders.configuration.localizationTest = originalLocalizationTest
  }

  def "verify that basic lookups return expected values"() {
    expect: ''
    GlobalUtils.lookup(key, locale) == result

    where:
    key                        | locale        | result
    'home.label'               | Locale.US     | 'Home'
    'searchStatus.green.label' | Locale.GERMAN | 'Grün'
    'gibberish.label'          | Locale.US     | 'gibberish.label'
    'gibberish'                | Locale.US     | 'gibberish'
  }

  def "verify that basic lookup triggers missing property indicator flag when configured"() {
    given: 'the config option is set'
    Holders.configuration.localizationTest = true

    expect: ''
    GlobalUtils.lookup(key, locale) == result

    where:
    key               | locale    | result
    'home.label'      | Locale.US | 'Home'
    'gibberish.label' | Locale.US | '-==gibberish.label==-'
  }

  def "verify that getRequestLocale uses the defaultLocale works for unit tests"() {
    given: 'a fallback locale'
    GlobalUtils.defaultLocale = Locale.GERMAN

    expect: 'the fallback is used'
    GlobalUtils.lookup('home.label') == GlobalUtils.lookup('home.label', Locale.GERMAN)
  }

  def "verify that getRequestLocale can used the passed-in locale"() {
    given: 'a fallback locale'
    GlobalUtils.defaultLocale = Locale.GERMAN

    expect: 'the fallback is used'
    GlobalUtils.getRequestLocale(Locale.US) == Locale.US
  }

  def "verify that toStringLocalized works safely with all cases"() {
    given: 'source for a simple class'
    def src = """
    package sample
    class SampleClass {
      String toStringLocalized(Locale locale = null) {
        return "ABC "+locale?.toString()
      }
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    def o = clazz.newInstance()

    expect: 'the basic cases work'
    GlobalUtils.toStringLocalized(o, Locale.US) == "ABC $Locale.US"
    GlobalUtils.toStringLocalized(null, Locale.GERMANY) == ''

    and: 'classes without this localized toString work Ok'
    GlobalUtils.toStringLocalized('ABC', Locale.GERMANY) == 'ABC'

    when: 'default locale is set'
    GlobalUtils.defaultLocale = Locale.GERMANY

    then: 'the request locale is used if none provided'
    GlobalUtils.toStringLocalized(o) == "ABC $Locale.GERMANY"
  }

  def "verify that lookupLabelAndTooltip returns expected values"() {
    expect: ''
    GlobalUtils.lookupLabelAndTooltip(labelKey, tooltipKey) == new Tuple2(result[0], result[1])

    where:
    labelKey            | tooltipKey          | result
    'delete.menu.label' | null                | [lookup('delete.menu.label'), lookup('delete.menu.tooltip')]
    'more.menu.label'   | 'edit.menu.tooltip' | [lookup('more.menu.label'), lookup('edit.menu.tooltip')]
    'NotLookedUp'       | null                | ['NotLookedUp', null]
    'more.bad.label'    | null                | ['more.bad.label', null]
  }

  def "verify that lookupLabelAndTooltip fails on missing label key"() {
    when: 'the method called with nulls'
    GlobalUtils.lookupLabelAndTooltip(null, null)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['labelKey', 'null'])

  }

}
