package org.simplemes.eframe.web.ui.webix.freemarker

import groovy.util.logging.Slf4j
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.web.ui.webix.widget.ButtonWidget

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Provides the efField freemarker marker implementation.
 * This builds an input field for user data entry.
 */
@Slf4j
@SuppressWarnings("unused")
class ButtonMarker extends BaseMarker {

  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    if (!(markerContext?.markerCoordinator?.formID)) {
      throw new MarkerException("efButton must be enclosed in an efForm marker.", this)
    }
    def type = parameters.type

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
    write(",$text\n")
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
      (label, tooltip) = GlobalUtils.lookupLabelAndTooltip((String) parameters.label, (String) parameters.tooltip)
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
    def after = parameters.spacer == 'after' ? ",{}" : ""
    def src = """,{view: "template", type: "clean", $size,$template }$after"""

    write(src)
  }

}
