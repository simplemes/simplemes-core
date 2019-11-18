package org.simplemes.eframe.web.javascript

import ch.qos.logback.classic.Level
import org.simplemes.eframe.misc.LogUtils
import org.simplemes.eframe.system.controller.LoggingController
import org.simplemes.eframe.test.BaseJSSpecification
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.UnitTestUtils
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests of the JSNLog client-side logging interaction with the server-side.
 * No tests are made of the console output.
 */
@IgnoreIf({ !sys['geb.env'] })
class EFrameJSLoggingGUISpec extends BaseJSSpecification {

  def "verify that JL logging works on the client for a specific page"() {
    given: 'the to-server level is set to a specific level'
    def originalToServerLevel = LogUtils.getLogger(LoggingController.CLIENT_TO_SERVER_LOGGER).level
    LogUtils.getLogger(LoggingController.CLIENT_TO_SERVER_LOGGER).setLevel(Level.INFO)

    and: 'the tester page level is set to allow info messages'
    def loggerName = 'client.javascriptTester'
    def originalPageLevel = LogUtils.getLogger(loggerName).level
    LogUtils.getLogger(loggerName).setLevel(Level.INFO)

    and: 'a script to load and display a looked up label'
    def src = """JL().info("some details info message")"""

    and: 'a mock appender for Info level only'
    def mockAppender = MockAppender.mock(loggerName, Level.INFO)

    when: 'the JS is executed'
    login()
    execute(src)
    waitFor {
      mockAppender.message
    }

    then: 'the log message is written on the server log'
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, ['INFO', 'some details info message', 'ip='])

    cleanup:
    LogUtils.getLogger(LoggingController.CLIENT_TO_SERVER_LOGGER).setLevel(originalToServerLevel)
    LogUtils.getLogger(loggerName).setLevel(originalPageLevel)
  }

  def "verify that JL logging works on the client for all pages"() {
    given: 'the to-server level is set to a specific level'
    def originalToServerLevel = LogUtils.getLogger(LoggingController.CLIENT_TO_SERVER_LOGGER).level
    LogUtils.getLogger(LoggingController.CLIENT_TO_SERVER_LOGGER).setLevel(Level.INFO)

    and: 'the logging level for all pages is set to allow info messages'
    def originalPageLevel = LogUtils.getLogger(LoggingController.CLIENT_LOGGER).level
    LogUtils.getLogger(LoggingController.CLIENT_LOGGER).setLevel(Level.INFO)

    and: 'a script to load and display a looked up label'
    def src = """JL().info("some details info message")"""

    and: 'a mock appender for Info level only'
    def mockAppender = MockAppender.mock(LoggingController.CLIENT_LOGGER, Level.INFO)

    when: 'the JS is executed'
    login()
    execute(src)
    waitFor {
      mockAppender.message
    }

    then: 'the log message is written on the server log'
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, ['INFO', 'some details info message', 'ip='])

    cleanup:
    LogUtils.getLogger(LoggingController.CLIENT_TO_SERVER_LOGGER).setLevel(originalToServerLevel)
    LogUtils.getLogger(LoggingController.CLIENT_LOGGER).setLevel(originalPageLevel)
  }


}
