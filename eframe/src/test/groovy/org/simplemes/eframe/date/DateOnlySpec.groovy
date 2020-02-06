/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.date

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils

import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 *
 */
class DateOnlySpec extends BaseSpecification {

  def "verify that a date created with the current time is correct"() {
    when: 'a date only is created'
    def d = new DateOnly()

    then: 'the time portion is all 0:0'
    Instant.ofEpochMilli(d.time).truncatedTo(ChronoUnit.DAYS) == Instant.ofEpochMilli(d.time)
  }

  def "verify that a date created with a time in milliseconds that is midnight"() {
    when: 'a date only is created'
    def d = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)

    then: 'the time portion is all 0:0'
    Instant.ofEpochMilli(d.time).truncatedTo(ChronoUnit.DAYS) == Instant.ofEpochMilli(d.time)
  }

  def "verify that the date constructor works"() {
    when: 'a date only is created'
    def d = new DateOnly(new Date(UnitTestUtils.SAMPLE_DATE_ONLY_MS))

    then: 'the date only is correct'
    d.toString() == UnitTestUtils.SAMPLE_ISO_DATE_ONLY_STRING
  }

  def "verify that the date constructor detects missing date input"() {
    when: 'a date only is created'
    new DateOnly((Date) null)?.toString()

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['date', 'not'])
  }

  def "verify that toString works"() {
    expect: 'a date only is created correctly'
    new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS).toString() == UnitTestUtils.SAMPLE_ISO_DATE_ONLY_STRING
  }

}
