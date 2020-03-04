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
    def showInProcess = markerContext.markerCoordinator.others[ShowMarker.SHOW_MARKER_IN_PROCESS_NAME]
    if (!(level) && !(showInProcess)) {
      throw new MarkerException("efMenuItem must be enclosed in an efMenu or efShow marker.", this)
    }
    id = id ?: parameters.key ?: "menuItem${uniqueIDCounter}"

    if (isSeparator()) {
      buildSeparatorMenuItem()
    } else {
      buildMenuItem()
    }
  }

  /**
   * Writes the JS a normal menu item.
   */
  protected void buildMenuItem() {
    def showInProcess = markerContext.markerCoordinator.others[ShowMarker.SHOW_MARKER_IN_PROCESS_NAME]

    def (label, tooltip) = lookupLabelAndTooltip(parameters.key ?: id)
    def handler = """ef.alert("No onClick or uri for menu item $id");"""
    if (parameters.onClick) {
      handler = parameters.onClick
    } else if (parameters.uri) {
      handler = """window.location="$parameters.uri" """
    }

    if (showInProcess) {
      // Used in a efShow tag, so put the menu the the marker coordinator so it can be used by efShow.
      def list = markerContext.markerCoordinator.others[ShowMarker.ADDED_MENUS_NAME] ?: []
      def click = handler
      def link = null
      if (parameters.uri) {
        click = null
        link = parameters.uri
      }
      list << [id   : "${NameUtils.toLegalIdentifier(id)}", label: "$label", tooltip: "${tooltip ?: ''}",
               click: click, uri: link]
      markerContext.markerCoordinator.others[ShowMarker.ADDED_MENUS_NAME] = list
    } else {
      // Used in a normal efMenu
      def s = """
      {
        id: "${NameUtils.toLegalIdentifier(id)}",
        value: "$label",
        tooltip: "${tooltip ?: ''}",
      }, 
      """

      write("$s\n")

      def arrayName = markerContext.markerCoordinator.others[MenuMarker.ACTION_ARRAY_NAME]
      def element = NameUtils.toLegalIdentifier(id)
      markerContext.markerCoordinator.addPostscript("${arrayName}.$element='${JavascriptUtils.escapeForJavascript(handler)}';\n")
    }
  }

  /**
   * Writes the JS a separator menu item.
   */
  protected void buildSeparatorMenuItem() {
    def showInProcess = markerContext.markerCoordinator.others[ShowMarker.SHOW_MARKER_IN_PROCESS_NAME]

    if (showInProcess) {
      // Used in a efShow tag, so put the menu the the marker coordinator so it can be used by efShow.
      def list = markerContext.markerCoordinator.others[ShowMarker.ADDED_MENUS_NAME] ?: []
      list << [separator: true]
      markerContext.markerCoordinator.others[ShowMarker.ADDED_MENUS_NAME] = list
    } else {
      write('{$template: "Separator"},\n')
    }
  }

  /**
   * Returns true if this menu item is the separator.
   * @return True if separator.
   */
  protected boolean isSeparator() {
    return (!parameters.id && !parameters.key && !parameters.uri && !parameters.onClick)
  }

}
