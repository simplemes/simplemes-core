package org.simplemes.eframe.web.ui.webix.freemarker

import groovy.util.logging.Slf4j
import org.simplemes.eframe.custom.ExtensibleFieldHelper
import org.simplemes.eframe.data.FieldDefinitions
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.web.PanelUtils
import org.simplemes.eframe.web.ui.WidgetFactory

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Provides the basic behavior for the efShow, efCreate and efEdit definition page Freemarker markers.
 * This creates the HTML/JS needed to process a single top-level domain object with controls to allow
 * editing and other actions on the domain.
 */
@Slf4j
abstract class BaseDefinitionPageMarker extends BaseMarker {

  /**
   * The field definitions for the top-level domain class.
   */
  FieldDefinitions fieldDefinitions

  /**
   * The fields to be displayed, including tab panels.
   */
  List<String> fieldsToDisplay

  /**
   * The key fields for header section.
   */
  List<String> keyFields

  /**
   * The fields to be displayed in a specific panel.  Stores the list of fields per panel.
   */
  Map<String, List<String>> fieldsByPanel

  /**
   * The errors from the domain object's latest validation.
   */
  def errors

  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    id = markerContext?.markerCoordinator?.formID ?: parameters.id
    determineFieldsToDisplay()

    markerContext?.markerCoordinator?.others[FormMarker.COORDINATOR_TOOLBAR] = buildToolbar()


    def pre = """
        ${buildFunctions()}
      """
    markerContext?.markerCoordinator?.addPrescript(pre)
    def s = """
          ${buildKeySection()}
          ${buildBody()}
          ${buildFooter()}
      """

    def post = """
      ${buildDefaultPanelSelection()}
      ${buildPostScript()}
      ef.loadDialogPreferences();
    """
    markerContext?.markerCoordinator?.addPostscript(post)

