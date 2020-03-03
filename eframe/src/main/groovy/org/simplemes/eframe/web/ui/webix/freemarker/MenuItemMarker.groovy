/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker


import groovy.util.logging.Slf4j
import org.simplemes.eframe.misc.JavascriptUtils
import org.simplemes.eframe.misc.NameUtils

/**
 * Provides the freemarker marker implementation.
 * This builds a menu item (child menu).
 */
@Slf4j
@SuppressWarnings("unused")
class MenuItemMarker extends BaseMarker {

  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    def level = markerContext.markerCoordinator.others[MenuMarker.MENU_LEVEL]
    if (!(level)) {
      throw new MarkerException("efMenuItem must be enclosed in an efMenu or efShow marker.", this)
    }
    id = id ?: parameters.key ?: "menuItem${uniqueIDCounter}"

    if (isSeparator()) {
      buildSeparatorMenuItem()
    } else {
      buildMenuItem()
    }

    def handler = """ef.alert("No onClick or uri for menu item $id");"""
    if (parameters.onClick) {
      handler = parameters.onClick
    } else if (parameters.uri) {
      handler = """window.location="$parameters.uri" """
    }

    def arrayName = markerContext.markerCoordinator.others[MenuMarker.ACTION_ARRAY_NAME]
    def element = NameUtils.toLegalIdentifier(id)
    markerContext.markerCoordinator.addPostscript("${arrayName}.$element='${JavascriptUtils.escapeForJavascript(handler)}';\n")
  }

  /**
   * Writes the JS a normal menu item.
   */
  protected void buildMenuItem() {
    def (label, tooltip) = lookupLabelAndTooltip(parameters.key ?: id)
    def s = """
    {
      id: "${NameUtils.toLegalIdentifier(id)}",
      value: "$label",
      tooltip: "${tooltip ?: ''}",
    }, 
    """

    write("$s\n")
  }

  /**
   * Writes the JS a separator menu item.
   */
  protected void buildSeparatorMenuItem() {
    write('{$template: "Separator"},\n')
  }

  /**
   * Returns true if this menu item is the separator.
   * @return True if separator.
   */
  protected boolean isSeparator() {
    return (!parameters.id && !parameters.key && !parameters.uri && !parameters.onClick)
  }

}
