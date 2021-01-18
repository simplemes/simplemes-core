/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.domain

import org.simplemes.eframe.custom.HistoryTracking
import org.simplemes.eframe.data.format.BasicFieldFormat

/**
 * Defines the basic user-defined data field.  This defines the basic type of the field
 * and its properties.  This is used for custom field extensions on domain objects.
 */
interface FieldInterface {
  /**
   * The name of the field.  This follows the normal naming conventions for column names.
   */
  String getFieldName()

  /**
   * The name of the field.  This follows the normal naming conventions for column names.
   */
  void setFieldName(String fieldName)

  /**
   * The label to use when displaying or prompting for this field value.
   * If the label contains a period, then it will be looked up from the message bundle.
   */
  String getFieldLabel()

  /**
   * The label to use when displaying or prompting for this field value.
   * If the label contains a period, then it will be looked up from the message bundle.
   */
  void setFieldLabel(String fieldLabel)

  /**
   * The format of this field (e.g. String, integer, etc).
   * <b>This should never change on existing records without database migration!</b>
   * It is possible to change to a String format for most cases, but most other changes will require a migration.
   */
  BasicFieldFormat getFieldFormat()

  /**
   * The format of this field (e.g. String, integer, etc).
   * <b>This should never change on existing records without database migration!</b>
   * It is possible to change to a String format for most cases, but most other changes will require a migration.
   */
  void setFieldFormat(BasicFieldFormat fieldFormat)

  /**
   * The maximum length of the custom value (Strings only).
   * 0 or null means no limit (except for column limitations).
   */
  Integer getMaxLength()

  /**
   * The maximum length of the custom value (Strings only).
   * 0 or null means no limit (except for column limitations).
   */
  void setMaxLength(Integer maxLength)

  /**
   * The sequence this field should be displayed in.
   * <b>(Default: 0)</b>
   */
  Integer getSequence()

  /**
   * The sequence this field should be displayed in.
   * <b>(Default: 0)</b>
   */
  void setSequence(Integer sequence)

  /**
   * True if the field is considered required.
   */
  Boolean getRequired()

  /**
   * True if the field is considered required.
   */
  void setRequired(Boolean required)

  /**
   * The history tracking setting.
   */
  HistoryTracking getHistoryTracking()

  /**
   * The history tracking setting.
   */
  void setHistoryTracking(HistoryTracking historyTracking)

  /**
   * The class that provides the field values.  Provides support for custom fields that are Enumerations,
   * domain class references and EncodedTypes.
   */
  String getValueClassName()

  /**
   * The class that provides the field values.  Provides support for custom fields that are Enumerations,
   * domain class references and EncodedTypes.
   */
  void setValueClassName(String valueClassName)

  /**
   * Some GUI hints, stored as name/value paris in a standard map.
   */
  String getGuiHints()

  /**
   * Some GUI hints, stored as name/value paris in a standard map.
   */
  void setGuiHints(String guiHints)

  UUID getUuid()

  void setUuid(UUID uuid)

}