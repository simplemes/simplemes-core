package org.simplemes.eframe.preference

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.simplemes.eframe.preference.BasePreferenceSetting
import org.simplemes.eframe.preference.event.DialogChanged

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This holds the specific preferences for a single dialog from.
 * The dialogs size/position are stored here.
 *
 */
@ToString(includePackage = false, includeNames = true, includeSuper = true)
@EqualsAndHashCode(includes = ['dialogID'])
class DialogPreference extends BasePreferenceSetting {

  DialogPreference() {
  }

  /**
   * The name/ID of the dialog.
   */
  String dialogID

  /**
   * The dialog width (percent).
   */
  BigDecimal width

  /**
   * The dialog height (percent).
   */
  BigDecimal height

  /**
   * The dialog left position (percent).
   */
  BigDecimal left

  /**
   * The dialog top position (percent).
   */
  BigDecimal top

  /**
   * The key for this preference.  This is unique within a single {@link Preference} object's settings list.
   */
  @Override
  String getKey() {
    return DialogChanged.KEY
  }
}
