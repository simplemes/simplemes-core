/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom

import groovy.transform.ToString
import org.simplemes.eframe.custom.domain.FieldInterface
import org.simplemes.eframe.data.SimpleFieldDefinition
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.misc.TypeUtils

/**
 * Holds the basic definition needed for a single field in a configurable type field element.  This provides the
 * basic FieldDefinitionInterface used for configurable types.  This class acts as a bridge from the
 * configurable types such as FlexType to a proper field definition for the GUIs and API access.
 *
 */
@ToString(includePackage = false, includeNames = true, includeSuper = true, includes = ['configTypeFieldName'])
class ConfigurableTypeFieldDefinition extends SimpleFieldDefinition {

  /**
   * The parent field the configurable type is stored in (e.g. the FlexType field in the domain class).
   */
  String configTypeFieldName

  /**
   * The history tracking option.
   */
  HistoryTracking historyTracking = HistoryTracking.NONE

  /**
   * Constructor for a persisted field definition.
   * @param field The field definition. Copies properties such as type, field, label, etc.
   * @param configTypeFieldName The name of the Configurable Type that this field belongs to.
   */
  ConfigurableTypeFieldDefinition(FieldInterface field, String configTypeFieldName = null) {
    super()
    this.name = field.fieldName
    this.configTypeFieldName = configTypeFieldName
    this.format = field.fieldFormat
    this.label = field.fieldLabel ?: field.fieldName
    this.maxLength = field.maxLength
    this.sequence = field.sequence
    this.required = field.required
    this.historyTracking = field.historyTracking
    if (field.valueClassName) {
      this.type = TypeUtils.loadClass(field.valueClassName)
    }
    this.guiHints = TextUtils.parseNameValuePairs(field.guiHints)
  }

  /**
   * The map constructor.  Assigns values to elements from the Map
   * @param map The map containing the field values.
   */
  ConfigurableTypeFieldDefinition(Map map) {
    super(map)
    ArgumentUtils.checkMissing(configTypeFieldName, 'configTypeFieldName')
    ArgumentUtils.checkMissing(name, 'name')
    this.name = "${configTypeFieldName}_${map.name}"
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


}
