/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import groovy.util.logging.Slf4j
import org.simplemes.eframe.data.SimpleFieldDefinition
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.domain.DomainReference
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.web.ui.WidgetFactory

/**
 * Provides the efField freemarker marker implementation.
 * This builds an input field for user data entry.
 */
@Slf4j
@SuppressWarnings("unused")
class FieldMarker extends BaseMarker {

  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    if (!(markerContext?.markerCoordinator?.formID)) {
      throw new MarkerException("efField must be enclosed in an efForm marker.", this)
    }

    def fullFieldName = (String) parameters.field
    if (!fullFieldName) {
      throw new MarkerException("efField is missing a 'field' parameter.", this)
    }

    // Find the field definition for this field.
    def fieldName
    def fieldDefinition
    if (fullFieldName.contains('.')) {
      def domainReference = DomainReference.buildDomainReference(fullFieldName)
      fieldName = domainReference.fieldName
      fieldDefinition = domainReference.fieldDefinition
    } else {
      // Not a domain reference, so assume some values.
      fieldName = fullFieldName
      def max = parameters.maxLength ?: '40'
      if (max instanceof BigDecimal) {
        max = max.intValue()
      }
      fieldDefinition = new SimpleFieldDefinition(name: fieldName, type: String,
                                                  format: StringFieldFormat.instance,
                                                  maxLength: Integer.valueOf(max))
    }

    def id = parameters.id ?: fieldName ?: 'unknown'

    // See if the domain/value object is in the current model.
    def modelName = parameters.modelName
    if (modelName) {
      // Use the domain value instead of the one derived from the controller.
      domainObject = unwrap(environment.dataModel?.get(modelName))
    }

    // delegates most of the work to the TextFieldWidget
    def widgetScript = new StringBuilder()
    def widgetContext = buildWidgetContext(fieldDefinition)
    widgetContext.object = domainObject ?: [:]
    if (parameters.value) {
      // Make sure it is safe to set the value on the object/map.
      if (widgetContext?.object instanceof Map) {
        widgetContext.object[fieldName] = parameters.value
      } else {
        if (widgetContext?.object?.hasProperty(fieldName)) {
          widgetContext.object[fieldName] = parameters.value
        } else {
          def object = widgetContext.object
          def name = object?.getClass()?.simpleName ?: "unknown"
          def s = "The domain object $name does not have the field '$fieldName'.\n"
          s += "You probably have an element in the model named '${NameUtils.lowercaseFirstLetter(name)} '.\n"
          s += "This confuses the marker logic when 'value=${}' is used.  Consider renaming the model element. Object = ${object}.\n"
          throw new MarkerException(s, this)
        }
      }
    }
    if (parameters.readOnly) {
      widgetContext.readOnly = ArgumentUtils.convertToBoolean(parameters.readOnly)
    }
    widgetContext.parameters.label = parameters.label
    addFieldSpecificParameters(widgetContext, fieldName, (Map) parameters)
    widgetContext.innerContent = renderContent()
    def widget = WidgetFactory.instance.build(widgetContext)

    // Check for an 'after' field directive.
    def after = parameters.after
    log.trace('apply() fieldDef={}, widgetContext={}, widget={}, after={}', fieldDefinition, widgetContext, widget, after)

    if (after) {
      def formID = markerContext.markerCoordinator.formID
      // Need to create a pre-script with the right field definition
      def pre = """ var ${id}FormData = 
        ${widget.build()}
      ;
      tk._addRowToForm(${formID}FormData,${id}FormData,"$after");

      """
      markerContext.markerCoordinator.addPrescript(pre)
    } else {
      widgetScript << widget.build()
      widgetScript << ","
    }

    write(widgetScript.toString())
  }

}
