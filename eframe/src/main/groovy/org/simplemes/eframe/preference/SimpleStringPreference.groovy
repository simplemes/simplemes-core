package org.simplemes.eframe.preference

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.simplemes.eframe.preference.BasePreferenceSetting
import org.simplemes.eframe.preference.PreferenceSettingInterface

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * This holds the a simple string preference.
 *
 */
@ToString(includePackage = false, includeNames = true, includeSuper = true)
@EqualsAndHashCode
class SimpleStringPreference extends BasePreferenceSetting implements PreferenceSettingInterface {

  /**
   * Empty constructor.
   */
  SimpleStringPreference() {
  }

  /**
   * Convenience Constructor.
   * @param key
   * @param value
   */
  SimpleStringPreference(String key, String value = null) {
    this.key = key
    this.value = value
  }

  /**
   * The simple value.
   */
  String value = ''

}
