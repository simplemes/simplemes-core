/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.service

import groovy.util.logging.Slf4j
import org.simplemes.eframe.custom.ExtensibleFieldHelper
import org.simplemes.eframe.custom.domain.FieldExtension
import org.simplemes.eframe.custom.domain.FieldGUIExtension
import org.simplemes.eframe.custom.gui.FieldAdjuster
import org.simplemes.eframe.data.format.BooleanFieldFormat
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.data.format.DateOnlyFieldFormat
import org.simplemes.eframe.data.format.DomainReferenceFieldFormat
import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.data.format.FieldFormatInterface
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.web.PanelUtils

import javax.inject.Singleton
import javax.transaction.Transactional

/**
 * Provides access to the extensions that allow user to customize the GUI and application.
 */
@Slf4j
@Singleton
class ExtensionService {

  /**
   * Finds the current extension configuration for a given domain. This returns the
   * available and configured list of fields in two lists.
   *
   * <h3>Fields</h3>
   * The lists contain maps with these fields:
   * <ul>
   *   <li><b>name</b> - The field name.
   *   <li><b>label</b> - The field label (localized).
   *   <li><b>type</b> - The field type (e.g. 'textField', 'dateField', etc).
   *   <li><b>custom</b> - True if the field is a custom field.
   * </ul>
   *
   * @param domainClass The domain class to return the current config for.
   * @return A list of available fields and a list of configured fields.
   *
   */
  @Transactional()
// TODO: readOnly = true)
  Tuple2<List<Map>, List<Map>> getExtensionConfiguration(Class domainClass) {
    ArgumentUtils.checkMissing(domainClass, 'domainClass')
    def available = []
    def configured = []

    def fieldDefinitions = ExtensibleFieldHelper.instance.getEffectiveFieldDefinitions(domainClass)
    def coreFieldOrder = DomainUtils.instance.getStaticFieldOrder(domainClass)
    def fieldOrder = ExtensibleFieldHelper.instance.getEffectiveFieldOrder(domainClass)
    // Build the configured list from the effective field order list
    for (fieldName in fieldOrder) {
      if (!PanelUtils.isPanel(fieldName)) {
        def fieldDefinition = fieldDefinitions[fieldName]
        def field = [name: fieldName]
        field.label = GlobalUtils.lookup(fieldDefinition.label)
        if (fieldDefinition.custom) {
          field.custom = true
          field.recordID = fieldDefinition.fieldExtensionId
        } else {
          field.custom = false
        }
        field.type = determineFieldType(fieldName, fieldDefinition.format)
        configured << field
      } else {
        // The field is a panel.
        def custom = !coreFieldOrder.contains(fieldName)
        def panelName = PanelUtils.getPanelName(fieldName)
        def label = GlobalUtils.lookup("${panelName}.panel.label")
        if (label == "${panelName}.panel.label") {
          // Could not find in messages.properties, so use the original value for custom panels.
          label = panelName
        }
        configured << [name: fieldName, type: 'tabbedPanels', label: label, custom: custom]
      }
    }

    // Build the available list from the other fields not in the fieldOrder
    for (fieldDefinition in fieldDefinitions) {
      if (!fieldOrder.contains(fieldDefinition.name)) {
        def field = [name: fieldDefinition.name]
        field.label = GlobalUtils.lookup(fieldDefinition.label)
        if (fieldDefinition.custom) {
          field.custom = true
          field.recordID = fieldDefinition.fieldExtensionId
        } else {
          field.custom = false
        }
        field.type = determineFieldType(fieldDefinition.name, fieldDefinition.format)
        available << field
      }
    }

    return [available, configured]
  }

  /**
   * Determines the field type for the Configuration GUI use.  Gives a hint on the appearance of the field.
   * @param fieldName The field name.
   * @param format The field format.
   * @return The type (e.g. 'dateField',etc).
   */
  String determineFieldType(String fieldName, FieldFormatInterface format) {
    if (PanelUtils.isPanel(fieldName)) {
      return 'tabbedPanels'
    }

    def type = 'textField'
    switch (format?.class) {
      case DateFieldFormat:
      case DateOnlyFieldFormat:
        type = 'dateField'
        break
      case EnumFieldFormat:
      case DomainReferenceFieldFormat:
        type = 'dropDown'
        break
      case BooleanFieldFormat:
        type = 'checkBox'
        break
    }
    return type
  }


  /**
   * Saves the new field order as configured by the user in the Configuration GUI for a definition page.
   *
   * @param domainClass The domain class to return the current config for.
   *
   */
  @Transactional
  void saveFieldOrder(Class domainClass, List<String> newFieldOrder) {
    log.debug("saveFieldOrder(): {} {}", domainClass, newFieldOrder)
    def fieldOrder = DomainUtils.instance.getStaticFieldOrder(domainClass)
    def adjustments = FieldAdjuster.determineDifferences(fieldOrder, newFieldOrder)

    // Now, update/create the FieldGUIExtension record for this domain with the new field order.
    def fieldGUIExtension = FieldGUIExtension.findByDomainName(domainClass.name)
    if (!fieldGUIExtension) {
      // Create a new record if needed.
      fieldGUIExtension = new FieldGUIExtension(domainName: domainClass.name)
    }
    fieldGUIExtension.adjustments = adjustments
    fieldGUIExtension.save()
  }


  /**
   * Deletes a custom field and it use in the field order for the domain.
   *
   * @param id The ID of the FieldExtension record.
   * @return The number of FieldExtension records deleted.
   */
  @Transactional
  int deleteField(String id) {
    def fieldExtension = FieldExtension.get(UUID.fromString(id))
    if (!fieldExtension) {
      return 0
    }
    def domainClassName = fieldExtension.domainClassName
    log.debug("deleteField(): {} {}", domainClassName, fieldExtension)

    fieldExtension.delete()

    return 1
  }


}
