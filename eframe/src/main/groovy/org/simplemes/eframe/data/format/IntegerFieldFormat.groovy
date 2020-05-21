/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data.format

import groovy.transform.CompileStatic
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.misc.NumberUtils

/**
 * Defines a basic Integer field format.
 */
@CompileStatic
class IntegerFieldFormat extends BasicFieldFormat {
  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static BasicFieldFormat instance = new IntegerFieldFormat()

  /**
   * The internal ID for the format.  Used as a short name in internal (string) representations of this field.
   */
  public static final String ID = 'I'

  /**
   * The internal ID for the format.  Used as a short name in internal (string) representations of this field.
   */
  @Override
  String getId() {
    return ID
  }

  /**
   * The Class that represents this type.
   */
  @Override
  Class getType() {
    return Integer
  }

  /**
   * Parses the given value from a string into the correct type for a field.  This method will fail with various format exceptions
   * if the string is not valid for the type.
   * @param value The string encoded value.  Null is allowed.  Empty strings are treated as null.
   * @param locale The locale to parse from (default: The request locale).
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The value.
   */
  @Override
  Object parse(String value, Locale locale, FieldDefinitionInterface fieldDefinition) {
    if (!value) {
      return null
    }
    value = value.replace(NumberUtils.determineGroupingSeparator(locale), '')
    return Integer.parseInt(value)
  }

  /**
   * Formats the given value for display as a string into the correct type for a field.
   * @param value The object to format for display.  Null is allowed.  Empty strings are treated as null.
   * @param locale The locale to format the value for (default: The request locale).
   * @param fieldDefinition The field definition used to define this field (optional).
   * @return The formatted value.
   */
  @Override
  String format(Object value, Locale locale, FieldDefinitionInterface fieldDefinition) {
    if (value == null) {
      return ''
    }
    return value.toString()
  }

  /**
   * Encodes the given value as a string for internal storage.
   * @param value The value.  Must be typed correctly.  Null is allowed.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The string encoded value.
   */
  @Override
  String encode(Object value, FieldDefinitionInterface fieldDefinition) {
    if (value == null) {
      return ''
    }
    if (value instanceof Integer) {
      return value.toString()
    }
    throw new IllegalArgumentException("Invalid field type, value = '$value'(${value.getClass()}). Must be an Integer.")
  }

  /**
   * Decodes the given string from internal storage into the appropriate type.  This method will fail with various format exceptions
   * if the string is not valid for the type.
   * @param encodedString The string encoded value.  Null is allowed.  Empty strings are treated as null.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The value.
   */
  @Override
  Object decode(String encodedString, FieldDefinitionInterface fieldDefinition) {
    if (!encodedString) {
      return null
    }
    return Integer.parseInt(encodedString)
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  @Override
  String toString() {
    return 'Integer'
  }
}
