package org.simplemes.eframe.web.task

import groovy.transform.Sortable
import groovy.transform.ToString
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.ArgumentUtils

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * Defines a folder (group) of task menu items.
 */
@ToString(includeNames = true, includePackage = false)
@Sortable(includes = ['displayOrder', 'name'])
class TaskMenuFolder {

  /**
   * The folder name (<b>Required</b>).
   */
  String name

  /**
   * The relative display order (<b>Optional</b>).
   */
  Integer displayOrder

  /**
   * The list of sub-folders inside of this folder.
   */
  List<TaskMenuFolder> folders = Collections.synchronizedList([])

  /**
   * The list of menu items in this folder.
   */
  List<TaskMenuItem> menuItems = Collections.synchronizedList([])

  /**
   * Sorts the folders and menu items.  Also sorts sub-folders.
   * Will assign any missing displayOrder fields in alphabetical order (starting at 9000).
   */
  void sortAll() {
    // Assign a display order to each folder (if not provided).
    // First sort alphabetically.
    folders.sort()
    // Then assign a high value display order (9000+) to each folder in alpha order.
    def displayOrder = 9000
    for (folder in folders) {
      if (!folder.displayOrder) {
        folder.displayOrder = displayOrder
        displayOrder++
      }
    }
    // Now, re-sort correctly on display order
    folders.sort()

    // Sort the menu items using the same logic.
    // Assign a display order to each menu item (if not provided).
    // First sort alphabetically.
    menuItems.sort()
    // Then assign a high value display order (9000+) to each folder in alpha order.
    displayOrder = 9000
    for (menuItem in menuItems) {
      if (!menuItem.displayOrder) {
        menuItem.displayOrder = displayOrder
        displayOrder++
      }
    }
    // Now, re-sort correctly on display order
    menuItems.sort()

    // Now, have any child folders sort themselves too.
    for (folder in folders) {
      folder.sortAll()
    }
  }

  /**
   * Adds the given menu item to this folder.
   * @param menuItem The menu item.
   */
  def leftShift(TaskMenuItem menuItem) {
    menuItems << menuItem
  }

  /**
   * Returns the localized name.  Uses the name with 'taskMenu.${name}.label' to find the localized name.
   * @param locale The optional locale.
   * @return The localized name.  If not found, then returns the raw 'name'.
   */
  String getLocalizedName(Locale locale = null) {
    def key = "taskMenu.${name}.label"
    def s = GlobalUtils.lookup(key, null, locale)
    if (s == key) {
      // No lookup found, so just use the name.
      return name
    }
    return s
  }

  /**
   * Returns the localized tooltip.  Uses the name with 'taskMenu.${name}.tooltip' to find the localized tooltip.
   * @param locale The optional locale.
   * @return The localized tooltip.  If not found, then returns the raw 'name'.
   */
  String getLocalizedTooltip(Locale locale = null) {
    def key = "taskMenu.${name}.tooltip"
    def s = GlobalUtils.lookup(key, null, locale)
    if (s == key) {
      // No lookup found, so just use the name.
      return name
    }
    return s
  }

  /**
   * Convenience builder to build one or more folders from the given folder definition.
   * The name is required, but the order is optional.
   * @param foldersDef The folder definition for one or more folders.  Format: 'name:order.name:order'
   * @return The list of folders in a flattened list (top-level first).
   */
  static List<TaskMenuFolder> buildFolders(String foldersDef) {
    ArgumentUtils.checkMissing(foldersDef, 'foldersDef')

    def res = []
    def list = foldersDef.tokenize('.')
    for (s in list) {
      res << buildFolder(s)
    }

    return res
  }

  /**
   * Convenience builder to build one folder with an optional display order from the string.
   * @param folderDef The single folder definition.  Format: 'name:order'
   */
  protected static TaskMenuFolder buildFolder(String folderDef) {
    ArgumentUtils.checkMissing(folderDef, 'folderDef')
    def list = folderDef.tokenize(':')
    def displayOrder = null
    if (list.size() > 1) {
      displayOrder = Integer.valueOf(list[1])
    }
    if (!list[0]) {
      throw new IllegalArgumentException("Invalid folder definition '$folderDef'.  No folder name found")
    }
    return new TaskMenuFolder(name: list[0], displayOrder: displayOrder)
  }

  /**
   * Build human readable version of this object's full contents.  This produces a multi-line string with indentation
   * to show levels.  This will recursively call toFullString() on child folders.
   * @param indentLevel The indent level for this element.
   * @return The indented string.
   */
  String toFullString(Integer indentLevel = 0) {
    def prefix = '  ' * indentLevel
    StringBuilder sb = new StringBuilder()
    sb << "$prefix-${name}\n"
    for (folder in folders) {
      sb << "${folder.toFullString(indentLevel + 1)}"
    }
    for (menuItem in menuItems) {
      sb << "$prefix ${menuItem.name} - ${menuItem.uri}\n"
    }
    return sb.toString()
  }
}
