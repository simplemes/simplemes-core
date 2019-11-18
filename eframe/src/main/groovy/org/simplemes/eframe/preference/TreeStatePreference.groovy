package org.simplemes.eframe.preference


import groovy.transform.ToString
import org.simplemes.eframe.preference.event.TreeStateChanged

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This holds the list of expanded rows from a tree.  This is a simple list of string row IDs.
 *
 */
@ToString(includePackage = false, includeNames = true, includeSuper = true)
class TreeStatePreference extends BasePreferenceSetting implements PreferenceSettingInterface {

  /**
   * The list of expanded row keys (comma-delimited).
   */
  String expandedKeys

  /**
   * The key for this preference.  This is unique within a single object's setting (string).
   */
  @Override
  String getKey() {
    return TreeStateChanged.KEY
  }

}
