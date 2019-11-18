package org.simplemes.eframe.web.ui.webix.widget

import groovy.transform.ToString
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.web.ui.webix.freemarker.MarkerCoordinator
import org.simplemes.eframe.web.ui.webix.freemarker.MarkerInterface

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The context the widget is executing in.  This includes the URI for the page, the marker and field definition
 * for the widget.
 */
@ToString(includeNames = true, includePackage = false)
class WidgetContext {

  /**
   * The parameters from the marker.  Never Null.
   */
  Map parameters = [:]

  /**
   * The marker this context is currently processing.
   */
  MarkerInterface marker

  /**
   * The coordinator used to coordinate between nested markers.  This keeps the nested markers in synch.
   */
  MarkerCoordinator markerCoordinator

  /**
   * The controller class that served up the page this widget will be displayed in.
   * <p>
   * <b>Note</b>: This method does not look at the marker parameter 'controller'.  This only uses the
   *              controller that provided the page.
   */
  Class controllerClass

  /**
   * The controller instance that served up the page this widget will be displayed in.
   */
  Object controller

  /**
   * The URI this page originated from.
   */
  String uri

  /**
   * The view (.hbs file) that the marker exists in.
   */
  String view

  /**
   * If true, then the widget should be created in readOnly (displayOnly) mode.
   */
  boolean readOnly = false

  /**
   * If true, then the widget should flag the UI element with an error flag (e.g. a red outline).
   */
  Boolean error = false

  /**
   * The domain/POGO that the field is part of.
   */
  Object object

  /**
   * The field definition to be used by the widget to build the control(s).
   */
  FieldDefinitionInterface fieldDefinition

  /**
   * The content of the field marker.  Supported by some widget types to add additional fields/buttons after the
   * main widget (e.g. a button/check box to the right of the input field).
   */
  String innerContent

  /**
   * Standard Map constructor.   Sets individual name properties from the given options.
   * @param options
   */
  WidgetContext(Map options = null) {
    options?.each { k, v ->
      this[(String) k] = v
    }
  }

  /**
   * A copy constructor that copies all properties, except for the parameters.  Those are passed in.
   * @param widgetContext The context to copy most properties from (except 'parameters').
   * @param parameters The  parameters Map for the new widget.
   */
  WidgetContext(WidgetContext widgetContext, Map parameters) {
    marker = widgetContext.marker
    controllerClass = widgetContext.controllerClass
    controller = widgetContext.controller
    uri = widgetContext.uri
    view = widgetContext.view
    readOnly = widgetContext.readOnly
    object = widgetContext.object
    fieldDefinition = widgetContext.fieldDefinition

    this.parameters = parameters
  }

}
