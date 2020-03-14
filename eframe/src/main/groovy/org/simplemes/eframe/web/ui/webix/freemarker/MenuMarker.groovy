/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import groovy.util.logging.Slf4j

/**
 * Provides the freemarker marker implementation.
 * This builds a menu (or sub-menu).
 */
@Slf4j
@SuppressWarnings("unused")
class MenuMarker extends BaseMarker {

  /**
   * The name of the current menu level in the marker coordinator.
   * Increments for each sub-menu.
   */
  public static final String MENU_LEVEL = '_menuLevel'

  /**
   * The name of the current sub-menu counter in the marker coordinator.
   * Defines a counter for any sub-menus created under the top-level menu.
   */
  public static final String SUB_MENU_COUNTER_NAME = '_subMenuCounter'

  /**
   * The name of the holder in the marker coordinator that holds the list of menu items defined by menuItem markers.
   */
  public static final String MENU_ITEM_DEFINITION_LIST_NAME = '_menuItemDefList'

  /**
   * The current indent level.
   */
  Integer level = 0

  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    if (!(markerContext?.markerCoordinator?.formID)) {
      throw new MarkerException("efMenu must be enclosed in an efForm marker.", this)
    }
    level = adjustMenuLevel(1)
    id = id ?: "menu$level"
    if (topLevel) {
      markerContext.markerCoordinator.others[SUB_MENU_COUNTER_NAME] = 1
      buildTopLevelMenu()
    } else {
      markerContext.markerCoordinator.others[SUB_MENU_COUNTER_NAME] = markerContext.markerCoordinator.others[SUB_MENU_COUNTER_NAME] + 1
      id = parameters.id ?: "subMenu${markerContext.markerCoordinator.others[SUB_MENU_COUNTER_NAME]}"
      buildSubMenu()
    }
    //write("Menu $level\n$content")
    adjustMenuLevel(-1)
  }

  /**
   * Writes the JS a menu and its content.
   */
  protected void buildTopLevelMenu() {
    def content = renderContent()

    // Build an if block to handle the menu item clicks.
    def menuItemList = markerContext.markerCoordinator.others[MENU_ITEM_DEFINITION_LIST_NAME]
    def sb = new StringBuilder()
    for (menuItem in menuItemList) {
      def click = menuItem.click
      def menuItemId = menuItem.id
      if (sb) {
        sb << "} else if (id=='$menuItemId') {\n"
      } else {
        sb << "if (id=='$menuItemId') {\n"
      }
      sb << "$click\n"
    }
    if (sb) {
      sb << '}\n'
    }

    def s = """
      ,{ view: "menu", openAction: "click", autowidth: true,type:{ subsign: true }, 
        data:[
          $content
        ],
        submenuConfig: {
          tooltip: function (item) {
            return item.tooltip || "??";
          }
        },
        on: {
          onMenuItemClick: function (id) {
            $sb
          }
        }
      } 
      """

    write("$s\n")
  }

  /**
   * Writes the JS for a sub-menu.
   */
  protected void buildSubMenu() {
    def label = ''
    if (parameters.key || parameters.label) {
      (label) = lookupLabelAndTooltip(parameters.key)
    }
    def content = renderContent()
    def s = """
          { id: "$id",value:"$label", submenu: [ 
            $content 
          ]},
    """
    write("$s\n")
  }

  /**
   * Increment/decrement the menu level as needed in the marker coordinator.
   * @param levelAdjustment +1 or -1.
   */
  Integer adjustMenuLevel(int levelAdjustment) {
    Integer level = markerContext.markerCoordinator.others[MENU_LEVEL] as Integer
    if (level == null) {
      level = 0
    }
    level = level + levelAdjustment

    if (level > 0) {
      markerContext.markerCoordinator.others[MENU_LEVEL] = level
    } else {
      markerContext.markerCoordinator.others[MENU_LEVEL] = null
    }
    return level
  }

  /**
   * Returns true if it is the top-level menu.
   * @return True if top-level.
   */
  boolean isTopLevel() {
    return (level == 1)
  }

}
