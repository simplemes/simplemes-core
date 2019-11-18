package org.simplemes.eframe.misc

import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright (c) 2018 Simple MES| LLC.  All rights reserved.  See license.txt for license terms.
 */
/**
 * Tests.
 */
class NumberUtilsSpec extends BaseSpecification {

  void cleanup() {
    // Reset the global locale to the JVM default.
    GlobalUtils.defaultLocale = Locale.default
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that formatLargeNumberReadable works for supported cases"() {
    expect: 'the correct result in the correct locale'
    NumberUtils.formatLargeNumberReadable(value, locale) == result

    where:
    value           | locale         | result
    1               | Locale.ENGLISH | '1'
    9               | Locale.ENGLISH | '9'
    19              | Locale.ENGLISH | '19'
    534             | Locale.ENGLISH | '534'
    978             | Locale.ENGLISH | '978'
    999             | Locale.ENGLISH | '999'
    1000            | Locale.ENGLISH | '1K'
    1025            | Locale.ENGLISH | '1K'
    1099            | Locale.ENGLISH | '1K'
    1127            | Locale.ENGLISH | '1.1K'
    1584            | Locale.ENGLISH | '1.5K'
    1999            | Locale.ENGLISH | '1.9K'
    2000            | Locale.ENGLISH | '2K'
    9999            | Locale.ENGLISH | '9.9K'
    19000           | Locale.ENGLISH | '19K'
    534000          | Locale.ENGLISH | '534K'
    978000          | Locale.ENGLISH | '978K'
    1000000         | Locale.ENGLISH | '1M'
    1000025         | Locale.ENGLISH | '1M'
    1000099         | Locale.ENGLISH | '1M'
    1127000         | Locale.ENGLISH | '1.1M'
    1584000         | Locale.ENGLISH | '1.5M'
    1999000         | Locale.ENGLISH | '1.9M'
    2000000         | Locale.ENGLISH | '2M'
    9999999         | Locale.ENGLISH | '9.9M'
    19000000        | Locale.ENGLISH | '19M'
    534000000       | Locale.ENGLISH | '534M'
    978000000       | Locale.ENGLISH | '978M'
    1000000000      | Locale.ENGLISH | '1G'
    1000025000      | Locale.ENGLISH | '1G'
    1000099000      | Locale.ENGLISH | '1G'
    1127000000      | Locale.ENGLISH | '1.1G'
    1584000000      | Locale.ENGLISH | '1.5G'
    1999000000      | Locale.ENGLISH | '1.9G'
    2000000000      | Locale.ENGLISH | '2G'
    9999999999      | Locale.ENGLISH | '9.9G'
    19000000000     | Locale.ENGLISH | '19G'
    534000000000    | Locale.ENGLISH | '534G'
    978000000000    | Locale.ENGLISH | '978G'
    1000000000000   | Locale.ENGLISH | '1T'
    1000025000000   | Locale.ENGLISH | '1T'
    1000099000000   | Locale.ENGLISH | '1T'
    1127000000000   | Locale.ENGLISH | '1.1T'
    1584000000000   | Locale.ENGLISH | '1.5T'
    1999000000000   | Locale.ENGLISH | '1.9T'
    2000000000000   | Locale.ENGLISH | '2T'
    9999999999999   | Locale.ENGLISH | '9.9T'
    19000000000000  | Locale.ENGLISH | '19T'
    534000000000000 | Locale.ENGLISH | '534T'
    978000000000000 | Locale.ENGLISH | '978T'
    1127000000      | Locale.GERMAN  | '1,1G'
    1584000000      | Locale.GERMAN  | '1,5G'
    1999000000      | Locale.GERMAN  | '1,9G'
    2000000000      | Locale.GERMAN  | '2G'
    9999999999      | Locale.GERMAN  | '9,9G'
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that formatNumber works with supported cases"() {
    expect: 'the value is formatted correctly'
    NumberUtils.formatNumber(value, locale, grouping) == result

    where:
    value | locale         | grouping | result
    1.127 | Locale.ENGLISH | true     | '1.127'
    127   | Locale.ENGLISH | true     | '127'
    1270  | Locale.ENGLISH | true     | '1,270'
    1270  | Locale.ENGLISH | false    | '1270'
    1.127 | Locale.GERMAN  | true     | '1,127'
    127   | Locale.GERMAN  | true     | '127'
    1270  | Locale.GERMANY | true     | '1.270'
  }

  def "verify that parseNumber works with supported cases"() {
    when: 'the default local is set for some cases'
    if (defaultLocale) {
      GlobalUtils.defaultLocale = defaultLocale
    }

    then: 'the method parses correctly, using the default locale in some cases'
    NumberUtils.parseNumber(value, passedLocale) == result

    cleanup:
    GlobalUtils.defaultLocale = Locale.US

    where:
    value   | passedLocale   | defaultLocale | result
    '1.127' | Locale.ENGLISH | null          | 1.127
    '127'   | Locale.ENGLISH | null          | 127
    '1270'  | Locale.ENGLISH | null          | 1270
    '1.127' | Locale.GERMAN  | null          | 1127
    '127'   | Locale.GERMAN  | null          | 127
    '1270'  | Locale.GERMANY | null          | 1270
    '1.127' | null           | Locale.GERMAN | 1127
    '1,127' | null           | Locale.US     | 1127
  }

  def "verify that determineDecimalSeparator works"() {
    expect: ''
    NumberUtils.determineDecimalSeparator(Locale.ENGLISH) == '.'
    NumberUtils.determineDecimalSeparator(Locale.GERMANY) == ','
  }

  def "verify that isNumberClass works with supported cases"() {
    expect:
    NumberUtils.isNumberClass(clazz) == result

    where:
    clazz      | result
    int        | true
    Integer    | true
    BigDecimal | true
    long       | true
    Long       | true
    short      | true
    String     | false
  }

  def "verify that divideRoundingUp works with supported cases"() {
    expect:
    NumberUtils.divideRoundingUp(top, bottom) == result

    where:
    top | bottom | result
    0   | 1      | 0
    6   | 10     | 1
    10  | 10     | 1
    11  | 10     | 2
    19  | 10     | 2
    20  | 10     | 2
    21  | 10     | 3
  }

  def "verify that isNumber works with supported cases"() {
    expect:
    NumberUtils.isNumber(value) == result

    where:
    value    | result
    null     | false
    ''       | false
    'A'      | false
    'A123'   | false
    '123A'   | false
    '123'    | true
    '123.00' | true
    '123,00' | true
  }

  def "verify that trimTrailingZeros works with supported cases"() {
    expect: 'the value is trimmed correctly'
    NumberUtils.trimTrailingZeros(value, locale) == result

    where:
    value       | locale         | result
    '0'         | Locale.ENGLISH | '0'
    '0.00'      | Locale.ENGLISH | '0.0'
    '0.0'       | Locale.ENGLISH | '0.0'
    '1.100'     | Locale.ENGLISH | '1.1'
    '1.10'      | Locale.ENGLISH | '1.1'
    '1.0'       | Locale.ENGLISH | '1.0'
    '1'         | Locale.ENGLISH | '1'
    '1.127'     | Locale.ENGLISH | '1.127'
    '1,231.127' | Locale.ENGLISH | '1,231.127'
    '0,0'       | Locale.GERMANY | '0,0'
    '1,100'     | Locale.GERMANY | '1,1'
    '1.231,127' | Locale.GERMANY | '1.231,127'
  }


}
