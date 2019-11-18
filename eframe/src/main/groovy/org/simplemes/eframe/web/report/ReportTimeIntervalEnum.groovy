package org.simplemes.eframe.web.report

import org.simplemes.eframe.date.DateRange
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.i18n.GlobalUtils

import java.time.temporal.ChronoUnit

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 *
 * Defines a report/chart interval of time.  The enum is capable of calculating the absolute start/end of the interval
 * based on the current time.  This is typically used for filtering report/chart data to specific amount of time.
 * These enumerations are usually evaluated relative to 'now'.
 *
 */
enum ReportTimeIntervalEnum implements Comparable {
  // Note: The ID has a gap (A..D) between each entry to allow insertion of new values between them if needed.

  /**
   * Today (since Midnight).
   */
  TODAY('A', { Date now -> new DateRange(DateUtils.truncate(now, ChronoUnit.DAYS), DateUtils.ceiling(now, ChronoUnit.DAYS)) }),

  /**
   * Yesterday
   */
  YESTERDAY('D',
            { Date now -> now--; new DateRange(DateUtils.truncate(now, ChronoUnit.DAYS), DateUtils.ceiling(now, ChronoUnit.DAYS)) }),

  /**
   * Last 24 Hours
   */
  LAST_24_HOURS('G', { Date now -> new DateRange(new Date(now.time - DateUtils.MILLIS_PER_DAY), now) }),

  /**
   * Last 30 days.
   */
  LAST_7_DAYS('I', { Date now -> new DateRange(new Date(now.time - 7 * DateUtils.MILLIS_PER_DAY), now) }),

  /**
   * Last 30 days.
   */
  LAST_30_DAYS('J', { Date now -> new DateRange(new Date(now.time - 30 * DateUtils.MILLIS_PER_DAY), now) }),

  /**
   * This Month.
   */
  THIS_MONTH('M',
             { Date now ->
               new DateRange(DateUtils.truncate(now, ChronoUnit.MONTHS), DateUtils.ceiling(now, ChronoUnit.MONTHS))
             }),

  /**
   * Last Month
   */
  LAST_MONTH('P',
             { Date now ->
               def d = DateUtils.addMonths(now, -1)
               new DateRange(DateUtils.truncate(d, ChronoUnit.MONTHS), DateUtils.ceiling(d, ChronoUnit.MONTHS))
             }),

  /**
   * Last 6 Months.  This is the previous 6 full months.  It does not include the current month.
   */
  LAST_6_MONTHS('R',
                { Date now ->
                  def d = DateUtils.addMonths(now, -7)
                  new DateRange(DateUtils.truncate(d, ChronoUnit.MONTHS), DateUtils.truncate(now, ChronoUnit.MONTHS))
                }),

  /**
   * This Year.
   */
  THIS_YEAR('T',
            { Date now ->
              new DateRange(DateUtils.truncate(now, ChronoUnit.YEARS), DateUtils.ceiling(now, ChronoUnit.YEARS))
            }),

  /**
   * Last Year.
   */
  LAST_YEAR('V',
            { Date now ->
              def d = DateUtils.addYears(now, -1)
              new DateRange(DateUtils.truncate(d, ChronoUnit.YEARS), DateUtils.ceiling(d, ChronoUnit.YEARS))
            }),

  /**
   * A custom range is used (values specified elsewhere)
   */
  CUSTOM_RANGE('Z', {})

  /**
   * The standardized field type associated with this field format.
   */
  final String id

  /**
   * The closure (logic) needed to calculate the absolute start/end date/times for the interval.
   * This closure is passed the time that is considered 'now' for this calculation.
   * It should return a DateRange with the right start/end values.
   */
  final Closure<DateRange> calculationClosure

  /**
   * Convenience constructor for an interval.
   * @param id The stored value in the DB.
   * @param calculationClosure A closure that calculates the start.end date relative to a passed in now date/time.
   */
  ReportTimeIntervalEnum(String id, Closure calculationClosure) {
    this.id = id
    this.calculationClosure = calculationClosure
  }

  /**
   * Calculates the date range for this enum instance, relative to now.
   * @param now The now value to use for the calculation.  (<b>Default</b>: current date/time)
   * @return The date/time range.
   */
  DateRange determineRange(Date now = null) {
    return (DateRange) calculationClosure(now ?: new Date())
  }

  /**
   * Build a human-readable version of this object.
   * @param locale The locale to display the enum display text.
   * @return The human-readable string.
   */
  String toStringLocalized(Locale locale = null) {
    return GlobalUtils.lookup("reportTimeInterval.${name()}.label", null, locale)
  }


}
