package org.simplemes.eframe.web.ui.webix.widget


import org.simplemes.eframe.data.format.DateOnlyFieldFormat

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The text input field for dateOnly widget.  Produces the UI elements needed for a simple input field element.
 * This widget supports the same options as the parent TextFieldWidget.
 *
 */
class DateOnlyFieldWidget extends DateFieldWidget {
  /**
   * Basic constructor.
   * @param widgetContext The widget context this widget is operating in.  Includes URI, parameters, marker etc.
   */
  DateOnlyFieldWidget(WidgetContext widgetContext) {
    super(widgetContext)
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
        return DateOnlyFieldFormat.instance.format(value, null, null)
      } else {
        // The edit value is in a locale-neutral format.
        return DateOnlyFieldFormat.instance.formatForm(value, null, null)
      }
    }
  }

  /**
   * Determines if this widget should display time.
   * @return True if it should display the time picker too.
   */
  @Override
  boolean isDisplayingTime() {
    return false
  }

  /**
   * Determines the size of this widget's input field (in characters).
   * @return The width.
   */
  @Override
  int getInputWidth() {
    return 10
  }


}
