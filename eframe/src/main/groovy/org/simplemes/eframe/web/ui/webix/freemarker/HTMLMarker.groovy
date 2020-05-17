/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.misc.JavascriptUtils

/**
 * Provides the implementation of the &lt;@efHTML&gt; marker.
 * This marker is used for fringe cases where you need to insert HTML in the .ftl file output.
 * <p>
 * This is used mostly in dashboard activity pages since most normal pages support HTML directly.
 *
 */
@SuppressWarnings("unused")
class HTMLMarker extends BaseMarker {
  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    def snippetMode = markerContext?.markerCoordinator?.others[FormMarker.COORDINATOR_SNIPPET_MODE]
    if (!snippetMode) {
      throw new MarkerException("efHTML must be used in an efForm with dashboard='true'", this)
    }

    def sb = new StringBuilder()

    def spacer = parameters.spacer
    if (spacer) {
      if (spacer instanceof Boolean) {
        spacer = 'before'
      }
    }

    def spaceText = ',{margin: 8,view: "label", template: "&nbsp;"}'
    if (spacer == 'before') {
      sb << spaceText
    }

    def html = JavascriptUtils.formatMultilineHTMLString(renderContent())
    if (!html) {
      html = '""'
    }

    def width = parameters.width ?: '10%'
    def height = parameters.height ? """height: tk.ph("$parameters.height"), """ : ''

    sb << """,{type: "clean", width: tk.pw("$width"),$height template: $html} """

    if (spacer == 'after') {
      sb << spaceText
    }
    write(sb)
  }

}

