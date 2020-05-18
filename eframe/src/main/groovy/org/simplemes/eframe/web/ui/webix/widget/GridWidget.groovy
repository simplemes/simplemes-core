/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.widget

import groovy.util.logging.Slf4j
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.JavascriptUtils
import org.simplemes.eframe.preference.ColumnPreference
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.web.PanelUtils
import org.simplemes.eframe.web.ui.webix.DomainToolkitUtils
import org.simplemes.eframe.web.ui.webix.freemarker.BaseMarker

/**
 * The base Grid widget class.  Produces the UI elements needed for a Grid (list) element.
 * <p>
 * <h3>Options</h3>
 * The options supported for this widget include:
 * <ul>
 *   <li><b>readOnly</b> - If true, then the text field is not editable (<b>default</b>: false). </li>
 *   <li><b>columns</b> - The list of columns to display (<b>default</b>: fieldOrder from domain class). </li>
 *   <li><b>totalWidth</b> - The total amount (percent) of the screen width available for the columns.  Used to define default width. (<b>Default</b>: 60). </li>
 *   <li><b>height</b> - The height the grid to cover (<b>Default</b>: '30%'). </li>
 *   <li><b>label</b> - The text label (<b>Default:</b> "${fieldName}.label").  Blank means no label. </li>
 * </ul>
 *
 */
@Slf4j
class GridWidget extends BaseLabeledFieldWidget {

  /**
   * The preference(s) to use for this grid.
   */
  PreferenceHolder preference

  /**
   * The primary sort column for the list.
   */
  String sortColumn

  /**
   * The sorting direction (sec or desc).
   */
  String sortDirection = 'asc'

  /**
   * The default height for this grid (<b>Default:</b> '30%').
   */
  static final String DEFAULT_HEIGHT = '30%'

  /**
   * The default total width to use for the column widths (<b>Default</b>: 60.0).
   * The initial columns widths are spread out of this percentage of the width (3 columns -> 20% each column).
   */
  static final BigDecimal DEFAULT_TOTAL_WIDTH = 60.0

  /**
   * Basic constructor.
   * @param widgetContext The widget context this widget is operating in.  Includes URI, parameters, marker etc.
   */
  GridWidget(WidgetContext widgetContext) {
    super(widgetContext)
    if (widgetContext?.parameters?.readOnly) {
      // Make sure the marker parameter readOnly is used for the widget.
      widgetContext.readOnly = ArgumentUtils.convertToBoolean(widgetContext?.parameters?.readOnly)
    }
  }

  /**
   * Loads the preferences for the grid/page.
   */
  void loadPreferences() {
    def s = widgetContext.uri
    def elementS = id ?: 'none'
    preference = PreferenceHolder.find {
      page s
      user SecurityUtils.currentUserName
      element elementS
    }

  }

  /**
   * Builds the field content widget itself.  This is the element to the right of the label.
   * This base class builds a label (readOnly) or text input field.
   * @param id The field ID.
   * @param value The value
   * @return The text for field element.
   */
  @Override
  String buildWidget(String id, Object value) {
    loadPreferences()
    //println "value = $value"
    def referenceClass = widgetContext.fieldDefinition.referenceType
    def columns = getColumns((Map) widgetContext.parameters, referenceClass)
    calculateSortOrder(columns)
    value = sortData((Collection) value)
    def height = widgetContext?.parameters?.height ?: DEFAULT_HEIGHT
    builder << """
    { view: "datatable",
      id: "${id}",
      height: tk.ph("$height"),
      headerRowHeight: tk.ph('1em'),
      resizeColumn: {size: 6, headerOnly: true},
      dragColumn: true,
      type: {readOnlyCheckbox: tk._readOnlyCheckbox},
      editable: ${widgetContext?.readOnly ? 'false' : 'true'},
      select: 'row',
      ${DomainToolkitUtils.instance.buildTableDataParser(referenceClass, columns, (Map) widgetContext.parameters)}
      columns: [ ${DomainToolkitUtils.instance.buildTableColumns(referenceClass, columns, buildColumnOptions(columns))}] ,
      data: [ ${buildRowData(columns, (Collection) value)}]
    }
    ${buildAddRemoveButtons()}
    """

    // Add the setup javascript logic to be run after the field definition.
    def post = new StringBuilder()

    post << buildKeyHandlers()
    post << buildAutoSelectFirstRow((Collection) value)
    post << buildMarkSorting()
    post << buildColumnSizingEventHandler()
    post << buildColumnSortEventHandler(referenceClass)
    post << buildAddButtonHandler(columns)
    post << buildRegisterForSubmission()


    widgetContext.markerCoordinator.addPostscript(post.toString())

    return builder.toString()
  }

