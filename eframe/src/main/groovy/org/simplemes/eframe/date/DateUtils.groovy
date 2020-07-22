/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.date


import groovy.transform.CompileStatic
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.i18n.GlobalUtils

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Various date manipulation utility methods.  Includes formatting and parsing for display.
 */
@CompileStatic
class DateUtils {
  /**
   * The UTC Timezone.
   */
  public static final TimeZone TIMEZONE_UTC = TimeZone.getTimeZone('UTC')

  /**
   * The number milliseconds in a whole day.
   */
  public static final long MILLIS_PER_DAY = 86400000

  /**
   * The date/elapsed time precision only needs to display a low precision (e.g. '3 hours').
   */
  public static final String PRECISION_LOW = 'low'

  /**
   * The date/elapsed time precision needs to display a medium precision (e.g. '3 hours 23 minutes').
   */
  public static final String PRECISION_MEDIUM = 'med'

  /**
   * Parses the given string into a Date using the optional Locale.  Uses the standard Java date format for the locale (SHORT/MEDIUM).
   * @param s The date string.  Can be null.
   * @param locale The locale to use for parsing the date (default is the request Locale). (<b>Optional</b>)
   * @return The parsed date or null.
   */
  static Date parseDate(String s, Locale locale = null) {
    if (s) {
      def dateFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, GlobalUtils.getRequestLocale(locale))
      dateFormatter.setTimeZone(Holders.globals.timeZone)
      return dateFormatter.parse(s)
    }
    return null
  }

  /**
   * Parses the given string into a DateOnly using the optional Locale.  Uses the standard Java date format for the locale (SHORT).
   * @param s The date string.  Can be null.
   * @param locale The locale to use for parsing the date (default is the request locale). (<b>Optional</b>)
   * @return The parsed date or null.
   */
  static DateOnly parseDateOnly(String s, Locale locale = null) {
    if (s) {
      def dateFormatter = DateFormat.getDateInstance(DateFormat.SHORT, GlobalUtils.getRequestLocale(locale))
      dateFormatter.setTimeZone(TIMEZONE_UTC)
      def dateOnly = dateFormatter.parse(s)
      return new DateOnly(dateOnly.time)
    }
    return null
  }

  /**
   * Formats the given date for GUI display.  Uses the standard Java SHORT and MEDIUM time format.
   * @param date The date or DateOnly to format.
   * @param locale The locale to use for formatting the date (default is the request Locale). (<b>Optional</b>)
   * @return The date string.
   */
  static String formatDate(Date date, Locale locale = null) {
    def dateFormatter = getDateFormat(GlobalUtils.getRequestLocale(GlobalUtils.getRequestLocale(locale)))
    dateFormatter.setTimeZone(Holders.globals.timeZone)
    return dateFormatter.format(date)
  }

  /**
   * Formats the given DateOnly for GUI display.  Uses the standard Java SHORT and MEDIUM time format.
   * @param date The date or DateOnly to format.
   * @param locale The locale to use for formatting the date (default is the request Locale). (<b>Optional</b>)
   * @return The date string.
   */
  static String formatDate(DateOnly date, Locale locale = null) {
    def dateFormatter = getDateOnlyFormat(GlobalUtils.getRequestLocale(GlobalUtils.getRequestLocale(locale)))
    return dateFormatter.format(new Date(date.time))
  }

  /**
   * Parses the given string into a Date using the locale-independent date format.
   * @param s The date string.  Can be null.
   * @return The parsed date or null.
   */
  static Date parseForm(String s) {
    if (s) {
      def dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      dateFormatter.setTimeZone(Holders.globals.timeZone)
      return dateFormatter.parse(s)
    }
    return null
  }

  /**
   * Formats the given date for form default values in locale-independent format.  Uses
   * the global time zone.
   * @param date The date or DateOnly to format.
   * @return The date string.
   */
  static String formatForm(Date date) {
    def dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    dateFormatter.setTimeZone(Holders.globals.timeZone)
    return dateFormatter.format(date)
  }

  /**
   * Finds the right date formatter for the given locale.
   * @param locale The locale to use for formatting the date (default is the request locale). (<b>Optional</b>)
   * @return The formatter
   */
  static DateFormat getDateFormat(Locale locale = null) {
    def dateFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, GlobalUtils.getRequestLocale(locale))
