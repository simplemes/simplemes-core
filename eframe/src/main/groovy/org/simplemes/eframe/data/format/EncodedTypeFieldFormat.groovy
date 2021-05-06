package org.simplemes.eframe.data.format

import org.simplemes.eframe.data.ChoiceListItemInterface
import org.simplemes.eframe.data.EncodedTypeListUtils
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.misc.ArgumentUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the format for a field that is a single encoded type value.
 */
//@CompileStatic
class EncodedTypeFieldFormat extends BasicFieldFormat {
  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static BasicFieldFormat instance = new EncodedTypeFieldFormat()

  /**
   * The internal ID for the format.  Used as a short name in internal (string) representations of this field.
   */
  public static final String ID = 'Y'

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
    ArgumentUtils.checkMissing(fieldDefinition, 'fieldDefinitions')
    if (!value) {
      return null
    }
    def choices = EncodedTypeListUtils.instance.getAllValues(fieldDefinition.type)
    return choices.find { ((ChoiceListItemInterface) it).id == value }
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
    if (value instanceof ChoiceListItemInterface) {
      ChoiceListItemInterface encodedValue = (ChoiceListItemInterface) value
      return encodedValue?.toStringLocalized(locale) ?: ''
    } else {
      return value?.toString() ?: ''
    }
  }

  /**
   * Encodes the given value as a string for internal storage.
   * @param value The value.  Must be typed correctly.  Null is allowed.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The string encoded value.
   */
  @Override
  String encode(Object value, FieldDefinitionInterface fieldDefinition) {
    return value?.id
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
    return parse(encodedString, null, fieldDefinition)
  }

  /**
   * Defines the editor to use for this type of field when used in an editable inline grid.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The editor (e.g. 'text').
   */
  @Override
  String getGridEditor(FieldDefinitionInterface fieldDefinition) {
    return 'combo'
  }

  /**
   * Returns the list of valid values for those formats that use a combobox or similar widget.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The list of valid values.
   */
  @Override
  List<ChoiceListItemInterface> getValidValues(FieldDefinitionInterface fieldDefinition) {
    ArgumentUtils.checkMissing(fieldDefinition, 'fieldDefinition')
    return EncodedTypeListUtils.instance.getAllValues(fieldDefinition.type)
  }

  /**
   * Converts the given value to a format suitable for JSON storage.
   * @param value The value object.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The converted value or the original if no conversion is needed.
   */
  @Override
  Object convertToJsonFormat(Object value, FieldDefinitionInterface fieldDefinition) {
    return encode(value, fieldDefinition)
  }

  /**
   * Converts the given value from a format suitable for JSON storage.
   * @param value The value object.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The converted value or the original if no conversion is needed.
   */
  @Override
  Object convertFromJsonFormat(Object value, FieldDefinitionInterface fieldDefinition) {
    return decode((String) value, fieldDefinition)
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
    return 'EncodedType'
  }
}
