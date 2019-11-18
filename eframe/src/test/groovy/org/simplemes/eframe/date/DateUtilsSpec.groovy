package org.simplemes.eframe.date


import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils

import java.time.temporal.ChronoUnit

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class DateUtilsSpec extends BaseSpecification {

  def "verify that parse of user input works"() {
    given: 'a date'
    def date = new Date(UnitTestUtils.SAMPLE_TIME_MS)

    expect: 'the parse works with round trip.'
    // Compare after round trip, but only to seconds.  The default toString() only shows seconds, so it is Ok.
    DateUtils.parseDate(DateUtils.formatDate(date, Locale.US), Locale.US).toString() == date.toString()
    DateUtils.parseDate(DateUtils.formatDate(date, Locale.GERMANY), Locale.GERMANY).toString() == date.toString()
    DateUtils.parseDate(null) == null
  }

  def "verify that parseDateOnly of user input works"() {
    given: 'a date'
    def date = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)

    expect: 'the parse works with round trip.'
    // Compare after round trip, but only to seconds.  The default toString() only shows seconds, so it is Ok.
    DateUtils.parseDateOnly(DateUtils.formatDate(date, Locale.US), Locale.US).toString() == date.toString()
    DateUtils.parseDateOnly(DateUtils.formatDate(date, Locale.GERMANY), Locale.GERMANY).toString() == date.toString()
    DateUtils.parseDateOnly(null) == null
  }

  def "verify that format works for display"() {
    expect:
    DateUtils.formatDate(new Date(UnitTestUtils.SAMPLE_TIME_MS), Locale.US) == '2/13/09 6:31:30 PM'
    DateUtils.formatDate(new Date(UnitTestUtils.SAMPLE_TIME_MS), Locale.GERMANY) == '13.02.09 18:31:30'
    DateUtils.formatDate(new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS), Locale.US) == '6/15/10'
    DateUtils.formatDate(new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS), Locale.GERMANY) == '15.06.10'
  }

  def "verify that determineDateFormat returns the right format"() {
    expect:
    DateUtils.getDateFormat(Locale.US).toPattern() == 'M/d/yy h:mm:ss a'
    DateUtils.getDateFormat(Locale.GERMANY).toPattern() == 'dd.MM.yy HH:mm:ss'
    DateUtils.getDateOnlyFormat(Locale.US).toPattern() == 'M/d/yy'
    DateUtils.getDateOnlyFormat(Locale.GERMANY).toPattern() == 'dd.MM.yy'
  }

  def "verify that formatForm works for form values"() {
    expect:
    DateUtils.formatForm(new Date(UnitTestUtils.SAMPLE_TIME_MS)) == '2009-02-13 18:31:30'
  }

  def "verify that parseForm works for form values"() {
    expect:
    DateUtils.parseForm('2010-06-14 21:34:38') == new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)
  }

  def "verify that truncateSeconds works on supported cases"() {

    expect: 'the truncate works'
    DateUtils.truncateSeconds(new Date(input)) == new Date(results)
    DateUtils.truncateSeconds(null) == null

    where:
    input         | results
    1443186735948 | 1443186720000
    1443186775952 | 1443186720000
    1443186715944 | 1443186660000
    1443186785953 | 1443186780000
  }

  def "verify that date truncate works with days"() {
    expect: 'truncate to work with a date truncation'
    def midnight = Calendar.getInstance(DateUtils.determineTimeZone())
    midnight.clear()
    midnight.set(2009, 1, 13, 0, 0, 0)
    DateUtils.truncate(new Date(UnitTestUtils.SAMPLE_TIME_MS), ChronoUnit.DAYS) == midnight.getTime()
  }

  def "verify that date truncate works with months"() {
    expect: 'truncate to work with a date truncation'
    def midnight = Calendar.getInstance(DateUtils.determineTimeZone())
    midnight.clear()
    midnight.set(2009, 2, 1, 0, 0, 0)
    DateUtils.truncate(new Date(UnitTestUtils.SAMPLE_TIME_MS + DateUtils.MILLIS_PER_DAY * 31), ChronoUnit.MONTHS) == midnight.getTime()
  }

  def "verify that date truncate works with years"() {
    expect: 'truncate to work with a date truncation'
    def midnight = Calendar.getInstance(DateUtils.determineTimeZone())
    midnight.clear()
    midnight.set(2009, 0, 1, 0, 0, 0)
    DateUtils.truncate(new Date(UnitTestUtils.SAMPLE_TIME_MS), ChronoUnit.YEARS) == midnight.getTime()
  }

  def "verify that date ceiling works with days"() {
    expect: 'the ceiling to work with days'
    def midnight = Calendar.getInstance(DateUtils.determineTimeZone())
    midnight.clear()
    midnight.set(2009, 1, 14, 0, 0, 0)
    DateUtils.ceiling(new Date(UnitTestUtils.SAMPLE_TIME_MS), ChronoUnit.DAYS) == midnight.getTime()
  }

  def "verify that date ceiling works with months"() {
    expect: 'the ceiling to work with months'
    def midnight = Calendar.getInstance(DateUtils.determineTimeZone())
    midnight.clear()
    midnight.set(2009, 2, 1, 0, 0, 0)
    DateUtils.ceiling(new Date(UnitTestUtils.SAMPLE_TIME_MS), ChronoUnit.MONTHS) == midnight.getTime()
  }

  def "verify that date ceiling works with years"() {
    expect: 'the ceiling to work with years'
    def midnight = Calendar.getInstance(DateUtils.determineTimeZone())
    midnight.clear()
    midnight.set(2010, 0, 1, 0, 0, 0)
    DateUtils.ceiling(new Date(UnitTestUtils.SAMPLE_TIME_MS), ChronoUnit.YEARS) == midnight.getTime()
  }

  def "verify that addMonths works"() {
    when: 'a month is subtracted from the date'
    def added = DateUtils.addMonths(new Date(UnitTestUtils.SAMPLE_TIME_MS), -1)

    then: 'The new date is 31 days less than the original (days in month of Jan).'
    def deltaDays = (added.time - UnitTestUtils.SAMPLE_TIME_MS) / DateUtils.MILLIS_PER_DAY
    deltaDays == -31
  }

  def "verify that addYears works"() {
    when: 'a year is added to the date'
    def added = DateUtils.addYears(new Date(UnitTestUtils.SAMPLE_TIME_MS), 1)

    then: 'The new date is 365 days later than the original'
    def deltaDays = (added.time - UnitTestUtils.SAMPLE_TIME_MS) / DateUtils.MILLIS_PER_DAY
    deltaDays == 365
  }

  def "verify that subtractDays works"() {
    expect: 'works with whole numbers'
    Date now = new Date()
    Date d1 = DateUtils.subtractDays(now, 1.0)
    assert now.time - d1.time == 86400000

    and: 'works with fractions'
    Date d2 = DateUtils.subtractDays(now, 1.2)
    assert now.time - d2.time == 103680000
  }
}
