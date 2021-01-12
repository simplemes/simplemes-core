/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.Appender
import ch.qos.logback.core.AppenderBase
import ch.qos.logback.core.ConsoleAppender
import org.slf4j.LoggerFactory

/**
 * A Mock appender for the logback logging system.  Used to collect messages logged from a class under test.
 * This approach does not require changes to the logback.groovy file or use of a non-final log instance.
 * <p>
 * This mock works well with the @Slf4j Groovy annotation.
 * <p>
 * <b>Note:</b> The <code>cleanup()</code> method must be called to clean up any mock appenders you create.
 *
 * <h3>Example</h3>
 * <pre>
 * void cleanup() &#123;
 *   MockAppender.cleanup()
 * &#125;
 *
 * def "test info logging"() &#123;
 *   given: 'a mock appender'
 *   def mockAppender = MockAppender.mock(MockAppenderTestClass)
 *
 *   when: 'a method logs a message'
 *   ClassUnderTest.doInfo('ABC')
 *
 *   then: 'a single message is logged with the correct details'
 *   mockAppender.assertMessageIsValid(['[INFO]','some Text ABC'])
 * &#125;
 * <pre>
 *
 * <b>Note:</b> This class attempts to suppress the standard console output.  This only works with the default logback.groovy
 *         setup as shown below.
 *
 * <pre>
 * appender('STDOUT', ConsoleAppender) &#123;
 *   . . .
 * &#125;
 * root(ERROR, ['STDOUT'])
 * </pre>
 *
 */
class MockAppender extends AppenderBase {

  /**
   * The list of classes that have been mocked since the last cleanup.
   * cleanup() will undo the mocking for these classes by setting the level to the original value.
   */
  static List<Map> mockedLoggers = []

  /**
   * The original output stream used by the default console appender.  This is temporarily replaced
   * by a byte output stream to prevent display to the console.  The original stream is restored by the cleanup() code.
   */
  static originalOutputStream

  /**
   * The messages logged since the last cleanup.
   */
  List<String> messages = []

  /**
   * The level to filter log messages by.  Null means no filtering.
   */
  Level filterLevel

  /**
   * Returns the first message.  Null if no messages.
   * @return The message.
   */
  String getMessage() {
    if (messages) {
      return messages[0]
    }
    return null
  }

  /**
   * Checks the given message to verify it contains all of the values (case ignored) and
   * has all parameters filled in.  Throws an assertion exception if not value.
   * @param values The list of strings to look for.
   * @param messageIndex The message to check (default: 0).
   * @return true - To support use in a Spock then/expect section.
   */
  boolean assertMessageIsValid(List<String> values, int messageIndex = 0) {
    def message = messages[messageIndex]
    assert message, "no message[$messageIndex] found in MockAppender log messages '${messages}'"
    def testStringLC = message.toLowerCase()
    for (s in values) {
      assert testStringLC.contains(s.toLowerCase())
    }

    // Remove some common custom fields used in JSON.  JSON with empty custom fields
    // results in the string '{}' in the messages sometimes.  Attempt to remove them so the
    // check for missing parameters will not fail.
    testStringLC = testStringLC.replaceAll('lds\":\\{\\}', '')

    // Check for missing parameters in messages.
    assert !testStringLC.contains('{}'), "'${testStringLC}' is missing a replaceable parameter"

    return true
  }

  /**
   * Finds the first log message that contains the given search string.  Then verifies that it is valid.
   * @param searchString Used to find the right message to test.
   * @param values The list of strings to look for.
   * @return true - To support use in a Spock then/expect section.
   */
  boolean assertFirstFoundMessageIsValid(String searchString, List<String> values) {
    def message = messages.find() { it.contains(searchString) }
    assert message, "Could not find '$searchString' in log messsages $messages"
    assert UnitTestUtils.assertContainsAllIgnoreCase(message, values)
    assert !message.contains('{}'), "'${message}' is missing a replaceable parameter"
    return true
  }

