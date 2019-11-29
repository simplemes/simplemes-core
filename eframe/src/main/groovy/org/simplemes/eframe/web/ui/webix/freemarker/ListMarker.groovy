package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.web.ui.webix.widget.DefinitionListWidget

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Provides the efList Freemarker marker implementation.
 * This creates the HTML/JS needed to display a list of data (grid).
 * This delegates most of the work to the {@link DefinitionListWidget}.
 */
class ListMarker extends BaseMarker {
  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    write(new DefinitionListWidget(buildWidgetContext()).build())
  }

}
