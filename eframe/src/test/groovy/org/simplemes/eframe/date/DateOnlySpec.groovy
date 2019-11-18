package org.simplemes.eframe.date

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils

import java.text.SimpleDateFormat

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
    SimpleDateFormat timeOnly = new SimpleDateFormat("HH:mm:ss.SSSZ")
    timeOnly.setTimeZone(TimeZone.getTimeZone("UTC"))
    timeOnly.format(d) == "00:00:00.000+0000"

    and: 'toString works'
    d.toString()
  }

  def "verify that a date created with a time in milliseconds that is midnight"() {
    when: 'a date only is created'
    def d = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)

    then: 'the time portion is all 0:0'
    SimpleDateFormat timeOnly = new SimpleDateFormat("HH:mm:ss.SSSZ")
    timeOnly.setTimeZone(TimeZone.getTimeZone("UTC"))
    timeOnly.format(d) == "00:00:00.000+0000"
  }

  def "verify that a date created with a time in milliseconds that is not midnight - should fail"() {
    when: 'a date only is created'
    new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS + 1).toString()

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['midnight'])
  }

  def "verify that a date constructor with string works"() {
    expect: 'a date only is created correctly'
    new DateOnly(UnitTestUtils.SAMPLE_ISO_DATE_ONLY_STRING) == new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
  }


}
