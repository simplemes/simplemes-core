package org.simplemes.eframe.exception


import org.simplemes.eframe.i18n.GlobalUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A general-purpose message exception that uses error numbers and messages from the .properties file
 * to localize the message.  These are just an error code, parameters and a string.
 * <p/>
 * This exception is used to avoid creating a large number of Exception classes.  If the exception
 * generally is used to transport an error message to some user, then this class can be used.
 * Methods are provided to create a translated message string.
 * <p/>
 * The error messages should exist in the standard messages.properties file with the key format:
 * <pre>
 *   error.1001.message=Order {0} cannot be worked.  It has a status of {1}* </pre>
 *
 * <b>Note:</b> These messages are typically logged to the server logs.
 *
 */
class MessageBasedException extends RuntimeException {
  /**
   * The error code.  Used to find the error in the message bundle.
   */

  int code

  /**
   * The parameters needed for the message.
   */
  List params

  /**
   * The empty constructor.  Used by sub-classes.
   */
  MessageBasedException() {
  }

  /**
   * The map constructor.  Assigns values to elements from the Map
   * @param map The map containing the field values.
   */
  MessageBasedException(Map map) {
    for (entry in map) {
      this."${entry.key}" = entry.value
    }
  }

  /**
   * Normal constructor.  Expected error code and optional parameters.
   * @param code The error code.
   * @param params The parameters (a list).
   */
  MessageBasedException(int code, List params) {
    this.code = code
    this.params = params
    //println "params = $params"
  }

  /**
   * Format the exception as a string.
   * @return The formatted message.
   */
  String toString() {
    try {
      return toStringLocalized()
    } catch (Throwable ex) {
      // Needed to avoid stack overflow if an exception occurs in the toStringLocalized() method.
      return "Exception occurred in toString() of this exception.  Exception ${ex?.getClass()}, stack trace = ${ex.stackTrace}"
    }
  }

  /**
   * Build a human-readable version of this exception.
   * @param locale The locale to display the enum display text.
   * @return The human-readable string.
   */
  String toStringLocalized(Locale locale = null) {
    return GlobalUtils.lookup("error.${code}.message", locale, params as Object[]) + " (${code})"
  }

  /**
   * Returns the detail message string of this throwable.
   *
   * @return the detail message string of this {@code Throwable} instance
   *          (which may be {@code null}).
   */
  @Override
  String getMessage() {
    return toString()
  }

}
