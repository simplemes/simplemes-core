package org.simplemes.eframe.web.ui.webix.widget
/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The text input field for a boolean (checkbox) widget.  Produces the UI elements needed for a simple input field element.
 * This widget supports the same options as the parent BaseLabeledFieldWidget.
 *
 */
class BooleanFieldWidget extends BaseLabeledFieldWidget {
  /**
   * Basic constructor.
   * @param widgetContext The widget context this widget is operating in.  Includes URI, parameters, marker etc.
   */
  BooleanFieldWidget(WidgetContext widgetContext) {
    super(widgetContext)
  }

  /**
   * Builds the field content widget itself.  This is the element to the right of the label.
   * This sub class builds a checkbox.
   * @param id The field ID.
   * @param value The value
   * @return The text for field element.
   */
  @Override
  String buildWidget(String id, Object value) {
    def s = value ? 'true' : 'false'

    def disabled = widgetContext.readOnly ? 'true' : 'false'
    return """{view: "checkbox", id: "$id", name: "$id", value: $s, disabled: $disabled}"""
  }


}
