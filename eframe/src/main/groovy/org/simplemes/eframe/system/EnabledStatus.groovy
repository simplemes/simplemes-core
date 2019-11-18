package org.simplemes.eframe.system

import groovy.transform.EqualsAndHashCode

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the BasicStatus for enabled elements.
 */
//@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode(includes = ['id'])
class EnabledStatus extends BasicStatus {
  static EnabledStatus instance = new EnabledStatus()

  String id = 'ENABLED'

  /**
   * Empty constructor.
   */
  EnabledStatus() {
    defaultChoice = true
  }

  /**
   * Returns true if this status means that the object is enabled.
   * @return True if enabled.
   */
  @Override
  boolean isEnabled() {
    return true
  }


  @Override
  String toString() {
    return "EnabledStatus{" +
      "id='" + id + '\'' +
      "default='" + defaultChoice + '\'' +
      '}'
  }
}
