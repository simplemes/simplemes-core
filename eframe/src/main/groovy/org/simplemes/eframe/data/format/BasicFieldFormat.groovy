package org.simplemes.eframe.data.format

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import org.simplemes.eframe.data.ChoiceListInterface
import org.simplemes.eframe.data.ChoiceListItemInterface
import org.simplemes.eframe.data.EncodedTypeInterface
import org.simplemes.eframe.data.EncodedTypeListUtils
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.NameUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the common base field format features.
 * <p>
 * This class covers two aspects of the encoded types/choice lists: 1) Defines a format that specifies
 * the values.  2) Also as a possible choice in a combobox.
 * <p>
 * Some sub-classes provide their own lists of valid values for the GUI.
 *
 */
@CompileStatic
@EqualsAndHashCode(includes = ['id'])
abstract class BasicFieldFormat implements FieldFormatInterface, EncodedTypeInterface,
  ChoiceListInterface, ChoiceListItemInterface {

  /**
   * The external name for the format.  This is used in APIs and such.
   */
  String name

  /**
   * The internal ID for the format.  Used as a short name in internal (string) representations of this field.
   */
  String id

  /**
   * The Class that represents this type.
   */
  Class type

  /**
   * A list of classes that are valid Basic Statuses.
   */
  static List coreValues = [StringFieldFormat, IntegerFieldFormat,
                            LongFieldFormat, BigDecimalFieldFormat,
                            BooleanFieldFormat, DateOnlyFieldFormat,
                            DateFieldFormat, DomainReferenceFieldFormat,
                            EnumFieldFormat, EncodedTypeFieldFormat,
                            ChildListFieldFormat, CustomChildListFieldFormat,
                            DomainRefListFieldFormat, ConfigurableTypeDomainFormat]

  /**
   * Returns the instance for the given DB ID.
   * @param id The ID (e.g. 'S')
   * @return The corresponding status (can be null if ID is not valid or null).
   */
  @SuppressWarnings("UnnecessaryQualifiedReference")
  static BasicFieldFormat valueOf(String id) {
    BasicFieldFormat entry = (BasicFieldFormat) EncodedTypeListUtils.instance.getAllValues(this).find {
      def ff = (BasicFieldFormat) it
      ff.id == id
    }
    return entry
  }

  static BasicFieldFormat instance

  /**
   * Returns a singleton instance of this format.
   * @return The instance.
   */
  static BasicFieldFormat getInstance() {
    // This is not used.  Clover/groovy do not work well with abstract static methods.
    // Results in ClassFormatError: Method getInstance in class org/simplemes/eframe/data/format/BasicFieldFormat has illegal modifiers: 0x409
    return null
  }


  /**
   * Parses the given value from a string using the form submission format.
   * This might be locale-independent in some cases (e.g. dates).  This depends mostly on the way the GUI
   * toolkit submits the field values.  Most implementation classes will just delegate to the format/parse
   * methods.
   * <p>
   * <b>Note:</b> This base class just uses the format()/parse() methods for the form formatting.
   * @param value The string encoded value.  Null is allowed.  Empty strings are treated as null.
   * @param locale The locale to parse from (default: The request locale).
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The value.
   */
  @Override
  Object parseForm(String value, Locale locale, FieldDefinitionInterface fieldDefinition) {
    return parse(value, locale, fieldDefinition)
  }

  /**
   * Formats the given value for uses in an input value in a UI form.
   * This might be locale-independent in some cases (e.g. dates).  This depends mostly on the way the GUI
   * toolkit submits the field values.  Most implementation classes will just delegate to the format/parse
   * methods.
   * <p>
   * <b>Note:</b> This base class just uses the format()/parse() methods for the form formatting.
   * @param value The object to format for display.  Null is allowed.  Empty strings are treated as null.
   * @param locale The locale to format the value for (default: The request locale).
   * @param fieldDefinition The field definition used to define this field (optional).
   * @return The formatted value.
   */
  @Override
  String formatForm(Object value, Locale locale, FieldDefinitionInterface fieldDefinition) {
    return format(value, locale, fieldDefinition)
  }

  /**
   * Defines the editor to use for this type of field when used in an editable inline grid.
   * <p>
   * This base class defaults to the simple text editor.  Other sub-classes will provide their own editors.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The editor (e.g. 'text').
   */
  // TODO: Remove when webix removed
  @Override
  String getGridEditor(FieldDefinitionInterface fieldDefinition) {
    return 'text'
  }

  /**
   * Returns the client format type code.  Used by the Vue client logic only.
   * @return The client code.
   */
  String getClientFormatType() {
    return getId()
  }

  /**
   * Returns the list of valid values for those formats that use a combobox or similar widget.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The list of valid values.
   */
  @Override
  List<ChoiceListItemInterface> getValidValues(FieldDefinitionInterface fieldDefinition) {
    return null
  }

  /**
   * Returns the URI used for the suggest/auto-complete lookup for this field.  See docs for details on auto-complete.
   * @param fieldDefinition The field definition used to define this field (optional, provides additional details on the value class).
   * @return The URI.
   */
  @Override
  String getValidValuesURI(FieldDefinitionInterface fieldDefinition) {
    return null
  }

  /**
   * Returns the value for the choice list (e.g. a domain record of enum element).
   * @return The value.
   */
  @Override
  Object getValue() {
    return null
  }

  /**
   * Returns true if this choice is the default choice in the list.
   * @return True if it is the default.
   */
  @Override
  boolean isDefaultChoice() {
    return this.defaultChoice
  }

  /**
   * True if this choice is the default choice in the list.
   */
  Boolean defaultChoice = false


  /**
   * Returns a localized string representation of the object.
   *
   * @return a string representation of the object.
   */
  @Override
  String toStringLocalized(Locale locale = null) {
    return GlobalUtils.lookup("basicFieldFormat.${getId()}.label", locale)
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
   * Returns the display value for the choice list.
   * @return The display value.
   */
  String getDisplayValue() {
    return "label.${NameUtils.lowercaseFirstLetter(this.getClass().simpleName)}"
  }

  /**
   * Finds the field format instance for the given Java standard type (e.g. String, Long, etc.).
   * @param type The type.
   */
  @CompileDynamic
  static BasicFieldFormat findByType(Class type) {
    for (value in EncodedTypeListUtils.instance.getAllValues(this)) {
      if (value instanceof BasicFieldFormat) {
        if (value.type && value.type == type) {
          // Get the instance using dynamic access
          return value.instance
        }
      }
    }

    return null
  }
}
