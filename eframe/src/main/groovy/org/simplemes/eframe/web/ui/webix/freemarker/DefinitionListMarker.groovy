/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.misc.NameUtils
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
    id = id ?: parameters.id
    if (!id && markerContext?.controllerClass) {
      def baseName = this.getClass().simpleName - 'Marker'
      baseName = markerContext.controllerClass.simpleName - 'Controller' + baseName
      id = NameUtils.lowercaseFirstLetter(baseName)
    }

    def widgetContext = buildWidgetContext()
    // Provide a height if not given as an attribute
    def height = widgetContext?.parameters?.height ?: '74%'
    widgetContext?.parameters?.height = height

    def src = """
    <div id="$id"></div>
    <script>
      ${markerContext?.markerCoordinator?.prescript}
      webix.ui({
        container: "$id", type: "space", margin: 4, id: "${id}Layout", rows: [
        ${new DefinitionListWidget(widgetContext).build()}
      ]}) ;
      ${markerContext?.markerCoordinator?.postscript}
    </script>
    """

    write(src)
  }

}
