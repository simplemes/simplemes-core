/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.misc

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import sample.pogo.SamplePOGO

/**
 * Tests.
 */
class ArgumentUtilsSpec extends BaseSpecification {

  def "verify that checkMissing detects missing value"() {
    when: 'the argument is checked'
    ArgumentUtils.checkMissing(null, 'name')

    then: 'a valid exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['name'])
  }

  def "verify that checkMissing passes when value is not missing"() {
    when: 'the argument is checked'
    ArgumentUtils.checkMissing('ABC', 'name')

    then: 'no exception is thrown'
    notThrown(Exception)
  }

  def "verify that checkForProperties passes when Ok when missing a field"() {
    expect: 'the check passes when Ok'
    ArgumentUtils.checkForProperties(new SamplePOGO(), ['name'])
  }

  def "verify that checkForProperties fails when missing a field"() {
    when: 'the check is made on a missing field'
    ArgumentUtils.checkForProperties(new SamplePOGO(), ['fieldXName'])

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['fieldXName'])
  }

  def "verify that checkForProperties fails when used on a null object"() {
    when: 'the check is made on a missing field'
    ArgumentUtils.checkForProperties(null, ['fieldXName'])

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['null'])
  }

  def "verify that convertToBigDecimal works for supported cases"() {
    expect: 'the value is converted correctly'
    ArgumentUtils.convertToBigDecimal(value) == result

    where:
    value  | result
    null   | null
    ''     | null
    '  '   | null
    '12.2' | 12.2
    22.2   | 22.2
    14     | 14.0
  }

  def "verify that convertToInteger works for supported cases"() {
    expect: 'the value is converted correctly'
    ArgumentUtils.convertToInteger(value) == result

    where:
    value | result
    null  | null
    ''    | null
    '  '  | null
    '12'  | 12
    14    | 14
  }

  def "verify that convertToBoolean works for supported cases"() {
    expect: 'the value is converted correctly'
    ArgumentUtils.convertToBoolean(value) == result

    where:
    value   | result
    null    | false
    ''      | false
    '  '    | false
    'f'     | false
    'F'     | false
    'false' | false
    'FALSE' | false
    't'     | true
    'T'     | true
    'True'  | true
    'true'  | true
    'TRUE'  | true
  }

}
