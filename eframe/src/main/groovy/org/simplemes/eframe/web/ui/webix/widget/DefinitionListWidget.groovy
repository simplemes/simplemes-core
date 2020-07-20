/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.misc.NameUtils

/**
 * The base definition list widget class.  Produces the UI elements needed for a list definition page.
 * This includes a toolbar.  
 *
 */
class DefinitionListWidget extends ListWidget {

  /**
   * Basic constructor.
   * @param widgetContext The widget context this widget is operating in.  Includes URI, parameters, marker etc.
   */
  DefinitionListWidget(WidgetContext widgetContext) {
    super(widgetContext)
  }


  /**
   * Builds the toolbar section of the list.
   */
  @Override
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
   * Builds the event handlers (in the post script).
   * @return
   */
  @Override
  def buildEventHandlers() {
    super.buildEventHandlers()
    buildSearchEnterKeyEventHandler()
  }

  /**
   * Build the event handler that performs the search.
   */
  void buildSearchEnterKeyEventHandler() {
    // The event handler refreshes the list with the search string and forces page 0.
    addPostscriptText("""    ${$$}("${id}Search").attachEvent("onEnter", function (id) {
      var value = ${$$}("${id}Search").getValue();
      console.log("Searching for "+value);
      tk.refreshList("$id",{search:value});
      return true;
    });
    """)

  }


}