/* JQWidgets work-around
    if (dateFormatter instanceof SimpleDateFormat) {
      // Work around a jqWidgets bug with 2 year dates and keyboard input.
      def s = dateFormatter.toPattern()
      s = (s =~ /yy/).replaceAll("yyyy")
      dateFormatter = new SimpleDateFormat(s, GlobalUtils.getRequestLocale(locale))
    }
*/
    return dateFormatter
  }

  /**
   * Finds the right date formatter for the given locale.
   * @param locale The locale to use for formatting the date (default is the request locale). (<b>Optional</b>)
   * @return The formatter
   */
  static DateFormat getDateOnlyFormat(Locale locale = null) {
    def dateFormatter = DateFormat.getDateInstance(DateFormat.SHORT, GlobalUtils.getRequestLocale(locale))
/*  JQWidgets work-around
    if (dateFormatter instanceof SimpleDateFormat) {
      def s = dateFormatter.toPattern()
      s = (s =~ /yy/).replaceAll("yyyy")
      dateFormatter = new SimpleDateFormat(s, GlobalUtils.getRequestLocale(locale))
    }
*/
    dateFormatter.setTimeZone(TIMEZONE_UTC)
    return dateFormatter
  }

  /**
   * Truncate the given date/time to nearest minute.
   * @param date The date to truncate.
   * @return The date, with the seconds and lower truncated.
   */
  static Date truncateSeconds(Date date) {
    if (!date) {
      return date
    }
    long millis = (date.time / 60000L) as long
    return new Date(millis * 60000L)
  }

  /**
   * Truncate the given date/time to nearest (lower) value of the given field.
   * @param date The date to truncate.
   * @param field The field from Java's ChronoUnit (YEARS, MONTHS, etc).
   * @param timeZone The time zone to truncate the value in.   (<b>Default</b>: JVM's default).
   * @return The truncated date.
   */
  static Date truncate(Date date, ChronoUnit field, TimeZone timeZone = null) {
    timeZone = timeZone ? timeZone : Holders.globals.timeZone
    Instant instant = date.toInstant()
    ZonedDateTime zonedDateTime = instant.atZone(timeZone.toZoneId())

    // Truncate to the given level
    ZonedDateTime truncatedZonedDateTime
    if (field == ChronoUnit.MONTHS || field == ChronoUnit.YEARS) {
      // Truncate to days first, then set the day to 1 and month to 1(for years)
      truncatedZonedDateTime = zonedDateTime.truncatedTo(ChronoUnit.DAYS)
      truncatedZonedDateTime = truncatedZonedDateTime.withDayOfMonth(1)
      if (field == ChronoUnit.YEARS) {
        truncatedZonedDateTime = truncatedZonedDateTime.withMonth(1)
      }
    } else {
      truncatedZonedDateTime = zonedDateTime.truncatedTo(field)
    }
    Instant truncatedInstant = truncatedZonedDateTime.toInstant()
    return Date.from(truncatedInstant)
  }

  /**
   * Round up to the next value for the given date field.
   * @param date The date to round up to.
   * @param field The field from Java's ChronoUnit.
   * @param timeZone The time zone to round up the value in.   (<b>Default</b>: JVM's default).
   * @return The rounded up date.
   */
  static Date ceiling(Date date, ChronoUnit field, TimeZone timeZone = null) {
    timeZone = timeZone ? timeZone : Holders.globals.timeZone

    // First, truncate to the given level
    Instant instant = date.toInstant()
    ZonedDateTime zonedDateTime = instant.atZone(timeZone.toZoneId())

    // Truncate to the given level
    ZonedDateTime truncatedZonedDateTime
    if (field == ChronoUnit.MONTHS || field == ChronoUnit.YEARS) {
      // Truncate to days first, then set the day to 1 and month to 1(for years)
      truncatedZonedDateTime = zonedDateTime.truncatedTo(ChronoUnit.DAYS)
      truncatedZonedDateTime = truncatedZonedDateTime.withDayOfMonth(1)
      if (field == ChronoUnit.YEARS) {
        truncatedZonedDateTime = truncatedZonedDateTime.withMonth(1)
      }
    } else {
      truncatedZonedDateTime = zonedDateTime.truncatedTo(field)
    }

    // Then add one of the given units to the truncated value.
    ZonedDateTime ceilingDate = truncatedZonedDateTime.plus(1, field)
    return Date.from(ceilingDate.toInstant())
  }

  /**
   * Determines the UI TimeZone.
   * @return The time zone.
   */
  static TimeZone determineTimeZone() {
    return Holders.globals.timeZone
  }

  /**
   * Adds the number of months to the given date.
   * @param date The date.
   * @param months The number of months (can be negative).
   * @return The date plus the months.
   */
  static Date addMonths(Date date, int months) {
    Instant instant = date.toInstant()
    ZonedDateTime zonedDateTime = instant.atZone(Holders.globals.timeZone.toZoneId())
    zonedDateTime = zonedDateTime.plusMonths(months)
    return Date.from(zonedDateTime.toInstant())
  }

  /**
   * Adds the number of years to the given date.
   * @param date The date.
   * @param years The number of years (can be negative).
   * @return The date plus the years.
   */
  static Date addYears(Date date, int years) {
    Instant instant = date.toInstant()
    ZonedDateTime zonedDateTime = instant.atZone(Holders.globals.timeZone.toZoneId())
    zonedDateTime = zonedDateTime.plusYears(years)
    return Date.from(zonedDateTime.toInstant())
  }

  /**
   * Subtract the given number of days from the given date/time.  Supports partial days.
   *
   * @param date The date to subtract from.
   * @param days The number of days to subtract.  Fractions allowed.
   * @return The new date/time.
   */
  static Date subtractDays(Date date, BigDecimal days) {
    long millis = (long) (date.time - days * MILLIS_PER_DAY)
    return new Date(millis)
  }

