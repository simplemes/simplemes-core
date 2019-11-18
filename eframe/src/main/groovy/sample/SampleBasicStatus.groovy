package sample

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.simplemes.eframe.system.BasicStatus

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A test class that can be added to the list of basic samples.
 */
@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode(includes = ['id'])
class SampleBasicStatus extends BasicStatus {
  String id = 'ADDED'

  static SampleBasicStatus instance = new SampleBasicStatus()

  /**
   * Returns true if this status means that the object is enabled.
   * @return True if enabled.
   */
  @Override
  boolean isEnabled() {
    return false
  }

}
