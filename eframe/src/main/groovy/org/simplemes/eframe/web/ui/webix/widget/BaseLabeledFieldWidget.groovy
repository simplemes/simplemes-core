package org.simplemes.eframe.web.ui.webix.widget


import org.simplemes.eframe.web.ui.UIDefaults

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The basic field widget that has a label.  Produces the UI elements needed for a simple field element.
 *
 * <h3>Options</h3>
 * This widget supports these options:
 * <ul>
 *   <li><b>id</b> - The field ID/Name. </li>
 *   <li><b>label</b> - The text label (<b>Default:</b> "${fieldName}.label").  Blank means no label. </li>
 *   <li><b>value</b> - The initial value of the field. </li>
 * </ul>
 *
 */
class BaseLabeledFieldWidget extends BaseWidget {

  /**
   * True if the field should be marked as required in the UI.
   */
  boolean required = false

  /**
   * Basic constructor.
   * @param widgetContext The widget context this widget is operating in.  Includes URI, parameters, marker etc.
   */
  BaseLabeledFieldWidget(WidgetContext widgetContext) {
    super(widgetContext)
    if (widgetContext.parameters.required) {
      //noinspection GroovyAssignabilityCheck
      required = Boolean.valueOf(widgetContext.parameters.required)
    }
  }

  /**
   * Builds the string for the UI elements.
   * @return The UI page text.
   */
  @Override
  CharSequence build() {
    def fieldDefinition = widgetContext.fieldDefinition
    def name = fieldDefinition?.name ?: 'field'
    id = widgetContext.parameters.id ?: name
    /*
    Precedence
      marker Label
      custom label
      name.label
     */
    def labelID = "${id}Label"
    def labelKey = fieldDefinition?.getLabel() ?: name + '.label'
    if (widgetContext.parameters.label != null) {
      // The marker label parameter can override the default label to be ''.
      labelKey = widgetContext.parameters.label
    }
    def labelSrc = ''
    if (labelKey) {
      def label = lookup((String) labelKey)
      def req = required ? '*' : ''
      labelSrc = """ {view: "label", id: "$labelID", label: "$req$label", ${
        buildStandardLabelWidth()
      }, align: "right"},"""
    }
    def object = widgetContext.object
    def value = null
    if (object && fieldDefinition) {
      value = fieldDefinition.getFieldValue(object)
    }

    def src = """
    { margin: ${UIDefaults.FIELD_LABEL_GAP},
      cols: [
        $labelSrc
        ${buildWidget((String) id, value)}
        ${widgetContext.innerContent ?: ''}
      ]
    }
    """
    return src.toString()
  }

  /**
   * Builds the field content widget itself.  This is the element to the right of the label.
   * This base class builds a label (readOnly) or text input field.
   * @param id The field ID.
   * @param value The value
   * @return The text for field element.
   */
  String buildWidget(String id, value) {
    return """{view: "label", id: "$id", label: "${escapeForJavascript(formatForDisplay(value), true)}"}"""
  }

  /**
   * Formats the given value for display in the text field.
   * @param value The value to format.
   * @return The formatted value.
   */
  String formatForDisplay(Object value) {
    return value?.toString() ?: ''
  }


  /**
   * Determine the display width (in characters) for the given maxLength of the input field.
   * This gradually reduces the display width as the maxLength increases up to a hard max.
   * @param maxLength
   * @return The adjust display width (in em characters).
   */
  static int calculateFieldWidth(int maxLength) {
    def nChars = 60
    if (maxLength <= 3) {
      nChars = 3
    } else if (maxLength <= 30) {
      nChars = maxLength
    } else if (maxLength <= 60) {
      nChars = 30
    } else if (maxLength <= 80) {
      nChars = 40
    }
    return (nChars * 3) / 4
  }

}
