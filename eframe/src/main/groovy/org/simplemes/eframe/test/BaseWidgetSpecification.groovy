/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import org.simplemes.eframe.data.CustomFieldDefinition
import org.simplemes.eframe.data.SimpleFieldDefinition
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.web.ui.webix.freemarker.MarkerCoordinator
import org.simplemes.eframe.web.ui.webix.widget.WidgetContext
import sample.controller.AllFieldsDomainController

/**
 * This is a base class that provide additional support for testing the UI widget classes.
 */
class BaseWidgetSpecification extends BaseSpecification {

  /**
   * Builds a WidgetContext with a mock field definition with the given options.
   * This creates a Map as the domain/POGO to hold the value to be displayed.
   *
   * <h3>Options</h3>
   * The options supported include:
   * <ul>
   *   <li><b>name</b> - The name of the field definition for the context. </li>
   *   <li><b>format</b> - The format of the field definition for the context. </li>
   *   <li><b>maxLength</b> - The max length of the field definition for the context. </li>
   *   <li><b>readOnly</b> - True for a read only field definition for the context. </li>
   *   <li><b>type</b> - The type (Class) of the field definition for the context. </li>
   *   <li><b>referenceType</b> - The referenceType (Class) of the field definition for the context. </li>
   *   <li><b>custom</b> - True if a custom field definition should be used. </li>
   *   <li><b>label</b> - The field label for a custom field definition. </li>
   *   <li><b>error</b> - True if the field definition should be flagged as an error field. </li>
   *   <li><b>value</b> - The value of the field for the context.  Ignored if <b>domainObject</b> is given.</li>
   *   <li><b>domainObject</b> - The domain object for the context. </li>
   *   <li><b>parameters</b> - The HTTP parameters defined for the context. </li>
   * </ul>
   *
   * @param options The options.  See above.
   * @return The widget context.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  WidgetContext buildWidgetContext(Map options = null) {
    def name = options?.name ?: 'aField'
    def custom = options?.custom ?: false

    // Build the field definition as custom or normal, as needed
    def fieldDef
    if (custom) {
      fieldDef = new CustomFieldDefinition(name: name, format: options?.format ?: StringFieldFormat.instance,
                                           referenceType: options?.referenceType,
                                           type: options?.type,
                                           label: options?.label,
                                           maxLength: options?.maxLength)
    } else {
      fieldDef = new SimpleFieldDefinition(name: name, format: options?.format ?: StringFieldFormat.instance,
                                           referenceType: options?.referenceType,
                                           type: options?.type,
                                           maxLength: options?.maxLength)
    }

    def object = options?.domainObject ?: [:]

    if (options?.value != null) {
      fieldDef.setFieldValue(object, options.value)
    }

    def parameters = options?.parameters ?: [:]
    def widgetContext = new WidgetContext(fieldDefinition: fieldDef,
                                          controllerClass: options?.controllerClass ?: AllFieldsDomainController,
                                          uri: options?.uri ?: '/dummy',
                                          error: options?.error,
                                          object: object,
                                          parameters: parameters,
                                          markerCoordinator: new MarkerCoordinator())
    if (options?.readOnly) {
      // Due to IllegalArgumentException, we can't do this in the constructor.
      widgetContext.readOnly = options.readOnly
    }

    return widgetContext
  }


}
