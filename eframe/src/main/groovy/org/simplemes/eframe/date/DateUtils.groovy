package org.simplemes.eframe.date

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.i18n.GlobalUtils

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

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
    def dateFormatter
    if (date instanceof DateOnly) {
      dateFormatter = getDateOnlyFormat(GlobalUtils.getRequestLocale(GlobalUtils.getRequestLocale(locale)))
    } else {
      dateFormatter = getDateFormat(GlobalUtils.getRequestLocale(GlobalUtils.getRequestLocale(locale)))
      dateFormatter.setTimeZone(Holders.globals.timeZone)
    }
    return dateFormatter.format(date)
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
   * Forces a change to the record's lastUpdated field to force a save of the record.
   * This will make sure the date is changed.
   * @param object The domain object.  Must have a 'lastUpdated' field.
   */
  @CompileDynamic
  static void forceUpdatedDate(object) {
    def oldDate = object.lastUpdated
    def newDate = new Date()
    if (oldDate == newDate) {
      // The date has not changed, so force a 1 ms change.
      newDate = new Date(newDate.time + 1)
    }
    object.lastUpdated = newDate
  }

}
