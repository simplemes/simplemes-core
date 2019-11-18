package org.simplemes.eframe.misc

import groovy.transform.CompileStatic

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *  Convenience methods for handling arguments in the grails/groovy world.
 * <p/>
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
 */
class ArgumentUtils {

  /**
   * Throws an IllegalArgumentException if the given value is null.  Suitable for use in most methods that need to prevent
   * null value processing.  For example:
   * <pre>
   *  ArgumentUtils.checkMissing(toObject,'toObject')
   *  ArgumentUtils.checkMissing(xml,'xml')
   * </pre>
   *
   * @param value The value to check.
   * @param name The argument's name (used for the exception message).
   */
  @CompileStatic
  static void checkMissing(Object value, String name) {
    if (value == null) {
      throw new IllegalArgumentException("Null ${name} not allowed")
    }
  }

  /**
   * Check the given POGO for existing properties with the given name(s).  If any are missing
   * then this throws an IllegalArgumentException.
   * @param param The POGO that needs the given properties.
   * @param propertyNames The properties to check for.
   */
  static void checkForProperties(param, List propertyNames) {
    if (!param) {
      throw new IllegalArgumentException("param is null")
    }
    for (p in propertyNames) {
      if (!param.properties.keySet().contains(p)) {
        throw new IllegalArgumentException("parameter ${param} does not contain property ${p}")
      }
    }
  }

  /**
   * Safely convert to BigDecimal if needed and return the value.
   * @param value The value (String or BigDecimal supported).
   * @return The converted value.  Can be null if input is null or blank.
   */
  static BigDecimal convertToBigDecimal(Object value) {
    if (value instanceof String) {
      // Handle blank string '  '
      value = value.trim()
    }
    if (!value) {
      return null
    }
    if (value instanceof BigDecimal) {
      return value
    } else if (value instanceof String) {
      return new BigDecimal((String) value)
    } else {
      // Any other object, convert to a string then try to use it as a number.
      return new BigDecimal(value.toString())
    }
  }

  /**
   * Safely convert to Integer if needed and return the value.
   * @param value The value (String or Integer supported).
   * @return The converted value.  Can be null if input is null or blank.
   */
  static Integer convertToInteger(Object value) {
    if (value instanceof String) {
      // Handle blank string '  '
      value = value.trim()
    }
    if (!value) {
      return null
    }
    if (value instanceof Integer) {
      return value
    } else if (value instanceof String) {
      return new Integer((String) value)
    } else {
      // Any other object, convert to a string then try to use it as a number.
      return new Integer(value.toString())
    }
  }
  /**
   * Gets and converts the given value to an integer.
   * @param params The params to get the value from.
   * @param paramName The name of the parameter to get the value from.
   */
  static def getInteger(LinkedHashMap<Object, Object> params, String paramName) {
    def v = params?.get(paramName)
    if (v instanceof Integer) {
      return v
    }
    if (v != null) {
      return Integer.valueOf(v.toString())
    }
    return null
  }
}
