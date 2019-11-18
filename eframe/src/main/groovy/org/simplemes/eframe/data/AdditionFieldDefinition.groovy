package org.simplemes.eframe.data

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.simplemes.eframe.custom.AdditionConfiguration
import org.simplemes.eframe.custom.AdditionFieldConfiguration
import org.simplemes.eframe.custom.ExtensibleFieldHelper
import org.simplemes.eframe.data.format.CustomChildListFieldFormat
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.misc.TypeUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A field definition for a field added by a module addition.
 * Typically corresponds to String, Integer, etc fields in a domain/POGO.
 */
@Slf4j
@ToString(includePackage = false, includeNames = true, includeSuper = true)
class AdditionFieldDefinition extends SimpleFieldDefinition {

  /**
   * The addition that contributed this field.
   */
  String additionName

  /**
   * Basic constructor to create from a AdditionFieldConfiguration.
   * @param fieldConfiguration The field configuration to create this from.
   * @param additionConfiguration The addition that this field came from.
   */
  AdditionFieldDefinition(AdditionFieldConfiguration fieldConfiguration, AdditionConfiguration additionConfiguration) {
    name = fieldConfiguration.name
    format = fieldConfiguration.format
    type = format?.type ?: Object
    if (fieldConfiguration.valueClass?.name) {
      // Use a different type if the value class name is provided.
      type = TypeUtils.loadClass(fieldConfiguration.valueClass.name)
      referenceType = type  // Used for domain references
    }
    maxLength = fieldConfiguration.maxLength
    additionName = additionConfiguration.name
    label = fieldConfiguration.label ?: name
    child = (format == CustomChildListFieldFormat.instance)
    custom = false
    try {
      guiHints = TextUtils.parseNameValuePairs(fieldConfiguration.guiHints)
    } catch (Exception ex) {
      log.error('Error parsing guiHints for field {}, addition {} : {}', name, additionName, ex)
    }
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
