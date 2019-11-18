package org.simplemes.eframe.system

import groovy.transform.EqualsAndHashCode

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the basic status that means the object is disabled.
 */
//@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode(includes = ['id'])
class DisabledStatus extends BasicStatus {
  static DisabledStatus instance = new DisabledStatus()

  String id = 'DISABLED'

  /**
   * Returns true if this status means that the object is enabled.
   * @return True if enabled.
   */
  @Override
  boolean isEnabled() {
    return false
  }

  @Override
  String toString() {
    return "EnabledStatus{" +
      "id='" + id + '\'' +
      "default='" + defaultChoice + '\'' +
      '}'
  }

}
