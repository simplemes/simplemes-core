package org.simplemes.eframe.test

import ch.qos.logback.classic.Level
import groovy.util.logging.Slf4j
import org.slf4j.LoggerFactory

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class MockAppenderSpec extends BaseSpecification {

  void cleanup() {
    MockAppender.cleanup()
  }

  def "basic appender works with info message"() {
    given: 'a mock appender'
    def mockAppender = MockAppender.mock(MockAppenderTestClass)

    when: 'a method logs a message'
    MockAppenderTestClass.doInfo('ABC')

    then: 'a single message is available for test'
    mockAppender.assertMessageIsValid(['[INFO]', 'doInfo ABC'])

    and: 'the simple message getter works'
    mockAppender.message.contains('ABC')
  }

  def "verify that cleanup will return to normal settings"() {
    given: 'a mock appender'
    def mockAppender = MockAppender.mock(MockAppenderTestClass)

    when: 'the mock is cleaned-up'
    mockAppender.cleanup()

    then: 'the mock appender is detached from the logger'
    def logger = LoggerFactory.ILoggerFactory.getLogger(MockAppenderTestClass.name)
    !logger.getAppender(mockAppender.name)
  }

  def "verifies that assertMessageIsValid() detects missing replaceable value"() {
    given: 'a mock appender'
    def mockAppender = MockAppender.mock(MockAppenderTestClass)

    and: 'a method logs a message without a parameter'
    MockAppenderTestClass.doInfoMissingValue()

    when: 'the message is tested'
    mockAppender.assertMessageIsValid([])

    then: 'an exception is thrown with the correct data'
    def e = thrown(AssertionError)
    UnitTestUtils.assertContainsAllIgnoreCase(e.toString(), ['doInfo', 'missing', 'replaceable', 'parameter'])
  }

  def "verifies that assertFirstFoundMessageIsValid detects missing replaceable value"() {
    given: 'a mock appender'
    def mockAppender = MockAppender.mock(MockAppenderTestClass)

    and: 'a method logs a message without a parameter'
    MockAppenderTestClass.doInfoMissingValue()

    when: 'the message is tested'
    mockAppender.assertFirstFoundMessageIsValid('doInfo', ['bad'])

    then: 'an exception is thrown with the correct data'
    def e = thrown(AssertionError)
    UnitTestUtils.assertContainsAllIgnoreCase(e.toString(), ['doInfo', 'other'])
  }

  def "verifies that assertFirstFoundMessageIsValid fails when no message found matching search value"() {
    given: 'a mock appender'
    def mockAppender = MockAppender.mock(MockAppenderTestClass)

    and: 'a method logs a message'
    MockAppenderTestClass.doInfoMissingValue()

    when: 'the message is tested'
    mockAppender.assertFirstFoundMessageIsValid('doInfoX', ['bad'])

    then: 'an exception is thrown with the correct data'
    def e = thrown(AssertionError)
    UnitTestUtils.assertContainsAllIgnoreCase(e.toString(), ['doInfo', 'other'])
  }

  def "verifies that assertFirstFoundMessageIsValid works"() {
    given: 'a mock appender'
    def mockAppender = MockAppender.mock(MockAppenderTestClass)

    when: 'a method logs a message without a parameter'
    MockAppenderTestClass.doInfo('ABC XYZ')

    then: 'the message is tested'
    mockAppender.assertFirstFoundMessageIsValid('XYZ', ['ABC'])
  }

  def "basic appender works with multiple messages"() {
    given: 'a mock appender'
    def mockAppender = MockAppender.mock(MockAppenderTestClass)

    when: 'a method logs a message'
    MockAppenderTestClass.doInfo('ABC')
    mockAppender.messages[0].contains('ABC')
    MockAppenderTestClass.doTrace('XYZ')

    then: 'both messages are available'
    mockAppender.messages.size() == 2
    mockAppender.messages[0].contains('ABC')
    mockAppender.messages[1].contains('XYZ')
  }

  def "appender filter messages with a given log level"() {
    given: 'a mock appender for Info level only'
    def mockAppender = MockAppender.mock(MockAppenderTestClass, Level.INFO)

    when: 'a method logs an info and trace message'
    MockAppenderTestClass.doInfo('ABC')
    MockAppenderTestClass.doTrace('XYZ')

    then: 'a single message is available for test'
    mockAppender.messages.size() == 1
  }

  def "basic appender works with logger name string input"() {
    given: 'a mock appender'
    def mockAppender = MockAppender.mock(MockAppenderTestClass.name)

    when: 'a method logs a message'
    MockAppenderTestClass.doInfo('ABC')

    then: 'a single message is available for test'
    mockAppender.assertMessageIsValid(['[INFO]', 'doInfo ABC'])

    and: 'the simple message getter works'
    mockAppender.message.contains('ABC')
  }

  def "test setting trace level from BaseSpecification"() {
    given: 'a mock appender'
    def mockAppender = MockAppender.mock(MockAppenderTestClass.name)

    and: 'the trace level is set on the tester'
    setTraceLogLevel(MockAppenderTestClass)

    when: 'a method logs a message'
    MockAppenderTestClass.doInfo('ABC')

    then: 'a single message is available for test'
    mockAppender.assertMessageIsValid(['[INFO]', 'doInfo ABC'])

    and: 'the simple message getter works'
    mockAppender.message.contains('ABC')

    and: 'the toString method works'
    mockAppender.toString()
  }


}

/**
 * The test class to test the MockAppender.  Mainly just logs messages.
 */
@Slf4j
class MockAppenderTestClass {

  /**
   * Log a test info message with a replaceable parameter.
   * @param o The parameter
   */
  static void doInfo(Object o) {
    log.info('doInfo {}', o)
  }

  /**
   * Log a test trace message with a replaceable parameter.
   * @param o The parameter
   */
  static void doTrace(Object o) {
    log.trace('doTrace {}', o)
  }

  /**
   * Log a test info message with a missing replaceable parameter.
   */
  static void doInfoMissingValue() {
    log.info('doInfo {} other data')
    log.info('ok message')
  }

}
