package org.simplemes.eframe.custom.domain

//import grails.gorm.annotation.Entity
import groovy.transform.EqualsAndHashCode
import org.simplemes.eframe.data.format.BasicFieldFormat
import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.misc.FieldSizes
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.misc.TypeUtils

/**
 * Defines the basic user-defined data field.  This defines the basic type of the field
 * and validations.  This is used for custom field extensions on domain objects.
 */
//@Entity
@EqualsAndHashCode(includes = ['fieldName'])
abstract class AbstractField {

  long id
  /**
   * The name of the field.  This follows the normal naming conventions for column names.
   */
  String fieldName

  /**
   * The label to use when displaying or prompting for this field value.
   * If the label contains a period, then it will be looked up from the message bundle.
   */
  String fieldLabel

  /**
   * The format of this field (e.g. String, integer, etc).
   * <b>This should never change on existing records without database migration!</b>
   * It is possible to change to a String format for most cases, but most other changes will require a migration.
   */
  BasicFieldFormat fieldFormat = StringFieldFormat.instance

  /**
   * The maximum length of the custom value (Strings only).
   * 0 or null means no limit (except for column limitations).
   */
  Integer maxLength

  /**
   * The sequence this field should be displayed in.
   * <b>(Default: 0)</b>
   */
  Integer sequence = 0

  /**
   * The class that provides the field values.  Provides support for custom fields that are Enumerations,
   * domain class references and EncodedTypes.
   */
  String valueClassName

  /**
   * Some GUI hints, stored as name/value paris in a standard map.
   */
  String guiHints

  /**
   * Internal constraints.
   */
  @SuppressWarnings("unused")
  static constraints = {
    fieldName(maxSize: FieldSizes.MAX_CODE_LENGTH,
              validator: {
                if (!NameUtils.isLegalIdentifier(it)) {
                  return ['invalidFieldName']
                }
              }, blank: false)
    fieldLabel(maxSize: FieldSizes.MAX_LABEL_LENGTH, nullable: true)
    fieldFormat(length: 1, nullable: false)
    maxLength(nullable: true)
    valueClassName(maxSize: FieldSizes.MAX_CLASS_NAME_LENGTH, nullable: true,
                   validator: { className, obj -> validateValueClassName(className, obj) })
    guiHints(nullable: true, maxSize: FieldSizes.MAX_NOTES_LENGTH)
  }

  /**
   * Internal Mapping of fields to columns.
   */
  @SuppressWarnings("unused")
  static mapping = {
    cache true
  }

  /**
   * Validates that the class name is correct for the current FieldFormat and is legal.
   * @param className The class name to validate
   * @param fieldObject The entire field object to be validated against.
   */
  static validateValueClassName(String className, AbstractField fieldObject) {
    //noinspection GrEqualsBetweenInconvertibleTypes
    if (fieldObject.fieldFormat == EnumFieldFormat.instance) {
      if (!className) {
        return ['missingValueClassName']
      }
      try {
        def clazz = TypeUtils.loadClass(className)
        if (!clazz.isEnum()) {
          return ['invalidValueClassName']
        }
      } catch (ClassNotFoundException ignored) {
        return ['classNotFound']
      }
    }
    return null
  }

  /**
   * Defines the order the fields are shown in the edit/show/etc GUIs.
   */
  @SuppressWarnings("unused")
  static fieldOrder = ['sequence', 'fieldName', 'fieldLabel', 'fieldFormat', 'maxLength', 'valueClassName', 'guiHints']

  /**
   *  Returns human readable form of this object.
   * @return human readable string.
   */

  @Override
  String toString() {
    return "AbstractField{" +
      "field='" + fieldName + '\'' +
      ", label='" + fieldLabel + '\'' +
      ", fieldFormat='" + fieldFormat + '\'' +
      ", maxLength=" + maxLength +
      ", sequence=" + sequence +
      ", guiHints=" + guiHints +
      '}'
  }

}
