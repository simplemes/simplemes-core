package org.simplemes.eframe.preference


import groovy.transform.ToString
import org.simplemes.eframe.json.TypeableJSONInterface

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * Defines the base class for all core preference settings.
 * This base class uses the key for equals checks.
 */
@ToString(includeNames = true, includePackage = false)
//@JsonIgnoreProperties(['key'])
abstract class BasePreferenceSetting implements PreferenceSettingInterface, TypeableJSONInterface {

  /**
   * The default key this preference is stored under.
   */
  static final DEFAULT_KEY = 'key'

  /**
   * The key this preference is stored under.
   */
  String key = DEFAULT_KEY

}
