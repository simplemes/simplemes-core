/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data.format

import org.simplemes.eframe.data.ChoiceListItemInterface
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.data.SimpleChoiceListItem
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.TypeUtils

/**
 * Defines the format for a field that is a list domain references to a foreign domain objects.
 */
//@CompileStatic
class DomainRefListFieldFormat extends BasicFieldFormat {
  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static BasicFieldFormat instance = new DomainRefListFieldFormat()

  /**
   * The internal ID for the format.  Used as a short name in internal (string) representations of this field.
   */
  public static final String ID = 'Q'

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
    return List
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
    def res = []
    def idS = value.tokenize(', ')
    for (s in idS) {
      def domainClass = fieldDefinition.referenceType
      def id = UUID.fromString(s)
      res << domainClass.findByUuid(id)
    }
    return res
  }

  /**
   * Formats the given value for display as a string into the correct type for a field.
   * This formats the display value for each element in the list.
   * @param value The object to format for display.  Null is allowed.  Empty strings are treated as null.
   * @param locale The locale to format the value for (default: The request locale).
   * @param fieldDefinition The field definition used to define this field (optional).
   * @return The formatted value.
   */
  @Override
  String format(Object value, Locale locale, FieldDefinitionInterface fieldDefinition) {
    def sb = new StringBuilder()
    for (o in value) {
      if (sb) {
        sb << ", "
      }
      sb << TypeUtils.toShortString(o) ?: ''
    }
    return sb.toString()
  }

  /**
   * Encodes the given value as a string for internal storage.
   * @param value The value.  Must be typed correctly.  Null is allowed.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The string encoded value.
   */
  @Override
  String encode(Object value, FieldDefinitionInterface fieldDefinition) {
    def sb = new StringBuilder()
    for (o in value) {
      if (sb) {
        sb << ","
      }
      sb << o.uuid
    }
    return sb.toString()
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
   * Returns the list of valid values for those formats that use a combobox or similar widget.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The list of valid values.
   */
  @Override
  List<ChoiceListItemInterface> getValidValues(FieldDefinitionInterface fieldDefinition) {
    ArgumentUtils.checkMissing(fieldDefinition, 'fieldDefinitions')

    List<ChoiceListItemInterface> res = []
    def referencedClass = fieldDefinition.referenceType
    def key = DomainUtils.instance.getPrimaryKeyField(referencedClass)
    referencedClass.withTransaction {
      def list = referencedClass.list()
      if (key) {
        // Sort by the primary key
        list = list.sort { a, b -> a[key] <=> b[key] }
      } else {
        // No primary key, so sort by the display value.
        list = list.sort { a, b -> TypeUtils.toShortString(a, false) <=> TypeUtils.toShortString(b, false) }
      }
      for (record in list) {
        res << new SimpleChoiceListItem(id: record.uuid, displayValue: TypeUtils.toShortString(record, false))
      }
    }
    return res
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  @Override
  String toString() {
    return 'DomainRefList'
  }
}
