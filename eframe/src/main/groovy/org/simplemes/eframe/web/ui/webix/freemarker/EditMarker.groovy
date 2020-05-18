/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.web.ui.webix.widget.ToolbarWidget

/**
 * Provides the efEdit Freemarker marker implementation.
 * This creates the HTML/JS needed to edit a single top-level domain object with controls to allow
 * saving and related actions.
 */
@SuppressWarnings("unused")
class EditMarker extends BaseDefinitionPageMarker {

  /**
   * The field to place the focus in.
   */
  String focusFieldName

  /**
   * The URI for this edit page (e.g. /user).
   */
  String rootURI

  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    id = parameters.id ?: 'edit'
    if (markerContext?.uri) {
      rootURI = ControllerUtils.instance.determineBaseURI(markerContext.uri) - "/edit"
      markerContext.markerCoordinator.formURL = "${rootURI}/edit"
    }
    if (!markerContext?.markerCoordinator?.formID) {
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
      function editSave() {
        efd._editSave('${id}','$rootURI/edit','${domainObject?.uuid}')
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
    def create = [id   : "${id}Save", label: 'update.menu.label', icon: 'fa-edit',
                  click: "editSave"]

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
    def labelS = """,label: "${lookup("update.menu.label")}" """
    def tooltipS = """,tooltip: "${lookup("update.menu.tooltip")}" """
    def appearanceS = """,click: "editSave", width: 200, type: "iconButton" ,icon: 'fas fa-edit' """
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
