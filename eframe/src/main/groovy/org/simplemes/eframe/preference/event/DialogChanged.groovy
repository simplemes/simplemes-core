package org.simplemes.eframe.preference.event

import groovy.util.logging.Slf4j
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.preference.DialogPreference
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.security.SecurityUtils

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Handles the DialogChanged event from the browser.  Stores the dialog dimensions for later re-display.
 *
 */
@Slf4j
class DialogChanged implements GUIEventInterface {

  /**
   * The event string expected for this event.
   */
  static final String EVENT = 'DialogChanged'

  /**
   * The key for the dialog state preference.  This is used just once per element, so the key
   * is static.
   */
  static final String KEY = 'dialogState'

  /**
   * Handles the list dialog change position/size event.
   * The controller parameters inputs are:
   * <ul>
   *   <li><code>pageURI</code> - The URI for the page these settings changed on.</li>
   *   <li><code>element</code> - The element (dialog) that changed.  This is normally the dialog Name.</li>
   *   <li><code>event</code> - The change event (e.g. 'DialogChanged', etc).</li>
   *   <li><code>width</code> - The dialog width (percentage)</li>
   *   <li><code>height</code> - The dialog height (percentage)</li>
   *   <li><code>left</code> - The dialog left position (percentage)</li>
   *   <li><code>top</code> - The dialog top position (percentage)</li>
   * </ul>
   *
   * @param params The HTTP request parameters.
   */
  void handleEvent(Map params) {
    String pageParam = ControllerUtils.instance.determineBaseURI(params.pageURI)
    String elementParam = params.get('element')
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

    DialogPreference dialogPreference = (DialogPreference) preference[KEY] ?: new DialogPreference()
    try {
      dialogPreference.width = params.get('width').toBigDecimal()
      dialogPreference.height = params.get('height').toBigDecimal()
      dialogPreference.left = params.get('left').toBigDecimal()
      dialogPreference.top = params.get('top') toBigDecimal()
    } catch (ignored) {
      // Bad size, so do nothing...
      return
    }
    preference.setPreference(dialogPreference).save()
    log.trace("Stored Preference {}", preference)
  }


}
