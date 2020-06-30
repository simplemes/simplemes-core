/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import groovy.util.logging.Slf4j
import org.simplemes.eframe.web.ui.webix.widget.ButtonWidget

/**
 * Provides the freemarker marker implementation.
 * This builds a general-purpose button.
 */
@Slf4j
@SuppressWarnings("unused")
class ButtonMarker extends BaseMarker {

  /**
   * The possible spacer before text.
   */
  String before = ''

  /**
   * The possible spacer after text.
   */
  String after = ''

  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    if (!(markerContext?.markerCoordinator?.formID)) {
      throw new MarkerException("efButton must be enclosed in an efForm marker.", this)
    }
    def type = parameters.type
    if (parameters.spacer) {
      if (parameters.spacer.contains('after')) {
        after = "{},"
      }
      if (parameters.spacer.contains('before')) {
        before = "{},"
      }
    }

    if (type) {
      writeNonStandardButton(type)
    } else {
      writeNormalButton()
    }
  }

  /**
   * Writes the JS for a normal button.
   */
  protected void writeNormalButton() {
    def onClick = (String) parameters.click
    def url = (String) parameters.link
    if (!onClick && !url) {
      throw new MarkerException("efButton is missing a 'click' or 'link' parameter.", this)
    }
    def widgetContext = buildWidgetContext()
    widgetContext.parameters.type = 'form'
    if (parameters.size) {
      widgetContext.parameters.height = """tk.ph("${parameters.size}em")"""
    }

    def text = new ButtonWidget(widgetContext).build().toString()
    write("$before$text,$after\n")
  }

  /**
   * Writes the JS for a non-standard (HTML) button, based on the type option.
   * @param type The button type.  Currently only supports 'undo'.
   */
  protected void writeNonStandardButton(String type) {
    if (type != 'undo') {
      throw new MarkerException("Invalid type $type.  efButton only supports a type of 'undo'.", this)
    }
    def onClick = (String) parameters.click
    if (!onClick) {
      throw new MarkerException("efButton is missing a 'click' parameter.", this)
    }

    def tooltip = ''
    def label = ''
    if (parameters.label) {
      (label, tooltip) = lookupLabelAndTooltip(null)
    }
    if (parameters.tooltip) {
      // Just a tooltip, with no label case
      tooltip = lookup((String) parameters.tooltip)
    }

    def id = parameters.id ?: 'undoButton'
    def css = parameters.css ?: 'undo-button-disabled'
    def click = parameters.click ?: ""
    def title = tooltip ? """title="$tooltip" """ : ""
    def template = """template: '<button type="button" id="$id" class="$css" onclick="$click" $title>$label</button>' """
    def size = """width: tk.pw("1.5em"), height: tk.ph("1.5em")"""
    def src = """$before {view: "template", type: "clean", $size,$template },$after"""

    write(src)
  }

}
