/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data

import groovy.transform.ToString
import org.simplemes.eframe.custom.ExtensibleFieldHelper
import org.simplemes.eframe.custom.HistoryTracking
import org.simplemes.eframe.custom.domain.FieldExtension
import org.simplemes.eframe.misc.TypeUtils

/**
 * A field definition for custom fields created by user or addition modules.
 * Typically corresponds to String, Integer, etc fields in a domain/POGO.
 */
@ToString(includePackage = false, includeNames = true, includeSuper = true)
class CustomFieldDefinition extends SimpleFieldDefinition {

  /**
   * The record ID for the field extension record that this field def represents.
   */
  UUID fieldExtensionUuid

  /**
   * The history tracking option.
   */
  HistoryTracking historyTracking = HistoryTracking.NONE

  /**
   * Basic constructor.
   * @param options The options.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  CustomFieldDefinition(Map options) {
    super(options)
    options.each { key, value ->
      this[key] = value
    }
    custom = true
  }

  /**
   * Basic constructor to create from a FieldExtension.
   * @param fieldExtension The field extension record to create this from.
   */
  CustomFieldDefinition(FieldExtension fieldExtension) {
    name = fieldExtension.fieldName
    format = fieldExtension.fieldFormat
    type = format?.type ?: Object
    if (fieldExtension.valueClassName) {
      // Use a different type if the value class name is provided.
      type = TypeUtils.loadClass(fieldExtension.valueClassName)
      referenceType = type  // Used for domain references
    }
    maxLength = fieldExtension.maxLength
    fieldExtensionUuid = fieldExtension.uuid
    label = fieldExtension.fieldLabel ?: name
    custom = true
    required = fieldExtension.required
    historyTracking = fieldExtension.historyTracking
  }

  /**
   * Gets the given field value from the given object (domain or POGO depending on sub-class).
   *
   * @param domainOrPogoObject The domain or POGO object the field is stored in.
   * @return The value of the field.
   */
  @Override
  Object getFieldValue(Object domainOrPogoObject) {
    return ExtensibleFieldHelper.instance.getFieldValue(domainOrPogoObject, name)
  }

  /**
   * Sets the given field value in the given object (domain or POGO depending on sub-class).
   *
   * @param domainOrPogoObject The domain or POGO object the field is to be stored in.
   * @param value The field value.
   */
  @Override
  void setFieldValue(Object domainOrPogoObject, Object value) {
    ExtensibleFieldHelper.instance.setFieldValue(domainOrPogoObject, name, value)
  }

  /**
   * The display label.  If not provided, then the field name is used.
   */
  @Override
  String getLabel() {
    return super.getLabel() ?: name
  }

}
