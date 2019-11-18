package org.simplemes.eframe.data.format

import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.date.ISODate
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
@SuppressWarnings("GroovyAssignabilityCheck")
class DateOnlyFieldFormatSpec extends BaseSpecification {

  def "verify that id and toString work and the format is registered in the BasicFieldFormat class"() {
    expect:
    DateOnlyFieldFormat.instance.id == DateOnlyFieldFormat.ID
    DateOnlyFieldFormat.instance.toString() == 'Date'
    DateOnlyFieldFormat.instance.type == DateOnly
    BasicFieldFormat.coreValues.contains(DateOnlyFieldFormat)
  }

  def "test parse"() {
    expect: 'the right value is returned'
    def date = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    def s = DateUtils.formatDate(date, locale)
    DateOnlyFieldFormat.instance.parse(s, locale, null) == date

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

  def "test format"() {
    expect: 'the right value is returned'
    DateOnlyFieldFormat.instance.format(date, locale, null) == DateUtils.formatDate(date, locale)

    where:
    locale         | date
    Locale.US      | new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    Locale.GERMANY | new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
  }

  def "test format with null date"() {
    expect: 'the right value is returned'
    DateOnlyFieldFormat.instance.format(null, Locale.GERMANY, null) == ''
  }

  def "test encode"() {
    expect: 'the right value is returned'
    def date = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    DateOnlyFieldFormat.instance.encode(date, null) == ISODate.format(date)
  }

  def "test encode with null value"() {
    expect: 'the right value is returned'
    DateOnlyFieldFormat.instance.encode(null, null) == ''
  }

  def "test decode with real date"() {
    expect: 'the right value is returned'
    def date = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    def s = ISODate.format(date)
    DateOnlyFieldFormat.instance.decode(s, null) == date
  }

  def "test decode with null"() {
    expect: 'the right value is returned'
    DateOnlyFieldFormat.instance.decode('', null) == null
  }

  def "test decode fails"() {
    when: 'a bad value is decoded'
    DateOnlyFieldFormat.instance.decode('abc45678901234567890', null)

    then: 'an exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertContainsAllIgnoreCase(ex.toString(), ['abc'])
  }

  def "test formatForm and parseForm round trip - and the locale is ignored"() {
    expect: 'the right value is returned'
    def date = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    def s = DateOnlyFieldFormat.instance.formatForm(date, Locale.US, null)
    s == '2010-06-15'
    DateOnlyFieldFormat.instance.parseForm(s, Locale.GERMANY, null) == date
  }

  def "test parseForm with extra time on the end"() {
    expect: 'the right value is returned'
    def date = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    def s = '2010-06-15 00:00:00'
    DateOnlyFieldFormat.instance.parseForm(s, Locale.GERMANY, null) == date
  }

  def "verify that the getGridEditor returns a standard date editor for this class"() {
    expect: 'the right editor is returned'
    DateOnlyFieldFormat.instance.getGridEditor() == 'keyboardEditDate'
  }

  def "verify that the convertToJsonFormat works"() {
    given: 'a date'
    def aDate = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)

    expect: 'conversion works'
    DateOnlyFieldFormat.instance.convertToJsonFormat(aDate, null) == ISODate.format(aDate)
  }

  def "verify that the convertFromJsonFormat works"() {
    expect: 'conversion works'
    DateOnlyFieldFormat.instance.convertFromJsonFormat(value, null) == result

    where:
    value                                                           | result
    ISODate.format(new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)) | new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    ''                                                              | null
    null                                                            | null
  }

  def "verify that basic encoding fails with invalid types"() {
    when: 'an invalid value is encoded'
    DateOnlyFieldFormat.instance.encode(value, null)

    then: 'the right exception is thrown'
    def ex = thrown(IllegalArgumentException)
    UnitTestUtils.assertExceptionIsValid(ex, ['type', 'DateOnly', value.toString(), value.getClass().toString()])

    where:
    value | _
    'abc' | _
    127L  | _
  }
}
