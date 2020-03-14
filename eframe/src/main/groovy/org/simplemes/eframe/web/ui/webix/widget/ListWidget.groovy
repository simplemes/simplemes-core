/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.widget


import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.preference.ColumnPreference
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.web.PanelUtils
import org.simplemes.eframe.web.ui.UIDefaults
import org.simplemes.eframe.web.ui.webix.DomainToolkitUtils

/**
 * The base definition list widget class.  Produces the UI elements needed for a list display
 * <h3>Attributes</h3>
 * The marker attributes (parameters) that are support include:
 * <ul>
 *   <li><b>height</b> - The height (e.g. '74%' or '20em' or '20') </li>
 *   <li><b>paddingX</b> - The padding to the left and right (e.g. '74%' or '20em' or '20') </li>
 *   <li><b>copyParameters</b> - If true, then copy the parameters from the HTTP request to the data URI (unless
 *                               they start with underscore.</li>
 * </ul>
 *
 */
class ListWidget extends BaseWidget {

  /**
   * The controller URI path for the controller.  This is the URI prefix for all actions under the controller.
   */
  String controllerRoot = '?'

  /**
   * The preference(s) to use for this list.
   */
  PreferenceHolder preference

  /**
   * Basic constructor.
   * @param widgetContext The widget context this widget is operating in.  Includes URI, parameters, marker etc.
   */
  ListWidget(WidgetContext widgetContext) {
    super(widgetContext)

    // Force widget to readOnly.
    widgetContext.readOnly = true

    // This widget can use the controller option on the marker.
    controllerOverrideAllowed = true

    String pageSource = requestParameters?.get(ControllerUtils.PARAM_PAGE_SOURCE) ?: widgetContext?.uri
    def idParam = id

    preference = PreferenceHolder.find {
      page pageSource
      user SecurityUtils.currentUserName
      element idParam
    }

    if (controllerClass) {
      controllerRoot = ControllerUtils.instance.getRootPath(controllerClass)
    }
  }

  /**
   * Builds the string for the UI elements.
   * @return The UI page text.
   */
  @Override
  CharSequence build() {
    buildEventHandlers()
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
   * Builds the event handlers (in the post script).
   * @return
   */
  def buildEventHandlers() {
    //widgetContext.markerCoordinator.addPostscript(post)
    buildColumnSizingEventHandler()
    buildColumnSortEventHandler()
    buildSortingMarkEventHandler()
    buildSelectionHandler()
  }

  /**
   * Builds the toolbar section of the list.
   */
  void buildToolbar() {
  }

  /**
   * Builds the table creation javascript.
   */
  void buildTable() {
    def columns = getColumns((Map) widgetContext.parameters, domainClass)

    def heightS = ''
    def height = getSizeAttribute('height') ?: null
    if (height) {
      heightS = "height: tk.ph('$height'),"
    }

    // Build some horizontal spacers, if desired.
    def before = ''
    def after = ''
    def paddingX = getSizeAttribute('paddingX')
    if (paddingX) {
      before = """,{ view: 'form', type: 'clean', margin: 0, padding: 10, cols: [ {width: tk.pw('$paddingX')}"""
      after = """   ,{width: tk.pw('$paddingX')} ] }"""
    }

    builder << """$before
      ,{view: "datatable", id: "$id",$heightS
       resizeColumn: {size: 6, headerOnly: true},
       dragColumn: true,
       select: "row",
       css: "webix_header_border webix_data_border data-table-with-border",
       datafetch: $pageSize,
       type: {readOnlyCheckbox: tk._readOnlyCheckbox},
       pager: "${id}Pager",
       autowidth: false,
       ${buildDataSource(controllerRoot)},
       ${DomainToolkitUtils.instance.buildTableDataParser(domainClass, columns, (Map) widgetContext.parameters)}
       columns: [ ${DomainToolkitUtils.instance.buildTableColumns(domainClass, columns, buildColumnOptions(columns))}
       ]
      }
      $after 
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
   * Build the event handler that handles row select events.
   */
  void buildSelectionHandler() {
    def onSelect = widgetContext?.parameters?.onSelect
    if (onSelect) {
      addPostscriptText("""    ${$$}("$id").attachEvent("onAfterSelect", function (selection) {
        $onSelect(${$$}("$id").getSelectedItem(),"$id");
      });
      """)
    }
  }

  /**
   * Build the event handler that marks the sort direction based on the server's list() result.
   */
  void buildSortingMarkEventHandler() {
    addPostscriptText("""    ${$$}("$id").data.attachEvent("onStoreLoad", function (driver, data) {
      if (data.sort != undefined) {
        ${$$}("$id").markSorting(data.sort, data.sortDir);
      }
    });
    """)
  }

  /**
   * Build the event handler that reacts to columns resizing
   */
  void buildColumnSizingEventHandler() {
    addPostscriptText("""    ${$$}("${id}").attachEvent("onColumnResize", function(id,newWidth,oldWidth,user_action) {
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
    addPostscriptText("""    ${$$}("${id}").attachEvent("onBeforeSort", function(by, dir, as) {
      tk._columnSorted("$id",window.location.pathname,by,dir,"$defaultSortField");
      ${$$}("$id").url="$controllerRoot/list";
      return true;
    });
    """)
  }

  /**
   * Builds the data source element for the datatable.
   * @param controllerName
   * @return The data source (data or url).
   */
  protected String buildDataSource(String controllerName) {
    if (!widgetContext?.parameters?.uri && widgetContext?.parameters?.dataFunction) {
      return "data: ${widgetContext.parameters.dataFunction}()"
    }

    def sort = ''

    def uri = widgetContext?.parameters?.uri ?: "$controllerName/list"

    def sortColumnPreference = (ColumnPreference) preference?.settings?.find { it instanceof ColumnPreference && it.sortLevel > 0 }
    if (sortColumnPreference) {
      sort = "?sort=${sortColumnPreference.column}&order=${sortColumnPreference.sortAscending ? 'asc' : 'desc'}"
    }
    def url = "$uri$sort"
    // copy any request parameters if desired.
    if (getBooleanAttribute('copyParameters')) {
      requestParameters?.each { k, v ->
        if (!k.startsWith('_')) {
          def map = [:]
          map[k] = v
          url = ControllerUtils.instance.buildURI(url, map)
        }
      }
    }

    return """url: "${url}" """
  }

}
