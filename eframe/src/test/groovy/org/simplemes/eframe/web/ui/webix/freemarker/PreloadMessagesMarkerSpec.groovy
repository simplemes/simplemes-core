package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.JavascriptUtils
import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.JavascriptTestUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class PreloadMessagesMarkerSpec extends BaseMarkerSpecification {

  def "verify that the basic lookup"() {
    given: 'a locale is set'
    GlobalUtils.defaultLocale = locale

    when: 'the output is rendered'
    def page = execute(source: '<@efPreloadMessages codes="ok.label,cancel.label"/>')

    then: 'the javascript is legal'
    JavascriptTestUtils.checkScriptsOnPage(page)

    and: 'the right messages are in the javascript'
    page.contains('eframe._addPreloadedMessages([')
    page.contains("""{"ok.label": "${GlobalUtils.lookup('ok.label', locale)}"}""")
    page.contains("""{"cancel.label": "${GlobalUtils.lookup('cancel.label', locale)}"}""")
    page.contains("""{"_decimal_": "$decimalString"}""")

    where:
    locale        | decimalString
    Locale.US     | '.'
    Locale.GERMAN | ','
  }

  def "verify that double quotes are supported in the looked-up text"() {
    when: 'the output is rendered'
    def page = execute(source: '<@efPreloadMessages codes="delete.confirm.message"/>')

    then: 'the javascript is legal'
    JavascriptTestUtils.checkScriptsOnPage(page)

    and: 'the right messages are in the javascript'
    def s = JavascriptUtils.escapeForJavascript(GlobalUtils.lookup('delete.confirm.message'))
    page.contains("""{"delete.confirm.message": "$s"}""")
  }

  def "verify that multi-line code lists work without embedding the newlines"() {
    when: 'the output is rendered'
    def page = execute(source: '<@efPreloadMessages codes="ok.label,   \n cancel.label"/>')

    then: 'the javascript is legal'
    JavascriptTestUtils.checkScriptsOnPage(page)

    and: 'the output is correct - with no newlines embedded in the localized text'
    page.contains("""{"ok.label": "${GlobalUtils.lookup('ok.label')}"}""")
    page.contains("""{"cancel.label": "${GlobalUtils.lookup('cancel.label')}"}""")
  }

  def "verify that missing commas in list of codes is handled"() {
    when: 'the output is rendered'
    def page = execute(source: '<@efPreloadMessages codes="ok.label,   cancel.label"/>')

    then: 'the javascript is legal'
    JavascriptTestUtils.checkScriptsOnPage(page)

    and: 'the output is correct'
    page.contains("""{"ok.label": "${GlobalUtils.lookup('ok.label')}"}""")
    page.contains("""{"cancel.label": "${GlobalUtils.lookup('cancel.label')}"}""")
  }

  def "verify that a missing code list is handled gracefully"() {
    when: 'the output is rendered'
    def page = execute(source: '<@efPreloadMessages/>')

    then: 'the javascript is legal'
    JavascriptTestUtils.checkScriptsOnPage(page)

    and: 'the output is correct'
    page.contains('<script>')
    page.contains('eframe._addPreloadedMessages([')
    page.contains("""{"_decimal_": "${NumberUtils.determineDecimalSeparator()}"}""")
  }

  def "verify that an empty code list is handled gracefully"() {
    when: 'the output is rendered'
    def page = execute(source: '<@efPreloadMessages codes=""/>')

    then: 'the javascript is legal'
    JavascriptTestUtils.checkScriptsOnPage(page)

    and: 'the output is correct'
    page.contains('<script>')
    page.contains('eframe._addPreloadedMessages([')
  }


}