/**
 * Build a human-readable elapsed time string from the given elapsed time (ms), but only shows one number
 * to make it readable.  Use this in scenarios when super-precise display is not needed.
 * Supports seconds, minutes, days and years.  This method does <b>not</b> round to nearest whole number when precision is
 * set to LOW (default).<p/>
 * This method ignores leap years. All years are assumed to be 365 days.
 * @param elapsedTimeMS The elapsed time (ms).
 * @param locale The locale to use for the elapsed time string (<b>Default:</b> Request Locale, or Locale.default).
 * @param precision The precision to display. (<b>Default:</b> PRECISION_LOW).
 * @return The string. (e.g. for 70 seconds, returns "1 minute").
 */
  static String formatElapsedTime(long elapsedTimeMS, Locale locale, String precision = PRECISION_LOW) {
    // Calculate the actual values with decimals for the units we support.
    BigDecimal seconds = elapsedTimeMS / 1000.0
    BigDecimal minutes = seconds / 60.0
    BigDecimal hours = seconds / 3600.0
    BigDecimal days = seconds / (24 * 3600.0)
    BigDecimal years = seconds / (365 * 24 * 3600.0)
    //println "seconds = $seconds"

    // Figure out which is the lowest unit that is greater than 1.0
    String units = 'seconds'
    def values = [seconds as int, null]
    if (years >= 1.0) {
      // Need two values (years and days) for the year case.
      units = 'years'
      values[0] = years as int
      values[1] = (years.remainder(1.0) * 365) as int  // Number of days in the fraction of the year
    } else if (days >= 1.0) {
      units = 'days'
      values[0] = days as int
      values[1] = (days.remainder(1.0) * 24) as int  // Number of hours in the fraction of the days
    } else if (hours >= 1.0) {
      units = 'hours'
      values[0] = hours as int
      values[1] = (hours.remainder(1.0) * 60) as int  // Number of minutes in the fraction of the hours
    } else if (minutes >= 1.0) {
      units = 'minutes'
      values[0] = minutes as int
      values[1] = (minutes.remainder(1.0) * 60) as int  // Number of seconds in the fraction of the minutes
    }

    def key = "time.elapsed.${units}.${precision}"
    //time.elapsed.seconds.low={0,number} {0,choice,0#seconds|1#second|1<seconds}
    //time.elapsed.minutes.low={0} {0,choice,0#minutes|1#minute|1<minutes}
    //time.elapsed.hours.low={0} {0,choice,0#hours|1#hour|1<hours}
    //time.elapsed.days.low={0} {0,choice,0#days|1#day|1<days}
    //time.elapsed.years.low={0} {0,choice,0#years|1#year|1<years}, {1} {1,choice,0#days|1#day|1<days}

    //time.elapsed.seconds.med={0,number} {0,choice,0#seconds|1#second|1<seconds}
    //time.elapsed.minutes.med={0} {0,choice,0#minutes|1#minute|1<minutes}, {1,number} {1,choice,0#seconds|1#second|1<seconds}
    //time.elapsed.hours.med={0} {0,choice,0#hours|1#hour|1<hours}, {1} {1,choice,0#minutes|1#minute|1<minutes}
    //time.elapsed.days.med={0} {0,choice,0#days|1#day|1<days}, {1} {1,choice,0#hours|1#hour|1<hours}
    //time.elapsed.years.med={0} {0,choice,0#years|1#year|1<years}, {1} {1,choice,0#days|1#day|1<days}

    String formatPattern = GlobalUtils.lookup(key, locale, values as Object[])
    //println "formatPattern = $formatPattern for $locale"
    return formatPattern

  }

  /**
   * Build a human-readable relative time. Only shows one number
   * to make it readable.  Handles past times (negative) and future times ('2 minutes ago' and '2 minutes from now').
   * Use this in scenarios when super-precise display is not needed.
   * Supports seconds, minutes, days and years.  This method does <b>not</b> round to nearest whole number when precision is
   * set to LOW (default).<p/>
   * This method ignores leap years. All years are assumed to be 365 days.
   * @param relativeTime The related time (ms).
   * @param locale The locale to use for the elapsed time string (<b>Default:</b> Request Locale, or Locale.default).
   * @param precision The precision to display. (<b>Default:</b> PRECISION_LOW).
   * @return The string. (e.g. for 70 seconds, returns "1 minute ago").
   */
  static String formatRelativeTime(long relativeTime, Locale locale = null, String precision = PRECISION_LOW) {
    def messageKey = 'time.fromNow.label'
    if (relativeTime <= 0) {
      messageKey = 'time.ago.label'
      relativeTime = -relativeTime
    }
    def s = formatElapsedTime(relativeTime, locale, precision)
    return GlobalUtils.lookup(messageKey, locale, s)
  }
}
