package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.data.format.DateFieldFormat

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The text input field for date/time widget.  Produces the UI elements needed for a simple input field element.
 * This widget supports the same options as the parent TextFieldWidget.
 *
 */
class DateFieldWidget extends TextFieldWidget {
  /**
   * Basic constructor.
   * @param widgetContext The widget context this widget is operating in.  Includes URI, parameters, marker etc.
   */
  DateFieldWidget(WidgetContext widgetContext) {
    super(widgetContext)
  }

  /**
   * Builds the field content widget itself.
   * This sub class builds a date picker.
   * @param id The field ID.
   * @param value The value
   * @return The text for field element.
   */
  @Override
  String buildWidget(String id, Object value) {
    if (widgetContext.readOnly) {
      return super.buildWidget(id, value)
    } else {
      def s = formatForDisplay(value)
      def width = """,inputWidth: tk.pw("${calculateFieldWidth(inputWidth)}em")"""
      def options = """, stringResult: true, editable: true, timepicker: ${displayingTime}"""
      def cssS = widgetContext.error ? ',css: "webix_invalid" ' : ''
      return """{view: "datepicker", id: "$id", name: "$id", value: "$s" $options $cssS $width}"""
    }
  }


  /**
   * Determines if this widget should display time.
   * @return True if it should display the time picker too.
   */
  boolean isDisplayingTime() {
    return true
  }

  /**
   * Determines the size of this widget's input field (in characters).
   * @return The width.
   */
  int getInputWidth() {
    return 20
  }

  /**
   * Formats the given value for display in the text field.
   * @param value The value to format.  Must be a Date.  Null allowed.
   * @return The formatted value.  Returns '' if null.
   */
  @Override
  String formatForDisplay(Object value) {
    if (value == null) {
      return ''
    } else {
      if (widgetContext.readOnly) {
        return DateFieldFormat.instance.format(value, null, null)
      } else {
        // The edit value is in a locale-neutral format.
        return DateFieldFormat.instance.formatForm(value, null, null)
      }
    }
  }

}
