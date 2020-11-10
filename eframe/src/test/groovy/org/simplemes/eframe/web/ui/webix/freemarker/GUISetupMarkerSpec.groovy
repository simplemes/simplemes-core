/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import asset.pipeline.micronaut.AssetPipelineService
import ch.qos.logback.classic.Level
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.LogUtils
import org.simplemes.eframe.system.controller.LoggingController
import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.MockBean

/**
 * Tests.
 */
class GUISetupMarkerSpec extends BaseMarkerSpecification {

  def "verify that the marker creates the setup head elements correctly"() {
    given: 'a mocked asset pipeline bean'
    new MockBean(this, AssetPipelineService).install()

    and: 'a locale'
    GlobalUtils.defaultLocale = locale

    when: 'the head section is created'
    def page = execute(source: '<@efGUISetup/>')

    then: 'the right locale is included in the right order'
    page.contains("""webix.i18n.setLocale("$expectedLanguage");""")
    page.indexOf('webix.i18n.setLocale("') < page.indexOf('webix.i18n.setLocale()')

    and: 'the override of the date formats is done'
    page.contains('webix.i18n.dateFormat = ')
    page.contains('webix.i18n.fullDateFormat = ')

    where:
    // Needs to provide a specific dialect for Webix 8.0 to use the locale.
    locale                         | expectedLanguage
    Locale.GERMANY                 | "de-DE"
    Locale.GERMAN                  | "de-DE"
    Locale.US                      | "en-US"
    Locale.ENGLISH                 | "en-US"
    Locale.forLanguageTag('ru-RU') | "ru-RU"
    Locale.FRENCH                  | "fr-FR"
    Locale.CANADA_FRENCH           | "fr-FR"
    Locale.JAPANESE                | "ja-JP"
    Locale.forLanguageTag('be-BY') | "be-BY"
    Locale.forLanguageTag('es-ES') | "es-ES"
    Locale.ITALIAN                 | "it-IT"
    Locale.CHINESE                 | "zh-CN"
    Locale.forLanguageTag('pt-BR') | "pt-BR"
    Locale.KOREA                   | "ko-KR"  // Not available with toolkit, but just in case it is added later.
  }

  def "verify that the marker creates the javascript logger setup logic"() {
    given: 'a mocked asset pipeline bean'
    new MockBean(this, AssetPipelineService).install()

    and: 'the to-server level is set to a specific level.'
    def originalToServerLevel = LogUtils.getLogger(LoggingController.CLIENT_TO_SERVER_LOGGER).level
    LogUtils.getLogger(LoggingController.CLIENT_TO_SERVER_LOGGER).setLevel(Level.INFO)

    and: 'the logging level is set for the client JS code'
    LogUtils.getLogger('client.logging').setLevel(Level.TRACE)

    when: 'the head section is created'
    def page = execute(source: '<@efGUISetup/>', uri: '/logging/dummy?arg=value')

    then: 'the HTML/JS is valid'
    checkPage(page)

    then: 'the client level is set to trace - 1000'
    def setupText = JavascriptTestUtils.extractBlock(page, 'JL().setOptions({')
    setupText.contains('"level": 1000')

    and: 'appender is set to send it to the server for logging'
    def ajaxAppenderText = JavascriptTestUtils.extractBlock(setupText, "JL.createAjaxAppender('ajaxAppender').setOptions({")
    ajaxAppenderText.contains('"url": "/logging/client?logger=client.logging"')
    ajaxAppenderText.contains('"level": 3000')
    setupText.contains("JL.createConsoleAppender('consoleAppender'")

    cleanup:
    LogUtils.getLogger(LoggingController.CLIENT_TO_SERVER_LOGGER).setLevel(originalToServerLevel)
  }

}
