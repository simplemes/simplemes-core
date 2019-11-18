package org.simplemes.eframe.preference

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * Defines a user preference setting to contain the user's setting between sessions.  This interface defines the
 * core features of a preference setting.
 */
interface PreferenceSettingInterface {
  /**
   * The key for this preference.  This is unique within a single {@link Preference} object's settings list.
   */
  String getKey()

}
