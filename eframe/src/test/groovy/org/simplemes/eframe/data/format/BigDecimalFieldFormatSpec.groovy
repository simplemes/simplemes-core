package org.simplemes.eframe.data.format

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
class BigDecimalFieldFormatSpec extends BaseSpecification {

  def "verify that id and toString work and the format is registered in the BasicFieldFormat class"() {
    expect:
    BigDecimalFieldFormat.instance.id == BigDecimalFieldFormat.ID
    BigDecimalFieldFormat.instance.toString() == 'Decimal'
    BigDecimalFieldFormat.instance.type == BigDecimal
    BasicFieldFormat.coreValues.contains(BigDecimalFieldFormat)
  }

  def "verify that basic formatting works"() {
    expect:
    BigDecimalFieldFormat.instance.format(value, Locale.US, null) == result

    where:
    value    | result
    null     | ''
    0.0      | '0'
    237.2    | '237.2'
    247237.2 | '247237.2'
  }

  def "verify that basic parsing works"() {
    expect:
    BigDecimalFieldFormat.instance.parse(value, Locale.US, null) == result

    where:
    value      | result
    null       | null
    ''         | null
    '0'        | 0.0
    '0.0'      | 0.0
    '237.2'    | 237.2
    '237247.2' | 237247.2
  }

  def "verify that basic encoding works"() {
    expect:
    BigDecimalFieldFormat.instance.encode(value, null) == result

    where:
    value    | result
    null     | ''
    0        | '0'
    0.0      | '0.0'
    237.2    | '237.2'
    247.2    | '247.2'
    247237.2 | '247237.2'
    237L     | '237'
  }

  def "verify that basic decoding works"() {
    expect:
    BigDecimalFieldFormat.instance.decode(value, null) == result

    where:
    value      | result
    null       | null
    ''         | null
    '0'        | 0
    '0.0'      | 0
    '237.2'    | 237.2
    '237247.2' | 237247.2
  }

  def "verify that basic encoding fails with invalid types"() {
    when: 'an invalid value is encoded'
    BigDecimalFieldFormat.instance.encode(value, null)

    then: 'the right exception is thrown'
    def ex = thrown(IllegalArgumentException)
    UnitTestUtils.assertExceptionIsValid(ex, ['type', 'Number', value.toString(), value.getClass().toString()])

    where:
    value      | _
    'abc'      | _
    new Date() | _
  }
}