  /**
   * Gets the effective list of columns to be processed.
   * @param parameters The Marker parameters.  Supports the 'columns' list (comma-delimited list).
   * @param domainClass The domain class (uses fieldOrder).
   * @return The effective list of columns.
   */
  private List<String> getColumns(Map parameters, Class domainClass) {
    def columns = parameters?.columns?.tokenize(',')
    def fields = columns ?: DomainUtils.instance.getStaticFieldOrder(domainClass)
    if (!fields) {
      def s = "A valid column list must be defined. This must be passed in options.columns or a static fieldOrder in the domain ${domainClass} "
      throw new IllegalArgumentException(s)
    }
    // now, remove any tab panels
    fields.removeAll { PanelUtils.isPanel(it) }
    return fields
  }


  /**
   * Build the options for the buildTableColumns() call, with default sizes from the preferences.
   * @param columns The list of columns to display.
   * @return A map with the desired options for the columns.
   */
  Map buildColumnOptions(List<String> columns) {
    def res = (Map) widgetContext.parameters.clone()
    res._widgetContext = widgetContext
    def widths = [:]
    res.widths = widths
    res.totalWidth = DEFAULT_TOTAL_WIDTH
    // If the label is not displayed, then add some to the total width available to spread over the columns
    if (widgetContext?.parameters?.label == '') {
      res.totalWidth += 20.0
    }

    res.keyHyperlink = false
    res.sort = true

    // Add any user preferences that have widths.
    for (column in columns) {
      def pref = preference[column]
      if (pref instanceof ColumnPreference) {
        widths[column] = pref.width
      }
    }

    return res
  }

  /**
   * Builds the row data (in JS object format) for the grid.
   * @param columns The list of columns to build in the results.
   * @param values The data value rows.
   * @return The javascript for the data.
   */
  String buildRowData(List<String> columns, Collection values) {
    def fieldDefs = DomainUtils.instance.getFieldDefinitions(widgetContext.fieldDefinition.referenceType)
    def sb = new StringBuilder()
    for (value in values) {
      if (value) {
        if (sb) {
          sb << ",\n"
        }

        sb << """{id: "${value.uuid}",_dbId: "${value.uuid}"  """
        for (key in columns) {
          def fDef = fieldDefs[key]
          def s = JavascriptUtils.formatForObject(value[key], fDef.format)
          // For combobox fields, use the display value in readOnly mode
          def format = fDef?.format
          if (format?.getGridEditor(fDef) == 'combo' && widgetContext.readOnly) {
            def displayValue = GlobalUtils.toStringLocalized(value[key])
            s = "\"${escapeForJavascript(displayValue)}\""
          }
          if (s != null) {
            sb << """,$key: $s """
          }
        }
        sb << """}\n"""
      }
    }
    return sb.toString()
  }


  /**
   * Sorts the data, if a user-sorting option is in needed.
   * Otherwise, leaves the original values list unchanged.
   * @param values The list of values.
   * @return The list (may be sorted).
   */
  Object sortData(Collection values) {
    if (sortColumn && values) {
      def newList = []
      newList.addAll(values)
      values = newList
      values = values.sort { a, b ->
        def res
        if (sortDirection == 'asc') {
          res = a[sortColumn] <=> b[sortColumn]
        } else {
          res = b[sortColumn] <=> a[sortColumn]
        }
        res
      }
    }
    return values
  }


  /**
   * Determines the sorting from the user preferences.
   * @param columns The list of columns to display.
   */
  void calculateSortOrder(List<String> columns) {
    for (column in columns) {
      def pref = preference[column]
      if (pref instanceof ColumnPreference && pref.sortLevel) {
        // This column is the sort column
        sortColumn = column
        sortDirection = pref.sortAscending ? 'asc' : 'desc'
      }
    }
  }

  /**
   * Build the event handler that reacts to columns resizing
   */
  String buildColumnSizingEventHandler() {
    return """    table${id}.attachEvent("onColumnResize", function(id,newWidth,oldWidth,user_action) {
      if (user_action) {
        tk._columnResized("$id",window.location.pathname,id,newWidth);
      }
    });
    """
  }

