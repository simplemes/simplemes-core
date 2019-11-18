package org.simplemes.eframe.date


import java.text.SimpleDateFormat

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * Date only class.  Provides most of the functions of a Date, but with the time set to midnight (in UTC).
 */
class DateOnly extends Date {

  /**
   * The UTC Timezone.
   */
  private static final TimeZone TIMEZONE_UTC = TimeZone.getTimeZone('UTC')

  /**
   * Constructs a date for the current date.
   */
  DateOnly() {
    setTimeToMidnightUTC()
  }

  /**
   * Constructs a date from the given ISO date string.
   * @param isoDate The ISO date.
   */
  DateOnly(String isoDate) {
    super(ISODate.parseDateOnly(isoDate).time)
    setTimeToMidnightUTC()
  }

  /**
   * Constructs a dateOnly object from a given timestamp (in milliseconds).
   * @param timeInMillis The time in milliseconds.  This must correspond to midnight in UTC.
   */
  DateOnly(long timeInMillis) {
    // This is a bit of a kludge to make DateOnly work with different timezones.
    super(timeInMillis)
    setTimeToMidnightUTC()
    if (timeInMillis != time) {
      throw new IllegalArgumentException("timeInMillis ($timeInMillis) must correspond to midnight in UTC.")
    }
    assert timeInMillis == time
  }

  /**
   * Sets the time part of this  DateOnly to midnight, UTC.
   */
  @SuppressWarnings("SimpleDateFormatMissingLocale")
  protected void setTimeToMidnightUTC() {
    SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd")
    isoFormat.setTimeZone(TIMEZONE_UTC)
    def s = isoFormat.format(this)
    Date date = isoFormat.parse(s)
    time = date.time
  }

  /**
   * Build a human-readable version of this object.
   * @return The human-readable string.
   */
  @Override
  String toString() {
    return DateUtils.formatDate(this)
  }
}
