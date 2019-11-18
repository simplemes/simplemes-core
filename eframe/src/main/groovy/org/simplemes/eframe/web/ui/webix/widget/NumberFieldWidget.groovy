package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.misc.NumberUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The text input field for numbers widget.  Produces the UI elements needed for a simple input field element.
 * This widget supports the same options as the parent TextFieldWidget.
 *
 */
class NumberFieldWidget extends TextFieldWidget {
  // TODO: Add name here and in Date/DateOnly widgets.
  /**
   * Basic constructor.
   * @param widgetContext The widget context this widget is operating in.  Includes URI, parameters, marker etc.
   */
  NumberFieldWidget(WidgetContext widgetContext) {
    super(widgetContext)
  }

  /**
   * Determines the max number of characters this field can contain.
   * @return The max.  Null is possible.
   */
  @Override
  Integer getMaxLength() {
    return 10
  }

  /**
   * Builds the attributes clause for the given max input length for the field.
   * This sub-class uses a fixed width, so the below elements are ignored.
   * @param maxLength The max length of the input field (never null).
   * @param type The field type ('password' or 'password-no-auto' supported).
   * @return The attributes clause.
   */
  @Override
  String buildAttributes(String type) {
    // Number fields have no need for a max length
    return ''
  }

  /**
   * Formats the given value for display in the text field.
   * @param value The value to format.  Must be a number.  Null allowed.
   * @return The formatted value.  Returns '' if null.
   */
  @Override
  String formatForDisplay(Object value) {
    if (value == null) {
      return ''
    } else {
      return NumberUtils.formatNumber((Number) value)
    }
  }

}
