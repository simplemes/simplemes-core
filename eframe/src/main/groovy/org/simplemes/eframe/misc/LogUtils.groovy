package org.simplemes.eframe.misc

import ch.qos.logback.classic.Level
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.system.controller.LoggingController
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * Utility methods for logging.  Provides static methods to handle common logging tasks.  Includes converting possibly long
 * lists of domain objects to limited strings.  Also has support for client side logging features.
 * <p/>
 * Original Author: mph
 *
 */
class LogUtils {

  /**
   * The maximum elements to be printed in the string representation of a list. (<b>Value</b>: 20)
   */
  public static final int MAX_ELEMENTS = 20

  /**
   * The maximum size of an element to be printed in the string representation of a list. (<b>Value</b>: 40)
   */
  public static final int MAX_ELEMENT_SIZE = 40

  /**
   * Build a string representation of the list of objects with limits on the size of each object and number of objects.
   * If the string or count exceeds the limits, place holders are printed (... and '10 more...').
   * @param list The list of objects to print strings for.
   * @return The string representation (with limits).
   */
  static String limitedLengthList(List list) {
    StringBuilder sb = new StringBuilder()
    sb << '['
    if (list) {
      //noinspection GroovyAssignabilityCheck
      for (int i = 0; i < Math.min(MAX_ELEMENTS, list.size()); i++) {
        if (i > 0) {
          sb << ', '
        }
        def s = list[i]?.toString()
        sb << limitedLengthString(s)
      }
      if (list.size() > MAX_ELEMENTS) {
        sb << ", ${list.size() - MAX_ELEMENTS} more..."
      }
    }

    sb << ']'

    return sb.toString()
  }

  /**
   * Trims the given string if it exceeds the MAX_ELEMENT_SIZE (40).
   * @param s The string to (maybe) trim.
   * @return The possibly trimmed string.  Notes how many characters were trimmed.
   */
  static String limitedLengthString(String s) {
    return limitedLengthString(s, MAX_ELEMENT_SIZE)
  }

  /**
   * Trims the given string if it exceeds the MAX_ELEMENT_SIZE (default: 40).
   * @param s The string to (maybe) trim.
   * @param maxLength The max length.
   * @return The possibly trimmed string.  Notes how many characters were trimmed.
   */
  static String limitedLengthString(String s, int maxLength) {
    if (s?.length() > maxLength) {
      int choppedCount = s?.length() - maxLength
      s = s[0..(maxLength - 1)] + "..." + "(${choppedCount} more chars)"
    }
    return s
  }

  /**
   * If true, then the stack trace logging is disabled.  Only use for unit testing.
   */
  static boolean disableStackTraceLogging = false

  /**
   * Logs a given stack trace for safety.  Does not log BusinessException since those are mostly expected exceptions.
   * @param logger The logger to use.
   * @param e The exception.
   * @param source The source (optional).  Logged with the message.
   */
  static void logStackTrace(Logger logger, Throwable e, Object source) {
    // Log to stack trace log to be safe.
    if (!(e instanceof BusinessException) && !disableStackTraceLogging) {
      logger.error("Exception for ${source ?: ''}", e)
    } else {
      // Not logged (yet), so check for the StackTrace logger.
      def stLogger = getLogger('StackTrace')
      stLogger.debug("Exception for ${source ?: ''}", e)
    }
  }

  /**
   * Maps the client logging framework's level number ranges with the server-side levels.
   */
  static Map<Level, Range> clientLevelMap = [
    (Level.TRACE): 0..1000,
    (Level.DEBUG): 1001..2000,
    (Level.INFO) : 2001..3000,
    (Level.WARN) : 3001..4000,
    (Level.ERROR): 4001..5000
  ]

  /**
   * Converts a server-side trace level to a client-side trace level number.  (e.g. Level.ERROR -> 5000).
   * @param level The server-side level.
   * @return The client-side trace level.
   */
  static Integer convertLevelToClientLogSetting(Level level) {
    def range = clientLevelMap[level]
    if (range) {
      return (Integer) range.to
    }
    // Not found, so just use a low level.
    return 1000
  }

  /**
   * Converts a client-side trace level number to a server-side trace level.  (e.g. 2000 -> Level.DEBUG).
   * If a client level is not found, then the next lowest value is returned.
   * @param level The client-side level number.
   * @return The server-side trace level.
   */
  static Level convertClientLevelToServerLevel(Integer clientLevel) {
    for (level in clientLevelMap.keySet()) {
      if (clientLevelMap[level].contains(clientLevel)) {
        return level
      }
    }

    // Client has a level of 6000-FATAL
    if (clientLevel >= 5000) {
      return Level.ERROR
    }

    // Still not found, then fall back to WARN
    return Level.WARN
  }

  /**
   * Converts a page URI to a server-side logger name for use with client-side logging.  For example,
   * converts '/order/show' to 'client.order.show'.
   * @param page The URI of the page to convert to a logger name.
   * @return The client-side logger name.
   */
  static String convertPageToClientLoggerName(String page) {
    if (!page) {
      return null
    }
    // Grab the root page
    def elements = page.tokenize('/')
    return "${LoggingController.CLIENT_PREFIX}.${elements[0]}"
  }

  /**
   * Logs the given client-side message to the standard logger.  The input JSON is provided by the client logging
   * framework.
   * @param json The content of the logging request from the client framework.  This contains an array ('lg') or maps.
   *        The maps contain two important elements: 'l' (level as int), 'm' (the message text).
   * @param params The HTTP request parameters (uses 'logger' as the name of the server-side logger).
   * @return The client-side logger name.
   */
  static void logClientMessage(Map json, Map params) {
    def loggerName = params.logger
    def logger = getLogger((String) loggerName)

    if (json) {
      def remoteIP = json.remoteIP ? " ip=$json.remoteIP" : ''
      for (msg in json?.lg) {
        def level = convertClientLevelToServerLevel((Integer) msg.l)
        if (level.isGreaterOrEqual((Level) logger.effectiveLevel)) {
          log(logger, level, msg.m + remoteIP)
        }
      }
    }
  }

  /**
   * Logs a message for a variable level.
   * @param logger The logger
   * @param level The level to log the message at.
   * @param msg The message.
   */
  private static void log(Object logger, Level level, Object msg) {
    //println "log1 = $logger"
    switch (level) {
      case Level.TRACE:
        logger.trace(msg)
        break
      case Level.DEBUG:
        logger.debug(msg)
        break
      case Level.INFO:
        logger.info(msg)
        break
      case Level.WARN:
        logger.warn(msg)
        break
    //default:
      case Level.ERROR:
        logger.error(msg)
        break
    }
  }

  /**
   * Finds the logger for the given logging ID (e.g. package/class name).
   * @param loggerID The logger ID (full class name).
   */
  static Logger getLogger(String loggerID) {
    return LoggerFactory.getLogger(loggerID)
  }

  /**
   * Finds the logger for the given class.
   * @param clazz The class.
   */
  static Logger getLogger(Class clazz) {
    return LoggerFactory.getLogger(clazz)
  }

  /**
   * Converts the given string Level to the correct logger Level.
   * @param level The level string to convert.
   */
  static Object toLevel(String level) {
    return Level.toLevel(level)
  }

  static String extractImportantMethodsFromStackTrace() {
    def ex = new IllegalArgumentException('none')
    def sb = new StringBuilder()
    def list = ex.stackTrace
    for (m in list) {
      def s = m.toString()
      if (s.startsWith('org.simplemes') && !s.contains('extractImportantMethodsFromStackTrace')) {
        sb.append(' ').append(s).append('\n')
      }
    }
    return sb.toString()
  }
}
