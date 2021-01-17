package org.simplemes.eframe.web.report

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.date.DateRange
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.reports.ReportTimeIntervalEnum
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * Tests.
 */
class ReportTimeIntervalEnumSpec extends BaseSpecification {

  def "verify that toString returns the enum name"() {
    expect: 'the method works correctly'
    for (t in ReportTimeIntervalEnum.enumConstants) {
      assert t.toString() == t.name()
    }
  }

  def "verify that toStringLocalized works"() {
    expect: 'the method localizes correctly'
    ReportTimeIntervalEnum.TODAY.toStringLocalized(locale) == GlobalUtils.lookup("reportTimeInterval.TODAY.label", null, locale)

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

  def "verify that determineRange works with multiple time zones and various enum values"() {
    given: 'a time zone over the date line'
    Holders.globals.timeZone = timeZone

    and: 'the expected values for the start and end of the time range'
    def start = Calendar.getInstance(DateUtils.determineTimeZone())
    start.clear()
    start.set(startDate[0], startDate[1], startDate[2], 0, 0, 0)

    def end = Calendar.getInstance(DateUtils.determineTimeZone())
    end.clear()
    end.set(endDate[0], endDate[1], endDate[2], 0, 0, 0)

    when: 'the range is calculated for a single day'
    def dateRange = interval.determineRange(new Date(UnitTestUtils.SAMPLE_TIME_MS))

    then: 'the date range is correct'
    dateRange == new DateRange(start.time, end.time)

    where:
    timeZone                           | interval                             | startDate     | endDate
    TimeZone.getTimeZone('Asia/Tokyo') | ReportTimeIntervalEnum.TODAY         | [2009, 1, 14] | [2009, 1, 15]
    TimeZone.default                   | ReportTimeIntervalEnum.TODAY         | [2009, 1, 13] | [2009, 1, 14]
    TimeZone.default                   | ReportTimeIntervalEnum.YESTERDAY     | [2009, 1, 12] | [2009, 1, 13]
    TimeZone.default                   | ReportTimeIntervalEnum.THIS_MONTH    | [2009, 1, 1]  | [2009, 2, 1]
    TimeZone.default                   | ReportTimeIntervalEnum.LAST_MONTH    | [2009, 0, 1]  | [2009, 1, 1]
    TimeZone.default                   | ReportTimeIntervalEnum.LAST_6_MONTHS | [2008, 6, 1]  | [2009, 1, 1]
    TimeZone.default                   | ReportTimeIntervalEnum.THIS_YEAR     | [2009, 0, 1]  | [2010, 0, 1]
    TimeZone.default                   | ReportTimeIntervalEnum.LAST_YEAR     | [2008, 0, 1]  | [2009, 0, 1]
  }

  def "verify that date range for last X days and hours works"() {
    given: 'the start and end for the last 24 hours'
    def start = new Date(UnitTestUtils.SAMPLE_TIME_MS - elapsedTime)
    def end = new Date(UnitTestUtils.SAMPLE_TIME_MS)

    when: 'the range is calculated'
    def dateRange = interval.determineRange(new Date(UnitTestUtils.SAMPLE_TIME_MS))

    then: 'it is correct'
    dateRange == new DateRange(start, end)

    where:
    interval                             | elapsedTime
    ReportTimeIntervalEnum.LAST_24_HOURS | DateUtils.MILLIS_PER_DAY
    ReportTimeIntervalEnum.LAST_7_DAYS   | 7 * DateUtils.MILLIS_PER_DAY
    ReportTimeIntervalEnum.LAST_30_DAYS  | 30 * DateUtils.MILLIS_PER_DAY
  }
}
