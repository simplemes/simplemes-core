package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.preference.ColumnPreference
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.web.PanelUtils
import org.simplemes.eframe.web.ui.UIDefaults
import org.simplemes.eframe.web.ui.webix.DomainToolkitUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The base definition list widget class.  Produces the UI elements needed for a list definition page.
 * This includes a toolbar.  This is not suitable for use inside of a <pre>&lt;@efForm/&gt;</pre> marker.
 *
 */
class DefinitionListWidget extends BaseWidget {

  /**
   * The URL to use to retrieve tha values.
   */
  String dataUrl = '?'

  /**
   * The controller URI path for the controller.  This is the URI prefix for all actions under the controller.
   */
  String controllerRoot = '?'

  /**
   * The preference(s) to use for this grid.
   */
  PreferenceHolder preference

  /**
   * Basic constructor.
   * @param widgetContext The widget context this widget is operating in.  Includes URI, parameters, marker etc.
   */
  DefinitionListWidget(WidgetContext widgetContext) {
    super(widgetContext)

    // Force widget to readOnly.
    widgetContext.readOnly = true

    // This widget can use the controller option on the marker.
    controllerOverrideAllowed = true

    preference = PreferenceHolder.find {
      page widgetContext.uri
      user SecurityUtils.currentUserName
      element id
    }

    if (controllerClass) {
      controllerRoot = ControllerUtils.instance.getRootPath(controllerClass)
      dataUrl = buildURL(controllerRoot)
    }
  }

  /**
   * Builds the string for the UI elements.
   * @return The UI page text.
   */
  @Override
  CharSequence build() {
    buildHTMLStart()
    buildLayout()
    buildToolbar()
    buildTable()
    buildPager()
    return generate()
  }

  /**
   * Determines the desired page size.
   * @return
   */
  Integer getPageSize() {
    return UIDefaults.PAGE_SIZE
  }

  /**
   * Builds the HTML tags for the start.
   */
  def buildHTMLStart() {
    builder << """<div id="$id"></div>\n"""
    builder << """<script>\n"""
    builder << """efd._checkURLMessages();\n"""

    addClosingText("""</script>\n""")

    // Make sure the event handlers are within the script page.
    buildColumnSizingEventHandler()
    buildColumnSortEventHandler()
    buildSortingMarkEventHandler()
    buildSearchEnterKeyEventHandler()
  }

  /**
   * Builds the beginning of the layout script.
   */
  void buildLayout() {
    builder << """
    webix.ui({
      container: "$id",
      type: "space", margin: 4, id: "${id}Layout", rows: [
    """

    addClosingText("""\n]});\n""")
  }

  /**
   * Builds the toolbar section of the grid.
   */
  void buildToolbar() {
    def domainName = NameUtils.toDomainName(domainClass) ?: '?'
    def label = lookup('create.button.label', null, lookup("${domainName}.label"))
    def tooltip = lookup('create.button.tooltip', null, lookup("${domainName}.label"))

    def cClick = """click: "window.location='$controllerRoot/create'","""
    def cIcon = """type: "icon",icon: "fas fa-plus-square","""

    builder << """
      { view: "toolbar",
        id: "${id}Toolbar",
        elements: [
          {view: "text", id: "${id}Search", width: tk.pw('20%'), placeholder: "${lookup('search.label')}"},
          {view: "label", template: "<span>&nbsp;</span>"},
          {view: "button", id: "${id}Create", autowidth: true,$cIcon $cClick label: "$label",tooltip: "$tooltip" }
        ]
      },
    """
  }

  /**
   * Builds the table creation javascript.
   */
  void buildTable() {
    def columns = getColumns((Map) widgetContext.parameters, domainClass)
    builder << """
      {view: "datatable",
       height: tk.ph("74%"),
       id: "$id",
       resizeColumn: {size: 6, headerOnly: true},
       dragColumn: true,
       select: "row",
       datafetch: $pageSize,
       type: {readOnlyCheckbox: tk._readOnlyCheckbox},
       pager: "${id}Pager",
       autowidth: false,
       url: "$dataUrl",
       ${DomainToolkitUtils.instance.buildTableDataParser(domainClass, columns, (Map) widgetContext.parameters)}
       columns: [ ${DomainToolkitUtils.instance.buildTableColumns(domainClass, columns, buildColumnOptions(columns))}
       ]
      }
    """
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
    def widths = [:]
    res.widths = widths
    res.sort = 'server'
    res._widgetContext = widgetContext
    res.displayValueForCombo = true
    res._controllerRoot = controllerRoot

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
   * Builds the pager view element.
   */
  void buildPager() {
    builder << """, {view: "pager", id: "${id}Pager", size: $pageSize, group: 5}"""
  }

  /**
   * Build the event handler that marks the sort direction based on the server's list() result.
   */
  void buildSortingMarkEventHandler() {
    addClosingText("""    ${$$}("$id").data.attachEvent("onStoreLoad", function (driver, data) {
      if (data.sort != undefined) {
        ${$$}("$id").markSorting(data.sort, data.sortDir);
      }
    });
    """)
  }

  /**
   * Build the event handler that performs the search.
   */
  void buildSearchEnterKeyEventHandler() {
    addClosingText("""    ${$$}("${id}Search").attachEvent("onEnter", function (id) {
      return true;
    });
    """)

  }

  /**
   * Build the event handler that reacts to columns resizing
   */
  void buildColumnSizingEventHandler() {
    addClosingText("""    ${$$}("${id}").attachEvent("onColumnResize", function(id,newWidth,oldWidth,user_action) {
      if (user_action) {
        tk._columnResized("$id",window.location.pathname,id,newWidth);
      }
    });
    """)
  }

  /**
   * Build the event handler that reacts to columns sorting.
   */
  void buildColumnSortEventHandler() {
    def defaultSortField = DomainUtils.instance.getPrimaryKeyField(domainClass) ?: ''

    // This sends the results to the server as a gui state change, then clears the default sort order
    // from the URL.
    addClosingText("""    ${$$}("${id}").attachEvent("onBeforeSort", function(by, dir, as) {
      tk._columnSorted("$id",window.location.pathname,by,dir,"$defaultSortField");
      ${$$}("$id").url="$controllerRoot/list";
      return true;
    });
    """)
  }

  /**
   * Builds the URL to retrieve the data.
   * @param controllerName
   * @return
   */
  private String buildURL(String controllerName) {
    def sort = ''

    def sortColumnPreference = (ColumnPreference) preference?.settings?.find { it.sortLevel > 0 }
    if (sortColumnPreference) {
      sort = "?sort=${sortColumnPreference.column}&order=${sortColumnPreference.sortAscending ? 'asc' : 'desc'}"
    }

    return "$controllerName/list${sort}"
  }


}
