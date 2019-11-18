package org.simplemes.eframe.web.task

import groovy.transform.ToString
import org.simplemes.eframe.misc.ArgumentUtils

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * Defines a task menu which has folders and tasks the user can execute.
 * This class maintains the menu items/folders for a given task menu.
 */
@ToString(includeNames = true, includePackage = false)
class TaskMenu {

  /**
   * The root folder.  Never displayed.  It just contains the first level of folders.
   */
  TaskMenuFolder root = new TaskMenuFolder(name: '')

  /**
   * Tracks if the task menu needs re-sorting due to new values inserted.
   * This only tracks the calls to leftShift().
   */
  boolean sorted = true

  /**
   * Sorts the folders and all sub folders.  Also sorts the menu items. See {@link TaskMenuFolder} for details.
   */
  void sort() {
    if (!sorted) {
      root.sortAll()
      sorted = true
    }
  }

  /**
   * Adds the given menu item to the folder structure in the correct location.
   * Will create the folders as needed.
   * @param menuItem The menu item.
   */
  def leftShift(TaskMenuItem menuItem) {
    ArgumentUtils.checkMissing(menuItem, 'menuItem')
    if (menuItem.folder) {
      def folder = buildFoldersIfNeeded(menuItem.folder)
      folder << menuItem
    } else {
      // No folder for the menu item, so add it to the root folder as a menu item
      root.menuItems << menuItem
    }
    sorted = false
  }

  /**
   * This will build the folders in the correct hierarchy.  It will add them to the current folder list
   * and return the lowest level folder.
   * @param folderDefinition The folder definition string to use in creating the folders.
   * @return The lowest level folder.
   */
  protected TaskMenuFolder buildFoldersIfNeeded(String folderDefinition) {
    def folderList = TaskMenuFolder.buildFolders(folderDefinition)
    return buildFoldersIfNeeded(root.folders, folderList)
  }

  /**
   * This will build the folders in the correct hierarchy.  It will add them to the current folder list
   * and return the lowest level folder.
   * @param currentFolders The current level of folders being processed.
   * @param menuFolders The list of folders to process from the current level.
   * @return The lowest level folder.
   */
  protected TaskMenuFolder buildFoldersIfNeeded(List<TaskMenuFolder> currentFolders, List<TaskMenuFolder> menuFolders) {
    def currentFolder = currentFolders.find { it.name == menuFolders[0].name }
    if (!currentFolder) {
      // Not found at this level, so create it
      currentFolder = menuFolders[0]
      currentFolders << currentFolder
    }

    // done with this level, drop down to the next level
    menuFolders.remove(0)
    if (menuFolders) {
      // More levels to process, recursively.
      return buildFoldersIfNeeded(currentFolder.folders, menuFolders)
    }

    return currentFolder
  }

  /**
   * Build human readable version of this object's full contents.  This produces a multi-line string with indentation
   * to show levels.
   * @return
   */
  String toFullString() {
    sort()
    root.toFullString()
  }
}
