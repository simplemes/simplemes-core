package org.simplemes.eframe.preference.event

import groovy.util.logging.Slf4j
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.preference.TreeStatePreference
import org.simplemes.eframe.security.SecurityUtils

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Handles the TreeStateChanged event from a tree/grid GUI.  Stores the list of expanded elements in an array.
 *
 * <h3>Logging</h3>
 * The logging for this class can be enabled:
 * <ul>
 *   <li><b>trace</b> - Logs the storage of the preference. </li>
 * </ul>
 */
@Slf4j
class TreeStateChanged implements GUIEventInterface {

  /**
   * The key for the tree state preference.  This is used just once per element, so the key
   * is static.
   */
  static final String KEY = 'treeState'

  /**
   * Handles the list column resized event.
   * Inputs are:
   * <ul>
   *   <li><code>pageURI</code> - The URI for the page these settings changed on.</li>
   *   <li><code>element</code> - The HTML ID of the element that changed.</li>
   *   <li><code>event</code> - The change event (e.g. 'TreeStateChanged', etc).</li>
   *   <li><code>expandedKeys</code> - The key for the expanded row(s).  Supports multiple.</li>
   * </ul>
   *
   * @param params The HTTP request parameters.
   */
  void handleEvent(Map params) {
    String pageParam = ControllerUtils.instance.determineBaseURI((String) params.get('pageURI'))
    String elementParam = params.get('element')
    String userParam = SecurityUtils.currentUserName
    if (!userParam) {
      // Can't remember the settings if not logged in.
      return
    }
    // Figure out the place to put this new splitter size
    def preference = PreferenceHolder.find {
      page pageParam
      user userParam
      element elementParam
    }

    // And save the preference, overwriting the previous state.
    preference.setPreference(new TreeStatePreference(expandedKeys: params.get('expandedKeys'))).save()
    log.trace("Stored Preference {}", preference)
  }
}
