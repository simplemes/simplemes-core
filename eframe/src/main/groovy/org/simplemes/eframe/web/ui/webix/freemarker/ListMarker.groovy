/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.web.ui.webix.widget.DefinitionListWidget
import org.simplemes.eframe.web.ui.webix.widget.ListWidget

/**
 * Provides the efDefinitionList Freemarker marker implementation.
 * This creates the HTML/JS needed to display a list of data (grid).
 * This delegates most of the work to the {@link DefinitionListWidget}.
 */
@SuppressWarnings("unused")
class ListMarker extends BaseMarker {
  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    if (!(markerContext?.markerCoordinator?.formID)) {
      throw new MarkerException("efList must be enclosed in an efForm marker.", this)
    }
    write(new ListWidget(buildWidgetContext()).build() + ",")
  }

}
