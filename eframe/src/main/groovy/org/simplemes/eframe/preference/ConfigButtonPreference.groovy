package org.simplemes.eframe.preference

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.simplemes.eframe.preference.event.ConfigButtonToggled

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This holds the specific preferences for the initial state of a config toggle button (only one globally).
 * This stores the visible state of true/false.
 *
 */
@ToString(includePackage = false, includeNames = true, includeSuper = true)
@EqualsAndHashCode
class ConfigButtonPreference extends BasePreferenceSetting implements PreferenceSettingInterface {

  /**
   * True if the config buttons should be visible by default.
   */
  Boolean visible

  /**
   * The simple value.
   */
  String value

  /**
   * The key for this preference.  This is unique within a single object's setting (string).
   */
  @Override
  String getKey() {
    return ConfigButtonToggled.KEY
  }

}
