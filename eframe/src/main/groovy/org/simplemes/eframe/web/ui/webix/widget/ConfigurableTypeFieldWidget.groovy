/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.data.ConfigurableTypeInterface
import org.simplemes.eframe.data.FieldDefinitionFactory
import org.simplemes.eframe.data.format.ConfigurableTypeDomainFormat
import org.simplemes.eframe.data.format.DomainReferenceFieldFormat
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.web.ui.UIDefaults
import org.simplemes.eframe.web.ui.WidgetFactory
import org.simplemes.eframe.web.ui.WidgetInterface

/**
 * This is a multi-field widget that supports an optional selection combobox that determines the dynamic fileds
 * display below the combobox.
 *
 */
class ConfigurableTypeFieldWidget extends BaseWidget {

  /**
   * The name of the marker attribute/param that indicates that the combobox for this widget will be read only.
   * The other fields will be left as-is (probably editable).
   */
  public static final String COMBO_READ_ONLY_PARAMETER = '_combo@readOnly'


  /**
   * The (optional) drop-down that will let the user choose the specific type.  This choice
   * controls the dynamic fields displayed below.
   */
  WidgetInterface comboboxWidget

  /**
   * The main field name of the configurable type.  This is the name used for the drop-down.
   */
  String fieldName

  /**
   * Basic constructor.
   * @param widgetContext The widget context this widget is operating in.  Includes URI, parameters, marker etc.
   */
  ConfigurableTypeFieldWidget(WidgetContext widgetContext) {
    super(widgetContext)
    //println "fd ($id) = ${widgetContext.fieldDefinition}"
    fieldName = widgetContext.fieldDefinition.name
    id = fieldName

    // Build a basic field definition for the drop-down
    def w = new WidgetContext(widgetContext, (Map) widgetContext.parameters.clone())
    w.fieldDefinition = FieldDefinitionFactory.copy(w.fieldDefinition, [format: DomainReferenceFieldFormat.instance])
    w.parameters.id = id
    def comboReadOnly = ArgumentUtils.convertToBoolean(w.parameters[COMBO_READ_ONLY_PARAMETER])
    if (comboReadOnly) {
      w.readOnly = true
    }
    comboboxWidget = new ComboboxWidget(w)
  }

  /**
   * Builds the string for the UI elements.
   * @return The UI page text.
   */
  @Override
  CharSequence build() {
    def choices = ConfigurableTypeDomainFormat.instance.getValidValues(widgetContext.fieldDefinition)
    def value = widgetContext.fieldDefinition.getFieldValue(widgetContext.object)
    if (!value) {
      def defaultChoice = choices.find { it.isDefaultChoice() }
      if (defaultChoice) {
        value = defaultChoice.value
        widgetContext.fieldDefinition.setFieldValue(widgetContext.object, value)
      }
    }
    // end remove

    StringBuilder sb = new StringBuilder()
    sb << comboboxWidget.build()
    sb << buildDynamicFieldArea()
    return sb.toString()
  }

  /**
   * Builds the dynamic field area
   * @return The area.
   */
  String buildDynamicFieldArea() {
    // Now build the content for each choice in the combobox drop-down.
    def format = comboboxWidget.widgetContext.fieldDefinition.format
    def defineChoices = new StringBuilder()
    defineChoices << "var ${id}Choices = []; \n"

    def value = widgetContext.fieldDefinition.getFieldValue(widgetContext.object)
    def choices = format.getValidValues(widgetContext.fieldDefinition)

    for (choice in choices) {
      String fieldView = buildFields((ConfigurableTypeInterface) choice.value)

      defineChoices << """${id}Choices["$choice.id"] =$fieldView;\n"""
    }
    //println "defineChoices($value) = $defineChoices"

    def post = """ 
        $defineChoices
        \$\$("${id}").attachEvent("onChange", function(newValue, oldValue) {
          var choice = ${id}Choices[newValue];
          \$\$("${id}Holder").removeView("${id}Content");
          \$\$("${id}Holder").addView(choice, 0);
        });
    """

    widgetContext.markerCoordinator.addPostscript(post)

    // Now, build the content for the current selection.
    def note = lookup('selectConfigType.label', lookup("${fieldName}.label"))
    def width = buildStandardLabelWidth()
    def labelSrc = """ {view: "label", id: "${id}NoteLabel", label: "", ${width}, align: "right"},"""

    def fieldSrc
    if (value) {
      fieldSrc = buildFields((ConfigurableTypeInterface) value)
    } else {
      fieldSrc = """
        { margin: ${UIDefaults.FIELD_LABEL_GAP}, id: "${id}Content",
          cols: [
            $labelSrc
            {view: "label", id: "${id}Note", label: "${escapeForJavascript(note, true)}"}
          ]
        }
    """
    }


    def src = """
    ,{ margin: ${UIDefaults.FIELD_LABEL_GAP}, id: "${id}Holder",
      rows: [
        $fieldSrc 
      ] 
    } 
    """

    return src
  }


  /**
   * Builds the fields for the input panel for the given configurable type value.
   * @param configTypeValue The value to build the input fields for.
   * @return The javascript to build the panel.
   */
  String buildFields(ConfigurableTypeInterface configTypeValue) {
    def widgetScript = new StringBuilder()
    for (field in configTypeValue.determineInputFields(id)) {
      def w = new WidgetContext(widgetContext, (Map) widgetContext.parameters.clone())
      w.object = widgetContext.object
      w.fieldDefinition = field
      w.parameters.required = false
      w.parameters.id = field.name
      def widget = WidgetFactory.instance.build(w)
      if (widgetScript) {
        widgetScript << ",\n"
      }
      widgetScript << widget.build()
    }

    def fieldView = """ {margin: ${UIDefaults.FIELD_LABEL_GAP}, id: "${id}Content",rows: [
        $widgetScript
      ] }"""
    return fieldView
  }


}