  /**
   * Unused.
   * @param eventObject
   */
  @Override
  protected void append(Object eventObject) {
  }

  /**
   * The appender.  Collects all messages logged for later use by a unit test.
   * @param eventObject
   */
  @SuppressWarnings('SynchronizedMethod')
  @Override
  synchronized void doAppend(Object eventObject) {
    def message = eventObject.toString()
    if (eventObject.hasProperty('throwableProxy')) {
      def throwable = eventObject.throwableProxy
      if (throwable?.throwable) {
        message += ". Caused by ${throwable?.throwable}"
      }
    }
    if (filterLevel == null) {
      // Record all log messages
      messages << message
    } else if (filterLevel == eventObject.level) {
      messages << message
    }
    super.doAppend(eventObject)
  }

  /**
   * Creates a mock appender and attaches it to the logger for the given class.
   * This also sets the level to TRACE.
   * <p>
   * Note: The {@link BaseSpecification} will automatically clean up this mock appender.
   * @param clazz The class.
   * @param filterLevel The level to filter the messages recorded.
   * @return The MockAppender.
   */
  static MockAppender mock(Class clazz, Level filterLevel = null) {
    return mock(clazz.name, filterLevel)
  }

  /**
   * Creates a mock appender and attaches it to the logger for the given class.
   * This also sets the level to TRACE.
   * <p>
   * Note: The {@link BaseSpecification} will automatically clean up this mock appender.
   * @param clazz The class.
   * @param filterLevel The level to filter the messages recorded.
   * @return The MockAppender.
   */
  static MockAppender mock(String loggerName, Level filterLevel = null) {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.ILoggerFactory
    //println "${clazz.simpleName} loggerContext = ${loggerContext.class}"

    // Suppress the console output if the normal default console appender for root is used.
    // This reduces console output during unit tests.
    def consoleAppender = loggerContext.getLogger("root").getAppender("STDOUT")
    if (consoleAppender instanceof ConsoleAppender && !(consoleAppender.outputStream instanceof ByteArrayOutputStream)) {
      //println "consoleAppender = $consoleAppender, target = ${consoleAppender.target}, output = ${consoleAppender.outputStream.class}"
      if (originalOutputStream == null) {
        originalOutputStream = consoleAppender.outputStream
      }
      consoleAppender.outputStream = new ByteArrayOutputStream(2000)
    }

    def appender = new MockAppender()
    appender.filterLevel = filterLevel
    appender.setContext(loggerContext)
    appender.setName('_MockAppender')
    def logger = loggerContext.getLogger(loggerName)
    def originalLevel = logger.level
    logger.setLevel(Level.TRACE)
    logger.addAppender(appender)

    mockedLoggers << [clazz: loggerName, level: originalLevel, appender: appender]

    return appender
  }

  /**
   * Cleans up all MockAppenders and resets the log level to the original value.
   * This is designed to be called in the test's <code>cleanup()</code> method.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  static void cleanup() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.ILoggerFactory

    for (mock in mockedLoggers) {
      loggerContext.getLogger(mock.clazz).setLevel(mock.level)
      loggerContext.getLogger(mock.clazz).detachAppender((Appender) mock.appender)
    }

    mockedLoggers = []

    /**
     * Now, restore the original console output if we replaced it with out dummy output stream.
     */
    //def logger = loggerContext.getLogger(this.getClass())
    //logger.error('test error1')
    if (originalOutputStream) {
      def appender1 = loggerContext.getLogger("root").getAppender("STDOUT")
      if (appender1 instanceof ConsoleAppender) {
        appender1.setOutputStream(originalOutputStream)
      }
      originalOutputStream = null
      //logger.error('test error2')
    }
  }

  /**
   * Clears any messages for this appender.
   */
  void clearMessages() {
    messages = []
  }

  String toString() {
    return """MockAppender: Level: $filterLevel, Messages: $messages"""
  }
}
