package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.i18n.GlobalUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The text input field widget.  Produces the UI elements needed for a simple input field element.
 *
 * This widget supports the options from the parent class.
 *
 * <h3>Options</h3>
 * This widget supports these options:
 * <ul>
 *   <li><b>type</b> - The field type.  Supported values: 'password'. </li>
 *   <li><b>readOnly</b> - If true, then the text field is not editable (<b>default</b>: false). </li>
 * </ul>
 */
class TextFieldWidget extends BaseLabeledFieldWidget {
  /**
   * Basic constructor.
   * @param widgetContext The widget context this widget is operating in.  Includes URI, parameters, marker etc.
   */
  TextFieldWidget(WidgetContext widgetContext) {
    super(widgetContext)
  }


  /**
   * Builds the field content widget itself.  This is the element to the right of the label.
   * This base class builds a label (readOnly) or text input field.
   * @param id The field ID.
   * @param value The value
   * @return The text for field element.
   */
  String buildWidget(String id, value) {
    if (widgetContext.readOnly) {
      return """{view: "label", id: "$id", label: "${escapeForJavascript(formatForDisplay(value), true)}"}"""
    } else {
      def iWidthS = ''
      def type = (String) widgetContext.parameters.type ?: ''
      if (getMaxLength()) {
        def iWidth = calculateFieldWidth(getMaxLength())
        iWidthS = """,inputWidth: tk.pw("${iWidth}em")"""
      }
      def attrs = buildAttributes(type)
      if (widgetContext.parameters.width) {
        // Override the input width above
        def width = calculateFieldWidth(Integer.valueOf((String) widgetContext.parameters.width))
        iWidthS = """,inputWidth: tk.pw("${width}em")"""
      }
      def cssS = widgetContext.error ? ',css: "webix_invalid" ' : ''
      //def valueS = escape(formatForDisplay(value))
      def valueS = escapeForJavascript(formatForDisplay(value))
      if (type == 'password-no-auto') {
        type = 'password'
      }
      def req = required ? ',required: true' : ''

      def typeS = widgetContext.parameters.type ? """,type: "${type}" """ : ''
      return """{view: "text", id: "$id", name: "$id", value: "$valueS" $cssS $iWidthS$attrs $typeS $req}"""
    }
  }

  /**
   * Builds the attributes clause for the widget's max input length and other attributes for the field.
   * @param type The field type ('password' or 'password-no-auto' supported).
   * @return The attributes clause.
   */
  String buildAttributes(String type) {
    def sb = new StringBuilder()
    if (type == 'password-no-auto') {
      sb << 'autocomplete:"new-password"'
    }

    def maxLength = getMaxLength()
    if (maxLength) {
      if (sb.size()) {
        sb << ','
      }
      sb << "maxlength: $maxLength"
    }

    if (id) {
      if (sb.size()) {
        sb << ','
      }
      sb << """id: "$id" """
    }

    if (sb.size()) {
      return """,attributes: {${sb.toString()}}"""
    } else {
      return ''
    }
  }

  /**
   * Determines the max number of characters this field can contain.
   * @return The max.  Null is possible.
   */
  Integer getMaxLength() {
    return widgetContext.fieldDefinition?.maxLength
  }

  /**
   * Formats the given value for display in the text field.
   * @param value The value to format.
   * @return The formatted value.
   */
  String formatForDisplay(Object value) {
    return GlobalUtils.toStringLocalized(value)
  }
}