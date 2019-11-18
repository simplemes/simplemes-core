package org.simplemes.eframe.misc

import ch.qos.logback.classic.Level
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.UnitTestUtils
import org.slf4j.LoggerFactory

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class LogUtilsSpec extends BaseSpecification {

  def oldLevelForLoggingIndex

  def setup() {
    // Remember the current setting for a logger so we can restore it when done.
    oldLevelForLoggingIndex = LogUtils.getLogger('javascript.logging.index').level
  }

  void cleanup() {
    // Restore the logger to avoid problems with other unit tests.
    LogUtils.getLogger('javascript.logging.index').setLevel(oldLevelForLoggingIndex)
    //LogUtils.mockLogger = null
  }

  def "verify that limitedLengthList handles the supported cases"() {
    when: 'long lists are truncated'
    def list = ['1234', '1234567890123456789012345678901234567890xxxx', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'LAST']
    def s = LogUtils.limitedLengthList(list)

    then: 'the trailing end is truncated'
    !s.contains('LAST')
    s.contains('R')
    s.contains('1 more')
    s.contains('(4 more chars)')
    !s.contains('x')

    when: 'a short list is used'
    list = ['1234', '1234567890123456789012345678901234567890', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R']
    s = LogUtils.limitedLengthList(list)

    then: 'it is not truncated'
    !s.contains('more')
    !s.contains('...')

    expect: 'null and empty cases work'
    LogUtils.limitedLengthList(null) == '[]'
    LogUtils.limitedLengthList([]) == '[]'
  }

  def "verify that limitedLengthString works"() {
    when: 'a string is limited'
    def s1 = '1234567890123456789012345678901234567890xxxx'
    def s = LogUtils.limitedLengthString(s1)

    then: 'the string is truncated'
    !s.contains('x')
    s.contains('(4 more chars)')

    expect: 'a short string is not truncated'
    LogUtils.limitedLengthString('ABC') == 'ABC'
  }

  def "verify that limitedLengthString works with passed in length"() {
    when: 'a string is limited'
    def s1 = '12345678901234567890123456789012345678901234567890123456789012345678901234567890xxxx'
    def s = LogUtils.limitedLengthString(s1, 80)

    then: 'the string is truncated'
    !s.contains('x')
  }

  def "verify that logStackTrace works"() {
    given: 'a mock appender to capture the logging message '
    def mockAppender = MockAppender.mock(LogUtils, Level.ERROR)

    and: 'a dummy logger'
    def logger = LoggerFactory.getLogger(LogUtils)

    when: 'an exception is logged'
    def e = new IllegalArgumentException('XYZ')
    LogUtils.logStackTrace(logger, e, 'ABCSource')

    then: 'the message is logged'
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, ['ERROR', 'ABCSource'])

  }

  def "verify that logStackTrace ignores BusinessException"() {
    given: 'a mock appender to capture the logging message'
    def mockAppender = MockAppender.mock(LogUtils, Level.ERROR)

    and: 'a dummy logger'
    def logger = LoggerFactory.getLogger(LogUtils)

    when: 'an exception is logged'
    def e = new BusinessException(109, [])
    LogUtils.logStackTrace(logger, e, 'ABCSource')

    then: 'the message is logged'
    mockAppender.messages.size() == 0
  }

  def "verify that convertLevelToClientLogSetting works"() {
    expect: 'the conversion works'
    LogUtils.convertLevelToClientLogSetting(level) == result

    where:
    level       | result
    Level.DEBUG | 2000
    Level.ERROR | 5000
    Level.INFO  | 3000
    Level.TRACE | 1000
    Level.WARN  | 4000
  }

  def "verify that convertPageToClientLoggerName works"() {
    expect: 'the method works'
    assert LogUtils.convertPageToClientLoggerName(page) == result

    where:
    page          | result
    null          | null
    ''            | null
    '/order'      | 'client.order'
    '/order/show' | 'client.order'
  }

  def "verify that convertClientLevelToServerLevel works"() {
    expect: 'the method works'
    assert LogUtils.convertClientLevelToServerLevel(clientLevel) == result

    where:
    clientLevel || result
    100         || Level.TRACE
    500         || Level.TRACE
    1000        || Level.TRACE
    1500        || Level.DEBUG
    2000        || Level.DEBUG
    3000        || Level.INFO
    4000        || Level.WARN
    5000        || Level.ERROR
    6000        || Level.ERROR
    7000        || Level.ERROR
  }

  def "verify that logClientMessage works"() {
    given: 'a mock appender to capture the logging message'
    def mockAppender = MockAppender.mock('javascript.logging.index', Level.DEBUG)

    when: 'a client message is logged as JSON'
    def jsonMap = [lg: [[t: 1429735795328, l: 2000, m: 'a simple debug message']]]
    LogUtils.logClientMessage(jsonMap, [logger: 'javascript.logging.index'])

    then: 'the line is logged'
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, ['DEBUG', 'a simple debug message'])
  }

  def "verify that logClientMessage does not log when disabled"() {
    given: 'a mock appender to capture the logging message'
    def mockAppender = MockAppender.mock('javascript.logging.index', Level.DEBUG)

    when: 'a client message is logged as JSON for a disabled level'
    def jsonMap = [lg: [[t: 1429735795328, l: 1000, m: 'a simple trace message']]]
    LogUtils.logClientMessage(jsonMap, [logger: 'javascript.logging.index'])

    then: 'the line is not logged'
    mockAppender.messages.size() == 0
  }

  def "verify that logClientMessage works with all supported levels"() {
    given: 'a mock appender to capture the logging message'
    def mockAppender = MockAppender.mock('javascript.logging.index', level)
    def numericLevel = LogUtils.convertLevelToClientLogSetting(level)

    when: 'a client message is logged as JSON'
    def jsonMap = [lg: [[t: 1429735795328, l: numericLevel, m: 'a simple debug message']]]
    LogUtils.logClientMessage(jsonMap, [logger: 'javascript.logging.index'])

    then: 'the line is logged'
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, ['a simple debug message'])

    where:
    level       | _
    Level.TRACE | _
    Level.DEBUG | _
    Level.INFO  | _
    Level.WARN  | _
    Level.ERROR | _

  }

  def "verify that extractImportantMethodsFromStackTrace gives the important methods from the stack trace"() {
    when: 'the method is called'
    def s = LogUtils.extractImportantMethodsFromStackTrace()

    then: 'the simplemes methods are returned in a string'
    s.contains('LogUtils')

    and: 'no other methods are returned'
    !s.contains('(Native Method)')

    and: 'does not contain the extract methods'
    !s.contains('extractImportantMethodsFromStackTrace')
  }
}
