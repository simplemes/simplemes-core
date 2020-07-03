/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data.format


import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.date.ISODate
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils

import java.text.ParseException

/**
 * Tests.
 */
@SuppressWarnings("GroovyAssignabilityCheck")
class DateFieldFormatSpec extends BaseSpecification {


  @SuppressWarnings('unused')
  static specNeeds = [SERVER]

  def "verify that id and toString work and the format is registered in the BasicFieldFormat class"() {
    expect:
    DateFieldFormat.instance.id == DateFieldFormat.ID
    DateFieldFormat.instance.toString() == 'Date/Time'
    DateFieldFormat.instance.type == Date
    BasicFieldFormat.coreValues.contains(DateFieldFormat)
  }

  def "test parse"() {
    expect: 'the right value is returned'
    def date = new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)
    def s = DateUtils.formatDate(date, locale)
    DateFieldFormat.instance.parse(s, locale, null) == date

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

  def "test format"() {
    expect: 'the right value is returned'
    DateFieldFormat.instance.format(date, locale, null) == DateUtils.formatDate(date, locale)

    where:
    locale         | date
    Locale.US      | new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)
    Locale.GERMANY | new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)
  }

  def "test format with null date"() {
    expect: 'the right value is returned'
    DateFieldFormat.instance.format(null, Locale.GERMANY, null) == ''
  }

  def "test encode"() {
    expect: 'the right value is returned'
    def date = new Date(UnitTestUtils.SAMPLE_TIME_MS)
    DateFieldFormat.instance.encode(date, null) == ISODate.format(date)
  }

  def "test encode with null value"() {
    expect: 'the right value is returned'
    DateFieldFormat.instance.encode(null, null) == ''
  }

  def "test decode with real date"() {
    expect: 'the right value is returned'
    def date = new Date(UnitTestUtils.SAMPLE_TIME_MS)
    def s = DateFieldFormat.instance.encode(date, null)
    DateFieldFormat.instance.decode(s, null) == date
  }

  def "test decode with null"() {
    expect: 'the right value is returned'
    DateFieldFormat.instance.decode('', null) == null
  }

  def "test formatForm/parseForm round-trip with different locale"() {
    expect: 'the right value is returned and the locale is ignored'
    def date = new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)
    def s = DateFieldFormat.instance.formatForm(date, Locale.US, null)
    DateFieldFormat.instance.parseForm(s, Locale.GERMANY, null) == date
  }

  def "test decode fails"() {
    when: 'a bad value is decoded'
    DateFieldFormat.instance.decode('abc45678901234567890', null)

    then: 'an exception is thrown'
    def ex = thrown(ParseException)
    UnitTestUtils.assertContainsAllIgnoreCase(ex.toString(), ['abc'])
  }

  def "verify that the getGridEditor returns a standard date editor for this class"() {
    expect: 'the right editor is returned'
    DateFieldFormat.instance.getGridEditor() == 'keyboardEditDateTime'
  }

  def "verify that the convertToJsonFormat works"() {
    given: 'a date'
    def aDate = new Date(UnitTestUtils.SAMPLE_TIME_MS)

    expect: 'convert works'
    DateFieldFormat.instance.convertToJsonFormat(aDate, null) == ISODate.format(aDate)
  }

  def "verify that the convertFromJsonFormat works"() {
    expect: 'the convert works'
    DateFieldFormat.instance.convertFromJsonFormat(value, null) == result

    where:
    value                                                  | result
    ISODate.format(new Date(UnitTestUtils.SAMPLE_TIME_MS)) | new Date(UnitTestUtils.SAMPLE_TIME_MS)
    ''                                                     | null
    null                                                   | null
  }

  def "verify that basic encoding fails with invalid types"() {
    when: 'an invalid value is encoded'
    DateFieldFormat.instance.encode(value, null)

    then: 'the right exception is thrown'
    def ex = thrown(IllegalArgumentException)
    UnitTestUtils.assertExceptionIsValid(ex, ['type', 'date', value.toString(), value.getClass().toString()])

    where:
    value | _
    'abc' | _
    127L  | _
  }
}
