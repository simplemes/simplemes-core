package org.simplemes.eframe.preference

import groovy.transform.ToString

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This holds the specific preferences for a panel in a splitter for the desired display mode (size).
 *
 * <h3>User Preference Usage</h3>
 * This preference is stored in the user preference table under the keys:
 * <ul>
 *   <li><b>page</b> - The URI of the page this splitter is in. Stripped of arguments. </li>
 *   <li><b>user</b> - The user name. </li>
 *   <li><b>name</b> - The dashboard ID, if the splitter is used in a dashboard.  (e.g. MANAGER) </li>
 *   <li><b>element</b> - The HTML ID of the grid the splitter. (e.g. MANAGERPanelA)</li>
 * </ul>
 *
 */
@ToString(includePackage = false, includeNames = true, includeSuper = true)
class SplitterPreference extends BasePreferenceSetting implements PreferenceSettingInterface {

  /**
   * The panel size (percentage).
   */
  BigDecimal size

  /**
   * The resizer ID.  Used as the preference key.
   */
  String resizer

  /**
   * The key for this preference.  This is unique within a single object's setting (string).
   */
  @Override
  String getKey() {
    return resizer
  }
}
