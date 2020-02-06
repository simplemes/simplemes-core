/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.date

import groovy.transform.EqualsAndHashCode
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import org.simplemes.eframe.misc.ArgumentUtils

import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Date only class.  Provides basic date-related functions.
 */
@TypeDef(type = DataType.DATE)
@EqualsAndHashCode(includes = ['time'])
class DateOnly {

  /**
   * The raw time (milliseconds since 1970).
   */
  long time

  /**
   * Constructs a date for the current date.
   */
  DateOnly() {
    time = System.currentTimeMillis()
    setTimeToMidnightUTC()
  }

  /**
   * Constructs a dateOnly object from a given timestamp (in milliseconds).
   * @param timeInMillis The time in milliseconds.
   */
  DateOnly(long timeInMillis) {
    time = timeInMillis
    setTimeToMidnightUTC()
  }

  /**
   * Constructs a dateOnly object from a given date/time.
   * @param date The date. 
   */
  DateOnly(Date date) {
    ArgumentUtils.checkMissing(date, 'date')
    time = date.time
    setTimeToMidnightUTC()
  }

  /**
   * Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT
   * represented by this <tt>Date</tt> object.
   *
   * @return the number of milliseconds since January 1, 1970, 00:00:00 GMT
   *          represented by this date.
   */
  long getTime() {
    return time
  }

  /**
   * Sets the time part of this  DateOnly to midnight, UTC.
   */
  protected void setTimeToMidnightUTC() {
    def instant = Instant.ofEpochMilli(time).truncatedTo(ChronoUnit.DAYS)
    time = instant.toEpochMilli()
  }

  /**
   * Returns the human readable format.  This is the ISO format.
   * @return
   */
  @Override
  String toString() {
    return ISODate.format(this)
  }
}