/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.web.ui.webix.widget.PagerWidget

/**
 * Provides the implementation of the &lt;@efLookup&gt; marker.
 * This marker can localize a simple string from the messages.properties file.
 *
 */
@SuppressWarnings("unused")
class PagerMarker extends BaseMarker {
  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    if (!parameters.uri) {
      throw new MarkerException("efPager requires the 'uri' option", this)
    }
    if (!parameters.from) {
      throw new MarkerException("efPager requires the 'from' option", this)
    }
    if (!parameters.total) {
      throw new MarkerException("efPager requires the 'total' option", this)
    }

    def widgetContext = buildWidgetContext()
    write(new PagerWidget(widgetContext).build().toString())

  }

}

