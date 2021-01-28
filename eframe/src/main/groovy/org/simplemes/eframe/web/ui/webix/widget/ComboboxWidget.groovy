package org.simplemes.eframe.web.ui.webix.widget
/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The combo box widget input field for a single selection widget.  Produces the UI elements needed for a combobox to select a
 * single element.
 *
 */
class ComboboxWidget extends TextFieldWidget {

  /**
   * The minimum width of the input section of the field (20).
   */
  public static final Integer MINIMUM_WIDTH = 20

  /**
   * Keep track of the max width of any value in the list.
   */
  int maxValueWidth = MINIMUM_WIDTH

  /**
   * Basic constructor.
   * @param widgetContext The widget context this widget is operating in.  Includes URI, parameters, marker etc.
   */
  ComboboxWidget(WidgetContext widgetContext) {
    super(widgetContext)
  }

  /**
   * Builds the field content widget itself.
   * This sub class builds a combobox.
   * @param id The field ID.
   * @param value The value
   * @return The text for field element.
   */
  @Override
  String buildWidget(String id, Object value) {
    if (widgetContext.readOnly) {
      // Display-only
      return super.buildWidget(id, value)
    } else {
      def format = widgetContext.fieldDefinition.format
      def s = ''
      if (value != null) {
        s = format.encode(value, widgetContext.fieldDefinition)
      }
      def values = buildValues()
      def width = """,inputWidth: tk.pw("${adjustFieldCharacterWidth(inputWidth)}em")"""
      def options = """, editable: true"""
      def cssS = widgetContext.error ? ',css: "webix_invalid" ' : ''
      if (widgetContext.parameters.onChange) {
        options += ",on:{onChange(newValue, oldValue){$widgetContext.parameters.onChange}}"
      }
      return """{view: "${view}", id: "$id", name: "$id", value: "$s" $options $cssS $width $values}"""
    }
  }

  /**
   * Returns the toolkit view widget type (e.g. 'combo').
   * @return The view type.
   */
  String getView() {
    return 'combo'
  }

  /**
   * Build the value options for the combobox.
   * @return The list of options as a javascript element.  The javascript elements contain 'id' and 'value' elements.
   */
  String buildValues() {
    //def clazz = widgetContext.fieldDefinition.type
    def format = widgetContext.fieldDefinition.format
    def validValues = format.getValidValues(widgetContext.fieldDefinition)
    def sb = new StringBuilder()
    for (value in validValues) {
      if (sb) {
        sb << "\n,"
      }
      def s = value.toStringLocalized()
      maxValueWidth = Math.max(s.size(), maxValueWidth)
      sb << """{id: "${value.id}", value: "${escapeForJavascript(s)}"}"""
    }

    return """,options: [
        ${sb}
      ]
    """
  }

  /**
   * Determines the size of this widget's input field (in characters).
   * @return The width.
   */
  int getInputWidth() {
    // Use the real width, up to a max value.
    return Math.min(getMaxValueWidth(), 40)
  }


}
