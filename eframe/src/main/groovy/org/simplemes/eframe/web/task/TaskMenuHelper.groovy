package org.simplemes.eframe.web.task

import groovy.util.logging.Slf4j
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.misc.LogUtils

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */
/**
 * Provides utility methods for building a task menu from controller responses.
 *  <h3>Logging</h3>
 * The logging for this class that can be enabled:
 * <ul>
 *   <li><b>debug</b> - Logs when a new TaskMenu is generated. </li>
 * </ul>

 */
@Slf4j
class TaskMenuHelper {

  /**
   * A static instance for this helper.
   */
  static TaskMenuHelper instance = new TaskMenuHelper()

  /**
   * This provides access to the controllers.  Can be replaced with a mock for easier testing.
   */
  TaskMenuControllerUtils taskMenuControllerUtils = new TaskMenuControllerUtils()

  /**
   * The cached task menu built from all controllers.  Generated and sorted for display on first use.
   */
  protected TaskMenu taskMenu = null

  /**
   * Determines the core task menu from the controllers.  This is the default task menu for
   * most users.
   * @return The task menu.
   */
  TaskMenu getTaskMenu() {
    // Clear the cache menu for dev mode.
    taskMenu = (Holders.environmentDev || Holders.environmentTest) ? null : taskMenu
    log.trace('getTaskMenu() taskMenu: {}, mode: {}', taskMenu, Holders.environmentDev)
    if (!taskMenu) {
      taskMenu = new TaskMenu()
      def menuItemList = taskMenuControllerUtils.getCoreTasks()

      // Take each menu item and insert it into the right folder in the task menu
      for (menuItem in menuItemList) {
        def failReason = menuItem.check()
        if (failReason) {
          log.error('Menu Item {} failed check.  Reason = {}', menuItem, failReason)
        } else {
          try {
            taskMenu << menuItem
          } catch (Exception e) {
            // We need to catch and log this exception since it can cause the error page to get garbled.
            LogUtils.logStackTrace(log, e, taskMenu)
          }
        }
      }
      taskMenu.sort()
      log.debug('Generated new TaskMenu {}. ', taskMenu)
    }

    return taskMenu
  }

}
