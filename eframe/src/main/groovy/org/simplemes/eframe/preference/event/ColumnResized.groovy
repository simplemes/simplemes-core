package org.simplemes.eframe.preference.event


import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.misc.LogUtils
import org.simplemes.eframe.preference.ColumnPreference
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.security.SecurityUtils

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Handles the ColumnResized event from a list GUI.  Stores the column size in the right preference area
 * for the user.
 *
 * <h3>Logging</h3>
 * The logging for this class can be enabled:
 * <ul>
 *   <li><b>trace</b> - Logs the storage of the preference. </li>
 * </ul>
 */
class ColumnResized implements GUIEventInterface {

  /**
   * The logger for this class.
   */
  static log = LogUtils.getLogger(this)

  /**
   * Handles the list column resized event.
   * Inputs are:
   * <ul>
   *   <li><code>pageURI</code> - The URI for the page these settings changed on.</li>
   *   <li><code>element</code> - The HTML ID of the element that changed.</li>
   *   <li><code>event</code> - The change event (e.g. 'ColumnResized', etc).</li>
   *   <li><code>column</code> - The name of the column that changed.</li>
   *   <li><code>newSize</code> - The new width of the column (integer, pixels).</li>
   * </ul>
   *
   * @param params The HTTP request parameters.
   */
  void handleEvent(Map params) {
    String pageParam = ControllerUtils.instance.determineBaseURI((String) params.get('pageURI'))
    String elementParam = params.get('element')
    String column = params.get('column')
    def newSize = params.get('newSize')?.toBigDecimal()
    String userParam = SecurityUtils.currentUserName
    if (!userParam) {
      // Can't remember the settings if not logged in.
      return
    }

    PreferenceHolder preference = PreferenceHolder.find {
      page pageParam
      user userParam
      element elementParam
    }

    ColumnPreference columnPref = (ColumnPreference) preference[column] ?: new ColumnPreference(column: column)
    columnPref.width = newSize
    preference.setPreference(columnPref).save()
    log.trace("Stored Preference {}", preference)
  }
}
