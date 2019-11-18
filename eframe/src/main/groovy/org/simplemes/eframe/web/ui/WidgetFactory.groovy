package org.simplemes.eframe.web.ui

import org.simplemes.eframe.data.format.BigDecimalFieldFormat
import org.simplemes.eframe.data.format.BooleanFieldFormat
import org.simplemes.eframe.data.format.ChildListFieldFormat
import org.simplemes.eframe.data.format.ConfigurableTypeDomainFormat
import org.simplemes.eframe.data.format.CustomChildListFieldFormat
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.data.format.DateOnlyFieldFormat
import org.simplemes.eframe.data.format.DomainRefListFieldFormat
import org.simplemes.eframe.data.format.DomainReferenceFieldFormat
import org.simplemes.eframe.data.format.EncodedTypeFieldFormat
import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.data.format.IntegerFieldFormat
import org.simplemes.eframe.data.format.LongFieldFormat
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.web.ui.webix.widget.BooleanFieldWidget
import org.simplemes.eframe.web.ui.webix.widget.ComboboxWidget
import org.simplemes.eframe.web.ui.webix.widget.ConfigurableTypeFieldWidget
import org.simplemes.eframe.web.ui.webix.widget.DateFieldWidget
import org.simplemes.eframe.web.ui.webix.widget.DateOnlyFieldWidget
import org.simplemes.eframe.web.ui.webix.widget.GridWidget
import org.simplemes.eframe.web.ui.webix.widget.MultiComboboxWidget
import org.simplemes.eframe.web.ui.webix.widget.NumberFieldWidget
import org.simplemes.eframe.web.ui.webix.widget.TextFieldWidget
import org.simplemes.eframe.web.ui.webix.widget.WidgetContext

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Builds a widget for the given field definition.
 */
class WidgetFactory {

  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static WidgetFactory instance = new WidgetFactory()


  /**
   * Builds the correct field widget for the given context (e.g. field Definition).
   * @param widgetContext The widget context, including the field definition.  Never null.
   */
  WidgetInterface build(WidgetContext widgetContext) {
    ArgumentUtils.checkMissing(widgetContext, 'widgetContext')
    def format = widgetContext.fieldDefinition?.format
    switch (format) {
      case StringFieldFormat:
        return new TextFieldWidget(widgetContext)
      case BigDecimalFieldFormat:
      case LongFieldFormat:
      case IntegerFieldFormat:
        return new NumberFieldWidget(widgetContext)
      case BooleanFieldFormat:
        return new BooleanFieldWidget(widgetContext)
      case DateFieldFormat:
        return new DateFieldWidget(widgetContext)
      case DateOnlyFieldFormat:
        return new DateOnlyFieldWidget(widgetContext)
      case DomainRefListFieldFormat:
        return new MultiComboboxWidget(widgetContext)
      case ConfigurableTypeDomainFormat:
        return new ConfigurableTypeFieldWidget(widgetContext)
      case DomainReferenceFieldFormat:
      case EnumFieldFormat:
      case EncodedTypeFieldFormat:
        return new ComboboxWidget(widgetContext)
      case ChildListFieldFormat:
      case CustomChildListFieldFormat:
        return new GridWidget(widgetContext)
      default:
        def widget = checkComplexField(widgetContext)
        // Fallback to simple text field and use the string form of the value
        return widget ?: new TextFieldWidget(widgetContext)
    }
  }

  /**
   * Checks for complex field types and builds the widget for them, if possible.
   * @param widgetContext The widget context, including the field definition.  Never null.
   * @return The widget.  Can be null.
   */
  WidgetInterface checkComplexField(WidgetContext widgetContext) {
    return null
  }

}
