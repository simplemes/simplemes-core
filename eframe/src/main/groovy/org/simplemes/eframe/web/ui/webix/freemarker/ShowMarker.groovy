/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.web.ui.webix.widget.ToolbarWidget
import org.simplemes.eframe.web.ui.webix.widget.WidgetContext

/**
 * Provides the efShow Freemarker marker implementation.
 * This creates the HTML/JS needed to display a single top-level domain object with controls to allow
 * editing and other actions on the domain.
 */
@SuppressWarnings("unused")
class ShowMarker extends BaseDefinitionPageMarker {

  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    id = parameters.id ?: 'show'
    super.execute()
  }

  /**
   * Builds the widget context for this marker.  This avoids embedding knowledge of the marker in the widget classes.
   * @param The field definition for the field this widget is generating the UI for.
   * @return The WidgetContext.
   */
  @Override
  WidgetContext buildWidgetContext(FieldDefinitionInterface fieldDefinition) {
    // This showMarker is always in readOnly mode.
    def w = super.buildWidgetContext(fieldDefinition)
    w.readOnly = true
    return w
  }

  /**
   * Builds the toolbar element for the show page.
   * @return The toolbar element.
   */
  String buildToolbar() {
    def widgetContext = buildWidgetContext(null)
    def uri = '?'
    if (markerContext?.uri) {
      uri = markerContext.uri - "/show/${domainObject?.uuid}"
    }
    def list = [id: "${id}List", label: 'list.menu.label', icon: 'fa-th-list', link: "${uri}"]
    def create = [id: "${id}Create", label: 'create.menu.label', icon: 'fa-plus-square', link: "${uri}/create"]
    def edit = [id: "${id}Edit", label: 'edit.menu.label', icon: 'fa-edit', link: "${uri}/edit/${domainObject?.uuid}"]

    def shortString = escape(TypeUtils.toShortString(domainObject))
    def click = "efd._confirmDelete('$uri/delete','${domainObject?.uuid}','${domainClass.simpleName}','${shortString}')"
    def deleteButton = [id: "${id}Delete", label: 'delete.menu.label', click: click]
    def more = [id: "${id}More", label: 'more.menu.label', icon: 'fa-th-list', 'subMenus': [deleteButton]]
    def buttons = [list, create, edit, more]

    widgetContext.parameters.id = "${id}Toolbar"
    widgetContext.parameters.buttons = buttons
    def toolbarWidget = new ToolbarWidget(widgetContext)

    return toolbarWidget.build().toString()
  }

  /**
   * Builds the script section for use after the UI elements are built.
   * @return The post script.
   */
  @Override
  String buildPostScript() {
    return super.buildPostScript() + buildPreloadedMessages(['ok.label', 'cancel.label', 'delete.confirm.message', 'delete.confirm.title'])
  }

}
