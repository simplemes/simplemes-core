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
class IntegerFieldFormatSpec extends BaseSpecification {

  def "verify that id and toString work and the format is registered in the BasicFieldFormat class"() {
    expect:
    IntegerFieldFormat.instance.id == IntegerFieldFormat.ID
    IntegerFieldFormat.instance.toString() == 'Integer'
    IntegerFieldFormat.instance.type == Integer
    BasicFieldFormat.coreValues.contains(IntegerFieldFormat)
  }

  def "verify that basic formatting works"() {
    expect:
    IntegerFieldFormat.instance.format(value, Locale.US, null) == result

    where:
    value  | result
    null   | ''
    0      | '0'
    247    | '247'
    247237 | '247237'
  }

  def "verify that basic parsing works"() {
    expect:
    IntegerFieldFormat.instance.parse(value, Locale.US, null) == result

    where:
    value         | result
    null          | null
    ''            | null
    '0'           | 0
    '237'         | 237
    '237247'      | 237247
    '237,247'     | 237247
    '237,247,137' | 237247137
  }

  def "verify that basic encoding works"() {
    expect:
    IntegerFieldFormat.instance.encode(value, null) == result

    where:
    value  | result
    null   | ''
    0      | '0'
    247    | '247'
    247237 | '247237'
  }

  def "verify that basic decoding works"() {
    expect:
    IntegerFieldFormat.instance.decode(value, null) == result

    where:
    value    | result
    null     | null
    ''       | null
    '0'      | 0
    '237'    | 237
    '237247' | 237247
  }

  def "verify that basic encoding fails with invalid types"() {
    when: 'an invalid value is encoded'
    IntegerFieldFormat.instance.encode(value, null)

    then: 'the right exception is thrown'
    def ex = thrown(IllegalArgumentException)
    UnitTestUtils.assertExceptionIsValid(ex, ['type', 'integer', value.toString(), value.getClass().toString()])

    where:
    value | _
    'abc' | _
    127L  | _
  }

}
