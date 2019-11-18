package org.simplemes.eframe.preference

import org.simplemes.eframe.preference.PreferenceSettingInterface
import org.simplemes.eframe.preference.SimpleStringPreference

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * Factory to build the correct SimplePreference, based on the value type.  This is mainly used to handle simple
 * string preferences.
 */
class SimplePreferenceFactory {

  /**
   * Builds the correct type of simple preference needed for the given value.  Supports Strings and POGOs.
   * @param value The value to be stored in the preference.
   * @return The Simple Preference object needed to hold the given object.
   */
  static PreferenceSettingInterface buildPreference(Object value) {
    if (value instanceof String) {
      return new SimpleStringPreference(value: value)
    }
    return null
  }

}
