/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.date

import org.simplemes.eframe.misc.ArgumentUtils

import java.text.SimpleDateFormat
import java.time.Instant

class ISODate {

  /**
   * The UTC Timezone.
   */
  private static final TimeZone TIMEZONE_UTC = TimeZone.getTimeZone('UTC')

  /**
   * Formats the given date in standard ISO 8601 format, with UTZ Timezone (e.g. 2010-11-14T13:24:56.987Z).
   * @param date The date.
   * @return The formatted string.
   */

  static String format(Date date) {
    ArgumentUtils.checkMissing(date, 'date')
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
    sdf.setLenient(false)
    // Format and add the trailing colon in the TZ section for proper ISO format.
    sdf.setTimeZone(TIMEZONE_UTC)
    return sdf.format(date) + 'Z'
  }

  /**
   * Formats the given dateOnly in standard ISO 8601 format.
   * @param date The dateOnly to format.
   * @return The formatted string.
   */
  static String format(DateOnly dateOnly) {
    ArgumentUtils.checkMissing(dateOnly, 'dateOnly')
    return Instant.ofEpochMilli(dateOnly.time).toString()[0..9]
  }

  /**
   * Parses the given date/time in standard ISO format.
   * @param s The date/time formatted.
   * @return The parse date/time.
   */
  static Date parse(String s) {
/*
    yyyy-mm-ddThh:mm:ss.sssZZZZZ.| 2009-02-13T18:31:30.000-05:00 (Format used when writing a date/time).
    yyyy-mm-ddThh:mm:ssZZZZZ.| 2009-02-13T18:31:30-05:00
    yyyy-mm-ddThh:mm:ss.sssZ.| 2009-02-13T23:31:30.000Z  (Always in UTC Timezone)
    yyyy-mm-ddThh:mm:ssZ.| 2009-02-13T23:31:30Z  (Always in UTC Timezone)
    yyyy-mm-ddThh:mm:ss.SSSZZZZZ | 2018-11-13T12:41:30.000-0500
*/

    ArgumentUtils.checkMissing(s, 's')
    def formatString
    def timeZone = null
    switch (s.length()) {
      case 20:
        // TZ is 'Z' in the string.
        formatString = "yyyy-MM-dd'T'HH:mm:ss"
        timeZone = TIMEZONE_UTC
        // Remove the trailing Z since SimpleDateFormat can't handle it.
        s = s.substring(0, s.length() - 1)
        break
      case 24:
        // TZ is 'Z' in the string.
        formatString = "yyyy-MM-dd'T'HH:mm:ss.SSS"
        timeZone = TIMEZONE_UTC
        // Remove the trailing Z since SimpleDateFormat can't handle it.
        s = s.substring(0, s.length() - 1)
        break
      case 25:
        // Remove the trailing colon since SimpleDateFormat can't handle it.
        s = s.substring(0, 22) + s.substring(23)
        formatString = "yyyy-MM-dd'T'HH:mm:ssZ"
        break
      case 28:
        formatString = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ"
        break
      case 29:
        formatString = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        // Remove the trailing colon since SimpleDateFormat can't handle it.
        s = s.substring(0, 26) + s.substring(27)
        break
      default:
        throw new IllegalArgumentException("ISO Date is wrong length (must be 20, 24, 25, 28 or 29).  Value = '${s}'")
    }
    SimpleDateFormat sdf = new SimpleDateFormat(formatString)
    if (timeZone) {
      // Adjust the TZ to UTC if needed.
      sdf.setTimeZone(timeZone)
    }
    sdf.setLenient(false)
    return sdf.parse(s)
  }

  /**
   * Parses the given date only standard ISO format.
   * @param s The date formatted.
   * @return The parse date/time.
   */
  static DateOnly parseDateOnly(String s) {
    ArgumentUtils.checkMissing(s, 's')
    if (s.length() != 10) {
      //2011-04-23
      throw new IllegalArgumentException("s ($s) is wrong length (must be 10)")
    }
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
    sdf.setLenient(false)
    // Force the UTC timezone for all DateOnly uses.
    sdf.setTimeZone(TIMEZONE_UTC)
    // Create a generic Date, then convert to DateOnly.
    return new DateOnly(sdf.parse(s).time)
  }

}
