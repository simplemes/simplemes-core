package org.simplemes.eframe.preference.event

import groovy.util.logging.Slf4j
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.preference.SplitterPreference
import org.simplemes.eframe.security.SecurityUtils

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Handles the SplitterResized event from a list GUI.  Stores the splitter panel size(s) in the right preference area
 * for the user.
 *
 */
@Slf4j
class SplitterResized implements GUIEventInterface {

  /**
   * The key for the splitter size state preference.  This is used just once per element, so the key
   * is static.
   */
  static final String KEY = 'splitterSize'

  /**
   * Handles the list splitter resized event.
   * The controller parameters inputs are:
   * <ul>
   *   <li><code>pageURI</code> - The URI for the page these settings changed on.</li>
   *   <li><code>element</code> - The HTML ID of the element (splitter) that changed (e.g. 'MANAGER_DEFAULTSplitter0').</li>
   *   <li><code>name</code> - The name of the configuration (typically a dashboard: 'MANAGER_DEFAULT').</li>
   *   <li><code>event</code> - The change event (e.g. 'SplitterResized', etc).</li>
   *   <li><code>panelN</code> - The panel size (percentage) for panels 1 to (N-1) panels.  This is the panel name and the value is the percentage. (e.g. 'MANAGER_DEFAULTSplitter0Panel0=23.4')</li>
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

    PreferenceHolder preference = PreferenceHolder.find {
      page pageParam
      user userParam
      element elementParam
    }
    String resizer = params.resizer
    if (!resizer) {
      // No resizer name, so don't process
      return
    }

    SplitterPreference splitterPreference = (SplitterPreference) preference[resizer] ?: new SplitterPreference()
    try {
      splitterPreference.resizer = resizer
      splitterPreference.size = params.size.toBigDecimal()
    } catch (ignored) {
      // Bad size, so do nothing...
      return
    }
    preference.setPreference(splitterPreference).save()
    log.trace("Stored Preference {}", preference)
  }


}
