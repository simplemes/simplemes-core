package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.misc.TypeUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A multi-select combobox widget.
 */
class MultiComboboxWidget extends ComboboxWidget {

  /**
   * Basic constructor.
   * @param widgetContext The widget context this widget is operating in.  Includes URI, parameters, marker etc.
   */
  MultiComboboxWidget(WidgetContext widgetContext) {
    super(widgetContext)
  }

  /**
   * Formats the given value for display in the text field.
   * @param value The value to format.
   * @return The formatted value.
   */
  @Override
  String formatForDisplay(Object value) {
    def sb = new StringBuilder()
    for (o in value) {
      if (sb) {
        sb << ", "
      }
      sb << TypeUtils.toShortString(o) ?: ''
    }
    return sb.toString()
  }

  /**
   * Keep track of the max width of any value in the list.
   */
  @Override
  int getMaxValueWidth() {
    return super.getMaxValueWidth() * 1.5
  }

  /**
   * Returns the toolkit view widget type (e.g. 'combo').
   * @return The view type.
   */
  @Override
  String getView() {
    return 'multiComboEF'
  }
}