    write(s)
  }

  /**
   * Determines which fields to display.  Stores them in the fieldsToDisplay and possibly in the
   * fieldsByPanel map.
   * @return
   */
  def determineFieldsToDisplay() {
    // Now, build the fields to display
    fieldDefinitions = ExtensibleFieldHelper.instance.getEffectiveFieldDefinitions(domainClass, domainObject)
    keyFields = DomainUtils.instance.getKeyFields(domainClass)
    fieldsToDisplay = getFieldsToDisplay(domainClass)

    // Remove the key fields so they can be displayed in the header
    keyFields.each { fieldsToDisplay.remove(it) }

    // Figure out the panels (if any) needed.
    fieldsByPanel = PanelUtils.organizeFieldsIntoPanels(fieldsToDisplay)

    if (domainObject) {
      errors = GlobalUtils.lookupValidationErrors(domainObject)
    }
  }

  /**
   * Builds the javascript functions used by the page.
   * @return The Javascript functions.
   */
  String buildFunctions() {
    return ''
  }


  /**
   * Builds the fields for the given panel (optional).
   * @return The Javascript needed for the fields.
   */
  String buildKeySection() {
    return buildFields(keyFields, true) + ','
  }


  /**
   * Builds the fields for the given panel (optional).
   * @return The Javascript needed for the fields.
   */
  String buildBody() {
    def body = new StringBuilder()

    if (fieldsByPanel) {
      // Build the tab panels as defined by the field order.
      body << buildPanelStart()
      for (panel in fieldsByPanel.keySet()) {
        body << buildPanel(panel, fieldsByPanel[panel])
        // calls buildFields(panel)
      }
      body << buildPanelEnd()
    } else {
      // No tab panels, so just dump the fields
      body << buildFields(fieldsToDisplay)
    }


    return body.toString()
  }

  /**
   * Builds the start of the tabbed-panel element.
   * @return The Javascript needed to being the tabbed panel view.
   */
  String buildPanelStart() {
    def s = """
      { view: "tabview", id: 'theTabView', paddingX: 10,
        tabbar: {
          tabMargin: 6,
          on: {
            onChange: function (x) {
              ef._storeLocal('theTabView', x);
            }
          },
          bottomPadding: -10
        },
        multiview: {
          fitBiggest: true
        },
        cells: [
    """
    return s
  }

  /**
   * Builds the given tabbed-panel contents.
   * @param panel The tabbed panel to build.
   * @return The Javascript needed for the content of one tabbed panel.
   */
  String buildPanel(String panel, List<String> fields) {
    def title = lookup("${panel}.panel.label")
    if (title.contains('.panel.label')) {
      // No entry in the .properties bundle, so use the main name as-is.  Allows custom panel
      // names to be used in the definition page Configuration Editor.
      title = panel
    }
    def s = """
      { header: '$title',
        body: {
          id: '${panel}Body',
          rows: [
            ${buildFields(fields)}
          ]
        }
      },
    """
    return s
  }

  /**
   * Builds the end of the tabbed-panel element.
   * @return The Javascript needed to being the tabbed panel view.
   */
  String buildPanelEnd() {
    def s = """
        ]
      }
    """
    return s
  }

  /**
   * Builds the fields for the given panel (optional).
   * @param panel The panel name (Optional).  If null, then all fields will be generated.
   * @param keyField If true, then this field is created as a key field (more centered on tabbed panels).
   * @return The Javascript needed for the fields.
   */
  String buildFields(List<String> fields, Boolean keyField = false) {
    def widgetScript = new StringBuilder()
    for (fieldName in fields) {
      def fieldDefinition = fieldDefinitions[fieldName]
      //println "fieldDefinition = $fieldDefinition"
      if (!fieldDefinition) {
        throw new MarkerException("No field definition for $fieldName in $domainClass", this)
      }
      def widgetContext = buildWidgetContext(fieldDefinition)
      widgetContext.object = domainObject
      if (errors) {
        widgetContext.error = (errors[fieldName] != null)
      }
      // Move the key field to the center of the tabbed panel
      if (fieldsByPanel && keyField) {
        widgetContext.parameters.labelWidth = '30%'
      }
      if (fieldDefinition.required) {
        widgetContext.parameters.required = 'true'
      }
      addFieldSpecificParameters(widgetContext, fieldName, (Map) parameters)
      def widget = WidgetFactory.instance.build(widgetContext)
      if (widgetScript) {
        widgetScript << ",\n"
      }
      widgetScript << widget.build()
      log.trace('buildFields() field={}, widgetContext={}, widget={}', fieldName, widgetContext, widget)
    }

    return widgetScript.toString()

  }

  /**
   * Builds the javascript needed to select the right default panel.
   * @return The select javascript (if panels).
   */
  String buildDefaultPanelSelection() {
    def s = ''
    if (fieldsByPanel) {
      s = """
            var selectedTab = ef._retrieveLocal('theTabView');
            if (selectedTab) {
              ${$$}("theTabView").getTabbar().setValue(selectedTab); 
            }
          """
    }
    return s
  }

  /**
   * Builds the footer for the definition page (e.g. the place for buttons outside of the tabbed panel).
   * @return The footer.
   */
  String buildFooter() {
    return ''
  }

  /**
   * Builds the toolbar element for the page.
   * @return The toolbar element.
   */
  String buildToolbar() {
    return ''
  }

  /**
   * Builds the script section for use after the UI elements are built.
   * @return The post script.
   */
  String buildPostScript() {
    return ''
  }


  /**
   * Determines the effective fields to display.
   * Uses the fieldOrder and marker parameter override to calculate this.
   * @param clazz The object to get the fieldOrder from.
   * @param parameterName The name of the marker parameter to use for the field list override (<b>Default</b>: 'fields').
   * @return The list of fields.
   */
  protected List<String> getFieldsToDisplay(Class clazz, String parameterName = 'fields') {
    def originalList = null
    if (parameters[parameterName]) {
      originalList = unwrap(parameters[parameterName]).tokenize(',')
    }
    return ExtensibleFieldHelper.instance.getEffectiveFieldOrder(clazz, originalList)
  }

  /**
   * The field to place the focus in.
   */
  String focusFieldName


  /**
   * Builds the script to set initial focus to a given field (focusFieldName).
   * @return
   */
  String buildFocusScript() {
    // Find the first field that has an error and use that as the focus field first.
    if (errors) {
      for (fieldName in fieldsToDisplay) {
        if (errors[fieldName]) {
          focusFieldName = fieldName
          break
        }
      }
      // Make sure the key field is focus if it has an error since the fieldsToDisplay
      if (keyFields && errors[keyFields[0]]) {
        focusFieldName = keyFields[0]
      }
    }
    // Default to key field, if no other focus found.
    if (keyFields) {
      focusFieldName = focusFieldName ?: keyFields[0]
    }
    def s = ''
    if (focusFieldName) {
      s = """${$$}("$focusFieldName").focus();\n"""
    }
    return s
  }

}
