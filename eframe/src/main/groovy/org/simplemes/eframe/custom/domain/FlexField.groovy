/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.data.format.BasicFieldFormat
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.misc.FieldSizes

import javax.persistence.Column
import javax.persistence.ManyToOne

/**
 * Defines the basic user-defined data field used explicitly in domain class.  This defines the basic type of the field
 * and some hints for appearance and validations (list of valid values, etc).
 * This is normally a child of the FlexType to group related items together.  The actual values are stored in a
 * a user definable field (<b>flexFields</b>).
 */
@MappedEntity
@DomainEntity
@EqualsAndHashCode(includes = ['flexType', 'fieldName'])
@ToString(includePackage = false, includeNames = true, excludes = ['flexType'])
@SuppressWarnings("unused")
class FlexField implements FieldInterface, FieldTrait {

  /**
   * The parent flex type this field is a child of.
   */
  @ManyToOne
  @MappedProperty(type = DataType.UUID)
  FlexType flexType

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

  @Id @AutoPopulated
  @MappedProperty(type = DataType.UUID)
  UUID uuid

  /**
   * Defines the order the fields are shown in the edit/show/etc GUIs.
   */
  static fieldOrder = ['sequence', 'fieldName', 'fieldLabel', 'fieldFormat', 'maxLength', 'valueClassName', 'guiHints']

  /**
   * The primary keys for this object.
   */
  static keys = ['flexType', 'fieldName']




}
