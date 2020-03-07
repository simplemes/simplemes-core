/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import freemarker.core.Environment
import freemarker.template.TemplateDirectiveBody
import freemarker.template.TemplateModel
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.web.ui.webix.widget.WidgetContext

/**
 * Defines the methods that a freemarker marker implementation will provide.  This is used to generate the
 * HTML page output for custom markers.
 */
interface MarkerInterface {

  /**
   * Sets the values passed from the directive execution.  This includes the values needed to build the HTML output.
   * @param env
   * @param params
   * @param loopVars
   * @param body
   */
  void setValues(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)

  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  void execute()

  /**
   * Builds a human-readable form of this marker and its context for use in exceptions.
   * This should give user some idea where the error happened.
   * @return The exception display.
   */
  String toStringForException()

  /**
   * Builds the widget context for this marker.  This avoids embedding knowledge of the marker in the widget classes.
   * @param The field definition for the field this widget is generating the UI for.
   * @return The WidgetContext.
   */
  WidgetContext buildWidgetContext(FieldDefinitionInterface fieldDefinition)

  /**
   * Gets the parameters from the HTTP request that called this marker.
   * @return The parameters from the HTTP request.
   */
  Map getRequestParameters()
}