package org.simplemes.eframe.exception
/**
 * Generic business exception that is based on error numbers.  These are typically triggered by
 * business logic and do not have special processing.  Just an error code, parameters and a string.
 * <p/>
 * This exception is used to avoid creating a large number of Exception classes.  If the exception
 * generally is used to transport an error message to some user, then this class can be used.
 * Methods are provided to create a translated message string.
 * <p/>
 * The error messages should exist in the standard messages.properties file with the key format:
 * <pre>
 *   error.1001.message=Order {0} cannot be worked.  It has a status of {1}* </pre>
 * <p>
 * <b>Note:</b> These messages are typically not logged to the server logs.
 */
class BusinessException extends MessageBasedException {

  /**
   * Constructors.
   */
  BusinessException() {
  }

  /**
   * Constructors.
   */
  BusinessException(Map map) {
    super(map)
  }

  /**
   * Constructors.
   */
  BusinessException(int code, List params) {
    super(code, params)
  }
}
