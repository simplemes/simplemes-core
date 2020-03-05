/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.web.ui.webix.widget.DefinitionListWidget

/**
 * Provides the efDefinitionList Freemarker marker implementation.
 * This creates the HTML/JS needed to display a list of data (grid).
 * This delegates most of the work to the {@link DefinitionListWidget}.
 */
class DefinitionListMarker extends BaseMarker {
  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    write(new DefinitionListWidget(buildWidgetContext()).build())
  }

}
