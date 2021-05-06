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
class LongFieldFormatSpec extends BaseSpecification {

  def "verify that id and toString work and the format is registered in the BasicFieldFormat class"() {
    expect:
    LongFieldFormat.instance.id == LongFieldFormat.ID
    LongFieldFormat.instance.toString() == 'Long'
    LongFieldFormat.instance.type == Long
    BasicFieldFormat.coreValues.contains(LongFieldFormat)
  }

  def "verify that basic formatting works"() {
    expect:
    LongFieldFormat.instance.format(value, Locale.US, null) == result

    where:
    value   | result
    null    | ''
    0L      | '0'
    247     | '247'
    247L    | '247'
    247237L | '247237'
  }

  def "verify that basic parsing works"() {
    expect:
    LongFieldFormat.instance.parse(value, Locale.US, null) == result

    where:
    value     | result
    null      | null
    ''        | null
    '0'       | 0L
    '237'     | 237L
    '237247'  | 237247L
    '237,247' | 237247L
  }

  def "verify that basic encoding works"() {
    expect:
    LongFieldFormat.instance.encode(value, null) == result

    where:
    value   | result
    null    | ''
    0       | '0'
    0L      | '0'
    247L    | '247'
    247237L | '247237'
  }

  def "verify that basic decoding works"() {
    expect:
    LongFieldFormat.instance.decode(value, null) == result

    where:
    value    | result
    null     | null
    ''       | null
    '0'      | 0L
    '237'    | 237L
    '237247' | 237247L
  }

  def "verify that basic encoding fails with invalid types"() {
    when: 'an invalid value is encoded'
    LongFieldFormat.instance.encode(value, null)

    then: 'the right exception is thrown'
    def ex = thrown(IllegalArgumentException)
    UnitTestUtils.assertExceptionIsValid(ex, ['type', 'long', value.toString(), value.getClass().toString()])
    //     throw new IllegalArgumentException("Invalid field type (${value.getClass()}). Must be Integer or Long.")

    where:
    value | _
    'abc' | _
    127.2 | _
  }

  def "verify that the client ID is correct - mapped to integer format on client"() {
    expect: 'the value is correct'
    LongFieldFormat.instance.clientFormatType == IntegerFieldFormat.instance.clientFormatType
  }

}
