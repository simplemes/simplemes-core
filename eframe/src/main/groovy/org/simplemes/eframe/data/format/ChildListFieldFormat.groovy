package org.simplemes.eframe.data.format

import org.simplemes.eframe.data.FieldDefinitionInterface

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the format for a field that is list of child records.
 */
//@CompileStatic
class ChildListFieldFormat extends BasicFieldFormat {
  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static BasicFieldFormat instance = new ChildListFieldFormat()

  /**
   * The internal ID for the format.  Used as a short name in internal (string) representations of this field.
   */
  public static final String ID = 'C'

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
    throw new UnsupportedOperationException('Simple parsing for child list is not supported.')
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
    throw new UnsupportedOperationException('Simple formatting for child list is not supported.')
  }

  /**
   * Encodes the given value as a string for internal storage.
   * @param value The value.  Must be typed correctly.  Null is allowed.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The string encoded value.
   */
  @Override
  String encode(Object value, FieldDefinitionInterface fieldDefinition) {
    throw new UnsupportedOperationException('Simple formatting for child list is not supported.')
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
    throw new UnsupportedOperationException('Simple parsing for child list is not supported.')
  }

  /**
   * Returns the client format type code.  Used by the Vue client logic only.
   * @return The client code.
   */
  @Override
  String getClientFormatType() {
    // This is treated as list of valid choices on the client.
    return EnumFieldFormat.instance.id
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  @Override
  String toString() {
    return 'ChildList'
  }
}
