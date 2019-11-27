package org.simplemes.eframe.web.ui.webix.freemarker

import groovy.util.logging.Slf4j
import org.simplemes.eframe.data.SimpleFieldDefinition
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.domain.DomainReference
import org.simplemes.eframe.web.ui.WidgetFactory

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

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

    // delegates most of the work to the TextFieldWidget
    def widgetScript = new StringBuilder()
    def widgetContext = buildWidgetContext(fieldDefinition)
    widgetContext.object = domainObject ?: [:]
    if (parameters.value) {
      widgetContext.object[fieldName] = parameters.value
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
      widgetScript << ","
      widgetScript << widget.build()
      //widgetScript << ","
    }

    write(widgetScript.toString())
  }

}
