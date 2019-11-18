package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.web.ui.webix.widget.ToolbarWidget

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Provides the efCreate handlebars marker implementation.
 * This creates the HTML/JS needed to create a single top-level domain object with controls to allow
 * saving and related actions.
 */
class CreateMarker extends BaseDefinitionPageMarker {

  /**
   * The URI for this edit page (e.g. /user).
   */
  String rootURI

  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    id = id ?: parameters.id ?: 'create'
    if (markerContext?.uri) {
      rootURI = ControllerUtils.instance.determineBaseURI(markerContext.uri) - "/create"
      markerContext.markerCoordinator.formURL = "${rootURI}/create"
    }
    if (!(markerContext?.markerCoordinator?.formID)) {
      throw new MarkerException("efCreate must be enclosed in an efForm marker.", this)
    }
    super.execute()
  }

  /**
   * Builds the javascript functions used by the page.
   * @return The Javascript functions.
   */
  @Override
  String buildFunctions() {
    return """
      function createSave() {
        efd._createSave('${id}','$rootURI/create')
      }
    """
  }

  /**
   * Builds the toolbar element for the show page.
   * @return The toolbar element.
   */
  String buildToolbar() {
    def widgetContext = buildWidgetContext(null)
    def list = [id: "${id}List", label: 'list.menu.label', icon: 'fa-th-list', link: "${rootURI}"]
    def create = [id   : "${id}Save", label: 'create.menu.label', icon: 'fa-plus-square',
                  click: "createSave"]
    //click: "efd._createSave('${id}Form','$uri/createSave')"]

    widgetContext.parameters.id = "${id}Toolbar"
    widgetContext.parameters.buttons = [list, '', create]
    def toolbarWidget = new ToolbarWidget(widgetContext)

    return toolbarWidget.build().toString()
  }

  /**
   * Builds the footer for the definition page (e.g. the place for buttons outside of the tabbed panel).
   * @return The footer.
   */
  @Override
  String buildFooter() {
    def labelS = """,label: "${lookup("create.menu.label")}" """
    def tooltipS = """,tooltip: "${lookup("create.menu.tooltip")}" """
    def appearanceS = """,click: "createSave", width: 200, type: "iconButton" ,icon: 'fas fa-plus-square' """
    def s = """
      ,{ margin: 15, cols: [
          {},
          {view: "button", id: "${id}SaveBottom" $labelS $tooltipS $appearanceS},
          {}
        ]
      }
    """
    return s
  }

  /**
   * Builds the script section for use after the UI elements are built.
   * @return The post script.
   */
  @Override
  String buildPostScript() {
    return buildFocusScript()
  }
}
