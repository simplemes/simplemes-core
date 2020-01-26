/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import org.simplemes.eframe.data.format.BasicFieldFormat
import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.domain.validate.ValidationError

//import grails.gorm.annotation.Entity

import org.simplemes.eframe.misc.FieldSizes
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.misc.TypeUtils

import javax.persistence.Column

/**
 * This class defines a single custom field for an application.  This field is inserted into the domain class
 * by the Enterprise Framework plugin at startup or when the custom field is changed.
 *
 */
// TODO: Replace with non-hibernate alternative
//@ExtensibleFields()
@MappedEntity
@DomainEntity
@EqualsAndHashCode(includes = ["domainClassName", "fieldName"])
@ToString(includePackage = false, includeNames = true)
class FieldExtension implements FieldInterface {

  /**
   * The domain class name this field is applied to (full package name and class).
   */
  // TODO: DDL Add unique constraint on domainClassName+fieldName
  @Column(length = FieldSizes.MAX_CLASS_NAME_LENGTH, nullable = false)
  String domainClassName

  /**
   * The name of the field.  This follows the normal naming conventions for column names.
   */
  @Column(length = FieldSizes.MAX_CODE_LENGTH, nullable = false)
  String fieldName

  /**
   * The label to use when displaying or prompting for this field value.
   * If the label contains a period, then it will be looked up from the message bundle.
   */
  @Column(length = FieldSizes.MAX_LABEL_LENGTH, nullable = true)
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
  @Column(nullable = true)
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
  @Column(length = FieldSizes.MAX_CLASS_NAME_LENGTH, nullable = true)
  String valueClassName

  /**
   * Some GUI hints, stored as name/value paris in a standard map.
   */
  @Column(length = FieldSizes.MAX_NOTES_LENGTH, nullable = true)
  String guiHints

  @Id @AutoPopulated UUID uuid

  /**
   * Defines the order the fields are shown in the edit/show/etc GUIs.
   */
  @SuppressWarnings("unused")
  static fieldOrder = ['sequence', 'fieldName', 'fieldLabel', 'fieldFormat', 'maxLength', 'valueClassName', 'guiHints']

  /**
   * The primary keys for this object.
   */
  @SuppressWarnings("unused")
  static keys = ['flexType', 'fieldName']

  /**
   * Validates this 
   */
  def validate() {
    if (fieldName && !NameUtils.isLegalIdentifier(fieldName)) {
      //error.201.message="{1}" is not a legal custom field name.  Must be a legal Java variable name.
      return new ValidationError(201, 'fieldName', fieldName)
    }
    return validateValueClassName()
  }

  /**
   * Validates that the class name is correct for the current FieldFormat and is legal.
   */
  def validateValueClassName() {
    //noinspection GrEqualsBetweenInconvertibleTypes
    if (fieldFormat == EnumFieldFormat.instance) {
      if (!valueClassName) {
        //error.1.message=Required value is missing "{0}".
        return new ValidationError(1, 'valueClassName')
      }
      try {
        def clazz = TypeUtils.loadClass(valueClassName)
        if (!clazz.isEnum()) {
          //error.202.message={0} ({1}) is not an enumeration.
          return new ValidationError(202, 'valueClassName', valueClassName)
        }
      } catch (ClassNotFoundException ignored) {
        //error.4.message=Invalid "{0}" class.  Class "{1}" not found.
        return new ValidationError(4, 'valueClassName', valueClassName)
      }
    }
    return null
  }


  /**
   * Delete will remove any references to this field from any FieldGUIExtensions
   */
  @SuppressWarnings("unused")
  def afterDelete() {
    // TODO: Support afterDelete() method?
    FieldGUIExtension.removeReferencesToField(domainClassName, fieldName)
  }

}
