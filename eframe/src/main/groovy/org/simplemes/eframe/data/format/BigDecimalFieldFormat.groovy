package org.simplemes.eframe.data.format

import groovy.transform.CompileStatic
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.i18n.GlobalUtils

import java.text.DecimalFormat

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines a basic BigDecimal field format.
 */
@CompileStatic
class BigDecimalFieldFormat extends BasicFieldFormat {
  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static BasicFieldFormat instance = new BigDecimalFieldFormat()

  /**
   * The internal ID for the format.  Used as a short name in internal (string) representations of this field.
   */
  public static final String ID = 'N'

  /**
   * The internal ID for the format.  Used as a short name in internal (string) representations of this field.
   */
  @Override
  String getId() {
    return ID
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
    def nf = DecimalFormat.getInstance(GlobalUtils.getRequestLocale(locale)) as DecimalFormat
    nf.setGroupingUsed(true)  // Always allow grouping (thousands) when parsing.
    nf.setParseBigDecimal(true)
    return nf.parse(value)
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
    def nf = DecimalFormat.getInstance(GlobalUtils.getRequestLocale(locale)) as DecimalFormat
    nf.setGroupingUsed(false)
    return nf.format(value)
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
    if (value instanceof Number) {
      return value.toString()
    }
    throw new IllegalArgumentException("Invalid field type, value = '$value'(${value.getClass()}). Must be a Number.")
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
    return new BigDecimal(encodedString)
  }

  /**
   * The Class that represents this type.
   */
  @Override
  Class getType() {
    return BigDecimal
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  @Override
  String toString() {
    return 'Decimal'
  }
}
