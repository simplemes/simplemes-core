package org.simplemes.eframe.data.format

import groovy.transform.CompileStatic
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.date.ISODate

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines a basic DateOnly field format (no time).
 */
@CompileStatic
class DateOnlyFieldFormat extends BasicFieldFormat {
  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static BasicFieldFormat instance = new DateOnlyFieldFormat()

  /**
   * The internal ID for the format.  Used as a short name in internal (string) representations of this field.
   */
  public static final String ID = 'D'

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
    return DateOnly
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
    return DateUtils.parseDateOnly(value, locale)
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
    if (!value) {
      return ''
    }
    return DateUtils.formatDate((DateOnly) value, locale)
  }

  /**
   * Parses the given value from a string using the form submission format.
   * This might be locale-independent in some cases (e.g. dates).  This depends mostly on the way the GUI
   * toolkit submits the field values.  Most implementation classes will just delegate to the format/parse
   * methods.
   * <p>
   * <b>Note:</b> This sub-class use the locale-independent format.
   * @param value The string encoded value.  Null is allowed.  Empty strings are treated as null.
   * @param locale The locale to parse from (default: The request locale).
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The value.
   */
  @Override
  Object parseForm(String value, Locale locale, FieldDefinitionInterface fieldDefinition) {
    // If the string ends in 00:00:00, then remove it so it is a legal ISO date for parsing.
    value = value - ' 00:00:00'
    // Use the fixed ISO format.
    return decode(value, fieldDefinition)
  }

  /**
   * Formats the given value for uses in an input value in a UI form.
   * This might be locale-independent in some cases (e.g. dates).  This depends mostly on the way the GUI
   * toolkit submits the field values.  Most implementation classes will just delegate to the format/parse
   * methods.
   * <p>
   * <b>Note:</b> This sub-class use the locale-independent format.
   * @param value The object to format for display.  Null is allowed.  Empty strings are treated as null.
   * @param locale The locale to format the value for (default: The request locale).
   * @param fieldDefinition The field definition used to define this field (optional).
   * @return The formatted value.
   */
  @Override
  String formatForm(Object value, Locale locale, FieldDefinitionInterface fieldDefinition) {
    return encode(value, fieldDefinition)
  }

  /**
   * Encodes the given value as a string for internal storage.
   * @param value The value.  Must be typed correctly.  Null is allowed.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The string encoded value.
   */
  @Override
  String encode(Object value, FieldDefinitionInterface fieldDefinition) {
    if (!value) {
      return ''
    }
    if (value instanceof DateOnly) {
      return ISODate.format((DateOnly) value)
    }
    throw new IllegalArgumentException("Invalid field type, value = '$value'(${value.getClass()}). Must be a DateOnly.")
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
    return ISODate.parseDateOnly(encodedString)
  }

  /**
   * Defines the editor to use for this type of field when used in an editable inline grid.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The editor (e.g. 'text').
   */
  @Override
  String getGridEditor(FieldDefinitionInterface fieldDefinition) {
    return 'keyboardEditDate'
  }

  /**
   * Converts the given value to a format suitable for JSON storage.
   * @param value The value object.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The converted value or the original if no conversion is needed.
   */
  @Override
  Object convertToJsonFormat(Object value, FieldDefinitionInterface fieldDefinition) {
    return ISODate.format((DateOnly) value)
  }

  /**
   * Converts the given value from a format suitable for JSON storage.
   * @param value The value object.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The converted value or the original if no conversion is needed.
   */
  @Override
  Object convertFromJsonFormat(Object value, FieldDefinitionInterface fieldDefinition) {
    if (value) {
      return ISODate.parseDateOnly((String) value)
    }
    return null
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  @Override
  String toString() {
    return 'Date'
  }
}
