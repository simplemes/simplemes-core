/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.date

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils

import java.text.SimpleDateFormat

/**
 * Tests.
 */
class ISODateSpec extends BaseSpecification {

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that format produces the right ISO date string"() {
    given: 'a date/time in the PDT timezone'
    def date
    if (input instanceof String) {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
      sdf.setLenient(false)
      sdf.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"))
      date = sdf.parse(input)
    } else {
      date = new Date(input)
    }

    when: 'the ISO date string is created'
    def s = ISODate.format(date)

    then: 'it matches'
    s == result

    where:
    input                                    | result
    '2010-11-14 13:24:56.987'                | '2010-11-14T21:24:56.987Z'
    '2010-06-14 13:24:56.987'                | '2010-06-14T20:24:56.987Z'
    UnitTestUtils.SAMPLE_TIME_MS             | UnitTestUtils.SAMPLE_ISO_TIME_STRING
    UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS | UnitTestUtils.SAMPLE_ISO_TIME_NO_FRACTION_STRING
  }

  def "verify that parse works with supported ISO formats"() {
    expect: 'the date is parsed'
    ISODate.parse(input) == new Date(result)

    where:
    input                                | result
    UnitTestUtils.SAMPLE_ISO_TIME_STRING | UnitTestUtils.SAMPLE_TIME_MS
    '2009-02-13T23:31:30Z'               | 1234567890000L
    '2009-02-13T18:31:30.000-05:00'      | 1234567890000L
    '2009-02-13T18:31:30-05:00'          | 1234567890000L
    '2009-02-13T18:31:30-05:00'          | 1234567890000L
    '2009-02-13T18:31:30.000-0500'       | 1234567890000L
  }

  def "verify that parseDateOnly works with supported ISO formats"() {
    expect: 'the date is parsed'
    ISODate.parseDateOnly(input) == new DateOnly(result)

    where:
    input                                     | result
    UnitTestUtils.SAMPLE_ISO_DATE_ONLY_STRING | UnitTestUtils.SAMPLE_DATE_ONLY_MS
  }

  def "verify that format produces the right ISO date string - DateOnly"() {
    expect: 'the ISO date string is created'
    ISODate.format(date) == result

    where:
    date                                            | result
    new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS) | UnitTestUtils.SAMPLE_ISO_DATE_ONLY_STRING
  }
}
