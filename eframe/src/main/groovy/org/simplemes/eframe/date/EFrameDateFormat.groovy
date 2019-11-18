package org.simplemes.eframe.date

import com.fasterxml.jackson.databind.util.StdDateFormat


/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the date/time format to use a standard ISO formatter.
 */
class EFrameDateFormat extends StdDateFormat {

  EFrameDateFormat() {
  }

  EFrameDateFormat(TimeZone tz, Locale loc, Boolean lenient, boolean formatTzOffsetWithColon) {
    super(tz, loc, lenient, formatTzOffsetWithColon)
  }

/*
  @Override
  StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
    //println "formatting date = $date"
    return super.format(date, toAppendTo, fieldPosition)
  }

*/

  @Override
  protected void _format(TimeZone tz, Locale loc, Date date, StringBuffer buffer) {
    //println "_formatting date = $date, $loc, $buffer"
    buffer << ISODate.format(date)
  }

  @Override
  StdDateFormat clone() {
    //println "_timezone = $_timezone"
    return new EFrameDateFormat(_timezone, _locale, _lenient, false)
  }
}
