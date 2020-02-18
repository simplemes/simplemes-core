/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data.format


import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.domain.annotation.DomainEntityHelper
import org.simplemes.eframe.domain.annotation.DomainEntityInterface

/**
 * Defines the format for a field that is list of custom child records.  These are domain records that are loosely
 * coupled with the parent object (via a record ID).
 */
class CustomChildListFieldFormat extends BasicFieldFormat implements ListFieldLoaderInterface {
  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static BasicFieldFormat instance = new CustomChildListFieldFormat()

  /**
   * The internal ID for the format.  Used as a short name in internal (string) representations of this field.
   */
  public static final String ID = 'K'

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
   * Gets the given field value from the given object (domain or POGO depending on sub-class).
   *
   * @param object The domain object the field is stored in.
   * @param fieldDefinition The field definition used to define this field (<b>Required</b>).
   * @return The list.
   */
  @Override
  List readList(DomainEntityInterface object, FieldDefinitionInterface fieldDefinition) {
    def domainClass = object.getClass()
    return DomainEntityHelper.instance.loadChildRecords(object, fieldDefinition.referenceType, domainClass.simpleName)
  }

  /**
   * Saves the list field values to the DB.  Relies on the save() mechanism and dirty
   * checking for the save.
   *
   * @param object The domain object the field is to be stored in.
   * @param list The field list.
   * @param fieldDefinition The field definition used to define this field (<b>Required</b>).
   */
  @Override
  void saveList(DomainEntityInterface object, List list, FieldDefinitionInterface fieldDefinition) {
    for (record in list) {
      record.save()
    }
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  @Override
  String toString() {
    return 'CustomChildList'
  }
}
