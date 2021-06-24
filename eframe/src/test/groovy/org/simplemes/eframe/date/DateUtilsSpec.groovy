/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.date


import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils

import java.time.temporal.ChronoUnit

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
    // Need to remove the comma's since some versions of the JDK inserts a comma after it (gradle vs. IDEA difference).
    DateUtils.formatDate(new Date(UnitTestUtils.SAMPLE_TIME_MS), Locale.US).replace(',', '') == '2/13/2009 6:31:30 PM'
    DateUtils.formatDate(new Date(UnitTestUtils.SAMPLE_TIME_MS), Locale.GERMANY).replace(',', '') == '13.02.2009 18:31:30'
    DateUtils.formatDate(new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS), Locale.US) == '6/15/2010'
    DateUtils.formatDate(new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS), Locale.GERMANY) == '15.06.2010'
  }

  def "verify that determineDateFormat returns the right format"() {
    expect:
    DateUtils.getDateFormat(Locale.US).toPattern().replace(',', '') == 'M/d/yyyy h:mm:ss a'
    DateUtils.getDateFormat(Locale.GERMANY).toPattern().replace(',', '') == 'dd.MM.yyyy HH:mm:ss'
    DateUtils.getDateOnlyFormat(Locale.US).toPattern() == 'M/d/yyyy'
    DateUtils.getDateOnlyFormat(Locale.GERMANY).toPattern() == 'dd.MM.yyyy'
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

  def "verify that subtractDays works for Dates"() {
    expect: 'works with whole numbers'
    Date now = new Date()
    Date d1 = DateUtils.subtractDays(now, 1.0)
    assert now.time - d1.time == 86400000

    and: 'works with fractions'
    Date d2 = DateUtils.subtractDays(now, 1.2)
    assert now.time - d2.time == 103680000
  }

  def "verify that subtractDays works for DateOnly"() {
    expect: 'works with whole numbers'
    DateOnly now = new DateOnly()
    DateOnly d1 = DateUtils.subtractDays(now, 1)
    assert now.time - d1.time == 86400000
  }

  def "verify that formatElapsedTime handles supported cases"() {
    expect: ''
    DateUtils.formatElapsedTime(elapsed, locale, precision) == result

    where:
    elapsed                                          | locale         | precision                  | result
    0 * 1000L                                        | Locale.US      | DateUtils.PRECISION_LOW    | '0 seconds'
    0 * 1000L                                        | Locale.GERMAN  | DateUtils.PRECISION_LOW    | '0 sekunden'
    1 * 1000L                                        | null           | DateUtils.PRECISION_LOW    | '1 second'
    20 * 1000L                                       | Locale.ENGLISH | DateUtils.PRECISION_LOW    | '20 seconds'
    20123                                            | Locale.ENGLISH | DateUtils.PRECISION_LOW    | '20 seconds'
    50 * 1000L + 20                                  | Locale.ENGLISH | DateUtils.PRECISION_LOW    | '50 seconds'
    60 * 1000L                                       | Locale.ENGLISH | DateUtils.PRECISION_LOW    | '1 minute'
    91 * 1000L                                       | Locale.ENGLISH | DateUtils.PRECISION_LOW    | '1 minute'
    120 * 1000L                                      | Locale.ENGLISH | DateUtils.PRECISION_LOW    | '2 minutes'
    59 * 60 * 1000L + 20                             | Locale.ENGLISH | DateUtils.PRECISION_LOW    | '59 minutes'

    60 * 60 * 1000L                                  | Locale.ENGLISH | DateUtils.PRECISION_LOW    | '1 hour'
    119 * 60 * 1000L                                 | Locale.ENGLISH | DateUtils.PRECISION_LOW    | '1 hour'
    120 * 60 * 1000L                                 | Locale.ENGLISH | DateUtils.PRECISION_LOW    | '2 hours'
    23 * 60 * 60 * 1000L + 20                        | Locale.ENGLISH | DateUtils.PRECISION_LOW    | '23 hours'

    24 * 60 * 60 * 1000L                             | Locale.ENGLISH | DateUtils.PRECISION_LOW    | '1 day'
    47 * 60 * 60 * 1000L                             | Locale.ENGLISH | DateUtils.PRECISION_LOW    | '1 day'
    48 * 60 * 60 * 1000L                             | Locale.ENGLISH | DateUtils.PRECISION_LOW    | '2 days'
    120 * 60 * 60 * 1000L + 20                       | Locale.ENGLISH | DateUtils.PRECISION_LOW    | '5 days'

    364 * 24 * 60 * 60 * 1000L                       | Locale.ENGLISH | DateUtils.PRECISION_LOW    | '364 days'
    365 * 24 * 60 * 60 * 1000L                       | Locale.ENGLISH | DateUtils.PRECISION_LOW    | '1 year, 0 days'

    366 * 24 * 60 * 60 * 1000L + 20                  | Locale.ENGLISH | DateUtils.PRECISION_LOW    | '1 year, 1 day'
    367 * 24 * 60 * 60 * 1000L + 20                  | Locale.ENGLISH | DateUtils.PRECISION_LOW    | '1 year, 2 days'
    (365 * 2) * 24 * 60 * 60 * 1000L + 20            | Locale.ENGLISH | DateUtils.PRECISION_LOW    | '2 years, 0 days'
    (365 * 2 + 1) * 24 * 60 * 60 * 1000L + 20        | Locale.ENGLISH | DateUtils.PRECISION_LOW    | '2 years, 1 day'
    (365 * 2 + 3) * 24 * 60 * 60 * 1000L + 20        | Locale.ENGLISH | DateUtils.PRECISION_LOW    | '2 years, 3 days'

    0 * 1000L                                        | Locale.GERMANY | DateUtils.PRECISION_MEDIUM | '0 sekunden'
    0 * 1000L                                        | Locale.ENGLISH | DateUtils.PRECISION_MEDIUM | '0 seconds'
    50 * 1000L + 20                                  | Locale.ENGLISH | DateUtils.PRECISION_MEDIUM | '50 seconds'

    120 * 1000L                                      | Locale.ENGLISH | DateUtils.PRECISION_MEDIUM | '2 minutes, 0 seconds'
    121 * 1000L                                      | Locale.ENGLISH | DateUtils.PRECISION_MEDIUM | '2 minutes, 1 second'
    154 * 1000L                                      | Locale.ENGLISH | DateUtils.PRECISION_MEDIUM | '2 minutes, 34 seconds'

    23 * 60 * 60 * 1000L + 20                        | Locale.ENGLISH | DateUtils.PRECISION_MEDIUM | '23 hours, 0 minutes'
    (23 * 60 + 1) * 60 * 1000L + 20                  | Locale.ENGLISH | DateUtils.PRECISION_MEDIUM | '23 hours, 1 minute'
    (23 * 60 + 34) * 60 * 1000L + 20                 | Locale.ENGLISH | DateUtils.PRECISION_MEDIUM | '23 hours, 34 minutes'

    120 * 60 * 60 * 1000L + 20                       | Locale.ENGLISH | DateUtils.PRECISION_MEDIUM | '5 days, 0 hours'
    121 * 60 * 60 * 1000L + 20                       | Locale.ENGLISH | DateUtils.PRECISION_MEDIUM | '5 days, 1 hour'
    126 * 60 * 60 * 1000L + 20                       | Locale.ENGLISH | DateUtils.PRECISION_MEDIUM | '5 days, 6 hours'

    364 * 24 * 60 * 60 * 1000L                       | Locale.ENGLISH | DateUtils.PRECISION_MEDIUM | '364 days, 0 hours'
    365 * 24 * 60 * 60 * 1000L                       | Locale.ENGLISH | DateUtils.PRECISION_MEDIUM | '1 year, 0 days'
    366 * 24 * 60 * 60 * 1000L + 20                  | Locale.ENGLISH | DateUtils.PRECISION_MEDIUM | '1 year, 1 day'
    376 * 24 * 60 * 60 * 1000L + 20                  | Locale.ENGLISH | DateUtils.PRECISION_MEDIUM | '1 year, 11 days'

    366 * 24 * 60 * 60 * 1000L + 60 * 60 * 1000L     | Locale.ENGLISH | DateUtils.PRECISION_MEDIUM | '1 year, 1 day'
    367 * 24 * 60 * 60 * 1000L + 2 * 60 * 60 * 1000L | Locale.ENGLISH | DateUtils.PRECISION_MEDIUM | '1 year, 2 days'
  }

  def "verify that formatRelativeTime handles supported cases"() {
    expect: ''
    DateUtils.formatRelativeTime(relative, locale, precision) == result

    where:
    relative    | locale    | precision               | result
    0 * 1000L   | Locale.US | DateUtils.PRECISION_LOW | '0 seconds ago'
    30 * 1000L  | Locale.US | DateUtils.PRECISION_LOW | '30 seconds from now'
    -50 * 1000L | Locale.US | DateUtils.PRECISION_LOW | '50 seconds ago'

  }

}
