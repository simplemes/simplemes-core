package org.simplemes.eframe.date

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils

import java.time.Instant
import java.time.temporal.ChronoUnit

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

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

  def "verify that toString works"() {
    expect: 'a date only is created correctly'
    new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS).toString() == UnitTestUtils.SAMPLE_ISO_DATE_ONLY_STRING
  }

}
