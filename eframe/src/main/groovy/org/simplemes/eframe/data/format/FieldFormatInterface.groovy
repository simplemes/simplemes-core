package org.simplemes.eframe.data.format

import org.simplemes.eframe.data.ChoiceListItemInterface
import org.simplemes.eframe.data.FieldDefinitionInterface

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the basic field format handling.  Implementations are provided for all of the major field types
 * (e.g. String, BigDecimal, etc).
 * <p>
 *  <b>Note:</b> The FieldDefinitionInterface passed into the format/parse/etc classes is used to provide
 *  additional information needed to process the raw value.  This is only used for more complex types.
 *  Simple types like BigDecimal and Integer don't use the field definition.
 */
interface FieldFormatInterface {

  /**
   * The internal ID for the format.  Used as a short name in internal (string) representations of this field.
   * @return The ID.
   */
  String getId()

  /**
   * The external name for the format.  This is used in APIs and such.
   * @return The name.
   */
  String getName()

  /**
   * Returns the Class that represents this type.
   * @return The type.
   */
  Class getType()

  /**
   * Parses the given value from a string into the correct type for a field.  This method will fail with various format exceptions
   * if the string is not valid for the type.
   * @param value The string encoded value.  Null is allowed.  Empty strings are treated as null.
   * @param locale The locale to parse from (default: The request locale).
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The value.
   */
  Object parse(String value, Locale locale, FieldDefinitionInterface fieldDefinition)

  /**
   * Formats the given value for display as a string into the correct type for a field.
   * @param value The object to format for display.  Null is allowed.  Empty strings are treated as null.
   * @param locale The locale to format the value for (default: The request locale).
   * @param fieldDefinition The field definition used to define this field (optional).
   * @return The formatted value.
   */
  String format(Object value, Locale locale, FieldDefinitionInterface fieldDefinition)

  /**
   * Parses the given value from a string using the form submission format.
   * This might be locale-independent in some cases (e.g. dates).  This depends mostly on the way the GUI
   * toolkit submits the field values.  Most implementation classes will just delegate to the format/parse
   * methods.
   * @param value The string encoded value.  Null is allowed.  Empty strings are treated as null.
   * @param locale The locale to parse from (default: The request locale).
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The value.
   */
  Object parseForm(String value, Locale locale, FieldDefinitionInterface fieldDefinition)

  /**
   * Formats the given value for uses in an input value in a UI form.
   * This might be locale-independent in some cases (e.g. dates).  This depends mostly on the way the GUI
   * toolkit submits the field values.  Most implementation classes will just delegate to the format/parse
   * methods.
   * @param value The object to format for display.  Null is allowed.  Empty strings are treated as null.
   * @param locale The locale to format the value for (default: The request locale).
   * @param fieldDefinition The field definition used to define this field (optional).
   * @return The formatted value.
   */
  String formatForm(Object value, Locale locale, FieldDefinitionInterface fieldDefinition)

  /**
   * Encodes the given value as a string for internal storage.
   * @param value The value.  Must be typed correctly.  Null is allowed.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The string encoded value.
   */
  String encode(Object value, FieldDefinitionInterface fieldDefinition)

  /**
   * Decodes the given string from internal storage into the appropriate type.  This method will fail with various format exceptions
   * if the string is not valid for the type.
   * @param encodedString The string encoded value.  Null is allowed.  Empty strings are treated as null.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The value.
   */
  Object decode(String encodedString, FieldDefinitionInterface fieldDefinition)

  // TODO: Consider moving to a new GridEditorFactory object.
  /**
   * Defines the editor to use for this type of field when used in an editable inline grid.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The editor (e.g. 'text').
   */
  String getGridEditor(FieldDefinitionInterface fieldDefinition)

  /**
   * Returns the list of valid values for those formats that use a combobox or similar widget.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The list of valid values.
   */
  List<ChoiceListItemInterface> getValidValues(FieldDefinitionInterface fieldDefinition)

  /**
   * Converts the given value to a format suitable for JSON storage.
   * @param value The value object.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The converted value or the original if no conversion is needed.
   */
  Object convertToJsonFormat(Object value, FieldDefinitionInterface fieldDefinition)

  /**
   * Converts the given value from a format suitable for JSON storage.
   * @param value The value object.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The converted value or the original if no conversion is needed.
   */
  Object convertFromJsonFormat(Object value, FieldDefinitionInterface fieldDefinition)

}