  String buildKeyHandlers() {
    return """
      var table$id = ${$$}("$id"); 
      webix.UIManager.removeHotKey('tab', table$id);
      webix.UIManager.removeHotKey('shift-tab', table$id);
      webix.UIManager.addHotKey('tab', tk._gridForwardTabHandler, table$id);
      webix.UIManager.addHotKey('shift-tab', tk._gridBackwardTabHandler, table$id);
      webix.UIManager.addHotKey('space', tk._gridStartEditing, table$id);
      webix.UIManager.addHotKey('enter', tk._gridStartEditing, table$id);
      webix.UIManager.addHotKey('alt+a', ${id}AddRow, table$id);
    """
  }

  /**
   * Returns a focus handle that auto selects the first row.
   * @param value The list of values.
   * @return The script to auto select onFocus.
   */
  String buildAutoSelectFirstRow(Collection value) {
    if (!(value?.size())) {
      return ''
    }

    if (widgetContext.readOnly) {
      return ''
    } else {
      def rowId = value[0].uuid
      return """
        table${id}.select("$rowId");
      """
    }
  }

  /**
   * Builds the script to mark the default sorting column/order.
   * @return The script.
   */
  String buildMarkSorting() {
    if (sortColumn) {
      return """  table${id}.markSorting("${sortColumn}","${sortDirection}");
        """
    }
    return ''
  }

  /**
   * Build the event handler that reacts to columns sorting.
   * @param referenceClass The domain class to sort.
   */
  String buildColumnSortEventHandler(Class referenceClass) {
    def defaultSortField = DomainUtils.instance.getPrimaryKeyField(referenceClass) ?: ''

    // This sends the results to the server as a gui state change, then clears the default sort order
    // from the URL.
    return """    table${id}.attachEvent("onBeforeSort", function(by, dir, as) {
      tk._columnSorted("$id",window.location.pathname,by,dir,"$defaultSortField");
      return true;
    });
    """
  }

  /**
   * Builds the add/remove buttons for the table.
   * @return The add/remove buttons (if needed).
   */
  String buildAddRemoveButtons() {
    if (widgetContext?.readOnly) {
      return ''
    } else {
      def addTip = GlobalUtils.lookup('addRow.tooltip')
      def deleteTip = GlobalUtils.lookup('deleteRow.tooltip')
      return """
      ,{ margin: 0, rows: [
        { id: "${id}Add", view: "button", click: '${id}AddRow()', tooltip: "$addTip", width: 40, height: 40, type: "icon", icon: "fas fa-plus-square", align: "top" },
        { id: "${id}Remove", view: "button", click: 'tk._gridRemoveRow(${$$}("$id"))', tooltip: "$deleteTip", width: 40, height: 40, type: "icon", icon: "fas fa-minus-square", align: "top" }]
      }
      """
    }
  }

  /**
   * Builds the add/remove button handle functions for the table.
   * @param columns The list of columns to display.
   * @return The add/remove buttons (if needed).
   */
  String buildAddButtonHandler(List<String> columns) {
    // Build a default row.
    def referenceClass = widgetContext.fieldDefinition.referenceType
    def row = referenceClass.newInstance()
    def defaultRow = Holders.objectMapper.writeValueAsString(row)

    /**
     * Build any custom logic from the tag for default values.
     *
     */
    def logic = new StringBuilder()

    // Add any custom logic for default values.
    for (column in columns) {
      def name = "$column${BaseMarker.FIELD_SPECIFIC_PARAMETER_DELIMITER}default"
      if (widgetContext.parameters[name]) {
        def defaultScript = widgetContext.parameters[name]
        if (!defaultScript.contains('return')) {
          defaultScript = 'return ' + defaultScript + ";"
        }
        logic << """
          function ${column}Default(){$defaultScript} 
          rowData.$column=${column}Default();
        """
      }

    }

    return """function ${id}AddRow() {
      var rowData = $defaultRow;
      var gridName = "$id";
      ${logic}
      tk._gridAddRow(${$$}("$id"),rowData)
    }
    """
  }

  /**
   * Returns the post-script code to register this form for submission.
   * @return The script.
   */
  String buildRegisterForSubmission() {
    if (widgetContext.readOnly) {
      return ""
    } else {
      return """ efd._registerInlineGridName("${id}");"""
    }
  }
}
