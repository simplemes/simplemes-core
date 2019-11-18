package org.simplemes.eframe.date

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines a single range of time (date/time) from a given start date/time to an end date/time.
 *
 */
@ToString(includePackage = false)
@EqualsAndHashCode
class DateRange {

  /**
   * The range's start date/time.
   */
  Date start

  /**
   * The range's end date/time.
   */
  Date end

  /**
   * Basic constructor.
   * @param start The range's start date/time.
   * @param end The range's end date/time.
   */
  DateRange(Date start, Date end) {
    this.start = start
    this.end = end
  }

}
