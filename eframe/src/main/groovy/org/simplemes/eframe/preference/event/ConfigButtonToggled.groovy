package org.simplemes.eframe.preference.event

import groovy.util.logging.Slf4j
import org.simplemes.eframe.preference.ConfigButtonPreference
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.security.SecurityUtils

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Detects when the global configuration toggle buttons is clicked and stores this new state in the users preferences.
 * This setting is global and used for all pages.
 *
 */
@Slf4j
class ConfigButtonToggled implements GUIEventInterface {

  /**
   * The page the preference is stored under.  This is a global setting for the user.
   */
  static final String PAGE = '/index'

  /**
   * The element the preference is stored under.  This is used just once per page, so the key
   * is static.
   */
  static final String ELEMENT = 'ToggleConfigButtons'

  /**
   * The key for the button state preference.  There is only one preference for a configuration button.
   */
  static final String KEY = 'state'

  /**
   * The event string expected for this event.
   */
  static final String EVENT = 'ConfigButtonsToggled'

  /**
   * Handles the toggled event when called from the client.
   * Inputs are:
   * <ul>
   *   <li><code>pageURI</code> - Always '/index' </li>
   *   <li><code>element</code> - Always 'ToggleConfigButtons'</li>
   *   <li><code>event</code> - The change event (e.g. 'ConfigButtonsToggled', etc).</li>
   *   <li><code>visible</code> - 'true' or 'false' for the current button visibility state.</li>
   * </ul>
   *
   * @param params The HTTP request parameters.
   */
  void handleEvent(Map params) {
    String pageParam = PAGE
    String elementParam = ELEMENT
    def visibleParam = (params.get('visible') == 'true')
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

    ConfigButtonPreference configButtonPref = (ConfigButtonPreference) preference[KEY] ?: new ConfigButtonPreference()
    configButtonPref.visible = visibleParam
    preference.setPreference(configButtonPref).save()
    log.trace("Stored Preference {}", preference)
  }

}
